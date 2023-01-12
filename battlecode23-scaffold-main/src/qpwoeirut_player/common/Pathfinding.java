package qpwoeirut_player.common;

import battlecode.common.*;
import qpwoeirut_player.utilities.FastRandom;

import java.util.Arrays;

public class Pathfinding {

    public static final int INF_DIST = 60 * 60 * 60 * 60;

    // TODO: assumes vision radius is always 20 (HQ/amplifier have 34)
    private static final int MAX_IN_RANGE = 80;
    private static final int MAX_SIZE = 4 + 4 + 1;

    public static final Direction[] DIRECTIONS = {  // put diagonal directions first since they should go faster maybe?
            Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST,
            Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH
    };

    public static Direction moveWhileStayingAdjacent(RobotController rc, MapLocation target) throws GameActionException {
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
        int visionLength = (int)(Math.sqrt(rc.getType().visionRadiusSquared) + 0.00001);
        int minX = rc.getLocation().x - visionLength, minY = rc.getLocation().y - visionLength;

        // note that indexing is [x][y]
        for (int x = visionLength + visionLength + 1; x --> 0;) {
            Arrays.fill(distance[x], INF_DIST);
        }

        int queueStart = 0, queueEnd = 0;
        queue[queueEnd++] = rc.getLocation().translate(-minX, -minY);
        distance[rc.getLocation().x - minX][rc.getLocation().y - minY] = 0;
        startingDir[rc.getLocation().x - minX][rc.getLocation().y - minY] = Direction.CENTER;

        Direction closestDir = rc.getLocation().directionTo(target);
        int closestDistance = INF_DIST;
        while (queueStart < queueEnd) {
            int x = queue[queueStart].x, y = queue[queueStart].y;

            int distanceRemaining = Math.max(
                    Math.abs(target.x - (x + minX)),
                    Math.abs(target.y - (y + minY))
            ) * 3 / 2;
            int totalDistance = distance[x][y] + distanceRemaining;

            if (closestDistance > totalDistance) {
                closestDistance = totalDistance;
                closestDir = startingDir[x][y];
                rc.setIndicatorString(closestDir + " " + (x + minX) + " " + (y + minY) + " " + target.x + " " + target.y + " " + totalDistance + " " + distanceRemaining + " " + queueStart);
            }

            for (int d = 8; d --> 0;) {
                Direction dir = Direction.allDirections()[d];
                MapLocation nextLoc = queue[queueStart].add(dir);
                int nx = nextLoc.x, ny = nextLoc.y;

                if (nx >= 0 && nx < MAX_SIZE && ny >= 0 && ny < MAX_SIZE) {
                    if (distance[nx][ny] == INF_DIST) {
                        MapLocation trueNextLoc = nextLoc.translate(minX, minY);

                        if (rc.canSenseLocation(trueNextLoc) && rc.sensePassability(trueNextLoc)) {
                            // check if we're processing starting location and trying to move to adjacent
                            if (startingDir[x][y] == Direction.CENTER) {
                                if (rc.canMove(dir)) {
                                    startingDir[nx][ny] = dir;
                                    distance[nx][ny] = distance[x][y] + 1;
                                    queue[queueEnd++] = nextLoc;
                                }
                            } else {
                                startingDir[nx][ny] = startingDir[x][y];
                                distance[nx][ny] = distance[x][y] + 1;
                                queue[queueEnd++] = nextLoc;
                            }
                        }
                    }
                }
            }
            ++queueStart;
        }
        return closestDir;
    }

    public static Direction spreadOut(RobotController rc, int weightX, int weightY, SpreadSettings settings) throws GameActionException {
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(settings.ally_dist_cutoff, rc.getTeam());
        for (RobotInfo robot : nearbyRobots) {
            if (robot.type == rc.getType()) {
                int dist = rc.getLocation().distanceSquaredTo(robot.location);
                int dx = robot.location.x - rc.getLocation().x;
                int dy = robot.location.y - rc.getLocation().y;
                // subtract since we want to move away
                weightX -= dx * (settings.ally_dist_cutoff - dist) / settings.ally_dist_divisor;
                weightY -= dy * (settings.ally_dist_cutoff - dist) / settings.ally_dist_divisor;
            }
        }

        int finalDx = FastRandom.nextInt(settings.random_bound) - settings.random_cutoff > weightX ? -1 : 1;
        int finalDy = FastRandom.nextInt(settings.random_bound) - settings.random_cutoff > weightY ? -1 : 1;
        return new MapLocation(0, 0).directionTo(new MapLocation(finalDx, finalDy));
    }

