package qpwoeirut_player;

import battlecode.common.*;
import qpwoeirut_player.utilities.FastRandom;

public class Launcher {
    private static RobotController rc;

    public static void initialize(RobotController robotController) {
        Launcher.rc = robotController;
    }

    public static void processRound() throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length >= 0) {
            // MapLocation toAttack = enemies[0].location;
            MapLocation toAttack = rc.getLocation().add(Direction.EAST);

            if (rc.canAttack(toAttack)) {
                rc.setIndicatorString("Attacking");
                rc.attack(toAttack);
            }
        }

        // Also try to move randomly.
        Direction dir = Direction.allDirections()[FastRandom.nextInt(Direction.allDirections().length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
