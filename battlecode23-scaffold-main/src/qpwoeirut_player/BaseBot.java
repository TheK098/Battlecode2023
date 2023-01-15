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

    // once we have enough bots to fill one-third of the map, the anchoring process can kick in
    // TODO: reduce threshold to start if the 2000th round is soon
    protected static boolean itsAnchorTime() {
        int mapSize = rc.getMapWidth() * rc.getMapHeight();
        int ourRobots = rc.getRobotCount();
        return ourRobots * 3 >= mapSize;
    }
}
