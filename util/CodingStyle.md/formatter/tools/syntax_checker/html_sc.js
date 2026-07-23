#!/usr/bin/env node
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
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
// set of conditions the spec defines as parse errors. Treat "OK" here
// as "a browser can render this without an ambiguous/undefined DOM",
// not "the markup is well-formed" in the XML sense.
//
// Install (once node/npm work):
//     npm install --prefix ~/mynpm parse5
//
// Run:
//     export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules
//     export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
//     node html_sc.js <file.html> [file2.html ...]

const fs = require('fs');
const parse5 = require('parse5');

function hasSyntaxError(source) {
    const errors = [];

    parse5.parse(source, {
        sourceCodeLocationInfo: true,
        onParseError: (error) => {
            errors.push(error);
        }
    });

    for (const e of errors) {
        const loc = e.startLine != null ? `${e.startLine}:${e.startCol}: ` : '';
        console.log(`${loc}${e.code}`);
    }

    return errors.length > 0;
}

function main() {
    const args = process.argv.slice(2);
    if (args.length < 1) {
        console.error('Usage: html_sc.js <file.html> [file2.html ...]');
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
