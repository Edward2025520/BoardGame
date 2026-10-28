// 把本地准备好的卡面导入游戏目录，支持三种命名：
//   1. 标准名  card-1-blue-0-0-0-0-2-1.png / noble-0-0-4-4-0.png
//   2. 卡号    3g2B.png / 4g4r.png（大小写不敏感，冲突时按原始大小写优先）
//   3. 序号    c0..c89.png / n0..n9.png（映射见 tools/art-index.json）
// 输出到 web/public/art-official/splendor/（已 gitignore，不进仓库）。
//
// 用法：
//   node tools/import-card-art.mjs "D:/我的卡面"
//   node tools/import-card-art.mjs "D:/我的卡面" --check
import { copyFileSync, existsSync, mkdirSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { basename, join, resolve } from 'node:path';
import { isImageFile, normalizeExtension } from './art-names.mjs';
import { readSplendorData } from './splendor-data.mjs';

const args = process.argv.slice(2);
const checkOnly = args.includes('--check');
const sourceArg = args.find((arg) => !arg.startsWith('--'));

if (!sourceArg) {
  console.error('用法：node tools/import-card-art.mjs <图片目录> [--check]');
  process.exit(1);
}

const sourceDir = resolve(sourceArg);
if (!existsSync(sourceDir) || !statSync(sourceDir).isDirectory()) {
  console.error(`目录不存在：${sourceDir}`);
  process.exit(1);
}

const ROOT = resolve(import.meta.dirname, '..');
const TARGET = join(ROOT, 'web', 'public', 'art-official', 'splendor');
const INDEX = JSON.parse(readFileSync(join(ROOT, 'tools', 'art-index.json'), 'utf8'));

const data = readSplendorData();
const standard = new Set([...data.cards, ...data.nobles].map((item) => item.artName));

const codeMap = new Map();
const lowerCodeMap = new Map();
for (const item of [...data.cards, ...data.nobles]) {
  codeMap.set(item.code, item.artName);
  const lower = item.code.toLowerCase();
  lowerCodeMap.set(lower, [...(lowerCodeMap.get(lower) ?? []), item.artName]);
}

const sequenceMap = new Map([...Object.entries(INDEX.cards), ...Object.entries(INDEX.nobles)]);

function imageKind(buffer) {
  if (buffer.length >= 8 && buffer[0] === 0x89 && buffer[1] === 0x50) {
    return 'png';
  }
  if (buffer.length >= 3 && buffer[0] === 0xff && buffer[1] === 0xd8) {
    return 'jpg';
  }
  if (buffer.length >= 12 && buffer.toString('ascii', 0, 4) === 'RIFF' && buffer.toString('ascii', 8, 12) === 'WEBP') {
    return 'webp';
  }
  return null;
}

function classify(stem) {
  if (standard.has(stem)) {
    return { artName: stem, how: '标准名' };
  }
  if (codeMap.has(stem)) {
    return { artName: codeMap.get(stem), how: '卡号' };
  }
  const lowerMatches = lowerCodeMap.get(stem.toLowerCase());
  if (lowerMatches && lowerMatches.length === 1) {
    return { artName: lowerMatches[0], how: '卡号（忽略大小写）' };
  }
  if (lowerMatches && lowerMatches.length > 1) {
    return { error: `卡号大小写有歧义：${lowerMatches.length} 张卡都可能` };
  }
  const sequence = sequenceMap.get(stem.toLowerCase());
  if (sequence) {
    return { artName: sequence, how: '序号' };
  }
  return { error: '文件名不认识（既不是标准名，也不是卡号或序号）' };
}

const files = readdirSync(sourceDir)
  .filter((file) => isImageFile(file))
  .sort();

if (files.length === 0) {
  console.error(`目录里没有图片：${sourceDir}`);
  process.exit(1);
}

const imported = [];
const replaced = [];
const unknown = [];
const broken = [];

const PRIORITY = { '标准名': 0, '卡号': 1, '卡号（忽略大小写）': 2, '序号': 3 };

const candidates = [];
for (const file of files) {
  const stem = basename(file, normalizeExtension(file)).replace(/\.(png|jpg|jpeg|webp)$/i, '');
  const result = classify(stem);
  if (result.error) {
    unknown.push({ file, reason: result.error });
    continue;
  }
  candidates.push({ file, artName: result.artName, how: result.how });
}

// 同一张卡可能有多种命名，优先用标准名，其次卡号，最后序号
candidates.sort((left, right) => (PRIORITY[left.how] ?? 9) - (PRIORITY[right.how] ?? 9));

const chosen = new Map();
for (const candidate of candidates) {
  if (chosen.has(candidate.artName)) {
    unknown.push({
      file: candidate.file,
      reason: `与 ${chosen.get(candidate.artName).file} 指向同一张卡，已跳过`,
    });
    continue;
  }
  const extension = normalizeExtension(candidate.file);
  const buffer = readFileSync(join(sourceDir, candidate.file));
  if (!imageKind(buffer)) {
    broken.push({ file: candidate.file, reason: '不是有效的 PNG/JPEG/WebP' });
    continue;
  }
  chosen.set(candidate.artName, candidate);

  const targetName = `${candidate.artName}${extension}`;
  const targetPath = join(TARGET, targetName);
  if (existsSync(targetPath)) {
    replaced.push(targetName);
  }
  if (!checkOnly) {
    mkdirSync(TARGET, { recursive: true });
    copyFileSync(join(sourceDir, candidate.file), targetPath);
  }
  imported.push({ file: candidate.file, targetName, how: candidate.how });
}

const existing = existsSync(TARGET)
  ? new Set(readdirSync(TARGET).map((file) => file.replace(/\.(png|jpg|jpeg|webp)$/i, '')))
  : new Set();
const missing = [...standard].filter((name) => !existing.has(name) && !chosen.has(name));

console.log(`${checkOnly ? '检查' : '导入'}完成：${imported.length} 张${checkOnly ? '可导入' : '已导入'}`);
if (replaced.length > 0 && !checkOnly) {
  console.log(`覆盖同名文件：${replaced.length} 张`);
}
if (unknown.length > 0) {
  console.log(`未识别 ${unknown.length} 个：`);
  unknown.slice(0, 12).forEach((item) => console.log(`  - ${item.file}：${item.reason}`));
}
if (broken.length > 0) {
  console.log(`文件损坏 ${broken.length} 个：`);
  broken.slice(0, 12).forEach((item) => console.log(`  - ${item.file}：${item.reason}`));
}
console.log(missing.length === 0 ? '全套齐全' : `还缺 ${missing.length} 张，例如：${missing.slice(0, 6).join(', ')}`);
if (!checkOnly) {
  console.log(`输出目录：${TARGET}`);
}

if (!checkOnly && imported.length === 0) {
  process.exit(2);
}
