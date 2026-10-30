package boardgame.engine.core;

import java.util.List;
import java.util.Random;

/**
 * 一款桌游的全部规则。实现必须满足：
 * <ul>
 *   <li>{@link #apply} 是纯函数，不修改入参；状态本身不可变。</li>
 *   <li>随机性只出现在 {@link #newGame}，seed 决定唯一的初始状态。</li>
 *   <li>隐藏信息通过 {@link #determinize} 采样，默认视为完全公开。</li>
 * </ul>
 */
public interface GameDefinition<S, A extends Action> {

    String id();

    String displayName();

    String description();

    int minPlayers();

    int maxPlayers();

    S newGame(NewGameConfig config);

    List<A> legalActions(S state);

    S apply(S state, A action);

    boolean isTerminal(S state);

    GameOutcome outcome(S state);

    /** 当前该谁行动。 */
    int currentSeat(S state);

    /** 阶段标识，前端用来切换交互，如 PLAY / DISCARD / OVER。 */
    String phase(S state);

    /** 渲染用 DTO，必须已按 seat 裁剪隐藏信息。 */
    Object view(S state, int seat);

    /** 写入动作日志的中文描述。 */
    String describe(S state, A action);

    /** 从 seat 视角对隐藏信息采样，返回一个与已知信息一致的世界。 */
    default S determinize(S state, Random rng, int seat) {
        return state;
    }

    /** 对未终局局面给 seat 的估值，越大越好。MCTS 截断时使用。 */
    default double heuristic(S state, int seat) {
        return 0d;
    }

    /**
     * MCTS 的 rollout 候选收窄钩子：默认原样返回（均匀随机）。
     * 游戏可以返回一个更小的候选集，让模拟更接近“会玩的人”。
     */
    default List<A> rolloutActions(S state, List<A> legal, int seat, Random rng) {
        return legal;
    }

    /**
     * 给动作附加“支付方案”之类的附加参数，默认忽略。
     * 方案本身是否合法由 {@link #apply} 校验。
     */
    default A withPayment(A action, List<Integer> plan) {
        return action;
    }
}
