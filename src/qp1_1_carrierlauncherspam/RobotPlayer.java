package qp1_1_carrierlauncherspam;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

@SuppressWarnings("unused")
public strictfp class RobotPlayer {
    /**
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings({"unused", "InfiniteLoopStatement"})
    public static void run(RobotController rc) throws GameActionException {
        BaseBot bot = null;
        // switch assignments don't work in 1.8 smh
        switch (rc.getType()) {
            case HEADQUARTERS:
                bot = new Headquarters(rc);
                break;
            case CARRIER:
                bot = new Carrier(rc);
                break;
            case LAUNCHER:
                bot = new Launcher(rc);
                break;
            case BOOSTER:
            case DESTABILIZER:
            case AMPLIFIER:
                throw new IllegalArgumentException("Type " + rc.getType() + " is not handled!");
        }
        while (true) {
            try {
                bot.processRound();
            } catch (GameActionException e) {
//                System.out.println("GameActionException: " + rc.getType());
//                e.printStackTrace();
            } catch (Exception e) {
//                System.out.println("Exception: " + rc.getType());
//                e.printStackTrace();
            } finally {  // end turn
                Clock.yield();
            }
        }
    }
}
