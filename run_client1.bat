@echo off
title Minecraft Client 1 - Corazon
echo ==========================================
echo   MINECRAFT CLIENT 1 - Corazon
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
if not exist "run\saves\MAP-DONT-LIE" (
    echo [INFO] Memasang World Map 'MAP-DONT-LIE' ke run/saves...
    xcopy /E /I /Y /Q "MAP-DONT-LIE" "run\saves\MAP-DONT-LIE" >nul
)
if not exist "run\MAP-DONT-LIE" (
    echo [INFO] Memasang World Map source ke run/MAP-DONT-LIE...
    xcopy /E /I /Y /Q "MAP-DONT-LIE" "run\MAP-DONT-LIE" >nul
)

if exist "fix_textures.py" (
    echo [INFO] Menjaga tekstur Money Pouch ^& Items tetap up-to-date...
    python fix_textures.py >nul 2>&1
)

echo Menjalankan Minecraft sebagai "Corazon"...
echo.
call gradlew.bat runClient --args="--username Corazon"
pause
