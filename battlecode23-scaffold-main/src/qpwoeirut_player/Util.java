package qpwoeirut_player;

import battlecode.common.Direction;

import java.util.Random;

public class Util {
    public static final Random rng = new Random(8);

    public static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
}
