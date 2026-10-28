package boardgame.engine.ai;

/**
 * 搜索预算。时间优先，迭代次数是兜底上限。
 */
public record SearchBudget(long timeMs, int maxIterations) {

    public static SearchBudget of(long timeMs) {
        return new SearchBudget(timeMs, Integer.MAX_VALUE);
    }
}
