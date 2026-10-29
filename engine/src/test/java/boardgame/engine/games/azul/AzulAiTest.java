package boardgame.engine.games.azul;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import boardgame.engine.ai.AiPlayer;
import boardgame.engine.ai.Difficulty;
import boardgame.engine.ai.GreedySearch;
import boardgame.engine.ai.MctsSearch;
import boardgame.engine.ai.RandomSearch;
import boardgame.engine.ai.SearchBudget;
import boardgame.engine.core.Action;
import boardgame.engine.core.GameAdapter;
import boardgame.engine.core.GameOutcome;
import boardgame.engine.core.NewGameConfig;
import boardgame.engine.games.azul.rules.AzulDefinition;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AzulAiTest {

    private final GameAdapter game = GameAdapter.of(new AzulDefinition());

    @Test
    @DisplayName("MCTS 在预算内返回合法动作")
    void mctsReturnsLegalAction() {
        Object state = game.newGame(new NewGameConfig(2, 5L, List.of("a", "b")));
        MctsSearch search = new MctsSearch(game, new Random(3));

        Action action = search.chooseMove(state, 0, SearchBudget.of(300)).orElseThrow();
        List<String> keys = game.legalActions(state).stream().map(Action::key).toList();
        assertTrue(keys.contains(action.key()));
    }

    @Test
    @DisplayName("贪心 AI 对随机 AI 胜率更高")
    void greedyBeatsRandom() {
        int wins = 0;
        int games = 20;
        for (int index = 0; index < games; index++) {
            final int seed = index;
            int winner = playOne(new Mover[] {
                    (state, seat) -> new GreedySearch(game, new Random(seed * 31L + seat))
                            .chooseMove(state, seat, SearchBudget.of(1)).orElseThrow(),
                    (state, seat) -> new RandomSearch(game, new Random(seed * 17L + seat))
                            .chooseMove(state, seat, SearchBudget.of(1)).orElseThrow()
            }, 2, 400L + index);
            if (winner == 0) {
                wins++;
            }
        }
        assertTrue(wins >= games * 6 / 10, "贪心胜率太低: " + wins + "/" + games);
    }

    @Test
    @DisplayName("完整对局能走到终局")
    void fullGameTerminates() {
        int winner = playOne(new Mover[] {
                (state, seat) -> AiPlayer.choose(game, state, seat, Difficulty.BEGINNER, new Random(7)),
                (state, seat) -> AiPlayer.choose(game, state, seat, Difficulty.BEGINNER, new Random(8))
        }, 2, 1L);
        assertTrue(winner >= 0 || winner == -1);
    }

    private int playOne(Mover[] players, int seats, long seed) {
        Object state = game.newGame(new NewGameConfig(seats, seed, List.of("a", "b", "c", "d")));
        int guard = 0;
        while (!game.isTerminal(state)) {
            int seat = game.currentSeat(state);
            Action action = players[seat].choose(state, seat);
            assertNotNull(action, "AI 没有给出动作");
            state = game.apply(state, action);
            assertTrue(++guard < 5000, "对局步数异常");
        }
        GameOutcome outcome = game.outcome(state);
        return outcome.winners().isEmpty() ? -1 : outcome.winners().get(0);
    }

    @FunctionalInterface
    private interface Mover {
        Action choose(Object state, int seat);
    }
}
