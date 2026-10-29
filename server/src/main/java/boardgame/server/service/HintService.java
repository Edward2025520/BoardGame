package boardgame.server.service;

import boardgame.engine.ai.MctsSearch;
import boardgame.engine.ai.SearchBudget;
import boardgame.engine.ai.SearchResult;
import boardgame.engine.store.GameSession;
import boardgame.engine.store.SessionStore;
import boardgame.server.api.ApiException;
import boardgame.server.api.dto.Dto;
import java.util.Random;
import org.springframework.stereotype.Service;

/** AI 教练「支一招」：在当前局面跑一次固定预算的搜索，不修改会话。 */
@Service
public class HintService {

    private static final long HINT_BUDGET_MS = 1_500;

    private final SessionStore store;

    public HintService(SessionStore store) {
        this.store = store;
    }

    public Dto.HintDto hint(String sessionId) {
        GameSession session = store.find(sessionId)
                .orElseThrow(() -> ApiException.notFound("对局不存在或已过期"));

        Object state;
        int seat;
        int version;
        session.lock().lock();
        try {
            if (session.finished()) {
                throw ApiException.conflict("对局已结束");
            }
            if (!session.isHumanSeat(session.currentSeat())) {
                throw ApiException.conflict("现在轮到 AI，稍等再看建议");
            }
            state = session.state();
            seat = session.currentSeat();
            version = session.version();
        } finally {
            session.lock().unlock();
        }

        SearchResult result = new MctsSearch(session.game(), new Random(session.seed() + version))
                .chooseWithStats(state, seat, SearchBudget.of(HINT_BUDGET_MS));
        if (result.action() == null) {
            throw ApiException.conflict("当前没有可以建议的动作");
        }

        session.lock().lock();
        try {
            if (session.version() != version) {
                throw ApiException.conflict("局面已经变化，请重试");
            }
        } finally {
            session.lock().unlock();
        }

        return new Dto.HintDto(result.action().key(), session.game().describe(state, result.action()));
    }
}
