package boardgame.engine.core;

/**
 * 一个离散、可序列化的游戏动作。key 必须在同一局面下唯一，前端只回传 key。
 */
public interface Action {

    String key();

    String label();
}
