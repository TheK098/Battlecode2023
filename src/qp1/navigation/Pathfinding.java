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

        int x, y, d, nx, ny, distanceRemaining, totalDistance;  // declare once at top to save bytecode
        while (queueStart < queueEnd) {
            debugBytecode("4.2");
            curLoc = queue[queueStart];
            x = curLoc.x - minX; y = curLoc.y - minY;

            distanceRemaining = Math.max(
                    Math.abs(target.x - (x + minX)),
                    Math.abs(target.y - (y + minY))
            ) * 5;
            totalDistance = distance[x][y] + distanceRemaining;

            if (closestDistance > totalDistance) {
                closestDistance = totalDistance;
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
                        distance[nx][ny] = distance[x][y] + 1;
                        queue[queueEnd++] = nextLoc;
                    } else if (rc.canMove(dir)) {
                        startingDir[nx][ny] = dir;
                        distance[nx][ny] = distance[x][y] + 1;
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
[A:CARRIER#12606@17] 4.2: 4511
[A:CARRIER#12606@17] 4.3.0: 4582
[A:CARRIER#12606@17] 4.3.1: 4639
[A:CARRIER#12606@17] 4.3.0: 4677
[A:CARRIER#12606@17] 4.3.1: 4734
[A:CARRIER#12606@17] 4.3.0: 4816
[A:CARRIER#12606@17] 4.3.1: 4873
[A:CARRIER#12606@17] 4.3.0: 4955
[A:CARRIER#12606@17] 4.3.1: 5012
[A:CARRIER#12606@17] 4.3.0: 5094
[A:CARRIER#12606@17] 4.3.1: 5151
[A:CARRIER#12606@17] 4.3.0: 5189
[A:CARRIER#12606@17] 4.3.1: 5246
[A:CARRIER#12606@17] 4.3.0: 5284
[A:CARRIER#12606@17] 4.3.1: 5341
[A:CARRIER#12606@17] 4.3.0: 5379
[A:CARRIER#12606@17] 4.3.1: 5436
[A:CARRIER#12606@17] 4.2: 5479
[A:CARRIER#12606@17] 4.3.0: 5542
[A:CARRIER#12606@17] 4.3.1: 5599
[A:CARRIER#12606@17] 4.3.0: 5637
[A:CARRIER#12606@17] 4.3.1: 5694
[A:CARRIER#12606@17] 4.3.0: 5767
[A:CARRIER#12606@17] 4.3.1: 5824
[A:CARRIER#12606@17] 4.3.0: 5897
[A:CARRIER#12606@17] 4.3.0: 5951
[A:CARRIER#12606@17] 4.3.0: 6005
[A:CARRIER#12606@17] 4.3.0: 6059
[A:CARRIER#12606@17] 4.3.1: 6116
[A:CARRIER#12606@17] 4.3.0: 6154
[A:CARRIER#12606@17] 4.3.1: 6211
[A:CARRIER#12606@17] 4.2: 6254
[A:CARRIER#12606@17] 4.3.0: 6317
[A:CARRIER#12606@17] 4.3.0: 6371
[A:CARRIER#12606@17] 4.3.0: 6425
[A:CARRIER#12606@17] 4.3.1: 6482
[A:CARRIER#12606@17] 4.3.0: 6520
[A:CARRIER#12606@17] 4.3.1: 6577
[A:CARRIER#12606@17] 4.3.0: 6650
[A:CARRIER#12606@17] 4.3.1: 6707
[A:CARRIER#12606@17] 4.3.0: 6780
[A:CARRIER#12606@17] 4.3.0: 6834
[A:CARRIER#12606@17] 4.3.0: 6888
[A:CARRIER#12606@17] 4.2: 6947
[A:CARRIER#12606@17] 4.3.0: 7010
[A:CARRIER#12606@17] 4.3.0: 7064
[A:CARRIER#12606@17] 4.3.0: 7118
[A:CARRIER#12606@17] 4.3.0: 7172
[A:CARRIER#12606@17] 4.3.0: 7226
[A:CARRIER#12606@17] 4.3.1: 7283
[A:CARRIER#12606@17] 4.3.0: 7321
[A:CARRIER#12606@17] 4.3.1: 7378
[A:CARRIER#12606@17] 4.3.0: 7416
[A:CARRIER#12606@17] 4.3.1: 7473
[A:CARRIER#12606@17] 4.3.0: 7511
[A:CARRIER#12606@17] 4.2: 7570
[A:CARRIER#12606@17] 4.3.0: 7633
[A:CARRIER#12606@17] 4.3.1: 7690
[A:CARRIER#12606@17] 4.3.0: 7728
[A:CARRIER#12606@17] 4.3.1: 7785
[A:CARRIER#12606@17] 4.3.0: 7858
[A:CARRIER#12606@17] 4.3.1: 7915
[A:CARRIER#12606@17] 4.3.0: 7988
[A:CARRIER#12606@17] 4.3.0: 8042
[A:CARRIER#12606@17] 4.3.0: 8096
[A:CARRIER#12606@17] 4.3.0: 8150
[A:CARRIER#12606@17] 4.3.1: 8207
[A:CARRIER#12606@17] 4.3.0: 8245
[A:CARRIER#12606@17] 4.3.1: 8302
[A:CARRIER#12606@17] 4.2: 8345
[A:CARRIER#12606@17] 4.3.0: 8408
[A:CARRIER#12606@17] 4.3.0: 8462
[A:CARRIER#12606@17] 4.3.0: 8516
[A:CARRIER#12606@17] 4.3.1: 8573
[A:CARRIER#12606@17] 4.3.0: 8611
[A:CARRIER#12606@17] 4.3.1: 8668
[A:CARRIER#12606@17] 4.3.0: 8706
[A:CARRIER#12606@17] 4.3.0: 8760
[A:CARRIER#12606@17] 4.3.0: 8814
[A:CARRIER#12606@17] 4.3.0: 8868
[A:CARRIER#12606@17] 4.2: 8927
[A:CARRIER#12606@17] 4.3.0: 8990
[A:CARRIER#12606@17] 4.3.0: 9044
[A:CARRIER#12606@17] 4.3.1: 9101
[A:CARRIER#12606@17] 4.3.0: 9139
[A:CARRIER#12606@17] 4.3.1: 9196
[A:CARRIER#12606@17] 4.3.0: 9234
[A:CARRIER#12606@17] 4.3.1: 9291
[A:CARRIER#12606@17] 4.3.0: 9321
[A:CARRIER#12606@17] 4.3.1: 9378
[A:CARRIER#12606@17] 4.3.0: 9408
[A:CARRIER#12606@17] 4.3.0: 9462
[A:CARRIER#12606@17] 4.3.0: 9516
[A:CARRIER#12606@17] 4.2: 9575
[A:CARRIER#12606@17] 4.3.0: 9638
[A:CARRIER#12606@17] 4.3.0: 9692
[A:CARRIER#12606@17] 4.3.0: 9746
[A:CARRIER#12606@17] 4.3.1: 9803
[A:CARRIER#12606@17] 4.3.0: 9833
[A:CARRIER#12606@17] 4.3.1: 9890
[A:CARRIER#12606@17] 4.3.0: 9920
[A:CARRIER#12606@17] 4.3.1: 9977
[A:CARRIER#12606@17] 4.3.0: 10015
[A:CARRIER#12606@17] 4.3.1: 10072
[A:CARRIER#12606@17] 4.3.0: 10110
[A:CARRIER#12606@17] 4.3.1: 10167
[A:CARRIER#12606@17] 4.3.0: 10205
[A:CARRIER#12606@17] 4.2: 10264
[A:CARRIER#12606@17] 4.3.0: 10327
[A:CARRIER#12606@17] 4.3.1: 10384
[A:CARRIER#12606@17] 4.3.0: 10422
[A:CARRIER#12606@17] 4.3.1: 10479
[A:CARRIER#12606@17] 4.3.0: 10552
[A:CARRIER#12606@17] 4.3.1: 10609
[A:CARRIER#12606@17] 4.3.0: 10682
[A:CARRIER#12606@17] 4.3.0: 10736
[A:CARRIER#12606@17] 4.3.0: 10790
[A:CARRIER#12606@17] 4.3.0: 10844
[A:CARRIER#12606@17] 4.3.1: 10901
[A:CARRIER#12606@17] 4.3.0: 10939
[A:CARRIER#12606@17] 4.3.1: 10996
[A:CARRIER#12606@17] 4.2: 11039
[A:CARRIER#12606@17] 4.3.0: 11102
[A:CARRIER#12606@17] 4.3.0: 11156
[A:CARRIER#12606@17] 4.3.0: 11210
[A:CARRIER#12606@17] 4.3.1: 11267
[A:CARRIER#12606@17] 4.3.0: 11340
[A:CARRIER#12606@17] 4.3.1: 11397
[A:CARRIER#12606@17] 4.3.0: 11435
[A:CARRIER#12606@17] 4.3.1: 11492
[A:CARRIER#12606@17] 4.3.0: 11530
[A:CARRIER#12606@17] 4.3.0: 11584
[A:CARRIER#12606@17] 4.3.0: 11638
[A:CARRIER#12606@17] 4.2: 11697
[A:CARRIER#12606@17] 4.3.0: 11760
[A:CARRIER#12606@17] 4.3.0: 11799
[A:CARRIER#12606@17] 4.3.0: 11838
[A:CARRIER#12606@17] 4.3.0: 11877
[A:CARRIER#12606@17] 4.3.0: 11931
[A:CARRIER#12606@17] 4.3.0: 11985
[A:CARRIER#12606@17] 4.3.0: 12039
[A:CARRIER#12606@17] 4.3.1: 12096
[A:CARRIER#12606@17] 4.3.0: 12134
[A:CARRIER#12606@17] 4.3.1: 12191
[A:CARRIER#12606@17] 4.2: 12234
[A:CARRIER#12606@17] 4.3.0: 12297
[A:CARRIER#12606@17] 4.3.0: 12336
[A:CARRIER#12606@17] 4.3.0: 12375
[A:CARRIER#12606@17] 4.3.0: 12414
[A:CARRIER#12606@17] 4.3.0: 12468
[A:CARRIER#12606@18] 4.3.1: 25
[A:CARRIER#12606@18] 4.3.0: 63
[A:CARRIER#12606@18] 4.3.0: 117
[A:CARRIER#12606@18] 4.3.0: 171
[A:CARRIER#12606@18] 4.2: 230
[A:CARRIER#12606@18] 4.3.0: 293
[A:CARRIER#12606@18] 4.3.0: 332
[A:CARRIER#12606@18] 4.3.0: 371
[A:CARRIER#12606@18] 4.3.0: 410
[A:CARRIER#12606@18] 4.3.1: 467
[A:CARRIER#12606@18] 4.3.0: 497
[A:CARRIER#12606@18] 4.3.1: 554
[A:CARRIER#12606@18] 4.3.0: 592
[A:CARRIER#12606@18] 4.3.1: 649
[A:CARRIER#12606@18] 4.3.0: 687
[A:CARRIER#12606@18] 4.3.0: 741
[A:CARRIER#12606@18] 4.4: 800
[A:CARRIER#12606@18] Started on round 17 but ended on round 18 with 1978 bytecode used
 */