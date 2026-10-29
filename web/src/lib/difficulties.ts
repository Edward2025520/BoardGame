import type { Difficulty } from '../api/types';

export const DIFFICULTY_OPTIONS: { value: Difficulty; label: string; hint: string }[] = [
  { value: 'BEGINNER', label: '一档 · 入门', hint: '纯贪心' },
  { value: 'CASUAL', label: '二档 · 简单', hint: '会失误' },
  { value: 'ADVANCED', label: '三档 · 进阶', hint: '少失误' },
  { value: 'MEDIUM', label: '四档 · 中等', hint: '约 1 秒' },
  { value: 'HARD', label: '五档 · 困难', hint: '约 2 秒' },
  { value: 'MASTER', label: '六档 · 大师', hint: '约 4 秒' },
  { value: 'HELL', label: '七档 · 地狱', hint: '约 8 秒' },
];

export function difficultyLabel(value: string): string {
  return DIFFICULTY_OPTIONS.find((option) => option.value === value)?.label ?? value;
}
