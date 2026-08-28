#!/usr/bin/env bash

#####
##### Copyright (C) 2022-2026 Aloysius Indrayanto
#####
##### This file is part of the JxMake program, see LICENSE file for the license details.
#####

# cp_run.sh
# Runs a java/javac-family tool with a classpath assembled from individual
# entries, instead of a single pre-joined ':'/';'-separated string.
#
# Why: baking a ';'-joined classpath into a Make variable and relying on the
# recipe's shell to preserve embedded quotes around it is fragile on native
# GNU Make for Windows -- its own command-line reconstruction before handing
# the recipe off to the child shell has known quoting quirks with that
# combination of characters. Passing each classpath entry as its own argv
# word sidesteps that: Make only ever has to pass plain space-separated
# tokens (its native list form), and the ':'/';' joining happens here, in a
# real bash process, using bash's own well-understood quoting.
#
# Usage:
#    cp_run.sh <tool> [<tool args...>] -cp <entry1> <entry2> ... -- <tool args...>
#
# Everything before "-cp" and everything after "--" are passed through to
# <tool> unchanged; everything between "-cp" and "--" is joined with the
# platform classpath separator and passed as a single "-cp" argument.

set -euo pipefail

SEP=":"
case "$(uname -s)" in
MINGW* | MSYS* | CYGWIN*)
    SEP=";"
    ;;
esac

TOOL_ARGS_PRE=()
CP_ENTRIES=()
TOOL_ARGS_POST=()
STATE=pre

for ARG in "$@"; do
    case "$STATE" in
    pre)
            if [ "$ARG" = "-cp" ]; then STATE=cp; else TOOL_ARGS_PRE+=("$ARG"); fi
        ;;
    cp)
            if [ "$ARG" = "--" ]; then STATE=post; else CP_ENTRIES+=("$ARG"); fi
        ;;
    post)
            TOOL_ARGS_POST+=("$ARG")
        ;;
    esac
done

CP=""
for ENTRY in "${CP_ENTRIES[@]}"; do
    CP="${CP:+$CP$SEP}$ENTRY"
done

exec "${TOOL_ARGS_PRE[@]}" -cp "$CP" "${TOOL_ARGS_POST[@]}"
