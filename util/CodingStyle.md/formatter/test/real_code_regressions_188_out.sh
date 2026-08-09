#!/usr/bin/env bash
#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

__classify_word ()
{
	case "$word" in
	\(\))
	    : skip parens of shell function definition
	    ;;
	\'*)
	    : skip opening quote after sh -c
	    ;;
	*)
	    cur="$word"
	    break
	esac
}

_git_checkout ()
{
	case "$cur" in
	*)
	    if [ -n "$(__git_find_on_cmdline "-b -B -d --detach --orphan")" ]; then
	    __git_complete_refs --mode="refs"
	    elif [ -n "$(__git_find_on_cmdline "--track")" ]; then
	    __git_complete_refs --mode="remote-heads"
	    else
	    __git_complete_refs $dwim_opt --mode="refs"
	    fi
	    ;;
	esac
}
