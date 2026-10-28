package boardgame.engine.core;

import java.util.List;

/**
 * 开新局的入参。随机性只允许出现在 newGame 里，seed 相同则初始状态完全一致。
 */
public record NewGameConfig(int seatCount, long seed, List<String> seatNames) {

    public NewGameConfig {
        seatNames = List.copyOf(seatNames);
    }
}
