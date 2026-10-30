// 无浏览器环境下的渲染冒烟测试：把 TS/TSX 就地编译成 CommonJS 后直接服务端渲染，
// 用来确认组件树能跑通、关键文案能出来。用法：node scripts/ssr-smoke.cjs
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

const React = require('react');
const { renderToStaticMarkup } = require('react-dom/server');
const { MemoryRouter } = require('react-router-dom');

let failed = 0;

function check(name, html, expected) {
  const missing = expected.filter((text) => !html.includes(text));
  if (missing.length > 0) {
    failed++;
    console.error(`[失败] ${name} 缺少文案：${missing.join('、')}`);
    console.error(html.slice(0, 400));
    return;
  }
  console.log(`[通过] ${name}`);
}

function checkOrder(name, html, before, after) {
  const from = html.indexOf(before);
  const to = html.indexOf(after);
  if (from >= 0 && to >= 0 && from < to) {
    console.log(`[通过] ${name}`);
    return;
  }
  failed++;
  console.error(`[失败] ${name}：${before} 应该出现在 ${after} 之前`);
}

const { Lobby } = require('../src/pages/Lobby.tsx');
check(
  '大厅',
  renderToStaticMarkup(React.createElement(MemoryRouter, null, React.createElement(Lobby))),
  ['桌游合集', '人数', '4 人', '你 + 3 个 AI', 'AI 难度', '一档 · 入门', '七档 · 地狱', '正在读取游戏列表'],
);

const { SplendorBoard } = require('../src/games/splendor/SplendorBoard.tsx');

function seat(index, current, reserved = [], tokens = [0, 0, 0, 0, 0, 0]) {
  return {
    seat: index,
    points: 0,
    cards: 0,
    tokens,
    bonuses: [0, 0, 0, 0, 0],
    reservedCount: reserved.length,
    reserved,
    purchased: [],
    current,
  };
}

const reservedCard = { code: '3r1B1g', tier: 1, points: 0, bonus: 'red', cost: { red: 3, black: 1, green: 1 } };
const reservedAffordable = { code: '4g', tier: 1, points: 1, bonus: 'green', cost: { green: 4 } };
const reservedShort = { code: '2g2B', tier: 2, points: 0, bonus: 'blue', cost: { green: 2, black: 2 } };

const view = {
  playerCount: 4,
  currentSeat: 0,
  phase: 'PLAY',
  finalRound: true,
  bank: [4, 4, 4, 4, 4],
  goldBank: 5,
  reservedLimit: 3,
  seats: [
    seat(0, true, [reservedAffordable, reservedShort], [0, 0, 4, 0, 0, 0]),
    seat(1, false, [reservedCard]),
    seat(2, false),
    seat(3, false),
  ],
  rows: [
    {
      tier: 1,
      deckSize: 36,
      cards: [
        { code: '2g2B', tier: 1, points: 0, bonus: 'blue', cost: { green: 2, black: 2 } },
        { code: '4g', tier: 1, points: 1, bonus: 'green', cost: { green: 4 } },
      ],
    },
    { tier: 2, deckSize: 26, cards: [] },
    { tier: 3, deckSize: 16, cards: [] },
  ],
  nobles: [{ code: '4g4r', requirement: { green: 4, red: 4 }, qualified: false }],
};

const boardHtml = renderToStaticMarkup(
  React.createElement(SplendorBoard, {
    view,
    mySeat: 0,
    actions: [
      { key: 'take:WHITE,BLUE,RED', label: '拿 白+蓝+红' },
      { key: 'buy:2g2B', label: '购买 1 级 蓝 卡' },
    ],
    log: [{ version: 1, seat: 0, text: '拿了 白+蓝+红 宝石', at: 1700000000000 }],
    disabled: false,
    onAction: () => undefined,
  }),
);

check('四人牌桌', boardHtml, [
  '购买',
  '预留',
  '点宝石取牌',
  'AI 1',
  'AI 2',
  'AI 3',
  '对局记录',
  '拿了 白+蓝+红 宝石',
  '缺口',
  '成本',
  '折扣',
  '实付',
  '已购 0 张',
  'buy-reserved',
  '实付 4绿',
  '还缺 2黑',
  '隐藏预留',
  '预留：1 级 red 卡',
]);
checkOrder('三行牌 3→2→1', boardHtml, '3 级', '1 级');
check('右栏是独立容器', boardHtml, ['board-rail']);
check('玩家信息卡', boardHtml, ['player-stats', '宝石', '折扣', 'stat-pip']);

const { PaymentDialog } = require('../src/games/splendor/parts.tsx');
const dialogHtml = renderToStaticMarkup(
  React.createElement(PaymentDialog, {
    card: reservedAffordable,
    seat: { ...view.seats[0], tokens: [0, 0, 2, 0, 0, 1] },
    onConfirm: () => undefined,
    onCancel: () => undefined,
  }),
);
check('支付面板', dialogHtml, [
  '支付 1 级 绿 卡',
  '需要 4',
  '宝石 2',
  '黄金',
  '黄金 2 / 1',
  '自动',
  '确认支付',
  '取消',
]);
check('支付面板有加减按钮', dialogHtml, ['−', '+']);

// 花砖物语
const { AzulBoard } = require('../src/games/azul/AzulBoard.tsx');

function azulSeat(index) {
  return {
    seat: index,
    score: 0,
    lines: [1, 2, 3, 4, 5].map((capacity) => ({ color: null, count: 0, capacity })),
    wall: Array.from({ length: 5 }, () =>
      Array.from({ length: 5 }, () => ({ color: 'blue', filled: false })),
    ),
    floor: [],
    hasMarker: false,
    current: index === 0,
    first: index === 0,
  };
}

const azulView = {
  playerCount: 4,
  currentSeat: 0,
  round: 1,
  phase: 'TAKING',
  bagCount: 80,
  factories: [1, 2, 3, 4, 5].map((index) => ({
    index,
    tiles: ['blue', 'blue', 'yellow', 'red'],
  })),
  center: { tiles: ['white', 'black'], hasMarker: true },
  seats: [azulSeat(0), azulSeat(1), azulSeat(2), azulSeat(3)],
};

const azulHtml = renderToStaticMarkup(
  React.createElement(AzulBoard, {
    view: azulView,
    mySeat: 0,
    actions: [{ key: 'take:1:BLUE:2', label: '取 工厂 1 的 2 枚蓝 → 第 3 行' }],
    log: [{ version: 1, seat: 0, text: '取 蓝 ×2 → 第 3 行', at: 1700000000000 }],
    disabled: false,
    onAction: () => undefined,
  }),
);

check('花砖物语牌桌', azulHtml, [
  '中央',
  '你',
  'AI 1',
  'AI 2',
  'AI 3',
  '地板',
  '点工厂或中央的一色瓦片',
  '取 蓝 ×2 → 第 3 行',
]);

if (failed > 0) {
  console.error(`渲染冒烟失败：${failed} 项`);
  process.exit(1);
}
console.log('渲染冒烟通过');
