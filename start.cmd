@echo off
chcp 65001 >nul
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
  echo [错误] 系统找不到 java。
  echo 请安装 JDK 21 或更高版本，或者把 JDK 的 bin 目录加进 PATH。
  echo 例如临时使用：set PATH=D:\otherwork\JavaJDK\bin;%PATH%
  pause
  exit /b 1
)

if not exist "server\target\boardgame.jar" (
  echo [提示] 还没有打包出 jar，先在当前目录执行：
  echo   cd web
  echo   npm install
  echo   npm run build
  echo   cd ..
  echo   mvn clean package -DskipTests
  pause
  exit /b 1
)

echo.
echo 正在启动桌游合集...
echo 启动后浏览器打开：http://localhost:8080
echo 玩完按 Ctrl+C，或直接关闭这个窗口。
echo.

java -jar "server\target\boardgame.jar"
pause
