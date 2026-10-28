import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Lobby } from '../pages/Lobby';

describe('Lobby', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('渲染游戏列表', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => [
          { id: 'splendor', name: '璀璨宝石', description: '收集宝石', minPlayers: 2, maxPlayers: 4 },
        ],
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
});
