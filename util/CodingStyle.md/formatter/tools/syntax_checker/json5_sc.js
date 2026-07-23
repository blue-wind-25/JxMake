#!/usr/bin/env node
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */
/*
 * Run this script with the following env vars set (Node.js + its packages are
 * installed under these non-default locations, not the system paths):
 *
 *   export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
 *   export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules:~/mynpm/node_modules
 *   export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
 */

'use strict';

// Lightweight JSON5 syntax checker.
//
// Parses a .json5 source file with the `json5` package and reports
// syntax errors (line/column come from json5's own error object).
// This does NOT do any schema/semantic validation.
//
// Install (once node/npm work):
//     npm install --prefix ~/mynpm json5
//
// Run:
//     export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules
//     export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
//     node json5_sc.js <file.json5> [file2.json5 ...]

const fs = require('fs');
const JSON5 = require('json5');

function hasSyntaxError(source) {
    try {
        JSON5.parse(source);
        return false;
    } catch (e) {
        if (typeof e.lineNumber === 'number') {
            console.log(`${e.lineNumber}:${e.columnNumber}: ${e.message}`);
        } else {
            console.log(e.message);
        }
        return true;
    }
}

function main() {
    const args = process.argv.slice(2);
    if (args.length < 1) {
        console.error('Usage: json5_sc.js <file.json5> [file2.json5 ...]');
        process.exit(2);
    }

    let anyError = false;

    for (const arg of args) {
        const source = fs.readFileSync(arg, 'utf8');
        const hasError = hasSyntaxError(source);
        if (hasError) {
            console.log(`SYNTAX ERRORS FOUND in ${arg}`);
            anyError = true;
        } else {
            console.log(`OK: no syntax errors in ${arg}`);
        }
    }

    if (anyError) {
        process.exit(1);
    }
}

main();
