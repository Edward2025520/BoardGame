import { useEffect, useMemo, useRef, useState } from 'react';
import type { ActionOption, AzulColor, AzulSeat, AzulView, LogItem } from '../../api/types';
import { AZUL_COLORS, AZUL_LABEL } from '../../api/types';
import { play } from '../../lib/audio';
import { StartMarkerTile, Tile } from './art';
import { availableTargets, ghostPlacement, groupTiles, takeKey } from './selection';

interface Props {
  view: AzulView;
  mySeat: number;
  actions: ActionOption[];
  log: LogItem[];
  disabled: boolean;
  onAction: (key: string) => void;
}

interface Selection {
  source: number;
  color: AzulColor;
  count: number;
}

const POSITION = ['s', 'n', 'w', 'e'];
const STAGE_WIDTH = 1440;
const STAGE_HEIGHT = 1200;

export function AzulBoard({ view, mySeat, actions, log, disabled, onAction }: Props) {
  const [selection, setSelection] = useState<Selection | null>(null);
  const [target, setTarget] = useState<number | null>(null);
  const [hover, setHover] = useState<number | null>(null);
  const wrapRef = useRef<HTMLDivElement | null>(null);
  const [scale, setScale] = useState(1);

  useEffect(() => {
    setSelection(null);
    setTarget(null);
    setHover(null);
  }, [view]);

  useEffect(() => {
    const element = wrapRef.current;
    if (!element) {
      return;
    }
    const measure = () => {
      const available = Math.min(element.clientWidth / STAGE_WIDTH, (window.innerHeight - 150) / STAGE_HEIGHT);
      setScale(Math.max(0.45, Math.min(1, available)));
    };
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  const keys = useMemo(() => new Set(actions.map((action) => action.key)), [actions]);
  const targets = useMemo(
    () => new Set(selection ? availableTargets(actions, selection.source, selection.color) : []),
    [actions, selection],
  );
  const labelFor = (seatNumber: number) =>    seatNumber === mySeat ? '你' : `AI ${seatNumber}`;

  const pick = (source: number, color: AzulColor, count: number) => {
    if (disabled) {
      return;
    }
    setSelection({ source, color, count });
    setTarget(null);
    setHover(null);
    play('select');
  };

  const chooseTarget = (line: number) => {
    if (disabled || !selection || !targets.has(line)) {
      return;
    }
    setTarget(line);
    play('select');
  };

  const confirm = () => {
    if (disabled || !selection || target === null) {
      return;
    }
    const key = takeKey(selection.source, selection.color, target);
    if (!keys.has(key)) {
      return;
    }
    play('take');
    onAction(key);
    setSelection(null);
    setTarget(null);
  };

  const cancel = () => {
    setSelection(null);
    setTarget(null);
    setHover(null);
  };

  const previewLine = target ?? hover;

  return (
    <div className="azul-wrap" ref={wrapRef} style={{ height: STAGE_HEIGHT * scale }}>
      <div
        className="azul-stage"
        style={{
          width: STAGE_WIDTH,
          height: STAGE_HEIGHT,
          transform: `translateX(-50%) scale(${scale})`,
        }}
      >
        {view.seats.map((player, index) => (
          <PlayerBoard
            key={player.seat}
            seat={player}
            label={labelFor(player.seat)}
            position={POSITION[index] ?? 's'}
            mine={player.seat === mySeat}
            targets={player.seat === mySeat ? targets : new Set<number>()}
            activeTarget={player.seat === mySeat ? target : null}
            previewTarget={player.seat === mySeat ? previewLine : null}
            selection={player.seat === mySeat ? selection : null}
            onPickTarget={chooseTarget}
            onHoverTarget={setHover}
            onFloor={() => chooseTarget(-1)}
            floorTargetable={targets.has(-1)}
          />
        ))}

        <div className="azul-center">
          <div className="factory-grid">
            {view.factories.map((factory) => (
              <div className="factory" key={factory.index}>
                {groupTiles(factory.tiles).map((group) => (
                  <button
                    key={group.color}
                    type="button"
                    className={
                      selection?.source === factory.index && selection.color === group.color
                        ? 'tile-group picked'
                        : 'tile-group'
                    }
                    disabled={disabled}
                    onClick={() => pick(factory.index, group.color, group.count)}
                    title={`工厂 ${factory.index}：${AZUL_LABEL[group.color]} ${group.count}`}
                  >
                    <Tile color={group.color} size={28} />
                    <span>{group.count}</span>
                  </button>
                ))}
              </div>
            ))}
          </div>

          <div className="center-pile">
            <span className="center-title">中央</span>
            {view.center.hasMarker && <StartMarkerTile size={26} />}
            {groupTiles(view.center.tiles).map((group) => (
              <button
                key={group.color}
                type="button"
                className={
                  selection?.source === -1 && selection.color === group.color
                    ? 'tile-group picked'
                    : 'tile-group'
                }
                disabled={disabled}
                onClick={() => pick(-1, group.color, group.count)}
                title={`中央：${AZUL_LABEL[group.color]} ${group.count}`}
              >
                <Tile color={group.color} size={28} />
                <span>{group.count}</span>
              </button>
            ))}
            {view.center.tiles.length === 0 && !view.center.hasMarker && (
              <span className="hint">空</span>
            )}
          </div>

          <div className="azul-actions">
            {selection ? (
              <>
                <span className="azul-selection">
                  已选 {AZUL_LABEL[selection.color]} × {selection.count}
                  {target !== null && (target < 0 ? ' → 地板' : ` → 第 ${target + 1} 行`)}
                </span>
                <button type="button" className="primary" disabled={target === null} onClick={confirm}>
                  {target === null ? '选一个目标行' : '确认放置'}
                </button>
                <button type="button" className="ghost" onClick={cancel}>
                  取消
                </button>
              </>
            ) : (
              <span className="hint">
                {disabled ? '等待 AI 行动…' : '点工厂或中央的一色瓦片，再点目标行'}
              </span>
            )}
          </div>

          <div className="azul-log">
            {[...log]
              .slice(-2)
              .reverse()
              .map((item) => (
                <div key={`${item.version}-${item.at}`} className="log-line">
                  <span className="log-seat">{item.seat === mySeat ? '你' : `AI ${item.seat}`}</span>
                  {item.text}
                </div>
              ))}
            {log.length === 0 && <span className="hint">还没有动作</span>}
          </div>
        </div>
      </div>
    </div>
  );
}

function PlayerBoard({
  seat,
  label,
  position,
  mine,
  targets,
  activeTarget,
  previewTarget,
  selection,
  onPickTarget,
  onHoverTarget,
  onFloor,
  floorTargetable,
}: {
  seat: AzulSeat;
  label: string;
  position: string;
  mine: boolean;
  targets: Set<number>;
  activeTarget: number | null;
  previewTarget: number | null;
  selection: Selection | null;
  onPickTarget: (line: number) => void;
  onHoverTarget: (line: number | null) => void;
  onFloor: () => void;
  floorTargetable: boolean;
}) {
  const preview = selection && previewTarget !== null && previewTarget >= 0 && previewTarget < 5
    ? { line: previewTarget, ...ghostPlacement(seat.lines[previewTarget], selection.color, selection.count) }
    : null;
  const floorGhost =
    selection && previewTarget === -1
      ? Math.min(selection.count, Math.max(0, 7 - seat.floor.length - (seat.hasMarker ? 1 : 0)))
      : 0;

  return (
    <div className={`azul-seat azul-seat-${position}${mine ? ' mine' : ''}`}>
      <div className="seat-head">
        <span className="seat-name">{label}</span>
        {seat.first && <StartMarkerTile size={16} />}
        <span className="seat-score">{seat.score} 分</span>
      </div>

      <div className="board-body">
        <div className="pattern-lines">
          {seat.lines.map((line, row) => {
            const enabled = mine && targets.has(row);
            const ghost = preview && preview.line === row ? preview.placed : 0;
            return (
              <button
                key={row}
                type="button"
                className={[
                  'pattern-line',
                  enabled ? 'enabled' : '',
                  activeTarget === row ? 'chosen' : '',
                ].join(' ')}
                disabled={!enabled}
                onClick={() => onPickTarget(row)}
                onMouseEnter={() => onHoverTarget(row)}
                onMouseLeave={() => onHoverTarget(null)}
              >
                {Array.from({ length: line.capacity }).map((_, slot) => {
                  const filled = slot < line.count;
                  const ghosted = !filled && slot < line.count + ghost;
                  return (
                    <span
                      key={slot}
                      className={`slot${filled ? ' filled' : ''}${ghosted ? ' ghost' : ''}`}
                    >
                      {filled && line.color && <Tile color={line.color as AzulColor} size={20} />}
                      {ghosted && selection && <Tile color={selection.color} size={20} />}
                    </span>
                  );
                })}
              </button>
            );
          })}
        </div>

        <div className="wall">
          {seat.wall.map((row, rowIndex) =>
            row.map((cell, colIndex) => (
              <span
                key={`${rowIndex}-${colIndex}`}
                className={`wall-cell${cell.filled ? ' filled' : ''}`}
                data-color={cell.color}
                title={AZUL_LABEL[cell.color as AzulColor] ?? cell.color}
              >
                {cell.filled && <Tile color={cell.color as AzulColor} size={22} />}
              </span>
            )),
          )}
        </div>
      </div>

      <div className="floor-line">
        <span className="floor-label">地板</span>
        {Array.from({ length: 7 }).map((_, index) => {
          const tile = seat.floor[index];
          const ghosted = index >= seat.floor.length && index < seat.floor.length + floorGhost;
          return (
            <span key={index} className={`floor-slot${tile ? ' filled' : ''}${ghosted ? ' ghost' : ''}`}>
              {tile && <Tile color={tile as AzulColor} size={18} />}
              {!tile && ghosted && selection && <Tile color={selection.color} size={18} />}
            </span>
          );
        })}
        {seat.hasMarker && <StartMarkerTile size={18} />}
        {mine && floorTargetable && (
          <button type="button" className="ghost tiny" onClick={onFloor}>
            只放地板
          </button>
        )}
      </div>
    </div>
  );
}

export { AZUL_COLORS };
