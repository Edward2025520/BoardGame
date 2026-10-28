import type { ComponentType } from 'react';
import type { ActionOption, LogItem } from '../api/types';
import { SplendorBoard } from './splendor/SplendorBoard';

export interface BoardProps {
  view: unknown;
  mySeat: number;
  actions: ActionOption[];
  log: LogItem[];
  disabled: boolean;
  onAction: (key: string) => void;
}

/**
 * 游戏 id → 棋盘组件。加新游戏时在这里注册一行，后端返回的 boards 会被原样传进去。
 */
export const GAME_BOARDS: Record<string, ComponentType<BoardProps>> = {
  splendor: SplendorBoard as ComponentType<BoardProps>,
};

export function boardFor(gameId: string): ComponentType<BoardProps> | undefined {
  return GAME_BOARDS[gameId];
}
