# 版权与商标声明

本项目是非商业的个人学习作品，与下列作品的作者、出版社、发行商没有任何关联，也未获得其授权或背书。

## 游戏规则与名称

- 《璀璨宝石》/ Splendor：设计 Marc André，出版 Space Cowboys / Asmodee。
- 《花砖物语》/ Azul：设计 Michael Kiesler，出版 Plan B Games / Next Move Games。

游戏名称、规则文本、商标归各自权利人所有。本仓库中的规则实现是依据公开规则说明重新编写的代码，不包含原版插画、卡面、图标或说明书文本。

## 美术资源

仓库内 `web/src/games/*/art.tsx` 中的图形是本项目自绘的几何示意，仅用于标识卡牌等级、颜色与费用，与官方美术无关。

`web/public/art-official/` 目录用于本地放置用户自备的官方图片，该目录已在 `.gitignore` 中，不随仓库分发。
`tools/fetch-splendor-art.mjs` 与 `tools/import-card-art.mjs` 只是在本地准备这些图片的便利脚本，仓库本身不包含图片。
使用者需自行确认所放置素材的合法来源与使用范围。

## 数据来源

- 90 张发展卡与 10 张贵族的属性（等级、颜色、分数、成本、贵族需求）来自
  [roeey777/Splendor-AI](https://github.com/roeey777/Splendor-AI)（MIT），由 `tools/gen-splendor-data.mjs` 机器转换生成
  `SplendorData.java`，未经人工改写。
- `tools/art-index.json` 中的序号顺序参考 [zhefu3/shanzhai-splendor](https://github.com/zhefu3/shanzhai-splendor)
  的 `src/engine/cards.ts`，仅用于把 `c0..c89` / `n0..n9` 映射到本项目的标准文件名。
- 上述使用都只涉及规则参数与文件名映射这类事实数据，不包含对方的源代码、文案或美术资源。
- 音效的合成配方（波形、音高、包络）参考 [Edward2025520/shanzhai-splendor](https://github.com/Edward2025520/shanzhai-splendor)
  的 `src/ui/sound.ts`（MIT），本项目在其基础上扩展了事件数量并接入音量控制。
- 七档难度的档位名称与预算梯度同样参考该项目。

## 代码

本仓库代码以 MIT 许可发布，见 [LICENSE](LICENSE)。
