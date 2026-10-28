package boardgame.server.service;

import boardgame.server.api.dto.Dto;
import boardgame.engine.core.Action;
import boardgame.engine.core.GameOutcome;
import boardgame.engine.store.GameSession;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** 把内部会话状态翻译成前端 DTO。 */
@Component
public class SessionViewMapper {

    public Dto.SessionDto toDto(GameSession session, boolean aiThinking) {
        List<Dto.SeatInfo> seats = new ArrayList<>(session.seatCount());
        for (int seat = 0; seat < session.seatCount(); seat++) {
            String kind = session.isHumanSeat(seat) ? "HUMAN" : "AI";
            seats.add(new Dto.SeatInfo(seat, kind, seat == session.currentSeat()));
        }

        List<Dto.ActionOption> actions = new ArrayList<>();
        if (!session.finished() && session.isHumanSeat(session.currentSeat())) {
            for (Action action : session.legalActions()) {
                actions.add(new Dto.ActionOption(action.key(), action.label()));
            }
        }

        List<Dto.LogItem> log = session.log().stream()
                .map(entry -> new Dto.LogItem(entry.version(), entry.seat(), entry.text(), entry.at()))
                .toList();

        GameOutcome outcome = session.outcome();
        Dto.OutcomeDto outcomeDto = outcome.finished()
                ? new Dto.OutcomeDto(outcome.points(), outcome.winners())
                : null;

        return new Dto.SessionDto(
                session.id(),
                session.game().id(),
                session.game().displayName(),
                session.seatCount(),
                session.version(),
                session.currentSeat(),
                session.phase(),
                session.finished(),
                session.difficulty().name(),
                session.humanSeats(),
                seats,
                aiThinking,
                session.views(),
                actions,
                log,
                outcomeDto);
    }

    public Dto.GameSummary toSummary(boardgame.engine.core.GameAdapter game) {
        return new Dto.GameSummary(game.id(), game.displayName(), game.description(),
                game.minPlayers(), game.maxPlayers());
    }
}
