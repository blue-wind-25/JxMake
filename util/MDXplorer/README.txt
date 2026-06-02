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

Highlighted file types
----------------------

Markdown     : .md files are rendered as HTML with GFM and task-list extensions.

JxMake       : JxMakeFile and .jxm files are syntax-highlighted using a built-in lexer
               that covers the full JxMake grammar (directives, variables, strings, shell
               commands, flow control, macros, and nested block comments).

GNU Make     : Makefile, GNUmakefile, BSDmakefile, makefile, .mk, .mak

All others   : any file type recognised by Pygments (C, C++, Python, Java, shell, …).

----------------------------------------------------------------------------------------------------

Caching
-------

All HTML responses are sent with Cache-Control: no-store.  This ensures that every browser
(including Firefox's back-forward cache) always fetches the current version of a file from disk
rather than serving a stale cached copy after edits.

----------------------------------------------------------------------------------------------------

MDXplorer is free software; you can redistribute it and/or modify it under the terms of the
GNU Lesser General Public License as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

====================================================================================================
