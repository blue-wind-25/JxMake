# STATE_DATA_FORMATS.md — Data & Markup Format JAR Support Tracker

Read `STATE_COMMON.md` first (shared commit/ambiguity/testing conventions).
`STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` are NOT required reading for this job.

---

## Purpose

Tracks data/markup format support in the deterministic JAR formatter
(`util/CodingStyle.md/formatter/`), per `STYLE_DATA_FORMATS.md` (JSON/JSON5,
XML, CSS, HTML5, YAML, TOML). **All six are now DONE** — real
tokenizer/parser/printer logic landed for each, `make test` green (see
Checklist for per-format notes/deferred edge cases; RDD_KEY_190/191/192/193/
194 for implementation history). Remaining gap: HTML5's `<script>` dispatch
to JS/TS — real JS content must be wrapped in `//% JXM_CFMT_DIS`/`ENA` until
`STATE_JS_TS.md`'s job lands a real JS/TS formatter (see that file's
checklist and this file's HTML5 entry below).

---

## Scope

`STYLE_DATA_FORMATS.md` covers non-imperative data/markup formats — no
functions/control flow, so most shared `STYLE.md` imperative-language rules
don't apply. Six sub-formats, each stating which `STYLE.md` sections it
borrows from:

1. **JSON / JSON5** (§1) — RFC 8259 JSON plus JSON5. Key/value `:` column
   alignment (declaration-alignment group/group-break shape, but always a
   space before `:`, unlike declaration `=`); bracket/complexity tight-loose
   padding (reuses existing nesting-complexity signal); JSON5-only
   multi-line strings via backslash-newline continuation (opaque, preserved
   exactly, same treatment as Java text blocks/JS template literals). Plain
   `.json` has no comment syntax, so `JXM_CFMT_DIS`/`ENA`/`CFG` directives
   only work in `.json5`.
2. **XML** (§2) — XML 1.0 and dialects (XHTML, SVG, MathML, RSS, Atom,
   Android XML, Maven POM, Ant `build.xml`, IntelliJ XML). **Uses existing
   global `indent-size`/`indent-style` config, no XML-specific indent
   config.** Tag/attribute formatting (nesting indent, attribute wrap on
   overflow, self-closing tags preserved, attribute order preserved incl.
   `xmlns`). `<!DOCTYPE>`/PIs/CDATA opaque/preserved verbatim, except CDATA
   that is direct content of a `<script>`/`<style>` tag, which unwraps and
   dispatches per §4.2. Directives: single `<!--% ... -->` block form (no
   line-comment equivalent in XML).
