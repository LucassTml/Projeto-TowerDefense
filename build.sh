#!/usr/bin/env sh
# Builds build/TowerRogue.jar with nothing but a JDK (21 or newer).
set -e
cd "$(dirname "$0")"

rm -rf build
mkdir -p build/classes
find src/main/java -name "*.java" > build/sources.txt
javac --release 21 -encoding UTF-8 -d build/classes @build/sources.txt
cp -r src/main/resources/. build/classes/
if ! command -v jar >/dev/null 2>&1; then
  echo "Compiled to build/classes ('jar' tool not on PATH, skipping the jar)."
  echo "Play: java -cp build/classes io.github.lucasstml.towerrogue.Main"
  exit 0
fi
jar --create --file build/TowerRogue.jar --main-class io.github.lucasstml.towerrogue.Main -C build/classes .

echo "Built build/TowerRogue.jar"
echo "Play:                java -jar build/TowerRogue.jar"
echo "Determinism check:   java -jar build/TowerRogue.jar --verify-determinism"
echo "Balance report:      java -jar build/TowerRogue.jar --balance-report 20"
