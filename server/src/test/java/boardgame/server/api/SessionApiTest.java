package boardgame.server.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SessionApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    @DisplayName("大厅能列出璀璨宝石")
    void listsGames() throws Exception {
        mvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("splendor"))
                .andExpect(jsonPath("$[0].minPlayers").value(2));
    }

    @Test
    @DisplayName("创建双人热座对局后由 0 号座位先手")
    void createSession() throws Exception {
        JsonNode dto = createSession(2, "[0,1]", "NORMAL");

        org.junit.jupiter.api.Assertions.assertEquals(0, dto.get("currentSeat").asInt());
        org.junit.jupiter.api.Assertions.assertTrue(dto.get("legalActions").size() > 5);
        org.junit.jupiter.api.Assertions.assertTrue(dto.get("boards").has("0"));
        org.junit.jupiter.api.Assertions.assertTrue(dto.get("boards").has("1"));
    }

    @Test
    @DisplayName("提交一个合法动作后版本号加一、轮到下家")
    void playAction() throws Exception {
        JsonNode created = createSession(2, "[0,1]", "NORMAL");
        String sessionId = created.get("sessionId").asText();
        String actionKey = created.get("legalActions").get(0).get("key").asText();

        JsonNode after = postAction(sessionId, actionKey, created.get("version").asInt(), 202);

        org.junit.jupiter.api.Assertions.assertEquals(created.get("version").asInt() + 1,
                after.get("version").asInt());
        org.junit.jupiter.api.Assertions.assertEquals(1, after.get("currentSeat").asInt());
        JsonNode log = after.get("log");
        org.junit.jupiter.api.Assertions.assertEquals(0, log.get(log.size() - 1).get("seat").asInt(),
                "日志应记给刚行动的座位 0");
    }

    @Test
    @DisplayName("版本号过期返回 409")
    void staleVersionConflicts() throws Exception {
        JsonNode created = createSession(2, "[0,1]", "NORMAL");
        String sessionId = created.get("sessionId").asText();
        String actionKey = created.get("legalActions").get(0).get("key").asText();
        int version = created.get("version").asInt();

        postAction(sessionId, actionKey, version, 202);
        postAction(sessionId, actionKey, version, 409);
    }

    @Test
    @DisplayName("非法动作 key 返回 400")
    void invalidActionKey() throws Exception {
        JsonNode created = createSession(2, "[0,1]", "NORMAL");
        postAction(created.get("sessionId").asText(), "take:NOT_A_GEM", 0, 400);
    }

    @Test
    @DisplayName("不存在的对局返回 404")
    void unknownSession() throws Exception {
        mvc.perform(get("/api/sessions/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("人机局里 AI 会自动落子")
    void aiPlaysItsTurn() throws Exception {
        JsonNode created = createSession(2, "[0]", "EASY");
        String sessionId = created.get("sessionId").asText();
        postAction(sessionId, created.get("legalActions").get(0).get("key").asText(),
                created.get("version").asInt(), 202);

        JsonNode latest = getSession(sessionId);
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline && latest.get("version").asInt() < 2) {
            Thread.sleep(100);
            latest = getSession(sessionId);
        }

        org.junit.jupiter.api.Assertions.assertTrue(latest.get("version").asInt() >= 2, "AI 没有行动");
        org.junit.jupiter.api.Assertions.assertEquals(0, latest.get("currentSeat").asInt());
    }

    @Test
    @DisplayName("四人局会连续跑完三个 AI 的回合")
    void fourPlayerSessionRunsAiChain() throws Exception {
        JsonNode created = createSession(4, "[0]", "EASY");
        String sessionId = created.get("sessionId").asText();
        org.junit.jupiter.api.Assertions.assertEquals(4, created.get("seats").size());

        postAction(sessionId, created.get("legalActions").get(0).get("key").asText(),
                created.get("version").asInt(), 202);

        JsonNode latest = getSession(sessionId);
        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline && latest.get("version").asInt() < 4) {
            Thread.sleep(100);
            latest = getSession(sessionId);
        }

        org.junit.jupiter.api.Assertions.assertTrue(latest.get("version").asInt() >= 4,
                "三个 AI 没有依次行动：" + latest.get("version").asInt());
        org.junit.jupiter.api.Assertions.assertEquals(0, latest.get("currentSeat").asInt());
    }

    @Test
    @DisplayName("对局中可以把难度改成七档里的任意一档")
    void changeDifficultyMidGame() throws Exception {
        JsonNode created = createSession(2, "[0,1]", "MEDIUM");
        String sessionId = created.get("sessionId").asText();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/sessions/" + sessionId + "/difficulty")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"difficulty\":\"HELL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty").value("HELL"))
                .andExpect(jsonPath("$.difficultyLabel").value("七档 · 地狱"));
    }

    @Test
    @DisplayName("悔棋回到人类回合，版本号继续递增")
    void undoRestoresHumanTurn() throws Exception {
        JsonNode created = createSession(2, "[0,1]", "MEDIUM");
        String sessionId = created.get("sessionId").asText();
        int before = created.get("version").asInt();

        postAction(sessionId, created.get("legalActions").get(0).get("key").asText(), before, 202);

        MvcResult result = mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/sessions/" + sessionId + "/undo"))
                .andExpect(status().isAccepted())
                .andReturn();
        JsonNode after = mapper.readTree(result.getResponse().getContentAsString());

        org.junit.jupiter.api.Assertions.assertEquals(0, after.get("currentSeat").asInt());
        org.junit.jupiter.api.Assertions.assertTrue(after.get("version").asInt() > before);
        org.junit.jupiter.api.Assertions.assertTrue(after.get("log").get(after.get("log").size() - 1)
                .get("text").asText().contains("悔棋"));
    }

    @Test
    @DisplayName("没有可悔的步骤时返回 409")
    void undoWithoutHistoryConflicts() throws Exception {
        JsonNode created = createSession(2, "[0,1]", "MEDIUM");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/sessions/" + created.get("sessionId").asText() + "/undo"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("轮到人类时可以要教练建议")
    void hintReturnsLegalAction() throws Exception {
        JsonNode created = createSession(2, "[0,1]", "MEDIUM");
        String sessionId = created.get("sessionId").asText();

        MvcResult result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/sessions/" + sessionId + "/hint"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode hint = mapper.readTree(result.getResponse().getContentAsString());

        java.util.List<String> keys = new java.util.ArrayList<>();
        created.get("legalActions").forEach(action -> keys.add(action.get("key").asText()));
        org.junit.jupiter.api.Assertions.assertTrue(keys.contains(hint.get("actionKey").asText()),
                "教练给的动作必须合法");
    }

    @Test
    @DisplayName("对局没结束时不能复盘")
    void reviewBeforeFinishConflicts() throws Exception {
        JsonNode created = createSession(2, "[0,1]", "MEDIUM");
        mvc.perform(get("/api/sessions/" + created.get("sessionId").asText() + "/review"))
                .andExpect(status().isConflict());
    }

    private JsonNode createSession(int seats, String humanSeats, String difficulty) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "gameId", "splendor",
                "seatCount", seats,
                "humanSeats", mapper.readTree(humanSeats),
                "difficulty", difficulty,
                "seed", 12345));
        MvcResult result = mvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode postAction(String sessionId, String actionKey, int expectedVersion,
                                int expectedStatus) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "actionKey", actionKey,
                "expectedVersion", expectedVersion));
        MvcResult result = mvc.perform(post("/api/sessions/" + sessionId + "/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        return content.isBlank() ? mapper.createObjectNode() : mapper.readTree(content);
    }

    private JsonNode getSession(String sessionId) throws Exception {
        MvcResult result = mvc.perform(get("/api/sessions/" + sessionId))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }
}
