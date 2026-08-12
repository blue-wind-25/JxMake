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
 * Usage (two modes -- see also the usage message in main below):
 *     Single pair:
 *         node js_ts_content_diff.js <original.(js|ts)> <formatted.(js|ts)>
 *     Batch (one Node process invocation over a whole corpus, avoiding a
 *     process restart per file -- rel-path list, one path per line, relative
 *     to both base dirs alike):
 *         node js_ts_content_diff.js <original_base_dir> <formatted_base_dir> <js_ts_rel_path_file_list.txt>
 *
 * Before each pair's AST diff, a "[yyyy-MM-dd HH:mm:ss.SSS] <relative path>"
 * line is printed -- lets a hang/slow file in a large batch run be pinpointed
 * (same "print immediately before the risky step" precedent as
 * Main.main's/ServerMode.FormatHandler's own "processing <file>" stderr
 * trace, see STATE_COMMON.md). In batch mode, a rel-path missing from either
 * base dir (or both) is a warning, not a crash -- the file is skipped and
 * the run continues; the final SUMMARY line and process exit code still
 * reflect it. One pair's compareOne() is wrapped in try/catch so a bad file
 * doesn't abort the rest of the batch.
 *
 * Exit 0 if content is preserved (all pairs, in batch mode), 1 if any
 * mismatch/missing file/error is found (description printed for each), 2 on
 * a usage error.
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
 *
 *  Two further narrow tolerances (added after the lodash/lodash dogfood
 *  pass found both as false positives -- see STATE_JS_TS.md):
 *   - A single-parameter arrow function's `(` `)` tokens are skipped when
 *     that one parameter is a plain identifier with no type annotation,
 *     default, rest, or optional marker -- STYLE_JS_TS.md Sec.6 legitimately
 *     adds parens around a bare single-identifier param (`x => x+1` becomes
 *     `(x) => x+1`), so both shapes must canonicalize identically.
 *   - A `for(...)` statement's incrementor clause, when it is a bare
 *     `++`/`--` unary expression (prefix or postfix, e.g. `i++` vs `++i`),
 *     is canonicalized to a fixed prefix-position form -- STYLE.md Sec.4's
 *     pre-increment-except-when-post-required rule legitimately rewrites an
 *     unused loop-incrementor's postfix form to prefix. Deliberately scoped
 *     to the incrementor slot only (not incrementor-shaped expressions
 *     elsewhere), since Sec.4 only mandates the rewrite when the value is
 *     otherwise unused -- a genuine ++/-- swap anywhere else could still be
 *     a real, catchable bug and must not be masked.
 */
function isBareableArrowParam(fn)
{
  if(fn.parameters.length !== 1) return false;
    const p = fn.parameters[0];

  return !p.dotDotDotToken && !p.type && !p.initializer && !p.questionToken && ts.isIdentifier(
      p.name
  );
} // isBareableArrowParam

function isPlainIncDec(n)
{
  return ( ts.isPostfixUnaryExpression(n) || ts.isPrefixUnaryExpression(n) )
      && (n.operator === ts.SyntaxKind.PlusPlusToken || n.operator === ts.SyntaxKind.MinusMinusToken);
}

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
    if( ts.isArrowFunction(n) && isBareableArrowParam(n) ) {
      for( const k of n.getChildren() ) {
        if(k.kind === ts.SyntaxKind.OpenParenToken || k.kind === ts.SyntaxKind.CloseParenToken) continue;
        walk(k);
      }
      return;
    } // if
    if( ts.isForStatement(n) ) {
      for( const k of n.getChildren() ) {
        if( n.incrementor && k === n.incrementor && isPlainIncDec(k) ) {
          parts.push(k.operator === ts.SyntaxKind.PlusPlusToken ? '++' : '--');
          walk(k.operand);
        }
        else walk(k);
      } // for
      return;
    } // if
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
       if( t.startsWith('///') ) t = t.slice(3);
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

function pad2(n)
{
  return n < 10 ? '0' + n : '' + n;
}

function pad3(n)
{
       if(n < 10)  return '00' + n;
  else if(n < 100) return '0' + n;

  return '' + n;
}

/**
 * "[yyyy-MM-dd HH:mm:ss.SSS] <relative path>" -- Node has no built-in
 *  strftime, so this is formatted manually via Date getters (all local time,
 *  zero-padded), matching java_content_diff.java's/kotlin_content_diff.java's
 *  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS") output shape
 *  exactly.
 */
function timestampNow()
{
  const d = new Date();

  return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()) +
    ' ' + pad2(d.getHours()) + ':' + pad2(d.getMinutes()) + ':' + pad2(d.getSeconds()) +
    '.' + pad3(d.getMilliseconds());
}

