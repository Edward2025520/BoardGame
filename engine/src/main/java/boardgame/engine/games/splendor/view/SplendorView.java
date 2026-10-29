package boardgame.engine.games.splendor.view;

import java.util.List;
import java.util.Map;

/**
 * 璀璨宝石的渲染视图。所有隐藏信息已按观看座位裁剪：
 * 其他玩家的预留卡只给数量，不给卡面。
 */
public record SplendorView(
        int playerCount,
        int currentSeat,
        String phase,
        boolean finalRound,
        int[] bank,
        int goldBank,
        int reservedLimit,
        List<SeatView> seats,
        List<RowView> rows,
        List<NobleView> nobles) {

    public record SeatView(
            int seat,
            int points,
            int cards,
            int[] tokens,
            int[] bonuses,
            int reservedCount,
            List<CardView> reserved,
            List<CardView> purchased,
            boolean current) {
    }

    public record RowView(int tier, List<CardView> cards, int deckSize) {
    }

    public record CardView(String code, int tier, int points, String bonus, Map<String, Integer> cost) {
    }

    public record NobleView(String code, Map<String, Integer> requirement, boolean qualified) {
    }
}
