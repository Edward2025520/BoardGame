package boardgame.engine.games.azul.model;

import boardgame.engine.core.Action;

public sealed interface AzulAction extends Action {

    /**
     * 从某个工厂（source >= 0）或中央（source = -1）取走一种颜色的全部瓦片，
     * 放进指定图案行（targetLine 0-4），或只放地板（-1）。
     */
    record TakeTiles(int source, TileColor color, int targetLine, int count) implements AzulAction {

        @Override
        public String key() {
            return "take:" + source + ":" + color.name() + ":" + targetLine;
        }

        @Override
        public String label() {
            String from = source < 0 ? "中央" : "工厂 " + (source + 1);
            String to = targetLine < 0 ? "地板" : "第 " + (targetLine + 1) + " 行";
            return "取 " + from + " 的 " + count + " 枚" + color.label() + " → " + to;
        }
    }
}
