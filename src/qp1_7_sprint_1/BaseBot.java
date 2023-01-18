package qp1_7_sprint_1;

import battlecode.common.*;
import qp1_7_sprint_1.utilities.FastRandom;

import static qp1_7_sprint_1.utilities.Util.pickNearest;

abstract public class BaseBot {
    protected static RobotController rc;
    protected static int lastMoveOrAction = 0;
    private static final MapLocation IRRELEVANT = new MapLocation(-10000, -10000);

    public BaseBot(RobotController rc) {
        BaseBot.rc = rc;
        for (int i = rc.getID() % 23; i --> 0;) FastRandom.nextInt();  // try to spread out the seeding a bit
    }

    abstract public void processRound() throws GameActionException;

    protected static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir) && dir != Direction.CENTER) {
            rc.move(dir);
            lastMoveOrAction = rc.getRoundNum();
            return true;
        }
        return false;
    }

    protected static boolean itsAnchorTime() {
        double mapSize = rc.getMapWidth() * rc.getMapHeight();
        double threshold = mapSize / Math.pow(Math.max(1, rc.getRoundNum() * rc.getRoundNum() - 3_200_000), 0.15);
        int ourRobots = rc.getRobotCount();
        return ourRobots * 3 >= threshold;
    }

    protected static MapLocation findNearestIslandLocation(Team team) throws GameActionException {
        int[] islands = rc.senseNearbyIslands();  // TODO: add comms to make anchoring more efficient
        if (islands.length > 0) {
            MapLocation[] nearestIslandLocations = new MapLocation[islands.length];
            for (int i = islands.length; i --> 0;) {
                nearestIslandLocations[i] = (rc.senseTeamOccupyingIsland(islands[i]) == team) ?
                        pickNearest(rc, rc.senseNearbyIslandLocations(islands[i])) : IRRELEVANT;
            }
            return pickNearest(rc, nearestIslandLocations);
        }
        return null;
    }
}
