package boardgame.engine.games.splendor.model;

import java.util.List;
import java.util.StringJoiner;

/** 宝石列表的稳定排序与中文拼接，保证 key 与界面显示一致。 */
public final class GemText {

    private GemText() {
    }

    public static List<Gem> sorted(List<Gem> gems) {
        return gems.stream().sorted().toList();
    }

    public static String join(List<Gem> gems) {
        StringJoiner joiner = new StringJoiner(",");
        sorted(gems).forEach(g -> joiner.add(g.name()));
        return joiner.toString();
    }

    public static String names(List<Gem> gems) {
        StringJoiner joiner = new StringJoiner("+");
        sorted(gems).forEach(g -> joiner.add(g.label()));
        return joiner.toString();
    }
}
