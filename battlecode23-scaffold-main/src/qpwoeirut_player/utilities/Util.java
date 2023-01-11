package qpwoeirut_player.utilities;

import battlecode.common.MapLocation;

import static qpwoeirut_player.common.Pathfinding.INF_DIST;

public class Util {
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
}
