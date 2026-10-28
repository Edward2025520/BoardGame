package boardgame.server.api.dto;

import java.util.List;

public final class Dto {

    private Dto() {
    }

    public record GameSummary(String id, String name, String description, int minPlayers, int maxPlayers) {
    }

    public record CreateSessionRequest(String gameId, Integer seatCount, List<Integer> humanSeats,
                                       String difficulty, Long seed) {
    }

    public record ActionRequest(String actionKey, Integer expectedVersion) {
    }

    public record ActionOption(String key, String label) {
    }

    public record SeatInfo(int seat, String kind, boolean current) {
    }

    public record LogItem(int version, int seat, String text, long at) {
    }

    public record SessionDto(
            String sessionId,
            String gameId,
            String gameName,
            int seatCount,
            int version,
            int currentSeat,
            String phase,
            boolean finished,
            String difficulty,
            List<Integer> humanSeats,
            List<SeatInfo> seats,
            boolean aiThinking,
            java.util.Map<Integer, Object> boards,
            List<ActionOption> legalActions,
            List<LogItem> log,
            OutcomeDto outcome) {
    }

    public record OutcomeDto(int[] points, List<Integer> winners) {
    }
}
