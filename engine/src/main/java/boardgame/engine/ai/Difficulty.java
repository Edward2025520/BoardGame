package boardgame.engine.ai;

/**
 * 七档难度：只影响搜索预算与失误率，不改变规则。
 *
 * <p>参数照搬 shanzhai-splendor：一档纯贪心，二三档靠注入失误变弱，
 * 四档起加大搜索预算。
 */
public enum Difficulty {

    BEGINNER("一档 · 入门", 0, 0.0),
    CASUAL("二档 · 简单", 200, 0.40),
    ADVANCED("三档 · 进阶", 500, 0.18),
    MEDIUM("四档 · 中等", 1_000, 0.0),
    HARD("五档 · 困难", 2_000, 0.0),
    MASTER("六档 · 大师", 4_000, 0.0),
    HELL("七档 · 地狱", 8_000, 0.0);

    private final String label;
    private final long timeMs;
    private final double blunderRate;

    Difficulty(String label, long timeMs, double blunderRate) {
        this.label = label;
        this.timeMs = timeMs;
        this.blunderRate = blunderRate;
    }

    public String label() {
        return label;
    }

    public long timeMs() {
        return timeMs;
    }

    public double blunderRate() {
        return blunderRate;
    }

    /** 预算为 0 表示不搜索，直接用贪心。 */
    public boolean greedy() {
        return timeMs == 0;
    }

    public static Difficulty parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return MEDIUM;
        }
        String value = raw.trim();
        for (Difficulty difficulty : values()) {
            if (difficulty.name().equalsIgnoreCase(value)) {
                return difficulty;
            }
        }
        // 兼容旧的四个档位命名
        return switch (value.toUpperCase()) {
            case "EASY" -> BEGINNER;
            case "NORMAL" -> MEDIUM;
            case "HARD" -> HARD;
            case "EXPERT" -> MASTER;
            default -> MEDIUM;
        };
    }
}
