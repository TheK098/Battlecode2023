package qp1_12_tuning.utilities;

// https://stackoverflow.com/questions/1640258/need-a-fast-random-generator-for-c
public class FastRandom {
    public static long x = 0;  // seed will be set in BaseBot initializer

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
