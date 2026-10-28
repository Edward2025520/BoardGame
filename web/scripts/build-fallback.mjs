// 备用构建：不依赖 Vite/esbuild（它们需要 spawn 子进程，在受限环境里会被拒绝）。
// 用 TypeScript 的 transpileModule 逐文件转译，再用 Rollup 打包成单个 ESM 文件。
// 用法：node scripts/build-fallback.mjs
import { existsSync, mkdirSync, readFileSync, readdirSync, cpSync, writeFileSync, rmSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { rollup } from 'rollup';
import nodeResolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import ts from 'typescript';

const root = resolve(import.meta.dirname, '..');
const outDir = join(root, 'dist');

const tsPlugin = {
  name: 'ts-transpile',
  load(id) {
    if (!/\.tsx?$/.test(id)) {
      return null;
    }
    const source = readFileSync(id, 'utf8');
    const { outputText } = ts.transpileModule(source, {
      fileName: id,
      compilerOptions: {
        module: ts.ModuleKind.ESNext,
        target: ts.ScriptTarget.ES2022,
        jsx: ts.JsxEmit.ReactJSX,
        esModuleInterop: true,
        sourceMap: false,
      },
    });
    return { code: outputText, map: null };
  },
};

const productionEnv = {
  name: 'production-env',
  transform(code) {
    if (!code.includes('process.env.NODE_ENV')) {
      return null;
    }
    return { code: code.replaceAll('process.env.NODE_ENV', '"production"'), map: null };
  },
};

const bundle = await rollup({
  input: join(root, 'src/main.tsx'),
  plugins: [
    tsPlugin,
    nodeResolve({ extensions: ['.ts', '.tsx', '.js', '.jsx', '.json'], browser: true }),
    commonjs(),
    productionEnv,
  ],
});

rmSync(outDir, { recursive: true, force: true });
mkdirSync(join(outDir, 'assets'), { recursive: true });
await bundle.write({ file: join(outDir, 'assets/app.js'), format: 'esm', sourcemap: false });

const publicDir = join(root, 'public');
if (existsSync(publicDir)) {
  for (const entry of readdirSync(publicDir)) {
    if (entry === 'index.html') {
      continue;
    }
    cpSync(join(publicDir, entry), join(outDir, entry), { recursive: true });
  }
}

const html = `<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>桌游合集</title>
    <link rel="stylesheet" href="/styles.css" />
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/assets/app.js"></script>
  </body>
</html>
`;
writeFileSync(join(outDir, 'index.html'), html, 'utf8');
console.log('前端构建完成：web/dist');
