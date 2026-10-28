package boardgame.engine.store;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单进程内存存储。以后要换 Redis，只需要另一个实现。
 */
public final class InMemorySessionStore implements SessionStore {

    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(GameSession session) {
        sessions.put(session.id(), session);
    }

    @Override
    public Optional<GameSession> find(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    @Override
    public void remove(String id) {
        sessions.remove(id);
    }

    @Override
    public Collection<GameSession> all() {
        return List.copyOf(sessions.values());
    }

    @Override
    public int removeIdleBefore(Instant threshold) {
        List<String> expired = new ArrayList<>();
        for (GameSession session : sessions.values()) {
            if (session.updatedAt().isBefore(threshold)) {
                expired.add(session.id());
            }
        }
        expired.forEach(sessions::remove);
        return expired.size();
    }
}
