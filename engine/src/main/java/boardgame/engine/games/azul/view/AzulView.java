package boardgame.engine.games.azul.view;

import java.util.List;

/**
 * 花砖物语的渲染视图。除袋与盖板的顺序外，全部信息公开。
 */
public record AzulView(
        int playerCount,
        int currentSeat,
        int round,
        String phase,
        int bagCount,
        List<FactoryView> factories,
        CenterView center,
        List<SeatView> seats) {

    public record FactoryView(int index, List<String> tiles) {
    }

    public record CenterView(List<String> tiles, boolean hasMarker) {
    }

    public record LineView(String color, int count, int capacity) {
    }

    public record WallCell(String color, boolean filled) {
    }

    public record SeatView(
            int seat,
            int score,
            List<LineView> lines,
            List<List<WallCell>> wall,
            List<String> floor,
            boolean hasMarker,
            boolean current,
            boolean first) {
    }
}
