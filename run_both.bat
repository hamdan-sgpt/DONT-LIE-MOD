@echo off
title Corazon Mod - Launch Both Clients
echo ==========================================
echo   CORAZON MOD - Launch 2 Clients
echo ==========================================
echo.

if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [INFO] gradle-wrapper.jar belum ada. Mengunduh otomatis...
    if not exist "gradle\wrapper" mkdir "gradle\wrapper"
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.5/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'"
)

echo [1/2] Menjalankan Client 1 (Corazon)...
start "Client 1 - Corazon" cmd /k "call run_client1.bat"

echo.
echo Tunggu 10 detik sebelum launch Client 2...
timeout /t 10 /nobreak >nul

echo.
echo [2/2] Menjalankan Client 2 (Player2)...
start "Client 2 - Player2" cmd /k "call run_client2.bat"

echo.
echo ==========================================
echo   Dua jendela terminal Client telah dibuka!
echo ==========================================
pause