/**
 * Printed immediately before a pair's AST diff starts -- if the tool
 *  hangs/is slow on one particular file in a large batch, this line
 *  (already flushed to stdout for every prior file) shows exactly which
 *  file and when, mirroring Main.main's/ServerMode.FormatHandler's own
 *  "processing <file>" trace precedent (STATE_COMMON.md).
 */
function printTimestampedHeader(relPath)
{
  console.log( '[' + timestampNow() + '] ' + relPath );
}

/**
 * The full single-pair AST-diff check (imports/statements/comments), shared
 *  by both the single-pair and batch modes. Assumes both paths are already
 *  confirmed to exist -- callers are responsible for the missing-file check
 *  so a batch run can warn-and-skip instead of throwing. Labels are passed
 *  separately from the paths actually read so batch mode can print a short
 *  relative path while still resolving full paths for fs.readFileSync.
 *
 * A `ts.createSourceFile` parse failure does not throw the way javac's
 *  JavacTask.parse() does -- the TypeScript compiler API always returns a
 *  SourceFile, with parse errors surfaced only as diagnostics attached to
 *  it (never consulted by this tool, single-pair mode included, both before
 *  and after this batch-mode extension). So there is nothing extra for
 *  batch mode to special-case here versus single-pair mode: a malformed
 *  file already degraded gracefully (best-effort AST, likely surfacing as
 *  an ordinary content mismatch rather than a hard error) before this
 *  change, and continues to do so in both modes now.
 */
function compareOne(origPath, fmtPath, origLabel, fmtLabel)
{
  const origSrc = fs.readFileSync(origPath, 'utf8');
  const fmtSrc  = fs.readFileSync(fmtPath, 'utf8');

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
    console.log('OK: content preserved (' + origLabel + ' == ' + fmtLabel + ')');

    return true;
  }
  else {
    console.log('MISMATCH: content differs between ' + origLabel + ' and ' + fmtLabel);
    for(const m of mismatches) console.log('  ' + m);

    return false;
  }
} // compareOne

function printUsage()
{
  console.error('Usage: js_ts_content_diff.sh <original.(js|ts)> <formatted.(js|ts)>');
  console.error('       js_ts_content_diff.sh <original_base_dir> <formatted_base_dir> <js_ts_rel_path_file_list.txt>');
}

function runSingle(origArg, fmtArg)
{
  printTimestampedHeader(origArg);

  const origExists = fs.existsSync(origArg);
  const fmtExists  = fs.existsSync(fmtArg);
  if(!origExists || !fmtExists) {
         if(!origExists && !fmtExists) console.log('WARNING: both ' + origArg + ' and ' + fmtArg + ' are missing');
    else if(!origExists)               console.log('WARNING: ' + origArg + ' is missing');
    else                                console.log('WARNING: ' + fmtArg + ' is missing');
    process.exit(1);
  }

  if( !compareOne(origArg, fmtArg, origArg, fmtArg) ) process.exit(1);
}

function runBatch(origBaseDir, fmtBaseDir, fileListPath)
{
  const relPaths = fs.readFileSync(fileListPath, 'utf8').split('\n');

  let okCount = 0, mismatchCount = 0, missingCount = 0;

  for(let rel of relPaths) {
    rel = rel.trim();
    if(rel === '') continue;

    const origPath = path.join(origBaseDir, rel);
    const fmtPath  = path.join(fmtBaseDir, rel);

    printTimestampedHeader(rel);

    const origExists = fs.existsSync(origPath);
    const fmtExists  = fs.existsSync(fmtPath);
    if(!origExists && !fmtExists) {
      console.log('  WARNING: missing from both ' + origBaseDir + ' and ' + fmtBaseDir + ' -- skipping');
      ++missingCount;
      continue;
    }
    if(!origExists) {
      console.log('  WARNING: missing from ' + origBaseDir + ' -- skipping');
      ++missingCount;
      continue;
    }
    if(!fmtExists) {
      console.log('  WARNING: missing from ' + fmtBaseDir + ' -- skipping');
      ++missingCount;
      continue;
    }

    try {
      if( compareOne(origPath, fmtPath, rel, rel) ) ++okCount;
      else                                          ++mismatchCount;
    }
    catch(e) {
      console.log('  ERROR: ' + e);
      ++mismatchCount;
    }
  } // for

  console.log('');
  console.log(
    'SUMMARY: ' + okCount + ' OK, ' + mismatchCount + ' MISMATCH/ERROR, ' + missingCount +
    ' MISSING (of ' + (okCount + mismatchCount + missingCount) + ' files checked)'
  );

  if(mismatchCount > 0 || missingCount > 0) process.exit(1);
}

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
