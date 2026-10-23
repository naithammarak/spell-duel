#!/usr/bin/env bash
# สร้าง dist/SpellDefender.jar (ดับเบิลคลิกหรือ java -jar ได้)
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out/main dist
javac --release 17 -encoding UTF-8 -d out/main $(find src/main/java -name '*.java')
cp -r src/main/resources/. out/main/
mkdir -p dist
jar --create --file dist/SpellDefender.jar --main-class spelldefender.Main -C out/main .
echo "Built dist/SpellDefender.jar"
