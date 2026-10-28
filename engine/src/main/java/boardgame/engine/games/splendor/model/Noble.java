package boardgame.engine.games.splendor.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * 贵族。requirement 是各色永久宝石的数量要求，长度 6（黄金恒为 0）。
 */
public record Noble(String code, int[] requirement) {

    public Noble {
        Objects.requireNonNull(code, "code");
        requirement = requirement.clone();
    }

    public int requirementOf(Gem gem) {
        return requirement[gem.ordinal()];
    }

    public int[] requirementCopy() {
        return requirement.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Noble noble && code.equals(noble.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return "Noble[" + code + ", requirement=" + Arrays.toString(requirement) + "]";
    }
}
