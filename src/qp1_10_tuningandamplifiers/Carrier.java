package qp1_10_tuningandamplifiers;

import battlecode.common.*;
import qp1_10_tuningandamplifiers.communications.Comms;
import qp1_10_tuningandamplifiers.communications.Comms.IslandInfo;
import qp1_10_tuningandamplifiers.communications.Comms.WellLocation;
import qp1_10_tuningandamplifiers.communications.EntityType;
import qp1_10_tuningandamplifiers.navigation.SpreadSettings;
import qp1_10_tuningandamplifiers.utilities.Util;

import static qp1_10_tuningandamplifiers.navigation.Pathfinding.*;
import static qp1_10_tuningandamplifiers.utilities.Util.*;


public class Carrier extends BaseBot {
    private static final int CAPACITY = 39;  // using 39 helps us move faster

    // some wells or HQs have so many carriers already that we need to direct this carrier to a different one
    // blacklist[x][y] = t --> don't use (x, y) as a target until round t
    private static int[][] blacklist;
    private static MapLocation currentTarget = null;
    private static int timeRemaining = 100;
    private static MapLocation enemySighting = null;
    private static int adamantiumCooldown = 0, manaCooldown = 0;

    // save the well we're collecting from so that we can collect the same resource
    private static MapLocation targetWell = null;

    public Carrier(RobotController rc) throws GameActionException {
        super(rc);
        blacklist = new int[rc.getMapWidth()][rc.getMapHeight()];
    }

    @Override
    public void processRound() throws GameActionException {
        Comms.addWells(rc, rc.senseNearbyWells());
        Comms.addNearbyIslands(rc);

        if (enemySighting != null) {
            if (Comms.reportEnemySighting(rc, enemySighting)) {
                enemySighting = null;
            }
            else {
                MapLocation nearestHq = Util.pickNearest(rc, Comms.getHqs(rc));
                tryMove(moveToward(rc, nearestHq, 700));
                return;
            }
        }

        // deal with issue where carriers with resources sometimes end up next to blacklisted headquarters
        whitelistAdjacent();

//        debugBytecode("1.0");

        boolean shouldReturn = handleCombat();
        if (shouldReturn) return;

        boolean adjacentToHq = adjacentToHeadquarters(rc, rc.getLocation());
        if ((rc.getID() % 8 == 0 && itsAnchorTime()) || rc.getAnchor() != null) {
            if (getCurrentResources() > 0) returnResources();
            else handleAnchor();
        } else if (adjacentToHq && ((rc.getResourceAmount(ResourceType.ADAMANTIUM) > 0 && adamantiumCooldown <= 0) || (rc.getResourceAmount(ResourceType.MANA) > 0 && manaCooldown <= 0))) {
            // ensure we don't hold on to a resource that takes up capacity
            returnResources();
        } else if (getCurrentResources() >= 39 || (getCurrentResources() > 0 && adjacentToHq && !adjacentToWell(rc, rc.getLocation()))) {
            returnResources();
        } else {
            collectResources();
        }
//        debugBytecode("1.1");

        dieIfStuck();
    }

