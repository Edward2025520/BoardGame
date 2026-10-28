package boardgame.server.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
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
class AzulApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    @DisplayName("大厅同时列出璀璨宝石与花砖物语")
    void listsBothGames() throws Exception {
        mvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("splendor"))
                .andExpect(jsonPath("$[1].id").value("azul"))
                .andExpect(jsonPath("$[1].name").value("花砖物语"));
    }

    @Test
    @DisplayName("三人花砖局：工厂数 7，AI 会连续行动")
    void threePlayerAzulRunsAiChain() throws Exception {
        JsonNode created = createSession(3);
        String sessionId = created.get("sessionId").asText();
        Assertions.assertEquals(3, created.get("seats").size());
        Assertions.assertEquals(7, created.get("boards").get("0").get("factories").size());

        String key = created.get("legalActions").get(0).get("key").asText();
        MvcResult result = mvc.perform(post("/api/sessions/" + sessionId + "/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("actionKey", key,
                                "expectedVersion", created.get("version").asInt()))))
                .andExpect(status().isAccepted())
                .andReturn();
        JsonNode after = mapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(1, after.get("version").asInt());

        JsonNode latest = after;
        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline && latest.get("version").asInt() < 3) {
            Thread.sleep(100);
            latest = getSession(sessionId);
        }
        Assertions.assertTrue(latest.get("version").asInt() >= 3, "两个 AI 没有依次行动");
        Assertions.assertEquals(0, latest.get("currentSeat").asInt());
    }

    private JsonNode createSession(int seats) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "gameId", "azul",
                "seatCount", seats,
                "humanSeats", mapper.readTree("[0]"),
                "difficulty", "EASY",
                "seed", 2026));
        MvcResult result = mvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode getSession(String sessionId) throws Exception {
        MvcResult result = mvc.perform(get("/api/sessions/" + sessionId))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }
}
