package qp1_1_carrierlauncherspam;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

abstract public class BaseBot {
    protected static RobotController rc;

    public BaseBot(RobotController rc) {
        BaseBot.rc = rc;
    }

    abstract public void processRound() throws GameActionException;
}
