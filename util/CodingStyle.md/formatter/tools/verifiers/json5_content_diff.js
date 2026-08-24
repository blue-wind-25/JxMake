#!/usr/bin/env node
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

'use strict';

/**
 * Content-preservation checker for JSON5 dogfood testing.
 *
 * Parses both an original and a formatted JSON5 file with the `json5`
 * package (same parser json5_syntax_check.js uses) and compares the
 * resulting JS values (object/array/scalar) for deep equality. This proves
 * keys, values, and structure were not altered by formatting -- it does NOT
 * catch comment-only changes (comment normalization is a separate, lighter-
 * weight concern; see the accompanying text-based comment scan below,
 * best-effort), same reasoning as yaml_content_diff.py's/toml_content_diff.py's
 * comment side-channel. Unlike those, JSON5 comments come in two forms
 * (`//` line comments and `/* ... *\/` block comments), so the scan below
 * collects both line-comment bodies and block-comment bodies rather than
 * matching on a single leading character.
 *
 * Node was chosen over Python for this one script (unlike json_content_diff.py,
 * which is plain Python using the stdlib `json` module) because no JSON5
 * parser is installed for this system's Python (`pip3.12 list` has no json5/
 * pyjson5), while the `json5` npm package is already the precedent set by
 * json5_syntax_check.js -- reusing it avoids adding a second, redundant
 * JSON5 grammar implementation to this repo.
 *
 * Env needed (same as json5_syntax_check.js):
 *     export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
 *     export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules:~/mynpm/node_modules
 *     export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
 *
 * Usage (two modes -- see also printUsage() below):
 *     Single pair:
 *         node json5_content_diff.js <original.json5> <formatted.json5>
 *     Batch (one Node process invocation over a whole corpus, avoiding a
 *     process restart per file -- rel-path list, one path per line, relative
 *     to both base dirs alike):
 *         node json5_content_diff.js <original_base_dir> <formatted_base_dir> <json5_rel_path_file_list.txt>
 *
 * Before each pair's diff, a "[yyyy-MM-dd HH:mm:ss.SSS] <relative path>" line
 * is printed -- lets a hang/slow file in a large batch run be pinpointed,
 * matching js_ts_content_diff.js's own precedent. In batch mode, a rel-path
 * missing from either base dir (or both) is a warning, not a crash -- the
 * file is skipped and the run continues; the final SUMMARY line and process
 * exit code still reflect it. A parse failure for one pair in batch mode is
 * also caught and counted as a MISMATCH/ERROR rather than aborting the whole
 * batch.
 *
 * Exit codes:
 *     0 - the parsed value matches, for every pair checked (comment
 *         differences, if any, are printed as informational only, not a
 *         failure)
 *     1 - a structural/data mismatch was found (or if any pair is
 *         missing/errors in batch mode)
 *     2 - (single-pair mode only) one of the two files failed to parse as
 *         JSON5 at all -- not applicable to a real dogfood run where both
 *         files should already be syntax-checked separately; also used for
 *         a usage error
 */

const fs    = require('fs');
const path  = require('path');
const JSON5 = require('json5');

class ParseError extends Error {}

function loadValue(filePath)
{
    const text = fs.readFileSync(filePath, 'utf8');
    try {
        return { value: JSON5.parse(text), text };
    }
    catch(e) {
        throw new ParseError(e.message);
    }
} // loadValue

function commentBodies(text)
{
    // Best-effort: collect `//` line-comment bodies and `/* ... */`
    // block-comment bodies, in order. Not a full lexer (doesn't account for
    // `//`/`/*` appearing inside a string literal), but good enough as a
    // lightweight informational signal, not the primary check -- same spirit
    // as yaml_content_diff.py's comment_lines().
    const out = [];
    const re  = /\/\/[^\n]*|\/\*[\s\S]*?\*\//g;
    let   m;
    while( ( m = re.exec(text) ) !== null ) out.push( m[0].trim() );

    return out;
} // commentBodies

function timestampNow()
{
    const now = new Date();
    const pad = (n, w) => String(n).padStart(w, '0');

    return `${now.getFullYear()}-${pad( now.getMonth() + 1, 2 )}-${pad( now.getDate(), 2 )} `
         + `${pad( now.getHours(), 2 )}:${pad( now.getMinutes(), 2 )}:${pad( now.getSeconds(), 2 )}.${pad( now.getMilliseconds(), 3 )}`;
} // timestampNow

function printTimestampedHeader(relPath)
{
    console.log(`[${timestampNow()}] ${relPath}`);
} // printTimestampedHeader

function deepEqual(a, b)
{
    if(a === b) return true;
    if(typeof a !== typeof b) return false;
    if(a === null || b === null) return a === b;
    if(typeof a !== 'object') return false; // Primitives already handled by === above

    if( Array.isArray(a) !== Array.isArray(b) ) return false;

    if( Array.isArray(a) ) {
        if(a.length !== b.length) return false;
        for(let i = 0; i < a.length; ++i) if( !deepEqual( a[i], b[i] ) ) return false;
        return true;
    }

    const aKeys = Object.keys(a);
    const bKeys = Object.keys(b);
    if(aKeys.length !== bKeys.length) return false;
    for(const k of aKeys) {
        if( !Object.prototype.hasOwnProperty.call(b, k) ) return false;
        if( !deepEqual( a[k], b[k] ) ) return false;
    }

    return true;
} // deepEqual

