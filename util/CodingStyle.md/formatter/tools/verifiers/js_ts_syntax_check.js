#!/usr/bin/env node
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */
/**
 * js_ts_syntax_check.js - syntax checker for JavaScript and TypeScript source files.
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
  return file.endsWith('.ts') ? ts.ScriptKind.TS : ts.ScriptKind.JS;
}

function checkFile(fileName)
{
  const source = fs.readFileSync(fileName, 'utf8');

  const sf = ts.createSourceFile(
    fileName,
    source,
    ts.ScriptTarget.Latest,
    /*setParentNodes*/ true,
    scriptKindFor(fileName));

  return sf.parseDiagnostics;
}

function main()
{
  if (process.argv.length < 3)
  {
    console.error(
      'Usage: js_ts_syntax_check.sh <file.(js|ts)> [file2.(js|ts) ...]');
    process.exit(2);
  }

  let ok = true;

  for (const file of process.argv.slice(2))
  {
    const diagnostics = checkFile(file);

    if (diagnostics.length !== 0)
    {
      ok = false;

      for (const d of diagnostics)
      {
        const pos = d.file.getLineAndCharacterOfPosition(d.start);

        console.error(
          '%s:%d:%d: %s',
          file,
          pos.line + 1,
          pos.character + 1,
          ts.flattenDiagnosticMessageText(d.messageText, '\n'));
      }
    }
  }

  process.exit(ok ? 0 : 1);
}

main();
