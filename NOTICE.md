# 版权与商标声明

本项目是非商业的个人学习作品，与下列作品的作者、出版社、发行商没有任何关联，也未获得其授权或背书。

## 游戏规则与名称

- 《璀璨宝石》/ Splendor：设计 Marc André，出版 Space Cowboys / Asmodee。
- 《花砖物语》/ Azul：设计 Michael Kiesler，出版 Plan B Games / Next Move Games。

游戏名称、规则文本、商标归各自权利人所有。本仓库中的规则实现是依据公开规则说明重新编写的代码，不包含原版插画、卡面、图标或说明书文本。

## 美术资源

仓库内 `web/src/games/*/art.tsx` 中的图形是本项目自绘的几何示意，仅用于标识卡牌等级、颜色与费用，与官方美术无关。

`web/public/art-official/` 目录用于本地放置用户自备的官方图片，该目录已在 `.gitignore` 中，不随仓库分发。
`tools/fetch-splendor-art.mjs` 只是从公开镜像抓取该素材的便利脚本，仓库本身不包含图片。
使用者需自行确认所放置素材的合法来源与使用范围。

## 代码

本仓库代码以 MIT 许可发布，见 [LICENSE](LICENSE)。
