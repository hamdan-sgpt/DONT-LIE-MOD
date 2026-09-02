@echo off
title Corazon Mod - Launch 4 Minecraft Clients
echo ==========================================
echo   CORAZON MOD - Launch 4 Minecraft Clients
echo ==========================================
echo.

if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [INFO] gradle-wrapper.jar belum ada. Mengunduh otomatis...
    if not exist "gradle\wrapper" mkdir "gradle\wrapper"
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.5/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'"
)

echo [1/4] Menjalankan Client 1 (Corazon)...
start "Client 1 - Corazon" cmd /k "call run_client1.bat"

echo.
echo Tunggu 8 detik sebelum launch Client 2...
timeout /t 8 /nobreak >nul

echo.
echo [2/4] Menjalankan Client 2 (Player2)...
start "Client 2 - Player2" cmd /k "call run_client2.bat"

echo.
echo Tunggu 8 detik sebelum launch Client 3...
timeout /t 8 /nobreak >nul

echo.
echo [3/4] Menjalankan Client 3 (Player3)...
start "Client 3 - Player3" cmd /k "call run_client3.bat"

echo.
echo Tunggu 8 detik sebelum launch Client 4...
timeout /t 8 /nobreak >nul

echo.
echo [4/4] Menjalankan Client 4 (Player4)...
start "Client 4 - Player4" cmd /k "call run_client4.bat"

echo.
echo ==========================================
echo   4 jendela terminal Client telah dibuka!
echo   Pemain: Corazon, Player2, Player3, Player4
echo ==========================================
pause
