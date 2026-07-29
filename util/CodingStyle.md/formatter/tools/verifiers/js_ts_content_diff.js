#!/usr/bin/env node
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */
/**
 * Content-preservation checker for JS/TS, modeled on java_content_diff.java
 * and kotlin_content_diff.java (same reasoning, applied via the TypeScript
 * compiler API instead of javac/PSI). One script handles both `.js` and
 * `.ts` -- ts.createSourceFile parses plain JS just fine, same idiom used
 * by every other Node-based *_syntax_check.js/*_content_diff.py tool in this repo.
 *
 * Gotcha (same shape as the Kotlin PSI one): TypeScript's AST does NOT
 * attach comments as tree nodes -- node.getChildren() never yields them.
 * Comments must be recovered separately from the raw source text via
 * ts.getLeadingCommentRanges/ts.getTrailingCommentRanges, scanned once
 * per top-level statement position (leading-only is enough since every
 * comment is some statement's leading comment or the final trailing one;
 * dedup by source position to avoid double-counting a comment that is
 * simultaneously one statement's trailing and the next statement's
 * leading range).
 *
 * This formatter *intentionally* reorders/reformats some JS/TS content
 * (js-import-order sorting, declaration-alignment column padding,
 * normalize-comment-start-case), so:
 *     - the import-statement block is compared as a MULTISET (js-import-order
 *       legitimately reorders/sorts them)
 *     - every other top-level statement/declaration is compared IN ORDER, via
 *       a leaf-token canonicalization (identifiers/literals/keywords/
 *       punctuation joined with single spaces, whitespace collapsed)
 *     - comments are extracted separately (see above) and compared as a
 *       MULTISET, whitespace-normalized AND case-normalized (lowercased) --
 *       a case-only change is expected (normalize-comment-start-case) and
 *       must not be flagged, but a dropped/corrupted comment still is
 *
 * NOTE: `typescript@7` (the new native tsgo-based rewrite) does NOT expose
 * createSourceFile/getLeadingCommentRanges -- only typescript@5.x's classic
 * API does. This system's ~/mynpm/node_modules/typescript is pinned to
 * 5.9.3 for that reason (installed via `npm install --prefix ~/mynpm
 * typescript@5`).
 *
 * JSX/TSX are explicitly out of scope (per STATE_JS_TS.md, the formatter
 * itself does not handle them yet) -- this tool only targets plain .js/.ts.
 *
 * Env needed (same as node/tsc dogfood use, see STATE_JS_TS.md's
 * "Tools/compiler used"):
 *     export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
 *     export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules:~/mynpm/node_modules
 *     export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
 *
 * Usage:
 *     node js_ts_content_diff.js <original.(js|ts)> <formatted.(js|ts)>
 *
 * Exit 0 if content is preserved, 1 with a description of each mismatch
 * otherwise, 2 on usage error.
 */
'use strict';

const fs   = require('fs');
const path = require('path');
const ts   = require('typescript');

function normalizeWhitespace(s)
{
  return s.trim().replace(/\s+/g, ' ');
}

function scriptKindFor(file)
{
  return file.endsWith('.ts') ? ts.ScriptKind.TS : ts.ScriptKind.JS;
}

function parse(source, fileName)
{
  return ts.createSourceFile(
    fileName, source, ts.ScriptTarget.Latest, /*SetParentNodes*/ true, scriptKindFor(fileName) );
}

/**
 * Leaf-token canonicalization: every terminal token's text (identifiers,
 *  keywords, literals, punctuation), whitespace collapsed -- comments are
 *  never tree nodes so they never appear here regardless.
 *
 *  A `Block` ({ ... }) containing exactly one statement is canonicalized
 *  identically to that single statement unwrapped (the `{`/`}` tokens are
 *  simply skipped in that specific shape) -- STYLE.md Sec.10 lets the
 *  formatter legitimately render a single-statement block with or without
 *  braces (`if (x) foo();` vs `if (x) { foo(); }`), in either direction,
 *  for any control-flow construct (if/else/for/while/do/etc., not just
 *  `if`), so both shapes must produce the same canonical token stream.
 *  A block with zero or 2+ statements is walked normally (braces included)
 *  -- only the exact one-statement shape is unwrapped, so a genuinely
 *  added/removed statement, or a different single statement, is still
 *  caught by the ordinary token-level comparison below.
 */
function canonicalize(node)
{
    const parts = [];
  ( function walk(n)
  {
    if( ts.isBlock(n) && n.statements.length === 1 ) {
      walk( n.statements[0] );
      return;
    }
    // A `/** ... */` JSDoc comment is parsed by the TS compiler as a real
    // AST child of its following declaration (even with no @tags), unlike
    // an ordinary `//`/`/* */` comment. Skip it here -- it's already
    // covered by collectComments() (which works off raw source ranges,
    // independent of AST attachment), so including its raw text here too
    // would both double-count it and bypass that path's period-stripping/
    // case-normalization tolerance, false-flagging exactly the same
    // intentional transformations this canonicalization is meant to
    // tolerate.
    if( ts.isJSDoc(n) ) return;
    const kids = n.getChildren();
    if(kids.length === 0) {
      const t = n.getText();
      if(t.length > 0) parts.push(t);
    }
    else {
      for(const k of kids) walk(k);
    }
  } )(node);

  return normalizeWhitespace( parts.join(' ') );
} // canonicalize

