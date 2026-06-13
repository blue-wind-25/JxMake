#!/usr/bin/env python3.12
#
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake program, see LICENSE file for the license details.
#

"""Simple directory browser with Markdown rendering and source code syntax highlighting HTTP server."""


import argparse
import calendar
import datetime
import email.utils
import html
import mimetypes
import os
import posixpath
import re
import sys
import urllib.parse
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer


from markdown_it import MarkdownIt
from mdit_py_plugins.gfm import gfm_plugin
from mdit_py_plugins.tasklists import tasklists_plugin
from pygments import highlight as _hl
from pygments.formatters import HtmlFormatter
from pygments.lexer import RegexLexer, bygroups
from pygments.lexers import get_lexer_for_filename, TextLexer
from pygments.token import (
    Comment, Keyword, Name, Number, Operator,
    Punctuation, String, Text,
)
from pygments.util import ClassNotFound


# ---------------------------------------------------------------------------
# GNU Makefile lexer
# ---------------------------------------------------------------------------

class _MakefileLexer(RegexLexer):
    name = "Makefile"
    aliases = ["makefile", "make"]
    filenames = ["Makefile", "makefile"]

    # GNU Make built-in functions — sorted longest-first so e.g. filter-out
    # is tried before filter and findstring before find.
    _BUILTIN_FUNCS = (
        "findstring", "filter-out", "firstword",  "addprefix",  "addsuffix",
        "patsubst",   "wordlist",   "lastword",    "realpath",   "basename",
        "wildcard",   "abspath",    "foreach",     "warning",    "origin",
        "notdir",     "flavor",     "suffix",      "filter",     "subst",
        "strip",      "words",      "value",       "shell",      "error",
        "guile",      "sort",       "word",        "join",       "info",
        "call",       "eval",       "file",        "dir",        "and",
        "let",        "if",         "or",
    )
    _FUNC_RE = (
        r"(?:" + "|".join(_BUILTIN_FUNCS) + r")"
        r"(?=[ \t,])"
    )

    tokens = {

        # =======================================================================
        # root — top-level file context
        # =======================================================================
        "root": [
            (r"#.*$", Comment),

            # Line-start directives
            (r"^(\s*)(-?include|sinclude)\b",
             bygroups(Text, Keyword)),
            (r"^(\s*)(override|export|unexport|undefine|vpath)\b",
             bygroups(Text, Keyword)),
            (r"^(\s*)(ifdef|ifndef|ifeq|ifneq|else|endif)\b",
             bygroups(Text, Keyword)),
            (r"^(\s*)(define)\b",  bygroups(Text, Keyword), "define_block"),
            (r"^(\s*)(endef)\b",   bygroups(Text, Keyword)),

            # Special targets  .PHONY: …
            (
                r"^\s*(\.(PHONY|SUFFIXES|DEFAULT|PRECIOUS|INTERMEDIATE|SECONDARY"
                r"|SECONDEXPANSION|DELETE_ON_ERROR|EXPORT_ALL_VARIABLES"
                r"|NOTPARALLEL|ONESHELL))(:)",
                bygroups(Name.Constant, Name.Constant, Operator),
            ),

            # Variable assignment — push assign_value so the RHS gets String colour
            (r"^(\s*[A-Za-z0-9_][A-Za-z0-9_.-]*)(\s*)([:+?!]?=)",
             bygroups(Name.Variable, Text, Operator), "assign_value"),

            # Target-specific variable: TARGET: VAR [:+?!]= value
            (r"^([^\s:#=]+)(:\s*)([A-Za-z0-9_][A-Za-z0-9_.-]*)(\s*)([:+?!]?=)",
             bygroups(Name.Label, Operator, Name.Variable, Text, Operator), "assign_value"),

            # Target / pattern rule  name:  or  %.o:
            (r"^([^\s:]+)(:+)", bygroups(Name.Label, Operator)),

            # Recipe line (leading tab)
            (r"^\t", Keyword, "recipe"),

            # Expansions
            (r"\$\$",               Text),
            (r"\$[@<\^\?\*\+\|%]",  Name.Variable),
            (r"\$\(",               Operator, "make_expr"),
            (r"\$\{",               Operator, "make_expr_brace"),

            # Whitespace — keep \n separate so ^\t can match on the next iteration
            (r"[^\S\n]+",  Text),
            (r"\n",        Text),
            (r".",         Text),
        ],

        # =======================================================================
        # assign_value — right-hand side of VAR := …
        # Text is String so it looks different from unrelated root-level text.
        # =======================================================================
        "assign_value": [
            (r"\n",    Text, "#pop"),
            (r"\\\n",  Text),
            (r"\$\$",               Text),
            (r"\$[@<\^\?\*\+\|%]",  Name.Variable),
            (r"\$\(",               Operator, "make_expr"),
            (r"\$\{",               Operator, "make_expr_brace"),
            (r"[^\$\\\n]+",         String),
            (r".",                  String),
        ],

        # =======================================================================
        # recipe — text after a leading tab; Make expansions still active
        # =======================================================================
        "recipe": [
            (r"\n",   Text, "#pop"),
            (r"\\\n", Text),
            (r"\$\$",               Text),
            (r"\$[@<\^\?\*\+\|%]",  Name.Variable),
            (r"\$\(",               Operator, "make_expr"),
            (r"\$\{",               Operator, "make_expr_brace"),
            (r"[^\$\\\n]+",         Keyword),
            (r".",                  Keyword),
        ],

        # =======================================================================
        # make_call_name — the callee identifier right after $(call …
        # Highlighted as Name.Function so it stands out from plain variables.
        # =======================================================================
        "make_call_name": [
            (r"[ \t]+",                        Text),
            (r"[A-Za-z0-9_][A-Za-z0-9_.-]*",  Name.Function, "#pop"),
            (r".",                             String, "#pop"),   # bail gracefully
        ],

        # =======================================================================
        # make_expr — inside $(…)
        # =======================================================================
        "make_expr": [
            # Automatic-var directional suffixes: $(@D) $(@F) $(<F) …
            (r"[@<\^\?\*\+\|%][DF]", Name.Variable),
            # call needs special treatment: its first arg is the callee name
            (r"call(?=[ \t,])", Name.Builtin, "make_call_name"),
            # Other built-in function names
            (_FUNC_RE, Name.Builtin),
            # Variable / parameter reference
            (r"[A-Za-z0-9_@<\^\?\*\+\|%][A-Za-z0-9_.-]*", Name.Variable),
            # Nested $(…) and ${…}
            (r"\$\(",               Operator, "#push"),
            (r"\$\{",               Operator, "make_expr_brace"),
            (r"\$[@<\^\?\*\+\|%]",  Name.Variable),
            (r"\$\$",               Text),
            # Closing and separators
            (r"\)",                 Operator, "#pop"),
            (r",",                  Punctuation),
            # Argument text — String so it differs from outside-expansion Text
            (r"[^$(),{}\n]+",       String),
            (r".",                  String),
        ],

        # =======================================================================
        # make_expr_brace — inside ${…}  (same logic, closes with })
        # =======================================================================
        "make_expr_brace": [
            (r"[@<\^\?\*\+\|%][DF]", Name.Variable),
            (r"call(?=[ \t,])", Name.Builtin, "make_call_name"),
            (_FUNC_RE, Name.Builtin),
            (r"[A-Za-z0-9_@<\^\?\*\+\|%][A-Za-z0-9_.-]*", Name.Variable),
            (r"\$\(",               Operator, "make_expr"),
            (r"\$\{",               Operator, "#push"),
            (r"\$[@<\^\?\*\+\|%]",  Name.Variable),
            (r"\$\$",               Text),
            (r"\}",                 Operator, "#pop"),
            (r",",                  Punctuation),
            (r"[^$(),{}\n]+",       String),
            (r".",                  String),
        ],

        # =======================================================================
        # define_block — multi-line variable body between define … endef
        # =======================================================================
        "define_block": [
            (r"^(\s*)(endef)\b",     bygroups(Text, Keyword), "#pop"),
            (r"\$\$",               Text),
            (r"\$[@<\^\?\*\+\|%]",  Name.Variable),
            (r"\$\(",               Operator, "make_expr"),
            (r"\$\{",               Operator, "make_expr_brace"),
            (r"[^\$\n]+",           String),
            (r"\n",                 Text),
            (r".",                  String),
        ],
    }


