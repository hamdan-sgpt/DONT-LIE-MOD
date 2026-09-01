@echo off
title Minecraft Client 2 - Player2
echo ==========================================
echo   MINECRAFT CLIENT 2 - Player2
echo ==========================================
echo.

if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo [INFO] gradle-wrapper.jar belum ada. Mengunduh otomatis...
    if not exist "gradle\wrapper" mkdir "gradle\wrapper"
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.5/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'"
)

if not exist "src\main\resources\maps\MAP-DONT-LIE" (
    echo [INFO] Memasang World Map ke src/main/resources/maps/MAP-DONT-LIE...
    xcopy /E /I /Y /Q "MAP-DONT-LIE" "src\main\resources\maps\MAP-DONT-LIE" >nul
)
if not exist "run-client2\saves\MAP-DONT-LIE" (
    echo [INFO] Memasang World Map 'MAP-DONT-LIE' ke run-client2/saves...
    xcopy /E /I /Y /Q "MAP-DONT-LIE" "run-client2\saves\MAP-DONT-LIE" >nul
)
if not exist "run-client2\MAP-DONT-LIE" (
    echo [INFO] Memasang World Map source ke run-client2/MAP-DONT-LIE...
    xcopy /E /I /Y /Q "MAP-DONT-LIE" "run-client2\MAP-DONT-LIE" >nul
)

if exist "fix_textures.py" (
    echo [INFO] Menjaga tekstur Money Pouch ^& Items tetap up-to-date...
    python fix_textures.py >nul 2>&1
)

echo Menjalankan Minecraft sebagai "Player2"...
echo.
call gradlew.bat runClient2 --args="--username Player2"
pause
