package boardgame.engine.store;

import boardgame.engine.ai.Difficulty;
import boardgame.engine.core.Action;
import boardgame.engine.core.GameAdapter;
import boardgame.engine.core.GameOutcome;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 一局对战的运行时状态。所有变更必须在 {@link #lock()} 内完成，保证动作串行。
 *
 * <p>人类动作前会压入快照，用于悔棋与复盘；AI 动作不压栈。
 */
public final class GameSession {

    private static final int MAX_SNAPSHOTS = 50;

    private final String id;
    private final GameAdapter game;
    private final int seatCount;
    private final List<Integer> humanSeats;
    private final long seed;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<LogEntry> log = new CopyOnWriteArrayList<>();
    private final Deque<Snapshot> snapshots = new ArrayDeque<>();
    private final List<HumanMove> humanMoves = new ArrayList<>();
    private final Instant createdAt = Instant.now();

    private volatile Difficulty difficulty;
    private volatile Object state;
    private volatile int version;
    private volatile Instant updatedAt = this.createdAt;

    public GameSession(String id, GameAdapter game, int seatCount, List<Integer> humanSeats,
                       Difficulty difficulty, long seed, Object state) {
        this.id = id;
        this.game = game;
        this.seatCount = seatCount;
        this.humanSeats = List.copyOf(humanSeats);
        this.difficulty = difficulty;
        this.seed = seed;
        this.state = state;
    }

    public String id() {
        return id;
    }

    public GameAdapter game() {
        return game;
    }

    public int seatCount() {
        return seatCount;
    }

    public List<Integer> humanSeats() {
        return humanSeats;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        this.updatedAt = Instant.now();
    }

    public long seed() {
        return seed;
    }

    public ReentrantLock lock() {
        return lock;
    }

    public Object state() {
        return state;
    }

    public int version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<LogEntry> log() {
        return List.copyOf(log);
    }

    public boolean isHumanSeat(int seat) {
        return humanSeats.contains(seat);
    }

    public int currentSeat() {
        return game.currentSeat(state);
    }

    public String phase() {
        return game.phase(state);
    }

    public boolean finished() {
        return game.isTerminal(state);
    }

    public GameOutcome outcome() {
        return game.outcome(state);
    }

    public List<Action> legalActions() {
        return game.legalActions(state);
    }

    /** 每个人类座位一份已裁剪隐藏信息的视图，AI 座位不返回。 */
    public Map<Integer, Object> views() {
        Map<Integer, Object> views = new LinkedHashMap<>();
        List<Integer> sorted = new ArrayList<>(humanSeats);
        sorted.sort(Integer::compareTo);
        for (int seat : sorted) {
            views.put(seat, game.view(state, seat));
        }
        return views;
    }

    /** 返回动作的中文描述，调用方负责在 apply 之前取。 */
    public String describe(Action action) {
        return game.describe(state, action);
    }

    /** 人类动作提交前调用：记录快照与复盘用的局面。 */
    public void checkpoint(Action action) {
        if (snapshots.size() >= MAX_SNAPSHOTS) {
            snapshots.removeFirst();
        }
        snapshots.addLast(new Snapshot(state, version, log.size(), humanMoves.size()));
        humanMoves.add(new HumanMove(state, action, humanMoves.size(), game.currentSeat(state)));
    }

    public void apply(Action action, String description) {
        // 必须先记下行动者：apply 之后 currentSeat 已经轮到下家了
        int actor = game.currentSeat(state);
        state = game.apply(state, action);
        version++;
        updatedAt = Instant.now();
        log.add(new LogEntry(version, actor, description, updatedAt.toEpochMilli()));
    }

    /** 回到最近一次人类动作之前；返回是否成功。 */
    public boolean undo() {
        Snapshot snapshot = snapshots.pollLast();
        if (snapshot == null) {
            return false;
        }
        state = snapshot.state();
        version++;
        updatedAt = Instant.now();
        while (log.size() > snapshot.logSize()) {
            log.remove(log.size() - 1);
        }
        while (humanMoves.size() > snapshot.moveCount()) {
            humanMoves.remove(humanMoves.size() - 1);
        }
        log.add(new LogEntry(version, currentSeat(), "—— 悔棋 ——", updatedAt.toEpochMilli()));
        return true;
    }

    public boolean canUndo() {
        return !snapshots.isEmpty() && !finished();
    }

    public List<HumanMove> humanMoves() {
        return List.copyOf(humanMoves);
    }

    public void note(String description) {
        updatedAt = Instant.now();
        log.add(new LogEntry(version, currentSeat(), description, updatedAt.toEpochMilli()));
    }

    public record Snapshot(Object state, int version, int logSize, int moveCount) {
    }

    public record HumanMove(Object stateBefore, Action action, int index, int seat) {
    }

    public record LogEntry(int version, int seat, String text, long at) {
    }
}
