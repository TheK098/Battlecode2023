package qp1_6_carrierlauncherspam;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.WellInfo;
import qp1_6_carrierlauncherspam.common.Communications;

abstract public class BaseBot {
    protected static RobotController rc;
    protected static int lastMoveOrAction = 0;

    public BaseBot(RobotController rc) {
        BaseBot.rc = rc;
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

    protected static void updateWellComms() throws GameActionException {
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        for (WellInfo wellInfo : nearbyWells) {
            Communications.addWell(rc, wellInfo.getMapLocation(), wellInfo.getResourceType());
        }
    }
}
