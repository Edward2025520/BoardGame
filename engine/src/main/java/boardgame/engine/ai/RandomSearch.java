package boardgame.engine.ai;

import boardgame.engine.core.Action;
import boardgame.engine.core.GameAdapter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class RandomSearch implements SearchStrategy {

    private final GameAdapter game;
    private final Random rng;

    public RandomSearch(GameAdapter game, Random rng) {
        this.game = game;
        this.rng = rng;
    }

    @Override
    public Optional<Action> chooseMove(Object state, int seat, SearchBudget budget) {
        List<Action> actions = game.legalActions(state);
        if (actions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(actions.get(rng.nextInt(actions.size())));
    }
}
