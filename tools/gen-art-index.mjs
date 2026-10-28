// 生成序号映射：公开实现用 c0..c89 / n0..n9 命名，这里换成我们的标准名。
// 只做「数据 → 文件名的对应」，仓库不包含任何图片。
// 用法：node tools/gen-art-index.mjs
import { writeFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { COLORS } from './art-names.mjs';
import { readSplendorData } from './splendor-data.mjs';

const OUT = join(resolve(import.meta.dirname, '..'), 'tools', 'art-index.json');
const DATA_URLS = [
  'https://cdn.jsdelivr.net/gh/zhefu3/shanzhai-splendor@main/src/engine/cards.ts',
  'https://raw.githubusercontent.com/zhefu3/shanzhai-splendor/main/src/engine/cards.ts',
];

async function readReference() {
  let source = null;
  let lastError = null;
  for (const url of DATA_URLS) {
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(20000) });
      if (response.ok) {
        source = await response.text();
        break;
      }
      lastError = new Error(`${url} -> ${response.status}`);
    } catch (error) {
      lastError = error;
    }
  }
  if (!source) {
    throw new Error(`拉取参考数据失败：${lastError?.message}`);
  }

  const cards = [...source.matchAll(
    /\{\s*id:\s*(\d+),\s*tier:\s*(\d+),\s*color:\s*(\d+),\s*points:\s*(\d+),\s*cost:\s*\[([^\]]+)\]\s*\}/g,
  )].map((match) => ({
    id: Number(match[1]),
    key: `${match[2]}|${match[3]}|${match[4]}|${match[5].split(',').map(Number).join(',')}`,
  }));

  const nobles = [...source.matchAll(
    /\{\s*id:\s*(\d+),\s*points:\s*(\d+),\s*requirement:\s*\[([^\]]+)\]\s*\}/g,
  )].map((match) => ({
    id: Number(match[1]),
    key: match[3].split(',').map(Number).join(','),
  }));

  if (cards.length !== 90 || nobles.length !== 10) {
    throw new Error(`参考数据数量不对：${cards.length} 张卡、${nobles.length} 张贵族`);
  }
  return { cards, nobles };
}

const ours = readSplendorData();
const reference = await readReference();

const cardByKey = new Map(
  ours.cards.map((card) => [`${card.tier}|${COLORS.indexOf(card.color)}|${card.points}|${card.cost.join(',')}`, card]),
);
const nobleByKey = new Map(ours.nobles.map((noble) => [noble.requirement.join(','), noble]));

const index = { cards: {}, nobles: {} };
for (const card of reference.cards) {
  const match = cardByKey.get(card.key);
  if (!match) {
    throw new Error(`参考卡 id=${card.id} 找不到对应：${card.key}`);
  }
  index.cards[`c${card.id}`] = match.artName;
  cardByKey.delete(card.key);
}
for (const noble of reference.nobles) {
  const match = nobleByKey.get(noble.key);
  if (!match) {
    throw new Error(`参考贵族 id=${noble.id} 找不到对应：${noble.key}`);
  }
  index.nobles[`n${noble.id}`] = match.artName;
  nobleByKey.delete(noble.key);
}
if (cardByKey.size || nobleByKey.size) {
  throw new Error(`还有 ${cardByKey.size} 张卡、${nobleByKey.size} 张贵族没有匹配`);
}

const payload = {
  source: 'zhefu3/shanzhai-splendor 的 cards.ts 序号顺序，仅做文件名映射',
  cards: index.cards,
  nobles: index.nobles,
};
writeFileSync(OUT, `${JSON.stringify(payload, null, 2)}\n`, 'utf8');
console.log(`写入 ${OUT}：${Object.keys(index.cards).length} 张卡、${Object.keys(index.nobles).length} 张贵族`);
