package nullplayer;

import battlecode.common.*;


/**
 * This player does literally nothing. For testing purposes.
 */
public strictfp class RobotPlayer {
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        while (true) Clock.yield();
    }
}
