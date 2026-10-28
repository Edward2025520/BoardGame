package boardgame.engine.games.azul.model;

/**
 * 一条图案行。颜色为 null 表示空行；capacity 固定等于行号 + 1。
 */
public record PatternLine(TileColor color, int count, int capacity) {

    public PatternLine {
        if (capacity < 1 || capacity > 5) {
            throw new IllegalArgumentException("图案行容量只能是 1-5");
        }
        if (count < 0 || count > capacity) {
            throw new IllegalArgumentException("图案行数量越界: " + count + "/" + capacity);
        }
        if (count == 0) {
            color = null;
        }
        if (count > 0 && color == null) {
            throw new IllegalArgumentException("有瓦片的行必须有颜色");
        }
    }

    public static PatternLine empty(int capacity) {
        return new PatternLine(null, 0, capacity);
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean isFull() {
        return count == capacity;
    }

    public int space() {
        return capacity - count;
    }

    /** 这一行能否放下指定颜色。 */
    public boolean accepts(TileColor candidate) {
        return count < capacity && (color == null || color == candidate);
    }

    public PatternLine add(TileColor candidate, int amount) {
        return new PatternLine(candidate, count + amount, capacity);
    }

    public PatternLine cleared() {
        return empty(capacity);
    }
}
