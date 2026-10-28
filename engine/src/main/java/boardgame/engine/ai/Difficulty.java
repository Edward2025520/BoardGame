package boardgame.engine.ai;

/**
 * 难度只影响搜索预算与失误率，不改变规则。
 */
public enum Difficulty {

    EASY(200, 0.25),
    NORMAL(800, 0.08),
    HARD(2500, 0.0),
    EXPERT(6000, 0.0);

    private final long timeMs;
    private final double blunderRate;

    Difficulty(long timeMs, double blunderRate) {
        this.timeMs = timeMs;
        this.blunderRate = blunderRate;
    }

    public long timeMs() {
        return timeMs;
    }

    public double blunderRate() {
        return blunderRate;
    }

    public static Difficulty parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return NORMAL;
        }
        for (Difficulty d : values()) {
            if (d.name().equalsIgnoreCase(raw.trim())) {
                return d;
            }
        }
        return NORMAL;
    }
}