    private static boolean handleCombat() throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemies.length > 0) {
            int closestEnemy = -1, closestLauncher = -1;
            int closestEnemyDist = INF_DIST, closestLauncherDist = INF_DIST;
            boolean onlyCarriersNearby = true;
            for (int i = enemies.length; i --> 0;) {
                onlyCarriersNearby &= enemies[i].type == RobotType.CARRIER;

                int distance = enemies[i].location.distanceSquaredTo(rc.getLocation());
                if (closestEnemyDist > distance && enemies[i].type != RobotType.HEADQUARTERS) {
                    closestEnemyDist = distance;
                    closestEnemy = i;
                }
                if (closestLauncherDist > distance && enemies[i].type == RobotType.LAUNCHER) {
                    closestLauncherDist = distance;
                    closestLauncher = i;
                }
            }

            if (closestEnemy == -1) return false;  // only visible enemy was HQ

            if (onlyCarriersNearby && rc.getHealth() == rc.getType().health) return false;  // continue as normal, report enemy location when depositing resources

            // record enemy location and run away
            if (!Comms.reportEnemySighting(rc, enemies[closestEnemy].location))
                enemySighting = enemies[closestEnemy].location;

            if ((closestLauncher == -1 || !tryToAttack(enemies[closestLauncher].location)) && !tryToAttack(enemies[closestEnemy].location)) {
                // dump resources and then run away
                if (rc.canAttack(rc.getLocation())) {
                    rc.attack(rc.getLocation());  // just toss the resources so we can move faster
                    return true;
                }
            }

            MapLocation nearestHq = Util.pickNearest(rc, Comms.getHqs(rc));
            assert nearestHq != null;
            tryMove(moveToward(rc, nearestHq, 700));
            tryMove(moveToward(rc, nearestHq, 700));
            return true;
        }
         return false;
    }

    private static boolean tryToAttack(MapLocation location) throws GameActionException {
        Direction toward = directionTowardImmediate(rc, location);
        MapLocation closer = rc.getLocation().add(toward);
        if (!rc.canAttack(location) && closer.isWithinDistanceSquared(location, rc.getType().actionRadiusSquared)) {
            tryMove(toward);
        }
        if (rc.canAttack(location)) {
            rc.attack(location);
            return true;
        }
        return false;
    }

    private static void handleAnchor() throws GameActionException {
        if (rc.getAnchor() == null) {  // need to pick up an anchor
            MapLocation targetHq = Util.pickNearest(rc, Comms.getHqs(rc), blacklist);
            if (targetHq != null) {
                moveTowardHeadquarters(targetHq);
                if (rc.canTakeAnchor(targetHq, Anchor.STANDARD)) {
                    rc.takeAnchor(targetHq, Anchor.STANDARD);
                }
            } else {
                tryMove(spreadOut(rc, 0, 0, SpreadSettings.CARRIER_SEARCHING));
                tryMove(spreadOut(rc, 0, 0, SpreadSettings.CARRIER_SEARCHING));
            }
        }
        if (rc.getAnchor() == Anchor.STANDARD) {
            // required since comms only stores approx location
            MapLocation nearestVisibleIsland = findNearestVisibleIslandLocation(Team.NEUTRAL);
            if (nearestVisibleIsland == null) {
                IslandInfo target = pickNearest(rc, Comms.getIslands(rc), Team.NEUTRAL);
                if (target != null) {
                    tryMove(moveToward(rc, target.location, 1500));  // no-op if we're at target already
                } else {
                    // spread out from other anchor bots
                    tryMove(spreadOut(rc, 0, 0, SpreadSettings.CARRIER_ANCHOR));
                }
            } else if (rc.canPlaceAnchor()) {
                rc.placeAnchor();
            } else tryMove(moveToward(rc, nearestVisibleIsland, 700));
        }
    }

    private static void collectResources() throws GameActionException {
//        debugBytecode("2.0");
        if (targetWell == null || !rc.getLocation().isAdjacentTo(targetWell)) {
            WellLocation well = pickWell();
            if (well != null) targetWell = well.location;
        }

        if (targetWell == null) {
            tryMove(spreadOut(rc, 0, 0, SpreadSettings.CARRIER_SEARCHING));
            return;
        }
        handleBlacklist(targetWell, EntityType.WELL);

//        Direction dir = pickDirectionForCollection(rc, targetWell);
        moveTowardsWell(targetWell);

        if (rc.canSenseLocation(targetWell)) {
            int toCollect = Math.min(CAPACITY - getCurrentResources(), rc.senseWell(targetWell).getRate());
            if (rc.canCollectResource(targetWell, toCollect)) {
                rc.collectResource(targetWell, toCollect);
                lastMoveOrAction = rc.getRoundNum();
                switch (rc.senseWell(targetWell).getResourceType()) {
                    case ADAMANTIUM: adamantiumCooldown = 10; break;
                    case MANA: manaCooldown = 10; break;
                }
            }
        }

//        debugBytecode("2.2");
    }

    private static WellLocation pickWell() throws GameActionException {
        int adamantiumPriority = Comms.getAdamantiumPriority(rc);
        int manaPriority = Comms.getManaPriority(rc);

        WellLocation[] wells = Comms.getKnownWells(rc);

        int closestIndex = -1;
        int closestDistance = INF_DIST;
        for (int i = wells.length; i --> 0;) {
            int prioritizationDiscount = wells[i].resourceType == ResourceType.ADAMANTIUM ? adamantiumPriority : (wells[i].resourceType == ResourceType.MANA ? manaPriority : 0);
            int distance = wells[i].location.distanceSquaredTo(rc.getLocation()) - prioritizationDiscount;
            if (closestDistance > distance && blacklist[wells[i].location.x][wells[i].location.y] <= rc.getRoundNum()) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        return closestIndex == -1 ? null : wells[closestIndex];
    }

    private static void returnResources() throws GameActionException {
//        debugBytecode("3.0");
        targetWell = null;

        MapLocation targetHq = Util.pickNearest(rc, Comms.getHqs(rc), blacklist);
        if (targetHq != null) {
            handleBlacklist(targetHq, EntityType.HQ);

//            debugBytecode("3.1");
            moveTowardHeadquarters(targetHq);
//            debugBytecode("3.2");

            int mana = rc.getResourceAmount(ResourceType.MANA);
            int adamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
            int elixir = rc.getResourceAmount(ResourceType.ELIXIR);
            if (mana > 0 && rc.canTransferResource(targetHq, ResourceType.MANA, mana)) {
                rc.transferResource(targetHq, ResourceType.MANA, mana);
                lastMoveOrAction = rc.getRoundNum();
            } else if (adamantium > 0 && rc.canTransferResource(targetHq, ResourceType.ADAMANTIUM, adamantium)) {
                rc.transferResource(targetHq, ResourceType.ADAMANTIUM, adamantium);
                lastMoveOrAction = rc.getRoundNum();
            } else if (elixir > 0 && rc.canTransferResource(targetHq, ResourceType.ELIXIR, elixir)) {
                rc.transferResource(targetHq, ResourceType.ELIXIR, elixir);
                lastMoveOrAction = rc.getRoundNum();
            }
//            debugBytecode("3.3");
        } else {
            // this will only happen when we can't reach any of the HQs, so let's move away to try and free up space
            MapLocation nearestHq = Util.pickNearest(rc, Comms.getHqs(rc));
            assert nearestHq != null;
            if (rc.getLocation().isWithinDistanceSquared(nearestHq, 16)) {
                tryMove(directionAway(rc, nearestHq));
            } else tryMove(spreadOut(rc, 0, 0, SpreadSettings.CARRIER_SEARCHING));
//            debugBytecode("3.4");
        }
    }

    private static void moveTowardHeadquarters(MapLocation targetHq) throws GameActionException {
        Direction dir;
        if (adjacentToHeadquarters(rc, rc.getLocation())) {
            resetBlacklistTimer(EntityType.HQ);
            dir = moveWhileStayingAdjacent(rc, targetHq);
        } else if (timeRemaining <= EntityType.HQ.randomMoveCutoff && timeRemaining % EntityType.HQ.randomMovePeriod == 0) {
            dir = randomDirection(rc);
        } else {
            dir = moveToward(rc, targetHq, 1950);
        }
        tryMove(dir);
        tryMove(similarDirection(rc, dir));
    }

    private static void moveTowardsWell(MapLocation targetWell) throws GameActionException {
        Direction dir;
        if (adjacentToWell(rc, rc.getLocation())) {
            resetBlacklistTimer(EntityType.WELL);
            dir = moveWhileStayingAdjacent(rc, targetWell);
        } else if (timeRemaining <= EntityType.WELL.randomMoveCutoff && timeRemaining % EntityType.WELL.randomMovePeriod == 0) {
            dir = randomDirection(rc);
        } else {
            dir = moveToward(rc, targetWell, 1950);
        }

//        debugBytecode("2.1");

        if (tryMove(dir)) {
            if (!adjacentToWell(rc, rc.getLocation())) tryMove(similarDirection(rc, dir));
        } else if (!adjacentToWell(rc, rc.getLocation())) {
            // just try moving away from HQ
            MapLocation nearestHq = Util.pickNearest(rc, Comms.getHqs(rc));
            tryMove(directionAway(rc, nearestHq));
            tryMove(directionAway(rc, nearestHq));
        }
    }

    private static void handleBlacklist(MapLocation target, EntityType entityType) {
        if (currentTarget == null || !currentTarget.equals(target)) {
            currentTarget = target;
            timeRemaining = entityType.blacklistBaseTimer + (int)(2 * Math.sqrt(rc.getLocation().distanceSquaredTo(target)));
        } else if (--timeRemaining == 0) {
            blacklist[target.x][target.y] = rc.getRoundNum() + entityType.blacklistLength;
//            System.out.println("Blacklisted " + target);
        }
    }

    private static void whitelistAdjacent() {
        int x = rc.getLocation().x, y = rc.getLocation().y;

        if (0 < x) {
            if (0 < y) blacklist[x - 1][y - 1] = 0;
            blacklist[x - 1][y] = 0;
            if (y + 1 < rc.getMapHeight()) blacklist[x - 1][y + 1] = 0;
        }

        if (0 < y) blacklist[x][y - 1] = 0;
        blacklist[x][y] = 0;
        if (y + 1 < rc.getMapHeight()) blacklist[x][y + 1] = 0;

        if (x + 1 < rc.getMapWidth()) {
            if (0 < y) blacklist[x + 1][y - 1] = 0;
            blacklist[x + 1][y] = 0;
            if (y + 1 < rc.getMapHeight()) blacklist[x + 1][y + 1] = 0;
        }
    }

    private static void dieIfStuck() {  // desperate times call for desperate measures
        if (rc.getRoundNum() - lastMoveOrAction >= 100 + getCurrentResources() * 20 + rc.getNumAnchors(Anchor.STANDARD) * 150) rc.disintegrate();
    }

    private static void resetBlacklistTimer(EntityType entityType) {
        timeRemaining = entityType.blacklistBaseTimer;
    }

    private static int getCurrentResources() {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.ELIXIR) + rc.getResourceAmount(ResourceType.MANA);
    }

    @SuppressWarnings("unused")
    public static void debugBytecode(String s) {
        if (rc.getID() == 12606 && rc.getRoundNum() <= 60) System.out.println(s + ": " + Clock.getBytecodeNum());
    }
}
