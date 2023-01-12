package qpwoeirut_player.common;

public enum SpreadSettings {
    CARRIER_COLLECTING(20, 2, 50),
    CARRIER_RETURNING(10, 2, 20),
    LAUNCHER(18, 10, 100);

    public final int ally_dist_cutoff;
    public final int ally_dist_factor;
    public final int random_cutoff;
    public final int random_bound;

    SpreadSettings(int ally_dist_cutoff, int ally_dist_factor, int random_cutoff) {
        this.ally_dist_cutoff = ally_dist_cutoff;
        this.ally_dist_factor = ally_dist_factor;
        this.random_cutoff = random_cutoff;
        this.random_bound = random_cutoff + random_cutoff + 1;
    }
}
