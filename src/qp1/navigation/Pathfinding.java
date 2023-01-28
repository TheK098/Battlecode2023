package qp1.navigation;

import battlecode.common.*;
import qp1.utilities.FastRandom;

import java.util.Arrays;

import static qp1.Carrier.debugBytecode;
import static qp1.utilities.Util.*;

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
        if (rc.canMove(Direction.NORTHWEST) && (rc.getLocation().add(Direction.NORTHWEST).isAdjacentTo(target) || rc.getLocation().add(Direction.NORTHWEST).equals(target)))
            return Direction.NORTHWEST;
        if (rc.canMove(Direction.NORTHEAST) && (rc.getLocation().add(Direction.NORTHEAST).isAdjacentTo(target) || rc.getLocation().add(Direction.NORTHEAST).equals(target)))
            return Direction.NORTHEAST;
        if (rc.canMove(Direction.SOUTHEAST) && (rc.getLocation().add(Direction.SOUTHEAST).isAdjacentTo(target) || rc.getLocation().add(Direction.SOUTHEAST).equals(target)))
            return Direction.SOUTHEAST;
        if (rc.canMove(Direction.SOUTHWEST) && (rc.getLocation().add(Direction.SOUTHWEST).isAdjacentTo(target) || rc.getLocation().add(Direction.SOUTHWEST).equals(target)))
            return Direction.SOUTHWEST;
        if (rc.canMove(Direction.NORTH) && (rc.getLocation().add(Direction.NORTH).isAdjacentTo(target) || rc.getLocation().add(Direction.NORTH).equals(target)))
            return Direction.NORTH;
        if (rc.canMove(Direction.EAST) && (rc.getLocation().add(Direction.EAST).isAdjacentTo(target) || rc.getLocation().add(Direction.EAST).equals(target)))
            return Direction.EAST;
        if (rc.canMove(Direction.SOUTH) && (rc.getLocation().add(Direction.SOUTH).isAdjacentTo(target) || rc.getLocation().add(Direction.SOUTH).equals(target)))
            return Direction.SOUTH;
        if (rc.canMove(Direction.WEST) && (rc.getLocation().add(Direction.WEST).isAdjacentTo(target) || rc.getLocation().add(Direction.WEST).equals(target)))
            return Direction.WEST;
        return Direction.CENTER;
    }


    // declare arrays once whenever possible to save bytecode
    private static final Direction[][] startingDir = new Direction[MAX_SIZE][MAX_SIZE];
    private static final int[][] distance = new int[MAX_SIZE][MAX_SIZE];
//    private static final MapLocation[] locations = new MapLocation[MAX_IN_RANGE];
//    private static final int[][] cost = new int[MAX_IN_RANGE][DIRECTIONS.length];

    private static final MapLocation[] queue = new MapLocation[MAX_IN_RANGE];


    // BFS, only handles passability
    public static Direction moveToward(RobotController rc, MapLocation target) throws GameActionException {
        debugBytecode("4.0");
        int visionLength = (int)(Math.sqrt(rc.getType().visionRadiusSquared) + 0.00001);

        Direction dir = directionToward(rc, target);
        MapLocation curLoc = rc.getLocation();
        MapLocation nextLoc = curLoc.add(dir);
        if (dir != Direction.CENTER && directionTowardHypothetical(rc, nextLoc, target) != Direction.CENTER) {
            rc.setIndicatorString("Shortcut move " + dir);
            return dir;
        }
        int x;
        for (x = MAX_SIZE; x --> 0;) {
            Arrays.fill(distance[x], INF_DIST);
        }

        debugBytecode("4.1");

        int queueStart = 0, queueEnd = 0;
        queue[queueEnd++] = curLoc;
        distance[visionLength][visionLength] = 0;
        startingDir[visionLength][visionLength] = Direction.CENTER;

        Direction closestDir = curLoc.directionTo(target);
        int closestDistance = INF_DIST;

        int minX = curLoc.x - visionLength, minY = curLoc.y - visionLength;

        int y, d, nx, ny, distanceRemaining, dist;   // declare once at top to save bytecode
        Direction curDir;
        while (queueStart < queueEnd) {
            debugBytecode("4.2");
            curLoc = queue[queueStart];
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
//                rc.setIndicatorString(closestDir + " " + (x + minX) + " " + (y + minY) + " " + target.x + " " + target.y + " " + totalDistance + " " + distanceRemaining + " " + queueStart);
            }

            for (d = 8; d --> 0;) {
                debugBytecode("4.3.0");
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
            ++queueStart;
        }
        debugBytecode("4.4");
        return closestDir;
    }

    private static final int EDGE_PUSH = 6;

    public static Direction spreadOut(RobotController rc, float weightX, float weightY, SpreadSettings settings) throws GameActionException {
        int x = rc.getLocation().x, y = rc.getLocation().y;
        // push away from edges
        weightX += (Math.max(0, cube(EDGE_PUSH - x)) - Math.max(0, cube(x - (rc.getMapWidth() - EDGE_PUSH - 1)))) / 10f;
        weightY += (Math.max(0, cube(EDGE_PUSH - y)) - Math.max(0, cube(y - (rc.getMapHeight() - EDGE_PUSH - 1)))) / 10f;

        RobotInfo[] robots = rc.senseNearbyRobots(settings.ally_dist_cutoff, rc.getTeam());
        int dist;
        for (int i = robots.length; i --> 0;) {
            if (rc.getType() != robots[i].getType()) continue;
            // when spreading out anchoring carriers, we only care about other anchor carriers
            if (settings == SpreadSettings.CARRIER_ANCHOR && robots[i].getNumAnchors(Anchor.STANDARD) == 0) continue;

            // add one to avoid div by 0 when running out of bytecode
            dist = rc.getLocation().distanceSquaredTo(robots[i].location) + 1;
            // subtract since we want to move away
            weightX -= settings.ally_dist_factor * (robots[i].location.x - x) / dist;
            weightY -= settings.ally_dist_factor * (robots[i].location.y - y) / dist;
        }
//        rc.setIndicatorString(weightX + " " + weightY);

        int finalDx = FastRandom.nextInt(settings.random_bound) - settings.random_cutoff > weightX ? -1 : 1;
        int finalDy = FastRandom.nextInt(settings.random_bound) - settings.random_cutoff > weightY ? -1 : 1;
        return similarDirection(rc, new MapLocation(0, 0).directionTo(new MapLocation(finalDx, finalDy)));
    }
}

