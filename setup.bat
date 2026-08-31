@echo off
title Corazon Mod - Setup Gradle Wrapper
echo ==========================================
echo   CORAZON MOD - SETUP AWAL
echo ==========================================
echo.
echo [STEP 1] Download Gradle Wrapper JAR...
echo.

@rem Check if gradle-wrapper.jar already exists
if exist "gradle\wrapper\gradle-wrapper.jar" (
    echo gradle-wrapper.jar sudah ada, skip download.
    goto :build
)

@rem Try using system Gradle to generate wrapper
where gradle >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo Menggunakan system Gradle untuk generate wrapper...
    gradle wrapper --gradle-version 8.1.1
    goto :build
)

@rem Download wrapper JAR manually via PowerShell
echo Gradle tidak ditemukan di PATH.
echo Downloading gradle-wrapper.jar dari GitHub...
powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.1.1/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar' }" 2>nul

if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo.
    echo ==========================================
    echo   GAGAL download otomatis.
    echo   Cara manual:
    echo   1. Download Forge MDK dari https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html
    echo   2. Extract, copy file gradle/ folder dan gradlew.bat ke project ini
    echo   3. Jalankan setup.bat lagi
    echo ==========================================
    pause
    exit /b 1
)

:build
echo.
echo [STEP 2] Building mod...
call gradlew.bat build
if %ERRORLEVEL% neq 0 (
    echo.
    echo BUILD GAGAL! Pastikan Java 17 JDK sudah terinstall.
    echo Download: https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b 1
)

echo.
echo [STEP 3] Generating run configurations...
call gradlew.bat genEclipseRuns
echo.
echo ==========================================
echo   SETUP SELESAI!
echo.
echo   Jalankan:
echo   - run_client1.bat  (Minecraft as Corazon)
echo   - run_client2.bat  (Minecraft as Player2)
echo   - run_both.bat     (Buka 2 client sekaligus)
echo ==========================================
pause
