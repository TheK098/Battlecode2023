package qp1_10_tuningandamplifiers.navigation;

import battlecode.common.*;
import qp1_10_tuningandamplifiers.communications.Comms;
import qp1_10_tuningandamplifiers.communications.Comms.EnemySighting;
import qp1_10_tuningandamplifiers.utilities.FastRandom;

import java.util.Arrays;

import static qp1_10_tuningandamplifiers.utilities.Util.*;

public class Pathfinding {

    public static final int INF_DIST = 60 * 60 * 60 * 60;

    // TODO: assumes vision radius is always 20 (HQ/amplifier have 34)
    private static final int MAX_IN_RANGE = 80;  // theoretical max is 69, but we might have bytecode issues
    private static final int MAX_SIZE = 4 + 4 + 1;

    public static final Direction[] DIRECTIONS = {  // put diagonal directions first since they should go faster maybe?
            Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST,
            Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH
    };

    public static Direction moveWhileStayingAdjacent(RobotController rc, MapLocation target) throws GameActionException {
        MapLocation loc1 = rc.getLocation().add(Direction.NORTHWEST); MapInfo info1 = rc.onTheMap(loc1) ? rc.senseMapInfo(loc1) : null;
        MapLocation loc2 = rc.getLocation().add(Direction.NORTHEAST); MapInfo info2 = rc.onTheMap(loc2) ? rc.senseMapInfo(loc2) : null;
        MapLocation loc3 = rc.getLocation().add(Direction.SOUTHEAST); MapInfo info3 = rc.onTheMap(loc3) ? rc.senseMapInfo(loc3) : null;
        MapLocation loc4 = rc.getLocation().add(Direction.SOUTHWEST); MapInfo info4 = rc.onTheMap(loc4) ? rc.senseMapInfo(loc4) : null;
        MapLocation loc5 = rc.getLocation().add(Direction.NORTH);     MapInfo info5 = rc.onTheMap(loc5) ? rc.senseMapInfo(loc5) : null;
        MapLocation loc6 = rc.getLocation().add(Direction.EAST);      MapInfo info6 = rc.onTheMap(loc6) ? rc.senseMapInfo(loc6) : null;
        MapLocation loc7 = rc.getLocation().add(Direction.SOUTH);     MapInfo info7 = rc.onTheMap(loc7) ? rc.senseMapInfo(loc7) : null;
        MapLocation loc8 = rc.getLocation().add(Direction.WEST);      MapInfo info8 = rc.onTheMap(loc8) ? rc.senseMapInfo(loc8) : null;

        if (info1 != null && rc.canMove(Direction.NORTHWEST) && !info1.hasCloud() && adjacentOrEqual(loc1, target) && adjacentOrEqual(loc1.add(info1.getCurrentDirection()), target)) return Direction.NORTHWEST;
        if (info2 != null && rc.canMove(Direction.NORTHEAST) && !info2.hasCloud() && adjacentOrEqual(loc2, target) && adjacentOrEqual(loc2.add(info2.getCurrentDirection()), target)) return Direction.NORTHEAST;
        if (info3 != null && rc.canMove(Direction.SOUTHEAST) && !info3.hasCloud() && adjacentOrEqual(loc3, target) && adjacentOrEqual(loc3.add(info3.getCurrentDirection()), target)) return Direction.SOUTHEAST;
        if (info4 != null && rc.canMove(Direction.SOUTHWEST) && !info4.hasCloud() && adjacentOrEqual(loc4, target) && adjacentOrEqual(loc4.add(info4.getCurrentDirection()), target)) return Direction.SOUTHWEST;
        if (info5 != null && rc.canMove(Direction.NORTH)     && !info5.hasCloud() && adjacentOrEqual(loc5, target) && adjacentOrEqual(loc5.add(info5.getCurrentDirection()), target)) return Direction.NORTH;
        if (info6 != null && rc.canMove(Direction.EAST)      && !info6.hasCloud() && adjacentOrEqual(loc6, target) && adjacentOrEqual(loc6.add(info6.getCurrentDirection()), target)) return Direction.EAST;
        if (info7 != null && rc.canMove(Direction.SOUTH)     && !info7.hasCloud() && adjacentOrEqual(loc7, target) && adjacentOrEqual(loc7.add(info7.getCurrentDirection()), target)) return Direction.SOUTH;
        if (info8 != null && rc.canMove(Direction.WEST)      && !info8.hasCloud() && adjacentOrEqual(loc8, target) && adjacentOrEqual(loc8.add(info8.getCurrentDirection()), target)) return Direction.WEST;

        if (info1 != null && rc.canMove(Direction.NORTHWEST) && adjacentOrEqual(loc1, target) && adjacentOrEqual(loc1.add(info1.getCurrentDirection()), target)) return Direction.NORTHWEST;
        if (info2 != null && rc.canMove(Direction.NORTHEAST) && adjacentOrEqual(loc2, target) && adjacentOrEqual(loc2.add(info2.getCurrentDirection()), target)) return Direction.NORTHEAST;
        if (info3 != null && rc.canMove(Direction.SOUTHEAST) && adjacentOrEqual(loc3, target) && adjacentOrEqual(loc3.add(info3.getCurrentDirection()), target)) return Direction.SOUTHEAST;
        if (info4 != null && rc.canMove(Direction.SOUTHWEST) && adjacentOrEqual(loc4, target) && adjacentOrEqual(loc4.add(info4.getCurrentDirection()), target)) return Direction.SOUTHWEST;
        if (info5 != null && rc.canMove(Direction.NORTH)     && adjacentOrEqual(loc5, target) && adjacentOrEqual(loc5.add(info5.getCurrentDirection()), target)) return Direction.NORTH;
        if (info6 != null && rc.canMove(Direction.EAST)      && adjacentOrEqual(loc6, target) && adjacentOrEqual(loc6.add(info6.getCurrentDirection()), target)) return Direction.EAST;
        if (info7 != null && rc.canMove(Direction.SOUTH)     && adjacentOrEqual(loc7, target) && adjacentOrEqual(loc7.add(info7.getCurrentDirection()), target)) return Direction.SOUTH;
        if (info8 != null && rc.canMove(Direction.WEST)      && adjacentOrEqual(loc8, target) && adjacentOrEqual(loc8.add(info8.getCurrentDirection()), target)) return Direction.WEST;

        if (rc.canMove(Direction.NORTHWEST) && adjacentOrEqual(loc1, target)) return Direction.NORTHWEST;
        if (rc.canMove(Direction.NORTHEAST) && adjacentOrEqual(loc2, target)) return Direction.NORTHEAST;
        if (rc.canMove(Direction.SOUTHEAST) && adjacentOrEqual(loc3, target)) return Direction.SOUTHEAST;
        if (rc.canMove(Direction.SOUTHWEST) && adjacentOrEqual(loc4, target)) return Direction.SOUTHWEST;
        if (rc.canMove(Direction.NORTH)     && adjacentOrEqual(loc5, target)) return Direction.NORTH;
        if (rc.canMove(Direction.EAST)      && adjacentOrEqual(loc6, target)) return Direction.EAST;
        if (rc.canMove(Direction.SOUTH)     && adjacentOrEqual(loc7, target)) return Direction.SOUTH;
        if (rc.canMove(Direction.WEST)      && adjacentOrEqual(loc8, target)) return Direction.WEST;

        return Direction.CENTER;
    }


