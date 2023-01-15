package qpwoeirut_player;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

abstract public class BaseBot {
    protected static RobotController rc;

    public BaseBot(RobotController rc) {
        BaseBot.rc = rc;
    }

    abstract public void processRound() throws GameActionException;

    protected static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir) && dir != Direction.CENTER) {
            rc.move(dir);
            return true;
        }
        return false;
    }

    protected static boolean itsAnchorTime() {
        double mapSize = rc.getMapWidth() * rc.getMapHeight();
        double threshold = mapSize / Math.pow(Math.max(1, rc.getRoundNum() * rc.getRoundNum() - 3_500_000), 0.15);
        int ourRobots = rc.getRobotCount();
        return ourRobots * 3 >= threshold;
    }
}
