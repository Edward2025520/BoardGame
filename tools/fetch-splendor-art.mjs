// 从公开镜像抓取璀璨宝石的官方卡面，按标准名落到 web/public/art-official/splendor/。
// 序号与标准名的对应见 tools/art-index.json；图片目录已 gitignore，仓库不包含任何图片。
// 用法：node tools/fetch-splendor-art.mjs
import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { join, resolve } from 'node:path';

const ROOT = resolve(import.meta.dirname, '..');
const TARGET = join(ROOT, 'web', 'public', 'art-official', 'splendor');
const INDEX = JSON.parse(readFileSync(join(ROOT, 'tools', 'art-index.json'), 'utf8'));

const MIRRORS = [
  'https://cdn.jsdelivr.net/gh/zhefu3/shanzhai-splendor@gh-pages/cards/',
  'https://raw.githubusercontent.com/zhefu3/shanzhai-splendor/gh-pages/cards/',
];

const sleep = (ms) => new Promise((done) => setTimeout(done, ms));

function isPng(path) {
  try {
    const head = readFileSync(path).subarray(0, 2);
    return head[0] === 0x89 && head[1] === 0x50;
  } catch {
    return false;
  }
}

async function download(sequence, target, attempt = 1) {
  for (const mirror of MIRRORS) {
    try {
      const response = await fetch(`${mirror}${sequence}.png`, { signal: AbortSignal.timeout(30000) });
      if (!response.ok) {
        continue;
      }
      const buffer = Buffer.from(await response.arrayBuffer());
      if (buffer.length < 1024 || buffer[0] !== 0x89 || buffer[1] !== 0x50) {
        continue;
      }
      mkdirSync(TARGET, { recursive: true });
      writeFileSync(target, buffer);
      return buffer.length;
    } catch {
      // 换下一个镜像
    }
  }
  if (attempt >= 5) {
    throw new Error(`${sequence}.png 下载失败（重试 ${attempt} 次）`);
  }
  await sleep(500 * attempt);
  return download(sequence, target, attempt + 1);
}

const entries = [
  ...Object.entries(INDEX.cards).map(([sequence, name]) => ({ sequence, name })),
  ...Object.entries(INDEX.nobles).map(([sequence, name]) => ({ sequence, name })),
];

if (entries.length !== 100) {
  throw new Error(`映射表数量不对：${entries.length}，期望 100`);
}

let bytes = 0;
let done = 0;
for (const entry of entries) {
  const target = join(TARGET, `${entry.name}.png`);
  if (isPng(target)) {
    bytes += readFileSync(target).length;
  } else {
    bytes += await download(entry.sequence, target);
  }
  done++;
  process.stdout.write(`\r卡面 ${done}/${entries.length}`);
}
process.stdout.write('\n');

const present = new Set(
  existsSync(TARGET)
    ? readdirSync(TARGET).map((file) => file.replace(/\.(png|jpe?g|webp)$/i, ''))
    : [],
);
const missing = entries.filter((entry) => !present.has(entry.name));

console.log(`完成：${entries.length - missing.length} 张，共 ${(bytes / 1024 / 1024).toFixed(1)} MB`);
console.log(`目录：${TARGET}`);
if (missing.length > 0) {
  console.error(`缺失 ${missing.length} 张，例如：${missing.slice(0, 6).map((entry) => entry.name).join(', ')}`);
  process.exit(1);
}
