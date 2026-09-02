@echo off
title Don't Lie - Launch 5 Minecraft Clients
echo ===================================================
echo 🚀 MEMULAI 5 CLIENT MINECRAFT (DON'T LIE TESTER)
echo ===================================================
echo.
echo [1/5] Membuka Client 1 (Host / Admin)...
start "Client 1 (Host)" cmd /c "gradlew.bat runClient"
timeout /t 10 /nobreak >nul

echo [2/5] Membuka Client 2...
start "Client 2" cmd /c "gradlew.bat runClient2"
timeout /t 5 /nobreak >nul

echo [3/5] Membuka Client 3...
start "Client 3" cmd /c "gradlew.bat runClient3"
timeout /t 5 /nobreak >nul

echo [4/5] Membuka Client 4...
start "Client 4" cmd /c "gradlew.bat runClient4"
timeout /t 5 /nobreak >nul

echo [5/5] Membuka Client 5...
start "Client 5" cmd /c "gradlew.bat runClient5"

echo.
echo ===================================================
echo ✅ SEMUA 5 CLIENT TELAH DIPANGGIL!
echo Harap tunggu hingga window Minecraft terbuka sepenuhnya.
echo ===================================================
pause
