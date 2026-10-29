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
  gold: '金',
};

export function shortfallText(shortfall: Shortfall[]): string {
  if (shortfall.length === 0) {
    return '无需支付';
  }
  return `还缺 ${shortfall.map((item) => `${item.missing}${LABEL[item.gem] ?? item.gem}`).join(' ')}`;
}

export interface GemCount {
  gem: Gem;
  n: number;
}

export interface PaymentBreakdown {
  cost: GemCount[];
  discount: GemCount[];
  payable: GemCount[];
  goldUsed: number;
  shortfall: GemCount[];
  affordable: boolean;
}

/**
 * 成本 - 永久折扣 = 需要支付；宝石不够用黄金补，黄金也不够才是缺口。
 */
export function paymentBreakdown(card: SplendorCard, seat: SplendorSeat): PaymentBreakdown {
  const cost: GemCount[] = [];
  const discount: GemCount[] = [];
  const payable: GemCount[] = [];
  const remaining: { gem: Gem; n: number }[] = [];

  GEMS.slice(0, 5).forEach((gem, index) => {
    const need = card.cost[gem] ?? 0;
    if (need <= 0) {
      return;
    }
    cost.push({ gem: gem as Gem, n: need });

    const bonus = seat.bonuses[index];
    const applied = Math.min(bonus, need);
    if (applied > 0) {
      discount.push({ gem: gem as Gem, n: applied });
    }

    const left = need - bonus;
    const paid = Math.min(Math.max(left, 0), seat.tokens[index]);
    if (paid > 0) {
      payable.push({ gem: gem as Gem, n: paid });
    }
    if (left - paid > 0) {
      remaining.push({ gem: gem as Gem, n: left - paid });
    }
  });

  let goldLeft = seat.tokens[5];
  const shortfall: GemCount[] = [];
  for (const item of remaining) {
    const cover = Math.min(goldLeft, item.n);
    goldLeft -= cover;
    if (cover > 0) {
      payable.push({ gem: 'gold' as Gem, n: cover });
    }
    if (item.n - cover > 0) {
      shortfall.push({ gem: item.gem, n: item.n - cover });
    }
  }

  const goldUsed = seat.tokens[5] - goldLeft;
  return {
    cost,
    discount,
    payable,
    goldUsed,
    shortfall,
    affordable: shortfall.length === 0,
  };
}

/** 供缩略图 title 用的一句话摘要。 */
export function paymentSummary(card: SplendorCard, seat: SplendorSeat): string {
  const breakdown = paymentBreakdown(card, seat);
  if (!breakdown.affordable) {
    return shortfallText(breakdown.shortfall.map((item) => ({ gem: item.gem, missing: item.n })));
  }
  if (breakdown.payable.length === 0) {
    return '实付 0（折扣全抵）';
  }
  const paid = breakdown.payable
    .map((item) => `${item.n}${LABEL[item.gem] ?? item.gem}`)
    .join(' ');
  return `实付 ${paid}`;
}

export function seatOf(view: { seats: SplendorSeat[] }, seat: number): SplendorSeat {
  return view.seats[seat];
}
