package qp1;

import battlecode.common.*;
import qp1.communications.Comms;
import qp1.navigation.SpreadSettings;
import qp1.utilities.FastRandom;

import static qp1.navigation.Pathfinding.INF_DIST;
import static qp1.navigation.Pathfinding.spreadOut;
import static qp1.utilities.Util.directionAway;

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
        else if (!investigateSightings()) {
            Direction dir = spreadOut(rc, 0, 0, SpreadSettings.AMPLIFIER);
            if (!rc.senseCloud(rc.getLocation().add(dir)) || FastRandom.nextInt(8) == 0) tryMove(dir);
        }
        dieIfStuck();
    }


    private static void dieIfStuck() {  // desperate times call for desperate measures
        if (rc.getRoundNum() - lastMoveOrAction >= 200) rc.disintegrate();
    }
}
