import { useState } from 'react';
import {
  GEMS,
  GEM_LABEL,
  type Gem,
  type LogItem,
  type SplendorCard,
  type SplendorNoble,
  type SplendorRow,
  type SplendorSeat,
} from '../../api/types';
import {
  autoPayment,
  paymentBreakdown,
  paymentOptions,
  paymentSummary,
  type GemCount,
} from './selection';
import { CardArt, DiscountIcon, GemIcon, NobleArt } from './art';

export function Avatar({ label, mine }: { label: string; mine?: boolean }) {
  return <span className={mine ? 'avatar mine' : 'avatar'}>{label.slice(0, 1)}</span>;
}

export function OpponentStrip({
  seat,
  label,
  showReserved,
}: {
  seat: SplendorSeat;
  label: string;
  showReserved: boolean;
}) {
  return (
    <div className={seat.current ? 'opponent current' : 'opponent'}>
      <div className="opponent-head">
        <Avatar label={label} />
        <span className="opponent-name">{label}</span>
        <strong>{seat.points} 分</strong>
        <span className="opponent-meta">
          {seat.cards} 卡 · 留 {seat.reservedCount}/3
        </span>
      </div>
      <PlayerStats seat={seat} />
      {showReserved && seat.reserved.length > 0 && (
        <div className="opponent-reserved">
          {seat.reserved.map((card) => (
            <div
              key={card.code}
              className="reserved-mini"
              title={`预留：${card.tier} 级 ${card.bonus} 卡，${card.points} 分`}
            >
              <CardArt card={card} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export function LogPanel({ log, labelFor }: { log: LogItem[]; labelFor: (seat: number) => string }) {
  const recent = [...log].slice(-14).reverse();
  return (
    <div className="log-panel">
      <div className="log-head">对局记录</div>
      <div className="log-body">
        {recent.length === 0 && <span className="hint">还没有动作</span>}
        {recent.map((item) => (
          <div key={`${item.version}-${item.at}`} className="log-line">
            <span className="log-seat">{labelFor(item.seat)}</span>
            {item.text}
          </div>
        ))}
      </div>
    </div>
  );
}

export function NoblesRow({
  nobles,
  choosable,
  onChoose,
}: {
  nobles: SplendorNoble[];
  choosable: boolean;
  onChoose: (code: string) => void;
}) {
  return (
    <div className="nobles">
      {nobles.map((noble) => (
        <button
          key={noble.code}
          type="button"
          className={[
            'noble',
            noble.qualified ? 'qualified' : '',
            choosable ? 'choosable' : '',
          ].join(' ')}
          disabled={!choosable}
          onClick={() => onChoose(noble.code)}
          title={choosable ? '选择这位贵族' : noble.code}
        >
          <NobleArt noble={noble} />
        </button>
      ))}
      {nobles.length === 0 && <span className="hint">贵族已被拿完</span>}
    </div>
  );
}

function CostPips({ items }: { items: GemCount[] }) {
  return (
    <span className="cost-pips">
      {items.map((item) => (
        <span
          key={item.gem}
          className="cost-pip"
          title={`${GEM_LABEL[item.gem as Gem] ?? item.gem} ${item.n}`}
        >
          <GemIcon gem={item.gem} size={13} />
          <b>{item.n}</b>
        </span>
      ))}
    </span>
  );
}

/** 可买时三行：成本 / 折扣 / 实付；买不起只显示缺口。 */
export function CardCostLines({ card, seat }: { card: SplendorCard; seat: SplendorSeat }) {
  const breakdown = paymentBreakdown(card, seat);

  if (!breakdown.affordable) {
    return (
      <div className="cost-lines">
        <div className="cost-line shortfall">
          <span className="cost-label">缺口</span>
          <CostPips items={breakdown.shortfall} />
        </div>
      </div>
    );
  }

  return (
    <div className="cost-lines">
      <div className="cost-line">
        <span className="cost-label">成本</span>
        <CostPips items={breakdown.cost} />
      </div>
      <div className="cost-line discount">
        <span className="cost-label">折扣</span>
        {breakdown.discount.length > 0 ? (
          <>
            <span className="cost-minus">−</span>
            <CostPips items={breakdown.discount} />
          </>
        ) : (
          <span className="cost-none">无</span>
        )}
      </div>
      <div className="cost-line payable">
        <span className="cost-label">实付</span>
        {breakdown.payable.length > 0 ? (
          <CostPips items={breakdown.payable} />
        ) : (
          <span className="cost-none">0</span>
        )}
        {breakdown.payable.length === 0 && <span className="cost-note">折扣全抵</span>}
      </div>
    </div>
  );
}

export function CardTile({
  card,
  seat,
  keys,
  disabled,
  hinted,
  onBuy,
  onReserve,
}: {
  card: SplendorCard;
  seat: SplendorSeat;
  keys: Set<string>;
  disabled: boolean;
  hinted?: boolean;
  onBuy: (card: SplendorCard, source: HTMLElement) => void;
  onReserve: (card: SplendorCard) => void;
}) {
  const canBuy = keys.has(`buy:${card.code}`);
  const canReserve = keys.has(`reserve:${card.code}`);
  const className = ['card', canBuy ? 'affordable' : '', hinted ? 'hinted' : '']
    .filter(Boolean)
    .join(' ');

  return (
    <div className={className}>
      <div className="card-art">
        <CardArt card={card} />
      </div>
      <div className="card-actions">
        <button
          type="button"
          className="buy"
          disabled={disabled || !canBuy}
          onClick={(event) => onBuy(card, event.currentTarget)}
        >
          购买
        </button>
        <button
          type="button"
          className="reserve"
          disabled={disabled || !canReserve}
          onClick={() => onReserve(card)}
        >
          预留
        </button>
      </div>
      <div className={canBuy ? 'card-status ok' : 'card-status'}>
        <CardCostLines card={card} seat={seat} />
      </div>
    </div>
  );
}

export function CardRows({
  rows,
  seat,
  keys,
  disabled,
  hintedCodes,
  hintedDeckTier,
  onBuy,
  onReserve,
  onDeckReserve,
}: {
  rows: SplendorRow[];
  seat: SplendorSeat;
  keys: Set<string>;
  disabled: boolean;
  hintedCodes?: Set<string>;
  hintedDeckTier?: number | null;
  onBuy: (card: SplendorCard, source: HTMLElement) => void;
  onReserve: (card: SplendorCard) => void;
  onDeckReserve: (tier: number) => void;
}) {
  return (
    <div className="rows">
      {rows.map((row) => (
        <div className="row" key={row.tier}>
          <button
            type="button"
            className={hintedDeckTier === row.tier ? 'deck hinted' : 'deck'}
            disabled={disabled || !keys.has(`reserve:deck:${row.tier}`) || row.deckSize === 0}
            onClick={() => onDeckReserve(row.tier)}
            title="盲抽预留，拿一个黄金"
          >
            <span>{row.deckSize}</span>
            <small>{row.tier} 级</small>
            <small className="deck-action">盲抽</small>
          </button>
          <div className="row-cards">
            {row.cards.map((card) => (
              <CardTile
                key={card.code}
                card={card}
                seat={seat}
                keys={keys}
                disabled={disabled}
                hinted={hintedCodes?.has(card.code)}
                onBuy={onBuy}
                onReserve={onReserve}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

export function GemBank({
  bank,
  goldBank,
  selected,
  selectable,
  hintedGems,
  onSelect,
}: {
  bank: number[];
  goldBank: number;
  selected: Gem[];
  selectable: boolean;
  hintedGems?: Set<string>;
  onSelect: (gem: Gem) => void;
}) {
  return (
    <div className="gem-bank">
      {GEMS.slice(0, 5).map((gem, index) => {
        const count = bank[index];
        const picked = selected.filter((pickedGem) => pickedGem === gem).length;
        const hinted = hintedGems?.has(gem) ?? false;
        return (
          <button
            key={gem}
            type="button"
            className={['gem-button', picked > 0 ? 'picked' : '', hinted ? 'hinted' : '']
              .filter(Boolean)
              .join(' ')}
            disabled={!selectable || count === 0}
            onClick={() => onSelect(gem as Gem)}
            title={`${GEM_LABEL[gem as Gem]}宝石，银行剩 ${count}`}
          >
            <GemIcon gem={gem} size={34} />
            <span className="gem-count">{count}</span>
            {picked > 0 && <span className="gem-picked">{picked}</span>}
          </button>
        );
      })}
      <div className="gem-button static" title={`黄金，银行剩 ${goldBank}`}>
        <GemIcon gem="gold" size={34} />
        <span className="gem-count">{goldBank}</span>
      </div>
    </div>
  );
}

export function MyPanel({
  seat,
  label,
  discarding,
  selectedGems,
  keys,
  disabled,
  hintKey,
  onDiscardClick,
  onBuy,
}: {
  seat: SplendorSeat;
  label: string;
  discarding: boolean;
  selectedGems: Gem[];
  keys: Set<string>;
  disabled: boolean;
  hintKey?: string | null;
  onDiscardClick: (gem: Gem) => void;
  onBuy: (card: SplendorCard, source: HTMLElement) => void;
}) {
  const [showPurchased, setShowPurchased] = useState(false);

  return (
    <div className="my-panel">
      <div className="my-head">
        <Avatar label={label} mine />
        <span className="my-name">{label}</span>
        <span className="my-points">{seat.points} 分</span>
        <span className="my-cards">{seat.cards} 张卡</span>
      </div>
      <PlayerStats
        seat={seat}
        discarding={discarding}
        selectedGems={selectedGems}
        onGemClick={onDiscardClick}
      />
      <div className="my-actions">
        <button
          type="button"
          className="ghost tiny"
          onClick={() => setShowPurchased((value) => !value)}
        >
          已购 {seat.purchased.length} 张 {showPurchased ? '▴' : '▾'}
        </button>
      </div>
      {showPurchased && (
        <div className="purchased-grid">
          {seat.purchased.map((card, index) => (
            <div
              key={`${card.code}-${index}`}
              className="purchased-card"
              title={`${card.tier} 级 ${GEM_LABEL[card.bonus as Gem] ?? card.bonus}卡，提供 +1 折扣`}
            >
              <CardArt card={card} />
            </div>
          ))}
          {seat.purchased.length === 0 && <span className="hint">还没有买卡</span>}
        </div>
      )}
      {seat.reserved.length > 0 && (
        <div className="my-reserved">
          {seat.reserved.map((card) => {
            const canBuy = keys.has(`buy:${card.code}`);
            const hinted = hintKey === `buy:${card.code}`;
            const summary = paymentSummary(card, seat);
            return (
              <div
                key={card.code}
                className={hinted ? 'reserved-card hinted' : 'reserved-card'}
                title={`预留：${card.tier} 级 ${GEM_LABEL[card.bonus as Gem] ?? card.bonus}卡，${summary}`}
              >
                <CardArt card={card} />
                <button
                  type="button"
                  className="buy-reserved"
                  disabled={disabled || !canBuy}
                  onClick={(event) => onBuy(card, event.currentTarget)}
                >
                  购买
                </button>
                <span className={canBuy ? 'reserved-status ok' : 'reserved-status'}>{summary}</span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

/** 你与 AI 共用的一行宝石、一行永久折扣。 */
export function PlayerStats({
  seat,
  discarding = false,
  selectedGems,
  onGemClick,
}: {
  seat: SplendorSeat;
  discarding?: boolean;
  selectedGems?: Gem[];
  onGemClick?: (gem: Gem) => void;
}) {
  const interactive = Boolean(onGemClick);
  const purchased = seat.purchased ?? [];

  const discountTitle = (gem: Gem, index: number) => {
    const cards = purchased.filter((card) => card.bonus === gem);
    const tiers = [1, 2, 3]
      .map((tier) => {
        const count = cards.filter((card) => card.tier === tier).length;
        return count > 0 ? `${tier} 级 ${count}` : '';
      })
      .filter(Boolean)
      .join('、');
    const points = cards.reduce((sum, card) => sum + card.points, 0);
    const detail = cards.length > 0 ? `，已购 ${cards.length} 张（${tiers}，共 ${points} 分）` : '，还没买过';
    return `${GEM_LABEL[gem]}折扣 ${seat.bonuses[index]}${detail}`;
  };

  return (
    <div className="player-stats">
      <div className="stats-row">
        <span className="stats-label">宝石</span>
        {GEMS.map((gem, index) => {
          const count = seat.tokens[index];
          const picked = selectedGems?.filter((pickedGem) => pickedGem === gem).length ?? 0;
          const className = ['stat-icon', count === 0 ? 'is-empty' : '', picked > 0 ? 'picked' : '']
            .filter(Boolean)
            .join(' ');
          const title = discarding ? `点击弃掉${GEM_LABEL[gem as Gem]}` : `${GEM_LABEL[gem as Gem]}宝石 ${count}`;
          const body = (
            <>
              <GemIcon gem={gem} size={22} dimmed={count === 0} />
              <span className="icon-badge">{count}</span>
            </>
          );
          return interactive ? (
            <button
              key={gem}
              type="button"
              className={className}
              disabled={!discarding || count === 0}
              onClick={() => onGemClick?.(gem as Gem)}
              title={title}
            >
              {body}
            </button>
          ) : (
            <span key={gem} className={className} title={title}>
              {body}
            </span>
          );
        })}
      </div>
      <div className="stats-row">
        <span className="stats-label">折扣</span>
        {GEMS.slice(0, 5).map((gem, index) => (
          <span
            key={`bonus-${gem}`}
            className={seat.bonuses[index] === 0 ? 'stat-icon discount is-empty' : 'stat-icon discount'}
            title={discountTitle(gem as Gem, index)}
          >
            <DiscountIcon gem={gem} size={22} />
            <span className="icon-badge">{seat.bonuses[index]}</span>
          </span>
        ))}
      </div>
    </div>
  );
}

/** 买卡前的手动支付面板：把某几种颜色改成用黄金付。 */
export function PaymentDialog({
  card,
  seat,
  onConfirm,
  onCancel,
}: {
  card: SplendorCard;
  seat: SplendorSeat;
  onConfirm: (payment: number[]) => void;
  onCancel: () => void;
}) {
  const options = paymentOptions(card, seat);
  const [plan, setPlan] = useState<number[]>(() => autoPayment(card, seat));
  const gold = seat.tokens[5];
  const goldUsed = plan.reduce((sum, count) => sum + count, 0);

  const adjust = (index: number, delta: number) => {
    const option = options.find((item) => item.index === index);
    if (!option) {
      return;
    }
    setPlan((previous) => {
      const value = previous[index] + delta;
      if (value < option.minGold || value > option.maxGold) {
        return previous;
      }
      const total = previous.reduce((sum, count) => sum + count, 0);
      if (delta > 0 && total + 1 > gold) {
        return previous;
      }
      const next = [...previous];
      next[index] = value;
      return next;
    });
  };

  return (
    <div className="payment-mask">
      <div className="payment-dialog" role="dialog" aria-modal="true">
        <div className="payment-head">
          <span>
            支付 {card.tier} 级 {GEM_LABEL[card.bonus as Gem] ?? card.bonus} 卡
          </span>
          <button type="button" className="ghost tiny" onClick={onCancel}>
            取消
          </button>
        </div>
        <div className="payment-body">
          <div className="payment-art">
            <CardArt card={card} />
          </div>
          <div className="payment-lines">
            {options.map((option) => (
              <div className="payment-line" key={option.gem}>
                <span className="payment-gem">
                  <GemIcon gem={option.gem} size={14} />
                  {GEM_LABEL[option.gem]}
                </span>
                <span className="payment-need">需要 {option.need}</span>
                <span className="payment-tokens">宝石 {option.tokens}</span>
                <span className="payment-gold">
                  <span className="payment-gold-label">黄金</span>
                  <button
                    type="button"
                    className="stepper"
                    disabled={plan[option.index] <= option.minGold}
                    onClick={() => adjust(option.index, -1)}
                  >
                    −
                  </button>
                  <b>{plan[option.index]}</b>
                  <button
                    type="button"
                    className="stepper"
                    disabled={plan[option.index] >= option.maxGold || goldUsed >= gold}
                    onClick={() => adjust(option.index, 1)}
                  >
                    +
                  </button>
                </span>
              </div>
            ))}
          </div>
        </div>
        <div className="payment-foot">
          <span className="payment-total">
            黄金 {goldUsed} / {gold}
          </span>
          <button type="button" className="ghost" onClick={() => setPlan(autoPayment(card, seat))}>
            自动
          </button>
          <button type="button" className="primary" onClick={() => onConfirm(plan)}>
            确认支付
          </button>
        </div>
      </div>
    </div>
  );
}
