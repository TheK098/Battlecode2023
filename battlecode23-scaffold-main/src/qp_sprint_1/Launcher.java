package qp_sprint_1;

import battlecode.common.*;
import qp_sprint_1.common.Communications;
import qp_sprint_1.common.Communications.EnemySighting;
import qp_sprint_1.common.SpreadSettings;
import qp_sprint_1.utilities.FastRandom;
import qp_sprint_1.utilities.IntHashMap;
import qp_sprint_1.utilities.Util;

import static qp_sprint_1.common.Pathfinding.spreadOut;
import static qp_sprint_1.utilities.Util.*;

public class Launcher extends BaseBot {
    private static final IntHashMap allyHealth = new IntHashMap(20);
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
//                    rc.setIndicatorString("Charged " + toAttack + " with direction " + dir);
                } else {
//                    rc.setIndicatorString("Charging " + toAttack + " with direction " + dir + " didn't work");
                }
            }
        } else {
            int allyToFollowLocal = allyToFollow;
            if (allyToFollowLocal != -1 && (--allyFollowTimer == 0 || !rc.canSenseRobot(allyToFollowLocal))) allyToFollowLocal = -1;
            if (allyToFollowLocal == -1) {  // check if any allies were recently hurt
                RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
                int added = 0;
                for (int i = allies.length; i --> 0;) {
                    int storedHealth = allyHealth.get(allies[i].ID);
                    if (allies[i].health < storedHealth) {  // ally took damage, go help them
                        allyToFollowLocal = allies[i].ID;
                        allyFollowTimer = ALLY_FOLLOW_TIME;
                        allyHealth.put(allies[i].ID, allies[i].health);
                    } else if (storedHealth == 0 && added++ <= 20) {  // don't add too many in one round for bytecode reasons
                        // haven't seen this ally before, record their health
                        allyHealth.put(allies[i].ID, allies[i].health);
                    }
                }
            }
            allyToFollow = allyToFollowLocal;

            if (allyToFollowLocal != -1) {  // support hurt ally
                tryMove(directionToward(rc, rc.senseRobot(allyToFollowLocal).location));
//                rc.setIndicatorString("Trying to help " + allyToFollowLocal);
            } else {
                MapLocation enemyIsland = itsAnchorTime() ? findNearestIslandLocation(rc.getTeam().opponent()) : null;
                if (enemyIsland != null) {  // find nearest enemy island and kill it
                    tryMove(directionToward(rc, enemyIsland));
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
                            }
                        }
                    }
                    float weightX = 0, weightY = 0;
                    if (targetIdx != -1) {
                        weightX = targetScore * (enemySightings[targetIdx].location.x - rc.getLocation().x);
                        weightY = targetScore * (enemySightings[targetIdx].location.y - rc.getLocation().y);
//                        rc.setIndicatorString(targetScore + " " + enemySightings[targetIdx] + " " + weightX + " " + weightY);
                    } //else rc.setIndicatorString("Spreading out");
                    // move towards target if exists and spread out
                    Direction dir = spreadOut(rc, weightX, weightY, SpreadSettings.LAUNCHER);
                    MapLocation newLoc = rc.getLocation().add(dir);

                    RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
                    // maintain space for carriers
                    if (adjacentToHeadquarters(rc, newLoc) && allies.length >= 16 && FastRandom.nextInt(8) != 0)
                        tryMove(directionAway(rc, Util.pickNearest(rc, Communications.getHqs(rc))));
                    else if (adjacentToWell(rc, newLoc) && allies.length >= 16 && FastRandom.nextInt(8) != 0)
                        if (rc.senseWell(rc.getLocation()) != null) tryMove(randomDirection(rc));
                        else tryMove(directionAway(rc, pickNearest(rc, Communications.getKnownWells(rc)).location));
                    else tryMove(dir);  // do tryMove because a round may have passed from running out of bytecode
//                rc.setIndicatorString("Spreading out");
                }
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
            RobotInfo[] enemies = rc.senseNearbyRobots(16, rc.getTeam().opponent());
            RobotInfo target = null;
            for (int i = enemies.length; i --> 0;) {
                if (canAttack(enemies[i].location) && enemies[i].type != RobotType.HEADQUARTERS) target = target == null ? enemies[i] : chooseTarget(target, enemies[i]);
            }
            return target;
        }
        return null;
    }

    private static boolean canAttack(MapLocation location) throws GameActionException {
        if (rc.isMovementReady()) {
            MapLocation closer = rc.getLocation().add(directionTowardImmediate(rc, location));
            if (closer.isWithinDistanceSquared(location, 16)) return true;
        }
        return rc.getLocation().isWithinDistanceSquared(location, 16);
    }

    // assume both robots are within attacking range
    private static RobotInfo chooseTarget(RobotInfo robot1, RobotInfo robot2) {
        return (robot1.type != robot2.type) ? (robotTypePriority(robot1.type) < robotTypePriority(robot2.type) ? robot1 : robot2) :
                (robot1.health != robot2.health) ? (robot1.health < robot2.health ? robot1 : robot2) :
                        rc.getLocation().isWithinDistanceSquared(robot1.location, rc.getLocation().distanceSquaredTo(robot2.location)) ? robot1 : robot2;
        // TODO: test tiebreak by distance, distance cutoff (needing to move vs not move), tiebreak by ID
        // tiebreak by distance for now; by ID probably won't matter since each robot attacks sequentially
    }

    private static int robotTypePriority(RobotType robotType) {
        switch (robotType) {
            case LAUNCHER: return 0;
            case CARRIER: return 1;
            case AMPLIFIER: return 2;
        }
        return 3;
    }

    private static void dieIfStuck() {  // desperate times call for desperate measures
        if (rc.getRoundNum() - lastMoveOrAction >= 200) rc.disintegrate();
    }
}
