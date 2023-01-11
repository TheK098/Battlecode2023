package qpwoeirut_player.common;

import battlecode.common.*;
import qpwoeirut_player.utilities.FastRandom;

public class Pathfinding {

    public static final int INF_DIST = 60 * 60 * 60 * 60;
    public static final int REGULAR_COST = 10;
    public static final int CLOUD_COST = 12;

    private static final int MAX_IN_RANGE = 120;

    public static final Direction[] DIRECTIONS = {  // put diagonal directions first since they should go faster maybe?
            Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST,
            Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH
    };

    public static boolean locationInArray(MapLocation[] array, MapLocation loc) {
        for (int i = array.length; i --> 0;) {
            if (array[i].equals(loc)) return true;
        }
        return false;
    }

    private static final int TARGET_DISTANCE_CUTOFF = 100;
    private static final int TARGET_DISTANCE_DIVISOR = 5;
    private static final int ALLY_DISTANCE_CUTOFF = 30;
    private static final int ALLY_DISTANCE_DIVISOR = 10;
    private static final int RANDOM_CUTOFF = 50;

    /**
     * Move the bot away from other allied bots of the same type, with a stronger attraction towards a target
     * @param target location that bot wants to go to
     * @return recommended direction
     */
    public static Direction spreadOut(RobotController rc, MapLocation target) throws GameActionException {
        int weightX = 0;
        int weightY = 0;
        if (target != null) {
            int distanceToTarget = rc.getLocation().distanceSquaredTo(target);
            if (distanceToTarget < TARGET_DISTANCE_CUTOFF) {
                int dx = target.x - rc.getLocation().x;
                int dy = target.y - rc.getLocation().y;
                weightX = dx * (TARGET_DISTANCE_CUTOFF - distanceToTarget) / TARGET_DISTANCE_DIVISOR;
                weightY = dy * (TARGET_DISTANCE_CUTOFF - distanceToTarget) / TARGET_DISTANCE_DIVISOR;
            }
        }
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(ALLY_DISTANCE_CUTOFF, rc.getTeam());
        for (RobotInfo robot: nearbyRobots) {
            if (robot.type == rc.getType()) {
                int dist = rc.getLocation().distanceSquaredTo(robot.location);
                int dx = robot.location.x - rc.getLocation().x;
                int dy = robot.location.y - rc.getLocation().y;
                // subtract since we want to move away
                weightX -= dx * (ALLY_DISTANCE_CUTOFF - dist) / ALLY_DISTANCE_DIVISOR;
                weightY -= dy * (ALLY_DISTANCE_CUTOFF - dist) / ALLY_DISTANCE_DIVISOR;
            }
        }

        int finalDx = FastRandom.nextInt(RANDOM_CUTOFF + RANDOM_CUTOFF + 1) - RANDOM_CUTOFF > weightX ? -1 : 1;
        int finalDy = FastRandom.nextInt(RANDOM_CUTOFF + RANDOM_CUTOFF + 1) - RANDOM_CUTOFF > weightY ? -1 : 1;
        return new MapLocation(0, 0).directionTo(new MapLocation(finalDx, finalDy));
    }

    // declare arrays once whenever possible to save bytecode
    private static final Direction[][] startingDir = new Direction[15][15];
    private static final int[][] distance = new int[15][15];
    private static final boolean[] passable = new boolean[MAX_IN_RANGE];
    private static final MapLocation[] locations = new MapLocation[MAX_IN_RANGE];
    private static final int[][] cost = new int[MAX_IN_RANGE][DIRECTIONS.length];
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
