import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createSession, listGames } from '../api/client';
import type { Difficulty, GameSummary } from '../api/types';

const DIFFICULTIES: { value: Difficulty; label: string; hint: string }[] = [
  { value: 'EASY', label: '入门', hint: '会故意下错' },
  { value: 'NORMAL', label: '普通', hint: '约 1 秒' },
  { value: 'HARD', label: '困难', hint: '约 3 秒' },
  { value: 'EXPERT', label: '地狱', hint: '约 6 秒' },
];

export function Lobby() {
  const navigate = useNavigate();
  const [games, setGames] = useState<GameSummary[]>([]);
  const [difficulty, setDifficulty] = useState<Difficulty>('NORMAL');
  const [error, setError] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);

  useEffect(() => {
    listGames()
      .then(setGames)
      .catch((cause: Error) => setError(cause.message));
  }, []);

  const start = async (game: GameSummary) => {
    setStarting(true);
    try {
      const session = await createSession({
        gameId: game.id,
        seatCount: 2,
        humanSeats: [0],
        difficulty,
      });
      navigate(`/g/${game.id}?session=${session.sessionId}`);
    } catch (cause) {
      setError((cause as Error).message);
      setStarting(false);
    }
  };

  return (
    <main className="lobby">
      <header className="lobby-head">
        <h1>桌游合集</h1>
        <p>和 AI 对战的德式桌游，纯本地运行。</p>
      </header>

      <section className="difficulty">
        <span className="section-title">AI 难度</span>
        <div className="chips">
          {DIFFICULTIES.map((item) => (
            <button
              key={item.value}
              type="button"
              className={difficulty === item.value ? 'chip active' : 'chip'}
              onClick={() => setDifficulty(item.value)}
            >
              {item.label}
              <small>{item.hint}</small>
            </button>
          ))}
        </div>
      </section>

      {error && <div className="error">{error}</div>}

      <section className="game-grid">
        {games.map((game) => (
          <article className="game-card" key={game.id}>
            <h2>{game.name}</h2>
            <p>{game.description}</p>
            <div className="game-meta">
              {game.minPlayers}-{game.maxPlayers} 人
            </div>
            <button type="button" className="primary" disabled={starting} onClick={() => start(game)}>
              {starting ? '开局中…' : '开始对局'}
            </button>
          </article>
        ))}
        {games.length === 0 && !error && <div className="hint">正在读取游戏列表…</div>}
      </section>
    </main>
  );
}
