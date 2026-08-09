#!/usr/bin/env bash
#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

format_header ()
{
  case "$output" in
  raw)
    case "$level" in
    1) printf '%s\n%s\n\n' "$header" "$(printf '%.0s=' {1..${#header}})" ;;
    *) printf '%s:\n\n' "$header" ;;
    esac ;;
  text)
    printf '%s\n\n' "$header"
    ;;
  esac
}
