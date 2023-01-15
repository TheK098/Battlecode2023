package qp_carrierlauncherspam_3.common;

public enum SpreadSettings {
    CARRIER_ANCHOR(25, 30, 100),
    LAUNCHER(16, 1, 20);

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
