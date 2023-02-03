package qp1_13_tuning;

import battlecode.common.*;
import qp1_13_tuning.communications.Comms;
import qp1_13_tuning.communications.Comms.EnemySighting;
import qp1_13_tuning.navigation.SpreadSettings;
import qp1_13_tuning.utilities.FastRandom;

import static qp1_13_tuning.navigation.Pathfinding.spreadOut;
import static qp1_13_tuning.utilities.Util.directionAway;

public class Amplifier extends BaseBot {
    private static float centerX, centerY;
    public Amplifier(RobotController rc) {
        super(rc);

        centerX = rc.getMapWidth() / 2f;
        centerY = rc.getMapHeight() / 2f;
    }

    @Override
    public void processRound() throws GameActionException {
        Comms.addWells(rc, rc.senseNearbyWells());
        Comms.addNearbyIslands(rc);

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        int nearestIdx = -1;
        int nearestDist = 12960000;
        for (int i = enemies.length; i --> 0;) {
            if (enemies[i].type != RobotType.HEADQUARTERS) {
                Comms.reportEnemySighting(rc, enemies[i].location);
            }
            int dist = rc.getLocation().distanceSquaredTo(enemies[i].location);
            if (nearestDist > dist) {
                nearestDist = dist;
                nearestIdx = i;
            }
        }

        if (nearestIdx != -1) tryMove(directionAway(rc, enemies[nearestIdx].location));
        else {
            EnemySighting sighting = pickSighting();
            float weightX = (sighting == null ? centerX : sighting.location.x) - rc.getLocation().x;
            float weightY = (sighting == null ? centerY : sighting.location.y) - rc.getLocation().y;
            Direction dir = spreadOut(rc, weightX, weightY, SpreadSettings.AMPLIFIER);

            if (shouldMove(dir, 4)) tryMove(dir);
            else if (shouldMove(dir.rotateLeft(), 8)) tryMove(dir.rotateLeft());
            else if (shouldMove(dir.rotateRight(), 8)) tryMove(dir.rotateRight());
        }
        dieIfStuck();
    }

    private static boolean shouldMove(Direction dir, int chance) throws GameActionException {
        MapLocation result = rc.getLocation().add(dir);
        return rc.canMove(dir) && rc.canSenseLocation(result) && (!rc.senseCloud(result) || FastRandom.nextInt(chance) == 0);
    }

    private static void dieIfStuck() {  // desperate times call for desperate measures
        if (rc.getRoundNum() - lastMoveOrAction >= 200) rc.disintegrate();
    }
}
