#!/usr/bin/env node
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

'use strict';

// Lightweight HTML5 syntax checker.
//
// Parses a .html source file with `parse5` (a spec-compliant WHATWG
// HTML5 parser) and reports parse errors via its onParseError callback.
//
// IMPORTANT CAVEAT: per the HTML5 spec, the parser is deliberately
// error-tolerant (e.g. it silently auto-closes mismatched tags rather
// than failing) so this will NOT catch most "malformed" HTML the way
// an XML parser catches malformed XML -- it only reports the specific
// set of conditions the spec defines as parse errors. Treat a clean exit
// here as "a browser can render this without an ambiguous/undefined DOM",
// not "the markup is well-formed" in the XML sense.
//
// Install (once node/npm work):
//     npm install --prefix ~/mynpm parse5
//
// Run:
//     export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
//     export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules:~/mynpm/node_modules
//     export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
//     node html_syntax_check.js <file.html> [file2.html ...]

const fs     = require('fs');
const parse5 = require('parse5');

function hasSyntaxError(source)
{
    const errors = [];

    parse5.parse( source, {
        sourceCodeLocationInfo: true,
        onParseError: (error) => {
            errors.push(error);
        }
    } );

    for(const e of errors) {
        const loc = e.startLine != null ? `${e.startLine}:${e.startCol}: ` : '';
        console.log(`${loc}${e.code}`);
    }

    return errors.length > 0;
} // hasSyntaxError

function main()
{
    const args = process.argv.slice(2);
    if(args.length < 1) {
        console.error('Usage: html_syntax_check.sh <file.html> [file2.html ...]');
        process.exit(2);
    }

    let anyError = false;

    for(const arg of args) {
        const source = fs.readFileSync(arg, 'utf8');
        if( hasSyntaxError(source) ) anyError = true;
    } // for

    if(anyError) process.exit(1);
} // main

main();
