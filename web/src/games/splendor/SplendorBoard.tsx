import { useEffect, useMemo, useRef, useState } from 'react';
import type { ActionOption, Gem, LogItem, SplendorCard, SplendorView } from '../../api/types';
import { play } from '../../lib/audio';
import { CardArt } from './art';
import { CardRows, GemBank, LogPanel, MyPanel, NoblesRow, OpponentStrip } from './parts';
import { emptySelection, selectionAction, toggleGem, type Selection } from './selection';

interface Props {
  view: SplendorView;
  mySeat: number;
  actions: ActionOption[];
  log: LogItem[];
  disabled: boolean;
  onAction: (key: string) => void;
}

interface Flight {
  card: SplendorCard;
  from: DOMRect;
  to: DOMRect;
}

const GEM_LABEL: Record<string, string> = {
  white: '白',
  blue: '蓝',
  green: '绿',
  red: '红',
  black: '黑',
  gold: '金',
};

export function SplendorBoard({ view, mySeat, actions, log, disabled, onAction }: Props) {
  const [selection, setSelection] = useState<Selection>(emptySelection);
  const [hint, setHint] = useState<string | null>(null);
  const [flight, setFlight] = useState<Flight | null>(null);
  const panelRef = useRef<HTMLDivElement | null>(null);

  // 每次局面更新都清空选择，避免拿着上一手的选中状态误操作
  useEffect(() => {
    setSelection(emptySelection);
    setHint(null);
  }, [view]);

  const seat = view.seats[mySeat];
  const discarding = view.phase === 'DISCARD';
  const choosingNoble = view.phase === 'CHOOSE_NOBLE';
  const totalTokens = seat.tokens.reduce((sum, count) => sum + count, 0);
  const maxDiscard = Math.max(0, totalTokens - 10);
  const keys = useMemo(() => new Set(actions.map((action) => action.key)), [actions]);
  const selectedAction = selectionAction(selection, { discarding }, actions);
  const opponents = view.seats.filter((item) => item.seat !== mySeat);
  const labelBySeat = new Map<number, string>();
  opponents.forEach((opponent, index) => labelBySeat.set(opponent.seat, `AI ${index + 1}`));
  const labelFor = (seatNumber: number) =>
    seatNumber === mySeat ? '你' : (labelBySeat.get(seatNumber) ?? 'AI');
  // 官方摆法：3 级在上，1 级在下
  const orderedRows = useMemo(() => [...view.rows].sort((left, right) => right.tier - left.tier), [view.rows]);

  const clickGem = (gem: Gem) => {
    if (disabled) {
      return;
    }
    const result = toggleGem(selection, gem, { discarding, maxDiscard });
    setSelection(result.selection);
    setHint(result.hint ?? null);
    play(result.hint ? 'error' : 'select');
  };

  const confirm = () => {
    if (!selectedAction || disabled) {
      return;
    }
    play(selectedAction.key.startsWith('take') ? 'take' : 'select');
    onAction(selectedAction.key);
    setSelection(emptySelection);
    setHint(null);
  };

  const fly = (card: SplendorCard, source: HTMLElement) => {
    const target = panelRef.current;
    if (!target) {
      return;
    }
    const from = source.getBoundingClientRect();
    const to = target.getBoundingClientRect();
    setFlight({ card, from, to });
    window.setTimeout(() => setFlight(null), 480);
  };

  const buy = (card: SplendorCard, source: HTMLElement) => {
    const key = `buy:${card.code}`;
    if (disabled || !keys.has(key)) {
      return;
    }
    play('buy');
    fly(card, (source.closest('.card') as HTMLElement) ?? source);
    onAction(key);
  };

  const reserve = (card: SplendorCard) => {
    const key = `reserve:${card.code}`;
    if (disabled || !keys.has(key)) {
      return;
    }
    play('select');
    onAction(key);
  };

  const deckReserve = (tier: number) => {
    const key = `reserve:deck:${tier}`;
    if (disabled || !keys.has(key)) {
      return;
    }
    play('select');
    onAction(key);
  };

  const chooseNoble = (code: string) => {
    const key = `noble:${code}`;
    if (disabled || !keys.has(key)) {
      return;
    }
    play('buy');
    onAction(key);
  };

  const status = disabled
    ? '等待 AI 行动…'
    : discarding
      ? `宝石超过 10 个，请点自己的宝石弃掉 ${maxDiscard} 个`
      : choosingNoble
        ? '请选择一位贵族'
        : '点宝石取牌，或点卡牌上的「购买 / 预留」';

  return (
    <div className="game-layout">
      <div className="board-main">
        <NoblesRow nobles={view.nobles} choosable={!disabled && choosingNoble} onChoose={chooseNoble} />

        <CardRows
          rows={orderedRows}
          seat={seat}
          keys={keys}
          disabled={disabled}
          onBuy={buy}
          onReserve={reserve}
          onDeckReserve={deckReserve}
        />

        <GemBank
          bank={view.bank}
          goldBank={view.goldBank}
          selected={selection.gems}
          selectable={!disabled && !discarding && !choosingNoble}
          onSelect={clickGem}
        />

        <div className="status-line">{status}</div>

        <div className="selection-bar">
          {selection.gems.length > 0 ? (
            <>
              <span className="selection-summary">
                已选 {selection.gems.map((gem) => GEM_LABEL[gem] ?? gem).join(' + ')}
              </span>
              <button type="button" className="primary" disabled={!selectedAction || disabled} onClick={confirm}>
                {selectedAction ? `确认：${selectedAction.label}` : '还不能确认'}
              </button>
              <button type="button" className="ghost" onClick={() => setSelection(emptySelection)}>
                取消
              </button>
            </>
          ) : (
            <span className="hint">{discarding ? '点击下方自己的宝石选择要弃的' : '选中的宝石会显示在这里'}</span>
          )}
          {hint && <span className="hint error">{hint}</span>}
          {keys.has('pass') && !disabled && (
            <button type="button" className="ghost" onClick={() => onAction('pass')}>
              跳过
            </button>
          )}
        </div>

        <div ref={panelRef}>
          <MyPanel
            seat={seat}
            label="你"
            discarding={discarding && !disabled}
            selectedGems={selection.gems}
            onDiscardClick={clickGem}
          />
        </div>
      </div>

      <aside className="board-rail">
        <div className="rail-opponents">
          {opponents.map((opponent) => (
            <OpponentStrip
              key={opponent.seat}
              seat={opponent}
              label={labelBySeat.get(opponent.seat) ?? 'AI'}
            />
          ))}
        </div>
        <LogPanel log={log} labelFor={labelFor} />
      </aside>

      {flight && <FlyingCard flight={flight} />}
    </div>
  );
}

function FlyingCard({ flight }: { flight: Flight }) {
  const [go, setGo] = useState(false);

  useEffect(() => {
    const frame = requestAnimationFrame(() => setGo(true));
    return () => cancelAnimationFrame(frame);
  }, []);

  const dx = flight.to.left + flight.to.width / 2 - (flight.from.left + flight.from.width / 2);
  const dy = flight.to.top + 24 - (flight.from.top + flight.from.height / 2);

  return (
    <div
      className="fly-card"
      style={{
        left: flight.from.left,
        top: flight.from.top,
        width: flight.from.width,
        height: flight.from.height,
        opacity: go ? 0 : 1,
        transform: go ? `translate(${dx}px, ${dy}px) scale(0.35)` : 'none',
      }}
    >
      <CardArt card={flight.card} />
    </div>
  );
}
