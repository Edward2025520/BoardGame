package boardgame.engine.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import boardgame.engine.ai.Difficulty;
import boardgame.engine.core.Action;
import boardgame.engine.core.GameAdapter;
import boardgame.engine.core.NewGameConfig;
import boardgame.engine.games.splendor.rules.SplendorDefinition;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameSessionTest {

    @Test
    @DisplayName("日志记录的是行动者，而不是行动结束后的下家")
    void logKeepsTheActor() {
        GameAdapter game = GameAdapter.of(new SplendorDefinition());
        Object state = game.newGame(new NewGameConfig(2, 42L, List.of("甲", "乙")));
        GameSession session = new GameSession("s1", game, 2, List.of(0), Difficulty.NORMAL, 42L, state);

        Action first = session.legalActions().get(0);
        session.apply(first, session.describe(first));

        assertEquals(0, session.log().get(0).seat(), "第一手是座位 0 打的，应记给 0");
        assertEquals(1, session.currentSeat(), "打完应轮到座位 1");
    }

    @Test
    @DisplayName("AI 的一手记给 AI 自己")
    void logTracksAiSeat() {
        GameAdapter game = GameAdapter.of(new SplendorDefinition());
        Object state = game.newGame(new NewGameConfig(2, 42L, List.of("甲", "乙")));
        GameSession session = new GameSession("s2", game, 2, List.of(0), Difficulty.NORMAL, 42L, state);

        Action first = session.legalActions().get(0);
        session.apply(first, session.describe(first));

        Action second = session.legalActions().get(0);
        session.apply(second, session.describe(second));

        assertEquals(1, session.log().get(1).seat(), "第二手是座位 1 打的");
        assertTrue(session.log().size() >= 2);
    }
}
