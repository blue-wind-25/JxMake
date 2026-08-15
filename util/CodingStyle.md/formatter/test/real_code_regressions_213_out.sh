#!/usr/bin/env zsh
#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

setopt extended_glob

files=(*.txt(|.git))
if [[ -n "$files" ]];then
for f in $files;do
echo "$f"|cat
done
fi
