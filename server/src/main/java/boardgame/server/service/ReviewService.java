package boardgame.server.service;

import boardgame.engine.ai.MctsSearch;
import boardgame.engine.ai.SearchBudget;
import boardgame.engine.ai.SearchResult;
import boardgame.engine.core.Action;
import boardgame.engine.store.GameSession;
import boardgame.engine.store.SessionStore;
import boardgame.server.api.ApiException;
import boardgame.server.api.dto.Dto;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

/** 局后复盘：逐手对比你实际下的与搜索认为更好的一手。 */
@Service
public class ReviewService {

    private static final long MOVE_BUDGET_MS = 350;
    private static final double MEANINGFUL_DELTA = 0.05;

    private final SessionStore store;

    public ReviewService(SessionStore store) {
        this.store = store;
    }

    public Dto.ReviewListDto list(String sessionId) {
        GameSession session = requireFinished(sessionId);
        List<GameSession.HumanMove> moves = session.humanMoves();
        List<Dto.ReviewItemDto> items = new ArrayList<>(moves.size());
        for (GameSession.HumanMove move : moves) {
            items.add(new Dto.ReviewItemDto(move.index(),
                    session.game().describe(move.stateBefore(), move.action())));
        }
        return new Dto.ReviewListDto(items.size(), items);
    }

    public Dto.ReviewMoveDto analyze(String sessionId, int index) {
        GameSession session = requireFinished(sessionId);
        List<GameSession.HumanMove> moves = session.humanMoves();
        if (index < 0 || index >= moves.size()) {
            throw ApiException.badRequest("复盘序号超出范围: " + index);
        }
        GameSession.HumanMove move = moves.get(index);
        SearchResult result = new MctsSearch(session.game(), new Random(session.seed() + index * 131L))
                .chooseWithStats(move.stateBefore(), move.seat(), SearchBudget.of(MOVE_BUDGET_MS));

        List<Action> legal = session.game().legalActions(move.stateBefore());
        SearchResult.ActionStat best = result.stats().stream()
                .max((left, right) -> Integer.compare(left.visits(), right.visits()))
                .orElse(new SearchResult.ActionStat(move.action().key(), 0, 0));
        SearchResult.ActionStat chosen = result.statOf(move.action().key());

        Action bestAction = legal.stream()
                .filter(action -> action.key().equals(best.key()))
                .findFirst()
                .orElse(move.action());

        double delta = Math.max(0, best.meanValue() - chosen.meanValue());
        if (best.visits() == 0 || chosen.visits() == 0 || delta < MEANINGFUL_DELTA) {
            delta = 0;
        }

        return new Dto.ReviewMoveDto(index,
                session.game().describe(move.stateBefore(), move.action()),
                session.game().describe(move.stateBefore(), bestAction),
                delta);
    }

    private GameSession requireFinished(String sessionId) {
        GameSession session = store.find(sessionId)
                .orElseThrow(() -> ApiException.notFound("对局不存在或已过期"));
        if (!session.finished()) {
            throw ApiException.conflict("对局还没结束，不能复盘");
        }
        return session;
    }
}
