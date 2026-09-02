#!/usr/bin/env bash
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is distributed under the Apache License, Version 2.0.
# See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
#
# ps1_syntax_check.sh - syntax checker for PowerShell source files.
#
# Uses PowerShell 7's own parser (System.Management.Automation.Language.Parser::ParseFile) to
# report parse errors without executing the script.
#
# Command used to invoke pwsh (override in your shell/CI environment if pwsh or its runtime
# library path live somewhere other than the defaults below):
: "${PWSH_LD_LIBRARY_PATH:=/opt/gcc-12.2.0/lib64}"
: "${PWSH:=/opt/powershell-7.4.19-linux-x64/pwsh}"
#     LD_LIBRARY_PATH=$PWSH_LD_LIBRARY_PATH:$LD_LIBRARY_PATH $PWSH ...
#
# Usage:
#     ps1_syntax_check.sh <file.ps1> [file2.ps1 ...]
#
# Exit 0 if all files parse successfully, 1 if one or more files contain
# syntax errors, 2 if the command-line usage is invalid.

if [ "$#" -lt 1 ]; then
    echo "Usage: ps1_syntax_check.sh <file.ps1> [file2.ps1 ...]" >&2
    exit 2
fi

status=0

for f in "$@"; do
    err=$(LD_LIBRARY_PATH="${PWSH_LD_LIBRARY_PATH}:${LD_LIBRARY_PATH}" "$PWSH" -NoProfile -File /dev/stdin "$f" <<'PS1_EOF' 2>&1 1>/dev/null
$ErrorActionPreference = "Stop"
$path = $args[0]
$tokens = $null
$errors = $null
[System.Management.Automation.Language.Parser]::ParseFile($path, [ref]$tokens, [ref]$errors) | Out-Null
if ($errors.Count -gt 0) {
    foreach ($e in $errors) {
        Write-Error $e.ToString()
    }
    exit 1
}
PS1_EOF
)
    # The pwsh binary on this system emits a harmless dynamic-linker warning on every
    # invocation ("no version information available (required by .../pwsh)"); strip it so it
    # is not mistaken for a parse error.
    err=$(printf '%s\n' "$err" | grep -v 'no version information available')
    if [ -n "$err" ]; then
        status=1
        printf '%s\n' "$err" >&2
    fi
done

exit "$status"
