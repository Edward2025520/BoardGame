package boardgame.engine.games.splendor.rules;

import boardgame.engine.games.splendor.model.Card;
import boardgame.engine.games.splendor.model.Gem;
import boardgame.engine.games.splendor.model.Noble;
import java.util.ArrayList;
import java.util.List;

/**
 * 璀璨宝石的 90 张发展卡与 10 张贵族。
 *
 * <p>数据由 tools/gen-splendor-data.mjs 生成，格式：
 * {@code 卡号|颜色|等级|分数|成本}。卡号在原版中唯一，直接用作动作与美术资源的 key。
 */
public final class SplendorData {

    private static final String RAW_CARDS = """
1b1B3w|white|1|0|blue=1,black=1,white=3
1r1b1B1g|white|1|0|red=1,blue=1,black=1,green=1
1r1b1B2g|white|1|0|red=1,blue=1,black=1,green=2
2b2B|white|1|0|blue=2,black=2
2g1B2b|white|1|0|green=2,black=1,blue=2
2r1B|white|1|0|red=2,black=1
3b|white|1|0|blue=3
4g|white|1|1|green=4
1r1w1B1g|blue|1|0|red=1,white=1,black=1,green=1
1w2B|blue|1|0|white=1,black=2
2g2B|blue|1|0|green=2,black=2
2g2r1w|blue|1|0|green=2,red=2,white=1
2r1w1B1g|blue|1|0|red=2,white=1,black=1,green=1
3B|blue|1|0|black=3
3g1r1b|blue|1|0|green=3,red=1,blue=1
4r|blue|1|1|red=4
1r1w1B1b|green|1|0|red=1,white=1,black=1,blue=1
1r1w2B1b|green|1|0|red=1,white=1,black=2,blue=1
2b2r|green|1|0|blue=2,red=2
2r2B1b|green|1|0|red=2,black=2,blue=1
2w1b|green|1|0|white=2,blue=1
3b1g1w|green|1|0|blue=3,green=1,white=1
3r|green|1|0|red=3
4B|green|1|1|black=4
1g1w1B1b|red|1|0|green=1,white=1,black=1,blue=1
1g2B2w|red|1|0|green=1,black=2,white=2
1g2w1B1b|red|1|0|green=1,white=2,black=1,blue=1
1r3B1w|red|1|0|red=1,black=3,white=1
2b1g|red|1|0|blue=2,green=1
2w2r|red|1|0|white=2,red=2
3w|red|1|0|white=3
4w|red|1|1|white=4
1g1w1r1b|black|1|0|green=1,white=1,red=1,blue=1
1g1w1r2b|black|1|0|green=1,white=1,red=1,blue=2
2b1r2w|black|1|0|blue=2,red=1,white=2
2g1r|black|1|0|green=2,red=1
2w2g|black|1|0|white=2,green=2
3g|black|1|0|green=3
3r1B1g|black|1|0|red=3,black=1,green=1
4b|black|1|1|blue=4
2r2B3g|white|2|1|red=2,black=2,green=3
3b3r2w|white|2|1|blue=3,red=3,white=2
4r2B1g|white|2|2|red=4,black=2,green=1
5r|white|2|2|red=5
5r3B|white|2|2|red=5,black=3
6w|white|2|3|white=6
1r4B2w|blue|2|2|red=1,black=4,white=2
2g3r2b|blue|2|1|green=2,red=3,blue=2
3g3B2b|blue|2|1|green=3,black=3,blue=2
5b|blue|2|2|blue=5
5w3b|blue|2|2|white=5,blue=3
6b|blue|2|3|blue=6
2b1B4w|green|2|2|blue=2,black=1,white=4
2g3r3w|green|2|1|green=2,red=3,white=3
3b2B2w|green|2|1|blue=3,black=2,white=2
5b3g|green|2|2|blue=5,green=3
5g|green|2|2|green=5
6g|green|2|3|green=6
2r3B2w|red|2|1|red=2,black=3,white=2
2r3B3b|red|2|1|red=2,black=3,blue=3
3w5B|red|2|2|white=3,black=5
4b2g1w|red|2|2|blue=4,green=2,white=1
5B|red|2|2|black=5
6r|red|2|3|red=6
2b2g3w|black|2|1|blue=2,green=2,white=3
3g2B3w|black|2|1|green=3,black=2,white=3
4g2r1b|black|2|2|green=4,red=2,blue=1
5g3r|black|2|2|green=5,red=3
5w|black|2|2|white=5
6B|black|2|3|black=6
3r6B3w|white|3|4|red=3,black=6,white=3
3w7B|white|3|5|white=3,black=7
5r3b3B3g|white|3|3|red=5,blue=3,black=3,green=3
7B|white|3|4|black=7
3b3B6w|blue|3|4|blue=3,black=3,white=6
3r3w5B3g|blue|3|3|red=3,white=3,black=5,green=3
7w|blue|3|4|white=7
7w3b|blue|3|5|white=7,blue=3
3r5w3B3b|green|3|3|red=3,white=5,black=3,blue=3
6b3g3w|green|3|4|blue=6,green=3,white=3
7b|green|3|4|blue=7
7b3g|green|3|5|blue=7,green=3
3g3w3B5b|red|3|3|green=3,white=3,black=3,blue=5
6g3r3b|red|3|4|green=6,red=3,blue=3
7g|red|3|4|green=7
7g3r|red|3|5|green=7,red=3
5g3w3r3b|black|3|3|green=5,white=3,red=3,blue=3
6r3B3g|black|3|4|red=6,black=3,green=3
7r|black|3|4|red=7
7r3B|black|3|5|red=7,black=3
            """;

    private static final String RAW_NOBLES = """
3b3g3r|blue=3,green=3,red=3
3g3r3B|green=3,red=3,black=3
3w3b3B|white=3,blue=3,black=3
3w3b3g|white=3,blue=3,green=3
3w3r3B|white=3,red=3,black=3
4b4g|blue=4,green=4
4g4r|green=4,red=4
4r4B|red=4,black=4
4w4b|white=4,blue=4
4w4B|white=4,black=4
            """;

    public static final List<Card> ALL_CARDS;
    public static final List<Noble> ALL_NOBLES;

    static {
        List<Card> cards = new ArrayList<>(90);
        for (String line : RAW_CARDS.strip().split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.strip().split("\\|");
            cards.add(new Card(parts[0], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                    Gem.fromName(parts[1]), parseCost(parts[4])));
        }
        ALL_CARDS = List.copyOf(cards);

        List<Noble> nobles = new ArrayList<>(10);
        for (String line : RAW_NOBLES.strip().split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.strip().split("\\|");
            nobles.add(new Noble(parts[0], parseCost(parts[1])));
        }
        ALL_NOBLES = List.copyOf(nobles);
    }

    private SplendorData() {
    }

    private static int[] parseCost(String raw) {
        int[] cost = new int[Gem.values().length];
        if (raw.isBlank()) {
            return cost;
        }
        for (String item : raw.split(",")) {
            String[] kv = item.split("=");
            cost[Gem.fromName(kv[0]).ordinal()] = Integer.parseInt(kv[1]);
        }
        return cost;
    }
}
