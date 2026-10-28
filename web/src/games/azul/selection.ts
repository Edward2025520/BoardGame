import { AZUL_COLORS, type ActionOption, type AzulColor, type AzulLine } from '../../api/types';

export interface TileGroup {
  color: AzulColor;
  count: number;
}

/** 把一堆瓦片按颜色分组，顺序固定，界面不会被洗牌打乱。 */
export function groupTiles(tiles: string[]): TileGroup[] {
  const counts = new Map<string, number>();
  for (const tile of tiles) {
    counts.set(tile, (counts.get(tile) ?? 0) + 1);
  }
  return AZUL_COLORS.filter((color) => counts.has(color)).map((color) => ({
    color,
    count: counts.get(color) ?? 0,
  }));
}

export function takeKey(source: number, color: string, targetLine: number): string {
  return `take:${source}:${color.toUpperCase()}:${targetLine}`;
}

/** 某个工厂/中央的某个颜色，后端允许放到哪些行（含 -1 地板）。 */
export function availableTargets(actions: ActionOption[], source: number, color: string): number[] {
  const prefix = `take:${source}:${color.toUpperCase()}:`;
  return actions
    .filter((action) => action.key.startsWith(prefix))
    .map((action) => Number(action.key.slice(prefix.length)))
    .filter((line) => Number.isInteger(line));
}

/** 这一行能放几块、溢出几块，用于幽灵预演。 */
export function ghostPlacement(
  line: AzulLine,
  color: string,
  count: number,
): { placed: number; overflow: number; lineColor: string } {
  const compatible = line.color === null || line.color === color;
  const space = compatible ? line.capacity - line.count : 0;
  const placed = Math.min(Math.max(space, 0), count);
  return { placed, overflow: count - placed, lineColor: line.color ?? color };
}
