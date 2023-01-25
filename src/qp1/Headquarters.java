package qp1;

import battlecode.common.*;
import qp1.communications.Comms;
import qp1.communications.Comms.EnemySighting;
import qp1.communications.Comms.WellLocation;
import qp1.utilities.FastRandom;

import static qp1.navigation.Pathfinding.INF_DIST;
import static qp1.utilities.Util.pickNearest;

public class Headquarters extends BaseBot {
    private static int lastEnemyCommUpdate = 0;
    private static MapLocation[] possibleLocations;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
        possibleLocations = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), RobotType.HEADQUARTERS.actionRadiusSquared);
    }

    @Override
    public void processRound() throws GameActionException {
        if (rc.getRoundNum() == 1) {
            Comms.addHq(rc, rc.getLocation()); // report HQ position
            Comms.addWells(rc, rc.senseNearbyWells());
        }
        updateEnemyComms();
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        EnemySighting[] sightings = Comms.getEnemySightings(rc);
        int adamantiumPriority, manaPriority = 3600 / (rc.getMapWidth() * rc.getMapHeight());
        int carrierDensity = 1;
        for (int i = allies.length; i--> 0;) carrierDensity += allies[i].type == RobotType.CARRIER ? 1 : 0;
        for (int i = sightings.length; i--> 0;) manaPriority += sightings[i].urgency;
        adamantiumPriority = possibleLocations.length / carrierDensity;  // locations is action radius, carrierDensity is vision radius
        Comms.setResourcePriorities(rc, adamantiumPriority, Math.min(30, manaPriority / 10));
        rc.setIndicatorString(adamantiumPriority + " " + manaPriority);

        if (rc.getRoundNum() % 20 == rc.getID() % 20) Comms.decreaseUrgencies(rc);
        // urgencies will decrease faster if there are multiple HQs; consider that a feature i guess?

        if (!itsAnchorTime() || (rc.getResourceAmount(ResourceType.ADAMANTIUM) >= 300 && rc.getResourceAmount(ResourceType.MANA) >= 300)) {
            RobotType[] spawnPriority = {RobotType.CARRIER, RobotType.LAUNCHER};
            if ((rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 0) ||
                    (5 < rc.getRoundNum() && rc.getRoundNum() <= 50 && rc.getRoundNum() % 2 == 0) ||
                    (50 < rc.getRoundNum() && FastRandom.nextInt(50 * Comms.getKnownWells(rc).length) < rc.getRobotCount()))
                spawnPriority = new RobotType[]{RobotType.LAUNCHER, RobotType.CARRIER};

            MapLocation newCarrierLoc  = pickEmptySpawnLocation(spawnPriority[0]);
            int typeIdx = 0;
            while (newCarrierLoc != null) {
                rc.buildRobot(spawnPriority[typeIdx], newCarrierLoc);  // it's guaranteed that we can build
                typeIdx ^= 1;
                newCarrierLoc = pickEmptySpawnLocation(spawnPriority[typeIdx]);
            }
            newCarrierLoc = pickEmptySpawnLocation(spawnPriority[typeIdx]);
            if (newCarrierLoc != null) rc.buildRobot(spawnPriority[typeIdx], newCarrierLoc);  // try again with other type
        }
        if (itsAnchorTime() && rc.canBuildAnchor(Anchor.STANDARD)) {
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
        if (rc.isActionReady()) {
            int nearbyRobots = rc.senseNearbyRobots(RobotType.HEADQUARTERS.actionRadiusSquared).length;
            int availableSpots = 0;
            int bestDist, bestIdx;

            switch (robotType) {
                case LAUNCHER:  // spawn as close to center as possible
                    MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
                    bestDist = INF_DIST;
                    bestIdx = -1;
                    for (int i = possibleLocations.length; i-- > 0; ) {
                        if (rc.canBuildRobot(robotType, possibleLocations[i])) {
                            ++availableSpots;
                            if (possibleLocations[i].isWithinDistanceSquared(center, bestDist - 1)) {
                                bestDist = possibleLocations[i].distanceSquaredTo(center);
                                bestIdx = i;
                            }
                        }
                    }
                    return bestIdx == -1 || availableSpots * 12 < nearbyRobots ? null : possibleLocations[bestIdx];
                case CARRIER:  // spawn close to well, with some random variation
                    WellLocation[] wells = Comms.getKnownWells(rc);
                    if (wells.length == 0) {  // can happen in HQ in cloud, just pick random spot
                        int[] validIndexes = new int[possibleLocations.length];
                        int n = 0;
                        for (int i = possibleLocations.length; i --> 0;) {
                            if (rc.canBuildRobot(robotType, possibleLocations[i])) {
                                validIndexes[n++] = i;
                            }
                        }
                        return n == 0 ? null : possibleLocations[validIndexes[FastRandom.nextInt(n)]];
                    }

                    bestDist = INF_DIST;
                    bestIdx = -1;
                    int adamantiumPriority = Comms.getAdamantiumPriority(rc), manaPriority = Comms.getManaPriority(rc);
                    int typeBonus;
                    for (int i = possibleLocations.length; i --> 0;) {
                        WellLocation nearestWell = pickNearest(possibleLocations[i], wells);
                        if (rc.canBuildRobot(robotType, possibleLocations[i]) && nearestWell != null) {
                            ++availableSpots;
                            typeBonus = nearestWell.resourceType == ResourceType.ADAMANTIUM ? adamantiumPriority : manaPriority;
                            if (possibleLocations[i].isWithinDistanceSquared(nearestWell.location, bestDist + typeBonus)) {
                                bestDist = possibleLocations[i].distanceSquaredTo(nearestWell.location);
                                bestIdx = i;
                            }
                        }
                    }
                    return bestIdx == -1 || availableSpots * 10 < nearbyRobots ? null : possibleLocations[bestIdx];
            }
            throw new IllegalArgumentException("Only handling building launchers and carriers");
        }
        return null;
    }

    private static void updateEnemyComms() throws GameActionException {
        if (lastEnemyCommUpdate + 5 <= rc.getRoundNum()) {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo nearestEnemy = pickNearest(rc, enemies, false);
            if (nearestEnemy != null && Comms.reportEnemySighting(rc, nearestEnemy.location))
                lastEnemyCommUpdate = rc.getRoundNum();
        }
    }
}
