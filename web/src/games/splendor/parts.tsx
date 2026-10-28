import {
  GEMS,
  GEM_COLOR,
  GEM_LABEL,
  type Gem,
  type SplendorCard,
  type SplendorNoble,
  type SplendorSeat,
  type SplendorView,
} from '../../api/types';
import { cardShortfall, shortfallText } from './selection';
import { CardArt, GemCircle, NobleArt } from './art';

export function Avatar({ label, mine }: { label: string; mine?: boolean }) {
  return <span className={mine ? 'avatar mine' : 'avatar'}>{label.slice(0, 1)}</span>;
}

export function OpponentStrip({ seat, index }: { seat: SplendorSeat; index: number }) {
  return (
    <div className={seat.current ? 'opponent current' : 'opponent'}>
      <Avatar label={`P${index + 1}`} />
      <div className="opponent-body">
        <div className="opponent-head">
          <span>对手 {index + 1}</span>
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
        </div>
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

export function CardTile({
  card,
  seat,
  keys,
  disabled,
  onBuy,
  onReserve,
}: {
  card: SplendorCard;
  seat: SplendorSeat;
  keys: Set<string>;
  disabled: boolean;
  onBuy: (card: SplendorCard, source: HTMLElement) => void;
  onReserve: (card: SplendorCard) => void;
}) {
  const canBuy = keys.has(`buy:${card.code}`);
  const canReserve = keys.has(`reserve:${card.code}`);
  const shortfall = cardShortfall(card, seat);

  return (
    <div className={canBuy ? 'card affordable' : 'card'}>
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
      <div className={canBuy ? 'card-status ok' : 'card-status'}>{shortfallText(shortfall)}</div>
    </div>
  );
}

export function CardRows({
  view,
  seat,
  keys,
  disabled,
  onBuy,
  onReserve,
  onDeckReserve,
}: {
  view: SplendorView;
  seat: SplendorSeat;
  keys: Set<string>;
  disabled: boolean;
  onBuy: (card: SplendorCard, source: HTMLElement) => void;
  onReserve: (card: SplendorCard) => void;
  onDeckReserve: (tier: number) => void;
}) {
  return (
    <div className="rows">
      {view.rows.map((row) => (
        <div className="row" key={row.tier}>
          <button
            type="button"
            className="deck"
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
  onSelect,
}: {
  bank: number[];
  goldBank: number;
  selected: Gem[];
  selectable: boolean;
  onSelect: (gem: Gem) => void;
}) {
  return (
    <div className="gem-bank">
      {GEMS.slice(0, 5).map((gem, index) => {
        const count = bank[index];
        const picked = selected.filter((pickedGem) => pickedGem === gem).length;
        return (
          <button
            key={gem}
            type="button"
            className={picked > 0 ? 'gem-button picked' : 'gem-button'}
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
  onDiscardClick,
}: {
  seat: SplendorSeat;
  label: string;
  discarding: boolean;
  selectedGems: Gem[];
  onDiscardClick: (gem: Gem) => void;
}) {
  return (
    <div className="my-panel">
      <div className="my-head">
        <Avatar label={label} mine />
        <span className="my-name">{label}</span>
        <span className="my-points">{seat.points} 分</span>
        <span className="my-cards">{seat.cards} 张卡</span>
      </div>
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
          {seat.reserved.map((card) => (
            <div key={card.code} className="reserved-card">
              <CardArt card={card} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
