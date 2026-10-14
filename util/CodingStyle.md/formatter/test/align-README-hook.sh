#!/usr/bin/env bash
#
# Re-runs align-README.py on test/README.txt, but writes the file back only when the alignment
# actually changes something (so an already-aligned file is never touched). Stateless: runs the
# aligner on a temporary copy and compares, so it works no matter how README.txt was edited
# (an editor, `sed`, a Python one-off, ...). Used as a Claude Code PostToolUse hook (see
# ../.claude/settings.json); safe to run by hand too.
#

set -e

dir="$( cd "$(dirname "$0")" && pwd )"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

cp "$dir/README.txt" "$dir/align-README.py" "$tmp/"
( cd "$tmp" && python3 align-README.py > /dev/null )

if ! cmp -s "$tmp/README.txt" "$dir/README.txt"; then
    cp "$tmp/README.txt" "$dir/README.txt"
    echo '{"systemMessage": "test/README.txt re-aligned by align-README.py"}'
fi
