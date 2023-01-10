package qpwoeirut_player;

import battlecode.common.*;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
@SuppressWarnings("unused")
public strictfp class RobotPlayer {
    static int turnCount = 0;

    /**
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        while (true) {
            turnCount += 1;
            try {
                switch (rc.getType()) {
                    case HEADQUARTERS:
                        Headquarters.initialize(rc);
                        Headquarters.processRound();
                        break;
                    case CARRIER:
                        Carrier.initialize(rc);
                        Carrier.processRound();
                        break;
                    case LAUNCHER:
                        Launcher.initialize(rc);
                        Launcher.processRound();
                        break;
                    case BOOSTER:
                    case DESTABILIZER:
                    case AMPLIFIER:     break;
                }

            } catch (GameActionException e) {
                System.out.println(rc.getType() + " GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } finally {  // end turn
                Clock.yield();
            }
        }
    }
}
