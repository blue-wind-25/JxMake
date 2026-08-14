#!/usr/bin/env bash
#
# Shared launcher for the c*/cpp*_syntax_check.sh and c*/cpp*_content_diff.sh
# scripts, modeled on _exec_python.sh / _exec_java.sh's PROGRAM= convention,
# but for GCC/Clang instead of a Python/Java helper program.
#
# Caller (one of the 10 c*/cpp*_*.sh scripts) must set before sourcing:
#     PROGRAM=cpp26_syntax_check     (script's own basename, for usage text)
#     EXT=cpp                        (file extension used in usage text)
#     MODE=SYNTAX  or  MODE=DIFF
#
# For MODE=SYNTAX, caller must also set:
#     SC_RUNS=( "COMPILER_VAR:std:lang:label" ... )
#         e.g. SC_RUNS=("GXX:c++20:c++:g++ -std=c++20" "CLANGXX:c++20:c++:clang++ -std=c++20")
#
# For MODE=DIFF, caller must also set:
#     CD_COMPILER_VAR=GXX|CLANGXX
#     CD_DIFF_MODE=gxx_dump|clang_ast|clang_json   (passed through to _exec_c_cpp.py)
#     CD_STD=<std>
#     CD_LANG=c|c++
#
# Both modes accept extra compiler options (e.g. -Iinclude/dir): for SYNTAX
# they must appear BEFORE the file arguments; for DIFF they must appear
# after a literal "--" following the positional args.
#

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$DIR/_exec_c_cpp_env.sh"

# Filters, matching the task-provided pattern:
#   - drop the harmless libstdc++ symbol-versioning warning
#   - strip generated hex addresses ("0x...")
#   - strip "<...>" angle-bracket noise (e.g. template/AST punctuation),
#     except on #include lines, where "<...>" is a real header name
#   - strip ANSI color escape sequences
_c_cpp_filter() {
    grep -v 'no version information available' \
      | sed -E 's/ 0x[0-9a-fA-F]+//g; /#include/!s/<[^>]*>//g' \
      | sed -E 's/\x1B\[[0-9;]*[mK]//g'
}

if [ -z "$PROGRAM" ] || [ -z "$MODE" ] || [ -z "$EXT" ]; then
    echo "PROGRAM, MODE, and EXT must be set before sourcing _exec_c_cpp.sh." >&2
    exit 1
fi

case "$MODE" in
SYNTAX)
    if [ "${#SC_RUNS[@]}" -eq 0 ]; then
        echo "SC_RUNS must be set for MODE=SYNTAX." >&2
        exit 1
    fi

    # Leading dash-prefixed args (before the first file) are extra compiler
    # options; everything from the first non-dash arg on is a file.
    EXTRA_OPTS=()
    FILES=()
    for a in "$@"; do
        if [ "${#FILES[@]}" -eq 0 ] && [ "${a#-}" != "$a" ]; then
            EXTRA_OPTS+=("$a")
        else
            FILES+=("$a")
        fi
    done

    if [ "${#FILES[@]}" -eq 0 ]; then
        echo "Usage: ${PROGRAM}.sh [extra compiler options, e.g. -Iinclude/dir ...] <file.${EXT}> [file2.${EXT} ...]" >&2
        exit 2
    fi

    status=0
    for f in "${FILES[@]}"; do
        for run in "${SC_RUNS[@]}"; do
            cvar="${run%%:*}"; rest="${run#*:}"
            std="${rest%%:*}"; rest="${rest#*:}"
            lang="${rest%%:*}"; label="${rest#*:}"
            compiler="${!cvar}"

            echo "[$label] $f"
            out=$("$compiler" -x "$lang" -std="$std" -fsyntax-only "${EXTRA_OPTS[@]}" "$f" 2>&1)
            rc=$?
            printf '%s\n' "$out" | _c_cpp_filter
            if [ $rc -ne 0 ]; then
                status=1
            fi
        done
    done
    exit $status
    ;;

DIFF)
    if [ -z "$CD_COMPILER_VAR" ] || [ -z "$CD_DIFF_MODE" ] || [ -z "$CD_STD" ] || [ -z "$CD_LANG" ]; then
        echo "CD_COMPILER_VAR, CD_DIFF_MODE, CD_STD, CD_LANG must be set for MODE=DIFF." >&2
        exit 1
    fi

    ARGS=("$@")
    EXTRA_OPTS=()
    for i in "${!ARGS[@]}"; do
        if [ "${ARGS[$i]}" = "--" ]; then
            EXTRA_OPTS=("${ARGS[@]:$((i+1))}")
            ARGS=("${ARGS[@]:0:$i}")
            break
        fi
    done

    compiler="${!CD_COMPILER_VAR}"

    if [ "${#ARGS[@]}" -eq 2 ]; then
        exec python3 "$DIR/_exec_c_cpp.py" "$compiler" "$CD_DIFF_MODE" "$CD_STD" "$CD_LANG" \
            "${ARGS[0]}" "${ARGS[1]}" -- "${EXTRA_OPTS[@]}"
    elif [ "${#ARGS[@]}" -eq 3 ]; then
        exec python3 "$DIR/_exec_c_cpp.py" "$compiler" "$CD_DIFF_MODE" "$CD_STD" "$CD_LANG" \
            --batch "${ARGS[0]}" "${ARGS[1]}" "${ARGS[2]}" -- "${EXTRA_OPTS[@]}"
    else
        echo "Usage: ${PROGRAM}.sh <original.${EXT}> <formatted.${EXT}>" >&2
        echo "       ${PROGRAM}.sh <original_base_dir> <formatted_base_dir> <${EXT}_rel_path_file_list.txt>" >&2
        echo "       (extra compiler options, e.g. -Iinclude/dir, may be appended after '--')" >&2
        exit 2
    fi
    ;;

*)
    echo "MODE must be SYNTAX or DIFF (got: $MODE)" >&2
    exit 1
    ;;
esac