    // declare arrays once whenever possible to save bytecode
    private static final Direction[][] startingDir = new Direction[MAX_SIZE][MAX_SIZE];
    private static final int[][] distance = new int[MAX_SIZE][MAX_SIZE];
//    private static final MapLocation[] locations = new MapLocation[MAX_IN_RANGE];
//    private static final int[][] cost = new int[MAX_IN_RANGE][DIRECTIONS.length];

    private static final MapLocation[] queue = new MapLocation[MAX_IN_RANGE];


    // BFS, only handles passability
    // bytecodeLimit should be at least 650
    public static Direction moveToward(RobotController rc, MapLocation target, int bytecodeLimit) throws GameActionException {
//        debugBytecode("4.0");
        int visionLength = (int)(Math.sqrt(rc.getType().visionRadiusSquared) + 0.00001);

        Direction closestDir = directionToward(rc, target);
        MapLocation curLoc = rc.getLocation();
        MapLocation nextLoc = curLoc.add(closestDir);
        if (closestDir != Direction.CENTER && directionTowardHypothetical(rc, nextLoc, target) != Direction.CENTER) {
            return closestDir;
        }
        int x;
        for (x = MAX_SIZE; x --> 0;) {
            Arrays.fill(distance[x], INF_DIST);
        }

//        debugBytecode("4.1");

        int queueStart = 0, queueEnd = 0;
        queue[queueEnd++] = curLoc;
        distance[visionLength][visionLength] = 0;
        startingDir[visionLength][visionLength] = Direction.CENTER;

        closestDir = curLoc.directionTo(target);
        int closestDistance = INF_DIST;

        int minX = curLoc.x - visionLength, minY = curLoc.y - visionLength;

        int y, d, nx, ny, distanceRemaining, dist;   // declare once at top to save bytecode
        Direction dir, curDir;
        while (queueStart < queueEnd && Clock.getBytecodesLeft() >= bytecodeLimit) {  // one iteration can take around 600 bytecodes
//            debugBytecode("4.2");
            curLoc = queue[queueStart++];
            x = curLoc.x - minX; y = curLoc.y - minY;

            distanceRemaining = Math.max(
                    Math.abs(target.x - (x + minX)),
                    Math.abs(target.y - (y + minY))
            ) * 5;

            dist = distance[x][y];
            curDir = startingDir[x][y];

            if (closestDistance > dist + distanceRemaining) {
                closestDistance = dist + distanceRemaining;
                closestDir = curDir;
            }

            for (d = 8; d --> 0;) {
//                debugBytecode("4.3.0");
                dir = Direction.allDirections()[d];
                nextLoc = curLoc.add(dir);
                nx = nextLoc.x - minX; ny = nextLoc.y - minY;

                if (rc.canSenseLocation(nextLoc) && distance[nx][ny] == INF_DIST && rc.sensePassability(nextLoc)) {
                    // check if we're processing starting location and trying to move to adjacent
                    if (curDir != Direction.CENTER) {
                        startingDir[nx][ny] = curDir;
                        distance[nx][ny] = dist + 1;
                        queue[queueEnd++] = nextLoc;
                    } else if (rc.canMove(dir)) {
                        startingDir[nx][ny] = dir;
                        distance[nx][ny] = dist + 1;
                        queue[queueEnd++] = nextLoc;
                    }
                }
            }
        }
//        debugBytecode("4.4");
        return closestDir;
    }