_MAKEFILE_FILENAMES = frozenset({
    "Makefile", "makefile", "GNUmakefile", "BSDmakefile",
})

# ---------------------------------------------------------------------------
# JxMake lexer
# ---------------------------------------------------------------------------

class _JxMakeLexer(RegexLexer):
    """Syntax lexer for the JxMake scripting language (.jxm / JxMakeFile)."""

    name = "JxMake"
    aliases = ["jxmake", "jxm"]
    filenames = ["JxMakeFile", "*.jxm"]

    # -- Token type aliases for readability --
    _KW   = Keyword
    _KWD  = Keyword.Declaration   # block-opening keywords (target, function, …)
    _KWE  = Keyword.Reserved      # block-closing keywords (endtarget, endfunction, …)
    _KWC  = Keyword.Pseudo        # compiler directives (:::include, :::pragma, …)
    _OP   = Operator
    _CM   = Comment
    _CMB  = Comment.Multiline
    _STS  = String.Single
    _STD  = String.Double
    _STR_ = String.Backtick       # raw / backtick string
    _NUM  = Number
    _VAR  = Name.Variable
    _SPV  = Name.Constant         # __special_var__
    _FUN  = Name.Function
    _BIF  = Name.Builtin          # built-in $func(…)
    _SHC  = Name.Label            # @ shell-command lines

    # Block/flow keywords (statement-level, not block-openers/closers)
    _KEYWORDS = (
        "local", "const", "unset", "deprecate", "by",
        "depload", "sdepload",
        "extradep",
        "echo", "echoln",
        "return", "break", "continue",
        "jxwait",
        "inc", "dec", "add", "sub", "mul", "div", "mod",
        "abs", "neg", "shl", "shr", "min", "max",
        "not", "and", "or", "xor",
        "eval",
        "label", "goto", "sgoto",
        "if", "elif", "else", "endif",
        "for", "endfor", "to", "step",
        "foreach", "endforeach", "in", "skip",
        "while", "endwhile",
        "do", "whilst",
        "repeat", "until",
        "loop", "endloop",
        "jxmake",
        "supersede", "deprecated",
    )

    # Block-opening keywords
    _BLOCK_OPEN = ("target", "function")

    # Block-closing keywords
    _BLOCK_CLOSE = ("endtarget", "endfunction")

    # Assignment operators (order matters: longer first)
    _ASSIGN_OPS = (
        r":?\+:?=",   # +=  :+=  +:=  (concatenation lazy / direct)
        r":?\?:?=",   # ?=  ?:=  :?=  (if-not-set lazy / direct)
        r":?=",       # =  :=
    )

    # Condition operators
    _COND_OPS = (
        r"&[!=]=",    # &== &!=
        r"[<>]=?",    # < <= > >=
        r"[!=]=",     # == !=
        r"!&?",       # ! !&
        r"&",         # &
    )

    # -----------------------------------------------------------------------
    # Shared helper patterns
    # -----------------------------------------------------------------------

    # Path shortcuts: ~${V}  ^${V}  -${V}  and same for $[V]
    _PATH_SHORTCUTS = [
        (r"[-~^]\$\{[\w./]+\}", _VAR),
        (r"[-~^]\$\[[\w^*?+%~:]+\]", _VAR),
    ]

    # Partn/partnm shortcuts: ${V}<…>  $[V]<…>
    # Emit the var part as _VAR and the opening '<' as Punctuation, then enter partn_index.
    _PARTN_SHORTCUTS = [
        (r"(\$\{[\w./]+\})(<)",      bygroups(_VAR, Punctuation), "partn_index"),
        (r"(\$\[[\w^*?+%~:]+\])(<)", bygroups(_VAR, Punctuation), "partn_index"),
    ]

    # Variable / auto-var inside strings: ${…} and $[…]
    # Use _VAR so string interpolations render identically to root-context vars.
    # Partn shortcuts are reused directly so the definition isn't duplicated.
    _VAR_IN_STR = [
        *_PARTN_SHORTCUTS,
        (r"\$\{\^?[\w./]+(?:/[^}]*)?\}", _VAR),   # ${var}  ${^ref}  ${var/re/repl/}
        (r"\$\[[\w^*?+%~:]+(?:/[^]]*)?\]", _VAR), # $[auto]  $[auto/re/repl/]
    ]

    # Stack shortcut {{ … }}  and set-creation { … }
    _STACK_SET = [
        (r"\{\{", Punctuation, "stack_shortcut"),
        (r"\{",   Punctuation, "set_creation"),
    ]

    tokens = {

        # ===================================================================
        # root state
        # ===================================================================
        "root": [
            # Line continuation
            (r"\\\n", Text),

            # Block comments  (* … *)
            (r"\(\*", _CMB, "block_comment"),

            # Line comments
            (r"#.*$", _CM),

            # Compiler directives  :::pragma  :::include[_once]  :::sinclude[_once]
            (r":::(?:s?include(?:_once)?|pragma)\b", _KWC),

            # Shell-command prefixes: -+@  ?@  +@  -@  @
            # Special named @-commands: @clrenv @delenv @setenv @addenv @sstdin @jxmake
            (r"(?:-\+|[?+\-])?@(?=\s*(clrenv|delenv|setenv|addenv|sstdin|jxmake)\b)",
             _SHC, "at_named"),
            (r"(?:-\+|[?+\-])?@", _SHC, "at_command"),

            # +jxmake keyword (must precede generic operator matching)
            (r"\+jxmake\b", _KW),

            # Highlight function definition names
            (r"\b(function)\b(\s+)([A-Za-z_][\w./\-]*)", bygroups(_KWD, Text, _FUN)),

            # Block-opening keywords
            (r"\b(" + "|".join(_BLOCK_OPEN) + r")\b", _KWD),

            # Block-closing keywords
            (r"\b(" + "|".join(_BLOCK_CLOSE) + r")\b", _KWE),

            # Other keywords
            (r"\b(" + "|".join(_KEYWORDS) + r")\b", _KW),

            # .macro  .endmacro  .undefmacro  .option  .PHONY etc.
            (r"\.(macro|endmacro|undefmacro|option)\b", _KWC),
            (r"\.[A-Z][A-Z_]+\b", Name.Constant),

            # __special_var__
            (r"__[A-Za-z0-9_]+__", _SPV),

            # Combining  ."…"  and flattening  :"…"  strings
            # Emit the prefix operator only; the '"' rule below handles the opening quote
            (r'[.:](?=")', _OP),

            # String literals  (multiline [["…"]] must precede plain ")
            (r'\[\["', _STD, "multiline_double_string"),
            (r"`", _STR_, "raw_string"),
            (r"'",  _STS,  "single_string"),
            (r'"',  _STD,  "double_string"),

            # Path shortcuts ~${V}  ^${V}  -${V}
            *_PATH_SHORTCUTS,

            # Indirect assignment LHS  ^var-name  (§4.1 / §9.7)
            (r"\^[A-Za-z_][\w.]*", _VAR),

            # Partn shortcuts ${V}<n>
            *_PARTN_SHORTCUTS,

            # Indirect ref  ${^var}
            (r"\$\{\^[\w./]+\}", _VAR),

            # Auto-vars  $[auto]
            (r"\$\[[\w^*?+%~:]+(?:/[^]]*)?\]", _VAR),

            # Var-eval  ${var}
            (r"\$\{[\w./]+(?:/[^}]*)?\}", _VAR),

            # Built-in / user function calls  -$func(  or  $func(
            (r"-?\$[A-Za-z_]\w*(?=\s*\()", _BIF),

            # Integers: hex, octal, decimal
            (r"0x[0-9A-Fa-f]+", _NUM),
            (r"0o[0-7]+", _NUM),
            (r"-?\d+", _NUM),

            # Assignment operators (longer first)
            (r"|".join(_ASSIGN_OPS), _OP),

            # Condition operators
            (r"|".join(_COND_OPS), _OP),

            # Other operators / punctuation
            (r"[,:()\[\]<>]", Punctuation),
            (r"[.;\\]", Punctuation),

            # Stack {{ }} and set { }
            *_STACK_SET,

            # Whitespace
            (r"\s+", Text),

            # Identifiers / bare-words
            (r"[A-Za-z_][\w./\-]*", Name),

            # Catch-all
            (r".", Text),
        ],

        # ===================================================================
        # block_comment  (*  … *)  — may be nested
        # Each (* pushes this state again; *) pops one level.
        # ===================================================================
        "block_comment": [
            (r"\(\*", _CMB, "block_comment"),   # nested open → push another level
            (r"\*\)", _CMB, "#pop"),             # close → pop one level
            (r"[^(*\n]+", _CMB),
            (r"[(*]",     _CMB),
            (r"\n",       _CMB),
        ],

        # ===================================================================
        # at_command — all text after @ until # or end-of-line is the command
        # ===================================================================
        "at_command": [
            (r"#.*$", _CM, "#pop"),          # # = end-of-args / comment
            (r"\\\n", _SHC),                 # line continuation
            (r"\n",   Text, "#pop"),
            # Variable interpolation inside @-lines
            *_PATH_SHORTCUTS,
            *_PARTN_SHORTCUTS,
            (r"\$\{\^[\w./]+\}", _VAR),
            (r"\$\[[\w^*?+%~:]+(?:/[^]]*)?\]", _VAR),
            (r"\$\{[\w./]+(?:/[^}]*)?\}", _VAR),
            (r"-?\$[A-Za-z_]\w*(?=\s*\()", _BIF),
            (r"[^\\\n#$]+", _SHC),
            (r"\$", _SHC),
        ],

        # ===================================================================
        # at_named — named @-modifiers: @clrenv @delenv @setenv @addenv @sstdin @jxmake
        # ===================================================================
        "at_named": [
            (r"\b(clrenv|delenv|setenv|addenv|sstdin|jxmake)\b", Keyword.Pseudo),
            (r"#.*$", _CM, "#pop"),
            (r"\\\n", _SHC),
            (r"\n",   Text, "#pop"),
            *_PATH_SHORTCUTS,
            *_PARTN_SHORTCUTS,
            (r"\$\{\^[\w./]+\}", _VAR),
            (r"\$\[[\w^*?+%~:]+(?:/[^]]*)?\]", _VAR),
            (r"\$\{[\w./]+(?:/[^}]*)?\}", _VAR),
            (r"-?\$[A-Za-z_]\w*(?=\s*\()", _BIF),
            (r"[^\\\n#$]+", _SHC),
            (r"\$", _SHC),
        ],

        # ===================================================================
        # raw_string  `…`  — no escapes, no variable expansion
        # ===================================================================
        "raw_string": [
            (r"`", _STR_, "#pop"),
            (r"[^`\n]+", _STR_),
            (r"\n", _STR_),
        ],

        # ===================================================================
        # single_string  '…'  — only \' and \\ recognised
        # ===================================================================
        "single_string": [
            (r"'",      _STS, "#pop"),
            (r"\\'",    String.Escape),
            (r"\\\\",   String.Escape),
            (r"[^'\\\n]+", _STS),
            (r"\\.",    _STS),   # other \X stored literally
            (r"\n",     _STS),
        ],

        # ===================================================================
        # double_string  "…"  — variable expansion + full escape sequences
        # (also used for ."…" and :"…" — the prefix operator is consumed in root)
        # ===================================================================
        "double_string": [
            (r'"',      _STD, "#pop"),
            # Escape sequences
            (r'\\[tvrnfb\'"\\$~`\-+?]', String.Escape),
            (r'\\o[0-7]{3}',            String.Escape),
            (r'\\d\d{3}',               String.Escape),
            (r'\\x[0-9A-Fa-f]{2}',      String.Escape),
            (r'\\u[0-9A-Fa-f]{4}',      String.Escape),
            (r'\\U[0-9A-Fa-f]{6}',      String.Escape),
            # Variable / auto-var interpolation
            *_VAR_IN_STR,
            # Everything else
            (r'[^"\\$\n]+', _STD),
            (r'\$',         _STD),
            (r'\n',         _STD),
        ],

        # ===================================================================
        # partn_index  — content of <…> in ${V}<…> / $[V]<…> shortcuts
        # Numbers → _NUM, identifiers → _VAR; shared by root and string states.
        # ===================================================================
        "partn_index": [
            (r">",               Punctuation, "#pop"),
            (r",",               Punctuation),
            (r":",               Punctuation),
            (r"0x[0-9A-Fa-f]+",  _NUM),
            (r"0o[0-7]+",        _NUM),
            (r"-?\d+",           _NUM),
            (r"[A-Za-z_][\w.]*", _VAR),
            (r"\s+",             Text),
            (r".",               Text, "#pop"),   # bail gracefully on unexpected input
        ],

        # ===================================================================
        # multiline_double_string  [["…"]]
        # Opened by [["; closed by "]] alone on a line.
        # Newlines are content; same variable expansion as double_string.
        # ===================================================================
        "multiline_double_string": [
            (r'"]]',    _STD, "#pop"),
            # Escape sequences (same as double_string)
            (r'\\[tvrnfb\'"\\$~`\-+?]', String.Escape),
            (r'\\o[0-7]{3}',            String.Escape),
            (r'\\d\d{3}',               String.Escape),
            (r'\\x[0-9A-Fa-f]{2}',      String.Escape),
            (r'\\u[0-9A-Fa-f]{4}',      String.Escape),
            (r'\\U[0-9A-Fa-f]{6}',      String.Escape),
            # Variable / auto-var interpolation
            *_VAR_IN_STR,
            # Body — newlines preserved; lone " (not "]]") is plain content
            (r'[^"\\$]+',    _STD),
            (r'"(?!\]\])',   _STD),
            (r'\$',          _STD),
        ],

        # ===================================================================
        # stack_shortcut  {{ term, … }}
        # ===================================================================
        "stack_shortcut": [
            (r"\}\}", Punctuation, "#pop"),
            (r",",    Punctuation),
            # Nest same constructs as root (simplified — no block keywords)
            (r"\(\*",  _CMB, "block_comment"),
            (r"#.*$",  _CM),
            *_PATH_SHORTCUTS,
            *_PARTN_SHORTCUTS,
            (r"\$\{\^[\w./]+\}", _VAR),
            (r"\$\[[\w^*?+%~:]+(?:/[^]]*)?\]", _VAR),
            (r"\$\{[\w./]+(?:/[^}]*)?\}", _VAR),
            (r"-?\$[A-Za-z_]\w*(?=\s*\()", _BIF),
            (r'[.:](?=")', _OP),
            (r"`", _STR_, "raw_string"),
            (r"'",  _STS,  "single_string"),
            (r'"',  _STD,  "double_string"),
            (r"__[A-Za-z0-9_]+__", _SPV),
            (r"-?\d+|0x[0-9A-Fa-f]+|0o[0-7]+", _NUM),
            (r"\s+", Text),
            (r"[A-Za-z_][\w./\-]*", Name),
            (r".", Text),
        ],

        # ===================================================================
        # set_creation  { term … }
        # ===================================================================
        "set_creation": [
            (r"\}", Punctuation, "#pop"),
            (r"\(\*",  _CMB, "block_comment"),
            (r"#.*$",  _CM),
            *_PATH_SHORTCUTS,
            *_PARTN_SHORTCUTS,
            (r"\$\{\^[\w./]+\}", _VAR),
            (r"\$\[[\w^*?+%~:]+(?:/[^]]*)?\]", _VAR),
            (r"\$\{[\w./]+(?:/[^}]*)?\}", _VAR),
            (r"-?\$[A-Za-z_]\w*(?=\s*\()", _BIF),
            (r'[.:](?=")', _OP),
            (r"`", _STR_, "raw_string"),
            (r"'",  _STS,  "single_string"),
            (r'"',  _STD,  "double_string"),
            (r"__[A-Za-z0-9_]+__", _SPV),
            (r"-?\d+|0x[0-9A-Fa-f]+|0o[0-7]+", _NUM),
            (r"\s+", Text),
            (r"[A-Za-z_][\w./\-]*", Name),
            (r".", Text),
        ],
    }


