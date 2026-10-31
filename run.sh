#!/usr/bin/env bash
# build แล้วเปิดเกม
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out/main
javac --release 17 -encoding UTF-8 -d out/main $(find src/main/java -name '*.java')
cp -r src/main/resources/. out/main/
java -cp out/main spellduel.Main
