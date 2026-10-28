package boardgame.engine.games.splendor.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * 一张发展卡。card 编号在原版牌库中唯一，可直接作为动作 key 与美术资源 key。
 */
public record Card(String code, int tier, int points, Gem bonus, int[] cost) {

    public Card {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(bonus, "bonus");
        cost = cost.clone();
    }

    public int costOf(Gem gem) {
        return cost[gem.ordinal()];
    }

    public int[] costCopy() {
        return cost.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Card card && code.equals(card.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return "Card[" + code + ", tier=" + tier + ", points=" + points + ", bonus=" + bonus
                + ", cost=" + Arrays.toString(cost) + "]";
    }
}
