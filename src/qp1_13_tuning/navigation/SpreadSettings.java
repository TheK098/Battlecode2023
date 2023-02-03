package qp1_13_tuning.navigation;

public enum SpreadSettings {
    CARRIER_ANCHOR(-1, 30, 100),
    CARRIER_SEARCHING(-1, 20, 100),
    LAUNCHER(13, 0.8f, 20),
    AMPLIFIER(-1, 80, 100);

    public int ally_dist_cutoff;
    public float ally_dist_factor;
    public final int random_cutoff;
    public final int random_bound;

    SpreadSettings(int ally_dist_cutoff, float ally_dist_factor, int random_cutoff) {
        this.ally_dist_cutoff = ally_dist_cutoff;
        this.ally_dist_factor = ally_dist_factor;
        this.random_cutoff = random_cutoff;
        this.random_bound = random_cutoff + random_cutoff + 1;
    }
}
