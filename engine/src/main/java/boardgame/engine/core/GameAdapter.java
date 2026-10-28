package boardgame.engine.core;

import java.util.List;
import java.util.Random;

/**
 * 擦除泛型的规则入口，供会话层与 AI 层统一使用；类型转换集中在这一个类里。
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class GameAdapter {

    private final GameDefinition definition;

    private GameAdapter(GameDefinition definition) {
        this.definition = definition;
    }

    public static GameAdapter of(GameDefinition<?, ?> definition) {
        return new GameAdapter(definition);
    }

    public String id() {
        return definition.id();
    }

    public String displayName() {
        return definition.displayName();
    }

    public String description() {
        return definition.description();
    }

    public int minPlayers() {
        return definition.minPlayers();
    }

    public int maxPlayers() {
        return definition.maxPlayers();
    }

    public Object newGame(NewGameConfig config) {
        return definition.newGame(config);
    }

    public List<Action> legalActions(Object state) {
        return (List<Action>) (List) definition.legalActions(state);
    }

    public Object apply(Object state, Action action) {
        return definition.apply(state, action);
    }

    public boolean isTerminal(Object state) {
        return definition.isTerminal(state);
    }

    public GameOutcome outcome(Object state) {
        return definition.outcome(state);
    }

    public int currentSeat(Object state) {
        return definition.currentSeat(state);
    }

    public String phase(Object state) {
        return definition.phase(state);
    }

    public Object view(Object state, int seat) {
        return definition.view(state, seat);
    }

    public String describe(Object state, Action action) {
        return definition.describe(state, action);
    }

    public Object determinize(Object state, Random rng, int seat) {
        return definition.determinize(state, rng, seat);
    }

    public double heuristic(Object state, int seat) {
        return definition.heuristic(state, seat);
    }
}
