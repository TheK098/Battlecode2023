package qpwoeirut_player.utilities;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Arrays;
import java.util.PriorityQueue;

public class Util {

    public static final int INF_DIST = 60 * 60 * 60 * 60;
    public static final int REGULAR_COST = 10;
    public static final int CLOUD_COST = 12;

    public static final Direction[] DIRECTIONS = {  // put diagonal directions first since they should go faster maybe?
            Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST,
            Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH
    };

    // TODO: include pathfinding in the future
    public static MapLocation pickNearest(MapLocation currentLocation, MapLocation[] locations) {
        int closestIndex = 0;
        int closestDistance = INF_DIST;
        for (int i = 0; i < locations.length; ++i) {
            int distance = locations[i].distanceSquaredTo(currentLocation);
            if (closestDistance > distance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        return locations[closestIndex];
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
    public static Direction directionToTarget(RobotController rc, MapLocation target) throws GameActionException {
        // note that indexing is [x][y]
        Direction[][] startingDir = new Direction[rc.getMapWidth()][rc.getMapHeight()];
        int[][] distance = new int[rc.getMapWidth()][rc.getMapHeight()];
        for (int r = 0; r < rc.getMapWidth(); ++r) {
            Arrays.fill(distance[r], INF_DIST);
        }

        PriorityQueue<Item> pq = new PriorityQueue<>(62);  // most common range is √20, 20 * π ≈ 62
        for (Direction dir : DIRECTIONS) {
            if (rc.canMove(dir)) {
                int dist = REGULAR_COST;

                // FIXME: waiting until API for this is released
                // int dist = determineCost(rc.getLocationType(rc.getLocation(), dir));

                MapLocation newLoc = rc.getLocation().add(dir);
                pq.add(new Item(dist, dir, newLoc));
                startingDir[newLoc.x][newLoc.y] = dir;
                distance[newLoc.x][newLoc.y] = dist;
            }
        }
        while (!pq.isEmpty()) {
            Item cur = pq.poll();
            if (distance[cur.loc.x][cur.loc.y] < cur.dist) continue;
            if (!rc.canSenseLocation(cur.loc)) continue;
            for (Direction dir : DIRECTIONS) {
                MapLocation newLoc = cur.loc.add(dir);
                if (newLoc.x < 0 || newLoc.x >= rc.getMapWidth() || newLoc.y < 0 || newLoc.y >= rc.getMapHeight())
                    continue;

                int newDist = cur.dist + REGULAR_COST;

                // FIXME: waiting until API for this is released
                // int newDist = cur.dist + determineCost(rc.getLocationType(cur.loc, dir));
                if (distance[newLoc.x][newLoc.y] > newDist) {
                    distance[newLoc.x][newLoc.y] = newDist;

                    startingDir[newLoc.x][newLoc.y] = cur.dir;
                    pq.add(new Item(newDist, cur.dir, newLoc));
                }
            }
        }

        Direction closestDir = Direction.CENTER;
        int closestDistance = INF_DIST;
        for (int x = 0; x < rc.getMapWidth(); ++x) {
            for (int y = 0; y < rc.getMapHeight(); ++y) {
                // multiply by 1.1 as a quick estimate
                int distanceRemaining = Math.max(Math.abs(target.x - x), Math.abs(target.y - y)) * 11 / 10;
                int totalDistance = distance[x][y] + distanceRemaining;

                if (closestDistance > totalDistance) {
                    closestDistance = totalDistance;
                    closestDir = startingDir[x][y];
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
