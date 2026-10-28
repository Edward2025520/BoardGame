package boardgame.engine.ai;

import boardgame.engine.core.Action;
import boardgame.engine.core.GameAdapter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 难度分档的唯一入口：低难度用贪心加失误，高难度用搜索。
 * 换游戏不需要改这里，只要游戏实现了 heuristic 与 determinize。
 */
public final class AiPlayer {

    private AiPlayer() {
    }

    public static Action choose(GameAdapter game, Object state, int seat, Difficulty difficulty,
                                Random rng) {
        List<Action> actions = game.legalActions(state);
        if (actions.isEmpty()) {
            return null;
        }
        if (actions.size() == 1) {
            return actions.get(0);
        }
        if (rng.nextDouble() < difficulty.blunderRate()) {
            return actions.get(rng.nextInt(actions.size()));
        }

        SearchStrategy strategy = difficulty == Difficulty.EASY
                ? new GreedySearch(game, rng)
                : new MctsSearch(game, rng);
        Optional<Action> move = strategy.chooseMove(state, seat, SearchBudget.of(difficulty.timeMs()));
        return move.orElse(actions.get(rng.nextInt(actions.size())));
    }
}
