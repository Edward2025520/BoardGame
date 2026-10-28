// 读 SplendorData.java 里的卡表与贵族表，供脚本生成文件名、映射与导入。
import { readFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { COLORS, cardArtName, nobleArtName } from './art-names.mjs';

const ROOT = resolve(import.meta.dirname, '..');
export const DATA_FILE = join(ROOT, 'engine', 'src', 'main', 'java', 'boardgame', 'engine', 'games',
  'splendor', 'rules', 'SplendorData.java');

function parseCost(raw) {
  const vector = [0, 0, 0, 0, 0];
  for (const item of (raw ?? '').split(',')) {
    if (!item) {
      continue;
    }
    const [gem, amount] = item.split('=');
    const index = COLORS.indexOf(gem);
    if (index < 0) {
      throw new Error(`未知颜色：${gem}`);
    }
    vector[index] = Number(amount);
  }
  return vector;
}

function readBlock(source, marker) {
  const start = source.indexOf(marker);
  if (start < 0) {
    throw new Error(`SplendorData.java 里找不到 ${marker}`);
  }
  const from = source.indexOf('"""', start) + 3;
  const to = source.indexOf('"""', from);
  return source
    .slice(from, to)
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.includes('|'));
}

export function readSplendorData() {
  const source = readFileSync(DATA_FILE, 'utf8');

  const cards = readBlock(source, 'RAW_CARDS').map((line) => {
    const [code, color, tier, points, rawCost] = line.split('|');
    const cost = parseCost(rawCost);
    const meta = { tier: Number(tier), color, points: Number(points), cost };
    return { ...meta, code, artName: cardArtName(meta) };
  });

  const nobles = readBlock(source, 'RAW_NOBLES').map((line) => {
    const [code, rawRequirement] = line.split('|');
    const requirement = parseCost(rawRequirement);
    const meta = { requirement };
    return { ...meta, code, artName: nobleArtName(meta) };
  });

  if (cards.length !== 90 || nobles.length !== 10) {
    throw new Error(`卡表数量异常：${cards.length} 张卡、${nobles.length} 张贵族`);
  }
  return { cards, nobles };
}
