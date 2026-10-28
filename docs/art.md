# 卡面导入

仓库不包含官方卡面，克隆下来默认显示自绘几何图形。想要官方卡面，按下面任一方式把图片放到本地目录即可，图片不会被 Git 跟踪。

目标目录固定为：

```
web/public/art-official/splendor/
```

## 方式一：自动抓取（最省事）

```bash
node tools/fetch-splendor-art.mjs
```

- 从公开镜像拉 90 张发展卡 + 10 张贵族，按标准名写入目标目录。
- 已存在的文件会跳过，可重复执行，中断后重跑会续传。
- 镜像走 jsDelivr，失败自动切 raw.githubusercontent，单张最多重试 5 次。

## 方式二：按卡号批量导入

你手上如果是按卡号命名的图，例如 `3g2B.png`、`7r3B.jpg`、`4g4r.png`：

```bash
node tools/import-card-art.mjs "D:/我的卡面"
```

先干跑看会导入哪些、缺哪些，不落盘：

```bash
node tools/import-card-art.mjs "D:/我的卡面" --check
```

## 方式三：按序号批量导入

有些公开素材是纯序号命名，`c0.png` ~ `c89.png`、`n0.png` ~ `n9.png`：

```bash
node tools/import-card-art.mjs "D:/下载的序号图"
```

序号到卡面的对应关系在 `tools/art-index.json`，导入时自动查表，不需要你手动改名。这套序号只对应公开实现
`zhefu3/shanzhai-splendor` 的顺序，其他来源的编号不一定通用。

## 命名规范

| 类型 | 文件名 | 示例 |
|---|---|---|
| 发展卡 | `card-{等级}-{颜色}-{分数}-{白-蓝-绿-红-黑成本}` | `card-1-blue-0-0-0-0-2-1.png` |
| 贵族 | `noble-{白-蓝-绿-红-黑需求}` | `noble-0-0-4-4-0.png` |

- 颜色写 `white` / `blue` / `green` / `red` / `black`。
- 支持 `.png`、`.jpg`、`.jpeg`、`.webp`；`.jpeg` 会被统一成 `.jpg`。
- 不用卡号当文件名：`3b` 与 `3B`、`4w4b` 与 `4w4B` 在 Windows 上会撞成同一个文件，标准名不会。
- 同一张卡同时存在标准名、卡号、序号时，优先用标准名，其余记为重复跳过。

## 导入工具会做什么

1. 扫描目录里的图片（只认扩展名，并校验文件头，坏图会单独列出）。
2. 依次按「标准名 → 卡号 → 序号」识别，卡号大小写不一致时：能唯一确定就转换，`3b`/`3B` 这种有歧义的会报出来让你手动处理。
3. 按标准名拷贝到目标目录，同名文件覆盖并计数。
4. 最后报告：导入了多少、覆盖了多少、哪些不认识、还缺多少张。

没有识别出任何图片时退出码非 0，方便脚本判断。

## 让改动生效

图片是构建时打进静态资源的，放好之后要重新构建：

```bash
# 开发模式：放到 web/public/... 后 Vite 会直接读到，刷新即可
cd web && npm run dev

# 单 jar：必须重新构建前端再打包
cd web && npm run build      # 或 npm run build:local
cd .. && mvn clean package
java -jar server/target/boardgame.jar
```

浏览器缓存会记住旧图，刷新用 Ctrl+F5。

## 怎么确认生效

- 对局页底部不再出现「当前显示的是自绘卡面」的提示。
- 直接访问 `http://localhost:8080/art-official/splendor/card-1-white-0-0-0-0-2-1.png` 能返回图片。
- 卡面明显变成插画而不是色块。

## 常见问题

**图放进去了但页面还是自绘？**
检查三件事：文件名是否是标准名（用 `--check` 看识别结果）、是否重新构建、是否强刷缓存。

**可以只放一部分吗？**
可以。缺的那几张会单独退回自绘，不影响其它卡。

**为什么不把图直接提交进仓库？**
《璀璨宝石》的插画版权归出版社，仓库只以 MIT 分发代码。图片目录已在 `.gitignore` 里，导入脚本也不会上传。

## 花砖物语（Azul）

花砖物语只需要 6 张图，仓库里默认是自绘的阿拉伯纹样，不依赖任何外部素材：

| 文件 | 说明 |
|---|---|
| `tile-blue.png` | 蓝色瓦片 |
| `tile-yellow.png` | 黄色瓦片 |
| `tile-red.png` | 红色瓦片 |
| `tile-black.png` | 黑色瓦片 |
| `tile-white.png` | 白色瓦片 |
| `tile-start.png` | 首拿标记 |

放到 `web/public/art-official/azul/` 即可，同样支持 png / jpg / webp，缺哪张就只有那张退回自绘。
不需要导入脚本，也不需要改命名规则——文件名就是上面这 6 个。

注意：没有提供花砖物语的抓图脚本。GitHub 上能找到的实现要么用自绘 SVG 且仓库无许可证，要么是官方素材转载，
都不适合作为默认来源；想要官方图请自己准备合法素材。

```bash
# 放好之后重新构建
cd web && npm run build:local
cd .. && mvn clean package
```

