@echo off
cd /d "%~dp0"

set VERSION=1.1.0

echo ========================================
echo   ModMedic -- Build Distribution Bundle
echo ========================================
echo.

:: Step 1: Build Plugin
echo [1/4] Building plugin...
cd modmedic-plugin
call .\gradlew.bat build -q
if %ERRORLEVEL% neq 0 ( echo Plugin build failed! & pause & exit /b 1 )
cd ..

:: Step 2: Build Desktop distribution
echo [2/4] Building desktop app...
cd modmedic-desktop
call .\gradlew.bat distZip -q
if %ERRORLEVEL% neq 0 ( echo Desktop build failed! & pause & exit /b 1 )
cd ..

:: Step 3: Assemble bundle
echo [3/4] Assembling bundle...
set BUNDLE=ModMedic-Bundle
if exist %BUNDLE% rmdir /s /q %BUNDLE%
mkdir %BUNDLE%\desktop
mkdir %BUNDLE%\server

:: Extract the distZip
powershell -NoLogo -NoProfile -Command "Expand-Archive -Path 'modmedic-desktop\build\distributions\ModMedicDesktop-%VERSION%.zip' -DestinationPath '%CD%\%BUNDLE%\desktop' -Force"

:: Move files from inner directory up one level
for /d %%d in ("%BUNDLE%\desktop\ModMedicDesktop-%VERSION%") do (
    if exist "%%d" (
        xcopy "%%d\*" "%BUNDLE%\desktop\" /E /Y >nul 2>&1
        rmdir /s /q "%%d" 2>nul
    )
)

:: Copy plugin jar
copy modmedic-plugin\build\libs\ModMedic-%VERSION%.jar %BUNDLE%\server\ >nul

:: Remove non-Windows JavaFX jars
del %BUNDLE%\desktop\lib\javafx-*-linux.jar 2>nul
del %BUNDLE%\desktop\lib\javafx-*-mac.jar 2>nul

:: Create README
(
echo ModMedic v%VERSION%
echo ================
echo.
echo Plugin error diagnostics for Paper servers.
echo.
echo === Quick Start ===
echo.
echo 1. Copy "server\ModMedic-%VERSION%.jar" to your Paper server's plugins/ folder
echo 2. Restart the server
echo 3. Double-click "desktop\bin\ModMedicDesktop.bat" to launch the GUI
echo.
echo The plugin auto-connects to the desktop app at ws://localhost:9876.
echo.
echo === Configuration ===
echo.
echo Plugin: edit plugins/ModMedic/config.yml
echo   - desktop_host: localhost
echo   - desktop_port: 9876
echo   - capture_console_log: true
echo   - log_buffer_lines: 200
echo.
echo Desktop: edit ~/.modmedic/settings.json
echo   - desktopPort: 9876
echo   - maxLogLines: 1000
echo.
) > %BUNDLE%\README.txt

echo.
echo ========================================
echo   Bundle ready: %CD%\%BUNDLE%\
echo ========================================
echo.
echo   Plugin:  %BUNDLE%\server\ModMedic-%VERSION%.jar
echo   Desktop: %BUNDLE%\desktop\bin\ModMedicDesktop.bat
echo.
pause
