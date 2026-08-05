====================================================================================================
Documentation Server (MDXplorer)
====================================================================================================

This server supports Markdown rendering, directory listings, and syntax-highlighted source files,
including JxMake script files (.jxm / JxMakeFile).

This program was developed with significant assistance from Claude Sonnet 4.6.

----------------------------------------------------------------------------------------------------

Install requirements first:
    pip3.12 install --user -r requirements.txt

Test the server:
    python3.12 mdx_server.py -b localhost -p 8080 -C ../..

You may adjust 'pip3.12' and 'python3.12' according to your requirements.

----------------------------------------------------------------------------------------------------
Formatted view (jxmake-code-formatter integration)
----------------------------------------------------------------------------------------------------

If a jxmake-code-formatter server ('../CodingStyle.md/formatter', run with '--server') is running on
the same machine, any non-Markdown file page can be viewed reformatted by appending '?' (or
'?key=value...' overrides) to its URL, or via the wand-icon "view formatted" toggle button next to
the dark/light toggle in the nav bar. Formatting is attempted regardless of whether Pygments
recognises the file type; unsupported/unreachable formatting fails silently back to the normal
view (failures are logged to stderr).

The formatter server is auto-discovered from its lockfile at
'~/.config/jxmake-code-formatter/server.lock' (same file the Java CLI uses). Pass '--formatter-port'
to point at a specific port instead:
    python3.12 mdx_server.py -C ../.. --formatter-port 17173

A gear-icon settings button opens a panel of the formatter's config properties (fetched live via
the server's own GET /properties, proxied same-origin through GET '/__mdxplorer/properties' so no
CORS setup is needed). Overrides are stored in the browser's localStorage
(key 'mdxplorer.formatterOverrides') and applied as query-string parameters on the next formatted
view; "Reset to defaults" clears all stored overrides.

----------------------------------------------------------------------------------------------------
Highlighted file types
----------------------------------------------------------------------------------------------------

Markdown     : .md files are rendered as HTML with GFM and task-list extensions.

JxMake       : JxMakeFile and .jxm files are syntax-highlighted using a built-in lexer
               that covers the full JxMake grammar (directives, variables, strings, shell
               commands, flow control, macros, and nested block comments).

GNU Make     : Makefile, GNUmakefile, BSDmakefile, makefile, .mk, .mak

All others   : any file type recognised by Pygments (C, C++, Python, Java, shell, …).

----------------------------------------------------------------------------------------------------
Caching
----------------------------------------------------------------------------------------------------

All HTML responses are sent with Cache-Control: no-store.  This ensures that every browser
(including Firefox's back-forward cache) always fetches the current version of a file from disk
rather than serving a stale cached copy after edits.

====================================================================================================

MDXplorer is free software; you can redistribute it and/or modify it under the terms of the
GNU Lesser General Public License as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

====================================================================================================
