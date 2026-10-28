// 把本地下载的官方卡面按卡号放进 web/public/art-official/<game>/。
// 用法：node scripts/import-art.mjs <图片目录> [--game splendor]
// 支持 png / jpg / jpeg / webp，文件名（不含扩展名）要与卡号一致，例如 3g2B.png、noble-4g4r.jpg。
import { copyFileSync, existsSync, mkdirSync, readFileSync, readdirSync } from 'node:fs';
import { basename, extname, join, resolve } from 'node:path';

const args = process.argv.slice(2);
if (args.length === 0) {
  console.error('用法：node scripts/import-art.mjs <图片目录> [--game splendor]');
  process.exit(1);
}

const sourceDir = resolve(args[0]);
const gameIndex = args.indexOf('--game');
const game = gameIndex >= 0 ? args[gameIndex + 1] : 'splendor';

if (!existsSync(sourceDir)) {
  console.error(`目录不存在：${sourceDir}`);
  process.exit(1);
}

const root = resolve(import.meta.dirname, '..');
const targetDir = join(root, 'public', 'art-official', game);

function knownNames() {
  if (game !== 'splendor') {
    return [];
  }
  const dataFile = resolve(root, '..', 'engine', 'src', 'main', 'java', 'boardgame', 'engine',
    'games', 'splendor', 'rules', 'SplendorData.java');
  const source = readFileSync(dataFile, 'utf8');
  const names = new Set();

  const grab = (marker) => {
    const start = source.indexOf(marker);
    if (start < 0) {
      return [];
    }
    const blockStart = source.indexOf('"""', start) + 3;
    const blockEnd = source.indexOf('"""', blockStart);
    return source
      .slice(blockStart, blockEnd)
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => line.includes('|'))
      .map((line) => line.split('|')[0].trim());
  };

  grab('RAW_CARDS').forEach((code) => names.add(code));
  grab('RAW_NOBLES').forEach((code) => names.add(`noble-${code}`));
  return [...names];
}

const valid = knownNames();
const validLower = new Map(valid.map((name) => [name.toLowerCase(), name]));
const imageExtensions = new Set(['.png', '.jpg', '.jpeg', '.webp']);

const files = readdirSync(sourceDir).filter((file) => imageExtensions.has(extname(file).toLowerCase()));
if (files.length === 0) {
  console.error(`目录里没有图片：${sourceDir}`);
  process.exit(1);
}

mkdirSync(targetDir, { recursive: true });

const imported = [];
const unknown = [];
for (const file of files) {
  const stem = basename(file, extname(file));
  const match = valid.includes(stem) ? stem : validLower.get(stem.toLowerCase());
  if (!match) {
    unknown.push(file);
    continue;
  }
  const extension = extname(file).toLowerCase() === '.jpeg' ? '.jpg' : extname(file).toLowerCase();
  copyFileSync(join(sourceDir, file), join(targetDir, `${match}${extension}`));
  imported.push(`${match}${extension}`);
}

const missing = valid.filter((name) => !imported.some((file) => file.startsWith(`${name}.`)));

console.log(`已导入 ${imported.length} 张到 ${targetDir}`);
if (unknown.length > 0) {
  console.log(`文件名对不上的 ${unknown.length} 张（需要按卡号命名）：`);
  console.log(unknown.slice(0, 12).map((file) => `  - ${file}`).join('\n'));
}
if (missing.length > 0) {
  console.log(`还缺 ${missing.length} 张，例如：${missing.slice(0, 8).join(', ')}`);
}
if (imported.length === 0) {
  process.exit(2);
}
