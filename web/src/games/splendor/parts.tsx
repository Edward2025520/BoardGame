import { useState } from 'react';
import {
  GEMS,
  GEM_COLOR,
  GEM_LABEL,
  type Gem,
  type LogItem,
  type SplendorCard,
  type SplendorNoble,
  type SplendorRow,
  type SplendorSeat,
} from '../../api/types';
import { paymentBreakdown, paymentSummary, type GemCount } from './selection';
import { CardArt, GemCircle, NobleArt } from './art';

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
      </div>
      <div className="opponent-gems">
        {GEMS.slice(0, 5).map((gem, gemIndex) => (
          <span key={gem} className="pip" style={{ background: GEM_COLOR[gem as Gem] }}>
            {seat.tokens[gemIndex] > 0 ? seat.tokens[gemIndex] : ''}
          </span>
        ))}
        <span className="pip gold">{seat.tokens[5] > 0 ? seat.tokens[5] : ''}</span>
        {seat.bonuses.map((count, gemIndex) =>
          count > 0 ? (
            <span key={`bonus-${gemIndex}`} className="bonus-chip">
              {GEM_LABEL[GEMS[gemIndex] as Gem]}
              {count}
            </span>
          ) : null,
        )}
        <span className="bonus-chip">留 {seat.reservedCount}/3</span>
        <span className="bonus-chip">{seat.cards} 卡</span>
      </div>
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
          style={{
            background: GEM_COLOR[item.gem as Gem] ?? '#666',
            color: item.gem === 'white' || item.gem === 'gold' ? '#2a2415' : '#fff',
          }}
        >
          {item.n}
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
            <svg viewBox="0 0 40 40" className="gem-icon">
              <GemCircle gem={gem} x={20} y={20} r={17} uid={`bank-${gem}`} />
            </svg>
            <span className="gem-count">{count}</span>
            {picked > 0 && <span className="gem-picked">{picked}</span>}
          </button>
        );
      })}
      <div className="gem-button static" title={`黄金，银行剩 ${goldBank}`}>
        <svg viewBox="0 0 40 40" className="gem-icon">
          <GemCircle gem="gold" x={20} y={20} r={17} uid="bank-gold" />
        </svg>
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
  const bonusPips: GemCount[] = seat.bonuses
    .map((count, index) => ({ gem: GEMS[index] as Gem, n: count }))
    .filter((item) => item.n > 0);

  return (
    <div className="my-panel">
      <div className="my-head">
        <Avatar label={label} mine />
        <span className="my-name">{label}</span>
        <span className="my-points">{seat.points} 分</span>
        <span className="my-cards">{seat.cards} 张卡</span>
      </div>
      <div className="my-discount">
        <span className="cost-label">永久折扣</span>
        {bonusPips.length > 0 ? <CostPips items={bonusPips} /> : <span className="cost-none">暂无</span>}
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
      <div className="my-gems">
        {GEMS.slice(0, 5).map((gem, index) => {
          const picked = selectedGems.filter((pickedGem) => pickedGem === gem).length;
          return (
            <button
              key={gem}
              type="button"
              className={picked > 0 ? 'gem-token picked' : 'gem-token'}
              disabled={!discarding || seat.tokens[index] === 0}
              onClick={() => onDiscardClick(gem as Gem)}
              title={discarding ? '点击弃掉这个宝石' : `${GEM_LABEL[gem as Gem]}宝石`}
            >
              <span className="dot" style={{ background: GEM_COLOR[gem as Gem] }} />
              {seat.tokens[index]}
            </button>
          );
        })}
        <button
          type="button"
          className={selectedGems.includes('gold') ? 'gem-token gold picked' : 'gem-token gold'}
          disabled={!discarding || seat.tokens[5] === 0}
          onClick={() => onDiscardClick('gold')}
          title={discarding ? '点击弃掉黄金' : '黄金'}
        >
          <span className="dot gold" />
          {seat.tokens[5]}
        </button>
      </div>
      <div className="my-bonuses">
        {seat.bonuses.map((count, index) =>
          count > 0 ? (
            <span key={`my-bonus-${index}`} className="bonus-chip">
              {GEM_LABEL[GEMS[index] as Gem]}
              {count}
            </span>
          ) : null,
        )}
      </div>
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
