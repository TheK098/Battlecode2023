package qp1_11_tuning;

import battlecode.common.*;
import qp1_11_tuning.communications.Comms;
import qp1_11_tuning.communications.Comms.EnemySighting;
import qp1_11_tuning.communications.Comms.WellLocation;
import qp1_11_tuning.utilities.FastRandom;

import static qp1_11_tuning.navigation.Pathfinding.INF_DIST;
import static qp1_11_tuning.utilities.Util.pickNearest;

public class Headquarters extends BaseBot {
    private static int lastEnemyCommUpdate = 0;
    private static MapLocation[] spawnLocations;
    private static int[] centerDist;

    // distance to nearest well, with slightly smaller values for locations far from HQ
    private static int[] adamantiumWellDist, manaWellDist;

    private static int AMPLIFIER_FREQUENCY;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);

        MapLocation curLocation = rc.getLocation();
        MapLocation[] reachableLocations = rc.getAllLocationsWithinRadiusSquared(curLocation, RobotType.HEADQUARTERS.actionRadiusSquared);
        int canBuild = 0;  // store array of 29 booleans
        int n = 0;
        for (int i = reachableLocations.length; i --> 0;)
            if (rc.canBuildRobot(RobotType.CARRIER, reachableLocations[i])) {
                canBuild |= 1 << i;
                ++n;
            }

        spawnLocations = new MapLocation[n];
        centerDist = new int[n];
        adamantiumWellDist = new int[n]; manaWellDist = new int[n];
        MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        for (int i = reachableLocations.length; i --> 0;)
            if (((canBuild >> i) & 1) == 1) {
                spawnLocations[--n] = reachableLocations[i];
                centerDist[n] = spawnLocations[n].distanceSquaredTo(center);
                adamantiumWellDist[n] = manaWellDist[n] = INF_DIST - curLocation.distanceSquaredTo(spawnLocations[n]);
            }

        Comms.addHq(rc, curLocation); // report HQ position
        Comms.addWells(rc, rc.senseNearbyWells());

        AMPLIFIER_FREQUENCY = 6000 / (int)Math.round(Math.sqrt(rc.getMapWidth() * rc.getMapHeight()));
    }

    @Override
    public void processRound() throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        EnemySighting[] sightings = Comms.getEnemySightings(rc);
        WellLocation[] wells = Comms.getKnownWells(rc);

        updateEnemyComms(enemies);
        updateResourcePriorities(allies, sightings);
        calculateWellDistances(wells);

        if ((rc.getRoundNum() - rc.getID()) % (18 / Comms.getHqs(rc).length) == 0) Comms.decreaseUrgencies(rc);
        if (enemies.length >= 8 && allies.length == 0) return;  // save resources, any bots will get spawnkilled

        if (!itsAnchorTime() || (rc.getResourceAmount(ResourceType.ADAMANTIUM) >= 300 && rc.getResourceAmount(ResourceType.MANA) >= 300)) {
            RobotType[] spawnPriority = {RobotType.CARRIER, RobotType.LAUNCHER};
            if ((rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 0) ||
                    (5 < rc.getRoundNum() && rc.getRoundNum() <= 50 && rc.getRoundNum() % 2 == 0) ||
                    (50 < rc.getRoundNum() && wells.length > 0 && FastRandom.nextInt(50 * wells.length) < rc.getRobotCount()))
                spawnPriority = new RobotType[]{RobotType.LAUNCHER, RobotType.CARRIER};

            MapLocation newLoc;
            if (rc.getRoundNum() >= 400 && rc.getRoundNum() % AMPLIFIER_FREQUENCY == 0) {
                newLoc = pickCentralSpawnLocation(RobotType.AMPLIFIER, allies.length);
                if (newLoc != null) rc.buildRobot(RobotType.AMPLIFIER, newLoc);
            }

            newLoc = pickEmptySpawnLocation(spawnPriority[0], allies.length);
            int typeIdx = 0;
            while (newLoc != null) {
                rc.buildRobot(spawnPriority[typeIdx], newLoc);  // it's guaranteed that we can build
                typeIdx ^= 1;
                newLoc = pickEmptySpawnLocation(spawnPriority[typeIdx], allies.length);
            }
            newLoc = pickEmptySpawnLocation(spawnPriority[typeIdx], allies.length);
            if (newLoc != null) rc.buildRobot(spawnPriority[typeIdx], newLoc);  // try again with other type
        }
        if (itsAnchorTime() && rc.canBuildAnchor(Anchor.STANDARD) && rc.getNumAnchors(Anchor.STANDARD) < 3) {
            // stick with Standard anchors for now, chances are we're already overrunning the map
            rc.buildAnchor(Anchor.STANDARD);
        }
    }

    /**
     * Picks a random spawn location within the Headquarters' action radius
     * Avoids spawning if most locations (relative to # of passable locations) are already full to prevent clogging
     * Returns null if cannot/should not be built
     */
    private static MapLocation pickEmptySpawnLocation(RobotType robotType, int visibleAllies) throws GameActionException {
        if (rc.isActionReady()) {
            switch (robotType) {
                case LAUNCHER:
                case AMPLIFIER:
                    return pickCentralSpawnLocation(robotType, visibleAllies);
                case CARRIER:  // spawn close to well, with some random variation
                    return pickCarrierSpawnLocation(visibleAllies);
            }
            throw new IllegalArgumentException("Only handling building launchers and carriers");
        }
        return null;
    }

    private static int locIdx = 0;

    private static void calculateWellDistances(WellLocation[] wells) {
        if (locIdx == 0) locIdx = spawnLocations.length;
        while (locIdx --> 0 && Clock.getBytecodesLeft() > 10000) {
            WellLocation nearestAdamantiumWell = pickNearest(spawnLocations[locIdx], wells, ResourceType.ADAMANTIUM);
            if (nearestAdamantiumWell != null) {
                adamantiumWellDist[locIdx] = spawnLocations[locIdx].distanceSquaredTo(nearestAdamantiumWell.location);
            }

            WellLocation nearestManaWell = pickNearest(spawnLocations[locIdx], wells, ResourceType.MANA);
            if (nearestManaWell != null) {
                manaWellDist[locIdx] = spawnLocations[locIdx].distanceSquaredTo(nearestManaWell.location);
            }
        }
    }

    private static MapLocation pickCentralSpawnLocation(RobotType robotType, int visibleAllies) {
        // spawn as close to center as possible
        if (rc.isActionReady()) {
            int bestDist = INF_DIST, bestIdx = -1;
            int availableSpots = 0;
            for (int i = spawnLocations.length; i-- > 0; ) {
                if (rc.canBuildRobot(robotType, spawnLocations[i])) {
                    ++availableSpots;
                    if (bestDist > centerDist[i]) {
                        bestDist = centerDist[i];
                        bestIdx = i;
                    }
                }
            }
            return bestIdx == -1 || availableSpots * 24 < visibleAllies ? null : spawnLocations[bestIdx];
        }
        return null;
    }

    // assumes calculateWellDistances has already been called
    private static MapLocation pickCarrierSpawnLocation(int visibleAllies) throws GameActionException {
        int adamantiumPriority = Comms.getAdamantiumPriority(rc), manaPriority = Comms.getManaPriority(rc);
        int bestDist = INF_DIST, bestIdx = -1;
        int dist;
        int availableSpots = 0;
        for (int i = spawnLocations.length; i --> 0;) {
            if (rc.canBuildRobot(RobotType.CARRIER, spawnLocations[i])) {
                ++availableSpots;
                dist = Math.min(adamantiumWellDist[i] - adamantiumPriority, manaWellDist[i] - manaPriority);
                if (bestDist > dist) {
                    bestDist = dist;
                    bestIdx = i;
                }
            }
        }
        return bestIdx == -1 || availableSpots * 20 < visibleAllies ? null : spawnLocations[bestIdx];
    }

    private static void updateEnemyComms(RobotInfo[] enemies) throws GameActionException {
        if (lastEnemyCommUpdate + 5 <= rc.getRoundNum()) {
            RobotInfo nearestEnemy = pickNearest(rc, enemies);
            if (nearestEnemy != null && Comms.reportEnemySighting(rc, nearestEnemy.location))
                lastEnemyCommUpdate = rc.getRoundNum();
        }
    }

    private static void updateResourcePriorities(RobotInfo[] allies, EnemySighting[] sightings) throws GameActionException {
        int carrierDensity = 4;
        for (int i = allies.length; i--> 0;) carrierDensity += allies[i].type == RobotType.CARRIER ? 1 : 0;
        int adamantiumPriority = spawnLocations.length / carrierDensity;  // locations is action radius, carrierDensity is vision radius

        int manaPriority = 0;
        for (int i = sightings.length; i--> 0;) manaPriority += sightings[i].urgency;
        manaPriority = Math.min(30, (manaPriority / 4) + 3600 / (rc.getMapWidth() * rc.getMapHeight()));

        Comms.setResourcePriorities(rc,
                calculateResourcePriority(Comms.getAdamantiumPriority(rc), adamantiumPriority),
                calculateResourcePriority(Comms.getManaPriority(rc), manaPriority));
        rc.setIndicatorString(adamantiumPriority + " " + manaPriority + " " + sightings.length);
    }

    private static int calculateResourcePriority(int oldValue, int newValue) {
        float factor = (float) Math.sqrt(rc.getRoundNum());
        return Math.round((oldValue * (rc.getRoundNum() - factor) + newValue * factor) / rc.getRoundNum());
    }
}