    private static final int EDGE_PUSH = 6;

    public static Direction spreadOut(RobotController rc, float weightX, float weightY, SpreadSettings settings) throws GameActionException {
        MapLocation currentLocation = rc.getLocation();
        float x = currentLocation.x, y = currentLocation.y;
        // push away from edges
        weightX += (Math.max(0, cube(EDGE_PUSH - x)) - Math.max(0, cube(x - (rc.getMapWidth() - EDGE_PUSH - 1)))) / 10f;
        weightY += (Math.max(0, cube(EDGE_PUSH - y)) - Math.max(0, cube(y - (rc.getMapHeight() - EDGE_PUSH - 1)))) / 10f;

        RobotInfo[] robots = rc.senseNearbyRobots(settings.ally_dist_cutoff, rc.getTeam());
        int dist;
        float allyWeightX = 0, allyWeightY = 0;
        MapLocation loc;
        boolean notAnchor = settings != SpreadSettings.CARRIER_ANCHOR;
        for (int i = robots.length; i --> 0;) {
            // when spreading out anchoring carriers, we only care about other anchor carriers
            if (rc.getType() == robots[i].getType() && (notAnchor || robots[i].getNumAnchors(Anchor.STANDARD) > 0)) {
                loc = robots[i].location;
                // add one to avoid div by 0 when running out of bytecode
                dist = currentLocation.distanceSquaredTo(loc) + 1;
                // subtract since we want to move away
                allyWeightX -= (loc.x - x) / dist;
                allyWeightY -= (loc.y - y) / dist;
            }
        }
        weightX += allyWeightX * settings.ally_dist_factor;
        weightY += allyWeightY * settings.ally_dist_factor;

        // searching carriers should avoid enemy sightings
        if (settings == SpreadSettings.CARRIER_SEARCHING) {
            float enemyWeightX = 0, enemyWeightY = 0;
            EnemySighting[] sightings = Comms.getEnemySightings(rc);
            for (int i = sightings.length; i --> 0;) {
                loc = sightings[i].location;
                dist = currentLocation.distanceSquaredTo(loc) + 1;
                enemyWeightX -= (loc.x - x) / dist;
                enemyWeightY -= (loc.y - y) / dist;
            }
            weightX += enemyWeightX * 40;
            weightY += enemyWeightY * 40;
        }

        int finalDx = FastRandom.nextInt(settings.random_bound) - settings.random_cutoff > weightX ? -1 : 1;
        int finalDy = FastRandom.nextInt(settings.random_bound) - settings.random_cutoff > weightY ? -1 : 1;
        return similarDirection(rc, new MapLocation(0, 0).directionTo(new MapLocation(finalDx, finalDy)));
    }
}
