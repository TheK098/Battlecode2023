package qp_navtest;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import qp_navtest.communications.Comms;

import static qp_navtest.utilities.Util.similarDirection;
import static qp_navtest.utilities.Util.pickNearest;

public class CarrierScout extends BaseBot {
    private static Direction scoutDirection;
    public CarrierScout(RobotController rc) throws GameActionException {
        super(rc);

        scoutDirection = pickNearest(rc, Comms.getHqs(rc)).directionTo(rc.getLocation());
    }

    @Override
    public void processRound() throws GameActionException {
        rc.setIndicatorString("Scouting " + scoutDirection);
        tryMove(similarDirection(rc, scoutDirection));
        tryMove(similarDirection(rc, scoutDirection));
    }
}
