package qp1_6_carrierlauncherspam;

import battlecode.common.*;
import qp1_6_carrierlauncherspam.common.Communications;
import qp1_6_carrierlauncherspam.common.Communications.EnemySighting;
import qp1_6_carrierlauncherspam.common.SpreadSettings;
import qp1_6_carrierlauncherspam.utilities.FastRandom;
import qp1_6_carrierlauncherspam.utilities.IntHashMap;
import qp1_6_carrierlauncherspam.utilities.Util;

import static qp1_6_carrierlauncherspam.common.Pathfinding.spreadOut;
import static qp1_6_carrierlauncherspam.utilities.Util.*;

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
        RobotInfo target = pickTarget();
        if (target != null) {
            MapLocation toAttack = target.location;
            if (rc.canAttack(toAttack)) {  // TODO: hide in a cloud?
                rc.attack(toAttack);
                lastMoveOrAction = rc.getRoundNum();
                Direction dir = directionAway(rc, toAttack);
                tryMove(dir);
//                rc.setIndicatorString("Attack and run");
            } else {
                Direction dir = directionTowardImmediate(rc, toAttack);
                tryMove(dir);
                if (rc.canAttack(toAttack)) {
                    rc.attack(toAttack);
                    lastMoveOrAction = rc.getRoundNum();
                }
//                rc.setIndicatorString("Charge and attack");
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
//                rc.setIndicatorString("Trying to help " + allyToFollow);
            } else {  // TODO: try to put carriers between this launcher and nearest HQ
                EnemySighting[] enemySightings = Communications.getEnemySightings(rc);
                int targetIdx = -1;
                int targetScore = 20;
                int factor = 10 * rc.getMapWidth() * rc.getMapHeight();
                for (int i = enemySightings.length; i --> 0;) {
                    if (enemySightings[i].urgency > 0) {
                        int score = factor * enemySightings[i].urgency / Math.max(1, rc.getLocation().distanceSquaredTo(enemySightings[i].location));
                        if (targetScore < score) {
                            targetScore = score;
                            targetIdx = i;
//                            rc.setIndicatorString(score + " " + enemySightings[i].urgency + " " + enemySightings[i].location);
                        }
                    }
                }
                float weightX = 0, weightY = 0;
                if (targetIdx != -1) {
                    weightX = targetScore * (enemySightings[targetIdx].location.x - rc.getLocation().x);
                    weightY = targetScore * (enemySightings[targetIdx].location.y - rc.getLocation().y);
                } //else rc.setIndicatorString("Spreading out");
                // move towards target if exists and spread out
                Direction dir = spreadOut(rc, weightX, weightY, SpreadSettings.LAUNCHER);
                MapLocation newLoc = rc.getLocation().add(dir);
                // maintain space for carriers
                if (adjacentToHeadquarters(rc, newLoc) && FastRandom.nextInt(8) != 0)
                    tryMove(directionAway(rc, Util.pickNearest(rc, Communications.getHqs(rc))));
                else if (adjacentToWell(rc, newLoc) && FastRandom.nextInt(8) != 0)
                    if (rc.senseWell(rc.getLocation()) != null) tryMove(randomDirection(rc));
                    else tryMove(directionAway(rc, pickNearest(rc, Communications.getKnownWells(rc)).location));
                else tryMove(dir);  // do tryMove because a round may have passed from running out of bytecode
//                rc.setIndicatorString("Spreading out");
            }

            // try attacking again
            target = pickTarget();
            if (target != null) {
                if (rc.canAttack(target.location)) {
                    rc.attack(target.location);
                    lastMoveOrAction = rc.getRoundNum();
                }
            }
        }

        dieIfStuck();
    }

    private static RobotInfo pickTarget() throws GameActionException {
        if (rc.isActionReady()) {
            int actionRadius = rc.getType().actionRadiusSquared;
            RobotInfo[] enemies = rc.senseNearbyRobots(actionRadius, rc.getTeam().opponent());
            RobotInfo target = null;
            for (int i = enemies.length; i --> 0;) {
                if (canAttack(enemies[i].location)) target = target == null ? enemies[i] : chooseTarget(target, enemies[i]);
            }
            return target;
        }
        return null;
    }

    private static boolean canAttack(MapLocation location) throws GameActionException {
        if (rc.isMovementReady()) {
            MapLocation closer = rc.getLocation().add(directionTowardImmediate(rc, location));
            if (closer.isWithinDistanceSquared(location, rc.getType().actionRadiusSquared)) return true;
        }
        return rc.getLocation().isWithinDistanceSquared(location, rc.getType().actionRadiusSquared);
    }

    // assume both robots are within attacking range
    private static RobotInfo chooseTarget(RobotInfo robot1, RobotInfo robot2) {
        return (robot1.type != robot2.type) ? (robot1.type == RobotType.LAUNCHER ? robot1 : (robot2.type == RobotType.LAUNCHER ? robot2 : null)) :
                (robot1.health != robot2.health) ? (robot1.health < robot2.health ? robot1 : robot2) :
                        (robot1.ID < robot2.ID) ? robot1 : robot2;  // tiebreak by IDs instead of distance to eliminate a single robot quickly
        // TODO: test tiebreak by distance, test distance cutoff (needing to move vs not move)
    }

    private static void dieIfStuck() {  // desperate times call for desperate measures
        if (rc.getRoundNum() - lastMoveOrAction >= 200) rc.disintegrate();
    }
}
