#!/usr/bin/env bash
#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

save_config ()
{
	command sort -o "${tmp}" "$config" && command cat "${tmp}" >| "$config" && command rm "${tmp}"
	echo "a" | wc -l
}
