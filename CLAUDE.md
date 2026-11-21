# JxMake — working notes

Paths are relative to this directory. `src/CLAUDE.md` has the build, test and commit rules.

## JxMake scripts (`JxMakeFile`, `*.jxm`)

JxMake script is its own language, not GNU Make. Before reading, writing, reviewing or debugging
a JxMake script (a `JxMakeFile`, any `*.jxm` file, or program text for `jxmake -e`):

1. Read `docs/txt/JxMake-Script-AI-Reference.txt` **in full**, once per session, before the first
   such task. It holds the formal grammar, the language topics and every built-in function's
   signature. Don't write JxMake from memory or by analogy with Make or shell.
2. When the reference doesn't settle a question, read the matching chapter of the full manual in
   `docs/txt/en_US/`:
   - `02`–`06`: strings, dependency lists, variables, special variables, regex in variables
   - `07-Built-In-Functions.txt`, `99-Appendix-X_Built-In-Function-Parameters.txt`: built-ins
   - `08-User-Defined-Function.txt`, `09-Targets.txt`, `10-Flow-Control.txt`, `11-Loop.txt`,
     `12-Numerical-Operations.txt`
   - `13-Loadable-Libraries.txt`: the libraries in `src/0-JxMake/lib/` (read the library source
     too)
3. Test a snippet with `jxmake -e '<program text>'` or a small `-f` script (see `src/CLAUDE.md`
   for running from the classes) instead of guessing.

Exception: the `*.jxm` fixtures under `util/CodingStyle.md/formatter/test/` are formatter test
data, governed by `util/CodingStyle.md/formatter/CLAUDE.md` and `STYLE_JXMAKE.md`. Read the
reference for them only when the work depends on what the script means.

Gotchas that are easy to miss (all confirmed by running them):

- A target block ends with `endtarget`.
- `echoln` cannot take a function call directly; assign the result to a variable first.
- `local` is only valid inside a function or target, not at top level.
- `${X}<n>` is not expanded inside a `"..."` string; assign `${X}<n>` to a variable first.
- `X +:= <empty>` (and `X := ''`) still add an empty set member, so `$part_count` counts it.
- `&val` only means `val &!= ''`. It works in conditions, not as a function argument (use
  `$cmp(${val}, '&!=', '')` there).
- `$re_match` must match the **whole** string (no `^`/`$` needed).
- A function call cannot span lines unless each line ends with `\`.
- A script outside `src/0-JxMake/lib/` loads a library with `:::include <GitHubTool>`;
  `:::include_once 'GitHubTool.jxm'` is relative to the including file.
- A library error result (e.g. GitHubTool, SourceForgeTool) is `{ '' <status> <reason> ... }`; the
  empty first member is deliberate, since callers test `if !&${Res}<1>`.
