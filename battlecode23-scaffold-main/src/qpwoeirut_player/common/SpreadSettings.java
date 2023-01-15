package qpwoeirut_player.common;

public enum SpreadSettings {
    CARRIER_ANCHOR(25, 1.5f, 100),
    LAUNCHER(16, 1.0f, 20);

    public final int ally_dist_cutoff;
    public final float ally_dist_exp;
    public final int random_cutoff;
    public final int random_bound;

    SpreadSettings(int ally_dist_cutoff, float ally_dist_exp, int random_cutoff) {
        this.ally_dist_cutoff = ally_dist_cutoff;
        this.ally_dist_exp = ally_dist_exp;
        this.random_cutoff = random_cutoff;
        this.random_bound = random_cutoff + random_cutoff + 1;
    }
}
