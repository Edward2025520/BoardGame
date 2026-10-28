// 从公开实现抓取璀璨宝石的官方卡面，按我们的卡号重命名后放到 web/public/art-official/splendor/。
// 图片目录已 gitignore，只在本机使用；仓库里只有这个脚本。
// 用法：node tools/fetch-splendor-art.mjs
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';

const ROOT = resolve(import.meta.dirname, '..');
const DATA_FILE = join(ROOT, 'engine', 'src', 'main', 'java', 'boardgame', 'engine', 'games',
  'splendor', 'rules', 'SplendorData.java');
const TARGET = join(ROOT, 'web', 'public', 'art-official', 'splendor');

const DATA_URLS = [
  'https://cdn.jsdelivr.net/gh/zhefu3/shanzhai-splendor@main/src/engine/cards.ts',
  'https://raw.githubusercontent.com/zhefu3/shanzhai-splendor/main/src/engine/cards.ts',
];
const IMAGE_BASES = [
  'https://cdn.jsdelivr.net/gh/zhefu3/shanzhai-splendor@gh-pages/cards/',
  'https://raw.githubusercontent.com/zhefu3/shanzhai-splendor/gh-pages/cards/',
];

const COLOR_INDEX = ['white', 'blue', 'green', 'red', 'black'];

// 文件名不能用原始卡号：3b/3B、4w4b/4w4B 在 Windows 上会撞成同一个文件。
// 改成「等级-颜色-分数-成本向量」，大小写无关且唯一。
const cardArtName = (card) => `card-${card.tier}-${COLOR_INDEX[card.color]}-${card.points}-${card.cost.join('-')}`;
const nobleArtName = (noble) => `noble-${noble.requirement.join('-')}`;

/* ---------- 我们的数据 ---------- */

function readJavaBlocks() {
  const source = readFileSync(DATA_FILE, 'utf8');
  const block = (marker) => {
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
  };

  const costToVector = (raw) => {
    const vector = [0, 0, 0, 0, 0];
    if (raw) {
      for (const item of raw.split(',')) {
        const [gem, count] = item.split('=');
        const index = COLOR_INDEX.indexOf(gem);
        if (index < 0) {
          throw new Error(`未知颜色 ${gem}`);
        }
        vector[index] = Number(count);
      }
    }
    return vector;
  };

  const cards = block('RAW_CARDS').map((line) => {
    const [code, color, tier, points, cost] = line.split('|');
    const vector = costToVector(cost);
    return {
      code,
      color: COLOR_INDEX.indexOf(color),
      tier: Number(tier),
      points: Number(points),
      cost: vector,
      key: `${tier}|${COLOR_INDEX.indexOf(color)}|${points}|${vector.join(',')}`,
    };
  });

  const nobles = block('RAW_NOBLES').map((line) => {
    const [code, requirement] = line.split('|');
    const vector = costToVector(requirement);
    return { code, requirement: vector, key: vector.join(',') };
  });

  return { cards, nobles };
}

/* ---------- 参考实现的数据 ---------- */

async function readReferenceCards() {
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

  const cards = [];
  for (const match of source.matchAll(
    /\{\s*id:\s*(\d+),\s*tier:\s*(\d+),\s*color:\s*(\d+),\s*points:\s*(\d+),\s*cost:\s*\[([^\]]+)\]\s*\}/g,
  )) {
    const [, id, tier, color, points, cost] = match;
    cards.push({ id: Number(id), tier: Number(tier), color: Number(color), points: Number(points), cost: cost.split(',').map(Number) });
  }
  if (cards.length !== 90) {
    throw new Error(`参考数据里只有 ${cards.length} 张卡，期望 90`);
  }

  const nobles = [];
  for (const match of source.matchAll(
    /\{\s*id:\s*(\d+),\s*points:\s*(\d+),\s*requirement:\s*\[([^\]]+)\]\s*\}/g,
  )) {
    const [, id, points, requirement] = match;
    nobles.push({ id: Number(id), points: Number(points), requirement: requirement.split(',').map(Number) });
  }
  if (nobles.length !== 10) {
    throw new Error(`参考数据里只有 ${nobles.length} 张贵族，期望 10`);
  }

  return { cards, nobles };
}

