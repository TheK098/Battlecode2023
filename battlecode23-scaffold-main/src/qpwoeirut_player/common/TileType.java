package qpwoeirut_player.common;

public enum TileType {
    WELL(0, 60),
    HQ(60, 4);

    public final int offset;
    public final int count;
    TileType(int offset, int count) {
        this.offset = offset;
        this.count = count;
    }
}
