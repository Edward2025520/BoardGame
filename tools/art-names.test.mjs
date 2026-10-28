// 命名与序号映射的自检：node tools/art-names.test.mjs
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import test from 'node:test';
import { cardArtName, isImageFile, nobleArtName, normalizeCost, normalizeExtension } from './art-names.mjs';
import { readSplendorData } from './splendor-data.mjs';

const ROOT = resolve(import.meta.dirname, '..');

test('发展卡标准名按 等级-颜色-分数-成本 生成', () => {
  assert.equal(
    cardArtName({ tier: 1, color: 'blue', points: 0, cost: [0, 0, 2, 0, 2] }),
    'card-1-blue-0-0-0-2-0-2',
  );
  assert.equal(
    cardArtName({ tier: 3, color: 3, points: 5, cost: [0, 0, 7, 3, 0] }),
    'card-3-red-5-0-0-7-3-0',
  );
});

test('成本支持对象写法', () => {
  assert.deepEqual(normalizeCost({ green: 2, black: 2 }), [0, 0, 2, 0, 2]);
  assert.equal(cardArtName({ tier: 1, color: 'green', points: 0, cost: { white: 1, red: 1 } }),
    'card-1-green-0-1-0-0-1-0');
});

test('贵族标准名按需求生成', () => {
  assert.equal(nobleArtName({ requirement: [0, 0, 4, 4, 0] }), 'noble-0-0-4-4-0');
  assert.equal(nobleArtName({ requirement: [3, 3, 3, 0, 0] }), 'noble-3-3-3-0-0');
});

test('扩展名与图片判断', () => {
  assert.equal(normalizeExtension('A.JPEG'), '.jpg');
  assert.equal(normalizeExtension('a.PNG'), '.png');
  assert.equal(isImageFile('x.webp'), true);
  assert.equal(isImageFile('x.txt'), false);
});

test('大小写不同的卡号生成不同文件名，不会在 Windows 上撞车', () => {
  const data = readSplendorData();
  const byCode = new Map(data.cards.map((card) => [card.code, card]));
  const lower = byCode.get('3b');
  const upper = byCode.get('3B');
  assert.ok(lower && upper, '数据里应同时存在 3b 与 3B');
  assert.notEqual(lower.artName, upper.artName);
});

test('序号映射齐全且不与数据冲突', () => {
  const index = JSON.parse(readFileSync(join(ROOT, 'tools', 'art-index.json'), 'utf8'));
  const data = readSplendorData();
  const standard = new Set([...data.cards, ...data.nobles].map((item) => item.artName));

  assert.equal(Object.keys(index.cards).length, 90);
  assert.equal(Object.keys(index.nobles).length, 10);

  for (const name of [...Object.values(index.cards), ...Object.values(index.nobles)]) {
    assert.ok(standard.has(name), `序号映射指向了不存在的标准名：${name}`);
  }

  const mapped = [...Object.values(index.cards), ...Object.values(index.nobles)];
  assert.equal(new Set(mapped).size, mapped.length, '映射不应重复指向同一张卡');
});

test('标准名全部唯一', () => {
  const data = readSplendorData();
  const names = [...data.cards, ...data.nobles].map((item) => item.artName);
  assert.equal(new Set(names).size, names.length);
  assert.equal(names.length, 100);
});