/* ---------- 匹配 ---------- */

function buildCardMap(mine, reference) {
  const byKey = new Map(mine.map((card) => [`${card.tier}|${card.color}|${card.points}|${card.cost.join(',')}`, card]));
  const pairs = reference.map((card) => {
    const key = `${card.tier}|${card.color}|${card.points}|${card.cost.join(',')}`;
    const target = byKey.get(key);
    if (!target) {
      throw new Error(`参考卡 id=${card.id} 在我们的数据里找不到对应：${key}`);
    }
    byKey.delete(key);
    return { id: card.id, code: target.code };
  });
  if (byKey.size !== 0) {
    throw new Error(`还有 ${byKey.size} 张卡没有匹配上`);
  }
  return pairs;
}

function buildNobleMap(mine, reference) {
  const byKey = new Map(mine.map((noble) => [noble.key, noble]));
  const pairs = reference.map((noble) => {
    const key = noble.requirement.join(',');
    const target = byKey.get(key);
    if (!target) {
      throw new Error(`参考贵族 id=${noble.id} 找不到对应：${key}`);
    }
    byKey.delete(key);
    return { id: noble.id, code: target.code };
  });
  if (byKey.size !== 0) {
    throw new Error(`还有 ${byKey.size} 张贵族没有匹配上`);
  }
  return pairs;
}

/* ---------- 下载 ---------- */

const sleep = (ms) => new Promise((done) => setTimeout(done, ms));

async function download(path, target, attempt = 1) {
  for (const base of IMAGE_BASES) {
    try {
      const response = await fetch(base + path, { signal: AbortSignal.timeout(30000) });
      if (!response.ok) {
        continue;
      }
      const buffer = Buffer.from(await response.arrayBuffer());
      if (buffer.length < 1024 || buffer[0] !== 0x89 || buffer[1] !== 0x50) {
        continue;
      }
      mkdirSync(dirname(target), { recursive: true });
      writeFileSync(target, buffer);
      return buffer.length;
    } catch {
      // 换下一个镜像
    }
  }
  if (attempt >= 5) {
    throw new Error(`${path} 下载失败（重试 ${attempt} 次）`);
  }
  await sleep(500 * attempt);
  return download(path, target, attempt + 1);
}

function alreadyThere(target) {
  try {
    const head = readFileSync(target).subarray(0, 2);
    return head[0] === 0x89 && head[1] === 0x50;
  } catch {
    return false;
  }
}

/* ---------- 主流程 ---------- */

const mine = readJavaBlocks();
const reference = await readReferenceCards();
const cardPairs = buildCardMap(mine.cards, reference.cards);
const noblePairs = buildNobleMap(mine.nobles, reference.nobles);

let bytes = 0;
let done = 0;
for (const pair of cardPairs) {
  const target = join(TARGET, `${cardArtName(mine.cards.find((card) => card.code === pair.code))}.png`);
  if (!alreadyThere(target)) {
    bytes += await download(`c${pair.id}.png`, target);
  } else {
    bytes += readFileSync(target).length;
  }
  done++;
  process.stdout.write(`\r卡面 ${done}/${cardPairs.length}`);
}
process.stdout.write('\n');

for (const pair of noblePairs) {
  const target = join(TARGET, `${nobleArtName(mine.nobles.find((noble) => noble.code === pair.code))}.png`);
  if (!alreadyThere(target)) {
    bytes += await download(`n${pair.id}.png`, target);
  } else {
    bytes += readFileSync(target).length;
  }
}

const missing = mine.cards.filter((card) => !existsSync(join(TARGET, `${cardArtName(card)}.png`)));
const missingNobles = mine.nobles.filter((noble) => !existsSync(join(TARGET, `${nobleArtName(noble)}.png`)));

console.log(`完成：${cardPairs.length} 张卡面，${noblePairs.length} 张贵族，共 ${(bytes / 1024 / 1024).toFixed(1)} MB`);
console.log(`目录：${TARGET}`);
if (missing.length || missingNobles.length) {
  console.error(`缺失：${missing.length} 张卡，${missingNobles.length} 张贵族`);
  process.exit(1);
}
