#!/usr/bin/env node
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */
/**
 * Js_ts_syntax_check.js - syntax checker for JavaScript and TypeScript source files.
 *
 * Uses the TypeScript compiler API to parse each input file. If parsing
 * succeeds and no parse diagnostics are produced, the file is syntactically
 * valid JavaScript or TypeScript. No code is executed and no output files
 * are generated.
 *
 * This tool shares the same parser (`ts.createSourceFile()`) used by
 * js_ts_content_diff.js, ensuring syntax validation is consistent with the
 * formatter verification tools.
 *
 * Usage:
 *     node js_ts_syntax_check.js <file.(js|ts)> [file2.(js|ts) ...]
 *
 * Exit 0 if all files parse successfully, 1 if one or more files contain
 * syntax errors, 2 if the command-line usage is invalid.
 */

'use strict';

const fs = require('fs');
const ts = require('typescript');

function scriptKindFor(file)
{
  // `.tsx`/`.jsx` need their own dedicated ScriptKind, not a fallback to plain TS/JS -- the
  // TypeScript compiler's TS/JS parsers don't do JSX disambiguation at all (a bare `<T>` is
  // always a type-assertion cast, never a JSX element), so a `.tsx` file parsed as plain TS
  // (the old fallback here, since it doesn't end in exactly `.ts`) makes every real JSX
  // element in the file a parse error, and a `.tsx` file's real TS generics (parsed as plain
  // JS, since `.tsx` isn't `.ts`) make every generic a parse error instead. Found via
  // STATE_JS_TS.md's Step 2 Increment 5 real-corpus dogfood: excalidraw/excalidraw's `.tsx`
  // files with generic-heavy hooks (`useState<T>()`, etc.) were false-failing under the old
  // JS fallback.
  if( file.endsWith('.tsx') ) return ts.ScriptKind.TSX;
  if( file.endsWith('.jsx') ) return ts.ScriptKind.JSX;

  return file.endsWith('.ts') ? ts.ScriptKind.TS : ts.ScriptKind.JS;
} // scriptKindFor

// `export Foo from './Bar';` (bare default re-export, no `{ }`/`*`) is a real,
// still-in-use Babel-only syntax (`@babel/plugin-proposal-export-default-from`,
// never standardized into ECMAScript or TypeScript) that `ts.createSourceFile`
// cannot parse under any ScriptKind -- found via STATE_JS_TS.md's Step 2
// Increment 5 real-corpus dogfood: reactstrap/reactstrap's `src/index.js` uses
// this form for every one of its ~100 re-exports, and fails identically on the
// original, unformatted file (not a formatter bug). Rewritten line-for-line to
// the standard-equivalent `export { default as Foo } from './Bar';` before
// parsing so line numbers stay aligned for any other diagnostic on the same
// file; this is a checker-only rewrite; it never touches formatter output.
const LEGACY_EXPORT_DEFAULT_FROM = /^(\s*)export\s+([A-Za-z_$][\w$]*)\s+from(\s+['"][^'"]+['"]\s*;?\s*)$/;

function rewriteLegacyExportDefaultFrom(source)
{
  return source
    .split('\n')
    .map( (line) => line.replace(LEGACY_EXPORT_DEFAULT_FROM, '$1export { default as $2 } from$3') )
    .join('\n');
} // rewriteLegacyExportDefaultFrom

function checkFile(fileName)
{
    const source = rewriteLegacyExportDefaultFrom( fs.readFileSync(fileName, 'utf8') );

  const sf = ts.createSourceFile(
    fileName,
    source,
    ts.ScriptTarget.Latest,
    /*SetParentNodes*/ true,
    scriptKindFor(fileName) );

  return sf.parseDiagnostics;
} // checkFile

function main()
{
  if(process.argv.length < 3) {
    console.error('Usage: js_ts_syntax_check.sh <file.(js|ts)> [file2.(js|ts) ...]');
    process.exit(2);
  }

    let ok = true;

  for( const file of process.argv.slice(2) ) {
    const diagnostics = checkFile(file);

    if(diagnostics.length !== 0) {
        ok = false;

      for(const d of diagnostics) {
        const pos = d.file.getLineAndCharacterOfPosition(d.start);

        console.error(
            '%s:%d:%d: %s',
            file,
            pos.line + 1,
            pos.character + 1,
            ts.flattenDiagnosticMessageText(d.messageText, '\n')
        );
      } // for
    } // if
  } // for

  process.exit(ok ? 0 : 1);
} // main

main();
