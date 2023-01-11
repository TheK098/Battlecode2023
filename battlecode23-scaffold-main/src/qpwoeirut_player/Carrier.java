package qpwoeirut_player;

import battlecode.common.*;
import qpwoeirut_player.utilities.FastRandom;
import qpwoeirut_player.utilities.Util;

import static qpwoeirut_player.common.Pathfinding.DIRECTIONS;
import static qpwoeirut_player.common.Pathfinding.spreadOut;


public class Carrier extends BaseBot {
    private static final int CAPACITY = 40;
    private static Direction fallbackDirection = DIRECTIONS[FastRandom.nextInt(DIRECTIONS.length)];

    public Carrier(RobotController rc) {
        super(rc);
    }

    @Override
    public void processRound() throws GameActionException {
        if (!capacityFull()) {
            collectResources();
        } else {
            returnResources();
        }
    }

    // TODO: fix potential issue where carriers clump around well/HQ and block others from leaving

    private static void collectResources() throws GameActionException {
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        for (WellInfo wellInfo : nearbyWells) {
            comms.addWell(rc, wellInfo.getMapLocation());
        }

        MapLocation[] knownWells = comms.getKnownWells(rc);
        MapLocation targetWell = Util.pickNearest(rc.getLocation(), knownWells);
        Direction dir = spreadOut(rc, targetWell);
        if (dir == Direction.CENTER) {
            if (FastRandom.nextInt(10) == 0) fallbackDirection = fallbackDirection.rotateLeft();
            if (FastRandom.nextInt(10) == 0) fallbackDirection = fallbackDirection.rotateRight();
            dir = fallbackDirection;
        }
        if (rc.canMove(dir)) rc.move(dir);
        else if (rc.canMove(dir.rotateLeft())) rc.move(dir.rotateLeft());
        else if (rc.canMove(dir.rotateRight())) rc.move(dir.rotateRight());

        if (rc.canCollectResource(targetWell, -1)) {
            rc.collectResource(targetWell, -1);
        }
    }

    private static void returnResources() throws GameActionException {
        MapLocation targetHq = Util.pickNearest(rc.getLocation(), comms.getHqs(rc));
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
            Direction dir = spreadOut(rc, targetHq);
//            rc.setIndicatorString(dir.toString());
            if (rc.canMove(dir)) rc.move(dir);
        }
    }

    private static boolean capacityFull() {
        return rc.getAnchor() != null || rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.ELIXIR) + rc.getResourceAmount(ResourceType.MANA) == CAPACITY;
    }
}
