package boardgame.engine.games.splendor.rules;

import boardgame.engine.core.GameDefinition;
import boardgame.engine.core.GameOutcome;
import boardgame.engine.core.NewGameConfig;
import boardgame.engine.games.splendor.model.Card;
import boardgame.engine.games.splendor.model.Gem;
import boardgame.engine.games.splendor.model.GemText;
import boardgame.engine.games.splendor.model.Noble;
import boardgame.engine.games.splendor.model.SplendorAction;
import boardgame.engine.games.splendor.model.SplendorAction.BuyCard;
import boardgame.engine.games.splendor.model.SplendorAction.ChooseNoble;
import boardgame.engine.games.splendor.model.SplendorAction.DiscardGems;
import boardgame.engine.games.splendor.model.SplendorAction.Pass;
import boardgame.engine.games.splendor.model.SplendorAction.ReserveCard;
import boardgame.engine.games.splendor.model.SplendorAction.TakeGems;
import boardgame.engine.games.splendor.model.SplendorPhase;
import boardgame.engine.games.splendor.model.SplendorState;
import boardgame.engine.games.splendor.view.SplendorView;
import boardgame.engine.games.splendor.view.SplendorView.CardView;
import boardgame.engine.games.splendor.view.SplendorView.NobleView;
import boardgame.engine.games.splendor.view.SplendorView.RowView;
import boardgame.engine.games.splendor.view.SplendorView.SeatView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 璀璨宝石规则实现。座位 2-4，15 分触发结束轮，同分比已购卡数（少者胜）。
 */
public final class SplendorDefinition implements GameDefinition<SplendorState, SplendorAction> {

    public static final String ID = "splendor";

