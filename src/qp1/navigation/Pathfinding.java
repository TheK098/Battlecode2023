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
        for (int x = visionLength + visionLength + 1; x --> 0;) {
            Arrays.fill(distance[x], INF_DIST);
        }

        int minX = curLoc.x - visionLength, minY = curLoc.y - visionLength;

        debugBytecode("4.1");

        int queueStart = 0, queueEnd = 0;
        queue[queueEnd++] = curLoc;
        distance[visionLength][visionLength] = 0;
        startingDir[visionLength][visionLength] = Direction.CENTER;

        Direction closestDir = curLoc.directionTo(target);
        int closestDistance = INF_DIST;

        int x, y, d, nx, ny, distanceRemaining, dist;   // declare once at top to save bytecode
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
[A:CARRIER#12606@17] 4.1: 4455
[A:CARRIER#12606@17] 4.2: 4505
[A:CARRIER#12606@17] 4.3.0: 4584
[A:CARRIER#12606@17] 4.3.0: 4648
[A:CARRIER#12606@17] 4.3.0: 4748
[A:CARRIER#12606@17] 4.3.0: 4848
[A:CARRIER#12606@17] 4.3.0: 4948
[A:CARRIER#12606@17] 4.3.0: 5012
[A:CARRIER#12606@17] 4.3.0: 5076
[A:CARRIER#12606@17] 4.3.0: 5140
[A:CARRIER#12606@17] 4.2: 5209
[A:CARRIER#12606@17] 4.3.0: 5282
[A:CARRIER#12606@17] 4.3.0: 5346
[A:CARRIER#12606@17] 4.3.0: 5433
[A:CARRIER#12606@17] 4.3.0: 5520
[A:CARRIER#12606@17] 4.3.0: 5576
[A:CARRIER#12606@17] 4.3.0: 5632
[A:CARRIER#12606@17] 4.3.0: 5688
[A:CARRIER#12606@17] 4.3.0: 5752
[A:CARRIER#12606@17] 4.2: 5821
[A:CARRIER#12606@17] 4.3.0: 5894
[A:CARRIER#12606@17] 4.3.0: 5950
[A:CARRIER#12606@17] 4.3.0: 6006
[A:CARRIER#12606@17] 4.3.0: 6070
[A:CARRIER#12606@17] 4.3.0: 6157
[A:CARRIER#12606@17] 4.3.0: 6244
[A:CARRIER#12606@17] 4.3.0: 6300
[A:CARRIER#12606@17] 4.3.0: 6356
[A:CARRIER#12606@17] 4.2: 6417
[A:CARRIER#12606@17] 4.3.0: 6490
[A:CARRIER#12606@17] 4.3.0: 6546
[A:CARRIER#12606@17] 4.3.0: 6602
[A:CARRIER#12606@17] 4.3.0: 6658
[A:CARRIER#12606@17] 4.3.0: 6714
[A:CARRIER#12606@17] 4.3.0: 6778
[A:CARRIER#12606@17] 4.3.0: 6842
[A:CARRIER#12606@17] 4.3.0: 6906
[A:CARRIER#12606@17] 4.2: 6967
[A:CARRIER#12606@17] 4.3.0: 7040
[A:CARRIER#12606@17] 4.3.0: 7104
[A:CARRIER#12606@17] 4.3.0: 7191
[A:CARRIER#12606@17] 4.3.0: 7278
[A:CARRIER#12606@17] 4.3.0: 7334
[A:CARRIER#12606@17] 4.3.0: 7390
[A:CARRIER#12606@17] 4.3.0: 7446
[A:CARRIER#12606@17] 4.3.0: 7510
[A:CARRIER#12606@17] 4.2: 7579
[A:CARRIER#12606@17] 4.3.0: 7652
[A:CARRIER#12606@17] 4.3.0: 7708
[A:CARRIER#12606@17] 4.3.0: 7764
[A:CARRIER#12606@17] 4.3.0: 7828
[A:CARRIER#12606@17] 4.3.0: 7892
[A:CARRIER#12606@17] 4.3.0: 7948
[A:CARRIER#12606@17] 4.3.0: 8004
[A:CARRIER#12606@17] 4.3.0: 8060
[A:CARRIER#12606@17] 4.2: 8121
[A:CARRIER#12606@17] 4.3.0: 8194
[A:CARRIER#12606@17] 4.3.0: 8250
[A:CARRIER#12606@17] 4.3.0: 8314
[A:CARRIER#12606@17] 4.3.0: 8378
[A:CARRIER#12606@17] 4.3.0: 8427
[A:CARRIER#12606@17] 4.3.0: 8476
[A:CARRIER#12606@17] 4.3.0: 8532
[A:CARRIER#12606@17] 4.3.0: 8588
[A:CARRIER#12606@17] 4.2: 8649
[A:CARRIER#12606@17] 4.3.0: 8722
[A:CARRIER#12606@17] 4.3.0: 8778
[A:CARRIER#12606@17] 4.3.0: 8834
[A:CARRIER#12606@17] 4.3.0: 8883
[A:CARRIER#12606@17] 4.3.0: 8932
[A:CARRIER#12606@17] 4.3.0: 8996
[A:CARRIER#12606@17] 4.3.0: 9060
[A:CARRIER#12606@17] 4.3.0: 9124
[A:CARRIER#12606@17] 4.2: 9185
[A:CARRIER#12606@17] 4.3.0: 9258
[A:CARRIER#12606@17] 4.3.0: 9322
[A:CARRIER#12606@17] 4.3.0: 9409
[A:CARRIER#12606@17] 4.3.0: 9496
[A:CARRIER#12606@17] 4.3.0: 9552
[A:CARRIER#12606@17] 4.3.0: 9608
[A:CARRIER#12606@17] 4.3.0: 9664
[A:CARRIER#12606@17] 4.3.0: 9728
[A:CARRIER#12606@17] 4.2: 9797
[A:CARRIER#12606@17] 4.3.0: 9870
[A:CARRIER#12606@17] 4.3.0: 9926
[A:CARRIER#12606@17] 4.3.0: 9982
[A:CARRIER#12606@17] 4.3.0: 10069
[A:CARRIER#12606@17] 4.3.0: 10133
[A:CARRIER#12606@17] 4.3.0: 10197
[A:CARRIER#12606@17] 4.3.0: 10253
[A:CARRIER#12606@17] 4.3.0: 10309
[A:CARRIER#12606@17] 4.2: 10370
[A:CARRIER#12606@17] 4.3.0: 10443
[A:CARRIER#12606@17] 4.3.0: 10492
[A:CARRIER#12606@17] 4.3.0: 10541
[A:CARRIER#12606@17] 4.3.0: 10590
[A:CARRIER#12606@17] 4.3.0: 10646
[A:CARRIER#12606@17] 4.3.0: 10702
[A:CARRIER#12606@17] 4.3.0: 10758
[A:CARRIER#12606@17] 4.3.0: 10822
[A:CARRIER#12606@17] 4.2: 10891
[A:CARRIER#12606@17] 4.3.0: 10964
[A:CARRIER#12606@17] 4.3.0: 11013
[A:CARRIER#12606@17] 4.3.0: 11062
[A:CARRIER#12606@17] 4.3.0: 11111
[A:CARRIER#12606@17] 4.3.0: 11167
[A:CARRIER#12606@17] 4.3.0: 11231
[A:CARRIER#12606@17] 4.3.0: 11287
[A:CARRIER#12606@17] 4.3.0: 11343
[A:CARRIER#12606@17] 4.2: 11404
[A:CARRIER#12606@17] 4.3.0: 11477
[A:CARRIER#12606@17] 4.3.0: 11526
[A:CARRIER#12606@17] 4.3.0: 11575
[A:CARRIER#12606@17] 4.3.0: 11624
[A:CARRIER#12606@17] 4.3.0: 11673
[A:CARRIER#12606@17] 4.3.0: 11737
[A:CARRIER#12606@17] 4.3.0: 11801
[A:CARRIER#12606@17] 4.3.0: 11857
[A:CARRIER#12606@17] 4.4: 11918
[A:CARRIER#12606@18] Started on round 17 but ended on round 18 with 556 bytecode used
 */