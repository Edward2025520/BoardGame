package boardgame.server.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    @DisplayName("带支付方案的买卡能指定用黄金抵扣，非法方案返回 400 且不改局面")
    void paymentPlanOverridesGold() throws Exception {
        JsonNode created = createSession(2, "[0]", "BEGINNER");
        String sessionId = created.get("sessionId").asText();
        JsonNode dto = created;
        JsonNode buy = null;

        for (int i = 0; i < 40 && buy == null; i++) {
            dto = waitHumanTurn(sessionId);
            if (dto.get("legalActions").isEmpty()) {
                break;
            }
            for (JsonNode action : dto.get("legalActions")) {
                if (action.get("key").asText().startsWith("buy:")) {
                    buy = action;
                    break;
                }
            }
            if (buy != null) {
                break;
            }
            dto = postAction(sessionId, nextSetupAction(dto), dto.get("version").asInt(), 202);
        }

        assertNotNull(buy, "40 手以内应该买得起至少一张卡");
        String key = buy.get("key").asText();
        int version = dto.get("version").asInt();
        JsonNode seat = dto.get("boards").get("0").get("seats").get(0);
        int goldBefore = seat.get("tokens").get(5).asInt();

        postActionWithPayment(sessionId, key, version, new int[] {99, 0, 0, 0, 0}, 400);
        JsonNode unchanged = getSession(sessionId);
        assertEquals(version, unchanged.get("version").asInt(), "非法支付不能改动局面");
        assertEquals(goldBefore, unaffectedSeat(unchanged).get("tokens").get(5).asInt());

        int[] plan = paymentPlan(findCard(dto, key.substring("buy:".length())), seat);
        int goldUsed = plan[0] + plan[1] + plan[2] + plan[3] + plan[4];
        assertTrue(goldUsed > 0, "这手方案至少要用一个黄金");

        JsonNode after = postActionWithPayment(sessionId, key, version, plan, 202);
        assertEquals(version + 1, after.get("version").asInt());
        assertEquals(goldBefore - goldUsed,
                unaffectedSeat(after).get("tokens").get(5).asInt(), "用掉的黄金要扣掉");
        assertTrue(after.get("log").get(after.get("log").size() - 1).get("text").asText().contains("黄金"),
                "日志里应说明用了几颗黄金");
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

    private JsonNode waitHumanTurn(String sessionId) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        JsonNode dto = getSession(sessionId);
        while (System.currentTimeMillis() < deadline
                && (dto.get("currentSeat").asInt() != 0 || dto.get("legalActions").isEmpty())) {
            Thread.sleep(50);
            dto = getSession(sessionId);
        }
        return dto;
    }

    private String nextSetupAction(JsonNode dto) {
        String reserve = null;
        String take = null;
        boolean needGold = dto.get("boards").get("0").get("seats").get(0).get("tokens").get(5).asInt() == 0;
        for (JsonNode action : dto.get("legalActions")) {
            String key = action.get("key").asText();
            if (take == null && key.startsWith("take:")) {
                take = key;
            }
            if (reserve == null && key.startsWith("reserve")) {
                reserve = key;
            }
        }
        if (needGold && reserve != null) {
            return reserve;
        }
        return take != null ? take : dto.get("legalActions").get(0).get("key").asText();
    }

    private JsonNode unaffectedSeat(JsonNode dto) {
        return dto.get("boards").get("0").get("seats").get(0);
    }

    private JsonNode findCard(JsonNode dto, String code) {
        JsonNode rows = unaffectedSeat(dto).get("reserved");
        for (JsonNode card : rows) {
            if (card.get("code").asText().equals(code)) {
                return card;
            }
        }
        for (JsonNode row : dto.get("boards").get("0").get("rows")) {
            for (JsonNode card : row.get("cards")) {
                if (card.get("code").asText().equals(code)) {
                    return card;
                }
            }
        }
        throw new IllegalStateException("找不到卡面: " + code);
    }

    private int[] paymentPlan(JsonNode card, JsonNode seat) {
        String[] ids = {"white", "blue", "green", "red", "black"};
        int[] need = new int[5];
        int[] plan = new int[5];
        for (int i = 0; i < 5; i++) {
            int cost = card.get("cost").path(ids[i]).asInt(0);
            int bonus = seat.get("bonuses").get(i).asInt();
            int tokens = seat.get("tokens").get(i).asInt();
            need[i] = Math.max(0, cost - bonus);
            plan[i] = Math.max(0, need[i] - tokens);
        }
        int spare = seat.get("tokens").get(5).asInt() - (plan[0] + plan[1] + plan[2] + plan[3] + plan[4]);
        for (int i = 0; i < 5 && spare > 0; i++) {
            int add = Math.min(spare, need[i] - plan[i]);
            plan[i] += add;
            spare -= add;
        }
        return plan;
    }

    private JsonNode postActionWithPayment(String sessionId, String actionKey, int expectedVersion,
                                          int[] payment, int expectedStatus) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "actionKey", actionKey,
                "expectedVersion", expectedVersion,
                "payment", payment));
        MvcResult result = mvc.perform(post("/api/sessions/" + sessionId + "/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        return content.isBlank() ? mapper.createObjectNode() : mapper.readTree(content);
    }
}
