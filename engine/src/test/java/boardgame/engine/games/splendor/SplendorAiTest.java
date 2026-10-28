package boardgame.engine.games.splendor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
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
import boardgame.engine.games.splendor.model.SplendorState;
import boardgame.engine.games.splendor.rules.SplendorDefinition;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SplendorAiTest {

    private final GameAdapter game = GameAdapter.of(new SplendorDefinition());

    @Test
    @DisplayName("MCTS 在预算内返回合法动作")
    void mctsReturnsLegalActionWithinBudget() {
        Object state = game.newGame(new NewGameConfig(2, 11L, List.of("a", "b")));
        MctsSearch search = new MctsSearch(game, new Random(3));

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            Action action = search.chooseMove(state, 0, SearchBudget.of(150)).orElseThrow();
            List<String> legal = game.legalActions(state).stream().map(Action::key).toList();
            assertTrue(legal.contains(action.key()));
        });
    }

    @Test
    @DisplayName("贪心 AI 对随机 AI 胜率明显更高")
    void greedyBeatsRandom() {
        int wins = 0;
        int games = 40;
        for (int i = 0; i < games; i++) {
            final int idx = i;
            int winner = playOne(new AiPlayerFn[] {
                    (state, seat) -> new GreedySearch(game, new Random(idx * 31L + seat))
                            .chooseMove(state, seat, SearchBudget.of(1)).orElseThrow(),
                    (state, seat) -> new RandomSearch(game, new Random(idx * 17L + seat))
                            .chooseMove(state, seat, SearchBudget.of(1)).orElseThrow()
            }, 2, 900L + i);
            if (winner == 0) {
                wins++;
            }
        }
        assertTrue(wins >= games * 3 / 4, "贪心胜率过低: " + wins + "/" + games);
    }

    @Test
    @DisplayName("短预算 MCTS 能与贪心打成平手以上")
    void mctsHoldsAgainstGreedy() {
        int wins = 0;
        int games = 4;
        for (int i = 0; i < games; i++) {
            final int idx = i;
            int winner = playOne(new AiPlayerFn[] {
                    (state, seat) -> new MctsSearch(game, new Random(1000L + idx))
                            .chooseMove(state, seat, SearchBudget.of(100)).orElseThrow(),
                    (state, seat) -> new GreedySearch(game, new Random(2000L + idx))
                            .chooseMove(state, seat, SearchBudget.of(1)).orElseThrow()
            }, 2, 500L + i);
            if (winner == 0) {
                wins++;
            }
        }
        assertTrue(wins >= 1, "MCTS 对贪心一局都没赢: " + wins + "/" + games);
    }

    @Test
    @DisplayName("一次完整对局能从开局走到终局")
    void fullGameReachesTerminal() {
        int winner = playOne(new AiPlayerFn[] {
                (state, seat) -> AiPlayer.choose(game, state, seat, Difficulty.EASY, new Random(7)),
                (state, seat) -> AiPlayer.choose(game, state, seat, Difficulty.EASY, new Random(8))
        }, 2, 1L);
        assertTrue(winner >= 0);
    }

    private int playOne(AiPlayerFn[] players, int seats, long seed) {
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
        assertEquals(true, outcome.finished());
        return outcome.winners().isEmpty() ? -1 : outcome.winners().get(0);
    }

    @FunctionalInterface
    private interface AiPlayerFn {
        Action choose(Object state, int seat);
    }
}
