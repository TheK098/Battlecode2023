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
[A:CARRIER#12606@17] 4.2: 4505
[A:CARRIER#12606@17] 4.3.0: 4580
[A:CARRIER#12606@17] 4.3.0: 4644
[A:CARRIER#12606@17] 4.3.1: 4705
[A:CARRIER#12606@17] 4.3.0: 4771
[A:CARRIER#12606@17] 4.3.1: 4832
[A:CARRIER#12606@17] 4.3.0: 4898
[A:CARRIER#12606@17] 4.3.1: 4959
[A:CARRIER#12606@17] 4.3.0: 5025
[A:CARRIER#12606@17] 4.3.0: 5089
[A:CARRIER#12606@17] 4.3.0: 5153
[A:CARRIER#12606@17] 4.3.0: 5217
[A:CARRIER#12606@17] 4.2: 5286
[A:CARRIER#12606@17] 4.3.0: 5353
[A:CARRIER#12606@17] 4.3.0: 5417
[A:CARRIER#12606@17] 4.3.1: 5478
[A:CARRIER#12606@17] 4.3.0: 5535
[A:CARRIER#12606@17] 4.3.1: 5596
[A:CARRIER#12606@17] 4.3.0: 5653
[A:CARRIER#12606@17] 4.3.0: 5709
[A:CARRIER#12606@17] 4.3.0: 5765
[A:CARRIER#12606@17] 4.3.0: 5821
[A:CARRIER#12606@17] 4.3.0: 5885
[A:CARRIER#12606@17] 4.2: 5954
[A:CARRIER#12606@17] 4.3.0: 6021
[A:CARRIER#12606@17] 4.3.0: 6077
[A:CARRIER#12606@17] 4.3.0: 6133
[A:CARRIER#12606@17] 4.3.0: 6197
[A:CARRIER#12606@17] 4.3.1: 6258
[A:CARRIER#12606@17] 4.3.0: 6315
[A:CARRIER#12606@17] 4.3.1: 6376
[A:CARRIER#12606@17] 4.3.0: 6433
[A:CARRIER#12606@17] 4.3.0: 6489
[A:CARRIER#12606@17] 4.3.0: 6545
[A:CARRIER#12606@17] 4.2: 6606
[A:CARRIER#12606@17] 4.3.0: 6673
[A:CARRIER#12606@17] 4.3.0: 6729
[A:CARRIER#12606@17] 4.3.0: 6785
[A:CARRIER#12606@17] 4.3.0: 6841
[A:CARRIER#12606@17] 4.3.0: 6897
[A:CARRIER#12606@17] 4.3.0: 6961
[A:CARRIER#12606@17] 4.3.0: 7025
[A:CARRIER#12606@17] 4.3.0: 7089
[A:CARRIER#12606@17] 4.2: 7150
[A:CARRIER#12606@17] 4.3.0: 7217
[A:CARRIER#12606@17] 4.3.0: 7281
[A:CARRIER#12606@17] 4.3.1: 7342
[A:CARRIER#12606@17] 4.3.0: 7399
[A:CARRIER#12606@17] 4.3.1: 7460
[A:CARRIER#12606@17] 4.3.0: 7517
[A:CARRIER#12606@17] 4.3.0: 7573
[A:CARRIER#12606@17] 4.3.0: 7629
[A:CARRIER#12606@17] 4.3.0: 7685
[A:CARRIER#12606@17] 4.3.0: 7749
[A:CARRIER#12606@17] 4.2: 7818
[A:CARRIER#12606@17] 4.3.0: 7885
[A:CARRIER#12606@17] 4.3.0: 7941
[A:CARRIER#12606@17] 4.3.0: 7997
[A:CARRIER#12606@17] 4.3.0: 8061
[A:CARRIER#12606@17] 4.3.0: 8125
[A:CARRIER#12606@17] 4.3.0: 8181
[A:CARRIER#12606@17] 4.3.0: 8237
[A:CARRIER#12606@17] 4.3.0: 8293
[A:CARRIER#12606@17] 4.2: 8354
[A:CARRIER#12606@17] 4.3.0: 8421
[A:CARRIER#12606@17] 4.3.0: 8477
[A:CARRIER#12606@17] 4.3.0: 8541
[A:CARRIER#12606@17] 4.3.0: 8605
[A:CARRIER#12606@17] 4.3.0: 8654
[A:CARRIER#12606@17] 4.3.0: 8703
[A:CARRIER#12606@17] 4.3.0: 8759
[A:CARRIER#12606@17] 4.3.0: 8815
[A:CARRIER#12606@17] 4.2: 8876
[A:CARRIER#12606@17] 4.3.0: 8943
[A:CARRIER#12606@17] 4.3.0: 8999
[A:CARRIER#12606@17] 4.3.0: 9055
[A:CARRIER#12606@17] 4.3.0: 9104
[A:CARRIER#12606@17] 4.3.0: 9153
[A:CARRIER#12606@17] 4.3.0: 9217
[A:CARRIER#12606@17] 4.3.0: 9281
[A:CARRIER#12606@17] 4.3.0: 9345
[A:CARRIER#12606@17] 4.2: 9406
[A:CARRIER#12606@17] 4.3.0: 9473
[A:CARRIER#12606@17] 4.3.0: 9537
[A:CARRIER#12606@17] 4.3.1: 9598
[A:CARRIER#12606@17] 4.3.0: 9655
[A:CARRIER#12606@17] 4.3.1: 9716
[A:CARRIER#12606@17] 4.3.0: 9773
[A:CARRIER#12606@17] 4.3.0: 9829
[A:CARRIER#12606@17] 4.3.0: 9885
[A:CARRIER#12606@17] 4.3.0: 9941
[A:CARRIER#12606@17] 4.3.0: 10005
[A:CARRIER#12606@17] 4.2: 10074
[A:CARRIER#12606@17] 4.3.0: 10141
[A:CARRIER#12606@17] 4.3.0: 10197
[A:CARRIER#12606@17] 4.3.0: 10253
[A:CARRIER#12606@17] 4.3.1: 10314
[A:CARRIER#12606@17] 4.3.0: 10371
[A:CARRIER#12606@17] 4.3.0: 10435
[A:CARRIER#12606@17] 4.3.0: 10499
[A:CARRIER#12606@17] 4.3.0: 10555
[A:CARRIER#12606@17] 4.3.0: 10611
[A:CARRIER#12606@17] 4.2: 10672
[A:CARRIER#12606@17] 4.3.0: 10739
[A:CARRIER#12606@17] 4.3.0: 10788
[A:CARRIER#12606@17] 4.3.0: 10837
[A:CARRIER#12606@17] 4.3.0: 10886
[A:CARRIER#12606@17] 4.3.0: 10942
[A:CARRIER#12606@17] 4.3.0: 10998
[A:CARRIER#12606@17] 4.3.0: 11054
[A:CARRIER#12606@17] 4.3.0: 11118
[A:CARRIER#12606@17] 4.2: 11187
[A:CARRIER#12606@17] 4.3.0: 11254
[A:CARRIER#12606@17] 4.3.0: 11303
[A:CARRIER#12606@17] 4.3.0: 11352
[A:CARRIER#12606@17] 4.3.0: 11401
[A:CARRIER#12606@17] 4.3.0: 11457
[A:CARRIER#12606@17] 4.3.0: 11521
[A:CARRIER#12606@17] 4.3.0: 11577
[A:CARRIER#12606@17] 4.3.0: 11633
[A:CARRIER#12606@17] 4.2: 11694
[A:CARRIER#12606@17] 4.3.0: 11761
[A:CARRIER#12606@17] 4.3.0: 11810
[A:CARRIER#12606@17] 4.3.0: 11859
[A:CARRIER#12606@17] 4.3.0: 11908
[A:CARRIER#12606@17] 4.3.0: 11957
[A:CARRIER#12606@17] 4.3.0: 12021
[A:CARRIER#12606@17] 4.3.0: 12085
[A:CARRIER#12606@17] 4.3.0: 12141
[A:CARRIER#12606@17] 4.4: 12202
[A:CARRIER#12606@18] Started on round 17 but ended on round 18 with 880 bytecode used
 */