 /*
 Abandoned for now
    // TODO: include effects of boosts/destabilization
    // TODO: include how crowded an area already is with bots
    // TODO: replace priority queue with bytecode-efficient impl
    public static Direction directionToTarget(RobotController rc, MapLocation target) throws GameActionException {
        if (rc.getID() == 12169) System.out.println("1: " + Clock.getBytecodeNum());
        // grab all visible locations that are passable
        MapInfo[] tilesInRange = rc.senseNearbyMapInfos();
        int n = tilesInRange.length;
        for (int i = n; i --> 0;) {
            locations[i] = tilesInRange[i].getMapLocation();
            passable[i] = rc.sensePassability(locations[i])
                    && previousLoc[0] != locations[i]
                    && previousLoc[1] != locations[i]
                    && previousLoc[2] != locations[i]
                    && previousLoc[3] != locations[i];
            for (int d = DIRECTIONS.length; d --> 0;) {
                cost[i][d] = (tilesInRange[i].getCurrentDirection() == DIRECTIONS[d]) ?
                        0 : (int) Math.ceil(REGULAR_COST * tilesInRange[i].getCooldownMuliplier(rc.getTeam()));
            }
        }

        // store current tile's cost
        MapInfo currentLocInfo = rc.senseMapInfo(rc.getLocation());
        for (int d = DIRECTIONS.length; d --> 0;) {
            cost[n][d] = (currentLocInfo.getCurrentDirection() == DIRECTIONS[d]) ?
                    0 : (int) Math.ceil(REGULAR_COST * currentLocInfo.getCooldownMuliplier(rc.getTeam()));
        }

        if (rc.getID() == 12169) System.out.println("2: " + Clock.getBytecodeNum());

        int visionSq = 0;
        int minX = rc.getLocation().x, maxX = rc.getLocation().x;
        int minY = rc.getLocation().y, maxY = rc.getLocation().y;
        for (int i = n; i --> 0;) {
            if (!passable[i]) continue;
            visionSq = Math.max(visionSq, rc.getLocation().distanceSquaredTo(locations[i]));
            minX = Math.min(minX, locations[i].x);
            minY = Math.min(minY, locations[i].y);
            maxX = Math.max(maxX, locations[i].x);
            maxY = Math.max(maxY, locations[i].y);
        }
        if (rc.getID() == 12169) System.out.println("3: " + Clock.getBytecodeNum());

        int vision = (int)(Math.sqrt(visionSq));
        int visionRange = Math.max(maxX - minX, maxY - minY) + 1;

        // note that indexing is [x][y]
        for (int r = visionRange; r --> 0;) {
            Arrays.fill(distance[r], INF_DIST);
        }

        if (rc.getID() == 12169) System.out.println("4: " + Clock.getBytecodeNum());

        for (int d = DIRECTIONS.length; d --> 0;) {
            Direction dir = DIRECTIONS[d];
            if (rc.canMove(dir)) {
                MapLocation newLoc = rc.getLocation().add(dir);
                if (newLoc.x < minX || newLoc.x > maxX || newLoc.y < minY || newLoc.y > maxY) continue;
                startingDir[newLoc.x - minX][newLoc.y - minY] = dir;
                distance[newLoc.x - minX][newLoc.y - minY] = cost[n][d];
            }
        }

        // regular bellman-ford loop uses # of nodes, but we should be able to cut down a bit
        for (int i = vision; i --> 0;)  {
            if (rc.getID() == 12169) System.out.println("5: " + Clock.getBytecodeNum());
            for (int u = n; u --> 0;) {
                if (!passable[u]) continue;
                int x = locations[u].x - minX;
                int y = locations[u].y - minY;
                if (distance[x][y] == INF_DIST) continue;
                for (int d = DIRECTIONS.length; d --> 0;) {
                    int nx = x + DIRECTIONS[d].dx;
                    int ny = y + DIRECTIONS[d].dy;
                    if (nx + minX < 0 || nx + minX >= rc.getMapWidth() || ny + minY < 0 || ny + minY >= rc.getMapHeight()) continue;
                    if (nx < 0 || nx >= visionRange || ny < 0 || ny >= visionRange) continue;
                    if (distance[nx][ny] > distance[x][y] + cost[u][d]) {
                        distance[nx][ny] = distance[x][y] + cost[u][d];
                        startingDir[nx][ny] = startingDir[x][y];
                    }
                }
            }
        }
        if (rc.getID() == 12169) System.out.println("6: " + Clock.getBytecodeNum());

        Direction closestDir = Direction.CENTER;
        int closestDistance = INF_DIST;
        MapLocation centerOfRange = new MapLocation(vision, vision);
        for (int u = n; u --> 0;) {
            if (!passable[u]) continue;
            int x = locations[u].x - minX;
            int y = locations[u].y - minY;
            // check the boundaries of the circle only
            if (centerOfRange.isWithinDistanceSquared(new MapLocation(x, y), visionSq * 4 / 5)) {
                continue;
            }
            // multiply by 15 as a quick estimate
            int distanceRemaining = Math.max(
                    Math.abs(target.x - (x + minX)),
                    Math.abs(target.y - (y + minY))
            ) * 15;
            int totalDistance = distance[x][y] + distanceRemaining;

            if (closestDistance > totalDistance) {
                closestDistance = totalDistance;
                closestDir = startingDir[x][y];
                rc.setIndicatorString((x + minX) + " " + (y + minY) + " " + target.x + " " + target.y + " " + totalDistance + " " + distanceRemaining);
            }
        }
        if (rc.getID() == 12169) System.out.println("7: " + Clock.getBytecodeNum());
        return closestDir;
    }

 */
}
