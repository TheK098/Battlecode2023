package qpwoeirut_player.common;

public enum EntityType {
    WELL(0, 16, 20, 200, 12, 3),
    ENEMY(16, 36, 1, 1, 1, 1),
    ISLAND(52, 8, 1, 1, 1, 1),
    HQ(60, 4, 50, 100, 36, 4);

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
