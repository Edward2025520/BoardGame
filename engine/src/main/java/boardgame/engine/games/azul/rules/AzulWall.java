package boardgame.engine.games.azul.rules;

import boardgame.engine.games.azul.model.TileColor;

/** 墙面的固定花纹与计分规则。 */
public final class AzulWall {

    private static final TileColor[] ORDER = TileColor.values();

    private AzulWall() {
    }

    /** 第 row 行第 col 列要求的颜色。 */
    public static TileColor colorAt(int row, int col) {
        return ORDER[Math.floorMod(col - row, ORDER.length)];
    }

    /** 某一行的指定颜色落在哪一列。 */
    public static int columnOf(int row, TileColor color) {
        for (int col = 0; col < 5; col++) {
            if (colorAt(row, col) == color) {
                return col;
            }
        }
        throw new IllegalArgumentException("第 " + row + " 行没有颜色 " + color);
    }

    /**
     * 在 (row, col) 放下一块后得到的分数。
     * 横向连续与纵向连续都计数；形成十字时两段相加。
     */
    public static int scorePlacement(int wallMask, int row, int col) {
        int left = 0;
        for (int c = col - 1; c >= 0 && has(wallMask, row, c); c--) {
            left++;
        }
        int right = 0;
        for (int c = col + 1; c < 5 && has(wallMask, row, c); c++) {
            right++;
        }
        int up = 0;
        for (int r = row - 1; r >= 0 && has(wallMask, r, col); r--) {
            up++;
        }
        int down = 0;
        for (int r = row + 1; r < 5 && has(wallMask, r, col); r++) {
            down++;
        }

        int horizontal = left + right;
        int vertical = up + down;
        if (horizontal > 0 && vertical > 0) {
            return (1 + horizontal) + (1 + vertical);
        }
        return Math.max(1 + horizontal, 1 + vertical);
    }

    private static boolean has(int mask, int row, int col) {
        return (mask & (1 << (row * 5 + col))) != 0;
    }

    public static int withTile(int mask, int row, int col) {
        return mask | (1 << (row * 5 + col));
    }
}
