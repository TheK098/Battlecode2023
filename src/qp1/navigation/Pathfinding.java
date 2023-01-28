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

                if (rc.canSenseLocation(nextLoc) && distance[nx][ny] == INF_DIST && rc.sensePassability(nextLoc) ) {
                    debugBytecode("4.3.1");
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
[A:CARRIER#12606@17] 4.3.1: 4709
[A:CARRIER#12606@17] 4.3.0: 4767
[A:CARRIER#12606@17] 4.3.1: 4828
[A:CARRIER#12606@17] 4.3.0: 4886
[A:CARRIER#12606@17] 4.3.1: 4947
[A:CARRIER#12606@17] 4.3.0: 5005
[A:CARRIER#12606@17] 4.3.0: 5069
[A:CARRIER#12606@17] 4.3.0: 5133
[A:CARRIER#12606@17] 4.3.0: 5197
[A:CARRIER#12606@17] 4.2: 5266
[A:CARRIER#12606@17] 4.3.0: 5339
[A:CARRIER#12606@17] 4.3.0: 5403
[A:CARRIER#12606@17] 4.3.1: 5464
[A:CARRIER#12606@17] 4.3.0: 5509
[A:CARRIER#12606@17] 4.3.1: 5570
[A:CARRIER#12606@17] 4.3.0: 5615
[A:CARRIER#12606@17] 4.3.0: 5671
[A:CARRIER#12606@17] 4.3.0: 5727
[A:CARRIER#12606@17] 4.3.0: 5783
[A:CARRIER#12606@17] 4.3.0: 5847
[A:CARRIER#12606@17] 4.2: 5916
[A:CARRIER#12606@17] 4.3.0: 5989
[A:CARRIER#12606@17] 4.3.0: 6045
[A:CARRIER#12606@17] 4.3.0: 6101
[A:CARRIER#12606@17] 4.3.0: 6165
[A:CARRIER#12606@17] 4.3.1: 6226
[A:CARRIER#12606@17] 4.3.0: 6271
[A:CARRIER#12606@17] 4.3.1: 6332
[A:CARRIER#12606@17] 4.3.0: 6377
[A:CARRIER#12606@17] 4.3.0: 6433
[A:CARRIER#12606@17] 4.3.0: 6489
[A:CARRIER#12606@17] 4.2: 6550
[A:CARRIER#12606@17] 4.3.0: 6623
[A:CARRIER#12606@17] 4.3.0: 6679
[A:CARRIER#12606@17] 4.3.0: 6735
[A:CARRIER#12606@17] 4.3.0: 6791
[A:CARRIER#12606@17] 4.3.0: 6847
[A:CARRIER#12606@17] 4.3.0: 6911
[A:CARRIER#12606@17] 4.3.0: 6975
[A:CARRIER#12606@17] 4.3.0: 7039
[A:CARRIER#12606@17] 4.2: 7100
[A:CARRIER#12606@17] 4.3.0: 7173
[A:CARRIER#12606@17] 4.3.0: 7237
[A:CARRIER#12606@17] 4.3.1: 7298
[A:CARRIER#12606@17] 4.3.0: 7343
[A:CARRIER#12606@17] 4.3.1: 7404
[A:CARRIER#12606@17] 4.3.0: 7449
[A:CARRIER#12606@17] 4.3.0: 7505
[A:CARRIER#12606@17] 4.3.0: 7561
[A:CARRIER#12606@17] 4.3.0: 7617
[A:CARRIER#12606@17] 4.3.0: 7681
[A:CARRIER#12606@17] 4.2: 7750
[A:CARRIER#12606@17] 4.3.0: 7823
[A:CARRIER#12606@17] 4.3.0: 7879
[A:CARRIER#12606@17] 4.3.0: 7935
[A:CARRIER#12606@17] 4.3.0: 7999
[A:CARRIER#12606@17] 4.3.0: 8063
[A:CARRIER#12606@17] 4.3.0: 8119
[A:CARRIER#12606@17] 4.3.0: 8175
[A:CARRIER#12606@17] 4.3.0: 8231
[A:CARRIER#12606@17] 4.2: 8292
[A:CARRIER#12606@17] 4.3.0: 8365
[A:CARRIER#12606@17] 4.3.0: 8421
[A:CARRIER#12606@17] 4.3.0: 8485
[A:CARRIER#12606@17] 4.3.0: 8549
[A:CARRIER#12606@17] 4.3.0: 8598
[A:CARRIER#12606@17] 4.3.0: 8647
[A:CARRIER#12606@17] 4.3.0: 8703
[A:CARRIER#12606@17] 4.3.0: 8759
[A:CARRIER#12606@17] 4.2: 8820
[A:CARRIER#12606@17] 4.3.0: 8893
[A:CARRIER#12606@17] 4.3.0: 8949
[A:CARRIER#12606@17] 4.3.0: 9005
[A:CARRIER#12606@17] 4.3.0: 9054
[A:CARRIER#12606@17] 4.3.0: 9103
[A:CARRIER#12606@17] 4.3.0: 9167
[A:CARRIER#12606@17] 4.3.0: 9231
[A:CARRIER#12606@17] 4.3.0: 9295
[A:CARRIER#12606@17] 4.2: 9356
[A:CARRIER#12606@17] 4.3.0: 9429
[A:CARRIER#12606@17] 4.3.0: 9493
[A:CARRIER#12606@17] 4.3.1: 9554
[A:CARRIER#12606@17] 4.3.0: 9599
[A:CARRIER#12606@17] 4.3.1: 9660
[A:CARRIER#12606@17] 4.3.0: 9705
[A:CARRIER#12606@17] 4.3.0: 9761
[A:CARRIER#12606@17] 4.3.0: 9817
[A:CARRIER#12606@17] 4.3.0: 9873
[A:CARRIER#12606@17] 4.3.0: 9937
[A:CARRIER#12606@17] 4.2: 10006
[A:CARRIER#12606@17] 4.3.0: 10079
[A:CARRIER#12606@17] 4.3.0: 10135
[A:CARRIER#12606@17] 4.3.0: 10191
[A:CARRIER#12606@17] 4.3.1: 10252
[A:CARRIER#12606@17] 4.3.0: 10297
[A:CARRIER#12606@17] 4.3.0: 10361
[A:CARRIER#12606@17] 4.3.0: 10425
[A:CARRIER#12606@17] 4.3.0: 10481
[A:CARRIER#12606@17] 4.3.0: 10537
[A:CARRIER#12606@17] 4.2: 10598
[A:CARRIER#12606@17] 4.3.0: 10671
[A:CARRIER#12606@17] 4.3.0: 10720
[A:CARRIER#12606@17] 4.3.0: 10769
[A:CARRIER#12606@17] 4.3.0: 10818
[A:CARRIER#12606@17] 4.3.0: 10874
[A:CARRIER#12606@17] 4.3.0: 10930
[A:CARRIER#12606@17] 4.3.0: 10986
[A:CARRIER#12606@17] 4.3.0: 11050
[A:CARRIER#12606@17] 4.2: 11119
[A:CARRIER#12606@17] 4.3.0: 11192
[A:CARRIER#12606@17] 4.3.0: 11241
[A:CARRIER#12606@17] 4.3.0: 11290
[A:CARRIER#12606@17] 4.3.0: 11339
[A:CARRIER#12606@17] 4.3.0: 11395
[A:CARRIER#12606@17] 4.3.0: 11459
[A:CARRIER#12606@17] 4.3.0: 11515
[A:CARRIER#12606@17] 4.3.0: 11571
[A:CARRIER#12606@17] 4.2: 11632
[A:CARRIER#12606@17] 4.3.0: 11705
[A:CARRIER#12606@17] 4.3.0: 11754
[A:CARRIER#12606@17] 4.3.0: 11803
[A:CARRIER#12606@17] 4.3.0: 11852
[A:CARRIER#12606@17] 4.3.0: 11901
[A:CARRIER#12606@17] 4.3.0: 11965
[A:CARRIER#12606@17] 4.3.0: 12029
[A:CARRIER#12606@17] 4.3.0: 12085
[A:CARRIER#12606@17] 4.4: 12146
[A:CARRIER#12606@18] Started on round 17 but ended on round 18 with 824 bytecode used
 */