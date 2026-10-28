package boardgame.engine.store;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface SessionStore {

    void save(GameSession session);

    Optional<GameSession> find(String id);

    void remove(String id);

    Collection<GameSession> all();

    int removeIdleBefore(Instant threshold);
}
