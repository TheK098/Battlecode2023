package qpwoeirut_player;

import battlecode.common.*;
import qpwoeirut_player.common.Communications;
import qpwoeirut_player.utilities.FastRandom;
import qpwoeirut_player.utilities.Util;

import static qpwoeirut_player.common.Pathfinding.DIRECTIONS;


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
            Communications.addWell(rc, wellInfo.getMapLocation());
        }

        MapLocation[] knownWells = Communications.getKnownWells(rc);
        MapLocation targetWell = Util.pickNearest(rc.getLocation(), knownWells);
        Direction dir = pickDirection(rc, targetWell);
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
        MapLocation targetHq = Util.pickNearest(rc.getLocation(), Communications.getHqs(rc));
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
            Direction dir = pickDirection(rc, targetHq);
//            rc.setIndicatorString(dir.toString());
            if (rc.canMove(dir)) rc.move(dir);
        }
    }

    private static final int TARGET_DISTANCE_CUTOFF = 100;
    private static final int TARGET_DISTANCE_DIVISOR = 5;
    private static final int ALLY_DISTANCE_CUTOFF = 30;
    private static final int ALLY_DISTANCE_DIVISOR = 10;
    private static final int RANDOM_CUTOFF = 50;

    /**
     * Move the bot away from other allied carriers, with a stronger attraction towards a target
     * @param target location that bot wants to go to
     * @return recommended direction
     */
    public static Direction pickDirection(RobotController rc, MapLocation target) throws GameActionException {
        int weightX = 0;
        int weightY = 0;
        int distanceToTarget = rc.getLocation().distanceSquaredTo(target);
        if (distanceToTarget < TARGET_DISTANCE_CUTOFF) {
            int dx = target.x - rc.getLocation().x;
            int dy = target.y - rc.getLocation().y;
            weightX = dx * (TARGET_DISTANCE_CUTOFF - distanceToTarget) / TARGET_DISTANCE_DIVISOR;
            weightY = dy * (TARGET_DISTANCE_CUTOFF - distanceToTarget) / TARGET_DISTANCE_DIVISOR;
        }
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(ALLY_DISTANCE_CUTOFF, rc.getTeam());
        for (RobotInfo robot: nearbyRobots) {
            if (robot.type == rc.getType()) {
                int dist = rc.getLocation().distanceSquaredTo(robot.location);
                int dx = robot.location.x - rc.getLocation().x;
                int dy = robot.location.y - rc.getLocation().y;
                // subtract since we want to move away
                weightX -= dx * (ALLY_DISTANCE_CUTOFF - dist) / ALLY_DISTANCE_DIVISOR;
                weightY -= dy * (ALLY_DISTANCE_CUTOFF - dist) / ALLY_DISTANCE_DIVISOR;
            }
        }

        int finalDx = FastRandom.nextInt(RANDOM_CUTOFF + RANDOM_CUTOFF + 1) - RANDOM_CUTOFF > weightX ? -1 : 1;
        int finalDy = FastRandom.nextInt(RANDOM_CUTOFF + RANDOM_CUTOFF + 1) - RANDOM_CUTOFF > weightY ? -1 : 1;
        return new MapLocation(0, 0).directionTo(new MapLocation(finalDx, finalDy));
    }

    private static boolean capacityFull() {
        return rc.getAnchor() != null || rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.ELIXIR) + rc.getResourceAmount(ResourceType.MANA) == CAPACITY;
    }
}
