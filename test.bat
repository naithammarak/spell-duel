@echo off
rem รันเทสต์ทั้งหมด (JUnit 5)  ตัวอย่างเทสต์ class เดียว: test.bat --select-class spelldefender.typing.LevenshteinTest
setlocal
cd /d "%~dp0"
set JUNIT_VERSION=1.11.4
set JUNIT_JAR=lib\junit-platform-console-standalone-%JUNIT_VERSION%.jar
if not exist "%JUNIT_JAR%" (
  if not exist lib mkdir lib
  curl -fsSL -o "%JUNIT_JAR%" "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/%JUNIT_VERSION%/junit-platform-console-standalone-%JUNIT_VERSION%.jar" || exit /b 1
)
if exist out rmdir /s /q out
dir /s /b src\main\java\*.java > sources.txt
javac --release 17 -encoding UTF-8 -d out\main @sources.txt || exit /b 1
xcopy /e /i /q src\main\resources out\main >nul
dir /s /b src\test\java\*.java > tests.txt
javac --release 17 -encoding UTF-8 -d out\test -cp "out\main;%JUNIT_JAR%" @tests.txt || exit /b 1
del sources.txt tests.txt
if "%~1"=="" (
  java -jar "%JUNIT_JAR%" execute --class-path "out\main;out\test" --disable-banner --scan-classpath
) else (
  java -jar "%JUNIT_JAR%" execute --class-path "out\main;out\test" --disable-banner %*
)
