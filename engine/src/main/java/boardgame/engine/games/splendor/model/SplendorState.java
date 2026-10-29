package boardgame.engine.games.splendor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 璀璨宝石的完整局面。约定不可变：任何修改都返回新实例。
 *
 * <p>随机性只有开局一次洗牌，牌堆顺序固定写在 state 里，所以「seed + 动作序列」可完全复现一局。
 */
public record SplendorState(
        int playerCount,
        int currentSeat,
        SplendorPhase phase,
        int[][] tokens,
        int[][] bonuses,
        int[] points,
        int[] cards,
        int[] bank,
        List<List<Card>> rows,
        List<List<Card>> decks,
        List<List<Card>> reserved,
        List<Noble> nobles,
        boolean finalRound,
        int finalRoundTrigger,
        List<List<Card>> purchased) {

    public SplendorState {
        tokens = deepCopy(tokens);
        bonuses = deepCopy(bonuses);
        points = points.clone();
        cards = cards.clone();
        bank = bank.clone();
        rows = copyRows(rows);
        decks = copyRows(decks);
        reserved = copyRows(reserved);
        nobles = List.copyOf(nobles);
        purchased = copyRows(purchased);
    }

    public static List<List<Card>> emptyPurchased(int players) {
        List<List<Card>> purchased = new ArrayList<>(players);
        for (int i = 0; i < players; i++) {
            purchased.add(new ArrayList<>());
        }
        return purchased;
    }

    private static int[][] deepCopy(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    private static List<List<Card>> copyRows(List<List<Card>> source) {
        List<List<Card>> copy = new ArrayList<>(source.size());
        for (List<Card> row : source) {
            copy.add(List.copyOf(row));
        }
        return List.copyOf(copy);
    }

    public int tokensOf(int seat, Gem gem) {
        return tokens[seat][gem.ordinal()];
    }

    public int totalTokens(int seat) {
        int sum = 0;
        for (int count : tokens[seat]) {
            sum += count;
        }
        return sum;
    }

    public int bonusOf(int seat, Gem gem) {
        return bonuses[seat][gem.ordinal()];
    }

    public int bankOf(Gem gem) {
        return bank[gem.ordinal()];
    }

    public boolean qualifies(Noble noble, int seat) {
        for (Gem gem : Gem.colors()) {
            if (bonuses[seat][gem.ordinal()] < noble.requirementOf(gem)) {
                return false;
            }
        }
        return true;
    }

    public List<Noble> qualifyingNobles(int seat) {
        List<Noble> result = new ArrayList<>(2);
        for (Noble noble : nobles) {
            if (qualifies(noble, seat)) {
                result.add(noble);
            }
        }
        return result;
    }

    /** 该座位能否支付这张卡（含黄金替代）。 */
    public boolean canAfford(int seat, Card card) {
        int goldNeeded = 0;
        for (Gem gem : Gem.colors()) {
            int need = Math.max(0, card.costOf(gem) - bonuses[seat][gem.ordinal()]);
            int have = tokens[seat][gem.ordinal()];
            if (need > have) {
                goldNeeded += need - have;
            }
        }
        return goldNeeded <= tokens[seat][Gem.GOLD.ordinal()];
    }
}
