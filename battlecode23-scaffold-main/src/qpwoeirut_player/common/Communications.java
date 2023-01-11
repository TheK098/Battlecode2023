package qpwoeirut_player.common;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import static qpwoeirut_player.common.Pathfinding.locationInArray;

// TODO: consider cycling through writing data so that all the bots can eventually know everything

/**
 * Handles all communications.
 * Indexes 0-59 hold well locations.
 * Indexes 60-63 hold HQ locations.
 * Locations are stored as x * 60 + y + 1. 0 represents no data.
 */
public class Communications {
    private static final int INVALID = 0;

    private static final int MAP_SIZE = 60;

    private static final int MAX_COUNT = 150;

    private static final MapLocation[] locations = new MapLocation[MAX_COUNT];

    private static final MapLocation[] wellCache = new MapLocation[MAX_COUNT];
    private static int wellCacheSize = 0;

    public static MapLocation[] getKnownWells(RobotController rc) throws GameActionException {
        return getLocations(rc, TileType.WELL);
    }

    public static MapLocation[] getHqs(RobotController rc) throws GameActionException {
        return getLocations(rc, TileType.HQ);
    }

    private static MapLocation[] getLocations(RobotController rc, TileType tileType) throws GameActionException {
        int locationsIdx = loadSharedLocations(rc, tileType);
        if (tileType == TileType.WELL) {
            for (int i = wellCacheSize; i --> 0;) {
                locations[locationsIdx++] = wellCache[i];
            }
        }

        MapLocation[] ret = new MapLocation[locationsIdx];
        System.arraycopy(locations, 0, ret, 0, locationsIdx);
        return ret;
    }

    private static int loadSharedLocations(RobotController rc, TileType tileType) throws GameActionException {
        int locationsIdx = 0;
        for (int i = tileType.count; i --> 0;) {
            int value = rc.readSharedArray(tileType.offset + i);
            if (value != INVALID) {
                --value;
                locations[locationsIdx++] = new MapLocation(value / MAP_SIZE, value % MAP_SIZE);
            }
        }
        return locationsIdx;
    }

    public void addWell(RobotController rc, MapLocation wellLoc) throws GameActionException {
        MapLocation[] knownLocations = getLocations(rc, TileType.WELL);
        if (!locationInArray(knownLocations, wellLoc)) {
            wellCache[wellCacheSize++] = wellLoc;
            tryPushCache(rc);
        }
    }
    public void addHq(RobotController rc, MapLocation hqLoc) throws GameActionException {
        MapLocation[] knownLocations = getLocations(rc, TileType.HQ);
        if (!locationInArray(knownLocations, hqLoc)) {
            int index = findEmptySpot(rc, TileType.HQ);
            if (rc.canWriteSharedArray(index, pack(hqLoc))) {
                rc.writeSharedArray(index, pack(hqLoc));
            }
        }
    }

    private static int findEmptySpot(RobotController rc, TileType tileType) throws GameActionException, IllegalStateException {
        for (int i = tileType.count; i --> 0;) {
            int value = rc.readSharedArray(tileType.offset + i);
            if (value == INVALID) return tileType.offset + i;
        }
        throw new IllegalStateException("Ran out of empty spots");
    }

    public static void tryPushCache(RobotController rc) throws GameActionException {
        MapLocation[] knownWells = getKnownWells(rc);
        while (wellCacheSize --> 0) {
            int ct = 0;
            for (int j = knownWells.length; j --> 0;) if (knownWells[j].equals(wellCache[wellCacheSize])) ++ct;

            if (ct > 1) continue;  // value has already been pushed by another bot

            int index = findEmptySpot(rc, TileType.WELL);
            if (index != -1 && rc.canWriteSharedArray(index, pack(wellCache[wellCacheSize]))) {
                rc.writeSharedArray(index, pack(wellCache[wellCacheSize]));
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
}
