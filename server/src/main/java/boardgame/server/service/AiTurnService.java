package boardgame.server.service;

import boardgame.engine.ai.AiPlayer;
import boardgame.engine.core.Action;
import boardgame.engine.store.GameSession;
import boardgame.engine.store.SessionStore;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AI 回合的执行器。每个会话同一时刻最多一个任务，逐个把 AI 座位走完，
 * 每走一步广播一次状态，前端据此刷新。
 */
@Service
public class AiTurnService {

    private static final Logger log = LoggerFactory.getLogger(AiTurnService.class);

    private final SessionStore store;
    private final SessionViewMapper mapper;
    private final SseHub hub;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Set<String> running = ConcurrentHashMap.newKeySet();

    public AiTurnService(SessionStore store, SessionViewMapper mapper, SseHub hub) {
        this.store = store;
        this.mapper = mapper;
        this.hub = hub;
    }

    public boolean isThinking(String sessionId) {
        return running.contains(sessionId);
    }

    public void maybeStart(GameSession session) {
        if (session.finished() || session.isHumanSeat(session.currentSeat())) {
            return;
        }
        if (!running.add(session.id())) {
            return;
        }
        executor.submit(() -> playAiTurns(session.id()));
    }

    private void playAiTurns(String sessionId) {
        try {
            while (true) {
                GameSession session = store.find(sessionId).orElse(null);
                if (session == null || session.finished()) {
                    return;
                }
                int seat = session.currentSeat();
                if (session.isHumanSeat(seat)) {
                    return;
                }

                hub.broadcast(sessionId, "ai-thinking",
                        Map.of("seat", seat, "difficulty", session.difficulty().name()));

                Action action;
                try {
                    Random rng = new Random(session.seed() * 31 + seat * 17L + session.version() * 131L);
                    action = AiPlayer.choose(session.game(), session.state(), seat,
                            session.difficulty(), rng);
                } catch (RuntimeException error) {
                    log.warn("AI 计算失败 session={} seat={}", sessionId, seat, error);
                    hub.broadcast(sessionId, "error", Map.of("message", "AI 计算失败，请重开一局"));
                    return;
                }
                if (action == null) {
                    return;
                }

                session.lock().lock();
                try {
                    if (session.finished() || session.currentSeat() != seat) {
                        return;
                    }
                    session.apply(action, session.describe(action));
                } finally {
                    session.lock().unlock();
                }
                hub.broadcast(sessionId, "state", mapper.toDto(session, true));
            }
        } finally {
            running.remove(sessionId);
            store.find(sessionId).ifPresent(session ->
                    hub.broadcast(sessionId, "state", mapper.toDto(session, false)));
        }
    }
}
