package boardgame.engine.games.splendor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import boardgame.engine.core.Action;
import boardgame.engine.core.NewGameConfig;
import boardgame.engine.games.splendor.model.Card;
import boardgame.engine.games.splendor.model.Gem;
import boardgame.engine.games.splendor.model.Noble;
import boardgame.engine.games.splendor.model.SplendorAction;
import boardgame.engine.games.splendor.model.SplendorPhase;
import boardgame.engine.games.splendor.model.SplendorState;
import boardgame.engine.games.splendor.rules.SplendorData;
import boardgame.engine.games.splendor.rules.SplendorDefinition;
import boardgame.engine.games.splendor.view.SplendorView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SplendorRulesTest {

    private final SplendorDefinition game = new SplendorDefinition();

    @Test
    @DisplayName("两人局初始配置：每人 4 个同色宝石、5 个黄金、3 张贵族、每行 4 张卡")
    void newGameSetup() {
        SplendorState state = game.newGame(new NewGameConfig(2, 42L, List.of("甲", "乙")));

        assertEquals(2, state.playerCount());
        for (Gem gem : Gem.colors()) {
            assertEquals(4, state.bankOf(gem), gem + " 数量不对");
        }
        assertEquals(5, state.bankOf(Gem.GOLD));
        assertEquals(3, state.nobles().size());
        assertEquals(4, state.rows().get(0).size());
        assertEquals(36, state.decks().get(0).size());
        assertEquals(26, state.decks().get(1).size());
        assertEquals(16, state.decks().get(2).size());
        assertEquals(SplendorPhase.PLAY, state.phase());
    }

    @Test
    @DisplayName("拿三种不同宝石成功后轮到下家")
    void takeThreeDifferent() {
        SplendorState state = game.newGame(new NewGameConfig(2, 1L, List.of("甲", "乙")));
        SplendorAction.TakeGems action = new SplendorAction.TakeGems(
                List.of(Gem.WHITE, Gem.BLUE, Gem.GREEN));

        SplendorState next = game.apply(state, action);

        assertEquals(1, next.tokensOf(0, Gem.WHITE));
        assertEquals(1, next.tokensOf(0, Gem.BLUE));
        assertEquals(1, next.tokensOf(0, Gem.GREEN));
        assertEquals(3, next.bankOf(Gem.WHITE));
        assertEquals(1, next.currentSeat());
    }

    @Test
    @DisplayName("同色宝石不足 4 个时不能一次拿两个")
    void cannotTakeTwoSameWhenBankLow() {
        SplendorState state = builder().bank(Gem.RED, 3).build();

        List<String> keys = game.legalActions(state).stream().map(Action::key).toList();

        assertFalse(keys.contains("take:RED,RED"));
    }

    @Test
    @DisplayName("超过 10 个宝石必须先弃到 10 个")
    void discardWhenOverTen() {
        SplendorState state = builder()
                .tokens(0, Gem.WHITE, 5)
                .tokens(0, Gem.BLUE, 5)
                .tokens(0, Gem.RED, 3)
                .build();

        SplendorState afterTake = game.apply(state, new SplendorAction.TakeGems(
                List.of(Gem.GREEN, Gem.BLACK)));

        assertEquals(SplendorPhase.DISCARD, afterTake.phase());
        assertEquals(0, afterTake.currentSeat());
        assertTrue(game.legalActions(afterTake).stream()
                .allMatch(a -> a instanceof SplendorAction.DiscardGems));

        SplendorState afterDiscard = game.apply(afterTake, new SplendorAction.DiscardGems(
                List.of(Gem.WHITE, Gem.WHITE, Gem.BLUE, Gem.BLUE, Gem.RED)));
        assertEquals(SplendorPhase.PLAY, afterDiscard.phase());
        assertEquals(10, afterDiscard.totalTokens(0));
        assertEquals(1, afterDiscard.currentSeat());
    }

    @Test
    @DisplayName("购买卡牌时永久宝石可抵扣成本，黄金可补差额")
    void buyWithBonusAndGold() {
        Card card = card("3g2B3w"); // 绿 3、黑 2、白 3，1 分
        SplendorState state = builder()
                .row(1, card)
                .tokens(0, Gem.GREEN, 2)
                .tokens(0, Gem.WHITE, 3)
                .tokens(0, Gem.GOLD, 2)
                .bonus(0, Gem.GREEN, 1)
                .build();

        SplendorState next = game.apply(state, new SplendorAction.BuyCard(card));

        assertEquals(1, next.points()[0]);
        assertEquals(1, next.cards()[0]);
        assertEquals(1, next.bonusOf(0, Gem.BLACK));
        assertEquals(0, next.tokensOf(0, Gem.GOLD));
        assertEquals(0, next.tokensOf(0, Gem.BLACK));
        assertTrue(next.rows().get(1).isEmpty());
        assertEquals(1, next.purchased().get(0).size(), "买到的卡要进已购列表");
        assertEquals(next.cards()[0], next.purchased().get(0).size(), "计数与列表长度一致");
        assertEquals("3g2B3w", next.purchased().get(0).get(0).code());
        assertTrue(next.purchased().get(1).isEmpty());
    }

    @Test
    @DisplayName("预留的卡买得起时能直接购买")
    void buyReservedCard() {
        Card card = card("3g");
        SplendorState state = builder()
                .reserved(0, card)
                .tokens(0, Gem.GREEN, 3)
                .build();

        List<String> keys = game.legalActions(state).stream().map(action -> action.key()).toList();
        assertTrue(keys.contains("buy:3g"), "预留卡应出现在可买动作里，实际: " + keys);

        SplendorState next = game.apply(state, new SplendorAction.BuyCard(card));

        assertTrue(next.reserved().get(0).isEmpty(), "买完要从预留区移除");
        assertEquals(1, next.purchased().get(0).size());
        assertEquals(0, next.tokensOf(0, Gem.GREEN));
        assertEquals(1, next.bonusOf(0, Gem.BLACK));
    }

    @Test
    @DisplayName("预留场上卡会补一张新卡并拿一个黄金")
    void reserveFromRowGivesGold() {
        Card card = card("1g1w1r1b");
        SplendorState state = builder().row(0, card).deck(0, card("2g2B")).build();

        SplendorState next = game.apply(state, new SplendorAction.ReserveCard(1, true, card));

        assertEquals(1, next.tokensOf(0, Gem.GOLD));
        assertEquals(1, next.reserved().get(0).size());
        assertEquals(1, next.rows().get(0).size());
        assertEquals("2g2B", next.rows().get(0).get(0).code());
        assertEquals(0, next.decks().get(0).size());
    }

    @Test
    @DisplayName("盲抽预留的牌离开牌堆，所有座位视图都能看到（单人版便于查看 AI）")
    void blindReserveIsVisibleInSinglePlayer() {
        SplendorState state = builder().deck(0, card("1g1w1r1b"), card("2g2B")).build();

        SplendorState next = game.apply(state, new SplendorAction.ReserveCard(1, false, null));

        assertEquals(1, next.reserved().get(0).size());
        SplendorView mine = game.view(next, 0);
        SplendorView theirs = game.view(next, 1);
        assertEquals(1, mine.seats().get(0).reserved().size());
        assertEquals(1, theirs.seats().get(0).reserved().size());
        assertEquals(1, theirs.seats().get(0).reservedCount());
        assertEquals(1, next.decks().get(0).size(), "牌堆要少一张");
    }

    @Test
    @DisplayName("满足一个贵族时自动获得，+3 分")
    void nobleAwardedAutomatically() {
        Noble noble = new Noble("test-noble", req(Gem.RED, 2, Gem.GREEN, 2));
        SplendorState state = builder()
                .nobles(noble)
                .bonus(0, Gem.RED, 2)
                .bonus(0, Gem.GREEN, 2)
                .build();

        SplendorState next = game.apply(state, new SplendorAction.TakeGems(
                List.of(Gem.WHITE, Gem.BLUE, Gem.BLACK)));

        assertEquals(3, next.points()[0]);
        assertTrue(next.nobles().isEmpty());
        assertEquals(1, next.currentSeat());
    }

    @Test
    @DisplayName("同时满足多个贵族时停下来让玩家选")
    void chooseAmongNobles() {
        Noble first = new Noble("n1", req(Gem.RED, 2));
        Noble second = new Noble("n2", req(Gem.GREEN, 2));
        SplendorState state = builder()
                .nobles(first, second)
                .bonus(0, Gem.RED, 2)
                .bonus(0, Gem.GREEN, 2)
                .build();

        SplendorState next = game.apply(state, new SplendorAction.TakeGems(
                List.of(Gem.WHITE, Gem.BLUE, Gem.BLACK)));

        assertEquals(SplendorPhase.CHOOSE_NOBLE, next.phase());
        assertEquals(2, game.legalActions(next).size());

        SplendorState chosen = game.apply(next, new SplendorAction.ChooseNoble(second));
        assertEquals(3, chosen.points()[0]);
        assertEquals(1, chosen.nobles().size());
        assertEquals("n1", chosen.nobles().get(0).code());
    }

    @Test
    @DisplayName("有人到 15 分后打完本轮才结束")
    void gameEndsAfterFinalRound() {
        SplendorState state = builder()
                .players(2)
                .seat(0)
                .points(0, 14)
                .points(1, 20)
                .row(0, card("4g"))
                .tokens(0, Gem.GREEN, 4)
                .build();

        SplendorState afterBuy = game.apply(state, new SplendorAction.BuyCard(card("4g")));
        assertFalse(game.isTerminal(afterBuy));
        assertEquals(1, afterBuy.currentSeat());

        SplendorState afterSecond = game.apply(afterBuy, new SplendorAction.TakeGems(
                List.of(Gem.WHITE, Gem.BLUE, Gem.BLACK)));
        assertTrue(game.isTerminal(afterSecond));
        assertEquals(List.of(1), game.outcome(afterSecond).winners());
    }

    @Test
    @DisplayName("同分时已购卡数少的玩家获胜")
    void tieBreakByFewerCards() {
        SplendorState state = new SplendorState(2, 1, SplendorPhase.PLAY,
                new int[2][Gem.values().length], new int[2][Gem.values().length],
                new int[] {15, 15}, new int[] {5, 3}, new int[Gem.values().length],
                List.of(List.of(), List.of(), List.of()),
                List.of(List.of(), List.of(), List.of()),
                List.of(List.of(), List.of()), List.of(), true, 0,
                SplendorState.emptyPurchased(2));
        SplendorState over = game.apply(state, new SplendorAction.Pass());

        assertEquals(List.of(1), game.outcome(over).winners());
    }

    @Test
    @DisplayName("任意局面下合法动作的 key 唯一")
    void actionKeysAreUnique() {
        SplendorState state = game.newGame(new NewGameConfig(4, 7L, List.of("a", "b", "c", "d")));

        List<String> keys = game.legalActions(state).stream().map(Action::key).toList();

        assertEquals(keys.size(), keys.stream().distinct().count());
        assertTrue(keys.size() > 10);
    }

    @Test
    @DisplayName("随机对局都能正常结束，不抛异常")
    void randomGamesTerminate() {
        Random rng = new Random(2026);
        for (int round = 0; round < 300; round++) {
            SplendorState state = game.newGame(new NewGameConfig(2 + rng.nextInt(3),
                    rng.nextLong(), List.of("a", "b", "c", "d")));
            int steps = 0;
            while (!game.isTerminal(state)) {
                List<SplendorAction> actions = game.legalActions(state);
                assertFalse(actions.isEmpty(), "无合法动作但不能推进");
                state = game.apply(state, actions.get(rng.nextInt(actions.size())));
                assertTrue(++steps < 5000, "对局步数异常");
            }
            assertNotNull(game.outcome(state));
            assertFalse(game.outcome(state).winners().isEmpty());
        }
    }

    private Card card(String code) {
        return SplendorData.ALL_CARDS.stream()
                .filter(c -> c.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("没有这张卡: " + code));
    }

    private int[] req(Gem gem, int amount) {
        int[] requirement = new int[Gem.values().length];
        requirement[gem.ordinal()] = amount;
        return requirement;
    }

    private int[] req(Gem first, int firstAmount, Gem second, int secondAmount) {
        int[] requirement = req(first, firstAmount);
        requirement[second.ordinal()] = secondAmount;
        return requirement;
    }

    private StateBuilder builder() {
        return new StateBuilder();
    }

    /** 手工搭局面，只测规则分支用。 */
    private final class StateBuilder {

        private int players = 2;
        private int seat;
        private SplendorPhase phase = SplendorPhase.PLAY;
        private final int[][] tokens = new int[4][Gem.values().length];
        private final int[][] bonuses = new int[4][Gem.values().length];
        private final int[] points = new int[4];
        private final int[] cards = new int[4];
        private final int[] bank = new int[Gem.values().length];
        private final List<List<Card>> rows = new ArrayList<>();
        private final List<List<Card>> decks = new ArrayList<>();
        private final List<List<Card>> reserved = new ArrayList<>();
        private final List<Noble> nobles = new ArrayList<>();
        private boolean finalRound;
        private int trigger = -1;

        StateBuilder() {
            for (Gem gem : Gem.colors()) {
                bank[gem.ordinal()] = 7;
            }
            bank[Gem.GOLD.ordinal()] = 5;
            for (int i = 0; i < 3; i++) {
                rows.add(new ArrayList<>());
                decks.add(new ArrayList<>());
            }
            for (int i = 0; i < 4; i++) {
                reserved.add(new ArrayList<>());
            }
        }

        StateBuilder players(int value) {
            players = value;
            return this;
        }

        StateBuilder seat(int value) {
            seat = value;
            return this;
        }

        StateBuilder phase(SplendorPhase value) {
            phase = value;
            return this;
        }

        StateBuilder bank(Gem gem, int amount) {
            bank[gem.ordinal()] = amount;
            return this;
        }

        StateBuilder tokens(int player, Gem gem, int amount) {
            tokens[player][gem.ordinal()] = amount;
            return this;
        }

        StateBuilder bonus(int player, Gem gem, int amount) {
            bonuses[player][gem.ordinal()] = amount;
            return this;
        }

        StateBuilder points(int player, int amount) {
            points[player] = amount;
            return this;
        }

        StateBuilder cards(int player, int amount) {
            cards[player] = amount;
            return this;
        }

        StateBuilder row(int tier, Card... value) {
            rows.get(tier).addAll(Arrays.asList(value));
            return this;
        }

        StateBuilder deck(int tier, Card... value) {
            decks.get(tier).addAll(Arrays.asList(value));
            return this;
        }

        StateBuilder reserved(int player, Card... value) {
            reserved.get(player).addAll(Arrays.asList(value));
            return this;
        }

        StateBuilder nobles(Noble... value) {
            nobles.addAll(Arrays.asList(value));
            return this;
        }

        SplendorState build() {
            return new SplendorState(players, seat, phase, tokens, bonuses, points, cards, bank,
                    rows, decks, reserved, nobles, finalRound, trigger,
                    SplendorState.emptyPurchased(players));
        }
    }
}
