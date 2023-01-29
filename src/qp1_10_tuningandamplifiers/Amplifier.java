package qp1_10_tuningandamplifiers;

import battlecode.common.*;
import qp1_10_tuningandamplifiers.communications.Comms;
import qp1_10_tuningandamplifiers.communications.Comms.EnemySighting;
import qp1_10_tuningandamplifiers.navigation.SpreadSettings;
import qp1_10_tuningandamplifiers.utilities.FastRandom;

import static qp1_10_tuningandamplifiers.navigation.Pathfinding.INF_DIST;
import static qp1_10_tuningandamplifiers.navigation.Pathfinding.spreadOut;
import static qp1_10_tuningandamplifiers.utilities.Util.directionAway;

public class Amplifier extends BaseBot {
    public Amplifier(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void processRound() throws GameActionException {
        Comms.addWells(rc, rc.senseNearbyWells());
        Comms.addNearbyIslands(rc);

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        int nearestIdx = -1;
        int nearestDist = INF_DIST;
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
            float weightX = sighting == null ? 0 : sighting.location.x - rc.getLocation().x;
            float weightY = sighting == null ? 0 : sighting.location.y - rc.getLocation().y;
            Direction dir = spreadOut(rc, weightX, weightY, SpreadSettings.AMPLIFIER);
            if (!rc.senseCloud(rc.getLocation().add(dir)) || FastRandom.nextInt(8) == 0) tryMove(dir);
        }
        dieIfStuck();
    }


    private static void dieIfStuck() {  // desperate times call for desperate measures
        if (rc.getRoundNum() - lastMoveOrAction >= 200) rc.disintegrate();
    }
}
