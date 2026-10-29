package boardgame.engine.ai;

import boardgame.engine.core.Action;
import boardgame.engine.core.GameAdapter;
import boardgame.engine.core.GameOutcome;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * 单机 AI 的通用搜索层。
 *
 * <p>用「确定性采样 + 每世界一棵 UCT 树」的 PIMC 方案：每个世界跑一小段 MCTS，
 * 再按各世界的选择投票。这样隐藏信息（牌堆顺序）不需要和树结构纠缠，
 * 新游戏只要实现 {@code determinize} 与 {@code heuristic} 就能直接用。
 */
public final class MctsSearch implements SearchStrategy {

    private static final double EXPLORATION = 1.15;
    private static final int ROLLOUT_DEPTH = 24;

    private final GameAdapter game;
    private final Random rng;

    public MctsSearch(GameAdapter game, Random rng) {
        this.game = game;
        this.rng = rng;
    }

    @Override
    public Optional<Action> chooseMove(Object rootState, int seat, SearchBudget budget) {
        return Optional.ofNullable(chooseWithStats(rootState, seat, budget).action());
    }

    /** 与 {@link #chooseMove} 同样的搜索，额外返回根节点每个动作的访问数与均值，供教练与复盘使用。 */
    public SearchResult chooseWithStats(Object rootState, int seat, SearchBudget budget) {
        List<Action> rootActions = game.legalActions(rootState);
        if (rootActions.isEmpty()) {
            return new SearchResult(null, List.of());
        }
        if (rootActions.size() == 1) {
            Action only = rootActions.get(0);
            return new SearchResult(only, List.of(new SearchResult.ActionStat(only.key(), 1, 1.0)));
        }

        long totalMs = Math.max(4, budget.timeMs());
        int worlds = (int) Math.max(1, Math.min(8, totalMs / 120));
        long perWorldNs = totalMs * 1_000_000L / worlds;

        Map<String, Integer> votes = new HashMap<>();
        Map<String, Double> voteWeight = new HashMap<>();
        Map<String, Integer> visitsByKey = new HashMap<>();
        Map<String, Double> totalByKey = new HashMap<>();
        Action fallback = rootActions.get(rng.nextInt(rootActions.size()));

        for (int world = 0; world < worlds; world++) {
            Object sampled = game.determinize(rootState, rng, seat);
            Node root = newNode(sampled, seat, null, null);
            long deadline = System.nanoTime() + perWorldNs;
            int iterations = 0;
            while (System.nanoTime() < deadline && iterations < 200_000) {
                iterate(root, seat);
                iterations++;
            }
            for (Node child : root.children) {
                visitsByKey.merge(child.action.key(), child.visits, Integer::sum);
                totalByKey.merge(child.action.key(), child.total, Double::sum);
            }
            Node best = bestByVisits(root);
            Action chosen = best == null ? fallback : best.action;
            votes.merge(chosen.key(), 1, Integer::sum);
            voteWeight.merge(chosen.key(), (double) (best == null ? 0 : best.visits), Double::sum);
        }

        String winner = null;
        int bestVotes = -1;
        double bestWeight = -1;
        for (Map.Entry<String, Integer> entry : votes.entrySet()) {
            double weight = voteWeight.getOrDefault(entry.getKey(), 0d);
            if (entry.getValue() > bestVotes || (entry.getValue() == bestVotes && weight > bestWeight)) {
                winner = entry.getKey();
                bestVotes = entry.getValue();
                bestWeight = weight;
            }
        }

        Map<String, SearchResult.ActionStat> stats = new LinkedHashMap<>();
        for (Action action : rootActions) {
            int visits = visitsByKey.getOrDefault(action.key(), 0);
            double total = totalByKey.getOrDefault(action.key(), 0d);
            stats.put(action.key(), new SearchResult.ActionStat(action.key(), visits,
                    visits == 0 ? 0d : total / visits));
        }

        Action chosen = fallback;
        if (winner != null) {
            for (Action action : rootActions) {
                if (action.key().equals(winner)) {
                    chosen = action;
                    break;
                }
            }
        }
        return new SearchResult(chosen, new ArrayList<>(stats.values()));
    }

    private void iterate(Node root, int seat) {
        Node node = root;
        while (!game.isTerminal(node.state) && node.untried.isEmpty() && !node.children.isEmpty()) {
            node = select(node);
        }
        if (game.isTerminal(node.state) || (node.untried.isEmpty() && node.children.isEmpty())) {
            backprop(node, node.state);
            return;
        }

        Action action = node.untried.remove(rng.nextInt(node.untried.size()));
        Object nextState = game.apply(node.state, action);
        Node child = newNode(nextState, game.currentSeat(nextState), node, action);
        node.children.add(child);

        Object endState = rollout(child.state);
        backprop(child, endState);
    }

    private Node newNode(Object state, int actor, Node parent, Action action) {
        Node node = new Node(state, actor, parent, action);
        node.untried.addAll(game.legalActions(state));
        return node;
    }

    private Node select(Node node) {
        Node best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double logVisits = Math.log(Math.max(2, node.visits));
        for (Node child : node.children) {
            double q = child.visits == 0 ? 1.0 : child.total / child.visits;
            double u = EXPLORATION * Math.sqrt(logVisits / (child.visits + 1));
            double score = q + u;
            if (score > bestScore) {
                bestScore = score;
                best = child;
            }
        }
        return best == null ? node : best;
    }

    private Object rollout(Object start) {
        Object state = start;
        for (int depth = 0; depth < ROLLOUT_DEPTH && !game.isTerminal(state); depth++) {
            List<Action> actions = game.legalActions(state);
            if (actions.isEmpty()) {
                break;
            }
            int actor = game.currentSeat(state);
            List<Action> candidates = game.rolloutActions(state, actions, actor, rng);
            List<Action> pool = candidates.isEmpty() ? actions : candidates;
            state = game.apply(state, pool.get(rng.nextInt(pool.size())));
        }
        return state;
    }

    private void backprop(Node leaf, Object leafState) {
        Node node = leaf;
        while (node != null) {
            node.visits++;
            if (node.parent != null) {
                node.total += valueFor(leafState, node.parent.actor);
            }
            node = node.parent;
        }
    }

    private double valueFor(Object state, int seat) {
        if (game.isTerminal(state)) {
            GameOutcome outcome = game.outcome(state);
            if (outcome.winners().contains(seat)) {
                return outcome.winners().size() > 1 ? 0.6 : 1.0;
            }
            return 0.0;
        }
        double h = game.heuristic(state, seat);
        return 1.0 / (1.0 + Math.exp(-h / 12.0));
    }

    private static Node bestByVisits(Node root) {
        Node best = null;
        for (Node child : root.children) {
            if (best == null || child.visits > best.visits) {
                best = child;
            }
        }
        return best;
    }

    private static final class Node {

        private final Object state;
        private final int actor;
        private final Node parent;
        private final Action action;
        private final List<Action> untried;
        private final List<Node> children = new ArrayList<>(24);
        private int visits;
        private double total;

        Node(Object state, int actor, Node parent, Action action) {
            this.state = state;
            this.actor = actor;
            this.parent = parent;
            this.action = action;
            this.untried = new ArrayList<>();
        }
    }
}
