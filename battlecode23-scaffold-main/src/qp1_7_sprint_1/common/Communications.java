package qp1_7_sprint_1.common;

import battlecode.common.*;

import static qp1_7_sprint_1.utilities.Util.locationInArray;

// TODO: consider cycling through writing data so that all the bots can eventually know everything

/**
 * Handles all communications.
 * Array spots are allocated in EntityType.java
 * Locations are stored as x * 60 + y + 1. 0 represents no data.
 */
public class Communications {
    private static final int INVALID = 0;

    private static final int MAP_SIZE = 60;
    private static final int MAX_LOCATION = MAP_SIZE * MAP_SIZE + 1;
    private static final int MAX_VALUE = 17;
    private static final int RESOURCE_INDEX = 63;

    private static final int MAX_COUNT = Math.max(6 * 6 * 4, EntityType.ENEMY.count);

    private static final int[] indexes = new int[MAX_COUNT];
    private static final MapLocation[] locations = new MapLocation[MAX_COUNT];
    private static final int[] urgencies = new int[EntityType.ENEMY.count];
    private static final ResourceType[] wellType = new ResourceType[EntityType.WELL.count];

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
            sightings[i] = new EnemySighting(locations[i], urgencies[i]);
        }
        return sightings;
    }

    private static int loadSharedLocations(RobotController rc, EntityType entityType) throws GameActionException {
        int locationsIdx = 0;
        for (int i = entityType.count; i --> 0;) {
            int value = rc.readSharedArray(entityType.offset + i);
            if (value != INVALID) {
                if (entityType == EntityType.ENEMY) urgencies[locationsIdx] = value / MAX_LOCATION;
                else if (entityType == EntityType.WELL) wellType[locationsIdx] = ResourceType.values()[value / MAX_LOCATION];
                indexes[locationsIdx] = entityType.offset + i;
                locations[locationsIdx++] = unpackLocation(value);
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
        tryPushCache(rc);
    }
    public static void addHq(RobotController rc, MapLocation hqLoc) throws GameActionException {
        MapLocation[] knownLocations = getHqs(rc);
        if (!locationInArray(knownLocations, hqLoc)) {
            int index = findEmptySpot(rc, EntityType.HQ);
            if (rc.canWriteSharedArray(index, pack(hqLoc))) {
                rc.writeSharedArray(index, pack(hqLoc));
            }
        }
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
                    if (minCount > urgencies[i]) {
                        minCount = urgencies[i];
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
                    int newValue = pack(locations[i], urgencies[i] + 1);
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
            int newValue = pack(locations[i], Math.max(0, urgencies[i] - 1));
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

    public static void tryPushCache(RobotController rc) throws GameActionException {
        WellLocation[] knownWells = getKnownWells(rc);
        while (wellCacheSize --> 0) {
            int ct = 0;
            for (int j = knownWells.length; j --> 0;) if (knownWells[j].location.equals(wellCache[wellCacheSize])) ++ct;

            if (ct > 1) continue;  // value has already been pushed by another bot

            int index = findEmptySpot(rc, EntityType.WELL);
            if (index != -1 && rc.canWriteSharedArray(index, pack(wellCache[wellCacheSize], wellTypeCache[wellCacheSize]))) {
                rc.writeSharedArray(index, pack(wellCache[wellCacheSize], wellTypeCache[wellCacheSize]));
            } else {
                ++wellCacheSize;
                break;
            }
        }
        if (wellCacheSize == -1) wellCacheSize = 0;
    }

    private static int pack(MapLocation loc) {
        return loc.x * MAP_SIZE + loc.y + 1;
    }
    private static MapLocation unpackLocation(int x) {
        x = (x % MAX_LOCATION) - 1;
        return new MapLocation(x / MAP_SIZE, x % MAP_SIZE);
    }
    private static int pack(MapLocation loc, int urgency) {
        return Math.min(MAX_VALUE, urgency) * MAX_LOCATION + pack(loc);
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
