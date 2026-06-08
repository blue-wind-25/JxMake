# Changelog — kate_jxMake.xml

All notable changes to the JxMake Kate syntax highlighting file are documented here.

---

## [1.0.123] — 2026-06-08

### Bug Fixes

- **`decnum` / `hexnum` entities — spurious sign prefix**
  Both entities included an optional leading `[+-]`, causing operator characters
  to be consumed as part of a number literal (e.g. the `-` in `-${X}` or `+5`
  was incorrectly absorbed). Signs are operators; the entities now match digits only:
  `decnum` = `[0-9]+`, `hexnum` = `0[xX][0-9a-fA-F]+`.

- **`NormalText` root context — `lineEndContext="#pop"` caused highlighter reset**
  The root context was popping the stack on every line end, leaving the highlighter
  in an undefined state. Fixed to `lineEndContext="#stay"`.

- **`NormalText` semicolon `;` — `context="#pop"` caused premature stack exit**
  The `;` statement-separator rule in the root context was incorrectly popping the
  stack. Fixed to `context="#stay"`.

- **`echo` regex — bare `echo` was never highlighted**
  `\becho(?:ln)\b` requires `ln` to be present, so a bare `echo` statement was
  never matched. Fixed to `\becho(?:ln)?\b` (the `ln` suffix is now optional).

- **`label` lookahead — single-quoted string label specs not recognised**
  `\blabel(?=\s+&symname;\s+:)` only matched when the label spec was a bare
  identifier. The grammar also allows `label 'string' :`. Extended lookahead to
  `(?=\s+(?:&symname;|&apos;[^&apos;]*&apos;)\s*:)`.

- **`LabelEval` context — symname rule missing `attribute`**
  The `\b&symname;\b` rule inside `LabelEval` had no `attribute=` declaration,
  leaving the label identifier unstyled. Added `attribute="NormalText"`.

- **`:::include` regex — `\b` placed after lookahead had no effect**
  `:::s?include(_once)?(?=\s+&libname;)\b` placed the word-boundary assertion
  inside/after the lookahead, where it cannot match. Moved to before the
  lookahead: `:::s?include(_once)?\b(?=\s+&libname;)`.

- **`MacroUsage` context — was empty, macro name never consumed or highlighted**
  The context body was empty, so the macro name following `.$name` or `.name`
  was never matched or coloured. Added a `&symname;` rule to consume and
  highlight it.

- **`EvalStatement2` binary-op regex — trailing empty alternative `|)` matched everywhere**
  The first binary-operator regex ended with `|==|!=|)(?=\s+)`, where the final
  `|)` is an empty alternative that matches zero characters at any position.
  Removed the empty alternative; the regex now ends with `|==|!=)(?=\s+)`.

- **`SROperEvalS` / `SROperEvalD` — wrong attribute colour**
  Both brace-operator contexts (`{ }` set-creation and `{{ }}` stack shortcut)
  used `attribute="PROperEval"` (the path-shortcut colour), making them
  visually indistinguishable from path operators. A new `SROperEval` itemData
  was added and both contexts updated to use it.

- **`VarAssign` — number rules had wrong order and missing octnum**
  `decnum` was tested before `hexnum`, so `0xFF` would match as `0` (decimal)
  then leave `xFF` unmatched. Reordered to `octnum → hexnum → decnum` and added
  the missing `\b&octnum;\b` rule.

- **`VarAssign` — combining-string prefix `."…"` not recognised**
  The dot-prefix rule only matched `."[["` (the multiline-string marker), missing
  the ordinary combining operator `."…"`. Fixed to `.(?=&quot;)` so it matches
  any double-quoted string following a dot.

### Additions

- **Octal integer literals `0o…` / `0O…`**
  The grammar specifies `integer-lit ::= '0o' <OCT-DIGITS>` but the file had no
  support for octal literals. Added `<!ENTITY octnum "0[oO][0-7]+">` and
  `\b&octnum;\b` rules in all numeric contexts: `NormalText`, `VarAssign`,
  `EvalStatement2`, `VariableEvalN`, and the `EvalStatement2` expression context.
  The entity is tested before `hexnum` and `decnum` to ensure correct priority.

- **Macro plain-form invocation `.name` now highlighted**
  The grammar allows two macro invocation forms: `.$macroname` and `.macroname`.
  Only the `.$name` form was detected. Added a `\.(?=&symname;\b)` rule for the
  plain `.name` form, styled as `MacroDef`.

- **`OptionDef` context — `CommentEval` include added**
  Comments (`#` and `(*…*)`) are syntactically valid inside `.option` statements
  but were not recognised there. Added `<IncludeRules context="CommentEval"/>`.

- **`Pragma` context — `LineContinue` rule added**
  Pragma lines can be continued with `\` like any other statement, but the
  `Pragma` context had no `LineContinue` rule. Added.

- **`CommandEvalSE` context — `CommentEval` include added**
  Comments are valid in the space before a shell command prefix (`-@`, `+@`, etc.)
  but were not recognised. Added `<IncludeRules context="CommentEval"/>`.

- **`SROperEval` itemData added**
  New styling entry for the `{ }` set-creation and `{{ }}` stack-shortcut
  operators, distinct from the path-shortcut `PROperEval` style.

- **KDE5 install path comment added**
  Added `<!-- ~/.local/share/katepart5/syntax/ (KDE5) -->` alongside the
  existing KDE4 path comment.

---

## [1.0.122] and earlier

No changelog maintained prior to this revision.
