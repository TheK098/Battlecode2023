package qpwoeirut_player;

import battlecode.common.*;
import qpwoeirut_player.common.Communications;
import qpwoeirut_player.utilities.FastRandom;

import java.util.ArrayList;

public class Headquarters extends BaseBot {

    private static final int HQ_SPAWN_RADIUS = 9;

    public Headquarters(RobotController rc) {
        super(rc);
    }

    @Override
    public void processRound() throws GameActionException {
        if (rc.getRoundNum() == 1) Communications.addHq(rc, rc.getLocation()); // report HQ position

        if (rc.getRoundNum() % 20 == rc.getID() % 20) Communications.decreaseUrgencies(rc);
        // urgencies will decrease faster if there are multiple HQs; consider that a feature i guess?

        if (!itsAnchorTime()) {
            RobotType[] spawnPriority = {RobotType.CARRIER, RobotType.LAUNCHER};
            if ((rc.getRoundNum() > 10 && rc.getRoundNum() % 2 == 0) || rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 0) {
                spawnPriority = new RobotType[]{RobotType.LAUNCHER, RobotType.CARRIER};
            }

            int spawnIdx = 0;
            int failures = 0;
            while (failures < spawnPriority.length) {
                // TODO: spawn carriers closer to wells
                // TODO: don't spawn if entire vision range is almost full to prevent clogging
                MapLocation newCarrierLoc = pickEmptySpawnLocation(spawnPriority[spawnIdx]);
                if (newCarrierLoc != null) {
                    rc.buildRobot(spawnPriority[spawnIdx], newCarrierLoc);  // it's guaranteed that we can build
                    failures = 0;
                } else ++failures;
                spawnIdx ^= 1;
            }
        } else if (rc.canBuildAnchor(Anchor.STANDARD)) {
            // stick with Standard anchors for now, chances are we're already overrunning the map
            rc.buildAnchor(Anchor.STANDARD);
        }
    }

    /**
     * Picks a random spawn location within the Headquarters' action radius
     * Avoids spawning if most locations (relative to # of passable locations) are already full to prevent clogging
     * Returns null if cannot/should not be built
     */
    private static MapLocation pickEmptySpawnLocation(RobotType robotType) throws GameActionException {
        // TODO: arraylist is probably bytecode-heavy, should optimize
        ArrayList<MapLocation> possibleLocations = new ArrayList<>();
        for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), HQ_SPAWN_RADIUS)) {
            if (rc.canBuildRobot(robotType, loc)) possibleLocations.add(loc);
        }
        int nearbyRobots = rc.senseNearbyRobots(RobotType.HEADQUARTERS.actionRadiusSquared).length;
        return possibleLocations.size() <= nearbyRobots / 12 ? null : possibleLocations.get(FastRandom.nextInt(possibleLocations.size()));
    }
}
