package qp1.communications;

import battlecode.common.*;

import static qp1.utilities.Util.locationInArray;

// TODO: consider cycling through writing data so that all the bots can eventually know everything

/**
 * Handles all communications.
 * Array spots are allocated in EntityType.java
 * Locations are stored as x * 60 + y + 1. 0 represents no data.
 */
public class Comms {
    private static final int INVALID = 0;

    private static final int MAP_SIZE = 60;
    private static final int MAP_COMPRESSION = 3;
    private static final int UPDATE_FREQ = 40;

    private static final int MAX_COMPRESSED_LOCATION = (MAP_SIZE / MAP_COMPRESSION) * (MAP_SIZE / MAP_COMPRESSION) + 1;
    private static final int MAX_LOCATION = MAP_SIZE * MAP_SIZE + 1;
    private static final int MAX_URGENCY = 17;
    private static final int MAX_ISLAND_ID = 35;
    private static final int RESOURCE_INDEX = 63;

    private static final int MAX_COUNT = 6 * 6 * 4;

    private static final int[] indexes = new int[MAX_COUNT];
    private static final MapLocation[] locations = new MapLocation[MAX_COUNT];
    private static final int[] additionalValues = new int[MAX_COUNT];
    private static final ResourceType[] wellType = new ResourceType[MAX_COUNT];

    private static final IslandInfo[] islandCache = new IslandInfo[MAX_ISLAND_ID];
    private static final MapLocation[] wellCache = new MapLocation[6 * 6 * 4];
    private static final ResourceType[] wellTypeCache = new ResourceType[6 * 6 * 4];
    private static int wellCacheSize = 0;

    public static class WellLocation {
        public MapLocation location;
        public ResourceType resourceType;
        WellLocation(MapLocation location, ResourceType resourceType) {
            this.location = location;
            this.resourceType = resourceType;
        }
        @Override
        public String toString() {
            return "WellLocation(" + location.toString() + ", " + resourceType + ")";
        }
    }

    public static class EnemySighting {
        public MapLocation location;
        public int urgency;
        EnemySighting(MapLocation location, int urgency) {
            this.location = location;
            this.urgency = urgency;
        }
        @Override
        public String toString() {
            return "EnemySighting(" + location.toString() + ", " + urgency + ")";
        }
    }

    public static class IslandInfo {
        public MapLocation location;
        public int id;
        public Team team;
        public int lastUpdate;
        public IslandInfo(MapLocation location, int id, Team team, int lastUpdate) {
            this.location = new MapLocation(location.x / MAP_COMPRESSION, location.y / MAP_COMPRESSION);
            this.id = id;
            this.team = team;
            this.lastUpdate = lastUpdate;
        }
        IslandInfo(int x) {
            --x;
            this.location = unpackLocation(x % MAX_COMPRESSED_LOCATION); x /= MAX_COMPRESSED_LOCATION;
            this.id = (x % MAX_ISLAND_ID) + 1; x /= MAX_ISLAND_ID;
            this.team = Team.values()[x % 3]; x /= 3;
            this.lastUpdate = x;
        }
        @Override
        public int hashCode() {
            return (lastUpdate * 3 + team.ordinal()) * MAX_COMPRESSED_LOCATION + pack(location) + 1;
        }
        @Override
        public String toString() {
            return "IslandInfo(" + location.toString() + ", " + id + ", " + team + ", " + lastUpdate + ")";
        }
    }

    public static WellLocation[] getKnownWells(RobotController rc) throws GameActionException {
        int locationsIdx = loadSharedLocations(rc, EntityType.WELL);
        int n = locationsIdx + wellCacheSize;
        WellLocation[] wells = new WellLocation[n];
        for (int i = locationsIdx; i --> 0;) wells[i] = new WellLocation(locations[i], wellType[i]);
        for (int i = wellCacheSize; i --> 0;) wells[locationsIdx + i] = new WellLocation(wellCache[i], wellTypeCache[i]);
        return wells;
    }
    public static MapLocation[] getHqs(RobotController rc) throws GameActionException {
        int locationsIdx = loadSharedLocations(rc, EntityType.HQ);
        MapLocation[] ret = new MapLocation[locationsIdx];
        System.arraycopy(locations, 0, ret, 0, locationsIdx);
        return ret;
    }
    public static EnemySighting[] getEnemySightings(RobotController rc) throws GameActionException {
        int locationsIdx = loadSharedLocations(rc, EntityType.ENEMY);
        EnemySighting[] sightings = new EnemySighting[locationsIdx];
        for (int i = locationsIdx; i --> 0;) {
            sightings[i] = new EnemySighting(locations[i], additionalValues[i]);
        }
        return sightings;
    }
    public static IslandInfo[] getIslands(RobotController rc) throws GameActionException {
        int n = 0;
        IslandInfo[] islandsTmp = new IslandInfo[rc.getIslandCount()];
        for (int i = EntityType.ISLAND.count; i --> 0;) {
            int value = rc.readSharedArray(EntityType.ISLAND.offset + i);
            if (value != INVALID) {
                islandsTmp[n] = new IslandInfo(value);
                islandsTmp[n].location = new MapLocation(islandsTmp[n].location.x * MAP_COMPRESSION, islandsTmp[n].location.y * MAP_COMPRESSION);
                ++n;
            }
        }
        IslandInfo[] islands = new IslandInfo[n];
        System.arraycopy(islandsTmp, 0, islands, 0, n);
        return islands;
    }

