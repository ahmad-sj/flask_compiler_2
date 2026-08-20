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
find src -name '*.java' > out/sources.generated.txt
javac -nowarn -cp dependencies/antlr-4.13.2-complete.jar -d out/classes "@out/sources.generated.txt"
echo "Build OK -> out/classes"
