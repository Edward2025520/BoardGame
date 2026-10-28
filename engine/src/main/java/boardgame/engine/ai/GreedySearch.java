package boardgame.engine.ai;

import boardgame.engine.core.Action;
import boardgame.engine.core.GameAdapter;
import boardgame.engine.core.GameOutcome;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** 一层贪心：只看动作执行后的估值。用作难度下限和 MCTS 的对照基线。 */
public final class GreedySearch implements SearchStrategy {

    private final GameAdapter game;
    private final Random rng;

    public GreedySearch(GameAdapter game, Random rng) {
        this.game = game;
        this.rng = rng;
    }

    @Override
    public Optional<Action> chooseMove(Object state, int seat, SearchBudget budget) {
        List<Action> actions = game.legalActions(state);
        if (actions.isEmpty()) {
            return Optional.empty();
        }
        Action best = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (Action action : actions) {
            Object next = game.apply(state, action);
            double value = evaluate(next, seat);
            if (value > bestValue || (value == bestValue && rng.nextBoolean())) {
                bestValue = value;
                best = action;
            }
        }
        return Optional.ofNullable(best);
    }

    private double evaluate(Object state, int seat) {
        if (game.isTerminal(state)) {
            GameOutcome outcome = game.outcome(state);
            if (outcome.winners().contains(seat)) {
                return 1.0;
            }
            return 0.0;
        }
        return game.heuristic(state, seat);
    }
}
