package qpwoeirut_player;

import battlecode.common.*;
import qpwoeirut_player.common.SpreadSettings;

import static qpwoeirut_player.common.Pathfinding.spreadOut;
import static qpwoeirut_player.utilities.Util.adjacentToHeadquarters;
import static qpwoeirut_player.utilities.Util.adjacentToWell;

public class Launcher extends BaseBot {
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

        Direction dir = spreadOut(rc, 0, 0, SpreadSettings.LAUNCHER);
        // maintain space for carriers
        if (rc.canMove(dir) &&
                !adjacentToHeadquarters(rc, rc.getLocation().add(dir)) && !adjacentToWell(rc, rc.getLocation().add(dir))) {
            rc.move(dir);
        }
    }
}
