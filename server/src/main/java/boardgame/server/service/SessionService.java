package boardgame.server.service;

import boardgame.engine.ai.Difficulty;
import boardgame.engine.core.Action;
import boardgame.engine.core.GameAdapter;
import boardgame.engine.core.NewGameConfig;
import boardgame.engine.store.GameSession;
import boardgame.engine.store.SessionStore;
import boardgame.server.api.ApiException;
import boardgame.server.api.dto.Dto;
import boardgame.server.config.GameCatalog;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

/** 会话的创建、读取与动作提交。 */
@Service
public class SessionService {

    private final GameCatalog catalog;
    private final SessionStore store;
    private final SessionViewMapper mapper;
    private final SseHub hub;
    private final AiTurnService aiTurns;

    public SessionService(GameCatalog catalog, SessionStore store, SessionViewMapper mapper,
                          SseHub hub, AiTurnService aiTurns) {
        this.catalog = catalog;
        this.store = store;
        this.mapper = mapper;
        this.hub = hub;
        this.aiTurns = aiTurns;
    }

    public List<Dto.GameSummary> games() {
        return catalog.all().stream().map(mapper::toSummary).toList();
    }

    public Dto.SessionDto create(Dto.CreateSessionRequest request) {
        if (request == null || request.gameId() == null) {
            throw ApiException.badRequest("缺少 gameId");
        }
        GameAdapter game = catalog.find(request.gameId())
                .orElseThrow(() -> ApiException.notFound("没有这个游戏: " + request.gameId()));

        int seats = request.seatCount() == null ? 2 : request.seatCount();
        if (seats < game.minPlayers() || seats > game.maxPlayers()) {
            throw ApiException.badRequest(
                    game.displayName() + " 只支持 " + game.minPlayers() + "-" + game.maxPlayers() + " 人");
        }

        List<Integer> humans = normalizeHumans(request.humanSeats(), seats);
        Difficulty difficulty = Difficulty.parse(request.difficulty());
        long seed = request.seed() == null ? ThreadLocalRandom.current().nextLong() : request.seed();

        List<String> names = new ArrayList<>(seats);
        for (int seat = 0; seat < seats; seat++) {
            names.add(humans.contains(seat) ? "玩家 " + (seat + 1) : "AI " + (seat + 1));
        }

        Object state = game.newGame(new NewGameConfig(seats, seed, names));
        GameSession session = new GameSession(UUID.randomUUID().toString(), game, seats, humans,
                difficulty, seed, state);
        store.save(session);
        aiTurns.maybeStart(session);
        return snapshot(session);
    }

    public Dto.SessionDto get(String sessionId) {
        GameSession session = require(sessionId);
        return snapshot(session);
    }

    public Dto.SessionDto act(String sessionId, Dto.ActionRequest request) {
        GameSession session = require(sessionId);
        if (request == null || request.actionKey() == null || request.actionKey().isBlank()) {
            throw ApiException.badRequest("缺少 actionKey");
        }

        session.lock().lock();
        try {
            if (session.finished()) {
                throw ApiException.conflict("对局已结束");
            }
            if (!session.isHumanSeat(session.currentSeat())) {
                throw ApiException.conflict("现在轮到 AI，请稍等");
            }
            if (request.expectedVersion() != null && request.expectedVersion() != session.version()) {
                throw ApiException.conflict("局面已经变化，请刷新后重试");
            }
            Action action = session.legalActions().stream()
                    .filter(candidate -> candidate.key().equals(request.actionKey()))
                    .findFirst()
                    .orElseThrow(() -> ApiException.badRequest("非法动作: " + request.actionKey()));
            if (request.payment() != null) {
                action = session.game().withPayment(action, request.payment());
            }
            session.checkpoint(action);
            try {
                session.apply(action, session.describe(action));
            } catch (IllegalStateException e) {
                session.abortCheckpoint();
                throw ApiException.badRequest("支付方案不合法：" + e.getMessage());
            }
        } finally {
            session.lock().unlock();
        }

        Dto.SessionDto dto = snapshot(session);
        hub.broadcast(sessionId, "state", dto);
        aiTurns.maybeStart(session);
        return dto;
    }

    public Dto.SessionDto changeDifficulty(String sessionId, String raw) {
        GameSession session = require(sessionId);
        Difficulty difficulty = Difficulty.parse(raw);
        session.lock().lock();
        try {
            if (session.finished()) {
                throw ApiException.conflict("对局已结束");
            }
            session.setDifficulty(difficulty);
        } finally {
            session.lock().unlock();
        }
        Dto.SessionDto dto = snapshot(session);
        hub.broadcast(sessionId, "state", dto);
        return dto;
    }

    public Dto.SessionDto undo(String sessionId) {
        GameSession session = require(sessionId);
        if (aiTurns.isThinking(sessionId)) {
            throw ApiException.conflict("AI 正在思考，等它走完再悔棋");
        }
        session.lock().lock();
        try {
            if (session.finished()) {
                throw ApiException.conflict("对局已结束，不能悔棋");
            }
            if (!session.isHumanSeat(session.currentSeat())) {
                throw ApiException.conflict("现在不是你的回合");
            }
            if (!session.undo()) {
                throw ApiException.conflict("没有可以悔的步骤");
            }
        } finally {
            session.lock().unlock();
        }
        Dto.SessionDto dto = snapshot(session);
        hub.broadcast(sessionId, "state", dto);
        return dto;
    }

    public void delete(String sessionId) {
        store.remove(sessionId);
        hub.close(sessionId);
    }

    private Dto.SessionDto snapshot(GameSession session) {
        return mapper.toDto(session, aiTurns.isThinking(session.id()));
    }

    private GameSession require(String sessionId) {
        return store.find(sessionId)
                .orElseThrow(() -> ApiException.notFound("对局不存在或已过期"));
    }

    private List<Integer> normalizeHumans(List<Integer> requested, int seats) {
        List<Integer> humans = requested == null || requested.isEmpty()
                ? List.of(0)
                : new ArrayList<>(new LinkedHashSet<>(requested));
        if (humans.isEmpty()) {
            throw ApiException.badRequest("至少需要一个真人座位");
        }
        for (int seat : humans) {
            if (seat < 0 || seat >= seats) {
                throw ApiException.badRequest("真人座位超出范围: " + seat);
            }
        }
        humans.sort(Integer::compareTo);
        return List.copyOf(humans);
    }
}
