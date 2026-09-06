#!/bin/bash
# Smoke test for a built jxmake_dist tree, run against one or more JDKs.
#
# Used by .github/workflows/test-jxmake-macos.yml (both zip_smoke_test and
# native_macos_build) so the checks live in one place instead of being
# inlined twice. Run from the directory containing `./jxmake` (i.e. an
# extracted dist zip, or `dist_build/` straight after `make dist`).
#
# Usage:
#   test-jxmake-macos_smoke_test.sh <jdk_home_1> <label_1> [<jdk_home_2> <label_2> ...]
#
# Example:
#   test-jxmake-macos_smoke_test.sh "$JAVA_HOME_8_ARM64" "Java 8" "$JAVA_HOME_25_ARM64" "Java 25"

set -euo pipefail

if [ "$#" -eq 0 ] || [ $(( $# % 2 )) -ne 0 ]; then
    echo "Usage: $0 <jdk_home_1> <label_1> [<jdk_home_2> <label_2> ...]" >&2
    exit 2
fi

chmod +x jxmake

run_for_jdk() {
    local jdk_home="$1"
    local label="$2"
    export JXMAKE_JAVA="$jdk_home/bin/java"

    echo "=== [$label] ./jxmake (no args) ==="
    usage_out="$(./jxmake 2>&1 || true)"
    echo "$usage_out"
    if [[ "$usage_out" != *"no command line arguments specified and no 'JxMakeFile' in the current directory"* ]]; then
        echo "FAIL [$label]: usage-error text not found" >&2
        exit 1
    fi

    echo "=== [$label] ./jxmake -e '\$printf(...)' ==="
    eval_out="$(./jxmake -e '$printf("%s is %d\n", "A", 10)')"
    echo "$eval_out"
    if [[ "$eval_out" != "A is 10" ]]; then
        echo "FAIL [$label]: expected 'A is 10', got '$eval_out'" >&2
        exit 1
    fi
}

while [ "$#" -gt 0 ]; do
    run_for_jdk "$1" "$2"
    shift 2
done

echo "PASS: smoke tests succeeded for all given JDKs."
