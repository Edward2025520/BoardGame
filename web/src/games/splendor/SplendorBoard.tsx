import {
  GEMS,
  GEM_COLOR,
  GEM_LABEL,
  type ActionOption,
  type Gem,
  type SplendorSeat,
  type SplendorView,
} from '../../api/types';
import { CardArt, GemCircle, NobleArt } from './art';

interface Props {
  view: SplendorView;
  mySeat: number;
  actions: ActionOption[];
  disabled: boolean;
  onAction: (key: string) => void;
}

const GROUPS: { prefix: string; title: string }[] = [
  { prefix: 'take:', title: '拿宝石' },
  { prefix: 'buy:', title: '购买' },
  { prefix: 'reserve:', title: '预留' },
  { prefix: 'discard:', title: '弃宝石' },
  { prefix: 'noble:', title: '选贵族' },
  { prefix: 'pass', title: '其它' },
];

export function SplendorBoard({ view, mySeat, actions, disabled, onAction }: Props) {
  const affordable = new Set(
    actions.filter((action) => action.key.startsWith('buy:')).map((action) => action.key.slice(4)),
  );
  const mine = view.seats[mySeat];
  const opponents = view.seats.filter((seat) => seat.seat !== mySeat);

  return (
    <div className="board">
      <div className="board-top">
        <div className="nobles">
          {view.nobles.map((noble) => (
            <div key={noble.code} className={noble.qualified ? 'noble qualified' : 'noble'}>
              <NobleArt noble={noble} />
            </div>
          ))}
          {view.finalRound && <div className="final-round">最后一轮</div>}
        </div>
        <div className="bank">
          {GEMS.slice(0, 5).map((gem, index) => (
            <div key={gem} className="bank-gem">
              <svg viewBox="0 0 34 34" className="gem-badge">
                <GemCircle gem={gem} x={17} y={17} r={15} uid={`bank-${gem}`} />
              </svg>
              <span>{view.bank[index]}</span>
            </div>
          ))}
          <div className="bank-gem">
            <svg viewBox="0 0 34 34" className="gem-badge">
              <GemCircle gem="gold" x={17} y={17} r={15} uid="bank-gold" />
            </svg>
            <span>{view.goldBank}</span>
          </div>
        </div>
      </div>

      <div className="rows">
        {view.rows.map((row) => (
          <div className="row" key={row.tier}>
            <div className="deck" title={`剩余 ${row.deckSize} 张`}>
              <span>{row.deckSize}</span>
              <small>{row.tier} 级</small>
            </div>
            <div className="row-cards">
              {row.cards.map((card) => (
                <div
                  key={card.code}
                  className={affordable.has(card.code) ? 'card affordable' : 'card'}
                  title={card.code}
                >
                  <CardArt card={card} />
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      <div className="seats">
        {opponents.map((seat) => (
          <SeatPanel key={seat.seat} seat={seat} label={`对手 ${seat.seat + 1}`} />
        ))}
        <SeatPanel seat={mine} label="你" highlight />
      </div>

      <div className="actions">
        {GROUPS.map((group) => {
          const items = actions.filter((action) =>
            group.prefix === 'pass' ? !GROUPS.some((g) => g.prefix !== 'pass' && action.key.startsWith(g.prefix)) : action.key.startsWith(group.prefix),
          );
          if (items.length === 0) {
            return null;
          }
          return (
            <div className="action-group" key={group.title}>
              <span className="action-title">{group.title}</span>
              <div className="chips">
                {items.map((action) => (
                  <button
                    key={action.key}
                    type="button"
                    className="chip"
                    disabled={disabled}
                    onClick={() => onAction(action.key)}
                    title={action.key}
                  >
                    {action.label}
                  </button>
                ))}
              </div>
            </div>
          );
        })}
        {actions.length === 0 && <div className="hint">等待对手行动…</div>}
      </div>
    </div>
  );
}

export function SeatPanel({
  seat,
  label,
  highlight,
}: {
  seat: SplendorSeat;
  label: string;
  highlight?: boolean;
}) {
  return (
    <div className={highlight ? 'seat-panel mine' : 'seat-panel'}>
      <div className="seat-head">
        <span className="seat-name">{label}</span>
        <span className="seat-points">{seat.points} 分</span>
        {seat.current && <span className="turn-dot" />}
      </div>
      <div className="seat-line">
        {GEMS.slice(0, 5).map((gem, index) => (
          <span key={`token-${gem}`} className="mini-token" style={{ background: GEM_COLOR[gem as Gem] }}>
            {seat.tokens[index] > 0 ? seat.tokens[index] : ''}
          </span>
        ))}
        <span className="mini-token gold" style={{ background: GEM_COLOR.gold }}>
          {seat.tokens[5] > 0 ? seat.tokens[5] : ''}
        </span>
      </div>
      <div className="seat-line">
        {GEMS.slice(0, 5).map((gem, index) => (
          <span key={`bonus-${gem}`} className="mini-bonus">
            {GEM_LABEL[gem as Gem]}
            {seat.bonuses[index]}
          </span>
        ))}
        <span className="mini-bonus">卡 {seat.cards}</span>
        <span className="mini-bonus">留 {seat.reservedCount}/3</span>
      </div>
      {seat.reserved.length > 0 && (
        <div className="seat-reserved">
          {seat.reserved.map((card) => (
            <div key={card.code} className="reserved-card" title={`${card.tier} 级 ${card.bonus}`}>
              <CardArt card={card} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
