package qpwoeirut_player.utilities;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

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

    private static final int WELL_OFFSET = 0;
    private static final int WELLS = 60;

    private static final int HQ_OFFSET = 60;
    private static final int HQS = 4;

    public static MapLocation[] getKnownWells(RobotController rc) throws GameActionException {
        return getLocations(rc, WELL_OFFSET, WELLS);
    }

    public static MapLocation[] getHqs(RobotController rc) throws GameActionException {
        return getLocations(rc, HQ_OFFSET, HQS);
    }

    private static MapLocation[] getLocations(RobotController rc, int offset, int size) throws GameActionException {
        MapLocation[] locations = new MapLocation[size];
        int locationsIdx = 0;
        for (int i = size; i --> 0;) {
            int value = rc.readSharedArray(offset + i);
            if (value != INVALID) {
                --value;
                locations[locationsIdx++] = new MapLocation(value / MAP_SIZE, value % MAP_SIZE);
            }
        }

        MapLocation[] ret = new MapLocation[locationsIdx];
        System.arraycopy(locations, 0, ret, 0, locationsIdx);
        return ret;
    }

    // TODO: give each bot a cache to avoid reading the shared array repeatedly
    // returns whether bot was able to write to the array
    public static boolean addWell(RobotController rc, MapLocation wellLoc) throws GameActionException {
        return addLocation(rc, wellLoc, WELL_OFFSET, WELLS);
    }
    public static boolean addHq(RobotController rc, MapLocation hqLoc) throws GameActionException {
        return addLocation(rc, hqLoc, HQ_OFFSET, HQS);
    }

    private static boolean addLocation(RobotController rc, MapLocation loc, int offset, int size) throws GameActionException {
        MapLocation[] knownLocations = getLocations(rc, offset, size);

        if (!Util.locationInArray(knownLocations, loc)) {
            int index = findEmptySpot(rc, offset, size);
            int value = loc.x * MAP_SIZE + loc.y + 1;
            if (index != -1 && rc.canWriteSharedArray(index, value)) {
                rc.writeSharedArray(index, value);
                return true;
            }
        }
        return false;
    }

    private static int findEmptySpot(RobotController rc, int offset, int size) throws GameActionException, IllegalStateException {
        for (int i = size; i --> 0;) {
            int value = rc.readSharedArray(offset + i);
            if (value == INVALID) return offset + i;
        }
        throw new IllegalStateException("Ran out of empty spots");
    }
}
