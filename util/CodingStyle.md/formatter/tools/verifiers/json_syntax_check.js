#!/usr/bin/env node
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

'use strict';

// Lightweight JSON syntax checker.
//
// Parses a .json source file with the built-in JSON.parse and reports
// syntax errors. This does NOT do any schema/semantic validation.
//
// Run:
//     export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
//     export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules:~/mynpm/node_modules
//     export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
//     node json_syntax_check.js <file.json> [file2.json ...]

const fs = require('fs');

function lineColOf(source, pos)
{
    let line = 1;
    let col  = 1;
    for(let i = 0; i < pos && i < source.length; ++i) {
        if( source[i] === '\n' ) {
            ++line;
            col = 1;
        }
        else {
            ++col;
        }
    } // for

    return { line, col };
} // lineColOf

function hasSyntaxError(source)
{
    try {
        JSON.parse(source);
        return false;
    }
    catch(e) {
        const m = /position (\d+)/.exec(e.message);
        if(m) {
            const { line, col } = lineColOf( source, parseInt( m[1], 10 ) );
            console.log(`${line}:${col}: ${e.message}`);
        }
        else {
            console.log(e.message);
        }
        return true;
    }
} // hasSyntaxError

function main()
{
    const args = process.argv.slice(2);
    if(args.length < 1) {
        console.error('Usage: json_syntax_check.sh <file.json> [file2.json ...]');
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