_JXMAKE_FILENAMES = frozenset({"JxMakeFile"})

_MAX_HIGHLIGHT_BYTES = 512 * 1024   # skip syntax highlighting for files larger than this

# ---------------------------------------------------------------------------
# Pygments formatters + CSS
# ---------------------------------------------------------------------------

_formatter     = HtmlFormatter(style="default", nowrap=True)
_src_formatter = HtmlFormatter(style="default", linenos="table")

# Light-mode Pygments CSS, brace-escaped for str.format().
_PYGMENTS_CSS = (
    _src_formatter.get_style_defs(".highlight")
    .replace("{", "{{")
    .replace("}", "}}")
)
# Dark-mode token CSS scoped under .dark, using monokai palette.
_DARK_PYGMENTS_CSS = (
    HtmlFormatter(style="monokai").get_style_defs(".dark .highlight")
    .replace("{", "{{")
    .replace("}", "}}")
)

_TOGGLE_HTML = (
    '<button class="theme-toggle" title="Toggle dark mode"'
    " onclick=\"(function(){var d=document.documentElement.classList.toggle('dark');"
    "localStorage.setItem('theme',d?'dark':'light');})()\""
    "></button>"
)

# ---------------------------------------------------------------------------
# Markdown renderer
# ---------------------------------------------------------------------------

