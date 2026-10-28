package boardgame.engine.games.azul.rules;

import boardgame.engine.core.GameDefinition;
import boardgame.engine.core.GameOutcome;
import boardgame.engine.core.NewGameConfig;
import boardgame.engine.games.azul.model.AzulAction;
import boardgame.engine.games.azul.model.AzulPhase;
import boardgame.engine.games.azul.model.AzulPlayer;
import boardgame.engine.games.azul.model.AzulState;
import boardgame.engine.games.azul.model.PatternLine;
import boardgame.engine.games.azul.model.TileColor;
import boardgame.engine.games.azul.view.AzulView;
import boardgame.engine.games.azul.view.AzulView.CenterView;
import boardgame.engine.games.azul.view.AzulView.FactoryView;
import boardgame.engine.games.azul.view.AzulView.LineView;
import boardgame.engine.games.azul.view.AzulView.SeatView;
import boardgame.engine.games.azul.view.AzulView.WallCell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 花砖物语规则实现（基础版）。
 *
 * <p>取砖阶段由玩家选择；一轮结束后自动铺墙、计分、扣地板并补工厂。
 * 随机只出现在 newGame 的洗袋，袋用完之后按盖板顺序补，保证同 seed 可复现。
 */
public final class AzulDefinition implements GameDefinition<AzulState, AzulAction> {

    public static final String ID = "azul";

