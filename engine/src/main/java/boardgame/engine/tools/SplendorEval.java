package boardgame.engine.tools;

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
import boardgame.engine.games.splendor.rules.SplendorDefinition;
import java.util.List;
import java.util.Random;

/**
 * 命令行棋力评测：java -cp engine/target/classes boardgame.engine.tools.SplendorEval [局数]
 *
 * <p>输出各策略的胜场与平均得分，用来校准难度档，不参与打包运行。
 */
public final class SplendorEval {

    private SplendorEval() {
    }

    public static void main(String[] args) {
        int games = args.length > 0 ? Integer.parseInt(args[0]) : 40;
        GameAdapter game = GameAdapter.of(new SplendorDefinition());

        if (args.length > 1 && "--trace".equals(args[1])) {
            trace(game, 1);
            return;
        }

        run(game, games, "贪心", "随机", (state, seat, seed) -> new GreedySearch(game, new Random(seed))
                .chooseMove(state, seat, SearchBudget.of(1)).orElseThrow(),
                (state, seat, seed) -> new RandomSearch(game, new Random(seed))
                        .chooseMove(state, seat, SearchBudget.of(1)).orElseThrow());

        if (args.length > 1 && "--mcts".equals(args[1])) {
            int rounds = Math.max(1, games / 10);
            run(game, rounds, "MCTS", "贪心", (state, seat, seed) -> new MctsSearch(game, new Random(seed))
                            .chooseMove(state, seat, SearchBudget.of(150)).orElseThrow(),
                    (state, seat, seed) -> new GreedySearch(game, new Random(seed))
                            .chooseMove(state, seat, SearchBudget.of(1)).orElseThrow());
        }
    }

    private static void trace(GameAdapter game, int games) {
        Object state = game.newGame(new NewGameConfig(2, 99L, List.of("A", "B")));
        int turn = 0;
        while (!game.isTerminal(state)) {
            int seat = game.currentSeat(state);
            Action action = new GreedySearch(game, new Random(5L + turn))
                    .chooseMove(state, seat, SearchBudget.of(1)).orElseThrow();
            System.out.printf("t%02d seat%d %s%n", turn, seat, game.describe(state, action));
            state = game.apply(state, action);
            turn++;
            if (turn > 300) {
                break;
            }
        }
        GameOutcome outcome = game.outcome(state);
        System.out.printf("结束：step=%d points=%s winners=%s%n", turn,
                java.util.Arrays.toString(outcome.points()), outcome.winners());
    }

    private static void run(GameAdapter game, int games, String nameA, String nameB, Mover a, Mover b) {
        int winsA = 0;
        int winsB = 0;
        double pointsA = 0;
        double pointsB = 0;
        double cardsA = 0;
        double steps = 0;

        for (int i = 0; i < games; i++) {
            Object state = game.newGame(new NewGameConfig(2, 1234L + i, List.of("A", "B")));
            int guard = 0;
            while (!game.isTerminal(state)) {
                int seat = game.currentSeat(state);
                Action action = seat == 0 ? a.choose(state, seat, 77L * (i + 1))
                        : b.choose(state, seat, 31L * (i + 1));
                state = game.apply(state, action);
                guard++;
            }
            GameOutcome outcome = game.outcome(state);
            pointsA += outcome.pointsOf(0);
            pointsB += outcome.pointsOf(1);
            steps += guard;
            if (outcome.winners().contains(0)) {
                winsA++;
            }
            if (outcome.winners().contains(1)) {
                winsB++;
            }
        }
        System.out.printf("%s vs %s：%d 局，%s 胜 %d，%s 胜 %d，平 %d，平均分 %.1f : %.1f，平均步数 %.0f%n",
                nameA, nameB, games, nameA, winsA, nameB, winsB, games - winsA - winsB,
                pointsA / games, pointsB / games, steps / games);
    }

    @FunctionalInterface
    private interface Mover {
        Action choose(Object state, int seat, long seed);
    }
}