3. **CSS** (§3) — modern CSS (CSS 3+ modules). Property/value `:` column
   alignment (same group shape as JSON's, space-before-`:`); at-rules
   (`@media`, `@supports`, `@keyframes`, `@font-face`) and CSS Nesting Module
   `&` blocks are headers starting their own independent alignment group,
   recursing per nested block. Directives: single `/*% ... */` block form
   (CSS has no `//` line comment).
4. **HTML5** (§4) — tag formatting reuses XML's §2.2 rules directly, with
   one override: void elements (`area`, `base`, `br`, `col`, `embed`, `hr`,
   `img`, `input`, `link`, `meta`, `param`, `source`, `track`, `wbr`) never
   get a closing tag and any self-closing `/` is normalized away. Main
   design point is **dispatch**: `<style>` content splices to the CSS
   formatter (§3) and back; `<script>` content splices to the JS/TS
   formatter (`STYLE_JS_TS.md`) and back, incl. CDATA-wrapped variant; a
   `<script type="...">` with non-JS/TS type stays opaque.
5. **YAML** (§5) — YAML 1.1/1.2, incl. multi-document streams. Key/value `:`
   column alignment (same group shape as JSON's, space-before-`:`); sequence
   items indent one level deeper than their parent mapping key; flow-style
   collections (`{a: 1}`, `[1, 2]`) preserved as written unless they'd
   exceed `line-length`, in which case converted to block style;
   anchors/aliases/tags and block scalars (`|`/`>`) preserved verbatim/
   opaque. **Uses the existing global `indent-size`, but `indent-style` is
   ignored for YAML** (YAML forbids tab indentation — always
   space-indented). Directives: single `#% ...` line form (YAML has no
   block comment).
6. **TOML** (§6) — TOML v1.0. Key/value `=` column alignment reuses
   STYLE.md §5/§6's assignment-alignment shape directly (no forced
   space-before-`=`, unlike JSON/YAML's `:`); table/array-of-table headers
   (`[section]`/`[[array]]`) are headers, not declarations, and get no
   added indentation for keys under them (TOML nests via dotted header
   names, not indentation); arrays reuse tight/loose bracket rule; inline
   tables always single-line (TOML grammar constraint, not a style choice);
   dotted keys and string quote styles preserved as written. **TOML doesn't
   use `indent-size`/`indent-style` at all.** Directives: single `#% ...`
   line form (TOML has no block comment).

Dispatch lives in shared `Lang.java`/`Main.java`/`ServerMode.java`/
`Config.java` (see routing `CLAUDE.md` table). Per-sub-format rule classes,
all with real logic now: `rules/JsonSpecificRule.java` (JSON/JSON5),
`rules/CssSpecificRule.java` (CSS), `rules/YamlSpecificRule.java`,
`rules/TomlSpecificRule.java`, `rules/XmlSpecificRule.java` (XML and HTML5,
one shared class per RDD_KEY_188).

---

## Resolved Design Decisions

Full text lives in `RDD_LOG.md` (shared with `STATE_C_CPP_JAVA.md`/
`STATE_KOTLIN.md` — continue existing `RDD_KEY_n` numbering, don't restart).
See `STATE_COMMON.md`'s lookup convention (`grep -Fm1`, no `-A`).

| Key | Topic |
|---|---|
| RDD_KEY_185 | §2.2/§2.4/§4 (new §4.3) — `<pre>` content is opaque like CDATA; bare text-node siblings reindent to parent structural depth like any content line |
| RDD_KEY_188 | Class Scoping — no separate HtmlTokenizer/HtmlFormatter; shared `*Tags` classes gated internally on `isHtml5`, concrete rules in `XmlSpecificRule.java` |
| RDD_KEY_189 | Class Scoping — JSON/JSON5/CSS/YAML/TOML extend `TokenizerCore` directly (no `TokenizerFlat`); concrete rules in `JsonSpecificRule.java`/`YamlSpecificRule.java`/`TomlSpecificRule.java`/`CssSpecificRule.java` |
| RDD_KEY_190 | `FormatterCore.forLanguage` dispatch for JSON/JSON5/CSS — new "SimpleBraced" family (`TokenizerSimpleBraced`/`FormatterSimpleBraced`), distinct from `*Curly` and the still-hypothetical YAML/TOML-only "Flat" family; `Lang.isSupported`/`SUPPORTED_LANGUAGES` gained json/json5, `isScaffoldOnly`/`SCAFFOLD_ONLY_LANGUAGES` dropped them |
| RDD_KEY_191 | `STYLE_DATA_FORMATS.md` §5/§6 (YAML/TOML formatting rules, drafted by user request): YAML mapping colons column-align matching JSON/CSS (§1.1/§3.1); sequence items indent one level deeper than their parent mapping key; flow-style collections preserved as written unless they'd overflow `line-length`, in which case converted to block style (one-directional, never block→flow); `indent-size` reuses the global default (4), no YAML-specific override, but local fixtures should exercise `indent-size=2` via an in-file `#% JXM_CFMT_CFG` directive; `indent-style` is explicitly ignored/inapplicable for YAML since the spec forbids tab indentation. TOML drafted with standard high-confidence conventions (no user question needed): `=` alignment reuses STYLE.md §5/§6's assignment shape directly; table/array-of-table headers get no added indentation for their keys (nesting is via dotted header names, matching real-world tooling like `taplo`/`cargo fmt`); inline tables are always single-line per the TOML v1.0 grammar itself, not a style choice. |
| RDD_KEY_192 | YAML/TOML real-logic implementation: line-based recursive-descent parser for YAML, flat single-pass line-scanner for TOML, independent `#%` frozen-span/comment-normalization logic (no `TokenizerCore.markFrozenSpans`/`FormatterSimpleBraced.capitalizeCommentStart` reuse); migrated out of `Lang.SCAFFOLD_ONLY_LANGUAGES` into `Lang.SUPPORTED_LANGUAGES`; all 8 fixtures pass `make test`; two YAML bugs (same-indent sequence-child silent truncation, untrimmed key breaking idempotency) and one TOML bug (multi-line array continuation breaking idempotency) found and fixed; one fixture-authoring error found and corrected. |
| RDD_KEY_193 | XML real-logic implementation: character-cursor recursive-descent parser (no natural line boundary in tag grammar, no `TokenizerCore` reuse); independent `<!--%`-based frozen-span/comment-normalization logic; `InFileConfig` extended for `<!--% JXM_CFMT_CFG ... -->`; migrated `xml` out of `Lang.SCAFFOLD_ONLY_LANGUAGES` into `Lang.SUPPORTED_LANGUAGES` (HTML5 stays scaffold-only); unlike YAML/TOML, XML's rule constructor takes `indentStyle` (§2.1 has no ignored-setting exception); wrap-shape judgment call (closing `>` attached to last attribute line); one bug found+fixed (childless-tag overflow wrap never triggered); all 4 fixtures (`xml_combined`/`xml_comments`) pass `make test`, 202/202 total (originally shipped as a mismatched `xml_core` pair, later corrected). |

---

## Config

- **JSON/JSON5:** no new config keys — §1.1's colon alignment is
  unconditional (no toggle), matching STYLE.md §5/§6.
- **XML:** no new config beyond the existing global `indent-size`/
  `indent-style`.
- **CSS:** §3.1's property/value alignment is unconditional, same reasoning
  as JSON's — no config toggle.
- **HTML5:** no HTML5-specific config beyond the CSS/JSON keys above plus
  whatever the JS/TS formatter defines in its own state file.
- **YAML:** uses the existing global `indent-size` (no YAML-specific default
  override — global default of 4 stands). **`indent-style` is
  ignored/inapplicable for YAML** — YAML forbids tab indentation, output is
  always space-indented regardless of configured value (RDD_KEY_191). Local
  YAML fixtures should set `indent-size=2` via an in-file
  `#% JXM_CFMT_CFG indent-size=2` directive to exercise YAML's
  community-standard indent width, rather than changing the tool-wide
  default (RDD_KEY_191).
- **TOML:** no new config; doesn't use `indent-size`/`indent-style` at all
  (§6.2 gives table-header keys no added indentation, and TOML rarely
  produces any incidental indentation).

---

## Dogfood Output Validation (syntax checkers)

For the still-open real-code-testing pass (see Checklist), "did the
formatter corrupt the file" is checked by feeding formatted output through a
real, independent third-party parser per format — same principle as
`java_sc`/`kotlin_sc` (parse-only, no semantic/schema validation, prints
`line:col: message` on error, exit 1 if any file has errors, exit 0
otherwise). Six Node.js scripts, one per format, live in
`tools/syntax_checker/` (committed, licensed project tooling; `java_sc`/
`kotlin_sc` remain outside the repo, unrelated to this campaign, at
`~/Projects/JxMake/0_excluded_directory/personal/SyntaxChecker/`):

- `json_sc.js` — built-in `JSON.parse` (plain `.json` only)
- `json5_sc.js` — `json5` package
- `yaml_sc.js` — `js-yaml`, `loadAll()` so multi-document streams are fully
  checked
- `toml_sc.js` — `smol-toml`
- `css_sc.js` — `postcss` (**not** `css-tree`: css-tree's parser is
  deliberately tolerant and silently auto-closes an unclosed `{ ... }` block
  at EOF instead of reporting a parse error — confirmed by direct testing,
  a hand-crafted `body { color: red` with no closing brace produced zero
  `onParseError` calls. `postcss.parse()` throws a `CssSyntaxError` with
  line/column for the same input, so it's used instead.)
- `xml_sc.js` — `@xmldom/xmldom`. Its default `onError` handler only logs
  warnings/`error`-level problems and does not throw, so a custom `onError`
  is wired in to capture those; but `fatalError`-level problems (e.g.
  mismatched tags) throw a `ParseError` regardless of `onError`, so
  `parseFromString` is also wrapped in try/catch (deduped against
  `onError` already having recorded the same fatalError).

**`xml_content_diff.py`** — a content-preservation checker for XML,
complementing `xml_sc.js` (which only proves "still parses", same
`css_content_diff.py` precedent). Parses both original and formatted files
with stdlib `xml.dom.minidom` (no extra package dependency, unlike
`xml_sc.js`) and walks both DOMs in parallel, comparing (skipping
pure-whitespace text nodes so pure re-indentation is never flagged):
(1) element names and attribute name+value pairs, **in order** (XML
attribute order is spec-preserved per `STYLE_DATA_FORMATS.md` SS2.2, so
reordering is a real bug here, unlike most other formats); (2) text-node
content, whitespace-normalized; (3) comment text, whitespace-normalized —
this is the check that would have caught CSS's `twbs/bootstrap`
rtlcss-directive comment-corruption bug had it been an XML/HTML bug, since a
corrupted comment is often still syntactically valid; (4) CDATA content,
byte-identical (no whitespace normalization — CDATA is opaque/verbatim).
Node-type mismatches at the same tree position are reported as a structural
mismatch. Exit 0/1, description of every mismatch on mismatch, exit 2 if the
original itself doesn't parse (not applicable). Verified against a
hand-crafted good pair (whitespace-only reformat) and a deliberately-mutated
bad pair (attribute reorder + comment text change) before being trusted for
real dogfood use — both caught correctly. Written and first used during the
`apache/maven` XML dogfood session (see Checklist); reusable as-is for the
remaining XML test-fixture repos (`apache/ant`, `jenkinsci/jenkins`,
`w3c/svgwg`). Usage: `python3 xml_content_diff.py <original.xml>
<formatted.xml>` (stdlib only, no `npm install` needed, unlike the `*_sc.js`
scripts).

**`toml_content_diff.py`** — a content-preservation checker for TOML,
complementing `toml_sc.js` (which only proves "still parses", same
`css_content_diff.py`/`xml_content_diff.py` precedent). This system's Python
is 3.6 (no stdlib `tomllib`, 3.11+ only) with no `toml`/`tomli` package
installed, so instead of parsing directly in Python it shells out to a small
inline Node.js helper (embedded as a string in the script, run via `node -e
... -- <path>`) that uses the already-installed `smol-toml` package (same one
`toml_sc.js` uses) to parse each file to JSON, then compares the two
resulting Python data structures (`dict`/`list`/scalar) for deep equality —
same principle a direct `tomllib` comparison would give, just relayed through
JSON as the interchange format. Note: `node -e <script> -- <path>` puts
`<path>` at `process.argv[1]`, not `argv[2]` (no script-file slot is filled
when using `-e`) — a gotcha hit and fixed during this script's own
verification. Exit 0 if parsed structures match, 1 with a diff of both
parsed structures otherwise, 2 if either file fails to parse as TOML at all.
Verified against a hand-crafted good pair (whitespace/alignment-only reformat)
and a bad pair (a scalar value changed) before being trusted for real dogfood
use — both caught correctly. Written and first used during the
`rust-lang/cargo` TOML dogfood session (see Checklist); reusable as-is for
the remaining TOML test-fixture repos (`python-poetry/poetry`,
`pola-rs/polars`, `toml-lang/toml`). Usage: `python3 toml_content_diff.py
<original.toml> <formatted.toml>` — needs the same `LD_LIBRARY_PATH`/
`NODE_PATH`/`PATH` env as `toml_sc.js` (see below), unlike `xml_content_diff.py`
which is stdlib-only.

**`yaml_content_diff.py`** — a content-preservation checker for YAML,
complementing `yaml_sc.js` (which only proves "still parses", same
`css_content_diff.py`/`xml_content_diff.py`/`toml_content_diff.py`
precedent). PyYAML is installed on this system, so unlike
`toml_content_diff.py` it parses directly in Python: `yaml.safe_load_all`
on both original and formatted files (multi-document-stream aware, same
`loadAll()` reasoning as `yaml_sc.js`), then compares the resulting
per-document Python data structures (`dict`/`list`/scalar) for deep
equality. Exit 0 if all documents' parsed structures match (a lightweight,
best-effort `#`-comment-line scan is also run and any textual difference
reported as informational-only, since comment normalization is a separate,
non-structural concern), exit 1 with a description of the mismatch
otherwise, exit 2 if either file fails to parse as YAML at all (not
applicable to a real dogfood run where both files are already
syntax-checked separately). Verified against a hand-crafted good pair
(whitespace-only reformat, same comment) and a bad pair (a scalar value
silently changed) before being trusted for real dogfood use — both caught
correctly. Written and first used during the `kubernetes/kubernetes` YAML
dogfood session (see Checklist); this is also the check that caught a real
bug (a plain block-scalar sequence item silently truncated to an empty
string) that `yaml_sc.js` alone missed, since the truncated output was
still syntactically valid YAML. Usage: `python3 yaml_content_diff.py
<original.yaml> <formatted.yaml>` (needs `pip3 install --user pyyaml` if
not already present; no Node/env vars needed, unlike `toml_content_diff.py`).

`html_sc.js` (`parse5`, `onParseError`) also exists for HTML5, but per-spec
HTML5 parsing is deliberately error-tolerant (e.g. auto-closes mismatched
tags rather than failing), so it only catches the narrow set of conditions
the spec defines as parse errors, not general malformed-markup the way the
XML checker does — documented as a caveat directly in the script.

**`html_content_diff.py`** — a content-preservation checker for HTML5,
complementing `html_sc.js` (which only proves "still parses per HTML5's
error-tolerant grammar", an even weaker signal than the other formats'
syntax checkers per that caveat, making this check even more essential
here). Python's stdlib can't parse real-world HTML5 (not XML-well-formed —
void elements, optional closing tags), so like `toml_content_diff.py` it
shells out to a small inline Node.js helper using the already-installed
`parse5` package (same one `html_sc.js` uses) to parse each file into a
simplified JSON tree, then walks both trees in parallel comparing element
tag names/structure, attribute name+value pairs in order, whitespace-
normalized text content, comment content, and the DOCTYPE declaration.
`<script>`/`<style>` element bodies are deliberately NOT compared
byte-for-byte (the formatter legitimately dispatches their content to the
JS/TS and CSS pipelines, which may reformat it) — only that the element
itself survives at the same tree position with the same attributes and a
non-empty body (proving the content wasn't silently dropped), the same
"structural HTML must be exactly preserved, embedded content just needs its
own pipeline's guarantees" split the task's methodology calls for. Verified
against a hand-crafted good pair (whitespace-only reformat) and two bad
pairs (a dropped attribute, a corrupted comment) before being trusted for
real dogfood use — all three cases caught correctly. Written and first used
during the `h5bp/html5-boilerplate` HTML5 dogfood session (see Checklist).
Usage: `python3 html_content_diff.py <original.html> <formatted.html>` —
needs the same `LD_LIBRARY_PATH`/`NODE_PATH`/`PATH` env as `html_sc.js`.

**`css_content_diff.py`** — a content-preservation checker, complementing
`css_sc.js` (which only proves "still parses", not "still means the same
thing" — the twbs/bootstrap rtlcss directive-comment corruption bug,
fixture `real_code_regressions_69`, produced still-valid CSS and would not
have been caught by `css_sc.js` alone). Takes `<original.css>
<formatted.css>`, checks: (1) every `/* ... */` comment's whitespace-
normalized text is byte-identical between the two files, in order; (2) the
comment-stripped, colon/whitespace-normalized token stream is identical
(proves no property/value/selector was added/removed/reordered); (3)
`!important` occurrence count matches; (4) vendor-prefixed
(`-webkit-`/`-moz-`/`-ms-`/`-o-`) property occurrence counts match exactly
per distinct prefixed-property string. Exit 0 if all four checks pass, exit
1 with a description of each mismatch otherwise. Written and verified
(positive case + a deliberately-mutated-comment negative case) during the
`necolas/normalize.css` dogfood session below; first used as an ad hoc
inline Python snippet during the `twbs/bootstrap` session, now promoted to a
permanent, reusable script alongside the other `*_sc.js` checkers. No new
package dependency (stdlib `re` only).

All six `*_sc.js` syntax checkers were verified against hand-crafted good/bad pairs (malformed
trailing comma, unclosed brace, mismatched tag, etc.) before being trusted
for real dogfood use; each caught its bad case and passed its good case.

Requires the same `LD_LIBRARY_PATH`/`NODE_PATH`/`PATH` env as
`STATE_JS_TS.md`'s "Tools/compiler used" section (same `node` binary, same
`~/mynpm`-installed package location) — see that file for the exact export
lines and why `LD_LIBRARY_PATH` is required on this system. Install each
script's package once via `npm install --prefix ~/mynpm <pkg>` (json5,
js-yaml, smol-toml, postcss, @xmldom/xmldom, parse5) before first use.

---

## Test-Fixture Repos

Recorded for regression testing once implemented (not a commitment — see
Checklist for current per-language status):

- **JSON/JSON5:** `json5/json5`, `microsoft/vscode`, `babel/babel`, `eslint/eslint`
- **XML:** `apache/maven`, `apache/ant`, `jenkinsci/jenkins`, `w3c/svgwg`
- **CSS:** `twbs/bootstrap`, `necolas/normalize.css`, `foundation/foundation-sites`,
  `primer/css`
- **HTML5:** `h5bp/html5-boilerplate`, `twbs/bootstrap` (docs site), `mdn/content`,
  `whatwg/html`
- **YAML:** `kubernetes/kubernetes` (manifests/Helm-adjacent config, heavy real-world
  nesting/anchors), `docker/compose` (compose-file corpus), `ansible/ansible`
  (playbooks — heavy on lists-of-maps, block scalars), `actions/starter-workflows`
  (small, dense GitHub Actions YAML, good for quick spot checks), `prometheus/prometheus`
  (done — see Checklist; alerting/recording rule YAML with PromQL expressions
  embedded in scalars, `$labels`/`$value` templating, deeply nested rule groups,
  plus large hand-authored `pnpm-lock.yaml` corpora under `web/ui/`),
  `home-assistant/core` (done — see Checklist; very large volume of
  hand-authored automation/config YAML, deeply nested, heavy use of block
  scalars, a different large-corpus stress test than kubernetes' API-manifest
  shape)
- **TOML:** `rust-lang/cargo` (`Cargo.toml` corpus across its own repo and any
  vendored crates), `python-poetry/poetry` (`pyproject.toml`-heavy), `pola-rs/polars`
  (large Rust workspace, many `Cargo.toml` files), `toml-lang/toml` (the spec repo
  itself — includes a compliance-test-style example corpus)

---

## Test Fixtures (Local)

Local dogfood pairs (distinct from the external-repo list above, which is
for corpus-scale validation) covering JSON, JSON5, XML, CSS, HTML5 authored
and registered in `formatter/test/` — see `test/README.txt` for pair list
and coverage.

**YAML/TOML also authored** (`yaml_core_inp/out.yaml`,
`yaml_comments_inp/out.yaml`, `toml_core_inp/out.toml`,
`toml_comments_inp/out.toml`, in `formatter/test/`, described in
`test/README.txt`), hand-drafted directly against `STYLE_DATA_FORMATS.md`
§5/§6 with no pre-existing draft. Now verified against real logic and
uncommented in the Makefile's `INP_FILES` (see Checklist).

---

## Class Scoping (post Core/Curly/Indent/Tags refactor)

- **XML/HTML5** are tag-based (`Lang.isTagBased()`). **Resolved
  (RDD_KEY_188):** no separate `HtmlTokenizer`/`HtmlFormatter` — XML and
  HTML5 share one parser/renderer, gated internally on `lang.isHtml5`
  (mirrors curly classes branching on `isKotlin`). Concrete XML/HTML5-only
  rule logic (§2/§4) lands in one `XmlSpecificRule.java`, fully real
  (RDD_KEY_193/194), gating HTML5-only additions (void elements,
  `<script>`/`<style>` dispatcher) on `lang.isHtml5`.
- **JSON/JSON5/CSS** are neither tag-based, curly, nor indent-based per the
  `Lang.java` family predicates (flat/braced, no block-scoping semantics).
  **Resolved (RDD_KEY_189):** no new `TokenizerCore` sibling (no
  `TokenizerFlat`) — each extends `TokenizerCore` directly with a minimal
  override when implemented. JSON/JSON5 share one `JsonSpecificRule.java`
  (gated on `lang.isJson5` for JSON5-only additions), CSS gets
  `CssSpecificRule.java`. YAML/TOML (same "neither curly/indent/tag-based"
  characteristic) get `YamlSpecificRule.java`/`TomlSpecificRule.java` on the
  same reasoning. **Resolved further (RDD_KEY_190), once JSON/JSON5 real
  logic landed:** JSON/JSON5 and CSS form a new "SimpleBraced"
  `Lang.isSimpleBraced` family — `TokenizerSimpleBraced` (shared `/* */`
  block-comment scan) and `FormatterSimpleBraced` (shared
  `padKeysForColonAlignment` group-padding, §1.1/§3.1's identical
  colon-alignment shape). `JsonTokenizer` extends `TokenizerSimpleBraced`;
  `FormatterJson` extends `FormatterSimpleBraced`, is
  `FormatterCore.forLanguage`'s new `isJson || isJson5` branch. Distinct
  from the still-hypothetical YAML/TOML-only "Flat" family (no braces at
  all) — CSS later joined `SimpleBraced` for real (`CssSpecificRule.java`
  fully implemented); YAML/TOML did NOT join `SimpleBraced` (RDD_KEY_192) —
  each implements its own from-scratch parser (line-based for YAML, flat
  single-pass line-scanner for TOML), confirming the "structurally
  different, don't conflate" note was correct.
- Implementation order (all complete): JSON/JSON5 → CSS → YAML/TOML → XML →
  HTML5 (HTML5 last, depended on CSS and, for one exception below, JS/TS).
- **HTML-before-JS/TS contingency — resolved differently than planned:**
  HTML5 landed (RDD_KEY_194) before `STATE_JS_TS.md` has a real JS/TS
  formatter. Rather than the originally-planned silent opaque passthrough
  for embedded `<script>` content, `renderScriptOrStyle` throws
  `XmlParseException` for real (non-frozen) JS-type `<script>` content,
  directing the caller to wrap it in `//% JXM_CFMT_DIS`/`//% JXM_CFMT_ENA`
  instead — so real JS content is never silently mis-formatted-as-untouched.
  See HTML5 checklist entry and `STATE_JS_TS.md`'s cross-job follow-up note
  for what happens once JS/TS lands for real.

## Open Questions

None recorded yet in this file.

---

## Checklist

- [x] **Implement JSON/JSON5 (§1).** DONE. `JsonTokenizer` (extends
      `TokenizerSimpleBraced`, RDD_KEY_190) handles strings (incl. JSON5
      single-quote/backslash-newline continuations), numbers, unquoted
      identifiers, `//`/`/* */` comments. `JsonSpecificRule`: recursive-
      descent parser+printer for §1.1 colon-alignment groups (via
      `FormatterSimpleBraced.padKeysForColonAlignment`) and §1.2 tight/loose
      array padding. `FormatterJson` wires `line-length`/`indent-size`/
      `indent-style`; no frozen-span mechanism (whole-file `--format-off`
      only). Malformed input throws `JsonSpecificRule.JsonParseException`,
      handled generically by `Main`. `Lang.isSupported`/`SUPPORTED_LANGUAGES`
      gained `json`/`json5`. Fixtures: `test/json_core_{inp,out}.json`,
      `test/json5_core_{inp,out}.json5` (`test/README.txt`).
      `FUTURE_TEST_FIXTURES.md` (one dir above `formatter/`) holds
      hand-drafted unverified fixture content for not-yet-authored pairs
      across every job — check before hand-writing new fixtures. Bugs
      found+fixed: objects only render tight with exactly ONE member (2+
      always loose, unlike arrays); trailing commas silently dropped in
      tight-array/tight-object rendering. Also added: `normalize-comment-
      start-case` (`FormatterSimpleBraced.capitalizeCommentStart`),
      block-comment reindentation
      (`FormatterSimpleBraced.reindentBlockComment`, shared with CSS),
      JSON5 `key /* comment */ : value` mid-comment handling. Fixture:
      `test/json5_comments_{inp,out}.json5`. `make test`: 95/95 forward +
      idempotency, zero regressions.
- [x] **Implement XML support (§2).** DONE. `XmlSpecificRule.java`: a
      from-scratch character-cursor recursive-descent parser (not line-based
      like YAML, no `TokenizerCore` reuse — XML's tag/attribute grammar has
      no natural line boundary). `Node` AST (`PI`/`DOCTYPE`/`COMMENT`/
      `ELEMENT`/`TEXT`/`CDATA`/`FROZEN`) covers: PIs/`<!DOCTYPE>`/CDATA
      preserved opaque/verbatim; attribute order preserved as parsed;
      text/CDATA-only elements render inline; empty pairs unexpanded
      (`<tag></tag>`); overflowing tags wrap one attribute per line with `>`
      attached to the last attribute line (judgment call — §2.2 doesn't
      specify exact shape, follows IntelliJ/Prettier-XML convention), incl.
      childless overflow. `<!--% JXM_CFMT_DIS/ENA -->` frozen spans and
      comment-case normalization implemented independently (not
      `TokenizerCore.markFrozenSpans` reuse), same per-format-family
      approach as YAML/TOML (RDD_KEY_192). `InFileConfig` extended for
      `<!--% JXM_CFMT_CFG ... -->`. `FormatterXml.java` — unlike YAML/TOML,
      XML's rule constructor takes `indentStyle` (§2.1 has no
      ignored-setting exception). `Lang.SUPPORTED_LANGUAGES`/`isSupported`
      gained `xml` (HTML5 stays scaffold-only). Fixtures:
      `test/xml_combined_*`, `test/xml_comments_*` (originally shipped as a
      mismatched `xml_core` pair, later corrected — `test/README.txt`).
      `make test`: 202/202 forward + idempotency, zero regressions. Bug
      found+fixed: childless overflowing tags never checked line length
      (fits/overflow check now runs before the empty-vs-non-empty children
      branch). **Known simplifications, not exercised by current
      fixtures:** no text reflow (only attributes wrap); mixed text+element
      content splits onto separate lines rather than staying inline;
      `indent-style = auto` not detected from existing indentation; §2.4's
      CDATA-inside-`<script>`/`<style>` dispatch exception not implemented
      (needs JS/TS or CSS dispatch from inside the XML pipeline).
- [x] **Implement CSS support (§3).** DONE. `CssTokenizer` (extends
      `TokenizerSimpleBraced`) is deliberately coarse-grained — emits
      WHITESPACE/NEWLINE/COMMENT_BLOCK/STRING/PUNCT and one OP run for
      everything else; `CssSpecificRule`'s parser reconstructs header/value
      text from token concatenation + paren-depth tracking rather than
      modeling CSS grammar token-by-token. One recursive `parseBlockBody`/
      `Rule`/`Decl` AST covers plain rules, at-rules (`@media`/`@supports`/
      `@keyframes`/`@font-face`), and native-nesting `&` blocks uniformly
      (any `{`-terminated header recurses, giving at-rules/`&` blocks their
      own alignment group for free). Colon-alignment reuses
      `FormatterSimpleBraced.padKeysForColonAlignment`. `FormatterCss` wires
      `line-length`/`indent-size`/`indent-style`; no frozen-span mechanism
      initially (same posture as JSON/JSON5). `Lang.isSupported`/
      `SUPPORTED_LANGUAGES` gained `css`. Fixture:
      `test/css_combined_{inp,out}.css` (group-break/re-merge, at-rule
      nesting, `&` nesting — `test/README.txt`). Comment-handling follow-up
      (same session): `normalize-comment-start-case`
      (`FormatterSimpleBraced.capitalizeCommentStart`), block-comment
      reindentation, `prop /* comment */ : value` mid-comment handling
      (`Decl.midComment`, excluded from alignment groups); `/*% JXM_CFMT_DIS
      */`/`ENA` per-region frozen spans implemented by reusing
      `TokenizerCore.markFrozenSpans` on the CSS token list before parsing.
      Fixture: `test/css_comments_{inp,out}.css`. `make test`: 95/95 forward
      + idempotency, zero regressions. **Deferred:** the curly family's
      classifier-backed keyword-exclusion comment normalization
      (`MiscRuleCore`) deliberately NOT reused — CSS/JSON have no keywords
      needing titlecasing protection, so the lightweight version suffices.
- [x] **Implement HTML5 support (§4)** (RDD_KEY_194). `XmlSpecificRule.java`
      extended (not a new class — HTML5 shares XML's parser internally per
      RDD_KEY_188, gated on `lang.isHtml5`): §4.1 void elements parsed as
      self-closing leaves rendered with a bare `>`; bare boolean attributes
      accepted; §4.3 `<pre>` content captured as a new `RAW` node type,
      preserved byte-for-byte; `<script>`/`<style>` content captured as raw
      text via new `finishRawTextElement` helper. §4.2 dispatch: `<style>`
      splices to `CssSpecificRule.format` and back (fully real, CSS already
      implemented). `<script>`: non-JS-MIME `type="..."` stays opaque; a
      recognized JS-type block has no JS/TS formatter to dispatch to yet
      (`STATE_JS_TS.md` still scaffold-only), so `renderScriptOrStyle`
      throws `XmlParseException` directing the caller to freeze the block
      instead of silently passing it through (refinement of the earlier
      "HTML-before-JS/TS contingency" note, which had proposed silent
      passthrough). Escape hatch: script-content-specific frozen-span
      detector `isFrozenScriptContent`, recognizing a `//%
      JXM_CFMT_DIS`/`//% JXM_CFMT_ENA` line pair anywhere in the raw content
      (not just the first line, to accommodate the CDATA-wrapped idiom's
      literal `<![CDATA[` first line) — see `STATE_JS_TS.md`'s checklist
      cross-job follow-up for what must happen when JS/TS lands (remove the
      two local fixtures' directive-wrapping, wire a real dispatch call,
      re-verify). Bug found+fixed (general, not HTML5-specific):
      self-closing/void elements never checked line length or wrapped
      attributes on overflow (RDD_KEY_193 gap) — fixed with the same
      fits-check + wrap branch used for non-self-closing tags.
      `Lang.SUPPORTED_LANGUAGES`/`isSupported` gained `html5`;
      `FormatterCore.forLanguage` routes `isHtml5` to `FormatterXml`
      alongside `isXml`. Two fixture-authoring mismatches corrected to match
      verified-real behavior (mixed content line-splitting per RDD_KEY_193;
      trailing comment spacing per `xml_comments_out.xml` precedent).
      `make test`: 212/212 PASS (up from 202, +2 HTML5 fixture pairs, zero
      regressions).
- [x] Author local test fixture pairs per `FUTURE_TEST_FIXTURES.md`'s
      "HTML5" section and register in Makefile's `INP_FILES`/
      `test/README.txt`. Done: `html_combined_inp/out.html` and
      `html_comments_inp/out.html` extracted to `test/`, registered live in
      the Makefile (real logic implemented, see HTML5 entry above),
      documented in `test/README.txt`. Both fixtures' `<script>` blocks
      temporarily wrapped in `//% JXM_CFMT_DIS`/`//% JXM_CFMT_ENA` since
      real JS/TS formatting doesn't exist yet — see `STATE_JS_TS.md`'s
      checklist for the required follow-up (remove wrapping, re-verify)
      once that job lands.
- [x] **Implement YAML support (§5).** `YamlSpecificRule.java`: from-scratch
      line-based recursive-descent parser (not `TokenizerCore` reuse —
      indentation-significant grammar, RDD_KEY_189/191): `parseBlock`
      recurses per indentation level; `parseKeyItem`/`parseSeqItem` handle
      block scalars, flow values, anchors, sequence-of-mappings; a custom
      `FlowNode`/`FlowScalar`/`FlowMap`/`FlowSeq`/`FlowCursor` AST+parser
      handles `{...}`/`[...]` flow collections. Colon-alignment reuses
      `FormatterSimpleBraced.padKeysForColonAlignment`; §5.4
      flow-preserved-unless-overflow is recursive per nesting level; §5.3
      sequence-of-mapping alignment uses a fixed 2-column dash offset.
      `#%`-based frozen spans and comment normalization implemented from
      scratch (not `TokenizerCore.markFrozenSpans` reuse). `FormatterYaml
      .java` omits `indentStyle` from its rule constructor (§5.1 mandates
      spaces always).
- [x] **Implement TOML support (§6).** `TomlSpecificRule.java`: a simpler
      flat, non-recursive, non-indented single-pass line scanner (nesting
      via dotted table-header names, e.g. `[a.b]`, per §6.2 — no recursive
      block parser needed). A `ValueNode`/`Scalar`/`Entry`/`Arr`/`Tbl`/
      `ValueCursor` AST+parser handles array/inline-table values. §6.3
      tight/loose is purely structural (tight iff every element is a
      `Scalar`, no line-length check unlike YAML). §6.4 inline tables are
      always single-line (grammar constraint). `=`-alignment and `#%`
      frozen-span/comment logic are structurally identical to YAML's
      (duplicated, not factored into a shared helper — flagged as a possible
      future DRY improvement). `FormatterToml.java` mirrors
      `FormatterYaml.java`. Both moved from `Lang.SCAFFOLD_ONLY_LANGUAGES`
      into `SUPPORTED_LANGUAGES` (`FormatterCore.forLanguage` gained
      `isYaml`/`isToml` branches), same precedent as JSON/CSS's migration
      (RDD_KEY_190).
- [x] **YAML/TOML fixtures authored ahead of implementation, then verified
      against real logic and uncommented in the Makefile.**
      `test/yaml_core_{inp,out}.yaml`, `test/yaml_comments_{inp,out}.yaml`,
      `test/toml_core_{inp,out}.toml`, `test/toml_comments_{inp,out}.toml`
      (`test/README.txt`) registered in the Makefile's `INP_FILES`, pass
      `make test` cleanly. Bugs found+fixed against these fixtures:
      (1) **YAML silent-data-loss**: `parseKeyItem`'s child-block trigger
      required strictly-deeper indent, but YAML allows a sequence's `-`
      items at the *same* indent as their parent mapping key — the miss
      silently dropped the rest of the document. Fixed by allowing
      `next.indent >= ln.indent` specifically for sequence-item children.
      (2) **YAML idempotency**: key extraction didn't `.trim()`, so
      re-parsing the formatter's own aligned output (`name : widget`)
      captured `"name "` as the key, widening alignment on the second pass.
      Fixed with `.trim()`. (3) **TOML idempotency**: the flat line scanner
      assumed each `key = value` was one physical line, but §6.3's
      loose-array output is intentionally multi-line, causing `unterminated
      array` on re-parse. Fixed with a quote-aware `bracketBalance` helper
      that consumes additional physical lines when brackets are unbalanced.
      Also corrected one fixture-authoring error: `yaml_core_out.yaml`'s
      nested `endpoints` flow array was hand-drafted expecting block
      conversion, but per §5.4's recursive-per-depth rule it fits under
      `line-length` and should stay flow — fixture corrected to match the
      (correct) implementation.
- [ ] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      `STYLE_DATA_FORMATS.md`'s listed test-fixture repos per sub-format.
      Status: JSON/JSON5 complete (4/4 repos); CSS complete (4/4 repos); XML
      3/4 (`apache/maven`, `w3c/svgwg`, `apache/ant` done; `jenkinsci/jenkins`
      not started); **YAML: all 4 originally-planned repos DONE, plus both
      later-added repos, list now fully complete** (`kubernetes/kubernetes`,
      `docker/compose`, `ansible/ansible`, `actions/starter-workflows`,
      `prometheus/prometheus`, `home-assistant/core` all done — see per-repo
      entries below); **TOML 4/4, DONE** (`rust-lang/cargo`,
      `python-poetry/poetry`, `pola-rs/polars`, `toml-lang/toml` all done —
      TOML Test-Fixture Repos list now fully complete); HTML5 1/4
      (`h5bp/html5-boilerplate` done; `twbs/bootstrap` docs site,
      `mdn/content`, `whatwg/html` not started). Overall item stays
      unchecked pending the remaining repos above.

      **Shared methodology note (applies to every run below, not restated per
      repo):** each run clones fresh (or reuses a prior-session checkout under
      `/tmp`/scratchpad if found) per `STATE_COMMON.md`'s methodology, excludes
      build/vendor/generated-output dirs, and — above a per-format sampling
      threshold (JSON/CSS: "several thousand+"; XML/YAML: "several hundred+")
      — takes a representative sample rather than the full set; below
      threshold the full set is processed. Every run does: baseline
      syntax-check of unformatted originals (to separate pre-existing-invalid/
      template/deliberately-malformed fixtures from real in-scope files) →
      round1 format (`--preserve-tree --root`, one invocation) → round2
      (reformat round1 output) → `diff -rq` idempotency check → syntax-check of
      round1 output vs baseline → format-specific content-preservation check
      (`css_content_diff.py`/`xml_content_diff.py`/`yaml_content_diff.py`/
      `toml_content_diff.py`, per "Dogfood Output Validation" above). A
      comment-capitalization diff (`normalize-comment-start-case=on`,
      lowercase-starting prose comment capitalized) recurs across several runs
      below and is expected/correct behavior per the `real_code_regressions_69`
      precedent, not a bug — noted per-repo only when it's the sole diff found.

      **CSS (all 4 repos done, sub-portion complete):**
      - `twbs/bootstrap`: real source is `.scss`; in-scope corpus 31
        hand-authored `.css` files (`site/src/assets/examples/**`, `dist/css/**`
        excluded as generated). Forward/idempotency/syntax-check all 31/31
        clean. **1 bug found+fixed** (via manual comment inspection, not the
        token-stream content-diff, which strips comments): `carousel.css`'s
        rtlcss directive comments (`/* rtl:begin:ignore */` etc.) were
        silently capitalized to `/* Rtl:... */` by
        `FormatterSimpleBraced.capitalizeCommentStart` — still valid CSS, so
        `css_sc.js` missed it, but semantically broken for any pipeline
        running rtlcss over the output. Fixed by adding
        `isSingleTokenDirective`: skip capitalization when a comment's
        first-line body is one whitespace-free token containing `:` or `-`
        (directive-shaped), while ordinary prose is still capitalized.
        Fixture `test/real_code_regressions_69_{inp,out}.css`. `make test`:
        118/118 forward + idempotency. Commit `8f5f597`. Final full re-run:
        31/31 clean on all four checks.
      - `necolas/normalize.css`: 1 hand-authored file (`normalize.css`, 6138
        bytes), no generated copy in-repo. Wrote `css_content_diff.py` this
        session (see "Dogfood Output Validation"). Zero bugs — 1/1 clean on
        all four checks (71 comments byte-identical, token stream identical,
        `!important` count and vendor-prefix counts matched).
      - `foundation/foundation-sites`: real source is 100% `.scss`; the only
        `.css` files are 8 generated `dist/css/**` outputs (Sass-compiled, with
        matching `.css.map`). **In-scope corpus: 0 files** — no run performed,
        nothing to regress-test.
      - `primer/css`: real source is `.scss`; in-scope corpus 2 hand-authored
        files (`docs/.storybook/{preview,storybook}.css`). Forward/idempotency/
        syntax-check 2/2 clean. Content-preservation: `storybook.css` exact
        match; `preview.css` one diff, `/* color */` → `/* Color */` —
        confirmed expected capitalization (not directive-shaped per
        `isSingleTokenDirective`), not a bug. Zero bugs found.

      **JSON/JSON5 (all 4 repos done, sub-portion complete):**
      - `json5/json5`: 6 files. Baseline 5/6 pass (`test/invalid.json5` is the
        repo's own deliberately-invalid fixture, expected fail). Forward 6/6,
        idempotent, syntax-check of output matches baseline exactly incl. the
        one expected failure (formatter left its content byte-identical).
        Zero bugs found.
      - `microsoft/vscode`: 1377 files found, below sampling threshold so full
        set processed. 5 genuinely-empty files correctly throw
        `JsonParseException`. 100 JSONC files (comments or trailing commas)
        excluded as out-of-scope by design (already fail baseline
        `JSON.parse`). **In-scope corpus: 1272 files.** Round1: exit 123 (the
        5 empty-file errors as expected), 1372 written. **1 idempotency bug
        found+fixed** (via `diff -rq round1 round2`, not syntax-check):
        `JsonSpecificRule.parseContainer` kept a dangling placeholder `Item`
        for any comment-less blank line before a closing brace, defeating the
        tight-`{}` short-circuit on round1 but not round2 (whose re-parse saw
        no comment) — non-idempotent (`extensions/vscode-api-tests/
        testWorkspace/bower.json`, source `"{\n\n\t\n}\n"`). Fixed by only
        keeping the placeholder when a real leading comment exists. Fixture
        `test/real_code_regressions_68_{inp,out}.json` (no copyright header —
        plain `.json` has no comment syntax). `make test`: 117/117. Commit
        `e2a6f0e`. After fix: full 1377-file re-run clean idempotency;
        syntax-check 1272/1272 matching baseline.
      - `babel/babel`: 9245 files found, above sampling threshold — sampled
        964 files (all 204 package.json across packages, all 500 non-fixture
        `.json`, every 20th of 8745 `test/fixtures/**` files = 438,
        deduplicated to 964). Baseline 810/964 pass (154 JSONC `tsconfig*`
        excluded by design, 2 deliberately-invalid error-handling fixtures
        excluded). **In-scope corpus: 810 files.** Forward/idempotency/
        syntax-check all 810/810 clean, matching baseline. Zero bugs found.
        (Methodology note: one sampled path contains literal spaces —
        file-list handling must preserve it as one path, e.g. `xargs -d '\n'`,
        not unquoted `$(cat ...)` word-splitting.)
      - `eslint/eslint`: 98 files, full set processed. Baseline 92/98 pass; 6
        excluded (2 real JSONC, 2 deliberately-malformed error-handling
        fixtures, 1 genuinely-empty file, plus 2 BOM-prefixed fixtures
        discovered only during round1 and retroactively confirmed
        pre-existing-invalid at baseline too — both this formatter and
        `JSON.parse` reject a leading BOM identically). **In-scope corpus: 91
        files.** Forward/idempotency/syntax-check 91/91 clean, matching
        baseline. Manual spot-check confirmed only re-indentation/
        colon-alignment, no content loss. Zero bugs found.

      **XML (3/4 repos done):**
      - `apache/maven`: 3158 files found, above sampling threshold — sampled
        398 files (all 90 top-level/module `pom.xml`, every 10th of 1882
        `test/resources` `pom.xml` = 189, every 10th of 1186 non-pom `.xml`
        = 119). Baseline 397/398 pass (1 genuinely 0-byte fixture, expected).
        Forward 398/398 (empty file stays empty), idempotency 398/398 clean,
        syntax-check 397/398 matching baseline. Wrote `xml_content_diff.py`
        this session (see "Dogfood Output Validation"), verified against a
        hand-crafted good/bad pair first, then run on all 397 non-empty
        files: 355/397 exact match, 42/397 flagged — all 86 individual
        mismatches across those files are case-insensitive-equal
        comment-capitalization diffs (the same expected
        `normalize-comment-start-case=on` behavior), confirmed
        programmatically, not a bug. Zero attribute-order/text/CDATA/
        structural mismatches. Zero bugs found.
      - `w3c/svgwg`: 298 files found (22 `.xml` + 276 `.svg`, both extensions
        are real XML — spot-checked before trusting the count), below the
        several-hundred sampling threshold so the full set was processed
        (after excluding 1 vendored `tools/publish/node_modules/**` fixture).
        Baseline `xml_sc.js`: 294/298 pass — 4 excluded (1 deliberately-
        illustrative two-root-element snippet, `05_07.xml`; 3 BOM-prefixed
        `.svg` files, same pre-existing-invalid precedent as `eslint/eslint`'s
        BOM fixtures). **In-scope corpus: 294 files.** **1 bug found+fixed**,
        via the forward pass itself erroring before syntax-check/content-
        preservation could even run: `Lang.infer` never mapped the `.svg`
        extension to `xml` at all, so all 276 `.svg` files failed with
        "could not infer language from file extension" — not a parser/printer
        bug, a missing extension-to-language mapping. Fixed by adding `.svg`
        alongside `.xml` in `Lang.infer`. Fixture
        `test/real_code_regressions_74_{inp,out}.svg`. `make test`: 123/123.
        Commit `3408acd`. Final full 298-file re-run: forward 298/298 (zero
        errors, the 4 baseline-invalid files still produce output — the
        formatter is more lenient than `xmldom`, tolerating the BOM and the
        stray second root element rather than crashing); idempotency 298/298
        clean; syntax-check of round1 output matches baseline exactly (same 4
        pre-existing failures, no new ones); content-preservation
        (`xml_content_diff.py`, reused as-is) 22/294 flagged, all 59
        individual comment mismatches across those 22 files confirmed
        programmatically case-insensitive-equal (the same expected
        `normalize-comment-start-case=on` behavior as `apache/maven`'s run) —
        zero attribute-order/text/CDATA/structural mismatches, zero further
        bugs. Namespaces (`xmlns:xlink` etc.), deeply nested `<g>` groups,
        whitespace-sensitive path-data attribute values, and CDATA (18 files)
        all exercised with no corruption. `apache/ant`, `jenkinsci/jenkins`
        remain not-started.
      - `apache/ant`: 558 files found (517 `.xml` + 2 `.xsd` + 39 `.xsl`),
        above the several-hundred sampling threshold — sampled every 3rd
        `.xml` file (sorted path order, 173 files) plus all 2 `.xsd` + 39
        `.xsl` (214 files total, no build/vendor/generated dirs to exclude —
        this is Ant's own source tree, not a built output). Baseline
        `xml_sc.js`: 207/214 pass — 7 excluded, all under
        `src/etc/testcases/**` (Ant's own deliberately-invalid/malformed XML
        task-test fixtures: entity-relative-include, encoding, and
        include-parse-error cases — same pre-existing-invalid precedent as
        prior repos). **In-scope corpus: 214 files** (the 7 excluded still
        get forward/idempotency-checked, just not syntax-checked against a
        parse-error baseline they were never expected to pass).
        **1 bug found+fixed:** `Lang.infer` never mapped the `.xsd`/`.xsl`
        extensions to `xml` at all (same gap shape as the `w3c/svgwg`
        session's `.svg` bug) — every `.xsl`/`.xsd` file in the sample (41
        files) failed with "could not infer language from file extension"
        instead of being formatted, surfaced immediately by the forward pass
        itself. Fixed by adding `.xsd`/`.xsl` alongside `.xml`/`.svg` in
        `Lang.infer`. Fixtures `test/real_code_regressions_91_{inp,out}.xsl`,
        `test/real_code_regressions_92_{inp,out}.xsd`. `make test`: 141/141
        forward + 141/141 idempotency. Commit `25bd5b8`. Full 214-file re-run
        after the fix: forward 214/214 (zero errors); idempotency 214/214
        clean (`diff -rq` round1 vs round2 empty); syntax-check of round1
        output matches baseline exactly (same 7 pre-existing
        `testcases/**` failures, no new ones). Content-preservation
        (`xml_content_diff.py`, reused as-is) flagged 66/214 files, 484
        individual mismatches across those files, all confirmed
        programmatically case-insensitive-equal (the same expected
        `normalize-comment-start-case=on` behavior as `apache/maven`'s and
        `w3c/svgwg`'s runs) — zero attribute-order/text/CDATA/structural
        mismatches, zero further bugs. Ant's `build.xml`-family files (heavy
        `<macrodef>`/`<target>`/`<condition>`/nested-task structure),
        XSLT report stylesheets (`junit-frames.xsl` etc., deeply nested
        `<xsl:template>`/`<xsl:for-each>`/HTML-in-XSLT), and the 2 `.xsd`
        schema files all exercised with no corruption beyond the expected
        comment-capitalization normalization. `jenkinsci/jenkins` remains
        not-started.

      **TOML (2/4 repos done):**
      - `rust-lang/cargo`: 672 files, full set processed (below sampling
        threshold). Baseline 670/672 pass (2 deliberately-invalid manifest
        error-handling fixtures excluded). **In-scope corpus: 670 files.**
        **2 bugs found+fixed, both via the forward pass crashing** (before
        syntax-check/content-preservation ever ran) on genuinely-valid files:
        (1) `Cargo.toml`'s interior per-element array comment
        (`"target/", # exclude bench testing`) — the continuation-line
        joining loop only stripped a trailing `#` comment from the fully
        assembled line at the very end, so an interior line's own comment
        swallowed the array's closing `]` as comment text, throwing
        "unterminated array". Fixed by stripping each continuation line's own
        trailing comment before joining. (2) `triagebot.toml`'s
        `"""..."""` multi-line string — TOML v1.0 multi-line basic/literal
        strings had no handling at all in the flat line-scanner, throwing
        "expected 'key = value' line". Fixed by detecting an unterminated
        `"""`/`'''` opener and consuming subsequent raw lines verbatim until
        the closing delimiter (same opaque-preserve treatment as JSON5's
        multi-line strings). Fixture combines both bugs:
        `test/real_code_regressions_70_{inp,out}.toml`. `make test`:
        119/119. Commit `d56eb3a`. After fix, full 670-file re-run: forward/
        idempotency/syntax-check/content-preservation (`toml_content_diff.py`,
        written this session, comment-blind by design) all 670/670 clean.
      - `python-poetry/poetry`: 106 files, full set processed. Baseline
        106/106 pass — this repo's poetry-schema-invalid fixtures (missing
        keys, malformed scripts) are all syntactically well-formed TOML, so
        none excluded. Forward/idempotency/syntax-check/content-preservation
        (`toml_content_diff.py`, reused as-is) all 106/106 clean, zero
        crashes (nested inline-table dependency specs like
        `{ version = "^1.0", extras = ["foo"] }` hit no unhandled shape).
        Zero bugs found.
      - `pola-rs/polars`: 57 files, full set processed (below sampling
        threshold — every `Cargo.toml`/`pyproject.toml`/`clippy.toml`/
        `rust-toolchain.toml`/`deny.toml`/`rustfmt.toml`/`.typos.toml` found,
        incl. the workspace root, member crates of varying complexity, and
        `py-polars`/`pyo3-polars` `pyproject.toml`s). Baseline 57/57 pass — no
        deliberately-invalid fixtures in this corpus. Forward/idempotency/
        syntax-check/content-preservation (`toml_content_diff.py`, reused
        as-is) all 57/57 clean. Zero crashes on workspace-inheritance
        dependency tables (`{ workspace = true, features = [...] }`), the
        root `[workspace] members = [...]` array, or feature arrays. Zero
        bugs found.
      - `toml-lang/toml`: this is the TOML spec's own repo, not a compliance-
        test-suite checkout — a shallow clone contains only the spec prose
        (`toml.md`, `toml.abnf`, `docs/`, `CHANGELOG.md`) and tooling config,
        no `examples/`/`tests/` directory and no `.gitmodules` pointing at a
        separate compliance repo (e.g. `toml-lang/compliance`) — confirmed by
        direct inspection of the checkout, not assumed from directory naming.
        **In-scope corpus: 1 file** (`.prettierrc.toml`, `proseWrap =
        "always"` — Prettier tool config, not a TOML-format example/edge-case
        corpus). Baseline `toml_sc.js` clean. Forward/idempotency/syntax-check/
        content-preservation (`toml_content_diff.py`, reused as-is) all 1/1
        clean. Zero bugs found — no exotic values (hex/octal/binary,
        inf/nan, date-times, arrays-of-tables, deep inline-table nesting) were
        actually present to stress-test, since the repo's real canonical
        example/compliance corpus lives outside this repo. **This completes
        all 4 planned TOML Test-Fixture Repos** (`rust-lang/cargo`,
        `python-poetry/poetry`, `pola-rs/polars`, `toml-lang/toml`).

      **YAML (2/4 repos done):**
      - `kubernetes/kubernetes`: 6366 files found, above sampling threshold —
        sampled 455 files (evenly spaced across the full sorted list).
        Baseline 453/455 pass (2 Go/Salt template files using `{{...}}`
        misidentified by `.yaml` extension, expected fail). **In-scope
        corpus: 453 files.** **6 bugs found+fixed** across two sessions (5 via
        the forward pass crashing on genuinely-valid files, 1 idempotency-only,
        1 more via content-preservation on a final re-run):
        (1) sequence-of-mapping's first key rejected a same-indent nested
        sequence child (`- apiGroups:\n    - "*"`) though plain mapping keys
        already allowed it; fixed by mirroring the rule. (2)/(3) quoted and
        unquoted scalars wrapping across physical lines with a more-indented
        continuation (common in CRD/API `description` fields) crashed the
        line-based parser; fixed by detecting an unterminated quote or
        deeper non-key/non-seq continuation line and capturing it as an
        opaque multi-line scalar body (applied to mapping keys,
        sequence-of-mapping first keys, and later, per bug 6, plain sequence
        item values). (4) a dangling comment-only item (null key) inside a
        sequence-of-mapping's children threw a NullPointerException in the
        colon-alignment padding helper; fixed by excluding dangling items from
        the padding key list. (5, idempotency-only) multi-line-scalar and
        block-scalar (`|`/`>`) continuation bodies were stored at original
        ABSOLUTE indentation, so a second pass could leave the body
        less-indented than its re-rendered key line; fixed by storing each
        continuation's indent as a delta RELATIVE to its own key's original
        indent, re-anchored to the key's newly-rendered column at render
        time. Fixture (bugs 1-5) `test/real_code_regressions_71_{inp,out}
        .yaml`. `make test`: 120/120. Commit `fff5a3f`. (6, found via a final
        453-file content-preservation re-run, not syntax-check — output was
        still valid YAML): a `|`/`>` block scalar as a plain (non-keyed)
        sequence item's own value (e.g. a shell script in a `command:` array
        element) rendered as an empty string — the no-colon sequence-item
        branch never checked for a block-scalar header. Fixing it also
        surfaced a latent bug-5 render-offset error: a block scalar's anchor
        must be the dash's own rendered column (not the "+2" offset that's
        only correct for the quoted/plain-scalar case, whose baseline is the
        value's column) for a plain sequence item or sequence-of-mapping
        first key — only sibling keys use the +2 anchor. Extended the same
        fixture. `make test`: 120/120. Commit `025af9f`. **Final numbers
        after all 6 fixes, full 453-file re-run:** forward/idempotency/
        syntax-check/content-preservation (`yaml_content_diff.py`, written
        this session) all 453/453 clean (informational-only comment-
        capitalization diffs on a handful of files, not structural).
      - `docker/compose`: 261 files, full set processed. Baseline 250/261
        pass (11 Helm-chart templates using `{{...}}` as mapping keys/values,
        expected fail). **In-scope corpus: 250 files.** Round1: the same 11
        fail identically (no new failures), 250/250 processed. Idempotency
        and syntax-check both clean/matching baseline. **1 bug found+fixed
        via content-preservation** (not syntax-check — output stayed
        syntactically valid): a blank line right after a keyed line with no
        inline value (e.g. `services:` then a blank line then its nested
        mappings — common human-authored compose-file style) caused the
        entire nested block to be silently dropped. Root cause: all four
        "does this key have a child block" detection sites in
        `YamlSpecificRule.java` used a plain `peek()`, so a blank next line
        was read as "no child block" instead of being skipped past. Fixed by
        adding a `peekNonBlank()` helper and using it at all four sites.
        Fixture `test/real_code_regressions_72_{inp,out}.yaml`. `make test`:
        121/121. Commit `2640cf2`. Final 250-file re-run: forward/
        idempotency/syntax-check 250/250 clean; content-preservation
        249/250 — the one "failure" is a `yaml_content_diff.py` tool gap, not
        a formatter bug: `.github/ISSUE_TEMPLATE/bug_report.yml` contains an
        emoji PyYAML's `safe_load` rejects on both original and formatted
        copies (js-yaml/`yaml_sc.js` confirms both parse fine; manual diff
        shows only expected re-indentation). Flagged as a checker-script gap,
        not fixed (out of scope this session). `ansible/ansible`,
        `actions/starter-workflows` remain not-started.
      - `ansible/ansible`: 2110 `.yaml`/`.yml` files found (well over the
        several-hundred sampling threshold), sampled every 5th file (sorted
        list) for 422 files. Baseline `yaml_sc.js`: 7 fail (Ansible-specific
        custom scalar tags `!vault`/`!unsafe` js-yaml doesn't recognize) —
        **in-scope corpus: 415 files.** Forward pass: 422/422 processed, zero
        crashes. Idempotency: 422/422 clean. Syntax-check: same 7 baseline
        failures, no new ones. **3 bugs found, all via content-preservation**
        (`yaml_content_diff.py` — every corrupted output stayed syntactically
        valid YAML, syntax-check alone missed all three, consistent with
        every prior YAML dogfood session). (1) A plain (non-keyed) sequence
        item's own unquoted scalar value wrapping across physical lines
        (common in changelog-fragment prose, e.g. `- some long sentence\n
        continuing here.`) had no continuation handling at all — unlike the
        keyed/seqOfMapping-firstKey cases, which already captured this shape
        — silently dropping every line past the first. Fixed by adding the
        same multi-line-plain-scalar capture to that branch, anchored one
        column later than a keyed value's baseline (there's no `key:` prefix
        eating a column first, so the continuation's own column can be equal
        to, not just deeper than, the scalar's start column). (2) A comment
        line dedented below its enclosing block's own indent (a real
        `# FIXME: ...` note at column 0 sitting between two sibling keys
        indented deeper) made `parseBlock` break out of every enclosing block
        in turn without ever consuming it — each level's own indent check
        rejected it in turn, with no caller left to consume it once it
        bubbled past the outermost block — permanently orphaning it and
        silently dropping everything that followed at every level. Root
        cause: a comment's own column was compared directly against each
        block's indent instead of considering where the comment actually
        belongs structurally. Fixed by looking past the comment (and any
        more like it) to the next real content line and attaching the
        comment to whichever block that next line's own indent actually
        belongs to, rather than to whichever block happened to be innermost
        when the comment was reached. (3) A bare top-level plain-scalar
        document (e.g. an `$ANSIBLE_VAULT;...`-header vault blob: an unquoted
        first line followed by several more lines of opaque hex data with no
        `key:`/`- ` shape of their own) kept only its first line, silently
        dropping the rest — the bare-scalar-document detection added for a
        prior single-line case never accounted for further wrapped lines.
        Fixed by emitting the remaining raw lines verbatim once that shape is
        detected. Fixture (all 3 combined) `test/real_code_regressions_73_
        {inp,out}.yaml`. `make test`: 122/122. Commit `9f2a80a`. **Final
        numbers after all 3 fixes, full 422-file re-run:**
        forward/idempotency/syntax-check clean (422/422, 422/422, same 7
        baseline failures respectively); content-preservation 415/415 clean
        (the 7 custom-tag files are out of scope for this check too, since
        `yaml_content_diff.py` also can't parse them). `actions/starter-
        workflows` remains not-started for YAML.
      - `actions/starter-workflows`: 188 `.yaml`/`.yml` files found, full set
        processed (well below sampling threshold — this is a template-library
        repo, not a monorepo). Baseline `yaml_sc.js`: 2 fail (`code-scanning/
        nowsecure.yml`, `code-scanning/nowsecure-mobile-sbom.yml`, both
        containing an unquoted `group_id: {{ groupId }}` Handlebars-style
        template placeholder — js-yaml parses the double-brace as a flow
        mapping whose key is itself a mapping, "object-based map does not
        support complex keys"; pre-existing/invalid as authored, unrelated to
        the quoted `${{ ... }}` GitHub Actions expression syntax used
        everywhere else in the corpus). **In-scope corpus: 186 files.**
        Forward pass: 188/188 processed, zero crashes. Idempotency: 188/188
        clean. Syntax-check of round1 output: same 2 baseline failures, no
        new ones. **1 bug found+fixed, via syntax-check on round1 output**
        (not baseline, not idempotency -- the corrupted output failed to
        parse at all, unlike every bug in the three prior YAML sessions,
        which were all only catchable via content-preservation): `code-
        scanning/codescan.yml` uses `-   name: foo` (extra spaces after the
        sequence dash before its first key, a hand-authored alignment style)
        for its `steps:` items. `parseSeqItem` computed the sibling-key
        column (used to decide whether the next key belongs to the same
        mapping as the sequence item's first key, or is instead a nested
        child of that first key) as a hardcoded `ln.indent + 2` -- correct
        only when exactly one space follows the dash. With extra padding,
        the real key column was deeper than that hardcoded value, so the
        next sibling key (`uses:`) was misidentified as a nested child one
        level too deep, producing an invalid "bad indentation" YAML output.
        Fixed by deriving the real first-key column from the dash line's
        actual leading whitespace (`keyCol`) and using it consistently for
        the sibling/nested-child decision and every multi-line-scalar
        continuation anchor point that previously assumed the hardcoded
        offset. Fixture `test/real_code_regressions_75_{inp,out}.yaml`.
        `make test`: 124/124. Commit `f1648c5`. **Final numbers after the
        fix, full 188-file re-run:** forward 188/188 clean; idempotency
        188/188 clean; syntax-check same 2 baseline failures, no new ones;
        content-preservation (`yaml_content_diff.py`) 179/186 clean, the
        remaining 7 all `ERROR: original file failed to parse` -- PyYAML's
        `safe_load` rejects an emoji character (`💁`/`📚`/`🦅`/`🏁`/`👋`, one
        per file) present in a prose comment, the same known
        `yaml_content_diff.py` tool gap first flagged during the
        `docker/compose` session (`yaml_sc.js`/js-yaml handles the same files
        fine on both original and formatted copies; manual line-by-line diff
        of all 7 confirmed only expected re-indentation and comment-
        capitalization changes, zero structural differences) -- not a
        formatter bug. **This completes all 4 originally-planned YAML
        Test-Fixture Repos** (`kubernetes/kubernetes`, `docker/compose`,
        `ansible/ansible`, `actions/starter-workflows`); two further repos
        (`prometheus/prometheus`, `home-assistant/core`) were added to the
        list above after this plan was made and remain not-started.

      **`prometheus/prometheus` (added-repo session, done):**
      - Fresh shallow clone. 380 `.yml`/`.yaml` files found (no `vendor/`
        directory present), full set processed in one batch invocation.
        Baseline `yaml_sc.js`: 5 fail, all `config/testdata/*.bad.yml`
        (`static_config.bad.yml`, `labelvalue.bad.yml`, `labelname.bad.yml`,
        `section_key_dup.bad.yml`, `labelmap.bad.yml`) — these are
        deliberately-invalid YAML-syntax test fixtures for Prometheus's own
        config-parser error-path tests (unknown `!!binary` scalar tags, a
        duplicated mapping key), not naive `*bad.yml`-glob false positives
        (152 files match that substring but most are legitimately-valid-YAML
        Prometheus-config-*schema*-invalid fixtures, not YAML-syntax-invalid
        ones — the exclusion list was derived from `yaml_sc.js`'s actual
        baseline failures, not filename pattern matching). **In-scope
        corpus: 375 files.** **4 bugs found+fixed**, all data loss, all only
        catchable via `yaml_content_diff.py` content-preservation (not
        syntax-check, since the corrupted output stayed syntactically
        valid), all in `YamlSpecificRule`'s sequence/mapping parsing, sharing
        the common theme of a dash/key line whose "value" is entirely
        absent/comment-only/anchor-tag-only/an unbalanced multi-line flow
        opener, with the real content on subsequent more-indented lines:
        (1) `parseKeyItem`'s early return for a flow-looking inline value
        didn't check whether the flow was actually closed on the same
        physical line, truncating everything after an unbalanced multi-line
        `[...]` opener (`documentation/examples/prometheus-kubernetes.yml`'s
        `source_labels: [...]` spanning several lines — 300→52 lines lost).
        (2) `parseSeqItem`'s `seqOfMapping` first-key handling had the same
        flow-closure gap for `- source_labels:\n    [...]` (opener entirely
        on the following line), and separately dropped everything after a
        `- # comment`-only dash line whose real mapping keys start on
        subsequent lines (`- # comment\n  region: eu-west-2`, found in a
        `prometheus-outscale.yml`-shaped fixture). (3) a dash line holding
        only an anchor tag (`- &highalert`) followed by a nested mapping at
        an indent *equal to* (not just greater than) the dash's own key
        column lost its child block entirely (`model/rulefmt/testdata/
        test_aliases.yaml`). (4) `renderFlowValue` rendered an empty flow
        map/seq (`{}`/`[]`) as a block conversion whenever its line didn't
        `fits()`, but `renderFlowBlock`'s loop has nothing to iterate for
        zero entries, so the value silently vanished — triggered whenever a
        long key elsewhere in the same colon-alignment group pushed an
        unrelated `{}`/`[]` line past the width limit, found in `web/ui/
        pnpm-lock.yaml` and `web/ui/react-app/pnpm-lock.yaml`'s dependency
        snapshot maps (most visibly when the dropped key was the file's very
        last line, e.g. `'@csstools/css-tokenizer@3.0.4': {}` at true EOF).
        All 4 combined into one fixture, `test/real_code_regressions_83_
        {inp,out}.yaml`. `make test`: 132/132 (133/133 after a concurrent
        session's own unrelated fixture 84 landed alongside). Commit
        `<pending — see commit below>`. **Final numbers after all 4 fixes,
        full 380-file re-run:** forward 380/380 clean, zero crashes;
        idempotency (`diff -rq round1 round2`) 380/380 clean; syntax-check
        of round1 output: same 5 baseline failures, no new ones;
        content-preservation (`yaml_content_diff.py`) 375/375 clean across
        the full in-scope corpus. **This completes `prometheus/prometheus`**
        in the YAML Test-Fixture Repos list; `home-assistant/core` remains
        not-started.

      **`home-assistant/core` (added-repo session, done — completes the YAML
      Test-Fixture Repos list):**
      - Fresh shallow clone. 921 `.yaml`/`.yml` files found (excluding
        `node_modules/`), full set processed in one batch invocation (below
        the several-hundred+ sampling threshold's practical ceiling for a
        single-pass run). Baseline `yaml_sc.js`: 43 fail — Home Assistant's
        own custom-tag/templating idioms (`!include`/`!secret`/`!env_var`
        custom YAML tags, `{{ ... }}` Jinja2 blueprint placeholders used as
        bare scalars) js-yaml doesn't recognize, same class of exclusion as
        `ansible/ansible`'s `!vault`/`!unsafe` and `actions/starter-
        workflows`'s Handlebars placeholders — confirmed via `yaml_sc.js`'s
        actual baseline failures, not filename-pattern guessing. **In-scope
        corpus: 878 files.** Forward pass (pre-fix jar): 921/921 processed,
        zero crashes. **1 bug found+fixed, via `yaml_content_diff.py`
        content-preservation** (not syntax-check — every corrupted output
        stayed syntactically valid YAML, consistent with every prior YAML
        dogfood session's bug history): a sequence item whose own value is
        itself another sequence, written in the compact single-line form
        `- - a\n  - b` (this repo's `services.yaml` files use it for
        mutually-exclusive `supported_features` group pairs, e.g. `toggle`
        needing either `OPEN` or `CLOSE`). `parseSeqItem` never recognized
        this shape at all — the inner `- ` was captured as a literal scalar
        value, leaving the sibling nested-seq item on the next physical line
        completely unconsumed in the line stream; that orphaned line's
        indent then didn't match the enclosing block's own `blockIndent`, so
        `parseBlock`'s loop broke out of the entire enclosing block early —
        silently dropping the rest of the nested sequence AND every sibling
        item/key that followed it, at every level (6 files affected:
        `cover`/`media_player`/`overkiz`/`siren`/`valve`/`wmspro`
        `services.yaml`, each losing everything from the second feature-
        group entry onward, including unrelated later top-level keys like
        `fields`/`stop_cover`). Fixed by detecting the nested-dash shape up
        front in `parseSeqItem` and parsing it via a new
        `parseInlineNestedSeq` helper (the inline dash-line's own value,
        recursing for further nesting, plus sibling nested items via the
        ordinary `parseBlock`); rendered through the existing generic
        `item.children` sequence-render path, which non-lossily expands the
        compact `- -` form into a bare dash followed by the nested items one
        level deeper — always valid YAML, no data loss, same "prefer an
        unambiguous expanded form" precedent as this formatter's flow-to-
        block conversion elsewhere. Fixture `test/real_code_regressions_86_
        {inp,out}.yaml`. `make test`: 135/135 forward + idempotency. Commit
        `e7f0334`. **Final numbers after the fix, full 921-file re-run:**
        forward 921/921 clean, zero crashes; idempotency (`diff -rq round1
        round2`) 921/921 clean; syntax-check of round1 output: same 43
        baseline failures, no new ones; content-preservation
        (`yaml_content_diff.py`) 870/878 clean across the in-scope corpus —
        the remaining 8 are the same `yaml_content_diff.py`/PyYAML
        emoji-in-comment tool gap first flagged during the `docker/compose`
        session (`.github/workflows/{detect-duplicate-issues,detect-non-
        english-issues,stale}.yml` and `homeassistant/components/{bring,
        habitica,html5,matrix,telegram_bot}/services.yaml`, one emoji each
        — `yaml_sc.js`/js-yaml parses all 8 fine on both original and
        formatted copies; not a formatter bug). **This completes
        `home-assistant/core`**, and with it all 6 planned YAML Test-Fixture
        Repos (4 originally-planned plus both later-added) — the YAML
        Test-Fixture Repos list is now fully exhausted.

      **HTML5 (1/4 repos done, first HTML5 dogfood run):**
      - `h5bp/html5-boilerplate`: 4 files (`dist/index.html`, `dist/404.html`,
        `src/index.html`, `src/404.html` — this version of the boilerplate
        uses external `<script src="...">`/linked stylesheets, no inline
        `<script>` bodies, but both `404.html` variants have an inline
        `<style>` block, exercising the CSS dispatch). Baseline `html_sc.js`
        4/4 clean. Forward 4/4 processed, zero errors/crashes. Idempotency
        (`diff -rq round1 round2`) 4/4 clean. Syntax-check of round1 output
        4/4 clean, matching baseline. Wrote `html_content_diff.py` this
        session (see "Dogfood Output Validation"), verified against a
        hand-crafted good pair plus two bad pairs (dropped attribute,
        corrupted comment) before trusting it — both bad cases caught
        correctly. Content-preservation 4/4 clean (DOCTYPE, element/attribute
        order, text, and comment content all preserved; `<style>` dispatch to
        `CssSpecificRule` confirmed present with non-empty body per the
        tool's script/style opaque-body-presence check). **Zero bugs found.**
        `twbs/bootstrap` (docs site), `mdn/content`, `whatwg/html` remain
        not-started for HTML5.
