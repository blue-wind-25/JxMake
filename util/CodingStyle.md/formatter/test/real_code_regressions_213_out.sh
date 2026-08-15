#!/usr/bin/env zsh
#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#
# zsh shebang (not bash/sh/dash/ksh) -> formatter must skip this file
# entirely (byte-identical inp/out). See STATE_TOOLING.md and README.md's
# Bash Known Limitations for the shebang-based gate this fixture proves.
#

setopt extended_glob

files=(*.txt(|.git))
if [[ -n "$files" ]];then
for f in $files;do
echo "$f"|cat
done
fi
