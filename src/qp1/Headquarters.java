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
    private static MapLocation[] spawnLocations;
    private static int[] centerDist;

    // distance to nearest well, with slightly smaller values for locations far from HQ
    private static int[] adamantiumWellDist, manaWellDist;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);

        MapLocation[] reachableLocations = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), RobotType.HEADQUARTERS.actionRadiusSquared);
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
                adamantiumWellDist[n] = INF_DIST - rc.getLocation().distanceSquaredTo(spawnLocations[n]);
                manaWellDist[n] = INF_DIST - rc.getLocation().distanceSquaredTo(spawnLocations[n]);
            }

        Comms.addHq(rc, rc.getLocation()); // report HQ position
        Comms.addWells(rc, rc.senseNearbyWells());
    }

    @Override
    public void processRound() throws GameActionException {
        if (rc.getRoundNum() == 2) updateCommsOffsets();

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

            MapLocation newCarrierLoc = pickEmptySpawnLocation(spawnPriority[0], allies.length);
            int typeIdx = 0;
            while (newCarrierLoc != null) {
                rc.buildRobot(spawnPriority[typeIdx], newCarrierLoc);  // it's guaranteed that we can build
                typeIdx ^= 1;
                newCarrierLoc = pickEmptySpawnLocation(spawnPriority[typeIdx], allies.length);
            }
            newCarrierLoc = pickEmptySpawnLocation(spawnPriority[typeIdx], allies.length);
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
    private static MapLocation pickEmptySpawnLocation(RobotType robotType, int visibleAllies) throws GameActionException {
        if (rc.isActionReady()) {
            switch (robotType) {
                case LAUNCHER:
                    return pickLauncherSpawnLocation(visibleAllies);
                case CARRIER:  // spawn close to well, with some random variation
                    return pickCarrierSpawnLocation(visibleAllies);
            }
            throw new IllegalArgumentException("Only handling building launchers and carriers");
        }
        return null;
    }

    private static void calculateWellDistances(WellLocation[] wells) {
        for (int i = spawnLocations.length; i --> 0;) {
            WellLocation nearestAdamantiumWell = pickNearest(spawnLocations[i], wells, ResourceType.ADAMANTIUM);
            if (nearestAdamantiumWell != null) {
                adamantiumWellDist[i] = spawnLocations[i].distanceSquaredTo(nearestAdamantiumWell.location);
            }

            WellLocation nearestManaWell = pickNearest(spawnLocations[i], wells, ResourceType.MANA);
            if (nearestManaWell != null) {
                manaWellDist[i] = spawnLocations[i].distanceSquaredTo(nearestManaWell.location);
            }
        }
    }

    private static MapLocation pickLauncherSpawnLocation(int visibleAllies) {
        // spawn as close to center as possible
        if (rc.isActionReady()) {
            int bestDist = INF_DIST, bestIdx = -1;
            int availableSpots = 0;
            for (int i = spawnLocations.length; i-- > 0; ) {
                if (rc.canBuildRobot(RobotType.LAUNCHER, spawnLocations[i])) {
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
            RobotInfo nearestEnemy = pickNearest(rc, enemies, false);
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
        manaPriority = Math.min(30, (manaPriority / 10) + 3600 / (rc.getMapWidth() * rc.getMapHeight()));

        Comms.setResourcePriorities(rc,
                calculateResourcePriority(Comms.getAdamantiumPriority(rc), adamantiumPriority),
                calculateResourcePriority(Comms.getManaPriority(rc), manaPriority));
        rc.setIndicatorString(adamantiumPriority + " " + manaPriority + " " + sightings.length);
    }

    private static int calculateResourcePriority(int oldValue, int newValue) {
        float factor = (float) Math.sqrt(rc.getRoundNum());
        return Math.round((oldValue * (1 - factor) + newValue * factor) / rc.getRoundNum());
    }
}
