package qpwoeirut_player.utilities;

import battlecode.common.*;

import static qpwoeirut_player.common.Pathfinding.DIRECTIONS;
import static qpwoeirut_player.common.Pathfinding.INF_DIST;

public class Util {
    // TODO: include pathfinding in the future
    public static MapLocation pickNearest(RobotController rc, MapLocation[] locations, int[][] blacklist) {
        int closestIndex = 0;
        int closestDistance = INF_DIST;
        for (int i = locations.length; i --> 0;) {
            int distance = locations[i].distanceSquaredTo(rc.getLocation());
            if (closestDistance > distance && blacklist[locations[i].x][locations[i].y] <= rc.getRoundNum()) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        return locations[closestIndex];
    }

    public static boolean adjacentToHeadquarters(RobotController rc, MapLocation location) throws GameActionException {
        for (Direction dir: DIRECTIONS) {
            MapLocation loc = location.add(dir);
            if (rc.canSenseLocation(loc)) {
                RobotInfo robot = rc.senseRobotAtLocation(loc);
                if (robot != null && robot.team == rc.getTeam() && robot.type == RobotType.HEADQUARTERS) return true;
            }
        }
        return false;
    }

    public static boolean adjacentToWell(RobotController rc, MapLocation location) throws GameActionException {
        for (Direction dir: Direction.allDirections()) {  // include current location
            MapLocation loc = location.add(dir);
            if (rc.canSenseLocation(loc)) {
                WellInfo well = rc.senseWell(loc);
                if (well != null) return true;
            }
        }
        return false;
    }

    public static Direction randomDirection(RobotController rc) throws GameActionException {
        Direction dir;

        for (int i = 15; i --> 0;) {
            dir = DIRECTIONS[FastRandom.nextInt(DIRECTIONS.length)];
            if (rc.canMove(dir)) return dir;
        }
        return Direction.CENTER;
    }

    public static boolean locationInArray(MapLocation[] array, MapLocation loc) {
        for (int i = array.length; i-- > 0; ) {
            if (array[i].equals(loc)) return true;
        }
        return false;
    }
}
