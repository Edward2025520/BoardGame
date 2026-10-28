import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Art } from '../components/Art';

describe('Art', () => {
  it('png 失败后换 jpg，全都失败才退回自绘', () => {
    render(<Art game="splendor" name="c31" alt="卡牌" fallback={<span>自绘卡面</span>} />);

    expect(screen.getByAltText('卡牌').getAttribute('src')).toBe('/art-official/splendor/c31.png');

    fireEvent.error(screen.getByAltText('卡牌'));
    expect(screen.getByAltText('卡牌').getAttribute('src')).toBe('/art-official/splendor/c31.jpg');

    fireEvent.error(screen.getByAltText('卡牌'));
    expect(screen.getByAltText('卡牌').getAttribute('src')).toBe('/art-official/splendor/c31.webp');

    fireEvent.error(screen.getByAltText('卡牌'));
    expect(screen.getByText('自绘卡面')).toBeInTheDocument();
    expect(screen.queryByAltText('卡牌')).toBeNull();
  });
});
