package qp1_10_tuningandamplifiers;

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






/*
[A:CARRIER#13335@257] Started on round 256 but ended on round 257 with 12213 bytecode used. Location [12, 21]
[A:CARRIER#12185@280] Started on round 279 but ended on round 280 with 12286 bytecode used. Location [12, 21]
[A:LAUNCHER#12249@502] Started on round 501 but ended on round 502 with 20 bytecode used. Location [17, 12]
[A:LAUNCHER#10249@507] Started on round 506 but ended on round 507 with 44 bytecode used. Location [17, 12]
[A:LAUNCHER#11347@652] Started on round 651 but ended on round 652 with 574 bytecode used. Location [10, 22]
 */