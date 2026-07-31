#!/usr/bin/env bash
#
# Common launcher for Java helper applications.
#
# Caller must set:
#     PROGRAM=java_content_diff
# or
#     PROGRAM=kotlin_syntax_check
#

if [ -z "$PROGRAM" ]; then
    echo "PROGRAM is not set." >&2
    exit 1
fi

JDK=/opt/openjdk-21_linux-x64_bin/jdk-21
KLIB="$HOME/xsdk/kotlin-compiler-2.4.0/kotlinc/lib"

JAVAC="$JDK/bin/javac"
JAVA="$JDK/bin/java"

DIR="$(dirname "$0")"

SRC="$DIR/${PROGRAM}.java"
CLASS="$PROGRAM"

#
# Build classpath. Class files live alongside the source in $DIR, not CWD.
#
CP="$DIR"

case "$PROGRAM" in
kotlin_*)
    CP="$CP:$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar"
    ;;
esac

#
# Compile if needed. -d puts the .class output back into $DIR regardless
# of caller's CWD.
#
if [ ! -f "$DIR/$CLASS.class" ] || [ "$SRC" -nt "$DIR/$CLASS.class" ]; then
    "$JAVAC" -d "$DIR" -cp "$CP" "$SRC" || exit $?
fi

#
# Note: CWD is intentionally left as the caller's, so that any relative
# file arguments in "$@" resolve the way the caller expects. The compiled
# classes are still found via $CP.
#
exec "$JAVA" -cp "$CP" "$CLASS" "$@"