_md = (
    MarkdownIt("commonmark")
    .use(gfm_plugin)
    .use(tasklists_plugin)
)

# ---------------------------------------------------------------------------
# HTML page template
# ---------------------------------------------------------------------------

_TEMPLATE = (
"<!DOCTYPE html>\n"
"<html lang=\"en\">\n"
"<head>\n"
"<meta charset=\"utf-8\"/>\n"
"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>\n"
"<title>{title}</title>\n"
"<script>\n"
"(function(){{\n"
"  var t = localStorage.getItem('theme');\n"
"  if(t === 'dark') document.documentElement.classList.add('dark');\n"
"  document.addEventListener('DOMContentLoaded', function(){{\n"
"    document.querySelectorAll('.code-wrap').forEach(function(w){{\n"
"      if(w.scrollWidth > w.clientWidth) {{\n"
"        var ts = document.createElement('div');\n"
"        ts.className = 'code-scroll-top';\n"
"        var si = document.createElement('div');\n"
"        ts.appendChild(si);\n"
"        w.parentNode.insertBefore(ts, w);\n"
"        w.classList.add('has-top-scroll');\n"
"        function upd() {{ si.style.width = w.scrollWidth + 'px'; }}\n"
"        upd();\n"
"        ts.addEventListener('scroll', function() {{ w.scrollLeft = ts.scrollLeft; }});\n"
"        w.addEventListener('scroll', function() {{ ts.scrollLeft = w.scrollLeft; }});\n"
"        if(window.ResizeObserver) new ResizeObserver(upd).observe(w);\n"
"      }}\n"
"    }});\n"
"  }});\n"
"}})()\n"
"</script>\n"
"<style>\n"
+ _PYGMENTS_CSS + "\n"
+ _DARK_PYGMENTS_CSS + "\n"
"""
:root {{
  --bg: #ffffff; --text: #24292f; --border: #d0d7de; --muted: #656d76;
  --link: #0969da; --code-bg: #f6f8fa;
  --ln-bg: #f6f8fa; --ln-border: #d0d7de; --ln-text: #8c959f;
}}

html.dark {{
  --bg: #0d1117; --text: #e6edf3; --border: #30363d; --muted: #8b949e;
  --link: #58a6ff; --code-bg: #161b22;
  --ln-bg: #1c2128; --ln-border: #30363d; --ln-text: #6e7681;
}}

body {{
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  max-width: 900px; margin: 2rem auto; padding: 0 1rem;
  line-height: 1.6; background: var(--bg); color: var(--text);
}}

nav.breadcrumb {{
  display: flex; align-items: center; justify-content: space-between; gap: 1em;
  font-size: 1rem; color: var(--muted); margin-bottom: 1.5rem;
  padding: 0.4em 0.8em; background: var(--code-bg); border-radius: 6px;
}}

nav.breadcrumb a {{ color: var(--link); }}

pre {{
  font-family: ui-monospace, "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 0.875em; tab-size: 4; background: var(--code-bg);
  padding: 1em; overflow-x: auto; border-radius: 6px;
}}

code {{
  font-family: ui-monospace, "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  background: var(--code-bg); padding: 0.2em 0.4em; border-radius: 3px;
  font-size: 0.875em;
}}

pre > code {{ background: none; padding: 0; font-size: 1em; }}

table {{ border-collapse: collapse; width: 100%; margin: 1em 0; }}
th, td {{ border: 1px solid var(--border); padding: 0.4em 0.8em; text-align: left; }}
th {{ background: var(--code-bg); }}

li.task-list-item {{ list-style: none; margin-left: -1.5em; }}

a {{ color: var(--link); text-decoration: none; }}
a:hover {{ text-decoration: underline; }}

h1, h2, h3 {{ border-bottom: 1px solid var(--border); padding-bottom: 0.3em; margin-top: 1.5em; }}

img {{ max-width: 100%; height: auto; }}

blockquote {{ border-left: 4px solid var(--border); margin: 0; padding: 0 1em; color: var(--muted); }}

.code-wrap {{ overflow-x: auto; border-radius: 6px; margin: 0; }}
.code-wrap.has-top-scroll {{ border-radius: 0 0 6px 6px; }}
.code-wrap pre {{ overflow-x: visible; }}

.code-scroll-top {{
  overflow-x: auto; overflow-y: hidden; height: 16px; margin-bottom: -10px;
  background: var(--code-bg); border-radius: 6px 6px 0 0;
}}
.code-scroll-top > div {{ height: 1px; }}

.highlighttable {{ border-spacing: 0; width: max-content; min-width: 100%; }}

.highlighttable td.linenos {{
  width: 1%; white-space: nowrap; vertical-align: top; user-select: none;
  background: var(--ln-bg); color: var(--ln-text);
  border-right: 1px solid var(--ln-border); padding: 0.8em 1em;
}}

.highlighttable td.linenos .linenodiv pre {{
  margin: 0; padding: 0; background: transparent; border-radius: 0;
}}

.highlighttable td.code {{ padding: 0; background: var(--code-bg); }}

.highlighttable td.code div pre {{
  margin: 0; padding: 0.8em 1em;
  background: transparent; border-radius: 0; overflow-x: visible;
}}

.theme-toggle {{
  flex-shrink: 0; background: transparent; border: 1px solid var(--border);
  border-radius: 6px; cursor: pointer; font-size: 0.875em;
  padding: 0.2em 0.5em; color: var(--text); line-height: 1;
}}

.theme-toggle::before {{ content: "🌙"; }}
html.dark .theme-toggle::before {{ content: "☀️"; }}

</style>
</head>
<body>
{body}
</body>
</html>
"""
)


