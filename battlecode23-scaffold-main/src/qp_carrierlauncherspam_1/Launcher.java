package qp_carrierlauncherspam_1;

import battlecode.common.*;
import qp_carrierlauncherspam_1.common.SpreadSettings;

import static qp_carrierlauncherspam_1.common.Pathfinding.spreadOut;
import static qp_carrierlauncherspam_1.utilities.Util.adjacentToHeadquarters;
import static qp_carrierlauncherspam_1.utilities.Util.adjacentToWell;

public class Launcher extends BaseBot {
    public Launcher(RobotController rc) {
        super(rc);
    }

    @Override
    public void processRound() throws GameActionException {
        int actionRadius = rc.getType().actionRadiusSquared;
        int closestDist = 3600;
        RobotInfo target = null;
        for (RobotInfo enemy: rc.senseNearbyRobots(actionRadius, rc.getTeam().opponent())) {
            if (enemy.type != RobotType.HEADQUARTERS && closestDist > rc.getLocation().distanceSquaredTo(enemy.location)) {
                closestDist = rc.getLocation().distanceSquaredTo(enemy.location);
                target = enemy;
            }
        }
        if (target != null) {
            MapLocation toAttack = target.location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
                Direction dir = rc.getLocation().directionTo(target.location).opposite();
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
                rc.setIndicatorString("Attack and run");
            } else {
                Direction dir = rc.getLocation().directionTo(target.location);
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    rc.attack(toAttack);
                }
                rc.setIndicatorString("Charge and attack");
            }
        }

        Direction dir = spreadOut(rc, 0, 0, SpreadSettings.LAUNCHER);
        // maintain space for carriers
        if (rc.canMove(dir) && !adjacentToHeadquarters(rc, rc.getLocation().add(dir)) && !adjacentToWell(rc, rc.getLocation().add(dir))) {
            rc.move(dir);
        }
    }
}