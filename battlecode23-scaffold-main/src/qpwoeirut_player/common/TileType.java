package qpwoeirut_player.common;

public enum TileType {
    WELL(0, 60, 20, 200, 12, 3),
    HQ(60, 4, 50, 100, 36, 4);

    public final int offset, count;
    public final int blacklistTimer, blacklistLength;
    public final int randomMoveCutoff, randomMovePeriod;
    TileType(int offset, int count, int blacklistTimer, int blacklistLength, int randomMoveCutoff, int randomMovePeriod) {
        this.offset = offset;
        this.count = count;
        this.blacklistTimer = blacklistTimer;
        this.blacklistLength = blacklistLength;
        this.randomMoveCutoff = randomMoveCutoff;
        this.randomMovePeriod = randomMovePeriod;
    }
}
