import { GEMS, type ActionOption, type Gem, type SplendorCard, type SplendorSeat } from '../../api/types';

/** 当前点选状态。gem 按点击顺序追加，同一颜色可以出现两次（拿两个同色）。 */
export interface Selection {
  gems: Gem[];
}

export const emptySelection: Selection = { gems: [] };

/** 后端动作 key 的排序规则：按 Gem 枚举顺序，不是字母序。 */
export function actionKeyForGems(prefix: 'take' | 'discard', gems: Gem[]): string {
  const ordered = [...gems].sort((left, right) => GEMS.indexOf(left) - GEMS.indexOf(right));
  return `${prefix}:${ordered.map((gem) => gem.toUpperCase()).join(',')}`;
}

export interface ToggleResult {
  selection: Selection;
  hint?: string;
}

export function toggleGem(
  selection: Selection,
  gem: Gem,
  context: { discarding: boolean; maxDiscard: number },
): ToggleResult {
  const gems = [...selection.gems];
  const count = gems.filter((picked) => picked === gem).length;

  if (context.discarding) {
    if (count > 0) {
      gems.splice(gems.indexOf(gem), 1);
      return { selection: { gems } };
    }
    if (gems.length >= context.maxDiscard) {
      return { selection, hint: `最多只能弃 ${context.maxDiscard} 个` };
    }
    gems.push(gem);
    return { selection: { gems } };
  }

  if (gem === 'gold') {
    return { selection, hint: '黄金只能通过预留获得' };
  }

  if (count === 2) {
    return { selection: { gems: gems.filter((picked) => picked !== gem) } };
  }
  if (count === 1) {
    if (gems.length > 1) {
      return { selection, hint: '两个同色之外不能再选别的颜色' };
    }
    gems.push(gem);
    return { selection: { gems } };
  }

  if (gems.length >= 3) {
    return { selection, hint: '最多选 3 个宝石' };
  }
  if (gems.length === 2 && gems[0] === gems[1]) {
    return { selection, hint: '拿两个同色时不能再拿别的颜色' };
  }
  gems.push(gem);
  return { selection: { gems } };
}

export function selectionAction(
  selection: Selection,
  context: { discarding: boolean },
  actions: ActionOption[],
): ActionOption | undefined {
  if (selection.gems.length === 0) {
    return undefined;
  }
  const key = actionKeyForGems(context.discarding ? 'discard' : 'take', selection.gems);
  return actions.find((action) => action.key === key);
}

export interface Shortfall {
  gem: Gem;
  missing: number;
}

/** 距离买下这张牌还差哪些宝石；黄金不计入差额展示。 */
export function cardShortfall(card: SplendorCard, seat: SplendorSeat): Shortfall[] {
  const result: Shortfall[] = [];
  GEMS.slice(0, 5).forEach((gem, index) => {
    const cost = card.cost[gem] ?? 0;
    const missing = cost - seat.bonuses[index] - seat.tokens[index];
    if (missing > 0) {
      result.push({ gem: gem as Gem, missing });
    }
  });
  return result;
}

/** 差额能否全部用黄金补上。 */
export function goldCovers(shortfall: Shortfall[], seat: SplendorSeat): boolean {
  const total = shortfall.reduce((sum, item) => sum + item.missing, 0);
  return total <= seat.tokens[5];
}

const LABEL: Record<string, string> = {
  white: '白',
  blue: '蓝',
  green: '绿',
  red: '红',
  black: '黑',
};

export function shortfallText(shortfall: Shortfall[]): string {
  if (shortfall.length === 0) {
    return '免费';
  }
  return `缺 ${shortfall.map((item) => `${item.missing}${LABEL[item.gem] ?? item.gem}`).join(' ')}`;
}

export function seatOf(view: { seats: SplendorSeat[] }, seat: number): SplendorSeat {
  return view.seats[seat];
}