    private static final int HAND_LIMIT = 10;
    private static final int RESERVE_LIMIT = 3;
    private static final int WIN_POINTS = 15;
    private static final int ROW_SIZE = 4;
    /** 评测时可以用 -Dboardgame.rollout=false 关掉启发式 rollout 做对比。 */
    private static final boolean ROLLOUT_POLICY =
            !"false".equalsIgnoreCase(System.getProperty("boardgame.rollout", "true"));

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "璀璨宝石";
    }

    @Override
    public String description() {
        return "收集宝石、购买发展卡、吸引贵族，先到 15 分者获胜。";
    }

    @Override
    public int minPlayers() {
        return 2;
    }

    @Override
    public int maxPlayers() {
        return 4;
    }

    @Override
    public SplendorState newGame(NewGameConfig config) {
        int players = config.seatCount();
        if (players < minPlayers() || players > maxPlayers()) {
            throw new IllegalArgumentException("璀璨宝石只支持 2-4 人，收到 " + players);
        }
        Random rng = new Random(config.seed());

        int perColor = switch (players) {
            case 2 -> 4;
            case 3 -> 5;
            default -> 7;
        };
        int[] bank = new int[Gem.values().length];
        for (Gem gem : Gem.colors()) {
            bank[gem.ordinal()] = perColor;
        }
        bank[Gem.GOLD.ordinal()] = 5;

        List<List<Card>> rows = new ArrayList<>(3);
        List<List<Card>> decks = new ArrayList<>(3);
        for (int tier = 1; tier <= 3; tier++) {
            List<Card> pool = new ArrayList<>();
            for (Card card : SplendorData.ALL_CARDS) {
                if (card.tier() == tier) {
                    pool.add(card);
                }
            }
            Collections.shuffle(pool, rng);
            rows.add(new ArrayList<>(pool.subList(0, Math.min(ROW_SIZE, pool.size()))));
            decks.add(new ArrayList<>(pool.subList(rows.get(rows.size() - 1).size(), pool.size())));
        }

        List<Noble> noblePool = new ArrayList<>(SplendorData.ALL_NOBLES);
        Collections.shuffle(noblePool, rng);

        return new SplendorState(players, 0, SplendorPhase.PLAY,
                new int[players][Gem.values().length],
                new int[players][Gem.values().length],
                new int[players], new int[players],
                bank, rows, decks, emptyReserved(players),
                noblePool.subList(0, players + 1), false, -1, SplendorState.emptyPurchased(players));
    }

    private static List<List<Card>> emptyReserved(int players) {
        List<List<Card>> reserved = new ArrayList<>(players);
        for (int i = 0; i < players; i++) {
            reserved.add(new ArrayList<>());
        }
        return reserved;
    }

    @Override
    public List<SplendorAction> legalActions(SplendorState state) {
        return switch (state.phase()) {
            case OVER -> List.of();
            case DISCARD -> discardActions(state);
            case CHOOSE_NOBLE -> state.qualifyingNobles(state.currentSeat()).stream()
                    .map(noble -> (SplendorAction) new ChooseNoble(noble))
                    .toList();
            case PLAY -> playActions(state);
        };
    }

    private List<SplendorAction> playActions(SplendorState state) {
        int seat = state.currentSeat();
        List<SplendorAction> actions = new ArrayList<>(48);

        for (List<Card> row : state.rows()) {
            for (Card card : row) {
                if (state.canAfford(seat, card)) {
                    actions.add(new BuyCard(card));
                }
            }
        }
        for (Card card : state.reserved().get(seat)) {
            if (state.canAfford(seat, card)) {
                actions.add(new BuyCard(card));
            }
        }

        if (state.reserved().get(seat).size() < RESERVE_LIMIT) {
            for (int tier = 0; tier < 3; tier++) {
                for (Card card : state.rows().get(tier)) {
                    actions.add(new ReserveCard(tier + 1, true, card));
                }
                if (!state.decks().get(tier).isEmpty()) {
                    actions.add(new ReserveCard(tier + 1, false, null));
                }
            }
        }

        List<Gem> available = new ArrayList<>(5);
        for (Gem gem : Gem.colors()) {
            if (state.bankOf(gem) > 0) {
                available.add(gem);
            }
        }
        if (available.size() >= 3) {
            for (int a = 0; a < available.size(); a++) {
                for (int b = a + 1; b < available.size(); b++) {
                    for (int c = b + 1; c < available.size(); c++) {
                        actions.add(new TakeGems(List.of(available.get(a), available.get(b), available.get(c))));
                    }
                }
            }
        } else if (!available.isEmpty()) {
            actions.add(new TakeGems(available));
        }
        for (Gem gem : Gem.colors()) {
            if (state.bankOf(gem) >= 4) {
                actions.add(new TakeGems(List.of(gem, gem)));
            }
        }

        if (actions.isEmpty()) {
            actions.add(new Pass());
        }
        return actions;
    }

    private List<SplendorAction> discardActions(SplendorState state) {
        int seat = state.currentSeat();
        int excess = state.totalTokens(seat) - HAND_LIMIT;
        List<SplendorAction> actions = new ArrayList<>();
        int[] pool = new int[Gem.COLOR_COUNT];
        for (Gem gem : Gem.colors()) {
            pool[gem.ordinal()] = state.tokensOf(seat, gem);
        }
        collectDiscards(pool, 0, excess, new ArrayList<>(), actions);
        return actions;
    }

    private void collectDiscards(int[] pool, int start, int remaining, List<Gem> picked,
                                 List<SplendorAction> actions) {
        if (remaining == 0) {
            actions.add(new DiscardGems(List.copyOf(picked)));
            return;
        }
        for (int i = start; i < pool.length; i++) {
            if (pool[i] <= 0) {
                continue;
            }
            pool[i]--;
            picked.add(Gem.values()[i]);
            collectDiscards(pool, i, remaining - 1, picked, actions);
            picked.remove(picked.size() - 1);
            pool[i]++;
        }
    }

    @Override
    public SplendorState apply(SplendorState state, SplendorAction action) {
        if (state.phase() == SplendorPhase.OVER) {
            throw new IllegalStateException("对局已结束");
        }
        int actor = state.currentSeat();
        return switch (action) {
            case TakeGems take -> {
                SplendorState taken = takeGems(state, take);
                yield taken.phase() == SplendorPhase.DISCARD ? taken : settle(taken, actor);
            }
            case DiscardGems discard -> discardGems(state, discard);
            case ReserveCard reserve -> reserve(state, reserve);
            case BuyCard buy -> buy(state, buy);
            case ChooseNoble choose -> chooseNoble(state, choose);
            case Pass ignored -> noSeatCanAct(state)
                    ? new SplendorState(state.playerCount(), actor, SplendorPhase.OVER, state.tokens(),
                            state.bonuses(), state.points(), state.cards(), state.bank(), state.rows(),
                            state.decks(), state.reserved(), state.nobles(), true, actor,
                            state.purchased())
                    : advance(state, actor, state.finalRound(), state.finalRoundTrigger());
        };
    }

    /** 所有座位都拿不了、买不了、预留不了时判定死局，直接结束，避免无限跳过。 */
    private boolean noSeatCanAct(SplendorState state) {
        for (int seat = 0; seat < state.playerCount(); seat++) {
            SplendorState probe = new SplendorState(state.playerCount(), seat, SplendorPhase.PLAY,
                    state.tokens(), state.bonuses(), state.points(), state.cards(), state.bank(),
                    state.rows(), state.decks(), state.reserved(), state.nobles(),
                    state.finalRound(), state.finalRoundTrigger(), state.purchased());
            List<SplendorAction> actions = playActions(probe);
            if (actions.size() > 1 || !(actions.get(0) instanceof Pass)) {
                return false;
            }
        }
        return true;
    }

    private SplendorState takeGems(SplendorState state, TakeGems action) {
        int seat = state.currentSeat();
        int[][] tokens = copy(state.tokens());
        int[] bank = state.bank().clone();
        for (Gem gem : action.gems()) {
            if (bank[gem.ordinal()] <= 0) {
                throw new IllegalStateException("银行没有足够的 " + gem.label() + " 宝石");
            }
            bank[gem.ordinal()]--;
            tokens[seat][gem.ordinal()]++;
        }
        int total = 0;
        for (int count : tokens[seat]) {
            total += count;
        }
        SplendorPhase phase = total > HAND_LIMIT ? SplendorPhase.DISCARD : SplendorPhase.PLAY;
        return new SplendorState(state.playerCount(), seat, phase, tokens, state.bonuses(),
                state.points(), state.cards(), bank, state.rows(), state.decks(), state.reserved(),
                state.nobles(), state.finalRound(), state.finalRoundTrigger(), state.purchased());
    }

    private SplendorState discardGems(SplendorState state, DiscardGems action) {
        int seat = state.currentSeat();
        int[][] tokens = copy(state.tokens());
        for (Gem gem : action.gems()) {
            if (tokens[seat][gem.ordinal()] <= 0) {
                throw new IllegalStateException("没有可弃的 " + gem.label() + " 宝石");
            }
            tokens[seat][gem.ordinal()]--;
        }
        SplendorState discarded = new SplendorState(state.playerCount(), seat, SplendorPhase.PLAY,
                tokens, state.bonuses(), state.points(), state.cards(), state.bank(), state.rows(),
                state.decks(), state.reserved(), state.nobles(), state.finalRound(),
                state.finalRoundTrigger(), state.purchased());
        return settle(discarded, seat);
    }

    private SplendorState reserve(SplendorState state, ReserveCard action) {
        int seat = state.currentSeat();
        int tierIndex = action.tier() - 1;
        Card card = action.card();
        List<List<Card>> rows = copyRows(state.rows());
        List<List<Card>> decks = copyRows(state.decks());

        if (action.fromRow()) {
            List<Card> row = rows.get(tierIndex);
            int index = row.indexOf(card);
            if (index < 0) {
                throw new IllegalStateException("场上没有这张卡: " + card.code());
            }
            row.remove(index);
            if (!decks.get(tierIndex).isEmpty()) {
                row.add(index, decks.get(tierIndex).remove(0));
            }
        } else {
            if (decks.get(tierIndex).isEmpty()) {
                throw new IllegalStateException("牌堆为空");
            }
            card = decks.get(tierIndex).remove(0);
        }

        List<List<Card>> reserved = copyRows(state.reserved());
        if (reserved.get(seat).size() >= RESERVE_LIMIT) {
            throw new IllegalStateException("预留卡已达上限");
        }
        List<Card> mine = new ArrayList<>(reserved.get(seat));
        mine.add(card);
        reserved.set(seat, mine);

        int[][] tokens = copy(state.tokens());
        int[] bank = state.bank().clone();
        if (bank[Gem.GOLD.ordinal()] > 0) {
            bank[Gem.GOLD.ordinal()]--;
            tokens[seat][Gem.GOLD.ordinal()]++;
        }

        SplendorState reservedState = new SplendorState(state.playerCount(), seat, SplendorPhase.PLAY,
                tokens, state.bonuses(), state.points(), state.cards(), bank, rows, decks, reserved,
                state.nobles(), state.finalRound(), state.finalRoundTrigger(), state.purchased());
        return settle(reservedState, seat);
    }

    private SplendorState buy(SplendorState state, BuyCard action) {
        int seat = state.currentSeat();
        Card card = action.card();
        if (!state.canAfford(seat, card)) {
            throw new IllegalStateException("买不起这张卡: " + card.code());
        }

        int[][] tokens = copy(state.tokens());
        int[][] bonuses = copy(state.bonuses());
        int[] points = state.points().clone();
        int[] cards = state.cards().clone();
        int[] bank = state.bank().clone();

        List<Integer> plan = action.goldPlan();
        int[] goldUse = new int[Gem.colors().length];
        for (Gem gem : Gem.colors()) {
            int idx = gem.ordinal();
            int need = Math.max(0, card.costOf(gem) - bonuses[seat][idx]);
            int gold = plan.isEmpty() ? Math.max(0, need - tokens[seat][idx]) : action.goldFor(idx);
            if (gold < 0 || gold > need) {
                throw new IllegalStateException("支付方案不合法：" + gem.label() + " 的黄金数量超出范围");
            }
            int paid = need - gold;
            if (paid > tokens[seat][idx]) {
                throw new IllegalStateException("宝石不够：" + gem.label());
            }
            tokens[seat][idx] -= paid;
            bank[idx] += paid;
            goldUse[idx] = gold;
        }
        int totalGold = 0;
        for (int gold : goldUse) {
            totalGold += gold;
        }
        if (totalGold > tokens[seat][Gem.GOLD.ordinal()]) {
            throw new IllegalStateException("黄金不够");
        }
        tokens[seat][Gem.GOLD.ordinal()] -= totalGold;
        bank[Gem.GOLD.ordinal()] += totalGold;

        bonuses[seat][card.bonus().ordinal()]++;
        points[seat] += card.points();
        cards[seat]++;

        List<List<Card>> purchased = copyRows(state.purchased());
        List<Card> ownCards = new ArrayList<>(purchased.get(seat));
        ownCards.add(card);
        purchased.set(seat, ownCards);

        List<List<Card>> rows = copyRows(state.rows());
        List<List<Card>> decks = copyRows(state.decks());
        List<List<Card>> reserved = copyRows(state.reserved());
        int tierIndex = card.tier() - 1;
        int rowIndex = rows.get(tierIndex).indexOf(card);
        if (rowIndex >= 0) {
            rows.get(tierIndex).remove(rowIndex);
            if (!decks.get(tierIndex).isEmpty()) {
                rows.get(tierIndex).add(rowIndex, decks.get(tierIndex).remove(0));
            }
        } else {
            List<Card> mine = new ArrayList<>(reserved.get(seat));
            if (!mine.remove(card)) {
                throw new IllegalStateException("这张卡既不在场上也不在预留区: " + card.code());
            }
            reserved.set(seat, mine);
        }

        SplendorState bought = new SplendorState(state.playerCount(), seat, SplendorPhase.PLAY,
                tokens, bonuses, points, cards, bank, rows, decks, reserved, state.nobles(),
                state.finalRound(), state.finalRoundTrigger(), purchased);
        return settle(bought, seat);
    }

    private SplendorState chooseNoble(SplendorState state, ChooseNoble action) {
        int seat = state.currentSeat();
        SplendorState awarded = awardNoble(state, action.noble(), seat);
        return advance(awarded, seat, awarded.finalRound(), awarded.finalRoundTrigger());
    }

    private SplendorState settle(SplendorState state, int seat) {
        List<Noble> qualifying = state.qualifyingNobles(seat);
        if (qualifying.size() > 1) {
            return new SplendorState(state.playerCount(), seat, SplendorPhase.CHOOSE_NOBLE,
                    state.tokens(), state.bonuses(), state.points(), state.cards(), state.bank(),
                    state.rows(), state.decks(), state.reserved(), state.nobles(),
                    state.finalRound(), state.finalRoundTrigger(), state.purchased());
        }
        SplendorState awarded = qualifying.size() == 1
                ? awardNoble(state, qualifying.get(0), seat)
                : state;
        return advance(awarded, seat, awarded.finalRound(), awarded.finalRoundTrigger());
    }

    private SplendorState awardNoble(SplendorState state, Noble noble, int seat) {
        List<Noble> nobles = new ArrayList<>(state.nobles());
        if (!nobles.remove(noble)) {
            throw new IllegalStateException("贵族已不在场上: " + noble.code());
        }
        int[] points = state.points().clone();
        points[seat] += 3;
        return new SplendorState(state.playerCount(), seat, state.phase(), state.tokens(),
                state.bonuses(), points, state.cards(), state.bank(), state.rows(), state.decks(),
                state.reserved(), nobles, state.finalRound(), state.finalRoundTrigger(),
                state.purchased());
    }

    private SplendorState advance(SplendorState state, int actor, boolean finalRound, int trigger) {
        boolean closing = finalRound || state.points()[actor] >= WIN_POINTS;
        int endSeat = finalRound ? trigger : (closing ? actor : -1);
        int next = (actor + 1) % state.playerCount();

        if (closing && next == endSeat) {
            return new SplendorState(state.playerCount(), actor, SplendorPhase.OVER, state.tokens(),
                    state.bonuses(), state.points(), state.cards(), state.bank(), state.rows(),
                    state.decks(), state.reserved(), state.nobles(), true, endSeat,
                    state.purchased());
        }
        return new SplendorState(state.playerCount(), next, SplendorPhase.PLAY, state.tokens(),
                state.bonuses(), state.points(), state.cards(), state.bank(), state.rows(),
                state.decks(), state.reserved(), state.nobles(), closing, endSeat,
                state.purchased());
    }

    @Override
    public boolean isTerminal(SplendorState state) {
        return state.phase() == SplendorPhase.OVER;
    }

    @Override
    public GameOutcome outcome(SplendorState state) {
        int[] points = state.points().clone();
        if (!isTerminal(state)) {
            return new GameOutcome(false, points, List.of());
        }
        int best = Integer.MIN_VALUE;
        for (int point : points) {
            best = Math.max(best, point);
        }
        List<Integer> leaders = new ArrayList<>();
        for (int seat = 0; seat < points.length; seat++) {
            if (points[seat] == best) {
                leaders.add(seat);
            }
        }
        int fewest = Integer.MAX_VALUE;
        for (int seat : leaders) {
            fewest = Math.min(fewest, state.cards()[seat]);
        }
        List<Integer> winners = new ArrayList<>();
        for (int seat : leaders) {
            if (state.cards()[seat] == fewest) {
                winners.add(seat);
            }
        }
        return new GameOutcome(true, points, winners);
    }

    @Override
    public int currentSeat(SplendorState state) {
        return state.currentSeat();
    }

    @Override
    public String phase(SplendorState state) {
        return state.phase().name();
    }

    @Override
    public SplendorAction withPayment(SplendorAction action, List<Integer> plan) {
        if (plan == null || plan.isEmpty() || !(action instanceof BuyCard buy)) {
            return action;
        }
        return new BuyCard(buy.card(), plan);
    }

    @Override
    public String describe(SplendorState state, SplendorAction action) {
        return switch (action) {
            case TakeGems take -> "拿了 " + GemText.names(take.gems()) + " 宝石";
            case DiscardGems discard -> "弃掉 " + GemText.names(discard.gems());
            case ReserveCard reserve -> reserve.fromRow()
                    ? "预留 " + reserve.tier() + " 级 " + reserve.card().bonus().label() + " 卡"
                    : "盲抽预留 " + reserve.tier() + " 级卡";
            case BuyCard buy -> {
                Card card = buy.card();
                String name = card.tier() + " 级 " + card.bonus().label() + " 卡";
                String base = card.points() > 0 ? "购买 " + name + "，+" + card.points() + " 分" : "购买 " + name;
                int gold = 0;
                for (int use : buy.goldPlan()) {
                    gold += use;
                }
                yield gold > 0 ? base + "，用 " + gold + " 黄金" : base;
            }
            case ChooseNoble choose -> "获得贵族 " + choose.noble().code() + "，+3 分";
            case Pass ignored -> "跳过";
        };
    }

    @Override
    public SplendorView view(SplendorState state, int seat) {
        List<SeatView> seats = new ArrayList<>(state.playerCount());
        for (int i = 0; i < state.playerCount(); i++) {
            // 单人版：所有座位的预留卡都返回，方便玩家看清 AI 手里有什么；
            // 以后做联机时需要按座位重新裁剪。
            List<CardView> reserved = new ArrayList<>();
            for (Card card : state.reserved().get(i)) {
                reserved.add(cardView(card));
            }
            List<CardView> purchased = new ArrayList<>();
            for (Card card : state.purchased().get(i)) {
                purchased.add(cardView(card));
            }
            seats.add(new SeatView(i, state.points()[i], state.cards()[i],
                    state.tokens()[i].clone(), state.bonuses()[i].clone(),
                    state.reserved().get(i).size(), reserved, purchased, i == state.currentSeat()));
        }

        List<RowView> rows = new ArrayList<>(3);
        for (int tier = 0; tier < 3; tier++) {
            List<CardView> cards = new ArrayList<>();
            for (Card card : state.rows().get(tier)) {
                cards.add(cardView(card));
            }
            rows.add(new RowView(tier + 1, cards, state.decks().get(tier).size()));
        }

        List<NobleView> nobles = new ArrayList<>();
        for (Noble noble : state.nobles()) {
            nobles.add(new NobleView(noble.code(), costMap(noble.requirementCopy()),
                    state.qualifies(noble, seat)));
        }

        return new SplendorView(state.playerCount(), state.currentSeat(), state.phase().name(),
                state.finalRound(), state.bank().clone(), state.bankOf(Gem.GOLD), RESERVE_LIMIT,
                seats, rows, nobles);
    }

    private static CardView cardView(Card card) {
        return new CardView(card.code(), card.tier(), card.points(), card.bonus().id(),
                costMap(card.costCopy()));
    }

    private static Map<String, Integer> costMap(int[] cost) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Gem gem : Gem.colors()) {
            if (cost[gem.ordinal()] > 0) {
                map.put(gem.id(), cost[gem.ordinal()]);
            }
        }
        return map;
    }

    @Override
    public List<SplendorAction> rolloutActions(SplendorState state, List<SplendorAction> legal, int seat,
                                               Random rng) {
        if (!ROLLOUT_POLICY || legal.size() <= 1 || rng.nextDouble() < 0.2) {
            return legal;
        }
        List<SplendorAction> buys = new ArrayList<>();
        for (SplendorAction action : legal) {
            if (action instanceof BuyCard) {
                buys.add(action);
            }
        }
        if (buys.isEmpty()) {
            return legal;
        }
        // 模拟时优先买高分卡，避免随机 rollout 把好局走废
        buys.sort((left, right) -> Integer.compare(((BuyCard) right).card().points(),
                ((BuyCard) left).card().points()));
        return buys.size() > 3 ? List.copyOf(buys.subList(0, 3)) : List.copyOf(buys);
    }

    @Override
    public double heuristic(SplendorState state, int seat) {
        double mine = rawScore(state, seat);
        double best = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < state.playerCount(); i++) {
            if (i != seat) {
                best = Math.max(best, rawScore(state, i));
            }
        }
        return mine - best;
    }

    private static double rawScore(SplendorState state, int seat) {
        double score = state.points()[seat] * 12.0 + state.cards()[seat] * 0.8;
        for (Gem gem : Gem.colors()) {
            score += state.bonusOf(seat, gem) * 3.0;
        }
        int total = state.totalTokens(seat);
        score += total * 0.1;
        score += state.tokensOf(seat, Gem.GOLD) * 0.4;

        // 离买得起最近的那张牌有多近，决定这手该攒哪个颜色
        double bestProgress = 0;
        for (List<Card> row : state.rows()) {
            for (Card card : row) {
                bestProgress = Math.max(bestProgress, progress(state, seat, card));
            }
        }
        for (Card card : state.reserved().get(seat)) {
            bestProgress = Math.max(bestProgress, progress(state, seat, card));
        }
        score += bestProgress;

        if (total > 8) {
            // 囤到 10 个宝石却不买牌是典型的坏棋，也是死局的来源
            score -= (total - 8) * 1.2;
        }
        return score;
    }

    /** 还差几个宝石能买下这张牌；越接近，这一手的价值越高。 */
    private static double progress(SplendorState state, int seat, Card card) {
        int missing = 0;
        for (Gem gem : Gem.colors()) {
            int diff = card.costOf(gem) - state.bonusOf(seat, gem) - state.tokensOf(seat, gem);
            if (diff > 0) {
                missing += diff;
            }
        }
        double value = card.points() * 12.0 + 4.0;
        if (missing == 0) {
            return value;
        }
        return value * Math.max(0d, 1d - missing / 5d);
    }

    private static int[][] copy(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    private static int[] copy(int[] source) {
        return source.clone();
    }

    private static List<List<Card>> copyRows(List<List<Card>> source) {
        List<List<Card>> copy = new ArrayList<>(source.size());
        for (List<Card> row : source) {
            copy.add(new ArrayList<>(row));
        }
        return copy;
    }
}
