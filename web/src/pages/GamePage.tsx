import { Link, useSearchParams } from 'react-router-dom';
import { useSession } from '../hooks/useSession';
import { boardFor } from '../games/registry';
import { useArtFallbackCount } from '../components/Art';

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

function GameView({ sessionId }: { sessionId: string }) {
  const { session, error, act } = useSession(sessionId);
  const fallbackArt = useArtFallbackCount();

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
  const recent = [...session.log].slice(-8).reverse();

  return (
    <main className="page game-page">
      <header className="game-head">
        <Link to="/" className="back">
          ← 大厅
        </Link>
        <h1>{session.gameName}</h1>
        <span className="badge">{session.difficulty}</span>
        <span className="version">第 {session.version} 手</span>
        {session.aiThinking && <span className="thinking">AI 思考中…</span>}
        {session.finished && <span className="badge done">已结束</span>}
      </header>

      {error && <div className="error">{error}</div>}

      {Board && view !== undefined ? (
        <Board view={view} mySeat={mySeat} actions={session.legalActions} disabled={!myTurn} onAction={act} />
      ) : (
        <div className="hint">这个游戏还没有界面。</div>
      )}

      <footer className="game-foot">
        {fallbackArt > 0 && (
          <p className="art-hint">
            当前显示的是自绘卡面：没有找到官方图。在仓库根目录运行
            <code> node tools/fetch-splendor-art.mjs </code>
            抓取一次后重新构建即可换上官方卡面。
          </p>
        )}
        <div className="log">
          {recent.map((item) => (
            <div key={`${item.version}-${item.at}`} className="log-line">
              <span className="log-seat">P{item.seat + 1}</span>
              {item.text}
            </div>
          ))}
        </div>
      </footer>

      {session.outcome && (
        <div className="overlay">
          <div className="overlay-card">
            <h2>对局结束</h2>
            <p>获胜：{session.outcome.winners.map((seat) => `P${seat + 1}`).join('、')}</p>
            <p>比分：{session.outcome.points.join(' : ')}</p>
            <Link className="primary" to="/">
              再来一局
            </Link>
          </div>
        </div>
      )}
    </main>
  );
}
