#!/usr/bin/env bash
# รันเทสต์ทั้งหมด (JUnit 5)
#   ./test.sh                                              ทุกเทสต์
#   ./test.sh --select-class spellduel.typing.LevenshteinTest   เทสต์ class เดียว
set -euo pipefail
cd "$(dirname "$0")"

JUNIT_VERSION=1.11.4
JUNIT_JAR="lib/junit-platform-console-standalone-$JUNIT_VERSION.jar"
if [ ! -f "$JUNIT_JAR" ]; then
  mkdir -p lib
  echo "Downloading JUnit $JUNIT_VERSION ..."
  curl -fsSL -o "$JUNIT_JAR" \
    "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$JUNIT_VERSION/junit-platform-console-standalone-$JUNIT_VERSION.jar"
fi

rm -rf out
javac --release 17 -encoding UTF-8 -d out/main $(find src/main/java -name '*.java')
cp -r src/main/resources/. out/main/
javac --release 17 -encoding UTF-8 -d out/test -cp "out/main:$JUNIT_JAR" $(find src/test/java -name '*.java')

if [ $# -eq 0 ]; then set -- --scan-classpath; fi
java -jar "$JUNIT_JAR" execute --class-path "out/main:out/test" --disable-banner "$@"
