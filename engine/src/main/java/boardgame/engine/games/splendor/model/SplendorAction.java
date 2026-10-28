package boardgame.engine.games.splendor.model;

import boardgame.engine.core.Action;
import java.util.List;

public sealed interface SplendorAction extends Action {

    /** 拿宝石：3 种不同色，或 2 个同色。 */
    record TakeGems(List<Gem> gems) implements SplendorAction {

        public TakeGems {
            gems = List.copyOf(gems);
        }

        @Override
        public String key() {
            return "take:" + GemText.join(gems);
        }

        @Override
        public String label() {
            return "拿 " + GemText.names(gems);
        }
    }

    /** 预留一张卡：fromRow 为 true 时 card 来自场上，否则从牌堆顶盲抽。 */
    record ReserveCard(int tier, boolean fromRow, Card card) implements SplendorAction {

        @Override
        public String key() {
            return fromRow ? "reserve:" + card.code() : "reserve:deck:" + tier;
        }

        @Override
        public String label() {
            return fromRow ? "预留 " + tier + " 级 " + card.bonus().label() + " 卡"
                    : "盲抽预留 " + tier + " 级卡";
        }
    }

    record BuyCard(Card card) implements SplendorAction {

        @Override
        public String key() {
            return "buy:" + card.code();
        }

        @Override
        public String label() {
            String suffix = card.points() > 0 ? "（" + card.points() + " 分）" : "";
            return "购买 " + card.tier() + " 级 " + card.bonus().label() + " 卡" + suffix;
        }
    }

    /** 弃宝石，直到手上回到 10 个。 */
    record DiscardGems(List<Gem> gems) implements SplendorAction {

        public DiscardGems {
            gems = List.copyOf(gems);
        }

        @Override
        public String key() {
            return "discard:" + GemText.join(gems);
        }

        @Override
        public String label() {
            return "弃 " + GemText.names(gems);
        }
    }

    record ChooseNoble(Noble noble) implements SplendorAction {

        @Override
        public String key() {
            return "noble:" + noble.code();
        }

        @Override
        public String label() {
            return "选择贵族 " + noble.code();
        }
    }

    /** 只在无任何合法动作时出现。 */
    record Pass() implements SplendorAction {

        @Override
        public String key() {
            return "pass";
        }

        @Override
        public String label() {
            return "跳过";
        }
    }
}
