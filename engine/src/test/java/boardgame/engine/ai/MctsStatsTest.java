package boardgame.engine.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import boardgame.engine.core.Action;
import boardgame.engine.core.GameAdapter;
import boardgame.engine.core.NewGameConfig;
import boardgame.engine.games.splendor.rules.SplendorDefinition;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MctsStatsTest {

    private final GameAdapter game = GameAdapter.of(new SplendorDefinition());

    @Test
    @DisplayName("chooseWithStats 返回合法动作，统计覆盖所有根动作")
    void statsCoverRootActions() {
        Object state = game.newGame(new NewGameConfig(2, 21L, List.of("a", "b")));
        Set<String> legal = game.legalActions(state).stream().map(Action::key).collect(Collectors.toSet());

        SearchResult result = new MctsSearch(game, new Random(5))
                .chooseWithStats(state, 0, SearchBudget.of(200));

        assertNotNull(result.action());
        assertTrue(legal.contains(result.action().key()));
        assertFalse(result.stats().isEmpty());
        for (SearchResult.ActionStat stat : result.stats()) {
            assertTrue(legal.contains(stat.key()), "统计里出现了非法动作: " + stat.key());
            assertTrue(stat.visits() >= 0);
        }
        assertTrue(result.stats().stream().anyMatch(stat -> stat.visits() > 0), "至少访问过一个动作");
    }

    @Test
    @DisplayName("statOf 对不存在的 key 返回零访问")
    void statOfUnknownKey() {
        SearchResult result = new SearchResult(null, List.of());
        assertEqualsZero(result);
    }

    private void assertEqualsZero(SearchResult result) {
        SearchResult.ActionStat stat = result.statOf("nope");
        assertTrue(stat.visits() == 0);
        assertTrue(stat.meanValue() == 0d);
    }
}
