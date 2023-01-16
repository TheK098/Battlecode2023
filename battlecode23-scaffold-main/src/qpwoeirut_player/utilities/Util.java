package qpwoeirut_player.utilities;

import battlecode.common.*;
import qpwoeirut_player.common.Communications.WellLocation;

import static qpwoeirut_player.common.Pathfinding.DIRECTIONS;
import static qpwoeirut_player.common.Pathfinding.INF_DIST;

public class Util {
    // TODO: include pathfinding in the future
    public static MapLocation pickNearest(RobotController rc, MapLocation[] locations, int[][] blacklist) {
        int closestIndex = -1;
        int closestDistance = INF_DIST;
        for (int i = locations.length; i --> 0;) {
            int distance = locations[i].distanceSquaredTo(rc.getLocation());
            if (closestDistance > distance && blacklist[locations[i].x][locations[i].y] <= rc.getRoundNum()) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        return closestIndex == -1 ? null : locations[closestIndex];
    }
    public static MapLocation pickNearest(RobotController rc, MapLocation[] locations) {
        return pickNearest(rc.getLocation(), locations);
    }
    public static MapLocation pickNearest(MapLocation origin, MapLocation[] locations) {
        int closestIndex = -1;
        int closestDistance = INF_DIST;
        for (int i = locations.length; i --> 0;) {
            int distance = locations[i].distanceSquaredTo(origin);
            if (closestDistance > distance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        return closestIndex == -1 ? null : locations[closestIndex];
    }
    public static WellLocation pickNearest(MapLocation origin, WellLocation[] wells) {
        int closestIndex = -1;
        int closestDistance = INF_DIST;
        for (int i = wells.length; i --> 0;) {
            int distance = wells[i].location.distanceSquaredTo(origin);
            if (closestDistance > distance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        return closestIndex == -1 ? null : wells[closestIndex];
    }
    public static RobotInfo pickNearest(RobotController rc, RobotInfo[] robots, boolean includeHeadquarters) {
        int closestIndex = -1;
        int closestDistance = INF_DIST;
        for (int i = robots.length; i --> 0;) {
            int distance = robots[i].location.distanceSquaredTo(rc.getLocation());
            if (closestDistance > distance && (includeHeadquarters || robots[i].getType() != RobotType.HEADQUARTERS)) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        return closestIndex == -1 ? null : robots[closestIndex];
    }
    public static WellLocation pickNearest(RobotController rc, WellLocation[] wells, ResourceType resourceType, int[][] blacklist) {
        int closestIndex = -1;
        int closestDistance = INF_DIST;
        for (int i = wells.length; i --> 0;) {
            int distance = wells[i].location.distanceSquaredTo(rc.getLocation());
            if (closestDistance > distance && wells[i].resourceType == resourceType && blacklist[wells[i].location.x][wells[i].location.y] <= rc.getRoundNum()) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        return closestIndex == -1 ? null : wells[closestIndex];
    }
    public static WellLocation pickNearest(RobotController rc, WellLocation[] wells) {
        int closestIndex = -1;
        int closestDistance = INF_DIST;
        for (int i = wells.length; i --> 0;) {
            int distance = wells[i].location.distanceSquaredTo(rc.getLocation());
            if (closestDistance > distance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        return closestIndex == -1 ? null : wells[closestIndex];
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

    public static Direction randomDirection(RobotController rc) {
        Direction dir;

        for (int i = 15; i --> 0;) {
            dir = DIRECTIONS[FastRandom.nextInt(DIRECTIONS.length)];
            if (rc.canMove(dir)) return dir;
        }
        return Direction.CENTER;
    }

    public static Direction directionToward(RobotController rc, MapLocation target) throws GameActionException {
        int bestIdx = 8, bestDist = INF_DIST * 100;
        for (int d = 9; d --> 0;) {
            Direction dir = Direction.allDirections()[d];
            if (!rc.canMove(dir)) continue;
            MapLocation immediateLocation = rc.getLocation().add(dir);
            MapInfo mapInfo = rc.senseMapInfo(immediateLocation);
            MapLocation result = immediateLocation.add(mapInfo.getCurrentDirection());
            int dist = (int)(10 * (result.distanceSquaredTo(target) * 10 + mapInfo.getCooldownMultiplier(rc.getTeam())));
            if (bestDist > dist) {
                bestDist = dist;
                bestIdx = d;
            }
        }
        return Direction.allDirections()[bestIdx];  // CENTER should always be allowed
    }

    public static Direction directionAway(RobotController rc, MapLocation target) throws GameActionException {
        int bestIdx = 8, bestDist = 0;
        for (int d = 9; d --> 0;) {
            Direction dir = Direction.allDirections()[d];
            if (!rc.canMove(dir)) continue;
            MapLocation immediateLocation = rc.getLocation().add(dir);
            MapInfo mapInfo = rc.senseMapInfo(immediateLocation);
            MapLocation result = immediateLocation.add(mapInfo.getCurrentDirection());
            int dist = (int)(10 * (result.distanceSquaredTo(target) * 10 + mapInfo.getCooldownMultiplier(rc.getTeam())));
            if (bestDist < dist) {
                bestDist = dist;
                bestIdx = d;
            }
        }
        return Direction.allDirections()[bestIdx];  // CENTER should always be allowed
    }

    public static Direction similarDirection(RobotController rc, Direction dir) throws GameActionException {
        if (directionOkay(rc, dir, dir)) return dir;
        if (directionOkay(rc, dir, dir.rotateLeft())) return dir.rotateLeft();
        if (directionOkay(rc, dir, dir.rotateRight())) return dir.rotateRight();
        if (directionOkay(rc, dir, dir.rotateLeft().rotateLeft())) return dir.rotateLeft().rotateLeft();
        return Direction.CENTER;
    }

    private static boolean directionOkay(RobotController rc, Direction targetDir, Direction actualDir) throws GameActionException {
        return rc.canMove(actualDir) && currentMatchesDirection(rc, rc.getLocation().add(actualDir), targetDir);
    }

    private static boolean currentMatchesDirection(RobotController rc, MapLocation location, Direction targetDir) throws GameActionException {
        Direction currentOppositeDir = rc.senseMapInfo(location).getCurrentDirection().opposite();
        return currentOppositeDir != targetDir && currentOppositeDir.rotateLeft() != targetDir && currentOppositeDir.rotateRight() != targetDir;
    }

    public static Direction directionTowardImmediate(RobotController rc, MapLocation target) throws GameActionException {
        return similarDirectionImmediate(rc, rc.getLocation().directionTo(target));
    }

    public static Direction similarDirectionImmediate(RobotController rc, Direction dir) {
        if (rc.canMove(dir)) return dir;
        if (rc.canMove(dir.rotateLeft())) return dir.rotateLeft();
        if (rc.canMove(dir.rotateRight())) return dir.rotateRight();
        if (rc.canMove(dir.rotateLeft().rotateLeft())) return dir.rotateLeft().rotateLeft();
        return Direction.CENTER;
    }

    public static boolean locationInArray(MapLocation[] array, MapLocation loc) {
        for (int i = array.length; i-- > 0; ) {
            if (array[i].equals(loc)) return true;
        }
        return false;
    }
    public static boolean locationInArray(MapLocation[] array, int n, MapLocation loc) {
        for (int i = n; i-- > 0; ) {
            if (array[i].equals(loc)) return true;
        }
        return false;
    }

    public static int cube(int x) {
        return x * x * x;
    }
}
