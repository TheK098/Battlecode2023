package qpwoeirut_player;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import qpwoeirut_player.utilities.Communications;
import qpwoeirut_player.utilities.FastRandom;

import java.util.ArrayList;

public class Headquarters {
    private static RobotController rc;

    private static final int HQ_SPAWN_RADIUS = 9;

    public static void initialize(RobotController robotController) {
        Headquarters.rc = robotController;
    }

    public static void processRound() throws GameActionException {
        // report HQ position
        Communications.addHq(rc, rc.getLocation());

        // for now the strat is to spam carriers
        MapLocation newCarrierLoc = pickEmptySpawnLocation(RobotType.CARRIER);
        while (newCarrierLoc != null) {
            rc.buildRobot(RobotType.CARRIER, newCarrierLoc);  // it's guaranteed that we can build
            newCarrierLoc = pickEmptySpawnLocation(RobotType.CARRIER);
        }
    }

    /**
     * Picks a random spawn location within the Headquarters' action radius
     * Returns null if no locations are available or if a robot cannot be built
     */
    private static MapLocation pickEmptySpawnLocation(RobotType robotType) throws GameActionException {
        // TODO: arraylist is probably bytecode-heavy, should optimize
        ArrayList<MapLocation> possibleLocations = new ArrayList<>();
        for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), HQ_SPAWN_RADIUS)) {
            if (rc.canBuildRobot(robotType, loc)) possibleLocations.add(loc);
        }
        return possibleLocations.isEmpty() ? null : possibleLocations.get(FastRandom.nextInt(possibleLocations.size()));
    }
}
