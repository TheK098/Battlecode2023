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
        queue[queueEnd++] = curLoc.translate(-minX, -minY);
        distance[visionLength][visionLength] = 0;
        startingDir[visionLength][visionLength] = Direction.CENTER;

        Direction closestDir = curLoc.directionTo(target);
        int closestDistance = INF_DIST;

        MapLocation trueNextLoc; int x, y, d, nx, ny, distanceRemaining, totalDistance;  // declare once at top to save bytecode
        while (queueStart < queueEnd) {
            debugBytecode("4.2");
            curLoc = queue[queueStart];
            x = curLoc.x; y = curLoc.y;

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
                nx = nextLoc.x; ny = nextLoc.y;

                if (nx >= 0 && nx < MAX_SIZE && ny >= 0 && ny < MAX_SIZE && distance[nx][ny] == INF_DIST) {
                    trueNextLoc = nextLoc.translate(minX, minY);
                    debugBytecode("4.3.1");
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
[A:CARRIER#12606@17] 4.1: 3563
[A:CARRIER#12606@17] 4.2: 3619
[A:CARRIER#12606@17] 4.3.0: 3690
[A:CARRIER#12606@17] 4.3.1: 3747
[A:CARRIER#12606@17] 4.3.0: 3785
[A:CARRIER#12606@17] 4.3.1: 3842
[A:CARRIER#12606@17] 4.3.0: 3924
[A:CARRIER#12606@17] 4.3.1: 3981
[A:CARRIER#12606@17] 4.3.0: 4063
[A:CARRIER#12606@17] 4.3.1: 4120
[A:CARRIER#12606@17] 4.3.0: 4202
[A:CARRIER#12606@17] 4.3.1: 4259
[A:CARRIER#12606@17] 4.3.0: 4297
[A:CARRIER#12606@17] 4.3.1: 4354
[A:CARRIER#12606@17] 4.3.0: 4392
[A:CARRIER#12606@17] 4.3.1: 4449
[A:CARRIER#12606@17] 4.3.0: 4487
[A:CARRIER#12606@17] 4.3.1: 4544
[A:CARRIER#12606@17] 4.2: 4587
[A:CARRIER#12606@17] 4.3.0: 4650
[A:CARRIER#12606@17] 4.3.1: 4707
[A:CARRIER#12606@17] 4.3.0: 4745
[A:CARRIER#12606@17] 4.3.1: 4802
[A:CARRIER#12606@17] 4.3.0: 4875
[A:CARRIER#12606@17] 4.3.1: 4932
[A:CARRIER#12606@17] 4.3.0: 5005
[A:CARRIER#12606@17] 4.3.0: 5059
[A:CARRIER#12606@17] 4.3.0: 5113
[A:CARRIER#12606@17] 4.3.0: 5167
[A:CARRIER#12606@17] 4.3.1: 5224
[A:CARRIER#12606@17] 4.3.0: 5262
[A:CARRIER#12606@17] 4.3.1: 5319
[A:CARRIER#12606@17] 4.2: 5362
[A:CARRIER#12606@17] 4.3.0: 5425
[A:CARRIER#12606@17] 4.3.0: 5479
[A:CARRIER#12606@17] 4.3.0: 5533
[A:CARRIER#12606@17] 4.3.1: 5590
[A:CARRIER#12606@17] 4.3.0: 5628
[A:CARRIER#12606@17] 4.3.1: 5685
[A:CARRIER#12606@17] 4.3.0: 5758
[A:CARRIER#12606@17] 4.3.1: 5815
[A:CARRIER#12606@17] 4.3.0: 5888
[A:CARRIER#12606@17] 4.3.0: 5942
[A:CARRIER#12606@17] 4.3.0: 5996
[A:CARRIER#12606@17] 4.2: 6055
[A:CARRIER#12606@17] 4.3.0: 6118
[A:CARRIER#12606@17] 4.3.0: 6172
[A:CARRIER#12606@17] 4.3.0: 6226
[A:CARRIER#12606@17] 4.3.0: 6280
[A:CARRIER#12606@17] 4.3.0: 6334
[A:CARRIER#12606@17] 4.3.1: 6391
[A:CARRIER#12606@17] 4.3.0: 6429
[A:CARRIER#12606@17] 4.3.1: 6486
[A:CARRIER#12606@17] 4.3.0: 6524
[A:CARRIER#12606@17] 4.3.1: 6581
[A:CARRIER#12606@17] 4.3.0: 6619
[A:CARRIER#12606@17] 4.2: 6678
[A:CARRIER#12606@17] 4.3.0: 6741
[A:CARRIER#12606@17] 4.3.1: 6798
[A:CARRIER#12606@17] 4.3.0: 6836
[A:CARRIER#12606@17] 4.3.1: 6893
[A:CARRIER#12606@17] 4.3.0: 6966
[A:CARRIER#12606@17] 4.3.1: 7023
[A:CARRIER#12606@17] 4.3.0: 7096
[A:CARRIER#12606@17] 4.3.0: 7150
[A:CARRIER#12606@17] 4.3.0: 7204
[A:CARRIER#12606@17] 4.3.0: 7258
[A:CARRIER#12606@17] 4.3.1: 7315
[A:CARRIER#12606@17] 4.3.0: 7353
[A:CARRIER#12606@17] 4.3.1: 7410
[A:CARRIER#12606@17] 4.2: 7453
[A:CARRIER#12606@17] 4.3.0: 7516
[A:CARRIER#12606@17] 4.3.0: 7570
[A:CARRIER#12606@17] 4.3.0: 7624
[A:CARRIER#12606@17] 4.3.1: 7681
[A:CARRIER#12606@17] 4.3.0: 7719
[A:CARRIER#12606@17] 4.3.1: 7776
[A:CARRIER#12606@17] 4.3.0: 7814
[A:CARRIER#12606@17] 4.3.0: 7868
[A:CARRIER#12606@17] 4.3.0: 7922
[A:CARRIER#12606@17] 4.3.0: 7976
[A:CARRIER#12606@17] 4.2: 8035
[A:CARRIER#12606@17] 4.3.0: 8098
[A:CARRIER#12606@17] 4.3.0: 8152
[A:CARRIER#12606@17] 4.3.1: 8209
[A:CARRIER#12606@17] 4.3.0: 8247
[A:CARRIER#12606@17] 4.3.1: 8304
[A:CARRIER#12606@17] 4.3.0: 8342
[A:CARRIER#12606@17] 4.3.1: 8399
[A:CARRIER#12606@17] 4.3.0: 8429
[A:CARRIER#12606@17] 4.3.1: 8486
[A:CARRIER#12606@17] 4.3.0: 8516
[A:CARRIER#12606@17] 4.3.0: 8570
[A:CARRIER#12606@17] 4.3.0: 8624
[A:CARRIER#12606@17] 4.2: 8683
[A:CARRIER#12606@17] 4.3.0: 8746
[A:CARRIER#12606@17] 4.3.0: 8800
[A:CARRIER#12606@17] 4.3.0: 8854
[A:CARRIER#12606@17] 4.3.1: 8911
[A:CARRIER#12606@17] 4.3.0: 8941
[A:CARRIER#12606@17] 4.3.1: 8998
[A:CARRIER#12606@17] 4.3.0: 9028
[A:CARRIER#12606@17] 4.3.1: 9085
[A:CARRIER#12606@17] 4.3.0: 9123
[A:CARRIER#12606@17] 4.3.1: 9180
[A:CARRIER#12606@17] 4.3.0: 9218
[A:CARRIER#12606@17] 4.3.1: 9275
[A:CARRIER#12606@17] 4.3.0: 9313
[A:CARRIER#12606@17] 4.2: 9372
[A:CARRIER#12606@17] 4.3.0: 9435
[A:CARRIER#12606@17] 4.3.1: 9492
[A:CARRIER#12606@17] 4.3.0: 9530
[A:CARRIER#12606@17] 4.3.1: 9587
[A:CARRIER#12606@17] 4.3.0: 9660
[A:CARRIER#12606@17] 4.3.1: 9717
[A:CARRIER#12606@17] 4.3.0: 9790
[A:CARRIER#12606@17] 4.3.0: 9844
[A:CARRIER#12606@17] 4.3.0: 9898
[A:CARRIER#12606@17] 4.3.0: 9952
[A:CARRIER#12606@17] 4.3.1: 10009
[A:CARRIER#12606@17] 4.3.0: 10047
[A:CARRIER#12606@17] 4.3.1: 10104
[A:CARRIER#12606@17] 4.2: 10147
[A:CARRIER#12606@17] 4.3.0: 10210
[A:CARRIER#12606@17] 4.3.0: 10264
[A:CARRIER#12606@17] 4.3.0: 10318
[A:CARRIER#12606@17] 4.3.1: 10375
[A:CARRIER#12606@17] 4.3.0: 10448
[A:CARRIER#12606@17] 4.3.1: 10505
[A:CARRIER#12606@17] 4.3.0: 10543
[A:CARRIER#12606@17] 4.3.1: 10600
[A:CARRIER#12606@17] 4.3.0: 10638
[A:CARRIER#12606@17] 4.3.0: 10692
[A:CARRIER#12606@17] 4.3.0: 10746
[A:CARRIER#12606@17] 4.2: 10805
[A:CARRIER#12606@17] 4.3.0: 10868
[A:CARRIER#12606@17] 4.3.0: 10907
[A:CARRIER#12606@17] 4.3.0: 10946
[A:CARRIER#12606@17] 4.3.0: 10985
[A:CARRIER#12606@17] 4.3.0: 11039
[A:CARRIER#12606@17] 4.3.0: 11093
[A:CARRIER#12606@17] 4.3.0: 11147
[A:CARRIER#12606@17] 4.3.1: 11204
[A:CARRIER#12606@17] 4.3.0: 11242
[A:CARRIER#12606@17] 4.3.1: 11299
[A:CARRIER#12606@17] 4.2: 11342
[A:CARRIER#12606@17] 4.3.0: 11405
[A:CARRIER#12606@17] 4.3.0: 11444
[A:CARRIER#12606@17] 4.3.0: 11483
[A:CARRIER#12606@17] 4.3.0: 11522
[A:CARRIER#12606@17] 4.3.0: 11576
[A:CARRIER#12606@17] 4.3.1: 11633
[A:CARRIER#12606@17] 4.3.0: 11671
[A:CARRIER#12606@17] 4.3.0: 11725
[A:CARRIER#12606@17] 4.3.0: 11779
[A:CARRIER#12606@17] 4.2: 11838
[A:CARRIER#12606@17] 4.3.0: 11901
[A:CARRIER#12606@17] 4.3.0: 11940
[A:CARRIER#12606@17] 4.3.0: 11979
[A:CARRIER#12606@17] 4.3.0: 12018
[A:CARRIER#12606@17] 4.3.1: 12075
[A:CARRIER#12606@17] 4.3.0: 12105
[A:CARRIER#12606@17] 4.3.1: 12162
[A:CARRIER#12606@17] 4.3.0: 12200
[A:CARRIER#12606@17] 4.3.1: 12257
[A:CARRIER#12606@17] 4.3.0: 12295
[A:CARRIER#12606@17] 4.3.0: 12349
[A:CARRIER#12606@17] 4.4: 12408
[A:CARRIER#12606@18] Started on round 17 but ended on round 18 with 1086 bytecode used
 */