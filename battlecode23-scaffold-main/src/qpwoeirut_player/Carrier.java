package qpwoeirut_player;

import battlecode.common.*;
import qpwoeirut_player.common.Communications;
import qpwoeirut_player.common.SpreadSettings;
import qpwoeirut_player.common.TileType;
import qpwoeirut_player.utilities.Util;

import java.util.Arrays;

import static qpwoeirut_player.common.Pathfinding.*;
import static qpwoeirut_player.utilities.Util.*;


public class Carrier extends BaseBot {
    private static final int CAPACITY = 39;  // using 39 helps us move faster

    // some wells or HQs have so many carriers already that we need to direct this carrier to a different one
    // blacklist[x][y] = t --> don't use (x, y) as a target until round t
    private static int[][] blacklist;
    private static MapLocation currentTarget = null;
    private static int timeRemaining = 100;

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
//        debugBytecode("1.0");

        boolean shouldReturn = handleCombat();
        if (shouldReturn) return;

        if (rc.getID() % 3 == 0 && itsAnchorTime()) {
            handleAnchor();
        } else if ((getCurrentResources() > 0 && adjacentToHeadquarters(rc, rc.getLocation())) || getCurrentResources() >= 39) {
            returnResources();
            rc.setIndicatorString("returning");
        } else {
            collectResources();
        }
//        debugBytecode("1.1");
    }

    private static boolean handleCombat() throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemies.length > 0) {
            RobotInfo nearestEnemy = pickNearest(rc, enemies);
            assert nearestEnemy != null;
            boolean launchersNearby = false;
            for (RobotInfo enemy : enemies) launchersNearby |= enemy.type == RobotType.LAUNCHER;

            // attack if possible; otherwise run if launchers nearby
            Direction toward = directionToward(rc, nearestEnemy.location);
            if (getCurrentResources() >= Math.max(5, rc.getHealth()) &&
                    rc.getLocation().add(toward).isWithinDistanceSquared(nearestEnemy.location, rc.getType().actionRadiusSquared)) {
                if (!rc.canAttack(nearestEnemy.location)) {
                    tryMove(toward);
                }
                if (rc.canAttack(nearestEnemy.location)) rc.attack(nearestEnemy.location);
                tryMove(directionAway(rc, nearestEnemy.location));
                rc.setIndicatorString("Attacking " + nearestEnemy.location);
                return true;
            } else if (launchersNearby) {
                tryMove(directionAway(rc, nearestEnemy.location));  // assume enemies are in same direction
                rc.setIndicatorString("Running away from " + nearestEnemy.location);
                return true;
            }
        }
         return false;
    }

    private static void handleAnchor() throws GameActionException {
        if (rc.getAnchor() == null) {  // need to pick up an anchor
            MapLocation targetHq = Util.pickNearest(rc, Communications.getHqs(rc), blacklist);
            moveTowardHeadquarters(targetHq);
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
            }
            if (!foundTargetIsland) {
                // spread out from other anchor bots
                tryMove(spreadOut(rc, SpreadSettings.CARRIER_ANCHOR));
            }
        }
    }

    private static void collectResources() throws GameActionException {
//        debugBytecode("2.0");
        MapLocation targetWell = Util.pickNearest(rc, Communications.getKnownWells(rc), blacklist);
        if (targetWell == null) return;
        handleBlacklist(targetWell, TileType.WELL);

//        Direction dir = pickDirectionForCollection(rc, targetWell);
        moveTowardsWell(targetWell);

        if (rc.canSenseLocation(targetWell)) {
            int toCollect = Math.min(CAPACITY - getCurrentResources(), rc.senseWell(targetWell).getRate());
            if (rc.canCollectResource(targetWell, toCollect)) {
                rc.setIndicatorString("Collecting " + toCollect);
                rc.collectResource(targetWell, toCollect);
            } else {
                rc.setIndicatorString("Could not collect " + toCollect);
            }
        } else rc.setIndicatorString("Could not sense " + targetWell);

//        debugBytecode("2.2");
    }

    private static void returnResources() throws GameActionException {
//        debugBytecode("3.0");

        MapLocation targetHq = Util.pickNearest(rc, Communications.getHqs(rc), blacklist);
        if (targetHq != null) {
            handleBlacklist(targetHq, TileType.HQ);

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
            resetBlacklistTimer(TileType.HQ);
            dir = moveWhileStayingAdjacent(rc, targetHq);
        } else if (timeRemaining <= TileType.HQ.randomMoveCutoff && timeRemaining % TileType.HQ.randomMovePeriod == 0) {
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
            resetBlacklistTimer(TileType.WELL);
            dir = moveWhileStayingAdjacent(rc, targetWell);
        } else if (timeRemaining <= TileType.WELL.randomMoveCutoff && timeRemaining % TileType.WELL.randomMovePeriod == 0) {
            dir = randomDirection(rc);
        } else {
            dir = moveToward(rc, targetWell);
        }

//        debugBytecode("2.1");

        if (tryMove(dir)) {
            tryMove(similarDirection(rc, dir));
        } else if (!adjacentToWell(rc, rc.getLocation())) {
            // just try moving away from HQ
            MapLocation nearestHq = Util.pickNearest(rc, Communications.getHqs(rc));
            tryMove(directionAway(rc, nearestHq));
            tryMove(directionAway(rc, nearestHq));
        }
    }

    private static void handleBlacklist(MapLocation target, TileType tileType) {
        if (currentTarget == null || !currentTarget.equals(target)) {
            currentTarget = target;
            timeRemaining = tileType.blacklistTimer;
        } else if (--timeRemaining == 0) {
            blacklist[target.x][target.y] = rc.getRoundNum() + tileType.blacklistLength;
//            System.out.println("Blacklisted " + target);
        }
    }

    private static void resetBlacklistTimer(TileType tileType) {
        timeRemaining = tileType.blacklistTimer;
    }

    private static int getCurrentResources() {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.ELIXIR) + rc.getResourceAmount(ResourceType.MANA);
    }

    private static int getCurrentCapacity() throws GameActionException {
        return rc.getAnchor() != null ? 40 : getCurrentResources();
    }

    public static void debugBytecode(String s) {
        if (rc.getID() == 12141 && rc.getRoundNum() <= 60) System.out.println(s + ": " + Clock.getBytecodeNum());
    }
}
