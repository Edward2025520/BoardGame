import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { SplendorBoard } from '../games/splendor/SplendorBoard';
import type { SplendorView } from '../api/types';

const view: SplendorView = {
  playerCount: 2,
  currentSeat: 0,
  phase: 'PLAY',
  finalRound: false,
  bank: [4, 4, 4, 4, 4],
  goldBank: 5,
  reservedLimit: 3,
  seats: [
    {
      seat: 0,
      points: 0,
      cards: 0,
      tokens: [0, 0, 0, 0, 0, 0],
      bonuses: [0, 0, 0, 0, 0],
      reservedCount: 0,
      reserved: [],
      current: true,
    },
    {
      seat: 1,
      points: 0,
      cards: 0,
      tokens: [0, 0, 0, 0, 0, 0],
      bonuses: [0, 0, 0, 0, 0],
      reservedCount: 0,
      reserved: [],
      current: false,
    },
  ],
  rows: [
    {
      tier: 1,
      deckSize: 36,
      cards: [
        { code: '2g2B', tier: 1, points: 0, bonus: 'blue', cost: { green: 2, black: 2 } },
      ],
    },
    { tier: 2, deckSize: 26, cards: [] },
    { tier: 3, deckSize: 16, cards: [] },
  ],
  nobles: [{ code: '4g4r', requirement: { green: 4, red: 4 }, qualified: false }],
};

describe('SplendorBoard', () => {
  it('按前缀分组展示合法动作，点击后回调 key', () => {
    const onAction = vi.fn();
    render(
      <SplendorBoard
        view={view}
        mySeat={0}
        actions={[
          { key: 'take:WHITE,BLUE,RED', label: '拿 白+蓝+红' },
          { key: 'buy:2g2B', label: '购买 1 级 蓝 卡' },
        ]}
        disabled={false}
        onAction={onAction}
      />,
    );

    expect(screen.getByText('拿宝石')).toBeInTheDocument();
    expect(screen.getByText('购买')).toBeInTheDocument();

    fireEvent.click(screen.getByText('拿 白+蓝+红'));
    expect(onAction).toHaveBeenCalledWith('take:WHITE,BLUE,RED');
  });

  it('非玩家回合时动作按钮禁用', () => {
    render(
      <SplendorBoard
        view={view}
        mySeat={0}
        actions={[{ key: 'take:WHITE,BLUE,RED', label: '拿 白+蓝+红' }]}
        disabled
        onAction={() => undefined}
      />,
    );

    expect(screen.getByRole('button', { name: '拿 白+蓝+红' })).toBeDisabled();
  });
});