    private static int loadSharedLocations(RobotController rc, EntityType entityType) throws GameActionException {
        int locationsIdx = 0;
        for (int i = entityType.count; i --> 0;) {
            int value = rc.readSharedArray(entityType.offset + i);
            if (value != INVALID) {
                additionalValues[locationsIdx] = value / MAX_LOCATION;
                indexes[locationsIdx] = entityType.offset + i;
                locations[locationsIdx] = unpackLocation(value);
                if (entityType == EntityType.WELL) wellType[locationsIdx] = ResourceType.values()[value / MAX_LOCATION];
                ++locationsIdx;
            }
        }
        return locationsIdx;
    }

    public static void addWells(RobotController rc, WellInfo[] wells) throws GameActionException {
        int n = loadSharedLocations(rc, EntityType.WELL);
        for (int i = wells.length; i --> 0;) {
            if (!locationInArray(locations, n, wells[i].getMapLocation()) && !locationInArray(wellCache, wellCacheSize, wells[i].getMapLocation())) {
                wellTypeCache[wellCacheSize] = wells[i].getResourceType();
                wellCache[wellCacheSize++] = wells[i].getMapLocation();
            }
        }
        tryPushWellCache(rc);
    }
    public static void addHq(RobotController rc, MapLocation hqLoc) throws GameActionException {
        MapLocation[] knownLocations = getHqs(rc);
        if (!locationInArray(knownLocations, hqLoc)) {
            int offset =  // need to pick lowest for BaseBot.updateCommsOffsets to work
                    rc.readSharedArray(EntityType.HQ.offset) == INVALID ? 0 :
                    rc.readSharedArray(EntityType.HQ.offset + 1) == INVALID ? 1 :
                    rc.readSharedArray(EntityType.HQ.offset + 2) == INVALID ? 2 :
                    rc.readSharedArray(EntityType.HQ.offset + 3) == INVALID ? 3 : 0;
            if (rc.canWriteSharedArray(EntityType.HQ.offset + offset, pack(hqLoc))) {
                rc.writeSharedArray(EntityType.HQ.offset + offset, pack(hqLoc));
            }
        }
    }
    public static void addNearbyIslands(RobotController rc) throws GameActionException {
        int[] islands = rc.senseNearbyIslands();
        for (int i = islands.length; i --> 0;) {
            MapLocation[] locations = rc.senseNearbyIslandLocations(islands[i]);
            int index = EntityType.ISLAND.offset + islands[i] - 1;
            IslandInfo island = new IslandInfo(locations[0], islands[i], rc.senseTeamOccupyingIsland(islands[i]), rc.getRoundNum() / UPDATE_FREQ);
            if (rc.canWriteSharedArray(index, island.hashCode())) rc.writeSharedArray(index, island.hashCode());
            else islandCache[island.id] = island;
        }
        tryPushIslandCache(rc);
    }
    public static boolean reportEnemySighting(RobotController rc, MapLocation enemyLoc) throws GameActionException {
        if (!rc.canWriteSharedArray(0, 0)) return false;

        int n = loadSharedLocations(rc, EntityType.ENEMY);
        int nearbyCount = 0;  // number of previous sightings close to this one
        for (int i = n; i --> 0;) {
            if (locations[i].isWithinDistanceSquared(enemyLoc, 25)) ++nearbyCount;
        }

        if (nearbyCount == 0) {
            int emptySpot = findEmptySpot(rc, EntityType.ENEMY);
            if (emptySpot == -1) {
                int minCount = MAX_COUNT;
                for (int i = EntityType.ENEMY.count; i --> 0;) {
                    if (minCount > additionalValues[i]) {
                        minCount = additionalValues[i];
                        emptySpot = indexes[i];
                    }
                }
            }
            int newValue = pack(enemyLoc, 2);
            if (rc.canWriteSharedArray(emptySpot, newValue)) {
                rc.writeSharedArray(emptySpot, newValue);
                return true;
            }
        } else {
            for (int i = n; i-- > 0; ) {
                if (locations[i].isWithinDistanceSquared(enemyLoc, 25)) {
                    int newValue = pack(locations[i], additionalValues[i] + 1);
                    if (rc.canWriteSharedArray(indexes[i], newValue)) {
                        rc.writeSharedArray(indexes[i], newValue);
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public static void decreaseUrgencies(RobotController rc) throws GameActionException {
        assert rc.canWriteSharedArray(0, 0);
        int n = loadSharedLocations(rc, EntityType.ENEMY);
        for (int i = n; i --> 0;) {
            int newValue = pack(locations[i], Math.max(0, additionalValues[i] - 1));
            rc.writeSharedArray(indexes[i], newValue);
        }
    }

    private static int findEmptySpot(RobotController rc, EntityType entityType) throws GameActionException {
        for (int i = entityType.count; i --> 0;) {
            int value = rc.readSharedArray(entityType.offset + i);
            if (value == INVALID) return entityType.offset + i;
        }
        return -1;
    }

    public static void tryPushWellCache(RobotController rc) throws GameActionException {
        if (rc.canWriteSharedArray(0, 0) && wellCacheSize > 0) {  // just check once at top, hopefully we don't have bytecode issues
            WellLocation[] knownWells = getKnownWells(rc);
            while (wellCacheSize-- > 0) {
                int ct = 0;
                for (int j = knownWells.length; j-- > 0; )
                    if (knownWells[j].location.equals(wellCache[wellCacheSize])) ++ct;

                if (ct > 1) continue;  // value has already been pushed by another bot

                int index = findEmptySpot(rc, EntityType.WELL);
                if (index != -1) {
                    rc.writeSharedArray(index, pack(wellCache[wellCacheSize], wellTypeCache[wellCacheSize]));
                } else {
                    ++wellCacheSize;
                    break;
                }
            }
            if (wellCacheSize == -1) wellCacheSize = 0;
        }
    }
    public static void tryPushIslandCache(RobotController rc) throws GameActionException {
        if (rc.canWriteSharedArray(0, 0)) {  // just check once at top, hopefully we don't have bytecode issues
            for (int i = EntityType.ISLAND.count; i-- > 0; ) {
                int index = EntityType.ISLAND.offset + i;
                int currentValue = rc.readSharedArray(index);

                if (islandCache[i] != null) {
                    if (currentValue == 0) {
                        rc.writeSharedArray(index, islandCache[i].hashCode());
                        islandCache[i] = null;
                    } else {
                        IslandInfo currentIsland = new IslandInfo(rc.readSharedArray(index));
                        if (islandCache[i].lastUpdate > currentIsland.lastUpdate) {
                            rc.writeSharedArray(index, islandCache[i].hashCode());
                            islandCache[i] = null;
                        }
                    }
                }
            }
        }
    }

    private static int pack(MapLocation loc) {
        return loc.x * MAP_SIZE + loc.y + 1;
    }
    private static MapLocation unpackLocation(int x) {
        x = (x % MAX_LOCATION) - 1;
        return new MapLocation(x / MAP_SIZE, x % MAP_SIZE);
    }
    private static int pack(MapLocation loc, int urgency) {
        return Math.min(MAX_URGENCY, urgency) * MAX_LOCATION + pack(loc);
    }
    private static int pack(MapLocation loc, ResourceType resourceType) {
        return resourceType.ordinal() * MAX_LOCATION + pack(loc);
    }

    public static int getAdamantiumPriority(RobotController rc) throws GameActionException {
        return rc.readSharedArray(RESOURCE_INDEX) / 256;
    }
    public static int getManaPriority(RobotController rc) throws GameActionException {
        return rc.readSharedArray(RESOURCE_INDEX) % 256;
    }
    public static void setResourcePriorities(RobotController rc, int adamantiumPriority, int manaPriority) throws GameActionException {
        rc.writeSharedArray(RESOURCE_INDEX, (adamantiumPriority << 8) | manaPriority);
    }
}
