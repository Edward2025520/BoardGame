package boardgame.engine.core;

import java.util.List;

/**
 * 终局结果。未结束时 points 仍返回当前分数，winners 为空。
 */
public record GameOutcome(boolean finished, int[] points, List<Integer> winners) {

    public GameOutcome {
        points = points.clone();
        winners = List.copyOf(winners);
    }

    public int pointsOf(int seat) {
        return seat >= 0 && seat < points.length ? points[seat] : 0;
    }
}
