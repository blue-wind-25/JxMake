#!/usr/bin/env bash
# _javac_args.sh
# Include file -- do not execute directly.
# Provides path-absolutization helpers and process_javac_args() for converting
# relative paths in javac arguments to absolute paths before forwarding to the
# compile-server daemon (which runs in a fixed CWD).
# Sourced by javac-client.sh and javac-daemon-wrapper.sh.
#
# Usage:
#   . "$(dirname "${BASH_SOURCE[0]}")/_javac_args.sh"
#   # CLIENT_CWD must be set before calling any function below.
#   CLIENT_CWD="$(pwd)"
#   process_javac_args "$@"

# Convert a single path to absolute, using CLIENT_CWD as the base.
_abs()    { [[ "$1" == /* ]] && printf '%s' "$1" || printf '%s/%s' "$CLIENT_CWD" "$1"; }

# Convert a colon-separated classpath string to absolute paths.
_abs_cp() { local r="" s="" e; local IFS=':'; for e in $1; do r+="${s}$(_abs "$e")"; s=":"; done; printf '%s' "$r"; }

# Rewrite all path-bearing javac arguments to absolute paths, printing one
# argument per line for safe consumption by the caller.
process_javac_args() {
    local nxt="" out=()
    for a; do
        if [[ -n "$nxt" ]]; then
            if [[ "$nxt" == cp ]]; then
                out+=("$(_abs_cp "$a")")
            else
                out+=("$(_abs "$a")")
            fi
            nxt=""
        else
            case "$a" in
                -d|-s|-h)
                    out+=("$a"); nxt=single ;;
                -cp|-classpath|--class-path|-sourcepath|--source-path|\
                -processorpath|--processor-path|-bootclasspath|--boot-class-path|\
                -extdirs|-endorseddirs|-modulepath|--module-path|-mp|-p|\
                --upgrade-module-path)
                    out+=("$a"); nxt=cp ;;
                @*)  out+=("@$(_abs "${a:1}")") ;;
                -*)  out+=("$a") ;;
                *)   out+=("$(_abs "$a")") ;;
            esac
        fi
    done
    if [[ ${#out[@]} -gt 0 ]]; then printf '%s\n' "${out[@]}"; fi
}
