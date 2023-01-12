package qpwoeirut_player;

import battlecode.common.*;
import qpwoeirut_player.common.SpreadSettings;
import qpwoeirut_player.utilities.IntHashMap;

import static qpwoeirut_player.common.Pathfinding.spreadOut;
import static qpwoeirut_player.utilities.Util.*;

public class Launcher extends BaseBot {
    private static final IntHashMap allyHealth = new IntHashMap(30);
    private static int allyToFollow = -1;
    private static int allyFollowTimer = 0;
    private static final int ALLY_FOLLOW_TIME = 8;

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
            if (rc.canAttack(toAttack)) {  // TODO: hide in a cloud?
                rc.attack(toAttack);
                Direction dir = directionAway(rc, toAttack);
                tryMove(dir);
                rc.setIndicatorString("Attack and run");
            } else {
                Direction dir = directionToward(rc, toAttack);
                tryMove(dir);
                if (rc.canAttack(toAttack)) {
                    rc.attack(toAttack);
                }
                rc.setIndicatorString("Charge and attack");
            }
        } else {
            if (allyToFollow != -1 && (--allyFollowTimer == 0 || !rc.canSenseRobot(allyToFollow))) allyToFollow = -1;
            if (allyToFollow == -1) {  // check if any allies were recently hurt
                RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
                for (RobotInfo ally: allies) {
                    if (allyHealth.get(ally.ID) > ally.health) {
                        allyToFollow = ally.ID;
                        allyFollowTimer = ALLY_FOLLOW_TIME;
                    }
                    allyHealth.put(ally.ID, ally.health);
                }
            }

            if (allyToFollow != -1) {  // support hurt ally
                tryMove(directionToward(rc, rc.senseRobot(allyToFollow).location));
                rc.setIndicatorString("Trying to help" + allyToFollow);
            } else {  // TODO: try to put carriers between this launcher and nearest HQ
                // for now, just spread out
                Direction dir = spreadOut(rc, 0, 0, SpreadSettings.LAUNCHER);
                // maintain space for carriers
                if (rc.canMove(dir) && !adjacentToHeadquarters(rc, rc.getLocation().add(dir)) && !adjacentToWell(rc, rc.getLocation().add(dir))) {
                    rc.move(dir);
                }
//                rc.setIndicatorString("Spreading out");
            }
        }
    }
}
