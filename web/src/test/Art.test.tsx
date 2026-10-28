import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Art } from '../components/Art';

describe('Art', () => {
  it('官方图加载失败时退回自绘', () => {
    render(<Art game="splendor" name="c31" alt="卡牌" fallback={<span>自绘卡面</span>} />);

    const image = screen.getByAltText('卡牌');
    expect(screen.queryByText('自绘卡面')).toBeNull();

    fireEvent.error(image);

    expect(screen.getByText('自绘卡面')).toBeInTheDocument();
    expect(screen.queryByAltText('卡牌')).toBeNull();
  });
});
