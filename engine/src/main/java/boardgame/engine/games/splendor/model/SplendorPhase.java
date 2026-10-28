package boardgame.engine.games.splendor.model;

public enum SplendorPhase {

    /** 正常行动：拿宝石、预留、购买。 */
    PLAY,
    /** 宝石超过 10 个，必须先弃回 10 个。 */
    DISCARD,
    /** 同时满足多个贵族，需要选一个。 */
    CHOOSE_NOBLE,
    /** 已终局。 */
    OVER
}