/*
[A:CARRIER#12606@17] 4.0: 2590
[A:CARRIER#12606@17] 4.1: 4441
[A:CARRIER#12606@17] 4.2: 4501
[A:CARRIER#12606@17] 4.3.0: 4580
[A:CARRIER#12606@17] 4.3.0: 4644
[A:CARRIER#12606@17] 4.3.0: 4744
[A:CARRIER#12606@17] 4.3.0: 4844
[A:CARRIER#12606@17] 4.3.0: 4944
[A:CARRIER#12606@17] 4.3.0: 5008
[A:CARRIER#12606@17] 4.3.0: 5072
[A:CARRIER#12606@17] 4.3.0: 5136
[A:CARRIER#12606@17] 4.2: 5205
[A:CARRIER#12606@17] 4.3.0: 5278
[A:CARRIER#12606@17] 4.3.0: 5342
[A:CARRIER#12606@17] 4.3.0: 5429
[A:CARRIER#12606@17] 4.3.0: 5516
[A:CARRIER#12606@17] 4.3.0: 5572
[A:CARRIER#12606@17] 4.3.0: 5628
[A:CARRIER#12606@17] 4.3.0: 5684
[A:CARRIER#12606@17] 4.3.0: 5748
[A:CARRIER#12606@17] 4.2: 5817
[A:CARRIER#12606@17] 4.3.0: 5890
[A:CARRIER#12606@17] 4.3.0: 5946
[A:CARRIER#12606@17] 4.3.0: 6002
[A:CARRIER#12606@17] 4.3.0: 6066
[A:CARRIER#12606@17] 4.3.0: 6153
[A:CARRIER#12606@17] 4.3.0: 6240
[A:CARRIER#12606@17] 4.3.0: 6296
[A:CARRIER#12606@17] 4.3.0: 6352
[A:CARRIER#12606@17] 4.2: 6413
[A:CARRIER#12606@17] 4.3.0: 6486
[A:CARRIER#12606@17] 4.3.0: 6542
[A:CARRIER#12606@17] 4.3.0: 6598
[A:CARRIER#12606@17] 4.3.0: 6654
[A:CARRIER#12606@17] 4.3.0: 6710
[A:CARRIER#12606@17] 4.3.0: 6774
[A:CARRIER#12606@17] 4.3.0: 6838
[A:CARRIER#12606@17] 4.3.0: 6902
[A:CARRIER#12606@17] 4.2: 6963
[A:CARRIER#12606@17] 4.3.0: 7036
[A:CARRIER#12606@17] 4.3.0: 7100
[A:CARRIER#12606@17] 4.3.0: 7187
[A:CARRIER#12606@17] 4.3.0: 7274
[A:CARRIER#12606@17] 4.3.0: 7330
[A:CARRIER#12606@17] 4.3.0: 7386
[A:CARRIER#12606@17] 4.3.0: 7442
[A:CARRIER#12606@17] 4.3.0: 7506
[A:CARRIER#12606@17] 4.2: 7575
[A:CARRIER#12606@17] 4.3.0: 7648
[A:CARRIER#12606@17] 4.3.0: 7704
[A:CARRIER#12606@17] 4.3.0: 7760
[A:CARRIER#12606@17] 4.3.0: 7824
[A:CARRIER#12606@17] 4.3.0: 7888
[A:CARRIER#12606@17] 4.3.0: 7944
[A:CARRIER#12606@17] 4.3.0: 8000
[A:CARRIER#12606@17] 4.3.0: 8056
[A:CARRIER#12606@17] 4.2: 8117
[A:CARRIER#12606@17] 4.3.0: 8190
[A:CARRIER#12606@17] 4.3.0: 8246
[A:CARRIER#12606@17] 4.3.0: 8310
[A:CARRIER#12606@17] 4.3.0: 8374
[A:CARRIER#12606@17] 4.3.0: 8423
[A:CARRIER#12606@17] 4.3.0: 8472
[A:CARRIER#12606@17] 4.3.0: 8528
[A:CARRIER#12606@17] 4.3.0: 8584
[A:CARRIER#12606@17] 4.2: 8645
[A:CARRIER#12606@17] 4.3.0: 8718
[A:CARRIER#12606@17] 4.3.0: 8774
[A:CARRIER#12606@17] 4.3.0: 8830
[A:CARRIER#12606@17] 4.3.0: 8879
[A:CARRIER#12606@17] 4.3.0: 8928
[A:CARRIER#12606@17] 4.3.0: 8992
[A:CARRIER#12606@17] 4.3.0: 9056
[A:CARRIER#12606@17] 4.3.0: 9120
[A:CARRIER#12606@17] 4.2: 9181
[A:CARRIER#12606@17] 4.3.0: 9254
[A:CARRIER#12606@17] 4.3.0: 9318
[A:CARRIER#12606@17] 4.3.0: 9405
[A:CARRIER#12606@17] 4.3.0: 9492
[A:CARRIER#12606@17] 4.3.0: 9548
[A:CARRIER#12606@17] 4.3.0: 9604
[A:CARRIER#12606@17] 4.3.0: 9660
[A:CARRIER#12606@17] 4.3.0: 9724
[A:CARRIER#12606@17] 4.2: 9793
[A:CARRIER#12606@17] 4.3.0: 9866
[A:CARRIER#12606@17] 4.3.0: 9922
[A:CARRIER#12606@17] 4.3.0: 9978
[A:CARRIER#12606@17] 4.3.0: 10065
[A:CARRIER#12606@17] 4.3.0: 10129
[A:CARRIER#12606@17] 4.3.0: 10193
[A:CARRIER#12606@17] 4.3.0: 10249
[A:CARRIER#12606@17] 4.3.0: 10305
[A:CARRIER#12606@17] 4.2: 10366
[A:CARRIER#12606@17] 4.3.0: 10439
[A:CARRIER#12606@17] 4.3.0: 10488
[A:CARRIER#12606@17] 4.3.0: 10537
[A:CARRIER#12606@17] 4.3.0: 10586
[A:CARRIER#12606@17] 4.3.0: 10642
[A:CARRIER#12606@17] 4.3.0: 10698
[A:CARRIER#12606@17] 4.3.0: 10754
[A:CARRIER#12606@17] 4.3.0: 10818
[A:CARRIER#12606@17] 4.2: 10887
[A:CARRIER#12606@17] 4.3.0: 10960
[A:CARRIER#12606@17] 4.3.0: 11009
[A:CARRIER#12606@17] 4.3.0: 11058
[A:CARRIER#12606@17] 4.3.0: 11107
[A:CARRIER#12606@17] 4.3.0: 11163
[A:CARRIER#12606@17] 4.3.0: 11227
[A:CARRIER#12606@17] 4.3.0: 11283
[A:CARRIER#12606@17] 4.3.0: 11339
[A:CARRIER#12606@17] 4.2: 11400
[A:CARRIER#12606@17] 4.3.0: 11473
[A:CARRIER#12606@17] 4.3.0: 11522
[A:CARRIER#12606@17] 4.3.0: 11571
[A:CARRIER#12606@17] 4.3.0: 11620
[A:CARRIER#12606@17] 4.3.0: 11669
[A:CARRIER#12606@17] 4.3.0: 11733
[A:CARRIER#12606@17] 4.3.0: 11797
[A:CARRIER#12606@17] 4.3.0: 11853
[A:CARRIER#12606@17] 4.4: 11914
[A:CARRIER#12606@18] Started on round 17 but ended on round 18 with 552 bytecode used
 */