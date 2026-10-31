@echo off
rem สร้าง dist\SpellDuel.jar
setlocal
cd /d "%~dp0"
if exist out\main rmdir /s /q out\main
if exist dist rmdir /s /q dist
dir /s /b src\main\java\*.java > sources.txt
javac --release 17 -encoding UTF-8 -d out\main @sources.txt || exit /b 1
del sources.txt
xcopy /e /i /q src\main\resources out\main >nul
mkdir dist
jar --create --file dist\SpellDuel.jar --main-class spellduel.Main -C out\main .
echo Built dist\SpellDuel.jar
