package boardgame.engine.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DifficultyTest {

    @Test
    @DisplayName("七档参数与山寨版一致")
    void sevenTiers() {
        assertEquals(7, Difficulty.values().length);
        assertEquals(0, Difficulty.BEGINNER.timeMs());
        assertTrue(Difficulty.BEGINNER.greedy());
        assertEquals(200, Difficulty.CASUAL.timeMs());
        assertEquals(0.40, Difficulty.CASUAL.blunderRate());
        assertEquals(500, Difficulty.ADVANCED.timeMs());
        assertEquals(0.18, Difficulty.ADVANCED.blunderRate());
        assertEquals(1_000, Difficulty.MEDIUM.timeMs());
        assertEquals(2_000, Difficulty.HARD.timeMs());
        assertEquals(4_000, Difficulty.MASTER.timeMs());
        assertEquals(8_000, Difficulty.HELL.timeMs());
    }

    @Test
    @DisplayName("旧的四档名字仍能解析")
    void legacyNames() {
        assertEquals(Difficulty.BEGINNER, Difficulty.parse("EASY"));
        assertEquals(Difficulty.MEDIUM, Difficulty.parse("NORMAL"));
        assertEquals(Difficulty.HARD, Difficulty.parse("HARD"));
        assertEquals(Difficulty.MASTER, Difficulty.parse("EXPERT"));
    }

    @Test
    @DisplayName("空值与未知值回落到中等")
    void fallback() {
        assertEquals(Difficulty.MEDIUM, Difficulty.parse(null));
        assertEquals(Difficulty.MEDIUM, Difficulty.parse("  "));
        assertEquals(Difficulty.MEDIUM, Difficulty.parse("不存在的档位"));
        assertEquals(Difficulty.HELL, Difficulty.parse("hell"));
    }
}