function isImportStatement(stmt)
{
  return ts.isImportDeclaration(stmt) || ts.isImportEqualsDeclaration(stmt);
}

function topLevelBuckets(sourceFile)
{
    const imports = [];
    const others  = [];
  for(const stmt of sourceFile.statements) {
    if( isImportStatement(stmt) ) imports.push( canonicalize(stmt) );
    else                          others.push( canonicalize(stmt) );
  }
  imports.sort();

  return { imports, others };
} // topLevelBuckets

/**
 * Comments are not tree nodes -- recover them from raw source text via
 *  getLeadingCommentRanges, scanning at each token's start position plus
 *  position 0 (covers any comment before the very first token) and the
 *  end of file (covers a final trailing comment with nothing after it).
 *  Dedup by [pos,end) since the same range can be reached from more than
 *  one scan point.
 */
function collectComments(sourceFile, sourceText)
{
    const seen = new Set();
    const out  = [];

  function addRangesAt(pos)
  {
    const ranges = ts.getLeadingCommentRanges(sourceText, pos) ||[];
    for(const r of ranges) {
        const key = r.pos + ':' + r.end;
      if( seen.has(key) ) continue;
      seen.add(key);
      out.push( stripCommentDelims( sourceText.slice(r.pos, r.end) ) );
    }
  } // addRangesAt

  addRangesAt(0);
  ( function walk(n)
  {
    addRangesAt( n.getFullStart() );
    n.forEachChild(walk);
  } )(sourceFile);
  addRangesAt(sourceText.length);

  out.sort();

  return out;
} // collectComments

/**
 * Strips a single trailing `.` immediately before the comment's closing
 *  delimiter/end (after outer delimiters are already stripped), mirroring
 *  `normalize-comment-end-period`'s `stripSoleTrailingPeriod` formatter
 *  behavior (MiscRuleCore.java) so an intentional period-strip isn't
 *  flagged as a content change. Only the very last `.` is affected -- a
 *  mid-sentence period, or a trailing ellipsis (`...`), is left alone.
 */
function stripSoleTrailingPeriod(t)
{
    const trimmed = t.replace(/\s+$/, '');
  if( trimmed.endsWith('.') && !trimmed.endsWith('..') ) return trimmed.slice(0, -1);

  return trimmed;
}

function stripCommentDelims(text)
{
    let t = text.trim();
  if( t.startsWith('///') )      t = t.slice(3);
  else if( t.startsWith('//') )  t = t.slice(2);
  else if( t.startsWith('/**') ) t = t.slice( 3, Math.max(3, t.length - 2) );
  else if( t.startsWith('/*') )  t = t.slice( 2, Math.max(2, t.length - 2) );
    t = stripSoleTrailingPeriod(t);

  return normalizeWhitespace(t).toLowerCase();
} // stripCommentDelims

function diffMultisets(label, a, b)
{
    const mismatches = [];
    const bCopy      = b.slice();
    const onlyInA    = [];
  for(const s of a) {
    const idx = bCopy.indexOf(s);
    if(idx === -1) onlyInA.push(s);
    else           bCopy.splice(idx, 1);
  }
  if(onlyInA.length > 0) mismatches.push(
      label + ': present in original, missing from formatted: ' + JSON.stringify(onlyInA)
  );
  if(bCopy.length > 0) mismatches.push(
      label + ': present in formatted, missing from original: ' + JSON.stringify(bCopy)
  );

  return mismatches;
} // diffMultisets

function main()
{
    const args = process.argv.slice(2);
  if(args.length !== 2) {
    console.error('Usage: node js_ts_content_diff.js <original.(js|ts)> <formatted.(js|ts)>');
    process.exit(2);
  }
    const [origPath, fmtPath] = args;
    const origSrc             = fs.readFileSync(origPath, 'utf8');
    const fmtSrc              = fs.readFileSync(fmtPath, 'utf8');

    const origFile = parse( origSrc, path.basename(origPath) );
    const fmtFile  = parse( fmtSrc, path.basename(fmtPath) );

    const mismatches = [];

    const origBuckets = topLevelBuckets(origFile);
    const fmtBuckets  = topLevelBuckets(fmtFile);

  mismatches.push( ...diffMultisets('imports', origBuckets.imports, fmtBuckets.imports) );

  if(origBuckets.others.length !== fmtBuckets.others.length) mismatches.push(
      'non-import top-level statement count changed: ' + origBuckets.others.length + ' -> ' + fmtBuckets.others.length
  );
    const n = Math.min(origBuckets.others.length, fmtBuckets.others.length);
  for(let i = 0; i < n; ++i) {
    if( origBuckets.others[i] !== fmtBuckets.others[i] ) mismatches.push(
        'non-import top-level statement #' + i + ' structure/content differs'
    );
  }

  mismatches.push( ...diffMultisets(
    'comments', collectComments(origFile, origSrc), collectComments(fmtFile, fmtSrc) ) );

  if(mismatches.length === 0) {
    console.log('OK: content preserved (' + origPath + ' == ' + fmtPath + ')');
  }
  else {
    console.log('MISMATCH: content differs between ' + origPath + ' and ' + fmtPath);
    for(const m of mismatches) console.log('  ' + m);
    process.exit(1);
  }
} // main

main();
