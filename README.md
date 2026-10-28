# 桌游合集（BoardGame）

一个能在电脑上单人玩的德式桌游合集：Spring Boot 持有对局状态与 AI，React 只负责渲染。

目前可玩：《璀璨宝石》。AI 是通用的蒙特卡洛树搜索层，新游戏只要实现规则接口就能自动获得分档 AI。

## 现状

| 游戏 | 状态 | 说明 |
|---|---|---|
| 璀璨宝石 Splendor | 可玩 | 2-4 人，拿宝石、预留、购买、贵族、15 分结束轮 |
| 花砖物语 Azul | 计划中 | 用来验证引擎抽象是否成立 |

## 架构

```
浏览器 (React + Vite)
   │ REST: 大厅 / 建局 / 提交动作
   │ SSE : state / ai-thinking / error
   ▼
server (Spring Boot)
   ├─ api       REST 控制器 + SSE + SPA fallback
   ├─ service   SessionService / AiTurnService / SseHub
   └─ store     InMemorySessionStore（接口可换 Redis）
   ▼
engine (纯 Java，零框架依赖)
   ├─ core      GameDefinition / Action / GameOutcome
   ├─ ai        MctsSearch / GreedySearch / AiPlayer / Difficulty
   └─ games     splendor/（azul/ 待加）
```

三条硬规矩：

1. `engine` 不依赖 Spring、不碰 IO 和 UI，可以用纯 JUnit 测。
2. 游戏之间不互相引用，只能通过 `engine.core` 的接口通信。
3. 随机性只出现在 `newGame(seed)`，同一 seed 的初始局面完全一致；隐藏信息通过 `determinize` 采样。这样联机、复盘、AI 采样都能复用同一套状态。

## 技术栈

- 后端：Java 21、Spring Boot 3.5、Maven
- 前端：React 18、TypeScript、Vite 5
- 测试：JUnit 5、Vitest + Testing Library
- 通信：REST + SSE

## 本地运行

需要 JDK 21+、Node 20+、Maven 3.9+（仓库带 `mvnw`，没有 Maven 也能构建）。

### 开发模式

```bash
# 终端 1：后端
mvn -pl server -am spring-boot:run

# 终端 2：前端（已配好 /api 代理到 8080）
cd web && npm install && npm run dev
```

打开 http://localhost:5173 。

### 单 jar 运行

```bash
cd web && npm install && npm run build   # 产出 web/dist
cd .. && mvn package                     # 把 dist 打进 jar
java -jar server/target/boardgame.jar
```

打开 http://localhost:8080 。前端构建失败或没构建时，后端仍能启动，只是没有页面。

> 受限环境里 Vite/esbuild 需要 spawn 子进程，可能被拒绝。这时用 `npm run build:local`：
> 它用 TypeScript + Rollup 在进程内直接打包，产物同样是 `web/dist`。

## 操作

- 大厅先选人数（2/3/4 人 = 你 + 1～3 个 AI）和难度。
- 点宝石图标选中，再点一次取消；凑成「3 种不同颜色」或「2 个同色」后点「确认」提交。
- 卡牌下方直接有「购买 / 预留」；牌堆可点「盲抽」，拿一张卡和一个黄金。
- 宝石超过 10 个时回合会停在弃牌阶段，点自己面板里的宝石弃到 10 个。
- 同时满足多个贵族时，点贵族牌选择。
- 左侧是牌桌（3 级在上、1 级在下），右侧是 AI 状态与对局记录。
- 右栏会直接显示 AI 预留的卡面，点「隐藏预留」可关掉。这是单人版的有意放宽，联机版需要重新裁剪这部分。
- 右上角「音效」可调音量与静音，设置会记住。

## AI 是怎么做的

- 通用层 `engine/ai`：`MctsSearch` 对隐藏信息做多次确定性采样，每个世界跑一棵 UCT 树，再按各世界的选择投票。
- 难度只调预算与失误率，不改规则：

| 难度 | 预算 | 失误率 |
|---|---|---|
| EASY | 贪心 | 25% |
| NORMAL | 800ms | 8% |
| HARD | 2.5s | 0 |
| EXPERT | 6s | 0 |

- 新游戏只需实现 `GameDefinition`，并可选地给一个 `heuristic`，就能直接继承这套 AI。

命令行棋力评测：

```bash
java -cp engine/target/classes boardgame.engine.tools.SplendorEval 40
```

## 加一款新游戏

见 [docs/adding-a-game.md](docs/adding-a-game.md)。

## 测试

```bash
mvn test                 # 规则 + AI + 接口
cd web && npm test       # 组件
cd web && npm run typecheck
```

## 卡面：让克隆版显示官方图

仓库里只有自绘的 SVG 图形（`web/src/games/*/art.tsx`），官方卡面不随仓库分发。想要插画卡面，把图放到本地目录
`web/public/art-official/splendor/` 后重新构建即可，这个目录已在 `.gitignore` 里。

三种放法，任选其一：

```bash
# 1. 自动抓取（90 张卡 + 10 张贵族）
node tools/fetch-splendor-art.mjs

# 2. 手上的图按卡号命名，如 3g2B.png、4g4r.jpg
node tools/import-card-art.mjs "D:/我的卡面"

# 3. 纯序号命名，如 c0.png ~ c89.png
node tools/import-card-art.mjs "D:/下载的序号图"
```

导入前可以先 `--check` 干跑，看看能识别多少、还缺哪些。命名规范、映射表、常见问题都在
[docs/art.md](docs/art.md)。

页面按 `/art-official/splendor/<标准名>.png` 优先加载，依次尝试 png / jpg / webp，找不到才退回自绘。
页脚在缺图时会有提示，方便确认到底有没有生效。

## Roadmap

- [x] 璀璨宝石（2/3/4 人、四档 AI、点选交互、官方卡面导入）
- [x] 花砖物语 Azul（2/3/4 人、完整规则、四边牌桌、自绘瓦片，`engine/core` 未改动）
- [ ] 联机对局（状态与动作已可序列化，还差房间与鉴权）

## 目录

```
engine/   纯 Java 引擎、AI、会话存储
server/   Spring Boot 外壳
web/      React 前端
tools/    数据生成、卡面抓取与导入脚本
docs/     文档
```

## 许可

代码 MIT，见 [LICENSE](LICENSE)；第三方作品的权利归各自所有者，见 [NOTICE.md](NOTICE.md)。
