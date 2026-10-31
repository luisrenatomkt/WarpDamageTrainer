#!/bin/sh
# Compila (Java 8 target) e executa os testes da lógica pura (sem Minecraft/Forge).
# Usa "javac" se existir; senão o módulo jdk.compiler do próprio JDK/JRE.
set -e
cd "$(dirname "$0")/.."
OUT=$(mktemp -d)
SRC=src/main/java/com/warpdmgtrainer
FILES="$SRC/model/*.java $SRC/core/PrecisionTimer.java $SRC/core/SessionResult.java $SRC/core/StatsCollector.java $SRC/core/DropGuardState.java $SRC/util/TimeFormat.java tools/LogicSelfTest.java"
if command -v javac >/dev/null 2>&1; then
  javac -encoding UTF-8 --release 8 -d "$OUT" $FILES
else
  java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 8 -target 8 -d "$OUT" $FILES
fi
java -cp "$OUT" LogicSelfTest