# ---------------------------------------------------------------------------
# MDRServer — typed subclass to avoid monkey-patching web_root
# ---------------------------------------------------------------------------

class MDRServer(ThreadingHTTPServer):
    def __init__(
        self,
        server_address: tuple[str, int],
        handler_class: type,
        web_root: str,
    ) -> None:
        super().__init__(server_address, handler_class)
        self.web_root: str = web_root


# ---------------------------------------------------------------------------
# Request handler
# ---------------------------------------------------------------------------

class MDRHandler(SimpleHTTPRequestHandler):

    def translate_path(self, path: str) -> str:
        path = urllib.parse.unquote(path).split("?", 1)[0].split("#", 1)[0]
        parts = [p for p in posixpath.normpath(path).split("/") if p and p != ".."]
        return os.path.join(self.server.web_root, *parts) if parts else self.server.web_root

    def _safe_path(self) -> str | None:
        fs = self.translate_path(self.path)
        real = os.path.realpath(fs)
        root = os.path.realpath(self.server.web_root)
        if real != root and not real.startswith(root + os.sep):
            return None
        return fs

    _FAVICON_SVG = (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16">'
        '<rect width="12" height="14" x="2" y="1" rx="1" fill="#0969da"/>'
        '<rect width="6" height="1.5" x="4" y="4" rx="0.5" fill="white"/>'
        '<rect width="6" height="1.5" x="4" y="7" rx="0.5" fill="white"/>'
        '<rect width="4" height="1.5" x="4" y="10" rx="0.5" fill="white"/>'
        '</svg>'
    ).encode()

    def do_GET(self) -> None:
        if self.path == "/favicon.ico":
            self.send_response(200)
            self.send_header("Content-Type", "image/svg+xml")
            self.send_header("Content-Length", str(len(self._FAVICON_SVG)))
            self.send_header("Cache-Control", "max-age=86400")
            self.end_headers()
            self.wfile.write(self._FAVICON_SVG)
            return

        fs_path = self._safe_path()
        if fs_path is None:
            self.send_error(403, "Forbidden")
            return

        if os.path.isdir(fs_path):
            url_part, _, qs = self.path.partition("?")
            if not url_part.endswith("/"):
                self.send_response(301)
                self.send_header("Location", url_part + "/")
                self.end_headers()
                return
            if "listing" not in qs.split("&"):
                for idx_name in ("index.md", "README.md"):
                    idx = os.path.join(fs_path, idx_name)
                    if os.path.isfile(idx):
                        self._serve_markdown(idx, dir_url=url_part + "?listing")
                        return
            self._serve_directory(fs_path)
            return

        if os.path.isfile(fs_path):
            if fs_path.endswith(".md"):
                self._serve_markdown(fs_path)
                return
            lexer = self._lexer_for_file(fs_path)
            if lexer is not None:
                self._serve_source(fs_path, lexer)
                return

        super().do_GET()

    def do_HEAD(self) -> None:
        fs_path = self._safe_path()
        if fs_path is None:
            self.send_error(403, "Forbidden")
            return
        if os.path.isdir(fs_path):
            self.do_GET()
            return
        if os.path.isfile(fs_path):
            if fs_path.endswith(".md") or self._lexer_for_file(fs_path) is not None:
                self.do_GET()  # _send_html omits body for HEAD
                return
        super().do_HEAD()

    def _not_modified(self, mtime: float) -> bool:
        """Return True (and send 304) when If-Modified-Since covers mtime."""
        ims = self.headers.get("If-Modified-Since")
        if ims:
            try:
                parsed = email.utils.parsedate(ims)
                if parsed and int(mtime) <= calendar.timegm(parsed):
                    self.send_response(304)
                    self.send_header("Cache-Control", "no-store")
                    self.end_headers()
                    return True
            except Exception:
                pass
        return False

    def _serve_directory(self, dir_path: str) -> None:
        try:
            st = os.stat(dir_path)
        except OSError:
            self.send_error(403, "Permission denied")
            return
        if self._not_modified(st.st_mtime):
            return
        try:
            # os.scandir caches is_dir() and stat() results — avoids a second
            # stat() call per entry compared to os.listdir() + os.path.isdir().
            with os.scandir(dir_path) as it:
                entries = sorted(it, key=lambda e: (not e.is_dir(), e.name.lower()))
        except OSError:
            self.send_error(403, "Permission denied")
            return

        url_path = urllib.parse.unquote(self.path.split("?", 1)[0])
        rows: list[str] = []

        if url_path not in ("/", ""):
            rows.append('<tr><td><a href="../">../</a></td><td></td><td></td></tr>')

        for entry in entries:
            href = urllib.parse.quote(entry.name)
            safe = html.escape(entry.name)
            try:
                entry_st = entry.stat()
                mod = datetime.datetime.fromtimestamp(entry_st.st_mtime).strftime("%Y/%m/%d %H:%M")
            except OSError:
                entry_st = None
                mod = ""
            if entry.is_dir():
                rows.append(
                    f'<tr><td><a href="{href}/">{safe}/</a></td>'
                    f'<td style="text-align:right">—</td>'
                    f'<td>{mod}</td></tr>'
                )
            else:
                size = f'{entry_st.st_size:,}' if entry_st is not None else ""
                rows.append(
                    f'<tr><td><a href="{href}">{safe}</a></td>'
                    f'<td style="text-align:right">{size}</td>'
                    f'<td>{mod}</td></tr>'
                )

        safe_url = html.escape(url_path)
        crumb = self._breadcrumb(url_path)
        body = (
            self._nav_bar(crumb) + "\n"
            '<table>\n'
            '<thead><tr><th>Name</th>'
            '<th style="text-align:right">Size (bytes)</th>'
            '<th>Modified</th></tr></thead>\n'
            '<tbody>\n' + "\n".join(rows) + '\n</tbody>\n</table>'
        )
        self._send_html(
            _TEMPLATE.format(title=f"Index of {safe_url}", body=body),
            last_modified=st.st_mtime,
        )

    def _serve_markdown(self, file_path: str, dir_url: str | None = None) -> None:
        try:
            st = os.stat(file_path)
        except OSError:
            self.send_error(404, "File not found")
            return
        if self._not_modified(st.st_mtime):
            return
        try:
            with open(file_path, encoding="utf-8") as f:
                text = f.read()
        except OSError:
            self.send_error(404, "File not found")
            return

        url_path = urllib.parse.unquote(self.path.split("?", 1)[0])
        rendered = _md.render(text)
        # Wrap fenced code blocks for dual-scrollbar support
        rendered = re.sub(r'<pre><code([^>]*)>',
                          r'<div class="code-wrap"><pre><code\1>', rendered)
        rendered = re.sub(r'</code></pre>',
                          r'</code></pre></div>', rendered)
        crumb = self._breadcrumb(url_path)
        if dir_url:
            safe_url = html.escape(dir_url)
            crumb += f' &nbsp;•&nbsp; <a href="{safe_url}">[directory listing]</a>'
        body = f'{self._nav_bar(crumb)}\n{rendered}'
        title = html.escape(os.path.basename(file_path))
        self._send_html(_TEMPLATE.format(title=title, body=body),
                        last_modified=st.st_mtime)

    def _serve_source(self, file_path: str, lexer) -> None:
        try:
            st = os.stat(file_path)
        except OSError:
            self.send_error(404, "File not found")
            return
        if st.st_size > _MAX_HIGHLIGHT_BYTES:
            super().do_GET()
            return
        if self._not_modified(st.st_mtime):
            return
        try:
            with open(file_path, encoding="utf-8", errors="replace") as f:
                text = f.read()
        except OSError:
            self.send_error(404, "File not found")
            return

        url_path = urllib.parse.unquote(self.path.split("?", 1)[0])
        crumb = self._breadcrumb(url_path)
        inner = f'<div class="code-wrap">{_hl(text, lexer, _src_formatter)}</div>'
        body = f'{self._nav_bar(crumb)}\n{inner}'
        title = html.escape(os.path.basename(file_path))
        self._send_html(_TEMPLATE.format(title=title, body=body),
                        last_modified=st.st_mtime)

    def _lexer_for_file(self, path: str):
        name = os.path.basename(path)
        if name in _JXMAKE_FILENAMES or name.endswith(".jxm"):
            return _JxMakeLexer(stripall=True)
        if name in _MAKEFILE_FILENAMES or name.endswith((".mk", ".mak")):
            return _MakefileLexer(stripall=True)
        try:
            lexer = get_lexer_for_filename(name, stripall=True)
        except ClassNotFound:
            return None
        return None if isinstance(lexer, TextLexer) else lexer

    def _breadcrumb(self, url_path: str) -> str:
        parts = [p for p in url_path.strip("/").split("/") if p]
        crumbs = ['<a href="/">~</a>']
        for i, part in enumerate(parts):
            href = "/" + "/".join(urllib.parse.quote(p) for p in parts[: i + 1])
            safe = html.escape(part)
            if i == len(parts) - 1:
                crumbs.append(safe)
            else:
                crumbs.append(f'<a href="{href}/">{safe}</a>')
        return "/".join(crumbs)

    @staticmethod
    def _nav_bar(crumb_html: str, right_html: str = "") -> str:
        right = (f"{right_html} " if right_html else "") + _TOGGLE_HTML
        return (
            f'<nav class="breadcrumb">'
            f'<span>{crumb_html}</span>'
            f'<span style="display:flex;align-items:center;gap:0.6em">{right}</span>'
            f'</nav>'
        )

    def _send_html(self, html_text: str, status: int = 200, *,
                   last_modified: float | None = None) -> None:
        data = html_text.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        if last_modified is not None:
            self.send_header("Last-Modified",
                             email.utils.formatdate(last_modified, usegmt=True))
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(data)

    _INLINE_MIME = frozenset({
        "application/pdf",
        "application/json",
        "application/javascript",
        "application/xhtml+xml",
    })

    def guess_type(self, path: str) -> str:  # type: ignore[override]
        mime, _ = mimetypes.guess_type(path)
        if mime and (
            mime.startswith("text/")
            or mime.startswith("image/")
            or mime in self._INLINE_MIME
        ):
            return mime
        try:
            with open(path, "rb") as f:
                sample = f.read(8192)
        except OSError:
            return mime or "application/octet-stream"
        if b"\x00" not in sample:
            try:
                sample.decode("utf-8")
                return "text/plain; charset=utf-8"
            except UnicodeDecodeError:
                pass
        return mime or "application/octet-stream"

    def end_headers(self) -> None:
        self.send_header("X-Content-Type-Options", "nosniff")
        super().end_headers()

    def send_error(self, code: int, message: str | None = None, explain: str | None = None) -> None:
        self.log_error("%s %s", code, message or "")
        title = f"{code} {message or 'Error'}"
        try:
            url_path = urllib.parse.unquote(getattr(self, "path", "/").split("?", 1)[0])
            crumb = self._breadcrumb(url_path)
        except Exception:
            crumb = '<a href="/">~</a>'
        body_parts = [
            self._nav_bar(crumb),
            f"<h1>{html.escape(title)}</h1>",
        ]
        if explain:
            body_parts.append(f"<p>{html.escape(explain)}</p>")
        self._send_html(
            _TEMPLATE.format(title=html.escape(title), body="\n".join(body_parts)),
            status=code,
        )

    def log_message(self, fmt: str, *args) -> None:
        print(f"[{self.address_string()}] {fmt % args}")


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(description="Markdown-rendering HTTP server")
    parser.add_argument(
        "-C", "--directory",
        default=os.getcwd(),
        metavar="DIR",
        help="web root directory (default: current working directory)",
    )
    parser.add_argument(
        "-p", "--port",
        type=int,
        default=8080,
        help="port to listen on (default: 8080)",
    )
    parser.add_argument(
        "-b", "--bind",
        default="127.0.0.1",
        metavar="ADDR",
        help="address to bind to (default: 127.0.0.1)",
    )
    args = parser.parse_args()

    web_root = os.path.realpath(args.directory)
    if not os.path.isdir(web_root):
        sys.exit(f"error: {web_root!r} is not a directory")

    server = MDRServer((args.bind, args.port), MDRHandler, web_root)

    print(f"Serving {web_root}")
    print(f"Listening on http://{args.bind}:{args.port}/")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down.")


if __name__ == "__main__":
    main()
