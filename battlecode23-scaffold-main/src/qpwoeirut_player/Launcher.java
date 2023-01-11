package qpwoeirut_player;

import battlecode.common.*;

import static qpwoeirut_player.common.Pathfinding.spreadOut;

public class Launcher extends BaseBot{
    public Launcher(RobotController rc) {
        super(rc);
    }

    @Override
    public void processRound() throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.setIndicatorString("Attacking");
                rc.attack(toAttack);
            }
        }

        // Also try to move randomly.
        Direction dir = spreadOut(rc, null);
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
