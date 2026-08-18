#!/usr/bin/env node
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

'use strict';

// Lightweight CSS syntax checker.
//
// Parses a .css source file with `postcss` and reports syntax errors.
// (An earlier version of this script used `css-tree`, but its parser is
// deliberately tolerant -- it silently auto-closes an unclosed `{ ... }`
// block at EOF instead of reporting a parse error, so real corruption like
// a missing closing brace went undetected. `postcss` throws a
// `CssSyntaxError` with line/column for that case, so it's used instead.)
// This does NOT do any semantic validation (unknown properties/values are
// not checked).
//
// Install (once node/npm work):
//     npm install --prefix ~/mynpm postcss
//
// Run:
//     export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
//     export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules:~/mynpm/node_modules
//     export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
//     node css_syntax_check.js <file.css> [file2.css ...]

const fs      = require('fs');
const postcss = require('postcss');

function hasSyntaxError(source)
{
    try {
        postcss.parse(source);
        return false;
    }
    catch(e) {
        if(e.name === 'CssSyntaxError') console.log(`${e.line}:${e.column}: ${e.reason}`);
        else                            console.log(e.message);
        return true;
    }
} // hasSyntaxError

function main()
{
    const args = process.argv.slice(2);
    if(args.length < 1) {
        console.error('Usage: css_syntax_check.sh <file.css> [file2.css ...]');
        process.exit(2);
    }

    let anyError = false;

    for(const arg of args) {
        const source   = fs.readFileSync(arg, 'utf8');
        const hasError = hasSyntaxError(source);
        if(hasError) {
            console.log(`SYNTAX ERRORS FOUND in ${arg}`);
            anyError = true;
        }
        else {
            console.log(`OK: no syntax errors in ${arg}`);
        }
    } // for

    if(anyError) process.exit(1);
} // main

main();
