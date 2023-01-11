package qpwoeirut_player.common;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Arrays;

public class Pathfinding {

    public static final int INF_DIST = 60 * 60 * 60 * 60;
    public static final int REGULAR_COST = 10;
    public static final int CLOUD_COST = 12;

    public static final Direction[] DIRECTIONS = {  // put diagonal directions first since they should go faster maybe?
            Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST,
            Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH
    };

    public static boolean locationInArray(MapLocation[] locations, MapLocation loc) {
        for (int i = locations.length; i --> 0;) {
            if (locations[i].equals(loc)) return true;
        }
        return false;
    }

    private static class Item implements Comparable<Item> {
        int dist;
        Direction dir;
        MapLocation loc;

        public Item(int dist, Direction dir, MapLocation loc) {
            this.dist = dist;
            this.dir = dir;
            this.loc = loc;
        }

        @Override
        public int compareTo(Item o) {
            return Integer.compare(
                    // 60 -> map size, 9 -> # of directions, including center
                    dist * 60 * 60 * 9 + loc.x * 60 * 9 + loc.y * 9 + dir.ordinal(),
                    o.dist * 60 * 60 * 9 + o.loc.x * 60 * 9 + o.loc.y * 9 + o.dir.ordinal()
            );
        }
    }

    // TODO: include effects of boosts/destabilization
    // TODO: include how crowded an area already is with bots
    // TODO: replace priority queue with bytecode-efficient impl
    public static Direction directionToTarget(RobotController rc, MapLocation target, int visionSq) throws GameActionException {
        int vision = (int)(Math.sqrt(visionSq));
        int visionRange = vision + vision + 1;

//        return rc.getLocation().directionTo(target);
        // note that indexing is [x][y]
        int offsetX = rc.getLocation().x - vision;
        int offsetY = rc.getLocation().y - vision;

        Direction[][] startingDir = new Direction[visionRange][visionRange];
        int[][] distance = new int[visionRange][visionRange];
        for (int r = visionRange; r --> 0;) {
            Arrays.fill(distance[r], INF_DIST);
        }
        for (Direction dir : DIRECTIONS) {
            if (rc.canMove(dir)) {
                int dist = REGULAR_COST;

                // FIXME: waiting until API for this is released
                // int dist = determineCost(rc.getLocationType(rc.getLocation(), dir));

                MapLocation newLoc = rc.getLocation().add(dir);
                startingDir[newLoc.x - offsetX][newLoc.y - offsetY] = dir;
                distance[newLoc.x - offsetX][newLoc.y - offsetY] = dist;
            }
        }
        // regular bellman-ford loop uses # of nodes, but we should be able to cut down a bit
        for (int i = visionRange; i --> 0;)  {
            for (MapLocation loc: rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), visionSq)) {
                int x = loc.x - offsetX;
                int y = loc.y - offsetY;
                if (distance[x][y] == INF_DIST) continue;
                for (Direction dir: DIRECTIONS) {
                    int nx = x + dir.dx;
                    int ny = y + dir.dy;
                    if (nx + offsetX < 0 || nx + offsetX >= rc.getMapWidth() || ny + offsetY < 0 || ny + offsetY >= rc.getMapHeight()) continue;
                    if (nx < 0 || nx >= visionRange || ny < 0 || ny >= visionRange) continue;
                    if (distance[nx][ny] > distance[x][y]) {
                        distance[nx][ny] = distance[x][y];
                        startingDir[nx][ny] = startingDir[x][y];
                    }
                }
            }
        }

        Direction closestDir = Direction.CENTER;
        int closestDistance = INF_DIST;
        MapLocation centerOfRange = new MapLocation(vision, vision);
        for (int x = visionRange; x --> 0;) {
            for (int y = visionRange; y --> 0;) {
                // check the boundaries of the circle only
                if (centerOfRange.isWithinDistanceSquared(new MapLocation(x, y), vision * vision)) {
                    continue;
                }
                // multiply by 1.5 as a quick estimate
                int distanceRemaining = Math.max(
                        Math.abs(target.x - (x + offsetX)),
                        Math.abs(target.y - (y + offsetY))
                ) * 15;
                int totalDistance = distance[x][y] + distanceRemaining;

                if (closestDistance > totalDistance) {
                    closestDistance = totalDistance;
                    closestDir = startingDir[x][y];
                    rc.setIndicatorString((x + offsetX) + " " + (y + offsetY) + " " + target.x + " " + target.y + " " + totalDistance + " " + distanceRemaining);
                }
            }
        }
        return closestDir;
    }

    // FIXME: waiting until API for this is released
//    private static int determineCost(LocationType locationType, Direction dir) throws GameActionException {
//        if (locationType == CLOUD) return CLOUD_COST;
//        else if (locationType == CURRENT && locationType.dir == dir) return 0;
//        // treat current going in other direction as regular tile
//
//        return REGULAR_COST;
//    }
}
