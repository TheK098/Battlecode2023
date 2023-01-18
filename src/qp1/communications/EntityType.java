package qp1.communications;

public enum EntityType {
    WELL(0, 20, 20, 200, 12, 3),
    ENEMY(20, 39, 1, 1, 1, 1),
    HQ(59, 4, 50, 100, 36, 4);
    // index 63 is for directing carriers to collect certain resource types

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
