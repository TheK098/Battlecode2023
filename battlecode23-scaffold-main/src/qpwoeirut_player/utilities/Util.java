package qpwoeirut_player.utilities;

import battlecode.common.*;

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

    public static boolean locationInArray(MapLocation[] locations, MapLocation loc) {
        for (int i = locations.length; i --> 0;) {
            if (locations[i].equals(loc)) return true;
        }
        return false;
    }

    // TODO: include pathfinding in the future
    public static MapLocation pickNearest(MapLocation currentLocation, MapLocation[] locations) {
        int closestIndex = 0;
        int closestDistance = INF_DIST;
        for (int i = locations.length; i --> 0;) {
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
    public static Direction directionToTarget(RobotController rc, MapLocation target, int vision) throws GameActionException {
        int visionRange = vision + vision + 1;

//        return rc.getLocation().directionTo(target);
        // note that indexing is [x][y]
        int offsetX = rc.getLocation().x - vision;
        int offsetY = rc.getLocation().y - vision;

        if (rc.getID() == 10438) System.out.println("1: " + Clock.getBytecodeNum());
        Direction[][] startingDir = new Direction[visionRange][visionRange];
        int[][] distance = new int[visionRange][visionRange];
        for (int r = visionRange; r --> 0;) {
            Arrays.fill(distance[r], INF_DIST);
        }
        if (rc.getID() == 10438) System.out.println("2: " + Clock.getBytecodeNum());

        PriorityQueue<Item> pq = new PriorityQueue<>(62);  // most common range is √20, 20 * π ≈ 62
        if (rc.getID() == 10438) System.out.println("3: " + Clock.getBytecodeNum());
        for (Direction dir : DIRECTIONS) {
            if (rc.canMove(dir)) {
                int dist = REGULAR_COST;

                // FIXME: waiting until API for this is released
                // int dist = determineCost(rc.getLocationType(rc.getLocation(), dir));

                MapLocation newLoc = rc.getLocation().add(dir);
                pq.add(new Item(dist, dir, newLoc));
                startingDir[newLoc.x - offsetX][newLoc.y - offsetY] = dir;
                distance[newLoc.x - offsetX][newLoc.y - offsetY] = dist;
            }
        }
        while (!pq.isEmpty()) {
            if (rc.getID() == 10438) System.out.println("4: " + Clock.getBytecodeNum());
            Item cur = pq.poll();
            if (rc.getID() == 10438) System.out.println("4.1: " + Clock.getBytecodeNum());
            assert cur != null;
            if (distance[cur.loc.x - offsetX][cur.loc.y - offsetY] < cur.dist) continue;
            for (Direction dir : DIRECTIONS) {
                MapLocation newLoc = cur.loc.add(dir);
                if (!rc.canSenseLocation(newLoc)) continue;

                int newDist = cur.dist + REGULAR_COST;

                // FIXME: waiting until API for this is released
                // int newDist = cur.dist + determineCost(rc.getLocationType(cur.loc, dir));
                if (distance[newLoc.x - offsetX][newLoc.y - offsetY] > newDist) {
                    distance[newLoc.x - offsetX][newLoc.y - offsetY] = newDist;

                    startingDir[newLoc.x - offsetX][newLoc.y - offsetY] = cur.dir;
                    pq.add(new Item(newDist, cur.dir, newLoc));
                }
            }
        }
        if (rc.getID() == 10438) System.out.println("5: " + Clock.getBytecodeNum());

        Direction closestDir = Direction.CENTER;
        int closestDistance = INF_DIST;
        MapLocation centerOfRange = new MapLocation(vision, vision);
        for (int x = visionRange; x --> 0;) {
            for (int y = visionRange; y --> 0;) {
                // check the boundaries of the circle only
                if (centerOfRange.isWithinDistanceSquared(new MapLocation(x, y), vision * vision)) {
                    continue;
                }
                // multiply by 1.1 as a quick estimate
                int distanceRemaining = Math.max(
                        Math.abs(target.x - (x + offsetX)),
                        Math.abs(target.y - (y + offsetY))
                ) * 11 / 10;
                int totalDistance = distance[x][y] + distanceRemaining;

                if (closestDistance > totalDistance) {
                    closestDistance = totalDistance;
                    closestDir = startingDir[x][y];
                }
            }
        }
        if (rc.getID() == 10438) System.out.println("6: " + Clock.getBytecodeNum());
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
