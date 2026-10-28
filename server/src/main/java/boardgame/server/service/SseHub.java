package boardgame.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 每个会话一组 SSE 订阅者，只做广播，不保存状态。 */
@Component
public class SseHub {

    private final ObjectMapper mapper;
    private final Map<String, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseHub(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public SseEmitter subscribe(String sessionId) {
        SseEmitter emitter = new SseEmitter(0L);
        subscribers.computeIfAbsent(sessionId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(sessionId, emitter));
        emitter.onTimeout(() -> remove(sessionId, emitter));
        emitter.onError(error -> remove(sessionId, emitter));
        send(emitter, "ready", "{}");
        return emitter;
    }

    public void broadcast(String sessionId, String event, Object payload) {
        List<SseEmitter> list = subscribers.get(sessionId);
        if (list == null || list.isEmpty()) {
            return;
        }
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return;
        }
        for (SseEmitter emitter : list) {
            if (!send(emitter, event, json)) {
                remove(sessionId, emitter);
            }
        }
    }

    public void close(String sessionId) {
        List<SseEmitter> list = subscribers.remove(sessionId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.complete();
            } catch (RuntimeException ignored) {
                // 客户端已断开
            }
        }
    }

    private boolean send(SseEmitter emitter, String event, String json) {
        try {
            emitter.send(SseEmitter.event().name(event).data(json));
            return true;
        } catch (IOException | IllegalStateException e) {
            try {
                emitter.complete();
            } catch (RuntimeException ignored) {
                // 已经结束
            }
            return false;
        }
    }

    private void remove(String sessionId, SseEmitter emitter) {
        List<SseEmitter> list = subscribers.get(sessionId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
