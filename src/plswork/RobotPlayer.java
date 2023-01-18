package plswork;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import plswork.Carrier.*;
import plswork.Launcher.*;
import plswork.Headquarters.*;

public strictfp class RobotPlayer {
    static int turnCount = 0;
    static final Random rng = new Random(1234);

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        Base bot = null;
        switch (rc.getType()) {
            case HEADQUARTERS:
                bot = new Headquarters(rc);  break;
            case CARRIER:
                bot = new Carrier2(rc);   break;
            case LAUNCHER:
                bot = new Launcher(rc); break;
            case BOOSTER:
            case DESTABILIZER:
            case AMPLIFIER:       break;
        }

        while (true) {
            turnCount += 1;
            try {
                bot.runPls();
            } catch (GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}
