import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Lobby } from '../pages/Lobby';

const games = [{ id: 'splendor', name: '璀璨宝石', description: '收集宝石', minPlayers: 2, maxPlayers: 4 }];

describe('Lobby', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('渲染游戏列表', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => games,
      }),
    );

    render(
      <MemoryRouter>
        <Lobby />
      </MemoryRouter>,
    );

    expect(await screen.findByText('璀璨宝石')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '开始对局' })).toBeInTheDocument();
  });

  it('接口失败时展示错误', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        text: async () => JSON.stringify({ error: '服务器炸了' }),
      }),
    );

    render(
      <MemoryRouter>
        <Lobby />
      </MemoryRouter>,
    );

    expect(await screen.findByText('服务器炸了')).toBeInTheDocument();
  });

  it('选 4 人后建局请求里 seatCount 是 4', async () => {
    const bodies: unknown[] = [];
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        if (String(url).endsWith('/api/games')) {
          return { ok: true, json: async () => games };
        }
        bodies.push(JSON.parse(String(init?.body)));
        return { ok: true, json: async () => ({ sessionId: 's1' }) };
      }),
    );

    render(
      <MemoryRouter>
        <Lobby />
      </MemoryRouter>,
    );

    fireEvent.click(await screen.findByRole('button', { name: /4 人/ }));
    fireEvent.click(screen.getByRole('button', { name: '开始对局' }));

    await waitFor(() => expect(bodies).toHaveLength(1));
    expect(bodies[0]).toMatchObject({ seatCount: 4, humanSeats: [0] });
  });
});
