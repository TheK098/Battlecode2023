package qpwoeirut_player;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import qpwoeirut_player.utilities.FastRandom;

import java.util.ArrayList;

public class Headquarters extends BaseBot {

    private static final int HQ_SPAWN_RADIUS = 9;

    public Headquarters(RobotController rc) {
        super(rc);
    }

    @Override
    public void processRound() throws GameActionException {
        // report HQ position
        comms.addHq(rc, rc.getLocation());

        RobotType[] spawnPriority = {RobotType.CARRIER, RobotType.LAUNCHER};
        if (rc.getRoundNum() > 10 && rc.getRoundNum() % 2 == 0) {
            spawnPriority = new RobotType[]{RobotType.LAUNCHER, RobotType.CARRIER};
        }

        int spawnIdx = 0;
        int failures = 0;
        while (failures < spawnPriority.length) {
            MapLocation newCarrierLoc = pickEmptySpawnLocation(spawnPriority[spawnIdx]);
            if (newCarrierLoc != null) {
                rc.buildRobot(spawnPriority[spawnIdx], newCarrierLoc);  // it's guaranteed that we can build
                failures = 0;
            } else ++failures;
            spawnIdx ^= 1;
        }

//        System.out.println(Arrays.toString(comms.getHqs(rc)));
//        System.out.println(Arrays.toString(comms.getKnownWells(rc)));
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
