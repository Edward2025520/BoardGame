package boardgame.engine.games.splendor.model;

import java.util.Arrays;

/**
 * 五种宝石加黄金。成本数组一律按本枚举的 ordinal 排列，长度 6。
 */
public enum Gem {

    WHITE("white", "白"),
    BLUE("blue", "蓝"),
    GREEN("green", "绿"),
    RED("red", "红"),
    BLACK("black", "黑"),
    GOLD("gold", "金");

    public static final int COLOR_COUNT = 5;

    private static final Gem[] COLORS = {WHITE, BLUE, GREEN, RED, BLACK};

    private final String id;
    private final String label;

    Gem(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public static Gem[] colors() {
        return Arrays.copyOf(COLORS, COLORS.length);
    }

    public static Gem fromName(String name) {
        for (Gem gem : values()) {
            if (gem.id.equalsIgnoreCase(name) || gem.name().equalsIgnoreCase(name)) {
                return gem;
            }
        }
        throw new IllegalArgumentException("未知宝石: " + name);
    }
}
