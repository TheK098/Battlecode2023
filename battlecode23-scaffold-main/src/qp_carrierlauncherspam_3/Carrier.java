package qp_carrierlauncherspam_3;

import battlecode.common.*;
import qp_carrierlauncherspam_3.common.Communications;
import qp_carrierlauncherspam_3.common.SpreadSettings;
import qp_carrierlauncherspam_3.common.EntityType;
import qp_carrierlauncherspam_3.utilities.Util;

import static qp_carrierlauncherspam_3.common.Pathfinding.*;
import static qp_carrierlauncherspam_3.utilities.Util.*;


public class Carrier extends BaseBot {
    private static final int CAPACITY = 39;  // using 39 helps us move faster

    // some wells or HQs have so many carriers already that we need to direct this carrier to a different one
    // blacklist[x][y] = t --> don't use (x, y) as a target until round t
    private static int[][] blacklist;
    private static MapLocation currentTarget = null;
    private static int timeRemaining = 100;
    private static MapLocation enemySighting = null;

    public Carrier(RobotController rc) {
        super(rc);
        blacklist = new int[rc.getMapWidth()][rc.getMapHeight()];
    }

    @Override
    public void processRound() throws GameActionException {
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        for (WellInfo wellInfo : nearbyWells) {
            Communications.addWell(rc, wellInfo.getMapLocation());
        }
        if (enemySighting != null && Communications.reportEnemySighting(rc, enemySighting)) {
            enemySighting = null;
        }

        // deal with issue where carriers with resources sometimes end up next to blacklisted headquarters
        for (Direction dir: Direction.allDirections()) {
            MapLocation loc = rc.getLocation().add(dir);
            if (rc.onTheMap(loc)) blacklist[loc.x][loc.y] = 0;
        }

//        debugBytecode("1.0");

        boolean shouldReturn = handleCombat();
        if (shouldReturn) return;

        if (rc.getID() % 4 == 0 && itsAnchorTime()) {
            if (getCurrentResources() > 0) returnResources();
            else handleAnchor();
        } else if ((getCurrentResources() > 0 && adjacentToHeadquarters(rc, rc.getLocation())) || getCurrentResources() >= 39) {
            returnResources();
        } else {
            collectResources();
        }
//        debugBytecode("1.1");
    }

