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
        while (queueStart < queueEnd) {
            debugBytecode("4.2");
            curLoc = queue[queueStart];
            x = curLoc.x - minX; y = curLoc.y - minY;

            distanceRemaining = Math.max(
                    Math.abs(target.x - (x + minX)),
                    Math.abs(target.y - (y + minY))
            ) * 5;

            dist = distance[x][y];

            if (closestDistance > dist + distanceRemaining) {
                closestDistance = dist + distanceRemaining;
                closestDir = startingDir[x][y];
//                rc.setIndicatorString(closestDir + " " + (x + minX) + " " + (y + minY) + " " + target.x + " " + target.y + " " + totalDistance + " " + distanceRemaining + " " + queueStart);
            }

            for (d = 8; d --> 0;) {
                debugBytecode("4.3.0");
                dir = Direction.allDirections()[d];
                nextLoc = curLoc.add(dir);
                nx = nextLoc.x - minX; ny = nextLoc.y - minY;

                if (rc.canSenseLocation(nextLoc) && distance[nx][ny] == INF_DIST && rc.sensePassability(nextLoc) ) {
                    debugBytecode("4.3.1");
                    // check if we're processing starting location and trying to move to adjacent
                    if (startingDir[x][y] != Direction.CENTER) {
                        startingDir[nx][ny] = startingDir[x][y];
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
[A:CARRIER#12606@17] 4.3.0: 4582
[A:CARRIER#12606@17] 4.3.0: 4646
[A:CARRIER#12606@17] 4.3.1: 4707
[A:CARRIER#12606@17] 4.3.0: 4769
[A:CARRIER#12606@17] 4.3.1: 4830
[A:CARRIER#12606@17] 4.3.0: 4892
[A:CARRIER#12606@17] 4.3.1: 4953
[A:CARRIER#12606@17] 4.3.0: 5015
[A:CARRIER#12606@17] 4.3.0: 5079
[A:CARRIER#12606@17] 4.3.0: 5143
[A:CARRIER#12606@17] 4.3.0: 5207
[A:CARRIER#12606@17] 4.2: 5276
[A:CARRIER#12606@17] 4.3.0: 5343
[A:CARRIER#12606@17] 4.3.0: 5407
[A:CARRIER#12606@17] 4.3.1: 5468
[A:CARRIER#12606@17] 4.3.0: 5521
[A:CARRIER#12606@17] 4.3.1: 5582
[A:CARRIER#12606@17] 4.3.0: 5635
[A:CARRIER#12606@17] 4.3.0: 5691
[A:CARRIER#12606@17] 4.3.0: 5747
[A:CARRIER#12606@17] 4.3.0: 5803
[A:CARRIER#12606@17] 4.3.0: 5867
[A:CARRIER#12606@17] 4.2: 5936
[A:CARRIER#12606@17] 4.3.0: 6003
[A:CARRIER#12606@17] 4.3.0: 6059
[A:CARRIER#12606@17] 4.3.0: 6115
[A:CARRIER#12606@17] 4.3.0: 6179
[A:CARRIER#12606@17] 4.3.1: 6240
[A:CARRIER#12606@17] 4.3.0: 6293
[A:CARRIER#12606@17] 4.3.1: 6354
[A:CARRIER#12606@17] 4.3.0: 6407
[A:CARRIER#12606@17] 4.3.0: 6463
[A:CARRIER#12606@17] 4.3.0: 6519
[A:CARRIER#12606@17] 4.2: 6580
[A:CARRIER#12606@17] 4.3.0: 6647
[A:CARRIER#12606@17] 4.3.0: 6703
[A:CARRIER#12606@17] 4.3.0: 6759
[A:CARRIER#12606@17] 4.3.0: 6815
[A:CARRIER#12606@17] 4.3.0: 6871
[A:CARRIER#12606@17] 4.3.0: 6935
[A:CARRIER#12606@17] 4.3.0: 6999
[A:CARRIER#12606@17] 4.3.0: 7063
[A:CARRIER#12606@17] 4.2: 7124
[A:CARRIER#12606@17] 4.3.0: 7191
[A:CARRIER#12606@17] 4.3.0: 7255
[A:CARRIER#12606@17] 4.3.1: 7316
[A:CARRIER#12606@17] 4.3.0: 7369
[A:CARRIER#12606@17] 4.3.1: 7430
[A:CARRIER#12606@17] 4.3.0: 7483
[A:CARRIER#12606@17] 4.3.0: 7539
[A:CARRIER#12606@17] 4.3.0: 7595
[A:CARRIER#12606@17] 4.3.0: 7651
[A:CARRIER#12606@17] 4.3.0: 7715
[A:CARRIER#12606@17] 4.2: 7784
[A:CARRIER#12606@17] 4.3.0: 7851
[A:CARRIER#12606@17] 4.3.0: 7907
[A:CARRIER#12606@17] 4.3.0: 7963
[A:CARRIER#12606@17] 4.3.0: 8027
[A:CARRIER#12606@17] 4.3.0: 8091
[A:CARRIER#12606@17] 4.3.0: 8147
[A:CARRIER#12606@17] 4.3.0: 8203
[A:CARRIER#12606@17] 4.3.0: 8259
[A:CARRIER#12606@17] 4.2: 8320
[A:CARRIER#12606@17] 4.3.0: 8387
[A:CARRIER#12606@17] 4.3.0: 8443
[A:CARRIER#12606@17] 4.3.0: 8507
[A:CARRIER#12606@17] 4.3.0: 8571
[A:CARRIER#12606@17] 4.3.0: 8620
[A:CARRIER#12606@17] 4.3.0: 8669
[A:CARRIER#12606@17] 4.3.0: 8725
[A:CARRIER#12606@17] 4.3.0: 8781
[A:CARRIER#12606@17] 4.2: 8842
[A:CARRIER#12606@17] 4.3.0: 8909
[A:CARRIER#12606@17] 4.3.0: 8965
[A:CARRIER#12606@17] 4.3.0: 9021
[A:CARRIER#12606@17] 4.3.0: 9070
[A:CARRIER#12606@17] 4.3.0: 9119
[A:CARRIER#12606@17] 4.3.0: 9183
[A:CARRIER#12606@17] 4.3.0: 9247
[A:CARRIER#12606@17] 4.3.0: 9311
[A:CARRIER#12606@17] 4.2: 9372
[A:CARRIER#12606@17] 4.3.0: 9439
[A:CARRIER#12606@17] 4.3.0: 9503
[A:CARRIER#12606@17] 4.3.1: 9564
[A:CARRIER#12606@17] 4.3.0: 9617
[A:CARRIER#12606@17] 4.3.1: 9678
[A:CARRIER#12606@17] 4.3.0: 9731
[A:CARRIER#12606@17] 4.3.0: 9787
[A:CARRIER#12606@17] 4.3.0: 9843
[A:CARRIER#12606@17] 4.3.0: 9899
[A:CARRIER#12606@17] 4.3.0: 9963
[A:CARRIER#12606@17] 4.2: 10032
[A:CARRIER#12606@17] 4.3.0: 10099
[A:CARRIER#12606@17] 4.3.0: 10155
[A:CARRIER#12606@17] 4.3.0: 10211
[A:CARRIER#12606@17] 4.3.1: 10272
[A:CARRIER#12606@17] 4.3.0: 10325
[A:CARRIER#12606@17] 4.3.0: 10389
[A:CARRIER#12606@17] 4.3.0: 10453
[A:CARRIER#12606@17] 4.3.0: 10509
[A:CARRIER#12606@17] 4.3.0: 10565
[A:CARRIER#12606@17] 4.2: 10626
[A:CARRIER#12606@17] 4.3.0: 10693
[A:CARRIER#12606@17] 4.3.0: 10742
[A:CARRIER#12606@17] 4.3.0: 10791
[A:CARRIER#12606@17] 4.3.0: 10840
[A:CARRIER#12606@17] 4.3.0: 10896
[A:CARRIER#12606@17] 4.3.0: 10952
[A:CARRIER#12606@17] 4.3.0: 11008
[A:CARRIER#12606@17] 4.3.0: 11072
[A:CARRIER#12606@17] 4.2: 11141
[A:CARRIER#12606@17] 4.3.0: 11208
[A:CARRIER#12606@17] 4.3.0: 11257
[A:CARRIER#12606@17] 4.3.0: 11306
[A:CARRIER#12606@17] 4.3.0: 11355
[A:CARRIER#12606@17] 4.3.0: 11411
[A:CARRIER#12606@17] 4.3.0: 11475
[A:CARRIER#12606@17] 4.3.0: 11531
[A:CARRIER#12606@17] 4.3.0: 11587
[A:CARRIER#12606@17] 4.2: 11648
[A:CARRIER#12606@17] 4.3.0: 11715
[A:CARRIER#12606@17] 4.3.0: 11764
[A:CARRIER#12606@17] 4.3.0: 11813
[A:CARRIER#12606@17] 4.3.0: 11862
[A:CARRIER#12606@17] 4.3.0: 11911
[A:CARRIER#12606@17] 4.3.0: 11975
[A:CARRIER#12606@17] 4.3.0: 12039
[A:CARRIER#12606@17] 4.3.0: 12095
[A:CARRIER#12606@17] 4.4: 12156
[A:CARRIER#12606@18] Started on round 17 but ended on round 18 with 834 bytecode used
 */