@echo off
REM ============================================================
REM  Rain Alert - compile and start the Java server
REM  Double-click this file, then open http://localhost:8080
REM ============================================================
cd /d "%~dp0"

REM Read the API key from key.txt (this file is never uploaded to GitHub)
if exist key.txt (
  set /p OWM_API_KEY=<key.txt
) else (
  echo key.txt not found. Create key.txt with your OpenWeatherMap API key inside.
  pause
  exit /b 1
)

REM Use Java from Android Studio if "javac" is not on the PATH
where javac >nul 2>nul
if %errorlevel%==0 (
  set "JAVA_BIN="
) else (
  set "JAVA_BIN=C:\Program Files\Android\Android Studio\jbr\bin\"
)

echo Compiling...
"%JAVA_BIN%javac" -cp "lib\json-20240303.jar" -d out RainAlertServer.java
if errorlevel 1 (
  echo.
  echo Compile failed. See the error above.
  pause
  exit /b 1
)

echo Starting server...
start "" http://localhost:8080
"%JAVA_BIN%java" -cp "out;lib\json-20240303.jar" RainAlertServer
pause
