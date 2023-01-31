package qp1_12_tuning;

import battlecode.common.Clock;
import battlecode.common.RobotController;
import qp1_12_tuning.utilities.FastRandom;

@SuppressWarnings("unused")
public strictfp class RobotPlayer {
    /**
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings({"unused", "InfiniteLoopStatement"})
    public static void run(RobotController rc) {
        BaseBot bot = null;
        // switch assignments don't work in 1.8 smh
        while (true) {
            try {  // wrap in a loop and keep trying, just in case
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
                    case AMPLIFIER:
                        bot = new Amplifier(rc);
                        break;
                    case BOOSTER:
                    case DESTABILIZER:
                        throw new IllegalArgumentException("Type " + rc.getType() + " is not handled!");
                }

                FastRandom.x = rc.getID();
                break;
//            } catch (GameActionException e) {
//                System.out.println("GameActionException: " + rc.getType());
//                e.printStackTrace();
            } catch (Exception e) {
//                System.out.println("Exception: " + rc.getType());
//                e.printStackTrace();
            }
        }

        while (true) {
//            int startingRound = rc.getRoundNum();
            try {
                bot.processRound();
//            } catch (GameActionException e) {
//                System.out.println("GameActionException: " + rc.getType());
//                e.printStackTrace();
            } catch (Exception e) {
//                System.out.println("Exception: " + rc.getType());
//                e.printStackTrace();
            } finally {  // end turn
//                if (startingRound != rc.getRoundNum()) {
//                    System.out.println("Started on round " + startingRound + " but ended on round " + rc.getRoundNum() + " with " + Clock.getBytecodeNum() + " bytecode used. Location " + rc.getLocation());
//                }
                if (Clock.getBytecodeNum() >= 500) Clock.yield();
            }
        }
    }
}
