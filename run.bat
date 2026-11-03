@echo off
setlocal EnableExtensions

cd /d "%~dp0"

set "JAR=target\wsl-docker-management-1.0.0-shaded.jar"

where java >nul 2>nul
if errorlevel 1 (
    echo Java が見つかりません。JDK 21 以上をインストールし、PATH に追加してください。
    pause
    exit /b 1
)

if not exist "%JAR%" (
    echo shaded JAR がありません。ビルドします...
    where mvn >nul 2>nul
    if errorlevel 1 (
        echo Maven が見つかりません。先に `mvn -q -DskipTests package` を実行してください。
        pause
        exit /b 1
    )
    call mvn -q -DskipTests package
    if errorlevel 1 (
        echo ビルドに失敗しました。
        pause
        exit /b 1
    )
)

start "" javaw -jar "%JAR%" %*
exit /b 0
