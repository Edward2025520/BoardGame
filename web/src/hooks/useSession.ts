import { useCallback, useEffect, useState } from 'react';
import { eventsUrl, fetchSession, submitAction } from '../api/client';
import type { SessionDto } from '../api/types';

export function useSession(sessionId: string) {
  const [session, setSession] = useState<SessionDto | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    fetchSession(sessionId)
      .then((dto) => {
        if (active) {
          setSession(dto);
        }
      })
      .catch((cause: Error) => {
        if (active) {
          setError(cause.message);
        }
      });

    const source = new EventSource(eventsUrl(sessionId));
    source.addEventListener('state', (event) => {
      try {
        setSession(JSON.parse((event as MessageEvent).data) as SessionDto);
      } catch {
        // 忽略坏包
      }
    });
    source.onopen = () => setError(null);
    source.onerror = () => setError('连接断开，正在重连…');

    return () => {
      active = false;
      source.close();
    };
  }, [sessionId]);

  const act = useCallback(
    async (actionKey: string, payment?: number[]) => {
      if (!session) {
        return;
      }
      try {
        const next = await submitAction(session.sessionId, actionKey, session.version, payment);
        setSession(next);
        setError(null);
      } catch (cause) {
        setError((cause as Error).message);
      }
    },
    [session],
  );

  return { session, error, act };
}
