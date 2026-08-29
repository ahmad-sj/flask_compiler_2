#!/bin/sh
# Builds the compiler into out/classes.
# Sources are globbed at build time, so no file list needs maintaining.
#
#   ./build.sh
#   java -cp "out/classes:dependencies/antlr-4.13.2-complete.jar" app.FlaskCompiler tests/app.py
#
# (on Windows use ';' instead of ':' in the classpath)
set -e
cd "$(dirname "$0")"
mkdir -p out/classes

# --- Step 1: regenerate the parsers from the .g4 grammars -------------------
# Without this, editing a grammar has no effect: src/antlr/*.java is committed
# and javac would just recompile the stale copies.
#
# templateFragments.g4 is not listed: it is a fragment-only grammar pulled in
# by `import templateFragments;` inside templateLexer.g4, so ANTLR resolves it
# from the grammars/ directory and emits no file of its own.
#
# No -package flag: all four grammars already declare `package antlr;` in their
# @header, and -package would emit it a second time, which does not compile.
java -jar dependencies/antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -o src/antlr \
    grammars/pythonLexer.g4 \
    grammars/pythonParser.g4 \
    grammars/templateLexer.g4 \
    grammars/templateParser.g4
echo "ANTLR OK  -> src/antlr"

find src -name '*.java' > out/sources.generated.txt
javac -nowarn -cp dependencies/antlr-4.13.2-complete.jar -d out/classes "@out/sources.generated.txt"
echo "Build OK -> out/classes"
