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

function check(name, html, expected) {
  const missing = expected.filter((text) => !html.includes(text));
  if (missing.length > 0) {
    console.error(`[失败] ${name} 缺少文案：${missing.join('、')}`);
    console.error(html.slice(0, 400));
    process.exitCode = 1;
    return;
  }
  console.log(`[通过] ${name}`);
}

const { Lobby } = require('../src/pages/Lobby.tsx');
check(
  '大厅',
  renderToStaticMarkup(React.createElement(MemoryRouter, null, React.createElement(Lobby))),
  ['桌游合集', 'AI 难度', '正在读取游戏列表'],
);

const { SplendorBoard } = require('../src/games/splendor/SplendorBoard.tsx');
const view = {
  playerCount: 2,
  currentSeat: 0,
  phase: 'PLAY',
  finalRound: true,
  bank: [4, 4, 4, 4, 4],
  goldBank: 5,
  reservedLimit: 3,
  seats: [
    {
      seat: 0,
      points: 0,
      cards: 0,
      tokens: [0, 0, 0, 0, 0, 0],
      bonuses: [0, 0, 0, 0, 0],
      reservedCount: 0,
      reserved: [],
      current: true,
    },
    {
      seat: 1,
      points: 0,
      cards: 0,
      tokens: [0, 0, 0, 0, 0, 0],
      bonuses: [0, 0, 0, 0, 0],
      reservedCount: 0,
      reserved: [],
      current: false,
    },
  ],
  rows: [
    { tier: 1, deckSize: 36, cards: [{ code: '2g2B', tier: 1, points: 0, bonus: 'blue', cost: { green: 2, black: 2 } }] },
    { tier: 2, deckSize: 26, cards: [] },
    { tier: 3, deckSize: 16, cards: [] },
  ],
  nobles: [{ code: '4g4r', requirement: { green: 4, red: 4 }, qualified: false }],
};

check(
  '璀璨宝石棋盘',
  renderToStaticMarkup(
    React.createElement(SplendorBoard, {
      view,
      mySeat: 0,
      actions: [
        { key: 'take:WHITE,BLUE,RED', label: '拿 白+蓝+红' },
        { key: 'buy:2g2B', label: '购买 1 级 蓝 卡' },
      ],
      disabled: false,
      onAction: () => undefined,
    }),
  ),
  ['购买', '预留', '点宝石取牌', '对手 2', '你', '缺 2绿 2黑'],
);

if (process.exitCode === 1) {
  console.error('渲染冒烟失败');
} else {
  console.log('渲染冒烟通过');
}
