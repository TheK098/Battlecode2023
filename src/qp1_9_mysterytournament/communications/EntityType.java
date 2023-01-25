package qp1_9_mysterytournament.communications;

public enum EntityType {
    WELL(0, 20, 10, 200, 12, 3),
    ENEMY(20, 39, 1, 1, 1, 1),
    HQ(59, 4, 20, 100, 36, 4);
    // index 63 is for directing carriers to collect certain resource types

    public final int offset, count;
    public final int blacklistBaseTimer, blacklistLength;
    public final int randomMoveCutoff, randomMovePeriod;
    EntityType(int offset, int count, int blacklistBaseTimer, int blacklistLength, int randomMoveCutoff, int randomMovePeriod) {
        this.offset = offset;
        this.count = count;
        this.blacklistBaseTimer = blacklistBaseTimer;
        this.blacklistLength = blacklistLength;
        this.randomMoveCutoff = randomMoveCutoff;
        this.randomMovePeriod = randomMovePeriod;
    }
}
