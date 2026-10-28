package boardgame.server.config;

import boardgame.engine.store.InMemorySessionStore;
import boardgame.engine.store.SessionStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {

    @Bean
    public SessionStore sessionStore() {
        return new InMemorySessionStore();
    }
}
