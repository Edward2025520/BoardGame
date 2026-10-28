package boardgame.engine.games.azul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import boardgame.engine.core.Action;
import boardgame.engine.core.NewGameConfig;
import boardgame.engine.games.azul.model.AzulAction;
import boardgame.engine.games.azul.model.AzulPhase;
import boardgame.engine.games.azul.model.AzulPlayer;
import boardgame.engine.games.azul.model.AzulState;
import boardgame.engine.games.azul.model.PatternLine;
import boardgame.engine.games.azul.model.TileColor;
import boardgame.engine.games.azul.rules.AzulDefinition;
import boardgame.engine.games.azul.rules.AzulWall;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AzulRulesTest {

    private final AzulDefinition game = new AzulDefinition();

    @Test
    @DisplayName("两人局初始：5 个工厂各 4 块、袋剩 80、中央有首拿标记")
    void newGameSetup() {
        AzulState state = game.newGame(new NewGameConfig(2, 7L, List.of("甲", "乙")));

        assertEquals(5, state.factories().size());
        for (List<TileColor> factory : state.factories()) {
            assertEquals(4, factory.size());
        }
        assertEquals(80, state.bag().size());
        assertTrue(state.center().isEmpty());
        assertTrue(state.centerHasMarker());
        assertEquals(1, state.round());
        assertEquals(100, state.totalTiles());
    }

    @Test
    @DisplayName("人数决定工厂数量 2n+1")
    void factoryCountFollowsPlayers() {
        assertEquals(5, game.newGame(new NewGameConfig(2, 1L, List.of("a", "b"))).factories().size());
        assertEquals(7, game.newGame(new NewGameConfig(3, 1L, List.of("a", "b", "c"))).factories().size());
        assertEquals(9, game.newGame(new NewGameConfig(4, 1L, List.of("a", "b", "c", "d"))).factories().size());
    }

    @Test
    @DisplayName("从工厂取一色，其余瓦片推进中央")
    void takeMovesRestToCenter() {
        AzulState state = game.newGame(new NewGameConfig(2, 11L, List.of("甲", "乙")));
        TileColor color = state.factories().get(0).get(0);
        int count = state.countIn(0, color);
        int rest = 4 - count;

        AzulState next = game.apply(state, new AzulAction.TakeTiles(0, color, 4, count));

        assertTrue(next.factories().get(0).isEmpty());
        assertEquals(rest, next.center().size());
        assertEquals(count, next.player(0).lines().get(4).count());
        assertEquals(color, next.player(0).lines().get(4).color());
        assertEquals(1, next.currentSeat());
        assertEquals(100, next.totalTiles());
    }

    @Test
    @DisplayName("放不下的部分进地板")
    void overflowGoesToFloor() {
        AzulState state = crafted(
                List.of(List.of(TileColor.BLUE, TileColor.BLUE, TileColor.BLUE), List.of(TileColor.WHITE)),
                List.of(), true, AzulPlayer.fresh(), AzulPlayer.fresh());

        AzulState next = game.apply(state, new AzulAction.TakeTiles(0, TileColor.BLUE, 1, 3));

        assertEquals(2, next.player(0).lines().get(1).count());
        assertEquals(1, next.player(0).floor().size());
    }

    @Test
    @DisplayName("地板只有 7 格，多出的回盒")
    void floorIsCapped() {
        List<PatternLine> lines = List.of(
                PatternLine.empty(1), PatternLine.empty(2), PatternLine.empty(3),
                PatternLine.empty(4), PatternLine.empty(5));
        AzulPlayer loaded = new AzulPlayer(0, lines, 0,
                List.of(TileColor.BLUE, TileColor.BLUE, TileColor.BLUE, TileColor.BLUE, TileColor.BLUE,
                        TileColor.BLUE, TileColor.BLUE), false);
        AzulState state = crafted(List.of(List.of(TileColor.RED), List.of(TileColor.WHITE)),
                List.of(), true, loaded, AzulPlayer.fresh());

        AzulState next = game.apply(state, new AzulAction.TakeTiles(0, TileColor.RED, -1, 1));

        assertEquals(7, next.player(0).floor().size());
        assertEquals(1, next.lid().size());
    }

    @Test
    @DisplayName("从中央取砖会拿走首拿标记并扣 1 分")
    void centerTakeGrabsMarker() {
        AzulState state = crafted(List.of(List.of(TileColor.RED, TileColor.RED)),
                List.of(TileColor.BLUE), true, AzulPlayer.fresh(), AzulPlayer.fresh());

        AzulState next = game.apply(state, new AzulAction.TakeTiles(-1, TileColor.BLUE, 0, 1));

        assertTrue(next.player(0).hasMarker());
        assertEquals(0, next.firstPlayerSeat());
        assertFalse(next.centerHasMarker());
    }

    @Test
    @DisplayName("一轮结束后完成的行铺到墙上，地板扣分")
    void completedLineScoresAtRoundEnd() {
        List<PatternLine> lines = List.of(
                new PatternLine(TileColor.BLUE, 1, 1),
                PatternLine.empty(2), PatternLine.empty(3), PatternLine.empty(4), PatternLine.empty(5));
        AzulPlayer player = new AzulPlayer(0, lines, 0, List.of(), false);
        AzulState state = crafted(List.of(List.of(TileColor.RED)), List.of(), true, player, AzulPlayer.fresh());

        AzulState next = game.apply(state, new AzulAction.TakeTiles(0, TileColor.RED, -1, 1));

        // 墙上一块（1 分）+ 地板一块（-1 分）= 0
        assertEquals(0, next.player(0).score());
        assertTrue(next.player(0).wallHas(0, 0));
        assertTrue(next.player(0).lines().get(0).isEmpty());
        assertEquals(2, next.round());
    }

    @Test
    @DisplayName("完成一整行触发终局，加算整行 2 分")
    void fullRowEndsGame() {
        int fullRow = 0b11111;
        AzulPlayer player = new AzulPlayer(0,
                List.of(PatternLine.empty(1), PatternLine.empty(2), PatternLine.empty(3),
                        PatternLine.empty(4), PatternLine.empty(5)),
                fullRow, List.of(), false);
        AzulState state = crafted(List.of(List.of(TileColor.RED)), List.of(), true, player, AzulPlayer.fresh());

        AzulState next = game.apply(state, new AzulAction.TakeTiles(0, TileColor.RED, -1, 1));

        assertTrue(game.isTerminal(next));
        // 0 分 - 地板 1 分 + 整行 2 分
        assertEquals(1, next.player(0).score());
        assertEquals(List.of(0), game.outcome(next).winners());
    }

    @Test
    @DisplayName("墙面计分：孤立 1 分、横向连成一段、十字两段相加")
    void wallScoring() {
        assertEquals(1, AzulWall.scorePlacement(0, 2, 2));

        int horizontal = (1 << (0 * 5 + 0)) | (1 << (0 * 5 + 2));
        assertEquals(3, AzulWall.scorePlacement(horizontal, 0, 1));

        int cross = (1 << (1 * 5 + 1)) | (1 << (0 * 5 + 2)) | (1 << (2 * 5 + 2));
        assertEquals(5, AzulWall.scorePlacement(cross, 1, 2));
    }

    @Test
    @DisplayName("随机对局必然终局，且瓦片总数保持 100")
    void randomGamesTerminate() {
        Random rng = new Random(2026);
        for (int round = 0; round < 200; round++) {
            int players = 2 + rng.nextInt(3);
            AzulState state = game.newGame(new NewGameConfig(players, rng.nextLong(), names(players)));
            int steps = 0;
            while (!game.isTerminal(state)) {
                List<AzulAction> actions = game.legalActions(state);
                assertFalse(actions.isEmpty(), "没有合法动作但不能推进");
                state = game.apply(state, actions.get(rng.nextInt(actions.size())));
                assertEquals(100, state.totalTiles(), "瓦片数量不守恒");
                assertTrue(++steps < 3000, "对局步数异常");
            }
            assertFalse(game.outcome(state).winners().isEmpty());
        }
    }

    @Test
    @DisplayName("非法动作会被拒绝")
    void rejectsIllegalAction() {
        AzulState state = game.newGame(new NewGameConfig(2, 3L, List.of("甲", "乙")));
        TileColor color = state.factories().get(0).get(0);
        int wrongCount = state.countIn(0, color) + 1;

        assertThrows(IllegalStateException.class,
                () -> game.apply(state, new AzulAction.TakeTiles(0, color, 4, wrongCount)));
    }

    @Test
    @DisplayName("动作 key 在任意局面唯一")
    void actionKeysAreUnique() {
        AzulState state = game.newGame(new NewGameConfig(4, 5L, List.of("a", "b", "c", "d")));
        List<String> keys = game.legalActions(state).stream().map(Action::key).toList();
        assertEquals(keys.size(), keys.stream().distinct().count());
    }

    private static List<String> names(int players) {
        List<String> names = new ArrayList<>(players);
        for (int index = 0; index < players; index++) {
            names.add("P" + (index + 1));
        }
        return names;
    }

    private AzulState crafted(List<List<TileColor>> factories, List<TileColor> center, boolean marker,
                              AzulPlayer... players) {
        return new AzulState(players.length, 0, AzulPhase.TAKING, 1, factories, center, marker,
                List.of(), List.of(), List.of(players), 0);
    }
}
