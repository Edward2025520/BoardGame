package boardgame.server.config;

import boardgame.engine.core.GameAdapter;
import boardgame.engine.core.GameDefinition;
import boardgame.engine.games.splendor.rules.SplendorDefinition;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 注册所有可用游戏。加新游戏只改这里。 */
@Component
public class GameCatalog {

    private final Map<String, GameAdapter> games = new LinkedHashMap<>();

    public GameCatalog() {
        register(new SplendorDefinition());
    }

    private void register(GameDefinition<?, ?> definition) {
        GameAdapter adapter = GameAdapter.of(definition);
        games.put(adapter.id(), adapter);
    }

    public Optional<GameAdapter> find(String gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    public Collection<GameAdapter> all() {
        return games.values();
    }
}
