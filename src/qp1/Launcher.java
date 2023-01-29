package qp1;

import battlecode.common.*;
import qp1.communications.Comms;
import qp1.communications.Comms.EnemySighting;
import qp1.communications.Comms.IslandInfo;
import qp1.navigation.SpreadSettings;
import qp1.utilities.FastRandom;
import qp1.utilities.IntHashMap;
import qp1.utilities.Util;

import static qp1.navigation.Pathfinding.moveToward;
import static qp1.navigation.Pathfinding.spreadOut;
import static qp1.utilities.Util.*;

public class Launcher extends BaseBot {
    private static final IntHashMap allyHealth = new IntHashMap(20);
    private static int allyToFollow = -1;
    private static int allyFollowTimer = 0;
    private static final int ALLY_FOLLOW_TIME = 8;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void processRound() throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (!handleCombat(enemies)) {
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            RobotInfo nearestEnemyHq = pickNearestEnemyHq(rc, enemies);
            if (!supportAlly(enemies, allies, nearestEnemyHq)) {
                if (!attackVisibleIsland()) {
                    if (!investigateSightings()) {
                        if (!attackNearestIsland()) {
                            float weightX = (rc.getMapWidth() / 2f) - rc.getLocation().x, weightY = (rc.getMapHeight() / 2f) - rc.getLocation().y;  // default drift to center
                            rc.setIndicatorString("Spreading out");
                            Direction dir = spreadOut(rc, weightX / 10, weightY / 10, SpreadSettings.LAUNCHER);
                            MapLocation newLoc = rc.getLocation().add(dir);
                            // maintain space for carriers
                            if (adjacentToHeadquarters(rc, newLoc) && allies.length >= 16 && FastRandom.nextInt(8) != 0)
                                tryMove(directionAway(rc, Util.pickNearest(rc, Comms.getHqs(rc))));
                            else if (adjacentToWell(rc, newLoc) && allies.length >= 16 && FastRandom.nextInt(8) != 0)
                                if (rc.senseWell(rc.getLocation()) != null) tryMove(randomDirection(rc));
                                else tryMove(directionAway(rc, pickNearest(rc, Comms.getKnownWells(rc)).location));
                            else {
                                if (nearestEnemyHq != null && rc.getLocation().add(dir).isWithinDistanceSquared(nearestEnemyHq.location, RobotType.HEADQUARTERS.actionRadiusSquared))
                                    tryMove(directionAway(rc, nearestEnemyHq.location));
                                else
                                    tryMove(dir);
                            }
                        }
//                rc.setIndicatorString("Spreading out");
                    }
                }
            }
        }

        if (rc.isActionReady()) {  // try attacking again after moving
            RobotInfo target = pickTarget(rc.senseNearbyRobots(RobotType.LAUNCHER.actionRadiusSquared, rc.getTeam().opponent()));
            if (target != null && rc.canAttack(target.location)) {
                rc.attack(target.location);
                lastMoveOrAction = rc.getRoundNum();
            }
        }

