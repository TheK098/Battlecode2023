package qp1_3_carrierlauncherspam.common;

import battlecode.common.*;
import qp1_3_carrierlauncherspam.utilities.FastRandom;

import java.util.Arrays;

import static qp1_3_carrierlauncherspam.utilities.Util.*;

public class Pathfinding {

    public static final int INF_DIST = 60 * 60 * 60 * 60;

    // TODO: assumes vision radius is always 20 (HQ/amplifier have 34)
    private static final int MAX_IN_RANGE = 80;  // theoretical max is 69, but we might have bytecode issues
    private static final int MAX_SIZE = 4 + 4 + 1;

    public static final Direction[] DIRECTIONS = {  // put diagonal directions first since they should go faster maybe?
            Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST,
            Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH
    };

    public static Direction moveWhileStayingAdjacent(RobotController rc, MapLocation target) {
        for (Direction dir: DIRECTIONS) {
            if (rc.canMove(dir) && (rc.getLocation().add(dir).isAdjacentTo(target) || rc.getLocation().add(dir).equals(target))) return dir;
        }
        return Direction.CENTER;
    }


    // declare arrays once whenever possible to save bytecode
    private static final Direction[][] startingDir = new Direction[MAX_SIZE][MAX_SIZE];
    private static final int[][] distance = new int[MAX_SIZE][MAX_SIZE];
//    private static final MapLocation[] locations = new MapLocation[MAX_IN_RANGE];
//    private static final int[][] cost = new int[MAX_IN_RANGE][DIRECTIONS.length];

    private static final MapLocation[] queue = new MapLocation[MAX_IN_RANGE];


    // BFS, only handles passability
    // TODO eventually optimize by declaring all variables outside loop
    public static Direction moveToward(RobotController rc, MapLocation target) throws GameActionException {
//        debugBytecode("4.0");
        Direction dir = directionToward(rc, target);
        if (dir != Direction.CENTER) {
            rc.setIndicatorString("Shortcut move " + dir);
            return dir;
        }

        int visionLength = (int)(Math.sqrt(rc.getType().visionRadiusSquared) + 0.00001);
        int minX = rc.getLocation().x - visionLength, minY = rc.getLocation().y - visionLength;

        // note that indexing is [x][y]
        for (int x = visionLength + visionLength + 1; x --> 0;) {
            Arrays.fill(distance[x], INF_DIST);
        }
//        debugBytecode("4.1");

        int queueStart = 0, queueEnd = 0;
        queue[queueEnd++] = rc.getLocation().translate(-minX, -minY);
        distance[rc.getLocation().x - minX][rc.getLocation().y - minY] = 0;
        startingDir[rc.getLocation().x - minX][rc.getLocation().y - minY] = Direction.CENTER;

        Direction closestDir = rc.getLocation().directionTo(target);
        int closestDistance = INF_DIST;

        MapLocation nextLoc; int x, y, d, nx, ny, distanceRemaining, totalDistance;  // declare once at top to save bytecode
        while (queueStart < queueEnd) {
//            debugBytecode("4.2");
            x = queue[queueStart].x; y = queue[queueStart].y;

            distanceRemaining = Math.max(
                    Math.abs(target.x - (x + minX)),
                    Math.abs(target.y - (y + minY))
            ) * 3 / 2;
            totalDistance = distance[x][y] + distanceRemaining;

            if (closestDistance > totalDistance) {
                closestDistance = totalDistance;
                closestDir = startingDir[x][y];
//                rc.setIndicatorString(closestDir + " " + (x + minX) + " " + (y + minY) + " " + target.x + " " + target.y + " " + totalDistance + " " + distanceRemaining + " " + queueStart);
            }

//            debugBytecode("4.3");
            for (d = 8; d --> 0;) {
                dir = Direction.allDirections()[d];
                nextLoc = queue[queueStart].add(dir);
                nx = nextLoc.x; ny = nextLoc.y;

                if (nx >= 0 && nx < MAX_SIZE && ny >= 0 && ny < MAX_SIZE && distance[nx][ny] == INF_DIST) {
                    MapLocation trueNextLoc = nextLoc.translate(minX, minY);
                    if (rc.canSenseLocation(trueNextLoc) && rc.sensePassability(trueNextLoc)) {
                        // check if we're processing starting location and trying to move to adjacent
                        if (startingDir[x][y] != Direction.CENTER) {
                            startingDir[nx][ny] = startingDir[x][y];
                            distance[nx][ny] = distance[x][y] + 1;
                            queue[queueEnd++] = nextLoc;
                        } else if (rc.canMove(dir)) {
                            startingDir[nx][ny] = dir;
                            distance[nx][ny] = distance[x][y] + 1;
                            queue[queueEnd++] = nextLoc;
                        }
                    }
                }
            }
            ++queueStart;
        }
//        debugBytecode("4.4");
        return closestDir;
    }

    private static final int EDGE_PUSH = 6;

    public static Direction spreadOut(RobotController rc, float weightX, float weightY, SpreadSettings settings) throws GameActionException {
        int x = rc.getLocation().x, y = rc.getLocation().y;
        // push away from edges
        weightX += (Math.max(0, cube(EDGE_PUSH - x)) - Math.max(0, cube(x - (rc.getMapWidth() - EDGE_PUSH - 1)))) / 10f;
        weightY += (Math.max(0, cube(EDGE_PUSH - y)) - Math.max(0, cube(y - (rc.getMapHeight() - EDGE_PUSH - 1)))) / 10f;

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(settings.ally_dist_cutoff, rc.getTeam());
        for (RobotInfo robot : nearbyRobots) {
            if (rc.getType() != robot.getType()) continue;
            // when spreading out anchoring carriers, we only care about other anchor carriers
            if (settings == SpreadSettings.CARRIER_ANCHOR && robot.getNumAnchors(Anchor.STANDARD) == 0) continue;

            // add one to avoid div by 0 when running out of bytecode
            int dist = rc.getLocation().distanceSquaredTo(robot.location) + 1;
            float dx = robot.location.x - x;
            float dy = robot.location.y - y;
            // subtract since we want to move away
            weightX -= settings.ally_dist_factor * dx / dist;
            weightY -= settings.ally_dist_factor * dy / dist;
        }
//        rc.setIndicatorString(weightX + " " + weightY);

        int finalDx = FastRandom.nextInt(settings.random_bound) - settings.random_cutoff > weightX ? -1 : 1;
        int finalDy = FastRandom.nextInt(settings.random_bound) - settings.random_cutoff > weightY ? -1 : 1;
        return similarDirection(rc, new MapLocation(0, 0).directionTo(new MapLocation(finalDx, finalDy)));
    }
}
