package qp1_12_tuning;

import battlecode.common.*;
import qp1_12_tuning.communications.Comms;
import qp1_12_tuning.communications.Comms.EnemySighting;
import qp1_12_tuning.communications.Comms.IslandInfo;
import qp1_12_tuning.navigation.SpreadSettings;
import qp1_12_tuning.utilities.FastRandom;
import qp1_12_tuning.utilities.IntHashMap;

import static qp1_12_tuning.navigation.Pathfinding.moveToward;
import static qp1_12_tuning.navigation.Pathfinding.spreadOut;
import static qp1_12_tuning.utilities.Util.*;

public class Launcher extends BaseBot {
    private static final IntHashMap allyHealth = new IntHashMap(20);
    private static int allyToFollow = -1;
    private static int allyFollowTimer = 0;

    public Launcher(RobotController rc) {
        super(rc);
    }

    @Override
    public void processRound() throws GameActionException {
        MapLocation curLoc = rc.getLocation();
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        MapLocation nearestHq = pickNearest(rc, Comms.getHqs(rc));
        if (!handleCombat(enemies)) {
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            RobotInfo nearestEnemyHq = pickNearestHq(rc, enemies);
            if (!supportAlly(enemies, allies, nearestEnemyHq)) {
                if (!attackVisibleIsland()) {
                    EnemySighting sighting = pickSighting();
                    if (sighting != null) {
                        tryMove(moveToward(rc, sighting.location, 700));
                    } else if (!attackNearestIsland()) {
                        Direction dir = spreadOut(rc,
                                ((rc.getMapWidth() / 2f) - curLoc.x) / 5,
                                ((rc.getMapHeight() / 2f) - curLoc.y) / 5,
                                SpreadSettings.LAUNCHER);
                        MapLocation newLoc = curLoc.add(dir);
                        // maintain space for carriers
                        if (adjacentToHeadquarters(rc, newLoc) && allies.length >= 16 && FastRandom.nextInt(8) != 0)
                            tryMove(directionAway(rc, nearestHq));
                        else if (adjacentToWell(rc, newLoc) && allies.length >= 16 && FastRandom.nextInt(8) != 0)
                            if (rc.senseWell(curLoc) != null) tryMove(randomDirection(rc));
                            else tryMove(directionAway(rc, pickNearest(rc, Comms.getKnownWells(rc)).location));
                        else if (nearestEnemyHq != null && newLoc.isWithinDistanceSquared(nearestEnemyHq.location, 9))
                            tryMove(directionAway(rc, nearestEnemyHq.location));
                        else
                            tryMove(dir);
                    }
                }
            }
        }

        if (rc.isActionReady()) {  // try attacking again after moving
            extraAttack(curLoc, nearestHq);
        }

        dieIfStuck();
    }

    private static void extraAttack(MapLocation curLoc, MapLocation nearestHq) throws GameActionException {
        RobotInfo target = pickTarget(rc.senseNearbyRobots(16, rc.getTeam().opponent()));
        if (target != null && rc.canAttack(target.location)) {
            rc.attack(target.location);
            lastMoveOrAction = rc.getRoundNum();
        } else if (rc.senseCloud(rc.getLocation())) {
            Direction dir = rc.getLocation().directionTo(nearestHq).opposite();
            MapLocation targetLoc = curLoc.add(dir).add(dir).add(dir.rotateLeft());  // rotateLeft to stay within attack range
            if (rc.canAttack(targetLoc)) rc.attack(targetLoc);
        } else {  // attack cloud
            MapLocation[] clouds = rc.senseNearbyCloudLocations(16);
            int farthestDist = 0, farthestIdx = -1;
            for (int i = clouds.length; i --> 0;) {
                if (!curLoc.isWithinDistanceSquared(clouds[i], farthestDist)) {
                    farthestDist = curLoc.distanceSquaredTo(clouds[i]);
                    farthestIdx = i;
                }
            }
            if (farthestIdx != -1 && rc.canAttack(clouds[farthestIdx])) rc.attack(clouds[farthestIdx]);
        }
    }

    private static boolean handleCombat(RobotInfo[] enemies) throws GameActionException {
        RobotInfo target = pickTarget(enemies);
        if (target == null) target = pickNearest(rc, enemies);
        if (target == null) return false;

        MapLocation toAttack = target.location;
        if (rc.canAttack(toAttack)) {
            rc.attack(toAttack);
            lastMoveOrAction = rc.getRoundNum();

            // if we're already in a cloud, the enemy can probably see us anyway so just run away
            // otherwise duck into a cloud if possible
            tryMove(rc.senseCloud(rc.getLocation()) ? directionAway(rc, toAttack) : directionAwayOrCloud(rc, toAttack));
        } else {
            RobotInfo[] allies = rc.senseNearbyRobots(toAttack, 20, rc.getTeam());
            int desperationScore = 0, allyLaunchers = 0;
            for (int i = allies.length; i --> 0;) {
                switch (allies[i].type) {
                    case CARRIER:
                        ++desperationScore;
                        break;
                    case HEADQUARTERS:
                        desperationScore += 5;
                        break;
                    case LAUNCHER:
                        ++allyLaunchers;
                        break;
                }
            }

            int enemyLaunchers = 0;
            for (int i = enemies.length; i --> 0;) if (enemies[i].type == RobotType.LAUNCHER) ++enemyLaunchers;
            if (allyLaunchers >= enemyLaunchers || desperationScore >= 3) {
                Direction dir = directionTowardImmediate(rc, toAttack);
                tryMove(dir);
                if (rc.canAttack(toAttack)) {
                    rc.attack(toAttack);
                    lastMoveOrAction = rc.getRoundNum();
                }
            } else {
                tryMove(directionAway(rc, toAttack));
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
                    allyFollowTimer = 8;
                    allyHealth.put(allies[i].ID, allies[i].health);
                } else if (storedHealth == 0 && ++added <= 10) {  // don't add too many in one round for bytecode reasons
                    // haven't seen this ally before, record their health
                    allyHealth.put(allies[i].ID, allies[i].health);
                }
            }
        }

        if (allyToFollow != -1) {  // support hurt ally
            Direction dir = directionToward(rc, rc.senseRobot(allyToFollow).location);  // no pathing, just try to be nearby
            if (enemies.length > 0 || nearestEnemyHq == null || !rc.getLocation().add(dir).isWithinDistanceSquared(nearestEnemyHq.location, 9))
                tryMove(dir);
            else tryMove(directionAway(rc, nearestEnemyHq.location));
            return true;
        }
        return false;
    }

    private static boolean attackVisibleIsland() throws GameActionException {
        MapLocation nearestVisibleIsland = findNearestVisibleIslandLocation(rc.getTeam().opponent());
        if (nearestVisibleIsland != null) {
            tryMove(moveToward(rc, nearestVisibleIsland, 700));  // if we're already on island, just stay there
            return true;
        }
        return false;
    }

    private static boolean attackNearestIsland() throws GameActionException {
        IslandInfo nearestIsland = pickNearest(rc, Comms.getIslands(rc), rc.getTeam().opponent());
        if (nearestIsland != null && rc.getLocation().isWithinDistanceSquared(nearestIsland.location, 81)) {
            tryMove(moveToward(rc, nearestIsland.location, 1600));
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
        return (rc.isMovementReady() && rc.getLocation().add(directionTowardImmediate(rc, location)).isWithinDistanceSquared(location, 16)) ||
                rc.getLocation().isWithinDistanceSquared(location, 16);
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
