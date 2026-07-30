#!/bin/sh
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

SRC="${PROGRAM}.java"
CLASS="$PROGRAM"

#
# Build classpath.
#
CP="."

case "$PROGRAM" in
kotlin_*)
    CP="$CP:$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar"
    ;;
esac

#
# Compile if needed.
#
if [ ! -f "$CLASS.class" ] || [ "$SRC" -nt "$CLASS.class" ]; then
    if [ "$CP" = "." ]; then
        exec_compile() {
            "$JAVAC" "$SRC"
        }
    else
        exec_compile() {
            "$JAVAC" -cp "$CP" "$SRC"
        }
    fi

    exec_compile || exit $?
fi

exec "$JAVA" -cp "$CP" "$CLASS" "$@"
