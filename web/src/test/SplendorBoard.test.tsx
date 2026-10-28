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
      cards: [{ code: '2g2B', tier: 1, points: 0, bonus: 'blue', cost: { green: 2, black: 2 } }],
    },
    { tier: 2, deckSize: 26, cards: [] },
    { tier: 3, deckSize: 16, cards: [] },
  ],
  nobles: [{ code: '4g4r', requirement: { green: 4, red: 4 }, qualified: false }],
};

describe('SplendorBoard', () => {
  it('点击宝石会累积选中，凑成合法组合后可以确认', () => {
    const onAction = vi.fn();
    render(
      <SplendorBoard
        view={view}
        mySeat={0}
        actions={[{ key: 'take:WHITE,BLUE,RED', label: '拿 白+蓝+红' }]}
        disabled={false}
        onAction={onAction}
      />,
    );

    fireEvent.click(screen.getByTitle('白宝石，银行剩 4'));
    fireEvent.click(screen.getByTitle('蓝宝石，银行剩 4'));
    fireEvent.click(screen.getByTitle('红宝石，银行剩 4'));

    fireEvent.click(screen.getByRole('button', { name: '确认：拿 白+蓝+红' }));
    expect(onAction).toHaveBeenCalledWith('take:WHITE,BLUE,RED');
  });

  it('买不起时显示差额，购买按钮禁用', () => {
    render(
      <SplendorBoard
        view={view}
        mySeat={0}
        actions={[{ key: 'reserve:2g2B', label: '预留' }]}
        disabled={false}
        onAction={() => undefined}
      />,
    );

    expect(screen.getByText('缺 2绿 2黑')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '购买' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '预留' })).toBeEnabled();
  });

  it('非玩家回合时所有动作禁用', () => {
    render(
      <SplendorBoard
        view={view}
        mySeat={0}
        actions={[{ key: 'buy:2g2B', label: '购买' }]}
        disabled
        onAction={() => undefined}
      />,
    );

    expect(screen.getByRole('button', { name: '购买' })).toBeDisabled();
    expect(screen.getByText('等待 AI 行动…')).toBeInTheDocument();
  });
});
