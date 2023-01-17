package qp1_1_carrierlauncherspam;

import battlecode.common.*;
import qp1_1_carrierlauncherspam.common.SpreadSettings;

import static qp1_1_carrierlauncherspam.common.Pathfinding.spreadOut;
import static qp1_1_carrierlauncherspam.utilities.Util.adjacentToHeadquarters;
import static qp1_1_carrierlauncherspam.utilities.Util.adjacentToWell;

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
