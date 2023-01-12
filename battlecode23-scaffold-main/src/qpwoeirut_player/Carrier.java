package qpwoeirut_player;

import battlecode.common.*;
import qpwoeirut_player.common.Communications;
import qpwoeirut_player.common.TileType;
import qpwoeirut_player.utilities.Util;

import static battlecode.common.GameConstants.MAP_MAX_HEIGHT;
import static battlecode.common.GameConstants.MAP_MAX_WIDTH;
import static qpwoeirut_player.common.Pathfinding.moveToward;
import static qpwoeirut_player.common.Pathfinding.moveWhileStayingAdjacent;
import static qpwoeirut_player.utilities.Util.*;


public class Carrier extends BaseBot {
    private static final int CAPACITY = 39;  // using 39 helps us move faster

    // some wells or HQs have so many carriers already that we need to direct this carrier to a different one
    // blacklist[x][y] = t --> don't use (x, y) as a target until round t
    private static final int[][] blacklist = new int[MAP_MAX_WIDTH][MAP_MAX_HEIGHT];
    private static MapLocation currentTarget = null;
    private static int timeRemaining = 100;

    public Carrier(RobotController rc) {
        super(rc);
    }

    @Override
    public void processRound() throws GameActionException {
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        for (WellInfo wellInfo : nearbyWells) {
            Communications.addWell(rc, wellInfo.getMapLocation());
        }

        if ((getCurrentResources() > 0 && adjacentToHeadquarters(rc, rc.getLocation())) || getCurrentResources() >= 39) {
            returnResources();
            rc.setIndicatorString("returning");
        } else {
            collectResources();
        }
    }

    private static void collectResources() throws GameActionException {
        MapLocation targetWell = Util.pickNearest(rc, Communications.getKnownWells(rc), blacklist);
        if (targetWell == null) return;
        handleBlacklist(targetWell, TileType.WELL);

//        Direction dir = pickDirectionForCollection(rc, targetWell);
        Direction dir;
        if (adjacentToWell(rc, rc.getLocation())) {
            resetBlacklistTimer(TileType.WELL);
            dir = moveWhileStayingAdjacent(rc, targetWell);
        } else if (timeRemaining <= TileType.WELL.randomMoveCutoff && timeRemaining % TileType.WELL.randomMovePeriod == 0) {
            dir = randomDirection(rc);
        } else {
            dir = moveToward(rc, targetWell);
        }
        if (tryMove(dir)) {
            tryMove(similarDirection(rc, dir));
        } else if (!adjacentToWell(rc, rc.getLocation())) {
            // just try moving away from HQ
            MapLocation nearestHq = Util.pickNearest(rc, Communications.getHqs(rc), blacklist);
            tryMove(directionAway(rc, nearestHq));
            tryMove(directionAway(rc, nearestHq));
        }

        if (rc.canSenseLocation(targetWell)) {
            int toCollect = Math.min(CAPACITY - getCurrentResources(), rc.senseWell(targetWell).getRate());
            if (rc.canCollectResource(targetWell, toCollect)) {
                rc.setIndicatorString("Collecting " + toCollect);
                rc.collectResource(targetWell, toCollect);
            } else {
                rc.setIndicatorString("Could not collect " + toCollect);
            }
        } else rc.setIndicatorString("Could not sense " + targetWell);
    }

    private static void returnResources() throws GameActionException {
        MapLocation targetHq = Util.pickNearest(rc, Communications.getHqs(rc), blacklist);
        if (targetHq == null) return;
        handleBlacklist(targetHq, TileType.HQ);

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

    private static int getCurrentCapacity() {
        return rc.getAnchor() != null ? 40 : getCurrentResources();
    }
}
