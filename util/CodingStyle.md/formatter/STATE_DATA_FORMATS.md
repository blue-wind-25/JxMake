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
`~/Projects/JxMake/0_excluded_directory/personal/SyntaxChecker/` (outside
the repo, alongside `java_sc`/`kotlin_sc`, not committed):

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
  (small, dense GitHub Actions YAML, good for quick spot checks)
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
      `STYLE_DATA_FORMATS.md`'s listed test-fixture repos per sub-format —
      **`json5/json5` done, `microsoft/vscode` done, `babel/babel` done**
      (see below); `eslint/eslint` still not started for JSON;
      **`apache/maven` done for XML (first XML dogfood run, see below)**;
      `apache/ant`/`jenkinsci/jenkins`/`w3c/svgwg` still not started for XML;
      **`twbs/bootstrap`/`necolas/normalize.css`/`foundation/foundation-sites`/
      `primer/css` done for CSS — all four CSS test-fixture repos now
      dogfood-tested** (see below);
      `h5bp/html5-boilerplate`/etc. still not started for HTML5;
      **`kubernetes/kubernetes` done for YAML (first YAML dogfood run, see
      below)**; `docker/compose`/`ansible/ansible`/`actions/starter-workflows`
      still not started for YAML;
      **`rust-lang/cargo`/`python-poetry/poetry` done for TOML (see below)**;
      `pola-rs/polars`/`toml-lang/toml` still not started for TOML.
      **`twbs/bootstrap` (CSS, first CSS dogfood run; fresh shallow clone
      `--depth 1`, not found under `/tmp` from a prior session):** bootstrap's
      real hand-authored source is `.scss`, not `.css` (this formatter only
      handles CSS proper, not SCSS) — its only genuine hand-authored `.css`
      is 47 files under `site/src/assets/examples/**` (Bootstrap's docs-site
      HTML example pages), plus `dist/css/**` (16 files) which is
      SCSS-compiled+minified *generated* output, excluded per this session's
      sizing guidance. **In-scope corpus: 31 files** (47 minus 16 `dist/`),
      a small corpus so the **full set** was processed, not a sample.
      Baseline syntax-check of the unformatted originals (`css_sc.js`):
      31/31 pass. Round1 format (`--preserve-tree --root`, one invocation):
      exit 0, 31/31 processed. Round2 vs round1: `diff -rq` empty (clean
      idempotency, 31/31). Syntax-check of round1 output: 31/31 pass,
      matching baseline. **Content-preservation spot-check** (this session's
      new requirement beyond syntax-checking, since CSS bugs can produce
      still-valid-but-semantically-wrong output): a comment-stripped/
      whitespace-collapsed token-stream diff of every one of the 31 files'
      original vs round1 output found 31/31 exact matches — **except** it
      cannot see inside comments (stripped before comparing), which is
      exactly where the one bug below was hiding; found instead by manually
      grepping/reading the two files (`carousel.css`) that contain the
      `@media` rules plus lowercase-starting `/* rtl:... */` comments used
      by the rtlcss build tool. **One bug found+fixed:**
      `FormatterSimpleBraced.capitalizeCommentStart` (shared by CSS/JSON5's
      `normalize-comment-start-case`) unconditionally capitalized any
      lowercase-starting comment's first letter with no exclusion mechanism
      (the class's own doc comment claimed "JSON/CSS have no language
      keywords a comment could start with that would need protecting" —
      true for keywords, but not for third-party *tool directives*).
      `carousel.css`'s `/* rtl:begin:ignore */`/`/* rtl:end:ignore */`/
      `/* rtl:begin:remove */`/`/* rtl:end:remove */` (rtlcss's
      case-sensitive RTL-conversion-suppression directive) got silently
      corrupted to `/* Rtl:begin:ignore */` etc. — still perfectly valid
      CSS syntactically (why `css_sc.js` never caught it), but semantically
      broken for any pipeline that runs rtlcss over the formatter's output.
      Fixed by adding `FormatterSimpleBraced.isSingleTokenDirective`: skip
      capitalization when the comment's entire first-line body (up to
      end-of-line/comment-close) is one whitespace-free token containing `:`
      or `-` (directive-shaped, e.g. `rtl:begin:ignore`,
      `stylelint-disable`), while ordinary prose that happens to start with
      a similar word followed by more text (e.g. `auto-generated file, do
      not edit`) is still capitalized as before — verified via direct unit
      calls covering both cases before/after the fix. Fixture:
      `test/real_code_regressions_69_{inp,out}.css` (`test/README.txt`).
      `make test`: 118/118 forward + 118/118 idempotency, zero regressions.
      **Final full re-run after the fix** (all 31 in-scope files,
      forward+idempotency+syntax-check+content-preservation-spot-check
      repeated end-to-end): 31/31 forward, 31/31 idempotency (`diff -rq`
      empty), 31/31 syntax-check pass, 31/31 content-preservation match,
      plus a manual re-check confirming all 4 `rtl:` directive occurrences
      in `carousel.css`'s round1 output are now byte-identical to source
      (no stray capitalization). Commit `8f5f597`.
      **`necolas/normalize.css` (fresh shallow clone, `--depth 1`, not found
      under `/tmp` from a prior session):** genuinely tiny corpus — exactly
      **1 hand-authored `.css` file** (`normalize.css` itself, 6138 bytes);
      the repo has no `test/`/`node_modules`/`dist`/`build` `.css` matches
      and no compiled/minified copy alongside it (`test.html` references it
      directly, `package.json` has no build step producing a second CSS
      artifact). Full set (1 file) processed, no sampling needed. Baseline
      syntax-check of the unformatted original (`css_sc.js`): pass. Round1
      format (`--preserve-tree --root`): exit 0, 1/1 processed. Round2 vs
      round1: `diff -rq` empty (idempotent). Syntax-check of round1 output:
      pass, matching baseline. **Content-preservation spot-check** (the one
      file, checked in full, not sampled), via the new permanent
      `css_content_diff.py` (see "Dogfood Output Validation" above, written
      this session): all 71 comments byte-identical in content (only
      re-indentation/re-wrap, no wording/case/punctuation change); the
      comment-stripped, colon/whitespace-normalized token stream identical
      (confirms no property/value/selector was altered); `!important` count
      matched (0 in both); vendor-prefixed property counts matched exactly
      (8 occurrences across 6 distinct prefixed properties in both), ruling
      out silent vendor-prefix drift. Script exits 0 (`OK: content
      preserved...`). Manual `diff` of the two files independently confirms
      the only differences are indentation width (2→4 spaces, the tool's
      default) and colon-alignment padding — no semantic change.
      **Zero bugs found** — forward 1/1, idempotency 1/1, syntax-check 1/1,
      content-preservation 1/1 (comments, tokens, `!important` count, vendor-
      prefix count all matched exactly). No new fixtures needed (nothing to
      regress-test).
      **`foundation/foundation-sites` (fresh shallow clone, `--depth 1`, not
      found under `/tmp` from a prior session):** verified foundation-sites'
      real hand-authored source is `.scss` (136 files under `scss/`), same as
      bootstrap — but unlike bootstrap, this repo has **zero genuinely
      hand-authored `.css` files anywhere**. The only `.css` matches in the
      whole tree are the 8 files under `dist/css/**`
      (`foundation.css`/`foundation-rtl.css`/`foundation-float.css`/
      `foundation-prototype.css` and their four `.min.css` counterparts,
      each with a matching `.css.map` sourcemap confirming Sass-compiled
      origin) — produced by the `gulp` `sass` task per `gulpfile.js`'s
      `build`/`watch` tasks, same generated-output reasoning as bootstrap's
      excluded `dist/css/**`. No docs/example HTML-adjacent hand-authored
      CSS exists in this repo the way bootstrap's `site/src/assets/
      examples/**` did (`docs/`'s ~191 HTML files have no accompanying
      plain-`.css` assets; `_vendor/` is Sass helper libraries, no CSS).
      **In-scope corpus: 0 files** — nothing to format, round-trip, syntax-
      check, or content-diff. No forward/idempotency/syntax-check/content-
      preservation pass was run (would be vacuous), no fixtures added (no
      bug to regress-test), no bugs found.
      **`primer/css` (fresh shallow clone `--depth 1`, not found under
      `/tmp` from a prior session; `github.com/primer/css` still exists,
      no archival/redirect — GitHub's own Primer CSS design system, v22.3.0
      per `package.json`):** verified this repo's real hand-authored source
      is `.scss` under `src/` (113 files), same pattern as bootstrap/
      foundation-sites. Unlike foundation-sites, this repo does have a
      small genuinely hand-authored `.css` corpus outside `src/`/`dist/`:
      **2 files**, `docs/.storybook/preview.css` (an `@import`-only file
      pulling in `@primer/primitives` CSS custom-property bundles, no rules
      of its own) and `docs/.storybook/storybook.css` (60 lines, ordinary
      selectors/declarations, Storybook-preview-only styling — no
      `dist/`/`.min.css`/`.css.map` matches anywhere in the tree, so nothing
      to exclude as generated). **In-scope corpus: 2 files, full set
      processed** (too small to sample). Baseline syntax-check of the
      unformatted originals (`css_sc.js`): 2/2 pass. Round1 format
      (`--preserve-tree --root`, one invocation): exit 0, 2/2 processed.
      Round2 vs round1: `diff -r` empty (idempotent, 2/2). Syntax-check of
      round1 output: 2/2 pass, matching baseline. **Content-preservation
      check** (`css_content_diff.py`, both files, full not sampled):
      `storybook.css` — exit 0, exact match (0 comments, 3 `!important`,
      0 vendor-prefixed properties, all matched). `preview.css` — exit 1,
      one comment-text mismatch: `/* color */` → `/* Color */`. Investigated
      and confirmed **not a bug**: `normalize-comment-start-case=on` is the
      documented default, and per the `real_code_regressions_69` fix (this
      same file, twbs/bootstrap session), only directive-shaped comments
      (single whitespace-free token containing `:` or `-`, e.g.
      `rtl:begin:ignore`) are excluded from capitalization — `color` is a
      plain word with neither `:` nor `-`, so `isSingleTokenDirective`
      correctly returns false and the comment is capitalized as intended,
      exactly the same normalization normal prose comments always get. A
      manual `diff` of both original-vs-round1 file pairs found no other
      discrepancy beyond this one intentional capitalization and expected
      re-indentation/colon-alignment (2-space → 4-space indent,
      colon-alignment padding on `storybook.css`'s declaration blocks).
      **Zero bugs found** — forward 2/2, idempotency 2/2, syntax-check 2/2,
      content-preservation 2/2 (once the one intentional, correctly-applied
      capitalization is accounted for as expected behavior, not a defect).
      No new fixtures needed (nothing to regress-test; the capitalization
      behavior exercised here is already covered by `real_code_regressions_69`).
      **All four CSS test-fixture repos are now dogfood-tested:**
      `twbs/bootstrap` (31 in-scope files, 1 bug found+fixed — comment
      capitalization corrupting rtlcss directives, fixture
      `real_code_regressions_69`); `necolas/normalize.css` (1 in-scope file,
      zero bugs); `foundation/foundation-sites` (0 in-scope files, 100%
      SCSS-compiled); `primer/css` (2 in-scope files, zero bugs). CSS's
      real-code-testing sub-portion of this checklist item is complete;
      the overall item stays unchecked pending JSON's `eslint/eslint`, XML,
      HTML5, YAML, and TOML repos.
      **`json5/json5` (fresh clone, not found under `/tmp` from a prior
      session):** small corpus — 6 hand-authored `.json`/`.json5` files
      total (`.eslintrc.json`, `package.json`, `package-lock.json`,
      `package.json5`, `test/test.json5`, `test/invalid.json5`; no
      `node_modules`/`.git`/`dist`/`build` matches). Baseline syntax-check
      of the unformatted originals first (per methodology, to rule out
      pre-existing invalid fixtures): 5/6 pass, `test/invalid.json5`
      (deliberately-invalid single-byte fixture `a`, used by the repo's own
      test suite to test invalid-JSON5 handling) fails with
      `1:1: JSON5: invalid character 'a' at 1:1` — expected, not a
      formatter bug. Round1 format (`--preserve-tree --root`): 6/6 files
      processed, exit 0. Round2 vs round1: `diff -r` empty (idempotent).
      Syntax-check of round1 output: identical result to baseline — 5/6
      pass, `test/invalid.json5` fails with the exact same message as the
      unformatted original (formatter left its content byte-identical, `a`,
      confirmed via direct diff). **Zero bugs found** — forward 6/6,
      idempotency 6/6, syntax-check 5/6 (matching baseline exactly, the one
      failure being the repo's own pre-existing invalid fixture, not
      formatter-induced). No new fixtures needed (nothing to regress-test).
      Corpus is small enough that a second JSON/JSON5-listed repo
      (`microsoft/vscode`/`babel/babel`/`eslint/eslint`) would give more
      real-code coverage in a future session.
      **`microsoft/vscode` (fresh shallow clone, `--depth 1`, not found
      under `/tmp` from a prior session):** much larger monorepo — 1377
      `.json`/`.json5` files found after excluding `node_modules`/`.git`/
      `out`/`dist`/`build` (shallow clone has no `node_modules` anyway; zero
      `.json5` files exist in this repo). Below the "several thousand+"
      threshold for mandatory sampling, so the **full set** was processed,
      not a sample. Of the 1377: 5 are genuinely empty files (0 bytes,
      `extensions/copilot/test/simulation/fixtures/{gen-json/test.json,
      tests/simple-ts-proj*/tsconfig.json}`) — the formatter correctly
      throws `JsonParseException: unexpected end of input` on these; not a
      bug (an empty file isn't valid JSON either) and not JSONC, just
      genuinely-invalid/placeholder fixtures. Of the remaining 1372
      successfully-parsed files, 100 are real JSONC (`.vscode/*.json`,
      `tsconfig.json`, `language-configuration.json`, etc. — verified: 87
      contain `//`/`/* */` comments, the other 13 contain no comment marker
      but do contain a trailing comma before `}`/`]`, confirmed by direct
      grep on each) and were excluded from formatter-bug scope per this
      session's instructions (plain `.json` mode has no comment/
      trailing-comma support, out of scope by design) — baseline
      syntax-check of the unformatted originals confirms all 100 already
      fail `JSON.parse` before the formatter ever touches them, so any
      formatter behavior on them is moot. **In-scope corpus: 1272 genuinely
      RFC-8259-clean `.json` files.**
      Round1 format (`--preserve-tree --root`, one invocation, all 1377
      files): exit 123 (the 5 empty-file errors), 1372 files written to
      round1, matching expectations. Round2 (reformat round1's 1372 output
      files): exit 0, zero internal errors. **`diff -r round1 round2` found
      one real idempotency bug**, fixed this session (see below) — after
      the fix, a full re-run of round1→round2 across all 1377 files (1372
      succeeding) shows `diff -rq` empty (0 differing files) — clean
      idempotency. Syntax-check (of the 1272 in-scope files' round1 output,
      via `json_sc.js`): 1272/1272 pass, exactly matching the 1272/1272
      baseline pass count on the unformatted originals — zero
      formatter-induced corruption.
      **One bug found+fixed:** `JsonSpecificRule.parseContainer`'s
      closing-brace handling kept a dangling placeholder `Item` for *any*
      blank line before the closer, not just a real dangling comment. A
      comment-less blank-only placeholder (`{` then only blank lines then
      `}`) made `Container.items` non-empty, so `render()`'s
      `c.items.isEmpty()` short-circuit for tight `{}"`/`"[]"` never fired,
      forcing loose `"{\n}"` rendering — but `renderItems`'s
      `i > 0 && item.blankBefore` check never actually emits that blank line
      for the first (only) item, so the loose output round1 produced had no
      blank line before `}`. Reformatting that (round2) found no
      leadingComments/blankBefore on re-parse, so `items.isEmpty()` was
      correctly true the second time and collapsed to tight `"{}"` — a
      genuine non-idempotency (`extensions/vscode-api-tests/testWorkspace/
      bower.json`, whose source is `"{\n\n\t\n}\n"`). Fixed by only keeping
      the dangling placeholder when `!item.leadingComments.isEmpty()` (drop
      the `|| item.blankBefore` disjunct) — a comment-less blank line before
      the closer is now dropped at parse time, so the whole container
      renders straight to tight `"{}"` in one pass, matching what round2
      would have collapsed to anyway. Fixture added:
      `test/real_code_regressions_68_{inp,out}.json` (no copyright-header
      comment — plain `.json` has no comment syntax to carry it, per
      `STATE_COMMON.md`'s methodology's own carve-out). `make test`:
      117/117 forward + 117/117 idempotency, zero regressions. Commit
      `e2a6f0e`.
      **`babel/babel` (fresh shallow clone, `--depth 1`, not found under
      `/tmp` from a prior session):** large monorepo — 9245 `.json`/`.json5`
      files found after excluding `node_modules`/`.git`/`lib`/`dist`/`build`
      (zero `.json5` files exist in this repo; shallow clone has no
      `node_modules` anyway). Well above the "several thousand+" sampling
      threshold, so a **representative sample of 964 files** was taken (not
      the full set) per this session's sizing guidance: all 204
      package-level `package.json`s across every package, all 500
      non-fixture `.json` files (config files, `.babelrc`-equivalents,
      etc.), plus every 20th file (438 files) from the 8745
      `test/fixtures/**` options.json-style files, deduplicated to 964
      unique paths. Baseline syntax-check of the unformatted sample first
      (per methodology): 810/964 pass, 154 fail as JSONC-flavored
      `tsconfig.json`/`tsconfig.paths.json` (contain `/* ... */` header
      comments — same carve-out as the vscode run, out of scope for this
      formatter's plain-`.json` mode by design), and 2 fail as
      deliberately-invalid fixtures used by babel's own error-handling test
      suite (`packages/babel-core/test/fixtures/{config/config-files/
      pkg-error,errors/invalid-pkg-json}/package.json`, both intentionally
      malformed JSON, e.g. `{\n  foo\n}` — same `test/invalid.json5`-style
      precedent as the json5/json5 run). **In-scope corpus: 810 genuinely
      RFC-8259-clean `.json` files.** (One methodology note: the sampled
      fixture tree includes at least one legitimate directory name
      containing literal spaces — `packages/babel-cli/test/fixtures/babel/
      dir --out-dir --watch multiple dir/options.json` — file-list handling
      must preserve it as one path, e.g. `xargs -d '\n'`, not naive
      unquoted `$(cat ...)` word-splitting, which breaks on it.)
      Round1 format (`--preserve-tree --root`, one invocation, all 810
      in-scope files): exit 0, 810/810 processed. Round2 (reformat round1's
      810 output files): exit 0, `diff -rq round1 round2` empty — clean
      idempotency, 810/810. Syntax-check of round1 output (`json_sc.js`):
      810/810 pass, exactly matching the 810/810 baseline pass count on the
      unformatted originals. **Zero bugs found** — forward 810/810,
      idempotency 810/810, syntax-check 810/810 (matching baseline exactly).
      No new fixtures needed (nothing to regress-test).
      **`eslint/eslint` (fresh shallow clone, `--depth 1`, not found under
      `/tmp` from a prior session):** small corpus — 98 `.json`/`.json5`
      files total after excluding `node_modules`/`.git`/`dist`/`build`/
      `coverage` (1 `.json5` — `.github/renovate.json5`; the rest `.json`).
      Below any sampling threshold, so the **full set** was processed, not a
      sample. Baseline syntax-check of the unformatted originals first (per
      methodology): 92/98 pass. **6 excluded as JSONC/pre-existing-invalid**
      (same carve-out as the vscode/babel runs): `tsconfig.types-legacy.json`
      (trailing `//` comment inside `compilerOptions`, real JSONC);
      `tests/fixtures/configurations/comments.json` (`/* */`/`//` comments,
      a deliberate fixture for eslint's own comments-in-config test);
      `tests/fixtures/config-file/broken-package-json/package.json` and
      `tests/fixtures/ignored-paths/broken-package-json/package.json` (both
      deliberately malformed — missing closing brace / missing comma — used
      by eslint's own broken-config error-handling tests, same
      `test/invalid.json5`-style precedent as the json5/json5 run);
      `tests/fixtures/configurations/empty/empty.json` (genuinely 0 bytes,
      not valid JSON, not JSONC either); and, found only once formatting was
      attempted (not caught by the baseline check since `JSON.parse` and
      this formatter both reject a leading UTF-8 BOM before any content),
      **2 more BOM-prefixed files** — `tests/fixtures/config-file/bom/
      package.json` and `tests/fixtures/config-file/bom/.eslintrc.json`
      (both deliberate fixtures for eslint's own BOM-handling tests) — both
      fail `JSON.parse` on the raw BOM byte identically to how the
      formatter's own parser rejects it (`unexpected token: ﻿`),
      confirmed via direct baseline re-check, so excluded as
      pre-existing-invalid too, not a formatter bug (a corpus of 6 baseline
      failures total). **In-scope corpus: 91 files** (90 `.json` + 1
      `.json5`; the two BOM files were only discovered during round1 and
      then retroactively excluded and re-verified against baseline, so the
      92-minus-1 arithmetic reflects that). Round1 format
      (`--preserve-tree --root`, one invocation): exit 0, 91/91 processed.
      Round2 vs round1: `diff -rq` empty (idempotent, 91/91). Syntax-check
      of round1 output: 90/90 `.json` + 1/1 `.json5` pass, exactly matching
      the 91/91 baseline pass count on the unformatted in-scope originals.
      Manual spot-check of `package.json` and `.github/renovate.json5`
      diffs confirms only re-indentation/colon-alignment, no content loss.
      **Zero bugs found** — forward 91/91, idempotency 91/91, syntax-check
      91/91 (matching baseline exactly). No new fixtures needed (nothing to
      regress-test).
      **All four JSON/JSON5 test-fixture repos are now dogfood-tested:**
      `json5/json5` (6 files, zero bugs); `microsoft/vscode` (1272 in-scope
      files, 1 bug found+fixed — `JsonSpecificRule.parseContainer` blank-line-
      before-closer non-idempotency, fixture `real_code_regressions_68`);
      `babel/babel` (810 in-scope files, sampled from 9245 found, zero bugs);
      `eslint/eslint` (91 in-scope files, zero bugs). JSON/JSON5's
      real-code-testing sub-portion of this checklist item is complete; the
      overall item stays unchecked pending XML, HTML5, YAML, and TOML
      repos (CSS's sub-portion is already complete per its own note above).
      **`apache/maven` (XML, first XML dogfood run; fresh shallow clone
      `--depth 1`, not found under `/tmp` from a prior session):** 3158
      `.xml` files found (no `target`/`build`/generated-output dirs present
      at all — a fresh shallow clone with no build ever run — so no
      exclusions were needed on that front). 3158 is well above the "several
      hundred+" sampling threshold, so a **representative sample of 398
      files** was taken (not the full set), per this session's sizing
      guidance: **all 90 top-level/module `pom.xml` files** (one per Maven
      module across the whole multi-module repo, giving a genuine
      cross-section of Maven's signature artifact from trivial parent POMs
      to large multi-dependency ones), plus every 10th of the remaining
      1882 `pom.xml` files living under `src/.../test/resources/**` (189
      sampled — these are small hand-authored POM fixtures used by Maven's
      own model-builder/core test suites), plus every 10th of the 1186
      non-`pom.xml` `.xml` files (settings.xml, `maven-metadata.xml`,
      `extensions.xml`, `components.xml`, `web.xml`, Doxia `site.xml`, etc.;
      119 sampled) — 398 total. Baseline syntax-check of the unformatted
      sample first (`xml_sc.js`): 397/398 pass; the one failure
      (`its/core-it-suite/src/test/resources/mng-5898/servlets/servlet/
      src/main/webapp/WEB-INF/web.xml`) is a genuinely 0-byte fixture file
      (not valid XML by definition), a pre-existing repo artifact, not a
      formatter concern. Round1 format (`--preserve-tree --root`, one
      invocation): exit 0, 398/398 processed (the empty file stays empty, as
      expected — same as the JSON dogfood runs' empty-file precedent).
      Round2 vs round1: `diff -rq` empty across all 398 files — clean
      idempotency. Syntax-check of round1 output: 397/398 pass, exactly
      matching the 397/398 baseline — zero formatter-induced corruption.
      **Content-preservation check:** no `xml_content_diff.py` existed yet
      for XML (unlike CSS's `css_content_diff.py`) — written this session
      (see "Dogfood Output Validation" above), verified against a
      hand-crafted good/bad pair before trusting it for real use, then run
      across all 397 non-empty sample files: 355/397 exact match, 42/397
      reported a mismatch — **all 86 individual mismatches across those 42
      files are comment-text differences that are case-insensitive-equal to
      the original** (verified programmatically, not just by inspection),
      i.e. exactly the documented `normalize-comment-start-case=on` default
      behavior capitalizing a lowercase-starting prose comment's first
      letter (e.g. `'various versions'` -> `'Various versions'`) — the same
      already-accepted normalization the `primer/css` CSS dogfood session
      hit and confirmed was correct behavior, not a defect. Zero attribute-
      order, text-content, CDATA-content, or structural (node-type/child-
      count) mismatches were found in any of the 397 files. **Zero bugs
      found** — forward 397/398 (1 pre-existing empty fixture, expected),
      idempotency 398/398, syntax-check 397/398 (matching baseline exactly),
      content-preservation 397/397 once the universally-expected comment
      capitalization is accounted for. No new fixtures needed (nothing to
      regress-test). `apache/ant`, `jenkinsci/jenkins`, `w3c/svgwg` remain
      not-started for XML.
      **`rust-lang/cargo` (TOML, first TOML dogfood run; fresh shallow clone
      `--depth 1`, not found under `/tmp` from a prior session):** 672 `.toml`
      files found (no `target`/`build`/generated-output dirs present at all —
      a fresh shallow clone that's never been built). Not above the "several
      hundred+" sampling threshold by enough to force sampling, and the files
      are all small, so the **full set was processed**, not a sample.
      Baseline syntax-check of the unformatted originals (`toml_sc.js`):
      670/672 pass; the 2 failures
      (`tests/testsuite/cargo_add/invalid_manifest/{in,out}/Cargo.toml`) are
      cargo's own deliberately-invalid fixtures for its manifest
      error-handling tests (`[invalid-section]` followed by a bare
      `key = invalid-value` with no quotes) — confirmed genuinely invalid
      TOML via the same real-parser baseline-check precedent as
      `json5/json5`'s `test/invalid.json5`/`eslint/eslint`'s broken-package-json
      fixtures, not a formatter bug. **In-scope corpus: 670 files.** Initial
      round1 format attempt hit **2 real bugs, both crashes on genuinely-valid
      files** (found via the forward pass itself failing, before syntax-check
      or content-preservation ever ran) — both fixed this session (see
      `real_code_regressions_70` above and the commit below) — Cargo's own
      `bracketBalance`/`splitTrailingComment` continuation-line logic had
      never been exercised against an interior per-element comment inside a
      multi-line array, and TOML's `"""`/`'''` multi-line basic/literal
      strings had no handling at all in the flat line-scanner (RDD_KEY_192's
      original implementation only tracked bracket balance for array/inline-
      table continuation, nothing for multi-line-string continuation).
      **After the fix, full re-run across all 670 in-scope files:** Round1
      format (`--preserve-tree --root`, one invocation): exit 0, 670/670
      processed, zero internal errors. Round2 vs round1: `diff -rq` empty
      across all 670 files — clean idempotency. Syntax-check of round1 output
      (`toml_sc.js`): 670/670 pass, matching the 670/670 in-scope baseline.
      **Content-preservation check** (`toml_content_diff.py`, written this
      session — see "Dogfood Output Validation" below for its design, since
      this system's Python 3.6 has no stdlib `tomllib`): all 670 files, exit
      0, zero mismatches (comment-blind by construction — only proves
      key/value/table/array structure, not comment wording — but this session
      hit no comment-corruption-shaped bug the way CSS's `real_code_
      regressions_69` did, so a comment-level checker wasn't additionally
      needed here). **Two bugs found+fixed** (both via the forward-pass
      crashing, not syntax-check or content-preservation):
      (1) `Cargo.toml`'s `exclude = [\n  "target/", # exclude bench
      testing\n]` — the continuation-line-joining loop only stripped a
      trailing `#` comment from the fully-assembled logical line at the very
      end, so an interior continuation line's own comment got treated as
      extending to the end of the whole joined string, swallowing the array's
      closing `]` as "comment text" and throwing "unterminated array". Fixed
      by stripping each continuation line's own trailing comment before
      joining it in. (2) `triagebot.toml`'s `message = """\...\n"""` — TOML
      v1.0's multi-line basic/literal strings were never handled by the flat
      line-scanner at all (no bracket to balance-track the way array/inline-
      table continuation uses), throwing "expected 'key = value' line".
      Fixed by detecting an unterminated `"""`/`'''` opener before the
      bracket-balance check and consuming subsequent raw (untrimmed) lines
      verbatim until the matching closing delimiter, preserving the string's
      real embedded newlines/whitespace exactly (same "opaque, preserve
      exactly" treatment as JSON5's multi-line string continuations). Fixture
      `test/real_code_regressions_70_{inp,out}.toml` combines both bugs in
      one file (`test/README.txt`). `make test`: 119/119 forward + 119/119
      idempotency, zero regressions. Commit `d56eb3a`.
      **Final numbers after the fix:** forward 670/670, idempotency 670/670,
      syntax-check 670/670, content-preservation 670/670.
      `python-poetry/poetry`, `pola-rs/polars`, `toml-lang/toml` remain
      not-started for TOML.
      **`kubernetes/kubernetes` (YAML, first YAML dogfood run; fresh shallow
      clone `--depth 1`, found already under the scratchpad from a prior
      session, reused). Excluding `.git`/`vendor`/`_output`/`bazel-*`/
      `build`, this monorepo has 6366 hand-authored `.yaml`/`.yml` files —
      far above the several-hundred sampling threshold, so a representative
      **455-file sample** (evenly spaced across the full sorted list, so it
      spans a broad cross-section of directories rather than one
      subdirectory) was used, not the full set. Baseline syntax-check of the
      unformatted sample originals (`yaml_sc.js`, `loadAll()`): 453/455 pass;
      the 2 failures (`test/integration/scheduler_perf/podgroup/tas/
      templates/podgroup.yaml`, `test/kubemark/resources/
      hollow-node_template.yaml`) are Go/Salt template files using `{{...}}`
      template syntax misidentified by their `.yaml` extension — confirmed
      genuinely invalid YAML via the same real-parser baseline-check
      precedent as `json5/json5`'s `test/invalid.json5`/`rust-lang/cargo`'s
      deliberately-invalid manifest fixtures, not a formatter bug.
      **In-scope corpus: 453 files.** Round1 format (one invocation,
      `--preserve-tree --root`) surfaced **6 real bugs, 5 found via the
      forward pass crashing on genuinely-valid files and 1 (idempotency-only)
      found re-formatting round1's own output** — all fixed this session
      (see `real_code_regressions_71` below and the two commits below).
      (1) A sequence-of-mapping's first key ("- key:") rejected a same-indent
      nested sequence child (the common `- apiGroups:\n    - "*"` manifest
      style) — a plain mapping key already allowed this, the
      sequence-of-mapping first-key path just hadn't been taught the same
      rule; fixed by mirroring it. (2)/(3) Both quoted and unquoted (plain)
      scalars can wrap across physical lines when a continuation is more
      indented than the key (common in real-world CRD/API `description`
      fields) — the line-based parser had zero support for this shape and
      crashed treating the continuation as its own malformed mapping line;
      fixed by detecting an unterminated quote / a non-key/non-seq deeper
      continuation line and capturing it as an opaque multi-line scalar
      body, applied to plain mapping keys, a sequence-of-mapping's first
      key, and (found only later, see bug 6) a plain sequence item's own
      value. (4) A trailing comment with no following sibling key inside a
      sequence-of-mapping's children (a "dangling" item with a null key)
      reached the colon-alignment padding helper and threw a
      NullPointerException calling `.length()` on the null key; fixed by
      excluding dangling items from the padding key list and rendering their
      leading comments directly. (5) Idempotency-only: the new multi-line-
      scalar continuation capture (bugs 2/3) and the pre-existing `|`/`>`
      block-scalar body capture both stored continuation lines at their
      original ABSOLUTE indentation; since the header key's own rendered
      column can shift (colon-alignment padding, indent-size, nesting-depth
      quirks elsewhere in the renderer), a second formatting pass could
      leave the body less-indented than its own re-rendered key line,
      breaking idempotency and, in one case, re-parseability. Fixed by
      storing every continuation/body line's indentation as a delta
      RELATIVE to its own key's original indent, and re-anchoring that delta
      to the key's newly-rendered column at render time. Fixture
      `test/real_code_regressions_71_{inp,out}.yaml` combines bugs 1-5 in
      one file (`test/README.txt`). `make test`: 120/120 forward + 120/120
      idempotency, zero regressions. Commit `fff5a3f`.
      **A final full re-run across all 453 in-scope files after bugs 1-5**
      caught a **6th bug via the content-preservation check** (not the
      syntax-checker — the corrupted output was still syntactically valid
      YAML): a `|`/`>` block scalar as a PLAIN (non-keyed) sequence item's
      own value (e.g. a shell script in a `command:` array element, a common
      real-world shape) was silently rendered as an empty string — the
      no-colon branch of the sequence-item parser never checked for a
      block-scalar header at all, unlike the mapping-key and
      sequence-of-mapping-first-key cases. Fixing it also surfaced a latent
      render-offset bug in the bug-5 relative-indent scheme: a block
      scalar's capture baseline is the dash line's own indent, not the
      value's column, so its render anchor must be the dash's own rendered
      column (not a "+2" offset, which is only correct for the
      quoted/plain-scalar case above, whose capture baseline is the value's
      own column) for both a plain sequence item and a sequence-of-mapping
      first key — only sibling keys (whose capture baseline already includes
      the +2) use the +2-offset anchor. Extended
      `test/real_code_regressions_71_{inp,out}.yaml` to also cover this
      shape. `make test`: 120/120 forward + 120/120 idempotency, zero
      regressions. Commit `025af9f`.
      **Final numbers after all 6 fixes, full 453-file re-run:** forward
      453/453 (matching the 453-file in-scope baseline exactly, the 2
      Go/Salt template files excluded per above), idempotency 453/453
      (`diff -rq` empty), syntax-check 453/453 pass, content-preservation
      (new `yaml_content_diff.py`, see "Dogfood Output Validation" above)
      453/453 match (informational-only comment-capitalization diffs noted
      on a handful of files, not structural mismatches).
      `docker/compose`, `ansible/ansible`, `actions/starter-workflows`
      remain not-started for YAML.
      **`python-poetry/poetry` (TOML, second TOML dogfood run; fresh shallow
      clone `--depth 1`, not found under `/tmp`/scratchpad from a prior
      session):** 106 `.toml` files found (no `.git`/`node_modules`/`build`/
      `dist`/`.venv` matches — a fresh shallow clone that's never been
      built), a modest count well under any sampling threshold, so the
      **full set was processed**, not a sample — mostly `pyproject.toml`
      (its own PEP 621 + `[tool.poetry.*]` root file, plus ~100 small
      test-fixture `pyproject.toml`s under `tests/**/fixtures/**` covering
      poetry's own dependency-resolution/build-system/git-dependency test
      matrix) with a handful of `poetry.toml`/JSON-source-derived `.toml`
      test fixtures (`tests/json/fixtures/**`). Baseline syntax-check of the
      unformatted originals (`toml_sc.js`): **106/106 pass** — unlike
      `rust-lang/cargo`'s corpus, this repo's test-fixture `.toml` files
      (e.g. `bad_scripts_project/{no_colon,too_many_colon}/pyproject.toml`,
      `invalid_pyproject/pyproject.toml`, `invalid_lock/pyproject.toml`)
      are deliberately invalid at the *poetry-schema* level (missing
      required keys, malformed script entries, mismatched lock hashes) but
      are all syntactically well-formed TOML, so none tripped `toml_sc.js` —
      no exclusions were needed. **In-scope corpus: 106 files, full set.**
      Round1 format (`--preserve-tree --root`, one invocation): exit 0,
      106/106 processed, zero internal errors/crashes (unlike the
      `rust-lang/cargo` run, this repo's dependency-version-constraint
      strings and nested inline-table dependency specs, e.g.
      `{ version = "^1.0", extras = ["foo"] }`, hit no unhandled shape).
      Round2 vs round1: `diff -rq` empty across all 106 files — clean
      idempotency. Syntax-check of round1 output (`toml_sc.js`): 106/106
      pass, matching the 106/106 baseline exactly. **Content-preservation
      check** (`toml_content_diff.py`, reused as-is per this session's
      instructions, no changes needed): all 106 files, exit 0, zero
      mismatches. **Zero bugs found** — forward 106/106, idempotency
      106/106, syntax-check 106/106, content-preservation 106/106. No new
      fixtures needed (nothing to regress-test).
      `pola-rs/polars`, `toml-lang/toml` remain not-started for TOML.
