@echo off
title Clean & Rebuild Don't Lie Mod
echo ===================================================
echo 🧹 MEMBERSIHKAN CACHE & REBUILD MOD MINECRAFT
echo ===================================================
echo.
call gradlew.bat clean compileJava downloadAssets
echo.
echo ===================================================
echo ✅ REBUILD SELESAI!
echo ===================================================
pause
