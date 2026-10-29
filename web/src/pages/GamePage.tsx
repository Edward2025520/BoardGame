import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  analyzeReviewMove,
  changeDifficulty,
  fetchReview,
  requestHint,
  undoMove,
} from '../api/client';
import type { HintResult, ReviewMove } from '../api/types';
import { useSession } from '../hooks/useSession';
import { boardFor } from '../games/registry';
import { useArtFallbackCount } from '../components/Art';
import { SoundPanel } from '../components/SoundPanel';
import { DIFFICULTY_OPTIONS } from '../lib/difficulties';
import { play } from '../lib/audio';

export function GamePage() {
  const [params] = useSearchParams();
  const sessionId = params.get('session');

  if (!sessionId) {
    return (
      <main className="page">
        <p>缺少对局参数。</p>
        <Link to="/">返回大厅</Link>
      </main>
    );
  }
  return <GameView sessionId={sessionId} />;
}

interface ReviewState {
  progress: number;
  total: number;
  items: ReviewMove[];
}

function GameView({ sessionId }: { sessionId: string }) {
  const { session, error, act } = useSession(sessionId);
  const fallbackArt = useArtFallbackCount();
  const [hint, setHint] = useState<HintResult | null>(null);
  const [hintBusy, setHintBusy] = useState(false);
  const [undoBusy, setUndoBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [review, setReview] = useState<ReviewState | null>(null);
  const [reviewBusy, setReviewBusy] = useState(false);
  const version = session?.version;

  useEffect(() => {
    setHint(null);
  }, [version]);

  if (!session) {
    return (
      <main className="page">
        <p>{error ?? '加载对局中…'}</p>
        <Link to="/">返回大厅</Link>
      </main>
    );
  }

  const mySeat = session.humanSeats[0] ?? 0;
  const view = session.boards[String(mySeat)];
  const Board = boardFor(session.gameId);
  const myTurn = !session.finished && session.currentSeat === mySeat && !session.aiThinking;

  const askHint = async () => {
    setHintBusy(true);
    setActionError(null);
    try {
      const result = await requestHint(session.sessionId);
      setHint(result);
      play('hint');
    } catch (cause) {
      setActionError((cause as Error).message);
      play('error');
    } finally {
      setHintBusy(false);
    }
  };

  const doUndo = async () => {
    setUndoBusy(true);
    setActionError(null);
    try {
      await undoMove(session.sessionId);
      play('select');
    } catch (cause) {
      setActionError((cause as Error).message);
      play('error');
    } finally {
      setUndoBusy(false);
    }
  };

  const switchDifficulty = async (value: string) => {
    setActionError(null);
    try {
      await changeDifficulty(session.sessionId, value);
      play('select');
    } catch (cause) {
      setActionError((cause as Error).message);
    }
  };

  const runReview = async () => {
    setReviewBusy(true);
    setActionError(null);
    setReview({ progress: 0, total: 0, items: [] });
    try {
      const list = await fetchReview(session.sessionId);
      setReview({ progress: 0, total: list.total, items: [] });
      const results: ReviewMove[] = [];
      for (const item of list.items) {
        try {
          const analyzed = await analyzeReviewMove(session.sessionId, item.index);
          if (analyzed.delta > 0) {
            results.push(analyzed);
          }
        } catch {
          // 单手失败不打断整体复盘
        }
        setReview({ progress: item.index + 1, total: list.total, items: [...results] });
      }
      results.sort((left, right) => right.delta - left.delta);
      setReview({ progress: list.total, total: list.total, items: results.slice(0, 3) });
      play('noble');
    } catch (cause) {
      setActionError((cause as Error).message);
      setReview(null);
    } finally {
      setReviewBusy(false);
    }
  };

  return (
    <main className="page game-page">
      <header className="game-head">
        <Link to="/" className="back">
          ← 大厅
        </Link>
        <h1>{session.gameName}</h1>
        <span className="badge">{session.seatCount} 人</span>
        <select
          className="difficulty-select"
          value={session.difficulty}
          disabled={session.finished}
          onChange={(event) => void switchDifficulty(event.target.value)}
          title="切换 AI 难度，下一次 AI 行动生效"
        >
          {DIFFICULTY_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <span className="version">第 {session.version} 手</span>
        {session.aiThinking && <span className="thinking">AI 思考中…</span>}
        {session.finished && <span className="badge done">已结束</span>}
        <button
          type="button"
          className="ghost"
          onClick={askHint}
          disabled={!myTurn || hintBusy}
          title="让 AI 帮你算一手"
        >
          {hintBusy ? '教练思考中…' : '支一招'}
        </button>
        <button
          type="button"
          className="ghost"
          onClick={doUndo}
          disabled={!session.canUndo || undoBusy}
          title="回到你上一手之前"
        >
          悔棋
        </button>
        <SoundPanel />
      </header>

      {(error || actionError) && <div className="error">{actionError ?? error}</div>}

      {hint && (
        <div className="coach-bar">
          <span>教练建议：{hint.label}</span>
          <button type="button" className="ghost tiny" onClick={() => setHint(null)}>
            知道了
          </button>
        </div>
      )}

      {Board && view !== undefined ? (
        <Board
          view={view}
          mySeat={mySeat}
          actions={session.legalActions}
          log={session.log}
          hintKey={hint?.actionKey ?? null}
          disabled={!myTurn}
          onAction={act}
        />
      ) : (
        <div className="hint">这个游戏还没有界面。</div>
      )}

      {fallbackArt > 0 && session.gameId === 'splendor' && (
        <p className="art-hint">
          当前显示的是自绘卡面：没有找到官方图。在仓库根目录运行
          <code> node tools/fetch-splendor-art.mjs </code>
          抓取一次后重新构建即可换上官方卡面。
        </p>
      )}

      {session.outcome && (
        <div className="overlay">
          <div className="overlay-card">
            <h2>对局结束</h2>
            <p>获胜：{session.outcome.winners.map((seat) => `P${seat + 1}`).join('、')}</p>
            <p>比分：{session.outcome.points.join(' : ')}</p>

            {session.reviewable && !review && (
              <button type="button" className="primary" disabled={reviewBusy} onClick={runReview}>
                教练复盘
              </button>
            )}

            {review && (
              <div className="review-panel">
                <h3>教练复盘</h3>
                {reviewBusy && (
                  <p className="hint">
                    复盘中 {review.progress}/{review.total}
                  </p>
                )}
                {!reviewBusy && review.items.length === 0 && <p>这局没有明显下亏的地方。</p>}
                {review.items.length > 0 && (
                  <ol className="review-list">
                    {review.items.map((item) => (
                      <li key={item.index}>
                        <div>第 {item.index + 1} 手，你下了「{item.chosenLabel}」</div>
                        <div>
                          更优是「{item.bestLabel}」，胜率差约 {(item.delta * 100).toFixed(0)}%
                        </div>
                      </li>
                    ))}
                  </ol>
                )}
              </div>
            )}

            <Link className="primary" to="/">
              再来一局
            </Link>
          </div>
        </div>
      )}
    </main>
  );
}
