package qp1_3_carrierlauncherspam.common;

public enum EntityType {
    WELL(0, 10, 20, 200, 12, 3),
    HQ(10, 4, 50, 100, 36, 4),
    ENEMY(14, 40, 1, 1, 1, 1);

    public final int offset, count;
    public final int blacklistTimer, blacklistLength;
    public final int randomMoveCutoff, randomMovePeriod;
    EntityType(int offset, int count, int blacklistTimer, int blacklistLength, int randomMoveCutoff, int randomMovePeriod) {
        this.offset = offset;
        this.count = count;
        this.blacklistTimer = blacklistTimer;
        this.blacklistLength = blacklistLength;
        this.randomMoveCutoff = randomMoveCutoff;
        this.randomMovePeriod = randomMovePeriod;
    }
}
