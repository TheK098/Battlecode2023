package qp1_13_tuning.communications;

public enum EntityType {
    ENEMY(1, 1, 1, 1),
    ISLAND(20, 100, 10, 6),
    WELL(20, 200, 12, 6),
    HQ(40, 100, 36, 8);
    // index 63 is for directing carriers to collect certain resource types

    public int offset, count;  // these will be initialized in BaseBot.updateCommsOffsets to fit specific maps
    public final int blacklistBaseTimer, blacklistLength;
    public final int randomMoveCutoff, randomMovePeriod;
    EntityType(int blacklistBaseTimer, int blacklistLength, int randomMoveCutoff, int randomMovePeriod) {
        this.blacklistBaseTimer = blacklistBaseTimer;
        this.blacklistLength = blacklistLength;
        this.randomMoveCutoff = randomMoveCutoff;
        this.randomMovePeriod = randomMovePeriod;
    }
}
