package boardgame.engine.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import boardgame.engine.core.Action;
import boardgame.engine.core.NewGameConfig;
import boardgame.engine.games.azul.rules.AzulDefinition;
import boardgame.engine.games.splendor.model.SplendorAction;
import boardgame.engine.games.splendor.model.SplendorState;
import boardgame.engine.games.splendor.rules.SplendorDefinition;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RolloutPolicyTest {

    @Test
    @DisplayName("璀璨宝石的 rollout 候选非空且都是合法动作")
    void splendorRolloutNarrowsLegalActions() {
        SplendorDefinition game = new SplendorDefinition();
        SplendorState state = game.newGame(new NewGameConfig(2, 42L, List.of("a", "b")));
        List<SplendorAction> legal = game.legalActions(state);

        for (int seed = 0; seed < 20; seed++) {
            List<SplendorAction> narrowed = game.rolloutActions(state, legal, 0, new Random(seed));
            assertFalse(narrowed.isEmpty(), "候选不能为空");
            assertTrue(legal.containsAll(narrowed), "候选必须是合法动作的子集");
        }
    }

    @Test
    @DisplayName("没有买得起卡时退回全部合法动作")
    void splendorRolloutFallsBackWhenNoPurchase() {
        SplendorDefinition game = new SplendorDefinition();
        SplendorState state = game.newGame(new NewGameConfig(2, 7L, List.of("a", "b")));
        List<SplendorAction> legal = game.legalActions(state).stream()
                .filter(action -> !(action instanceof SplendorAction.BuyCard))
                .toList();

        List<SplendorAction> narrowed = game.rolloutActions(state, legal, 0, new Random(3));

        assertTrue(narrowed.size() == legal.size());
    }

    @Test
    @DisplayName("未实现钩子的游戏保持均匀随机")
    void defaultHookReturnsLegal() {
        AzulDefinition game = new AzulDefinition();
        var state = game.newGame(new NewGameConfig(2, 5L, List.of("a", "b")));
        var legal = game.legalActions(state);

        var rollout = game.rolloutActions(state, legal, 0, new Random(1));

        assertTrue(rollout == legal, "默认实现应原样返回");
    }
}
