package plswork;

import battlecode.common.*;
import static plswork.RobotPlayer.*;

public class Launcher {

    static void runLauncher(RobotController rc) throws GameActionException {
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
        MapLocation currLoc = rc.getLocation();
        WellInfo[] wells = rc.senseNearbyWells();
        if (wells.length > 0) {
            // TODO: sort by dist to well
            rc.setIndicatorString(" num wells near me " + wells.length);
            WellInfo well_one = wells[0];
            currLoc = rc.getLocation();
            Direction dir = currLoc.directionTo(well_one.getMapLocation().add(Direction.NORTHEAST).add(Direction.NORTHEAST));
            if (rc.canMove(dir)) {
                rc.move(dir);
            } else if (rc.canMove(dir.rotateLeft())) {
                rc.move(dir.rotateLeft());

            } else if (rc.canMove(dir.rotateRight())) {
                rc.move(dir.rotateRight());

            } else {
                dir = directions[rng.nextInt(directions.length)];
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        }
        else {
//              If we don't see anything nearby go randomly TODO: optimize this to go towards unexplored areas
            Direction dir = directions[rng.nextInt(directions.length)];
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }
    }
}
