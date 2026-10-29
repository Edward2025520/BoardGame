package boardgame.server.api;

import boardgame.server.api.dto.Dto;
import boardgame.server.service.HintService;
import boardgame.server.service.ReviewService;
import boardgame.server.service.SessionService;
import boardgame.server.service.SseHub;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final HintService hints;
    private final ReviewService reviews;
    private final SseHub hub;

    public SessionController(SessionService sessions, HintService hints, ReviewService reviews,
                             SseHub hub) {
        this.sessions = sessions;
        this.hints = hints;
        this.reviews = reviews;
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

    @PatchMapping("/{sessionId}/difficulty")
    public Dto.SessionDto changeDifficulty(@PathVariable String sessionId,
                                           @RequestBody Dto.DifficultyRequest request) {
        return sessions.changeDifficulty(sessionId, request == null ? null : request.difficulty());
    }

    @PostMapping("/{sessionId}/hint")
    public Dto.HintDto hint(@PathVariable String sessionId) {
        return hints.hint(sessionId);
    }

    @PostMapping("/{sessionId}/undo")
    public ResponseEntity<Dto.SessionDto> undo(@PathVariable String sessionId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(sessions.undo(sessionId));
    }

    @GetMapping("/{sessionId}/review")
    public Dto.ReviewListDto review(@PathVariable String sessionId) {
        return reviews.list(sessionId);
    }

    @PostMapping("/{sessionId}/review/{index}")
    public Dto.ReviewMoveDto reviewMove(@PathVariable String sessionId, @PathVariable int index) {
        return reviews.analyze(sessionId, index);
    }

    @GetMapping(value = "/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String sessionId) {
        sessions.get(sessionId);
        return hub.subscribe(sessionId);
    }
}
