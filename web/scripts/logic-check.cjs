// 无浏览器环境下的交互逻辑校验：把 TS 就地编译成 CommonJS 后直接断言纯函数。
// 用法：node scripts/logic-check.cjs
const fs = require('node:fs');
const ts = require('typescript');

for (const extension of ['.ts', '.tsx']) {
  require.extensions[extension] = function load(module, filename) {
    const source = fs.readFileSync(filename, 'utf8');
    const { outputText } = ts.transpileModule(source, {
      fileName: filename,
      compilerOptions: {
        module: ts.ModuleKind.CommonJS,
        target: ts.ScriptTarget.ES2022,
        jsx: ts.JsxEmit.ReactJSX,
        esModuleInterop: true,
      },
    });
    module._compile(outputText, filename);
  };
}

const {
  actionKeyForGems,
  toggleGem,
  selectionAction,
  cardShortfall,
  goldCovers,
  shortfallText,
  emptySelection,
} = require('../src/games/splendor/selection.ts');

let failed = 0;
function check(name, condition, detail) {
  if (condition) {
    console.log(`[通过] ${name}`);
  } else {
    failed++;
    console.error(`[失败] ${name}${detail ? `：${JSON.stringify(detail)}` : ''}`);
  }
}

// 1. key 排序按 Gem 枚举顺序，不是字母序
check(
  'key 按枚举顺序排序',
  actionKeyForGems('take', ['red', 'white']) === 'take:WHITE,RED',
  actionKeyForGems('take', ['red', 'white']),
);
check(
  '黑色排在蓝色之后',
  actionKeyForGems('take', ['black', 'blue']) === 'take:BLUE,BLACK',
  actionKeyForGems('take', ['black', 'blue']),
);

// 2. 选两个同色
let state = emptySelection;
state = toggleGem(state, 'white', { discarding: false, maxDiscard: 0 }).selection;
check('点一次选一个', state.gems.join(',') === 'white', state);
state = toggleGem(state, 'white', { discarding: false, maxDiscard: 0 }).selection;
check('同色点两次变成两个', state.gems.join(',') === 'white,white', state);
const blocked = toggleGem(state, 'blue', { discarding: false, maxDiscard: 0 });
check('两个同色后不能加别的色', blocked.selection.gems.length === 2 && Boolean(blocked.hint), blocked);
state = toggleGem(state, 'white', { discarding: false, maxDiscard: 0 }).selection;
check('同色点第三次清空', state.gems.length === 0, state);

// 3. 选三个不同色
state = emptySelection;
for (const gem of ['white', 'blue', 'green']) {
  state = toggleGem(state, gem, { discarding: false, maxDiscard: 0 }).selection;
}
check('可以选三个不同色', state.gems.length === 3, state);
const fourth = toggleGem(state, 'red', { discarding: false, maxDiscard: 0 });
check('第四个被拒绝', fourth.selection.gems.length === 3 && Boolean(fourth.hint), fourth);

// 4. 混搭被拒绝
state = emptySelection;
state = toggleGem(state, 'white', { discarding: false, maxDiscard: 0 }).selection;
state = toggleGem(state, 'blue', { discarding: false, maxDiscard: 0 }).selection;
const mixed = toggleGem(state, 'white', { discarding: false, maxDiscard: 0 });
check('混搭同色加异色被拒绝', mixed.selection.gems.length === 2 && Boolean(mixed.hint), mixed);

// 5. 黄金不可直接拿
const gold = toggleGem(emptySelection, 'gold', { discarding: false, maxDiscard: 0 });
check('黄金不可直接取', gold.selection.gems.length === 0 && Boolean(gold.hint), gold);

// 6. 弃牌上限
let discard = emptySelection;
discard = toggleGem(discard, 'white', { discarding: true, maxDiscard: 2 }).selection;
discard = toggleGem(discard, 'blue', { discarding: true, maxDiscard: 2 }).selection;
const overflow = toggleGem(discard, 'green', { discarding: true, maxDiscard: 2 });
check('弃牌超过上限被拒绝', overflow.selection.gems.length === 2 && Boolean(overflow.hint), overflow);
discard = toggleGem(discard, 'white', { discarding: true, maxDiscard: 2 }).selection;
check('弃牌可以取消单个', discard.gems.join(',') === 'blue', discard);

