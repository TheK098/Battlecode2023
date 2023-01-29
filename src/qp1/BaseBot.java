package qp1;

import battlecode.common.*;
import qp1.communications.EntityType;
import qp1.utilities.FastRandom;

import static qp1.utilities.Util.pickNearest;

abstract public class BaseBot {
    protected static RobotController rc;
    protected static int lastMoveOrAction = 0;
    private static final MapLocation IRRELEVANT = new MapLocation(-10000, -10000);

    public BaseBot(RobotController rc) throws GameActionException {
        BaseBot.rc = rc;
        for (int i = rc.getID() % 23; i --> 0;) FastRandom.nextInt();  // try to spread out the seeding a bit
        updateCommsOffsets();
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
        double threshold = mapSize / Math.pow(Math.max(1, rc.getRoundNum() * rc.getRoundNum() - 2_500_000), 0.15);
        int ourRobots = rc.getRobotCount();
        return ourRobots * 3 >= threshold;
    }

    // only finds islands within range; required since comms only stores a compressed approximation of locations
    protected static MapLocation findNearestVisibleIslandLocation(Team team) throws GameActionException {
        int[] islands = rc.senseNearbyIslands();
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

    public static void updateCommsOffsets() {
        EntityType.ENEMY.offset = 0;
        EntityType.ENEMY.count = (int) (Math.log(rc.getMapWidth() * rc.getMapHeight())) + 1;  // from 9 to 12

        EntityType.ISLAND.offset = EntityType.ENEMY.offset + EntityType.ENEMY.count;
        EntityType.ISLAND.count = rc.getIslandCount();

        EntityType.HQ.offset = EntityType.ISLAND.offset + EntityType.ISLAND.count;
        EntityType.HQ.count = 4;  // always set to 4, trying to rearrange the boundaries is annoying and not worth it

        EntityType.WELL.offset = EntityType.HQ.offset + EntityType.HQ.count;
        EntityType.WELL.count = 63 - EntityType.WELL.offset;  // will always have at least 9 spots
        // index 63 is used for resource prioritization
    }
}
