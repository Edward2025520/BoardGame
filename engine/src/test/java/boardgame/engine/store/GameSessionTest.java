package boardgame.engine.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        GameSession session = new GameSession("s1", game, 2, List.of(0), Difficulty.MEDIUM, 42L, state);

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
        GameSession session = new GameSession("s2", game, 2, List.of(0), Difficulty.MEDIUM, 42L, state);

        Action first = session.legalActions().get(0);
        session.apply(first, session.describe(first));

        Action second = session.legalActions().get(0);
        session.apply(second, session.describe(second));

        assertEquals(1, session.log().get(1).seat(), "第二手是座位 1 打的");
        assertTrue(session.log().size() >= 2);
    }

    @Test
    @DisplayName("悔棋回到人类上一手之前，并截断日志与复盘记录")
    void undoRestoresPreviousHumanMove() {
        GameAdapter game = GameAdapter.of(new SplendorDefinition());
        Object state = game.newGame(new NewGameConfig(2, 42L, List.of("甲", "乙")));
        GameSession session = new GameSession("s3", game, 2, List.of(0), Difficulty.MEDIUM, 42L, state);

        Action first = session.legalActions().get(0);
        session.checkpoint(first);
        session.apply(first, session.describe(first));
        int versionAfterMove = session.version();
        assertTrue(session.canUndo());
        assertEquals(1, session.humanMoves().size());
        assertEquals(1, session.currentSeat());

        assertTrue(session.undo());

        assertEquals(0, session.currentSeat(), "应回到人类回合");
        assertEquals(0, session.humanMoves().size(), "复盘记录要回滚");
        assertEquals(versionAfterMove + 1, session.version(), "版本号继续递增");
        assertTrue(session.log().get(session.log().size() - 1).text().contains("悔棋"));
        assertFalse(session.canUndo(), "只有一步时，悔完就空了");
    }

    @Test
    @DisplayName("没有快照时悔棋失败")
    void undoWithoutSnapshotFails() {
        GameAdapter game = GameAdapter.of(new SplendorDefinition());
        Object state = game.newGame(new NewGameConfig(2, 1L, List.of("甲", "乙")));
        GameSession session = new GameSession("s4", game, 2, List.of(0), Difficulty.MEDIUM, 1L, state);

        assertFalse(session.undo());
    }
}