// 7. 差额计算
const card = { code: 'x', tier: 1, points: 0, bonus: 'blue', cost: { green: 3, black: 2 } };
const seat = {
  seat: 0,
  points: 0,
  cards: 0,
  tokens: [0, 0, 1, 0, 1, 0],
  bonuses: [0, 0, 1, 0, 0],
  reservedCount: 0,
  reserved: [],
  current: true,
};
const shortfall = cardShortfall(card, seat);
check(
  '差额按颜色计算',
  shortfallText(shortfall) === '缺 1绿 1黑',
  shortfallText(shortfall),
);
check('黄金不足时无法覆盖', goldCovers(shortfall, seat) === false);
const rich = { ...seat, tokens: [0, 0, 1, 0, 1, 2] };
check('黄金足够时覆盖', goldCovers(shortfall, rich) === true);
check('免费卡显示免费', shortfallText(cardShortfall({ ...card, cost: {} }, seat)) === '免费');

// 8. 选中组合能命中后端动作
const actions = [
  { key: 'take:WHITE,BLUE,GREEN', label: '拿 白+蓝+绿' },
  { key: 'discard:WHITE,BLUE', label: '弃 白+蓝' },
];
check(
  '选中三个不同色命中动作',
  selectionAction({ gems: ['green', 'white', 'blue'] }, { discarding: false }, actions)?.key ===
    'take:WHITE,BLUE,GREEN',
);
check(
  '弃牌组合命中弃牌动作',
  selectionAction({ gems: ['blue', 'white'] }, { discarding: true }, actions)?.key ===
    'discard:WHITE,BLUE',
);
check(
  '不存在组合返回空',
  selectionAction({ gems: ['red', 'black'] }, { discarding: false }, actions) === undefined,
);

// 9. 花砖物语：取砖选择
const azul = require('../src/games/azul/selection.ts');
check(
  '瓦片按颜色分组且顺序固定',
  JSON.stringify(azul.groupTiles(['red', 'blue', 'blue', 'red'])) ===
    JSON.stringify([
      { color: 'blue', count: 2 },
      { color: 'red', count: 2 },
    ]),
  azul.groupTiles(['red', 'blue', 'blue', 'red']),
);
check('取砖 key 格式正确', azul.takeKey(-1, 'blue', 3) === 'take:-1:BLUE:3', azul.takeKey(-1, 'blue', 3));
check(
  '可用目标行从动作里解析',
  JSON.stringify(
    azul.availableTargets(
      [
        { key: 'take:2:BLUE:0', label: '' },
        { key: 'take:2:BLUE:-1', label: '' },
        { key: 'take:2:RED:1', label: '' },
      ],
      2,
      'blue',
    ),
  ) === JSON.stringify([0, -1]),
);
const emptyLine = { color: null, count: 0, capacity: 5 };
check(
  '空行全部放得下',
  JSON.stringify(azul.ghostPlacement(emptyLine, 'blue', 3)) ===
    JSON.stringify({ placed: 3, overflow: 0, lineColor: 'blue' }),
);
check(
  '半满行只放剩余容量',
  JSON.stringify(azul.ghostPlacement({ color: 'blue', count: 2, capacity: 3 }, 'blue', 3)) ===
    JSON.stringify({ placed: 1, overflow: 2, lineColor: 'blue' }),
);
check(
  '异色行一块也放不下',
  JSON.stringify(azul.ghostPlacement({ color: 'red', count: 1, capacity: 3 }, 'blue', 2)) ===
    JSON.stringify({ placed: 0, overflow: 2, lineColor: 'red' }),
);

// 10. 七档难度的显示名
const { difficultyLabel, DIFFICULTY_OPTIONS } = require('../src/lib/difficulties.ts');
check('七档数量正确', DIFFICULTY_OPTIONS.length === 7, DIFFICULTY_OPTIONS.length);
check('七档显示名', difficultyLabel('HELL') === '七档 · 地狱', difficultyLabel('HELL'));
check('未知难度回退原值', difficultyLabel('???') === '???', difficultyLabel('???'));

// 11. 音量设置
const { clampVolume, DEFAULT_VOLUME, loadVolume } = require('../src/lib/audio-settings.ts');
check('音量默认 70', DEFAULT_VOLUME === 70, DEFAULT_VOLUME);
check('音量钳制下限', clampVolume(-10) === 0, clampVolume(-10));
check('音量钳制上限', clampVolume(150) === 100, clampVolume(150));
check('音量四舍五入', clampVolume(33.6) === 34, clampVolume(33.6));
check('没有 localStorage 时用默认值', loadVolume() === DEFAULT_VOLUME, loadVolume());

if (failed > 0) {
  console.error(`交互逻辑校验失败：${failed} 项`);
  process.exit(1);
}
console.log('交互逻辑校验通过');
