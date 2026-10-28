package boardgame.engine.ai;

import boardgame.engine.core.Action;
import java.util.Optional;

public interface SearchStrategy {

    Optional<Action> chooseMove(Object state, int seat, SearchBudget budget);
}
