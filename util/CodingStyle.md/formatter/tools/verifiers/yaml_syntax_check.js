#!/usr/bin/env node
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

'use strict';

// Lightweight YAML syntax checker.
//
// Parses a .yaml/.yml source file with `js-yaml` and reports syntax
// errors. Uses loadAll() so multi-document files (separated by `---`)
// are fully checked. This does NOT do any schema/semantic validation.
//
// Install (once node/npm work):
//     npm install --prefix ~/mynpm js-yaml
//
// Run:
//     export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
//     export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules:~/mynpm/node_modules
//     export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
//     node yaml_syntax_check.js <file.yaml> [file2.yaml ...]

const fs   = require('fs');
const yaml = require('js-yaml');

function hasSyntaxError(source)
{
    try {
        yaml.loadAll(source);
        return false;
    }
    catch(e) {
        if(e.mark) console.log(`${e.mark.line + 1}:${e.mark.column + 1}: ${e.reason}`);
        else       console.log(e.message);
        return true;
    }
} // hasSyntaxError

function main()
{
    const args = process.argv.slice(2);
    if(args.length < 1) {
        console.error('Usage: yaml_syntax_check.sh <file.yaml> [file2.yaml ...]');
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