        dieIfStuck();
    }

    private static boolean handleCombat(RobotInfo[] enemies) throws GameActionException {
        RobotInfo target = pickTarget(enemies);
        if (target == null) target = pickNearest(rc, enemies, false);
        if (target == null) return false;

        MapLocation toAttack = target.location;
        if (rc.canAttack(toAttack)) {
            rc.attack(toAttack);
            lastMoveOrAction = rc.getRoundNum();

            // if we're already in a cloud, the enemy can probably see us anyway so just run away
            // otherwise duck into a cloud if possible
            tryMove(rc.senseCloud(rc.getLocation()) ? directionAway(rc, toAttack) : directionAwayOrCloud(rc, toAttack));
            rc.setIndicatorString("Attack and run");
        } else {
            RobotInfo[] allies = rc.senseNearbyRobots(toAttack, 20, rc.getTeam());
            int desperationScore = 0, allyLaunchers = 0;
            for (int i = allies.length; i --> 0;) {
                desperationScore += allies[i].type == RobotType.CARRIER ? 1 : (allies[i].type == RobotType.HEADQUARTERS ? 5 : 0);
                allyLaunchers += allies[i].type == RobotType.LAUNCHER ? 1 : 0;
            }

            int enemyLaunchers = 0;
            for (int i = enemies.length; i --> 0;) enemyLaunchers += enemies[i].type == RobotType.LAUNCHER ? 1 : 0;
            if (allyLaunchers >= enemyLaunchers || desperationScore >= 3) {
                Direction dir = directionTowardImmediate(rc, toAttack);
                tryMove(dir);
                if (rc.canAttack(toAttack)) {
                    rc.attack(toAttack);
                    lastMoveOrAction = rc.getRoundNum();
                }
                rc.setIndicatorString("Fighting, desperation=" + desperationScore + ". " + allyLaunchers + " vs " + enemyLaunchers + ". Charging " + dir + " to " + toAttack);
            } else {
                tryMove(directionAway(rc, toAttack));
                rc.setIndicatorString("Retreating, outnumbered " + allyLaunchers + " vs " + enemyLaunchers);
            }
        }
        return true;
    }

    private static boolean supportAlly(RobotInfo[] enemies, RobotInfo[] allies, RobotInfo nearestEnemyHq) throws GameActionException {
        if (allyToFollow != -1 && (--allyFollowTimer == 0 || !rc.canSenseRobot(allyToFollow))) allyToFollow = -1;
        if (allyToFollow == -1) {  // check if any allies were recently hurt
            int added = 0, storedHealth;
            for (int i = allies.length; i --> 0;) {
                storedHealth = allyHealth.get(allies[i].ID);
                if (allies[i].health < storedHealth) {  // ally took damage, go help them
                    allyToFollow = allies[i].ID;
                    allyFollowTimer = ALLY_FOLLOW_TIME;
                    allyHealth.put(allies[i].ID, allies[i].health);
                } else if (storedHealth == 0 && ++added <= 15) {  // don't add too many in one round for bytecode reasons
                    // haven't seen this ally before, record their health
                    allyHealth.put(allies[i].ID, allies[i].health);
                }
            }
        }

        if (allyToFollow != -1) {  // support hurt ally
            Direction dir = directionToward(rc, rc.senseRobot(allyToFollow).location);  // no pathing, just try to be nearby
            if (enemies.length > 0 || nearestEnemyHq == null || !rc.getLocation().add(dir).isWithinDistanceSquared(nearestEnemyHq.location, RobotType.HEADQUARTERS.actionRadiusSquared))
                tryMove(dir);
            else tryMove(directionAway(rc, nearestEnemyHq.location));
            rc.setIndicatorString("Trying to help " + allyToFollow);
            return true;
        }
        return false;
    }

    private static boolean investigateSightings() throws GameActionException {  // TODO: try to put carriers between this launcher and nearest HQ
        EnemySighting[] enemySightings = Comms.getEnemySightings(rc);
        int targetIdx = -1;
        int targetScore = 10;
        int factor = rc.getMapWidth() * rc.getMapHeight();
        for (int i = enemySightings.length; i --> 0;) {
            if (enemySightings[i].urgency > 0) {
                int score = factor * enemySightings[i].urgency / Math.max(1, rc.getLocation().distanceSquaredTo(enemySightings[i].location));
                if (targetScore < score) {
                    targetScore = score;
                    targetIdx = i;
                }
            }
        }
        if (targetIdx != -1) {
            tryMove(moveToward(rc, enemySightings[targetIdx].location, 650));
            rc.setIndicatorString(targetScore + " " + enemySightings[targetIdx]);
            return true;
        }
        return false;
    }

    private static boolean attackVisibleIsland() throws GameActionException {
        MapLocation nearestVisibleIsland = findNearestVisibleIslandLocation(rc.getTeam().opponent());
        if (nearestVisibleIsland != null) {
            tryMove(moveToward(rc, nearestVisibleIsland, 650));  // if we're already on island, just stay there
            return true;
        }
        return false;
    }

    private static boolean attackNearestIsland() throws GameActionException {
        IslandInfo nearestIsland = pickNearest(rc, Comms.getIslands(rc), rc.getTeam().opponent());
        if (nearestIsland != null && rc.getLocation().isWithinDistanceSquared(nearestIsland.location, 81)) {
            tryMove(moveToward(rc, nearestIsland.location, 1500));
            rc.setIndicatorString("Moving toward island " + nearestIsland.location);
            return true;
        }
        return false;
    }

    private static RobotInfo pickTarget(RobotInfo[] enemies) {
        if (rc.isActionReady()) {
            RobotInfo target = null;
            for (int i = enemies.length; i --> 0;) {
                if (canAttack(enemies[i].location) && enemies[i].type != RobotType.HEADQUARTERS)
                    target = target == null ? enemies[i] : chooseTarget(target, enemies[i]);
            }
            return target;
        }
        return null;
    }

    private static boolean canAttack(MapLocation location) {
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
