package boardgame.engine.games.azul.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 一名玩家的面板。墙面用 25 位掩码表示已铺格，位序为 row * 5 + col。
 */
public record AzulPlayer(int score, List<PatternLine> lines, int wallMask, List<TileColor> floor,
                         boolean hasMarker) {

    public AzulPlayer {
        lines = List.copyOf(lines);
        floor = List.copyOf(floor);
    }

    public static AzulPlayer fresh() {
        List<PatternLine> lines = new ArrayList<>(5);
        for (int index = 0; index < 5; index++) {
            lines.add(PatternLine.empty(index + 1));
        }
        return new AzulPlayer(0, lines, 0, List.of(), false);
    }

    public boolean wallHas(int row, int col) {
        return (wallMask & (1 << (row * 5 + col))) != 0;
    }

    public boolean hasFullRow() {
        for (int row = 0; row < 5; row++) {
            boolean full = true;
            for (int col = 0; col < 5; col++) {
                if (!wallHas(row, col)) {
                    full = false;
                    break;
                }
            }
            if (full) {
                return true;
            }
        }
        return false;
    }
}
