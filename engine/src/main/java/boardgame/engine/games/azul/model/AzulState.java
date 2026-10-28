package boardgame.engine.games.azul.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 花砖物语的完整局面。约定不可变：任何修改都返回新实例。
 * 随机只发生在 newGame 的洗袋；之后袋与盖板都是有顺序的列表。
 */
public record AzulState(
        int playerCount,
        int currentSeat,
        AzulPhase phase,
        int round,
        List<List<TileColor>> factories,
        List<TileColor> center,
        boolean centerHasMarker,
        List<TileColor> bag,
        List<TileColor> lid,
        List<AzulPlayer> players,
        int firstPlayerSeat) {

    public AzulState {
        List<List<TileColor>> copiedFactories = new ArrayList<>(factories.size());
        for (List<TileColor> factory : factories) {
            copiedFactories.add(List.copyOf(factory));
        }
        factories = List.copyOf(copiedFactories);
        center = List.copyOf(center);
        bag = List.copyOf(bag);
        lid = List.copyOf(lid);
        players = List.copyOf(players);
    }

    public AzulPlayer player(int seat) {
        return players.get(seat);
    }

    /** 工厂与中央都空了，这一轮结束。 */
    public boolean roundOver() {
        if (!center.isEmpty()) {
            return false;
        }
        for (List<TileColor> factory : factories) {
            if (!factory.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int countIn(int source, TileColor color) {
        List<TileColor> pile = source < 0 ? center : factories.get(source);
        int count = 0;
        for (TileColor tile : pile) {
            if (tile == color) {
                count++;
            }
        }
        return count;
    }

    /** 用于测试的瓦片守恒检查：全场瓦片数应恒为 100。 */
    public int totalTiles() {
        int total = bag.size() + lid.size() + center.size();
        for (List<TileColor> factory : factories) {
            total += factory.size();
        }
        for (AzulPlayer player : players) {
            total += player.floor().size();
            for (PatternLine line : player.lines()) {
                total += line.count();
            }
            total += Integer.bitCount(player.wallMask());
        }
        return total;
    }
}
