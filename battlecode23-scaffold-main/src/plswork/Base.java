package plswork;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

abstract public class Base {
    protected static RobotController rc;
    public Base(RobotController rc) {
        Base.rc = rc;
    }
    abstract public void runPls() throws GameActionException;
}
