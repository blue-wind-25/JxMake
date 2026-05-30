#!/usr/bin/env bash
# _port.sh
# Include file -- do not execute directly.
# Provides derive_port(): maps a Java binary's major version to its daemon port
# (major * 1000).  Sourced by javac-client.sh, javac-daemon-wrapper.sh,
# start-compile-server.sh, and stop-compile-server.sh.
#
# Usage:
#   . "$(dirname "${BASH_SOURCE[0]}")/_port.sh"
#   PORT=$(derive_port /path/to/java)

# Derive port from Java major version: major * 1000
# Usage: derive_port <java-binary>
# Prints the derived port number on success; prints an error and returns 1 on failure.
derive_port() {
    local java_bin="$1"
    local ver_str major
    ver_str=$("$java_bin" -version 2>&1 | head -1)
    if [[ "$ver_str" =~ \"1\.([0-9]+) ]]; then
        major="${BASH_REMATCH[1]}"
    elif [[ "$ver_str" =~ \"([0-9]+) ]]; then
        major="${BASH_REMATCH[1]}"
    else
        echo "ERROR: Cannot parse Java version from: $ver_str" >&2
        return 1
    fi
    echo $((major * 1000))
}
