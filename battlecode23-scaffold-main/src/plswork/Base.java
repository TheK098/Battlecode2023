package plswork;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import qp_carrierlauncherspam_2.BaseBot;

abstract public class Base {
    protected static RobotController rc;
    public Base(RobotController rc) {
        Base.rc = rc;
    }
    abstract public void runPls() throws GameActionException;
}
