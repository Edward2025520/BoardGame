package boardgame.engine.ai;

import boardgame.engine.core.Action;
import java.util.List;

/**
 * 一次搜索的结果：选中的动作，以及根节点每个动作的访问数与平均价值（从搜索座位视角）。
 */
public record SearchResult(Action action, List<ActionStat> stats) {

    public record ActionStat(String key, int visits, double meanValue) {
    }

    public ActionStat statOf(String actionKey) {
        for (ActionStat stat : stats) {
            if (stat.key().equals(actionKey)) {
                return stat;
            }
        }
        return new ActionStat(actionKey, 0, 0);
    }
}
