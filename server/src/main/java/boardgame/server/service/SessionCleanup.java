package boardgame.server.service;

import boardgame.engine.store.GameSession;
import boardgame.engine.store.SessionStore;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 回收长时间没人动的对局，避免内存里的会话只增不减。 */
@Component
public class SessionCleanup {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanup.class);

    private final SessionStore store;
    private final SseHub hub;
    private final long idleMinutes;

    public SessionCleanup(SessionStore store, SseHub hub,
                          @Value("${boardgame.session.idle-timeout-minutes:30}") long idleMinutes) {
        this.store = store;
        this.hub = hub;
        this.idleMinutes = idleMinutes;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void sweep() {
        Instant threshold = Instant.now().minusSeconds(idleMinutes * 60);
        int removed = 0;
        for (GameSession session : List.copyOf(store.all())) {
            if (session.updatedAt().isBefore(threshold)) {
                store.remove(session.id());
                hub.close(session.id());
                removed++;
            }
        }
        if (removed > 0) {
            log.info("回收了 {} 个闲置对局", removed);
        }
    }
}
