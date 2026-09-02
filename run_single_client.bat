@echo off
title Don't Lie - Single Client Launcher
:menu
cls
echo ===================================================
echo 🎮 DON'T LIE - SINGLE CLIENT LAUNCHER
echo ===================================================
echo [1] Jalankan Client 1 (Host Admin)
echo [2] Jalankan Client 2
echo [3] Jalankan Client 3
echo [4] Jalankan Client 4
echo [5] Jalankan Client 5
echo [6] Jalankan SEMUA 5 Client Sekaligus
echo [0] Keluar
echo ===================================================
set /p choice="Pilih nomor (0-6): "

if "%choice%"=="1" start "Client 1" cmd /c "gradlew.bat runClient"
if "%choice%"=="2" start "Client 2" cmd /c "gradlew.bat runClient2"
if "%choice%"=="3" start "Client 3" cmd /c "gradlew.bat runClient3"
if "%choice%"=="4" start "Client 4" cmd /c "gradlew.bat runClient4"
if "%choice%"=="5" start "Client 5" cmd /c "gradlew.bat runClient5"
if "%choice%"=="6" call run_5_players.bat
if "%choice%"=="0" exit

pause
goto menu
