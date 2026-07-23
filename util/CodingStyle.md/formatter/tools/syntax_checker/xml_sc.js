#!/usr/bin/env node
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

'use strict';

// Lightweight XML syntax checker.
//
// Parses a .xml source file with `@xmldom/xmldom` and reports syntax
// errors. xmldom's default onError handler only logs warnings/errors to
// the console and does not throw for `error`-level problems, so a custom
// onError is wired in to capture those too; `fatalError`-level problems
// (e.g. mismatched tags) throw a ParseError regardless of onError, so
// that path is also caught. This does NOT validate against a DTD/XSD
// schema.
//
// Install (once node/npm work):
//     npm install --prefix ~/mynpm @xmldom/xmldom
//
// Run:
//     export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules
//     export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
//     node xml_sc.js <file.xml> [file2.xml ...]

const fs = require('fs');
const { DOMParser } = require('@xmldom/xmldom');

function hasSyntaxError(source) {
    const messages = [];

    const parser = new DOMParser({
        onError: (level, msg) => {
            messages.push(`${level}: ${msg}`);
        }
    });

    try {
        parser.parseFromString(source, 'text/xml');
    } catch (e) {
        // A fatalError is reported via onError above AND thrown -- only
        // record it here if onError didn't already capture it.
        if (messages.length === 0) {
            messages.push(e.message || String(e));
        }
    }

    for (const m of messages) {
        console.log(m);
    }

    return messages.length > 0;
}

function main() {
    const args = process.argv.slice(2);
    if (args.length < 1) {
        console.error('Usage: xml_sc.js <file.xml> [file2.xml ...]');
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
