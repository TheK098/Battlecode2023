package qp1;

import battlecode.common.*;
import qp1.communications.Comms;
import qp1.communications.Comms.EnemySighting;
import qp1.navigation.SpreadSettings;
import qp1.utilities.FastRandom;

import static qp1.navigation.Pathfinding.INF_DIST;
import static qp1.navigation.Pathfinding.spreadOut;
import static qp1.utilities.Util.directionAway;

public class Amplifier extends BaseBot {
    public Amplifier(RobotController rc) {
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