    private static boolean handleCombat() throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemies.length > 0) {
            RobotInfo nearestEnemy = pickNearest(rc, enemies, false);
            if (nearestEnemy == null) return false;  // only visible enemy was HQ
            if (!Communications.reportEnemySighting(rc, nearestEnemy.location)) enemySighting = nearestEnemy.location;

            boolean onlyCarriersNearby = true;
            for (RobotInfo enemy : enemies) onlyCarriersNearby &= enemy.type == RobotType.CARRIER;

            if (onlyCarriersNearby) return false;  // continue as normal, report enemy location when depositing resources

            // dump resources in an attack and then run away
            // TODO: prioritize attacking launchers
            Direction toward = directionToward(rc, nearestEnemy.location);
            MapLocation closer = rc.getLocation().add(toward);
            if (!rc.canAttack(nearestEnemy.location) && closer.isWithinDistanceSquared(nearestEnemy.location, rc.getType().actionRadiusSquared)) {
                tryMove(toward);
            }
            if (rc.canAttack(nearestEnemy.location)) {
                rc.attack(nearestEnemy.location);
                rc.setIndicatorString("Attacking " + nearestEnemy.location);
            }
            else if (rc.canAttack(closer)) {
                rc.attack(closer);  // just toss the resources so we can move faster
                rc.setIndicatorString("Tossing resources to " + closer);
            }

            MapLocation nearestHq = Util.pickNearest(rc, Communications.getHqs(rc));
            assert nearestHq != null;
            tryMove(directionToward(rc, nearestHq));
            tryMove(directionToward(rc, nearestHq));
            return true;
        }
         return false;
    }

    private static void handleAnchor() throws GameActionException {
        if (rc.getAnchor() == null) {  // need to pick up an anchor
            MapLocation targetHq = Util.pickNearest(rc, Communications.getHqs(rc), blacklist);
            moveTowardHeadquarters(targetHq);
            rc.setIndicatorString("Waiting for anchor from " + targetHq);
            if (rc.canTakeAnchor(targetHq, Anchor.STANDARD)) {
                rc.takeAnchor(targetHq, Anchor.STANDARD);
            }
        }
        if (rc.getAnchor() == Anchor.STANDARD) {
            int[] islands = rc.senseNearbyIslands();  // TODO: add comms to make anchoring more efficient
            boolean foundTargetIsland = false;
            if (islands.length > 0) {
                MapLocation irrelevantLocation = new MapLocation(-10000, -10000);
                assert !rc.getLocation().isWithinDistanceSquared(irrelevantLocation, INF_DIST);

                MapLocation[] nearestIslandLocations = new MapLocation[islands.length];
                for (int i = islands.length; i --> 0;) {
                    nearestIslandLocations[i] = (rc.senseTeamOccupyingIsland(islands[i]) != rc.getTeam()) ?
                         pickNearest(rc, rc.senseNearbyIslandLocations(islands[i])) : irrelevantLocation;
                }
                MapLocation target = pickNearest(rc, nearestIslandLocations);
                if (target != null) {
                    tryMove(directionToward(rc, target));  // no-op if we're at target already
                    if (rc.getLocation().equals(target) && rc.canPlaceAnchor()) rc.placeAnchor();
                    foundTargetIsland = true;
                }
                rc.setIndicatorString("Trying to place anchor at " + target);
            }
            if (!foundTargetIsland) {
                // spread out from other anchor bots
                tryMove(spreadOut(rc, 0, 0, SpreadSettings.CARRIER_ANCHOR));
                rc.setIndicatorString("Spreading out with anchor");
            }
        }
    }

    private static void collectResources() throws GameActionException {
//        debugBytecode("2.0");
        MapLocation targetWell = Util.pickNearest(rc, Communications.getKnownWells(rc), blacklist);
        if (targetWell == null) return;
        handleBlacklist(targetWell, EntityType.WELL);

//        Direction dir = pickDirectionForCollection(rc, targetWell);
        moveTowardsWell(targetWell);

        if (rc.canSenseLocation(targetWell)) {
            int toCollect = Math.min(CAPACITY - getCurrentResources(), rc.senseWell(targetWell).getRate());
            if (rc.canCollectResource(targetWell, toCollect)) {
                rc.setIndicatorString("Collecting " + toCollect + " from " + targetWell);
                rc.collectResource(targetWell, toCollect);
            } else {
                rc.setIndicatorString("Could not collect " + toCollect + " from " + targetWell);
            }
        } else rc.setIndicatorString("Could not sense " + targetWell);

//        debugBytecode("2.2");
    }

    private static void returnResources() throws GameActionException {
//        debugBytecode("3.0");

        MapLocation targetHq = Util.pickNearest(rc, Communications.getHqs(rc), blacklist);
        rc.setIndicatorString("Returning to " + targetHq);
        if (targetHq != null) {
            handleBlacklist(targetHq, EntityType.HQ);

//            debugBytecode("3.1");
            moveTowardHeadquarters(targetHq);
//            debugBytecode("3.2");

            int adamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
            int elixir = rc.getResourceAmount(ResourceType.ELIXIR);
            int mana = rc.getResourceAmount(ResourceType.MANA);
            if (adamantium > 0 && rc.canTransferResource(targetHq, ResourceType.ADAMANTIUM, adamantium)) {
                rc.transferResource(targetHq, ResourceType.ADAMANTIUM, adamantium);
            } else if (elixir > 0 && rc.canTransferResource(targetHq, ResourceType.ELIXIR, elixir)) {
                rc.transferResource(targetHq, ResourceType.ELIXIR, elixir);
            } else if (mana > 0 && rc.canTransferResource(targetHq, ResourceType.MANA, mana)) {
                rc.transferResource(targetHq, ResourceType.MANA, mana);
            }
//            debugBytecode("3.3");
        } else {
            // this will only happen when we can't reach any of the HQs, so let's move away to try and free up space
            MapLocation nearestHq = Util.pickNearest(rc, Communications.getHqs(rc));
            assert nearestHq != null;
            if (rc.getLocation().isWithinDistanceSquared(nearestHq, 16)) {
                tryMove(directionAway(rc, nearestHq));
            }
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
            dir = moveToward(rc, targetHq);
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
            dir = moveToward(rc, targetWell);
        }

//        debugBytecode("2.1");

        if (tryMove(dir)) {
            if (!adjacentToWell(rc, rc.getLocation())) tryMove(similarDirection(rc, dir));
        } else if (!adjacentToWell(rc, rc.getLocation())) {
            // just try moving away from HQ
            MapLocation nearestHq = Util.pickNearest(rc, Communications.getHqs(rc));
            tryMove(directionAway(rc, nearestHq));
            tryMove(directionAway(rc, nearestHq));
        }
    }

    private static void handleBlacklist(MapLocation target, EntityType entityType) {
        if (currentTarget == null || !currentTarget.equals(target)) {
            currentTarget = target;
            timeRemaining = entityType.blacklistTimer;
        } else if (--timeRemaining == 0) {
            blacklist[target.x][target.y] = rc.getRoundNum() + entityType.blacklistLength;
//            System.out.println("Blacklisted " + target);
        }
    }

    private static void resetBlacklistTimer(EntityType entityType) {
        timeRemaining = entityType.blacklistTimer;
    }

    private static int getCurrentResources() {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.ELIXIR) + rc.getResourceAmount(ResourceType.MANA);
    }

    @SuppressWarnings("unused")
    public static void debugBytecode(String s) {
        if (rc.getID() == 12141 && rc.getRoundNum() <= 60) System.out.println(s + ": " + Clock.getBytecodeNum());
    }
}