    private static final int TILES_PER_COLOR = 20;
    private static final int FACTORY_SIZE = 4;
    private static final int FLOOR_CAPACITY = 7;
    private static final int[] FLOOR_PENALTIES = {-1, -1, -2, -2, -2, -3, -3};

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "花砖物语";
    }

    @Override
    public String description() {
        return "从工厂取同色瓦片铺满图案行，再铺到墙面连分；先完成一整行者触发终局结算。";
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
    public AzulState newGame(NewGameConfig config) {
        int players = config.seatCount();
        if (players < minPlayers() || players > maxPlayers()) {
            throw new IllegalArgumentException("花砖物语只支持 2-4 人，收到 " + players);
        }
        Random rng = new Random(config.seed());

        List<TileColor> bag = new ArrayList<>(TILES_PER_COLOR * TileColor.COUNT);
        for (TileColor color : TileColor.values()) {
            for (int index = 0; index < TILES_PER_COLOR; index++) {
                bag.add(color);
            }
        }
        Collections.shuffle(bag, rng);

        int factoryCount = factoryCount(players);
        List<List<TileColor>> factories = new ArrayList<>(factoryCount);
        for (int index = 0; index < factoryCount; index++) {
            factories.add(draw(bag, FACTORY_SIZE));
        }

        List<AzulPlayer> seats = new ArrayList<>(players);
        for (int index = 0; index < players; index++) {
            seats.add(AzulPlayer.fresh());
        }

        return new AzulState(players, 0, AzulPhase.TAKING, 1, factories, List.of(), true, bag,
                List.of(), seats, 0);
    }

    public static int factoryCount(int players) {
        return players * 2 + 1;
    }

    private static List<TileColor> draw(List<TileColor> bag, int amount) {
        List<TileColor> drawn = new ArrayList<>(amount);
        for (int index = 0; index < amount && !bag.isEmpty(); index++) {
            drawn.add(bag.remove(bag.size() - 1));
        }
        return List.copyOf(drawn);
    }

    @Override
    public List<AzulAction> legalActions(AzulState state) {
        if (state.phase() == AzulPhase.OVER) {
            return List.of();
        }
        List<AzulAction> actions = new ArrayList<>();
        for (int factory = 0; factory < state.factories().size(); factory++) {
            collectActions(actions, state, factory);
        }
        collectActions(actions, state, -1);
        return actions;
    }

    private void collectActions(List<AzulAction> actions, AzulState state, int source) {
        List<TileColor> pile = source < 0 ? state.center() : state.factories().get(source);
        if (pile.isEmpty()) {
            return;
        }
        Map<TileColor, Integer> counts = new EnumMap<>(TileColor.class);
        for (TileColor tile : pile) {
            counts.merge(tile, 1, Integer::sum);
        }
        AzulPlayer player = state.player(state.currentSeat());
        for (Map.Entry<TileColor, Integer> entry : counts.entrySet()) {
            for (int line = 0; line < 5; line++) {
                if (player.lines().get(line).accepts(entry.getKey())) {
                    actions.add(new AzulAction.TakeTiles(source, entry.getKey(), line, entry.getValue()));
                }
            }
            actions.add(new AzulAction.TakeTiles(source, entry.getKey(), -1, entry.getValue()));
        }
    }

    @Override
    public AzulState apply(AzulState state, AzulAction action) {
        if (state.phase() == AzulPhase.OVER) {
            throw new IllegalStateException("对局已结束");
        }
        if (!(action instanceof AzulAction.TakeTiles take)) {
            throw new IllegalArgumentException("未知动作");
        }
        int available = state.countIn(take.source(), take.color());
        if (available <= 0 || available != take.count()) {
            throw new IllegalStateException("这个来源没有足够的 " + take.color());
        }
        int seat = state.currentSeat();
        AzulPlayer player = state.player(seat);
        if (take.targetLine() >= 0 && !player.lines().get(take.targetLine()).accepts(take.color())) {
            throw new IllegalStateException("这一行放不下该颜色");
        }

        List<List<TileColor>> factories = copyFactories(state.factories());
        List<TileColor> center = new ArrayList<>(state.center());
        List<TileColor> lid = new ArrayList<>(state.lid());
        boolean markerInCenter = state.centerHasMarker();
        boolean tookMarker = false;

        if (take.source() < 0) {
            center.removeIf(tile -> tile == take.color());
            if (markerInCenter) {
                tookMarker = true;
                markerInCenter = false;
            }
        } else {
            List<TileColor> factory = factories.get(take.source());
            List<TileColor> rest = new ArrayList<>();
            for (TileColor tile : factory) {
                if (tile != take.color()) {
                    rest.add(tile);
                }
            }
            factory.clear();
            center.addAll(rest);
        }

        List<PatternLine> lines = new ArrayList<>(player.lines());
        List<TileColor> floor = new ArrayList<>(player.floor());
        boolean hasMarker = player.hasMarker();
        int remaining = take.count();

        if (take.targetLine() >= 0) {
            PatternLine line = lines.get(take.targetLine());
            int place = Math.min(line.space(), remaining);
            if (place > 0) {
                lines.set(take.targetLine(), line.add(take.color(), place));
                remaining -= place;
            }
        }
        if (tookMarker) {
            hasMarker = true;
        }
        int freeSlots = Math.max(0, FLOOR_CAPACITY - (hasMarker ? 1 : 0) - floor.size());
        int floored = Math.min(remaining, freeSlots);
        for (int index = 0; index < floored; index++) {
            floor.add(take.color());
        }
        int discarded = remaining - floored;
        for (int index = 0; index < discarded; index++) {
            lid.add(take.color());
        }

        int score = player.score();
        List<AzulPlayer> players = new ArrayList<>(state.players());
        players.set(seat, new AzulPlayer(score, lines, player.wallMask(), floor, hasMarker));

        int nextSeat = (seat + 1) % state.playerCount();
        int firstPlayer = tookMarker ? seat : state.firstPlayerSeat();
        AzulState next = new AzulState(state.playerCount(), nextSeat, AzulPhase.TAKING, state.round(),
                factories, center, markerInCenter, state.bag(), lid, players, firstPlayer);

        return next.roundOver() ? endRound(next) : next;
    }

    private AzulState endRound(AzulState state) {
        List<AzulPlayer> players = new ArrayList<>(state.players());
        List<TileColor> lid = new ArrayList<>(state.lid());

        for (int seat = 0; seat < state.playerCount(); seat++) {
            AzulPlayer player = players.get(seat);
            int score = player.score();
            int wall = player.wallMask();
            List<PatternLine> lines = new ArrayList<>(player.lines());

            for (int row = 0; row < 5; row++) {
                PatternLine line = lines.get(row);
                if (!line.isFull()) {
                    continue;
                }
                int col = AzulWall.columnOf(row, line.color());
                if ((wall & (1 << (row * 5 + col))) == 0) {
                    score += AzulWall.scorePlacement(wall, row, col);
                    wall = AzulWall.withTile(wall, row, col);
                    // 一行只用掉一块，其余回盒
                    for (int extra = 1; extra < line.capacity(); extra++) {
                        lid.add(line.color());
                    }
                } else {
                    for (int extra = 0; extra < line.capacity(); extra++) {
                        lid.add(line.color());
                    }
                }
                lines.set(row, line.cleared());
            }

            for (int index = 0; index < player.floor().size(); index++) {
                score += FLOOR_PENALTIES[Math.min(index, FLOOR_PENALTIES.length - 1)];
            }
            if (player.hasMarker()) {
                score -= 1;
            }
            // 地板的瓦片退回盒子（盖板），保证瓦片守恒
            lid.addAll(player.floor());

            players.set(seat, new AzulPlayer(score, lines, wall, List.of(), false));
        }

        boolean finished = players.stream().anyMatch(AzulPlayer::hasFullRow);
        if (finished) {
            List<AzulPlayer> scored = new ArrayList<>(players.size());
            for (AzulPlayer player : players) {
                scored.add(new AzulPlayer(player.score() + finalBonus(player.wallMask()), player.lines(),
                        player.wallMask(), player.floor(), player.hasMarker()));
            }
            return new AzulState(state.playerCount(), state.firstPlayerSeat(), AzulPhase.OVER,
                    state.round(), state.factories(), state.center(), false, state.bag(), lid, scored,
                    state.firstPlayerSeat());
        }

        List<TileColor> bag = new ArrayList<>(state.bag());
        List<List<TileColor>> factories = new ArrayList<>(AzulDefinition.factoryCount(state.playerCount()));
        for (int factory = 0; factory < AzulDefinition.factoryCount(state.playerCount()); factory++) {
            if (bag.isEmpty() && !lid.isEmpty()) {
                bag.addAll(lid);
                lid.clear();
            }
            factories.add(draw(bag, FACTORY_SIZE));
        }

        // 极端情况下袋和盖板都空了，直接结束，避免无动作可走
        boolean dry = factories.stream().allMatch(List::isEmpty);
        return new AzulState(state.playerCount(), state.firstPlayerSeat(),
                dry ? AzulPhase.OVER : AzulPhase.TAKING, state.round() + 1, factories, List.of(), true,
                bag, lid, players, state.firstPlayerSeat());
    }

    private static int finalBonus(int wallMask) {
        int bonus = 0;
        for (int row = 0; row < 5; row++) {
            boolean full = true;
            for (int col = 0; col < 5; col++) {
                full &= (wallMask & (1 << (row * 5 + col))) != 0;
            }
            if (full) {
                bonus += 2;
            }
        }
        for (int col = 0; col < 5; col++) {
            boolean full = true;
            for (int row = 0; row < 5; row++) {
                full &= (wallMask & (1 << (row * 5 + col))) != 0;
            }
            if (full) {
                bonus += 7;
            }
        }
        for (TileColor color : TileColor.values()) {
            int count = 0;
            for (int row = 0; row < 5; row++) {
                int col = AzulWall.columnOf(row, color);
                if ((wallMask & (1 << (row * 5 + col))) != 0) {
                    count++;
                }
            }
            if (count == 5) {
                bonus += 10;
            }
        }
        return bonus;
    }

    @Override
    public boolean isTerminal(AzulState state) {
        return state.phase() == AzulPhase.OVER;
    }

    @Override
    public GameOutcome outcome(AzulState state) {
        int[] points = new int[state.playerCount()];
        for (int seat = 0; seat < state.playerCount(); seat++) {
            points[seat] = state.player(seat).score();
        }
        if (!isTerminal(state)) {
            return new GameOutcome(false, points, List.of());
        }
        int best = Integer.MIN_VALUE;
        for (int point : points) {
            best = Math.max(best, point);
        }
        List<Integer> winners = new ArrayList<>();
        for (int seat = 0; seat < points.length; seat++) {
            if (points[seat] == best) {
                winners.add(seat);
            }
        }
        return new GameOutcome(true, points, winners);
    }

    @Override
    public int currentSeat(AzulState state) {
        return state.currentSeat();
    }

    @Override
    public String phase(AzulState state) {
        return state.phase().name();
    }

    @Override
    public String describe(AzulState state, AzulAction action) {
        return action.label();
    }

    @Override
    public AzulView view(AzulState state, int seat) {
        List<FactoryView> factories = new ArrayList<>(state.factories().size());
        for (int index = 0; index < state.factories().size(); index++) {
            factories.add(new FactoryView(index + 1, ids(state.factories().get(index))));
        }

        List<SeatView> seats = new ArrayList<>(state.playerCount());
        for (int index = 0; index < state.playerCount(); index++) {
            AzulPlayer player = state.player(index);
            List<LineView> lines = new ArrayList<>(5);
            for (PatternLine line : player.lines()) {
                lines.add(new LineView(line.color() == null ? null : line.color().id(), line.count(),
                        line.capacity()));
            }
            List<List<WallCell>> wall = new ArrayList<>(5);
            for (int row = 0; row < 5; row++) {
                List<WallCell> cells = new ArrayList<>(5);
                for (int col = 0; col < 5; col++) {
                    boolean filled = (player.wallMask() & (1 << (row * 5 + col))) != 0;
                    cells.add(new WallCell(AzulWall.colorAt(row, col).id(), filled));
                }
                wall.add(cells);
            }
            seats.add(new SeatView(index, player.score(), lines, wall, ids(player.floor()),
                    player.hasMarker(), index == state.currentSeat(), index == state.firstPlayerSeat()));
        }

        return new AzulView(state.playerCount(), state.currentSeat(), state.round(),
                state.phase().name(), state.bag().size(), factories,
                new CenterView(ids(state.center()), state.centerHasMarker()), seats);
    }

    private static List<String> ids(List<TileColor> tiles) {
        List<String> result = new ArrayList<>(tiles.size());
        for (TileColor tile : tiles) {
            result.add(tile.id());
        }
        return result;
    }

    @Override
    public double heuristic(AzulState state, int seat) {
        double mine = seatScore(state, seat);
        double best = Double.NEGATIVE_INFINITY;
        for (int other = 0; other < state.playerCount(); other++) {
            if (other != seat) {
                best = Math.max(best, seatScore(state, other));
            }
        }
        return mine - best;
    }

    private static double seatScore(AzulState state, int seat) {
        AzulPlayer player = state.player(seat);
        double score = player.score() * 10.0;
        for (PatternLine line : player.lines()) {
            if (line.isEmpty()) {
                continue;
            }
            score += line.count() * 1.4;
            if (line.isFull()) {
                score += 4.0;
            }
        }
        score += Integer.bitCount(player.wallMask()) * 0.9;
        score -= player.floor().size() * 1.6;
        if (player.hasMarker()) {
            score -= 1.2;
        }
        return score;
    }

    @Override
    public AzulState determinize(AzulState state, Random rng, int seat) {
        List<TileColor> bag = new ArrayList<>(state.bag());
        Collections.shuffle(bag, rng);
        List<TileColor> lid = new ArrayList<>(state.lid());
        Collections.shuffle(lid, rng);
        return new AzulState(state.playerCount(), state.currentSeat(), state.phase(), state.round(),
                state.factories(), state.center(), state.centerHasMarker(), bag, lid, state.players(),
                state.firstPlayerSeat());
    }

    private static List<List<TileColor>> copyFactories(List<List<TileColor>> source) {
        List<List<TileColor>> copy = new ArrayList<>(source.size());
        for (List<TileColor> factory : source) {
            copy.add(new ArrayList<>(factory));
        }
        return copy;
    }
}
