package qpwoeirut_player;

import battlecode.common.*;
import qpwoeirut_player.utilities.FastRandom;

import static qpwoeirut_player.utilities.Util.adjacentToHeadquarters;
import static qpwoeirut_player.utilities.Util.adjacentToWell;

public class Launcher extends BaseBot{
    public Launcher(RobotController rc) {
        super(rc);
    }

    @Override
    public void processRound() throws GameActionException {
        int radius = rc.getType().actionRadiusSquared;
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, rc.getTeam().opponent());
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.setIndicatorString("Attacking");
                rc.attack(toAttack);
            }
        }

        Direction dir = pickDirection(rc);
        // maintain space for carriers
        if (rc.canMove(dir) &&
                !adjacentToHeadquarters(rc, rc.getLocation().add(dir)) && !adjacentToWell(rc, rc.getLocation().add(dir))) {
            rc.move(dir);
        }
    }
    
    private static final int ALLY_DISTANCE_CUTOFF = 30;
    private static final int ALLY_DISTANCE_DIVISOR = 10;
    private static final int RANDOM_CUTOFF = 50;

    /**
     * Move the bot away from other allied launchers
     * @return recommended direction
     */
    public static Direction pickDirection(RobotController rc) throws GameActionException {
        int weightX = 0;
        int weightY = 0;
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
}
