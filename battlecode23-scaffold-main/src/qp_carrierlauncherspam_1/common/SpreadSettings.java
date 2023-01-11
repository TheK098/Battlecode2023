package qp_carrierlauncherspam_1.common;

public enum SpreadSettings {
    CARRIER_COLLECTING(20, 10, 50),
    CARRIER_RETURNING(10, 10, 20),
    LAUNCHER(20, 5, 100);

    public final int ally_dist_cutoff;
    public final int ally_dist_divisor;
    public final int random_cutoff;
    public final int random_bound;

    SpreadSettings(int ally_dist_cutoff, int ally_dist_divisor, int random_cutoff) {
        this.ally_dist_cutoff = ally_dist_cutoff;
        this.ally_dist_divisor = ally_dist_divisor;
        this.random_cutoff = random_cutoff;
        this.random_bound = random_cutoff + random_cutoff + 1;
    }
}
