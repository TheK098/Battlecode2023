package qp1.utilities;

// https://stackoverflow.com/questions/1640258/need-a-fast-random-generator-for-c
public class FastRandom {
    static long x = (long) (Math.random() * 0xffffffffL) & 0xffffffffL;

    public static int nextInt() {
        x = (214013 * x + 2531011);
        return (int) (x >> 16) & 0x7FFF;
    }

    // [0, bound)
    public static int nextInt(int bound) {  // technically the probability isn't uniform, but it shouldn't really matter
        final int ret = nextInt() % bound;
        return ret < 0 ? ret + bound : ret;
    }
}
