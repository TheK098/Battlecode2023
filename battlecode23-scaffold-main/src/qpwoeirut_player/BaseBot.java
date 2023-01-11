package qpwoeirut_player;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import qpwoeirut_player.common.Communications;

abstract public class BaseBot {
    protected static RobotController rc;
    protected static Communications comms;

    public BaseBot(RobotController rc) {
        BaseBot.rc = rc;
    }

    abstract public void processRound() throws GameActionException;
}
