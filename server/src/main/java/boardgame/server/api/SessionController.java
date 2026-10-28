package boardgame.server.api;

import boardgame.server.api.dto.Dto;
import boardgame.server.service.SessionService;
import boardgame.server.service.SseHub;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessions;
    private final SseHub hub;

    public SessionController(SessionService sessions, SseHub hub) {
        this.sessions = sessions;
        this.hub = hub;
    }

    @PostMapping
    public ResponseEntity<Dto.SessionDto> create(@RequestBody Dto.CreateSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessions.create(request));
    }

    @GetMapping("/{sessionId}")
    public Dto.SessionDto get(@PathVariable String sessionId) {
        return sessions.get(sessionId);
    }

    @PostMapping("/{sessionId}/actions")
    public ResponseEntity<Dto.SessionDto> act(@PathVariable String sessionId,
                                              @RequestBody Dto.ActionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(sessions.act(sessionId, request));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> delete(@PathVariable String sessionId) {
        sessions.delete(sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String sessionId) {
        sessions.get(sessionId);
        return hub.subscribe(sessionId);
    }
}
