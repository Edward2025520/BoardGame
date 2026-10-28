package boardgame.engine.games.azul.model;

/** 花砖物语的五种瓦片。顺序即墙面的颜色编号，不要随意调整。 */
public enum TileColor {

    BLUE("blue", "蓝"),
    YELLOW("yellow", "黄"),
    RED("red", "红"),
    BLACK("black", "黑"),
    WHITE("white", "白");

    public static final int COUNT = values().length;

    private final String id;
    private final String label;

    TileColor(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public static TileColor fromId(String value) {
        for (TileColor color : values()) {
            if (color.id.equalsIgnoreCase(value) || color.name().equalsIgnoreCase(value)) {
                return color;
            }
        }
        throw new IllegalArgumentException("未知瓦片颜色: " + value);
    }
}