function compareOne(origPath, fmtPath, origLabel, fmtLabel)
{
    let orig, fmt;
    try {
        orig = loadValue(origPath);
    }
    catch(e) {
        throw new ParseError(`original file failed to parse: ${origPath}\n${e.message}`);
    }

    try {
        fmt = loadValue(fmtPath);
    }
    catch(e) {
        throw new ParseError(`formatted file failed to parse: ${fmtPath}\n${e.message}`);
    }

    if( !deepEqual(orig.value, fmt.value) ) {
        console.log(`MISMATCH: ${origLabel} vs ${fmtLabel}`);
        console.log(
            `  - parsed structure differs\n  original:  ${JSON.stringify(orig.value)}\n  formatted: ${JSON.stringify(fmt.value)}`
        );
        return false;
    } // if

    // Informational-only comment scan (not a failure condition by itself)
    const oc   = commentBodies(orig.text);
    const fc   = commentBodies(fmt.text);
    const same = oc.length === fc.length && oc.every( (v, i) => v === fc[i] );
    if(!same) {
        if(oc.length === fc.length) {
            const diffs = [];
            for(let i = 0; i < oc.length; ++i) if( oc[i] !== fc[i] ) diffs.push( [ oc[i], fc[i] ] );
            console.log(`OK: content preserved (data structures match: ${origLabel} == ${fmtLabel}). `
                      + `Note: ${diffs.length} comment(s) textually changed (informational only):`);
            for( const [a, b] of diffs.slice(0, 10) ) console.log(`    ${JSON.stringify(a)} -> ${JSON.stringify(b)}`);
        } // if
        else {
            console.log(`OK: content preserved (data structures match: ${origLabel} == ${fmtLabel}). `
                      + `Note: comment count differs (${oc.length} -> ${fc.length}), informational only.`);
        }
        return true;
    } // if

    console.log(`OK: content preserved: ${origLabel} vs ${fmtLabel}`);

    return true;
} // compareOne

function printUsage()
{
    console.error('Usage: json5_content_diff.sh <original.json5> <formatted.json5>');
    console.error(
        '       json5_content_diff.sh <original_base_dir> <formatted_base_dir> <json5_rel_path_file_list.txt>'
    );
} // printUsage

function runSingle(origArg, fmtArg)
{
    printTimestampedHeader(origArg);

    const origExists = fs.existsSync(origArg);
    const fmtExists  = fs.existsSync(fmtArg);
    if(!origExists || !fmtExists) {
        if(!origExists && !fmtExists) console.error(
            `WARNING: both ${origArg} and ${fmtArg} are missing`
        );
        else if(!origExists) console.error(`WARNING: ${origArg} is missing`);
        else console.error(`WARNING: ${fmtArg} is missing`);
        process.exit(1);
    } // if

    try {
        process.exit( compareOne(origArg, fmtArg, origArg, fmtArg) ? 0 : 1 );
    }
    catch(e) {
        if(e instanceof ParseError) {
            console.error(`ERROR: ${e.message}`);
            process.exit(2);
        }
        throw e;
    }
} // runSingle

function runBatch(origBaseDir, fmtBaseDir, fileListPath)
{
    const relPaths = fs.readFileSync(fileListPath, 'utf8')
        .split('\n')
        .map( (l) => l.trim() )
        .filter( (l) => l.length > 0 );

    let okCount = 0, mismatchCount = 0, missingCount = 0;

    for(const rel of relPaths) {
        const origPath = path.join(origBaseDir, rel);
        const fmtPath  = path.join(fmtBaseDir, rel);

        printTimestampedHeader(rel);

        const origExists = fs.existsSync(origPath);
        const fmtExists  = fs.existsSync(fmtPath);
        if(!origExists && !fmtExists) {
            console.log(
                `  WARNING: missing from both ${origBaseDir} and ${fmtBaseDir} -- skipping`
            );
            ++missingCount;
            continue;
        } // if
        if(!origExists) {
            console.log(`  WARNING: missing from ${origBaseDir} -- skipping`);
            ++missingCount;
            continue;
        }
        if(!fmtExists) {
            console.log(`  WARNING: missing from ${fmtBaseDir} -- skipping`);
            ++missingCount;
            continue;
        }

        try {
            if( compareOne(origPath, fmtPath, rel, rel) ) ++okCount;
            else                                          ++mismatchCount;
        }
        catch(e) {
            console.log(`  ERROR: ${e.message}`);
            ++mismatchCount;
        }
    } // for

    console.log('');
    console.log(`SUMMARY: ${okCount} OK, ${mismatchCount} MISMATCH/ERROR, ${missingCount} MISSING `
              + `(of ${okCount + mismatchCount + missingCount} files checked)`);

    if(mismatchCount > 0 || missingCount > 0) process.exit(1);
} // runBatch

function main()
{
    const args = process.argv.slice(2);
         if(args.length === 2) runSingle( args[0], args[1] );
    else if(args.length === 3) runBatch( args[0], args[1], args[2] );
    else                       {
        printUsage();
        process.exit(2);
    }
} // main

main();
