package nullplayer;

import battlecode.common.*;


/**
 * This player does literally nothing. For testing purposes.
 */
public strictfp class RobotPlayer {
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        while (true) {
//            if (rc.getRoundNum() == 3) rc.resign();
            Clock.yield();
        }
    }
}
