@echo off
title Don't Lie - Launch 5 Minecraft Clients
echo ===================================================
echo 🚀 MEMULAI 5 CLIENT MINECRAFT (DON'T LIE TESTER)
echo ===================================================
echo [0/5] Menyiapkan Asset Minecraft (Mencegah Bentrok Download)...
call gradlew.bat downloadAssets
echo.
echo [1/5] Membuka Client 1 (Corazon)...
start "Client 1 - Corazon" cmd /k "gradlew.bat runClient --args="--username Corazon""
timeout /t 3 /nobreak >nul

echo [2/5] Membuka Client 2 (Hoshi)...
start "Client 2 - Hoshi" cmd /k "gradlew.bat runClient2 --args="--username Hoshi""
timeout /t 2 /nobreak >nul

echo [3/5] Membuka Client 3 (Mingyu)...
start "Client 3 - Mingyu" cmd /k "gradlew.bat runClient3 --args="--username Mingyu""
timeout /t 2 /nobreak >nul

echo [4/5] Membuka Client 4 (DK)...
start "Client 4 - DK" cmd /k "gradlew.bat runClient4 --args="--username DK""
timeout /t 2 /nobreak >nul

echo [5/5] Membuka Client 5 (Wonwoo)...
start "Client 5 - Wonwoo" cmd /k "gradlew.bat runClient5 --args="--username Wonwoo""

echo.
echo ===================================================
echo ✅ 5 WINDOW CMD CLIENT TELAH DIPANGGIL!
echo Setiap window akan mengkompilasi & membuka Minecraft.
echo ===================================================
pause
