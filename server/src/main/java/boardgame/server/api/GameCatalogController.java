package boardgame.server.api;

import boardgame.server.api.dto.Dto;
import boardgame.server.service.SessionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameCatalogController {

    private final SessionService sessions;

    public GameCatalogController(SessionService sessions) {
        this.sessions = sessions;
    }

    @GetMapping
    public List<Dto.GameSummary> list() {
        return sessions.games();
    }
}
