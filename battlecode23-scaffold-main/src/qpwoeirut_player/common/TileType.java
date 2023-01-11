package qpwoeirut_player.common;

public enum TileType {
    WELL(0, 60, 50, 200),
    HQ(60, 4, 100, 100);

    public final int offset, count;
    public final int blacklistTimer, blacklistLength;
    TileType(int offset, int count, int blacklistTimer, int blacklistLength) {
        this.offset = offset;
        this.count = count;
        this.blacklistTimer = blacklistTimer;
        this.blacklistLength = blacklistLength;
    }
}
