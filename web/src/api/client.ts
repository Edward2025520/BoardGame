import type {
  Difficulty,
  GameSummary,
  HintResult,
  ReviewList,
  ReviewMove,
  SessionDto,
} from './types';

async function parse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const text = await response.text();
    let message = text;
    try {
      message = (JSON.parse(text) as { error?: string }).error ?? text;
    } catch {
      // 保留原文
    }
    throw new Error(message || `请求失败：${response.status}`);
  }
  return (await response.json()) as T;
}

export async function listGames(): Promise<GameSummary[]> {
  return parse<GameSummary[]>(await fetch('/api/games'));
}

export interface CreateSessionRequest {
  gameId: string;
  seatCount: number;
  humanSeats: number[];
  difficulty: Difficulty;
  seed?: number;
}

export async function createSession(request: CreateSessionRequest): Promise<SessionDto> {
  const response = await fetch('/api/sessions', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(request),
  });
  return parse<SessionDto>(response);
}

export async function fetchSession(sessionId: string): Promise<SessionDto> {
  return parse<SessionDto>(await fetch(`/api/sessions/${sessionId}`));
}

export async function submitAction(
  sessionId: string,
  actionKey: string,
  expectedVersion: number,
): Promise<SessionDto> {
  const response = await fetch(`/api/sessions/${sessionId}/actions`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ actionKey, expectedVersion }),
  });
  return parse<SessionDto>(response);
}

export function eventsUrl(sessionId: string): string {
  return `/api/sessions/${sessionId}/events`;
}

export async function changeDifficulty(
  sessionId: string,
  difficulty: string,
): Promise<SessionDto> {
  const response = await fetch(`/api/sessions/${sessionId}/difficulty`, {
    method: 'PATCH',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ difficulty }),
  });
  return parse<SessionDto>(response);
}

export async function requestHint(sessionId: string): Promise<HintResult> {
  return parse<HintResult>(await fetch(`/api/sessions/${sessionId}/hint`, { method: 'POST' }));
}

export async function undoMove(sessionId: string): Promise<SessionDto> {
  return parse<SessionDto>(await fetch(`/api/sessions/${sessionId}/undo`, { method: 'POST' }));
}

export async function fetchReview(sessionId: string): Promise<ReviewList> {
  return parse<ReviewList>(await fetch(`/api/sessions/${sessionId}/review`));
}

export async function analyzeReviewMove(sessionId: string, index: number): Promise<ReviewMove> {
  return parse<ReviewMove>(
    await fetch(`/api/sessions/${sessionId}/review/${index}`, { method: 'POST' }),
  );
}
