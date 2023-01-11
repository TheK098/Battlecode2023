package qpwoeirut_player;

import battlecode.common.*;
import qpwoeirut_player.common.Communications;
import qpwoeirut_player.common.SpreadSettings;
import qpwoeirut_player.common.TileType;
import qpwoeirut_player.utilities.Util;

import static battlecode.common.GameConstants.MAP_MAX_HEIGHT;
import static battlecode.common.GameConstants.MAP_MAX_WIDTH;
import static qpwoeirut_player.common.Pathfinding.*;
import static qpwoeirut_player.utilities.Util.adjacentToWell;


public class Carrier extends BaseBot {
    private static final int CAPACITY = 40;

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

        if (!capacityFull()) {
            collectResources();
        } else {
            returnResources();
        }
    }

    private static void collectResources() throws GameActionException {
        MapLocation[] knownWells = Communications.getKnownWells(rc);
        if (knownWells.length == 0) return; // just in case, to avoid throwing

        MapLocation targetWell = Util.pickNearest(rc, knownWells, blacklist);
        handleBlacklist(targetWell, TileType.WELL);

//        Direction dir = pickDirectionForCollection(rc, targetWell);
        Direction dir;
        if (adjacentToWell(rc, rc.getLocation())) {
            dir = moveWhileStayingAdjacent(rc, targetWell);
        } else {
            dir = moveToward(rc, targetWell);
        }
        if (rc.canMove(dir)) rc.move(dir);

        if (rc.canCollectResource(targetWell, -1)) {
            rc.collectResource(targetWell, -1);
        }
    }

    private static void returnResources() throws GameActionException {
        MapLocation targetHq = Util.pickNearest(rc, Communications.getHqs(rc), blacklist);
        handleBlacklist(targetHq, TileType.HQ);

        int adamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        int elixir = rc.getResourceAmount(ResourceType.ELIXIR);
        int mana = rc.getResourceAmount(ResourceType.MANA);
        if (adamantium > 0 && rc.canTransferResource(targetHq, ResourceType.ADAMANTIUM, adamantium)) {
            rc.transferResource(targetHq, ResourceType.ADAMANTIUM, adamantium);
        } else if (elixir > 0 && rc.canTransferResource(targetHq, ResourceType.ELIXIR, elixir)) {
            rc.transferResource(targetHq, ResourceType.ELIXIR, elixir);
        } else if (mana > 0 && rc.canTransferResource(targetHq, ResourceType.MANA, mana)) {
            rc.transferResource(targetHq, ResourceType.MANA, mana);
        } else {  // out of range, move closer
            Direction dir = pickDirectionForReturn(rc, targetHq);
//            rc.setIndicatorString(dir.toString());
            if (rc.canMove(dir)) rc.move(dir);
        }
    }

    private static void handleBlacklist(MapLocation target, TileType tileType) {
        rc.setIndicatorString(String.valueOf(target));
        if (currentTarget == null || !currentTarget.equals(target)) {
            currentTarget = target;
            timeRemaining = tileType.blacklistTimer;
        } else if (--timeRemaining == 0) {
            blacklist[target.x][target.y] = rc.getRoundNum() + tileType.blacklistLength;
//            System.out.println("Blacklisted " + target);
        }
    }

    private static final int TARGET_DISTANCE_CUTOFF = 400;
    private static final int TARGET_DISTANCE_DIVISOR = 2;

    /**
     * Move the bot away from other allied carriers, with a stronger attraction towards a target
     *
     * @param target location that bot wants to go to
     * @return recommended direction
     */
    public static Direction pickDirectionForCollection(RobotController rc, MapLocation target) throws GameActionException {
        int weightX = 0;
        int weightY = 0;
        int distanceToTarget = rc.getLocation().distanceSquaredTo(target);
        if (distanceToTarget < TARGET_DISTANCE_CUTOFF) {
            int dx = target.x - rc.getLocation().x;
            int dy = target.y - rc.getLocation().y;
            weightX = dx * (TARGET_DISTANCE_CUTOFF - distanceToTarget) / TARGET_DISTANCE_DIVISOR;
            weightY = dy * (TARGET_DISTANCE_CUTOFF - distanceToTarget) / TARGET_DISTANCE_DIVISOR;
        }
        return spreadOut(rc, weightX, weightY, SpreadSettings.CARRIER_COLLECTING);
    }

    /**
     * Move bot towards target, occasionally making random moves once close to target
     *
     * @param target location that bot wants to go to
     * @return recommended direction
     */
    public static Direction pickDirectionForReturn(RobotController rc, MapLocation target) throws GameActionException {
        int weightX = target.x - rc.getLocation().x;
        int weightY = target.y - rc.getLocation().y;
        return spreadOut(rc, weightX, weightY, SpreadSettings.CARRIER_RETURNING);
    }

    private static boolean capacityFull() {
        return rc.getAnchor() != null || rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.ELIXIR) + rc.getResourceAmount(ResourceType.MANA) == CAPACITY;
    }
}
