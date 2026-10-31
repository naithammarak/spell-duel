@echo off
rem build แล้วเปิดเกม
setlocal
cd /d "%~dp0"
if exist out\main rmdir /s /q out\main
dir /s /b src\main\java\*.java > sources.txt
javac --release 17 -encoding UTF-8 -d out\main @sources.txt || exit /b 1
del sources.txt
xcopy /e /i /q src\main\resources out\main >nul
java -cp out\main spellduel.Main
