package qpwoeirut_player.utilities;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.ArrayList;
import java.util.Arrays;

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
        ArrayList<MapLocation> locations = new ArrayList<>();
        for (int i = size; i --> 0;) {
            int value = rc.readSharedArray(offset + i);
            if (value != INVALID) {
                --value;
                locations.add(new MapLocation(value / MAP_SIZE, value % MAP_SIZE));
            }
        }

        // https://stackoverflow.com/questions/9572795/convert-list-to-array-in-java
        return locations.toArray(new MapLocation[0]);
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
        MapLocation[] knownWells = getKnownWells(rc);

        // TODO: check bytecode for this line, probably expensive
        // lambda is required because the Battlecode impl doesn't allow passing ::equals reference
        if (Arrays.stream(knownWells).noneMatch(l -> loc.equals(l))) {
            int index = findEmptySpot(rc, offset, size);
            int value = loc.x * MAP_SIZE + loc.y + 1;
            if (index != -1 && rc.canWriteSharedArray(index, value)) {
                rc.writeSharedArray(index, value);
                return true;
            }
        }
        return false;
    }

    private static int findEmptySpot(RobotController rc, int offset, int size) throws GameActionException {
        for (int i = size; i --> 0;) {
            int value = rc.readSharedArray(offset + i);
            if (value == INVALID) return offset + i;
        }
        return -1;
    }
}
