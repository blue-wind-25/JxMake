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
| RDD_KEY_198 | HTML5 `<ruby>` implied-end-tag support: small, extensible `XmlSpecificRule.OPAQUE_IMPLIED_END_TAG_ELEMENTS` name set (currently just `ruby`) reusing the existing `<script>`/`<style>`/`<pre>` opaque-verbatim-span pattern -- scans from the element's own opening `<` to its own MATCHING `</tag>` (nested same-name depth tracked) and captures the whole span (incl. `<rb>`/`<rt>`/`<rp>`/`<rtc>` children and the outer tags themselves) byte-for-byte verbatim as a new `NodeType.OPAQUE` node; no per-element implied-closing-trigger logic built. Fixture `test/real_code_regressions_104_{inp,out}.html`; `make test` 153/153, zero regressions. |
| RDD_KEY_199 | HTML5 unquoted attribute value support: `XmlSpecificRule.parseAttr`'s `lang.isHtml5` branch now accepts an unquoted value per the HTML5 spec grammar (no whitespace/`"`/`'`/`=`/`<`/`>`/backtick) instead of requiring `"`/`'`; preserved unquoted on output, no forced normalization to double-quoted (consistent with the codebase's existing "preserve as written" quote-style posture elsewhere); plain XML unchanged, still requires quotes. Fixture `test/real_code_regressions_106_{inp,out}.html`; `make test` 155/155, zero regressions. Unblocked the `alexandersandberg/html5-elements-tester` dogfood run past line 718, but it now hits a distinct, unrelated blocker at line 759 (bare `<option>` tags relying on HTML5's implied-end-tag rule, not yet in `OPAQUE_IMPLIED_END_TAG_ELEMENTS`) -- see HTML5 checklist entry. |
| RDD_KEY_200 | HTML5 `<option>` implied-closing-trigger support: new, general, reusable `XmlSpecificRule.IMPLIED_CLOSE_TRIGGERS` (`Map<String, Set<String>>`, element name -> sibling start-tag names that implicitly close it), distinct from `OPAQUE_IMPLIED_END_TAG_ELEMENTS` -- a registered element is still parsed as a REAL node (attributes/children/normal rendering), only the "when do children stop" decision changes; only `parseNodes`/`parseElement` were touched, no per-element control-flow. Registered only `option` -> `{option, optgroup}` today. `parseNodes` gained an optional trigger-set parameter that also breaks its loop on an upcoming (non-closing) start tag matching the set; `parseElement` still consumes an explicit `</tag>` when present (regression-safe), otherwise treats the element as implicitly closed with no explicit tag consumed when a trigger set is registered (covering both the sibling-trigger case and the pre-existing parent-close-via-`stopAtCloseTag` case, reused rather than reinvented) -- otherwise the pre-existing hard `XmlParseException` is unchanged. Fixture `test/real_code_regressions_108_{inp,out}.html` (explicit-close regression guard + `<datalist>`/`<optgroup>` implied-close cases). `make test` 157/157, zero regressions. This was the `alexandersandberg/html5-elements-tester` dogfood run's third and final blocker -- the full 42KB file now completes end-to-end (forward pass, round2, idempotency diff, `html_sc.js` syntax-check, `html_content_diff.py` content-preservation all clean); dogfood run for this candidate is DONE. |

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
- **HTML5:** `h5bp/html5-boilerplate` (done — see Checklist). `twbs/bootstrap`
  (docs site), `mdn/content`, `whatwg/html`, and `kangax/html-minifier` were
  all investigated and dropped — see Open Questions resolution below; none
  have a real committed HTML5 corpus (content is Astro/MDX, Markdown, a
  giant non-HTML preprocessed spec source, and JS unit tests with inline
  HTML strings, respectively). Three replacement candidates added (user,
  2026-07-24), verified via `gh api` to actually contain real `.html`:
  `web-platform-tests/wpt` (confirmed 6,552 real `.html` files via code
  search — strongest candidate, but the full repo is ~2.6GB/"TOO MASSIVE",
  so scope to one targeted subtree, e.g. `html/` or a specific test-suite
  dir, same as the `llvm-project`/`gcc-mirror` C/C++ candidates' partial-run
  precedent); `WordPress/wordpress-develop` (263 real `.html` hits, but
  mostly thin Gutenberg block-theme templates dominated by
  `<!-- wp:... {json} -->` comment shorthand rather than dense markup —
  `src/readme.html` is a genuine standalone page; usable as a light
  supplement, not a flagship run); `alexandersandberg/html5-elements-tester`
  (a single 42KB `index.html` exercising many HTML5 elements — good
  breadth-of-tag smoke test, but one file, not a corpus; treat as a quick
  spot-check, not a full dogfood session).
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

- **`twbs/bootstrap` docs-site HTML dogfood candidate no longer exists in the
  current repo — how to proceed?** Investigated a fresh-enough checkout
  (`main`, shallow clone reused from the prior CSS dogfood session, HEAD at a
  dependabot commit dated 2026-07-22) looking for the docs-site HTML this
  candidate was picked for (many inline `<script>`/`<style>` blocks alongside
  component markup, to exercise `XmlSpecificRule.renderScriptOrStyle`). Found
  **zero** `.html` files anywhere under `site/` — the docs site has migrated
  to Astro + MDX (`site/src/content/docs/**/*.mdx`, `site/src/pages/**/*.astro`);
  there is no committed HTML source for the docs pages at all in this
  snapshot (they're presumably generated at build time, not checked in). A
  full-tree search found only 14 `.html` files total in the entire repo, all
  under `js/tests/visual/**` and `js/tests/integration/index.html` — JS
  component test pages, not docs-site content, and out of the task's declared
  scope ("do not touch compiled JS/CSS or component source unless incidentally
  alongside the docs HTML"). Options not decided: (a) substitute the
  `js/tests/visual`/`integration` HTML corpus anyway despite it not being
  "docs site" HTML, (b) mark `twbs/bootstrap` (docs site) as not-applicable/
  skipped in the HTML5 Test-Fixture Repos list and rely on the other three
  (`mdn/content`, `whatwg/html`, already-done `h5bp/html5-boilerplate`), (c)
  try an older commit/release tag of `twbs/bootstrap` from before the Astro
  migration (last version with Jekyll-rendered docs HTML actually committed)
  to get a real docs-site HTML corpus. No changes made to
  `XmlSpecificRule.java` or any fixture pending this decision.

  **Resolved (user, 2026-07-24): option (b).** `twbs/bootstrap` (docs site)
  dropped from the HTML5 Test-Fixture Repos list — its docs site no longer
  ships committed HTML (Astro/MDX now), so there's no real docs-HTML corpus
  left to dogfood against, and substituting its unrelated JS component test
  pages (option a) wasn't worth diluting the point of the candidate.

- **`mdn/content` and `whatwg/html` also turned out to have no real HTML5
  corpus — both dropped (user, 2026-07-24).** Verified via `gh api`:
  `mdn/content` is entirely Markdown (`files/en-us/**/*.md` with frontmatter,
  no `.html` anywhere in the docs tree — content is rendered to HTML only at
  publish time, not checked in). `whatwg/html` is a single 7.9MB `source`
  file with no `.html` extension, written in a custom Nim-preprocessed macro
  syntax (not valid standalone HTML5) — the repo's only real `.html` is a
  single tiny `404.html` plus two small `demos/canvas`/`demos/workers` dirs,
  not enough for a meaningful corpus. A third candidate suggested in the same
  session, `kangax/html-minifier` (its `tests/` dir, hoped to contain many
  real hand-authored edge-case `.html` fixture files), was also checked and
  is actually all `.js` unit tests with inline HTML template strings, not
  real `.html` files — also dropped. **HTML5 Test-Fixture Repos list is now
  down to just `h5bp/html5-boilerplate` (done); no replacement candidates
  queued.** Next session (or the user) needs to find a real hand-authored or
  build-committed static HTML5 site corpus before HTML5 dogfooding can
  continue past what's already done — repos that look promising by name
  (docs sites, spec repos, minifier test suites) have repeatedly turned out
  to store their HTML as Markdown/MDX/custom-macro-source/JS-string-literals
  instead of real `.html` files; verify actual file contents via `gh api`
  before proposing a candidate, not just repo purpose/reputation.

---

- **WordPress magic-comment capitalization — should HTML/XML comment-start
  normalization skip "directive-shaped" comments the way CSS's
  `isSingleTokenDirective` already does, and if so, what shape qualifies?**
  Found during the `WordPress/wordpress-develop` HTML5 dogfood session
  (2026-07-24): `normalize-comment-start-case=on`'s `normComment` in
  `XmlSpecificRule.java` unconditionally capitalizes any lowercase-starting
  comment, with no directive-shape exclusion at all (unlike
  `FormatterSimpleBraced`'s CSS-specific `isSingleTokenDirective`, added for
  the `twbs/bootstrap` rtlcss-comment bug, `real_code_regressions_69`). Two
  WordPress fixtures (`tests/phpunit/data/blocks/fixtures/core__more.server
  .html`, `core__nextpage.server.html`, plus `do-blocks-expected.html`) use
  `<!--more-->` and `<!--nextpage-->` — WordPress core's own literal,
  case-sensitive(-ish) content-splitting magic comments (`get_extended()`,
  block "next page" splitting), not prose — which the formatter silently
  rewrites to `<!--More-->`/`<!--Nextpage-->`. Unlike the CSS precedent, a
  straight reuse of `isSingleTokenDirective`'s exact rule (single
  whitespace-free token containing `:` or `-`) would NOT catch this case —
  `more`/`nextpage` contain neither character, so the existing "directive
  shape" heuristic doesn't generalize as-is. Whether HTML/XML should adopt a
  broader "any single lowercase word with no interior whitespace, regardless
  of `:`/`-` content" exclusion (risking under-capitalizing genuine short
  one-word prose comments like `<!--fixme-->` or `<!--todo-->`, which
  arguably *should* still get capitalized) or some other rule
  (allow-list of known magic-comment strings? scope the exclusion to HTML5
  only, not XML?) is a real design tradeoff not specified anywhere in
  `STYLE_DATA_FORMATS.md` §4. No code change made to `normComment` pending
  this decision — leaving the current blanket-capitalization behavior as-is
  (same posture as CSS had before its own precedent bug was found). Not
  blocking: the HTML5 `WordPress/wordpress-develop` dogfood run otherwise
  completed and is recorded as done in the Checklist below; this is a
  content-correctness caveat on that run's comment-normalization output
  only, does not affect structural output, and does not block moving on to
  other HTML5 candidates.

- **HTML5 optional/implied end tags -- RESOLVED (RDD_KEY_198, user,
  2026-07-24).** Rather than the full per-element-family spec feature, a
  small, extensible `XmlSpecificRule.OPAQUE_IMPLIED_END_TAG_ELEMENTS` name
  set (currently just `ruby`) reuses the existing `<script>`/`<style>`/
  `<pre>` opaque-verbatim-span pattern: the whole element, from its own
  opening `<` through its own MATCHING `</tag>` (nested same-name
  opens/closes tracked), is captured byte-for-byte verbatim as a new
  `NodeType.OPAQUE` node -- no interior parsing, so `<rb>`/`<rt>`/`<rp>`/
  `<rtc>` children are never touched. Extending to other implied-end-tag
  families (`<li>`/`<p>`/`<td>`/`<tr>`/`<option>`/etc.) later is adding a
  name to the set, not new logic. Full narrative in `RDD_LOG.md`
  `RDD_KEY_198`. This unblocked the specific `XmlParseException` the
  `alexandersandberg/html5-elements-tester` dogfood spot-check found, but a
  separate, unrelated pre-existing bug (`parseAttr` requires a quoted
  attribute value; the same file has an unquoted `size=5` on a `<select>`,
  line 718) still blocks that file's full end-to-end dogfood run -- see its
  Checklist entry below (still marked partial, now blocked on the
  unquoted-attribute-value gap instead).

- **HTML5 unquoted attribute values -- RESOLVED (RDD_KEY_199).** `parseAttr`'s
  `lang.isHtml5` branch now accepts an unquoted attribute value per the HTML5
  spec grammar and preserves it unquoted on output (no forced double-quote
  normalization -- `STYLE_DATA_FORMATS.md` says nothing about attribute
  quote-style normalization, and the codebase's existing quoted-value
  behavior already preserves whichever quote character was written verbatim,
  so unquoted-stays-unquoted follows the same "preserve as written" posture,
  not a new convention). Plain XML unchanged (still requires quotes -- no
  such grammar exists in the XML spec). Fixture
  `test/real_code_regressions_106_{inp,out}.html`; `make test` 155/155, zero
  regressions. Unblocked the `alexandersandberg/html5-elements-tester`
  forward pass past line 718's `<select size=5>`, but it now hits a new,
  distinct, unrelated blocker at line 759: a `<datalist>` containing bare
  `<option value="...">` tags with no closing `</option>` at all, relying on
  HTML5's implied-end-tag rule for `<option>` (closed by the next sibling
  `<option>` or the parent's close) -- the same open-ended
  implied-end-tag-family gap RDD_KEY_198 named but deliberately did not build
  out beyond `ruby`. Not fixed (out of scope for this fix); see the HTML5
  checklist entry below for current status.

  **RESOLVED (RDD_KEY_200, user, 2026-07-24).** Rather than extending
  `OPAQUE_IMPLIED_END_TAG_ELEMENTS` (which would make `<option>` fully opaque
  and stop reformatting it even in its common explicitly-closed case) or
  building the full per-element-family spec feature, a small, general,
  reusable `XmlSpecificRule.IMPLIED_CLOSE_TRIGGERS` table was added and
  populated with only `option` -> `{option, optgroup}`. `<option>` continues
  to be parsed as a real node in both the explicitly-closed and implied-close
  cases. This was the `alexandersandberg/html5-elements-tester` file's third
  and final blocker -- the full file now completes end-to-end. See the
  Resolved Design Decisions table and HTML5 checklist entry below.

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
      Status: JSON/JSON5 complete (4/4 repos); CSS complete (4/4 repos); **XML
      4/4 repos done, DONE** (`apache/maven`, `w3c/svgwg`, `apache/ant`,
      `jenkinsci/jenkins` all done — XML Test-Fixture Repos list fully
      complete); **YAML: all 4 originally-planned repos DONE, plus both
      later-added repos, list now fully complete** (`kubernetes/kubernetes`,
      `docker/compose`, `ansible/ansible`, `actions/starter-workflows`,
      `prometheus/prometheus`, `home-assistant/core` all done — see per-repo
      entries below); **TOML 4/4, DONE** (`rust-lang/cargo`,
      `python-poetry/poetry`, `pola-rs/polars`, `toml-lang/toml` all done —
      TOML Test-Fixture Repos list now fully complete); HTML5
      2-of-4-candidate-list, one candidate blocked
      (`h5bp/html5-boilerplate` done; `twbs/bootstrap` docs site,
      `mdn/content`, `whatwg/html`, `kangax/html-minifier` all investigated
      and dropped — no real HTML5 corpus in any, see Open Questions
      resolution; of the three added replacements, `WordPress/wordpress-
      develop` done (1 bug found+fixed, 1 open question raised — see below),
      `alexandersandberg/html5-elements-tester` DONE (its
      `<ruby>`/implied-end-tag `XmlParseException` fixed per RDD_KEY_198, its
      unquoted-attribute-value `XmlParseException` fixed per RDD_KEY_199, and
      its third, distinct blocker — a `<datalist>` at line 759 containing
      bare `<option value="...">` tags relying on HTML5's implied-end-tag
      rule for `<option>` — fixed per RDD_KEY_200's new
      `IMPLIED_CLOSE_TRIGGERS` mechanism; the full 42KB file now completes
      end-to-end: forward pass, round2, idempotency diff, `html_sc.js`
      syntax-check, and `html_content_diff.py` content-preservation all
      clean); `web-platform-tests/
      wpt` not started). Overall item stays unchecked pending only
      `web-platform-tests/wpt` (HTML5) now that XML is fully done.

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
        hand-authored `.css` files (`dist/css/**` excluded as generated).
        Forward/idempotency/syntax-check all 31/31 clean. **1 bug
        found+fixed** (via manual comment inspection, not the comment-
        stripping token-diff): `carousel.css`'s rtlcss directive comments
        (`/* rtl:begin:ignore */` etc.) were silently capitalized to
        `/* Rtl:... */` by `FormatterSimpleBraced.capitalizeCommentStart` —
        still valid CSS so `css_sc.js` missed it, but breaks any pipeline
        running rtlcss over the output. Fixed by adding
        `isSingleTokenDirective`: skip capitalization when a comment's
        first-line body is one whitespace-free token containing `:` or `-`
        (directive-shaped), ordinary prose still capitalized. Fixture
        `test/real_code_regressions_69_{inp,out}.css`. `make test`: 118/118
        forward + idempotency. Commit `8f5f597`. Final full re-run: 31/31
        clean on all four checks.
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
      - `microsoft/vscode`: 1377 files found, full set processed. 5
        genuinely-empty files correctly throw `JsonParseException`. 100 JSONC
        files excluded as out-of-scope (already fail baseline `JSON.parse`).
        **In-scope corpus: 1272 files.** **1 idempotency bug found+fixed**
        (via `diff -rq round1 round2`): `JsonSpecificRule.parseContainer`
        kept a dangling placeholder `Item` for a comment-less blank line
        before a closing brace, defeating the tight-`{}` short-circuit on
        round1 but not round2 — non-idempotent
        (`extensions/vscode-api-tests/testWorkspace/bower.json`, source
        `"{\n\n\t\n}\n"`). Fixed by only keeping the placeholder when a real
        leading comment exists. Fixture
        `test/real_code_regressions_68_{inp,out}.json`. `make test`: 117/117.
        Commit `e2a6f0e`. After fix: full 1377-file re-run clean idempotency;
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

      **XML (4/4 repos done, DONE):**
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
      - `w3c/svgwg`: 298 files found (22 `.xml` + 276 `.svg`, spot-checked as
        real XML), full set processed (1 vendored `node_modules` fixture
        excluded). Baseline `xml_sc.js`: 294/298 pass — 4 excluded (1
        illustrative two-root-element snippet, 3 BOM-prefixed `.svg`, same
        precedent as `eslint/eslint`'s BOM fixtures). **In-scope corpus: 294
        files.** **1 bug found+fixed**, forward pass erroring outright:
        `Lang.infer` never mapped `.svg` to `xml` at all, so all 276 `.svg`
        files failed with "could not infer language" — a missing extension
        mapping, not a parser bug. Fixed by adding `.svg` alongside `.xml` in
        `Lang.infer`. Fixture `test/real_code_regressions_74_{inp,out}.svg`.
        `make test`: 123/123. Commit `3408acd`. Final 298-file re-run:
        forward/idempotency 298/298 clean; syntax-check matches baseline (4
        pre-existing failures, no new); content-preservation
        (`xml_content_diff.py`) 22/294 flagged, all 59 mismatches confirmed
        case-insensitive-equal comment-capitalization diffs (expected
        `normalize-comment-start-case=on` behavior) — zero structural
        mismatches. Namespaces, nested `<g>` groups, path-data attributes,
        CDATA (18 files) all exercised with no corruption. `apache/ant`,
        `jenkinsci/jenkins` remain not-started.
      - `apache/ant`: 558 files found (517 `.xml` + 2 `.xsd` + 39 `.xsl`),
        sampled every 3rd `.xml` (173) plus all `.xsd`/`.xsl` (214 total, no
        exclusion dirs — this is Ant's own source tree). Baseline `xml_sc.js`:
        207/214 pass — 7 excluded, all Ant's own deliberately-invalid
        `src/etc/testcases/**` fixtures. **In-scope corpus: 214 files** (the
        7 excluded still forward/idempotency-checked, just not
        syntax-checked). **1 bug found+fixed:** `Lang.infer` never mapped
        `.xsd`/`.xsl` to `xml` at all (same gap shape as `w3c/svgwg`'s `.svg`
        bug) — all 41 `.xsl`/`.xsd` files in the sample failed with "could
        not infer language". Fixed by adding `.xsd`/`.xsl` alongside
        `.xml`/`.svg` in `Lang.infer`. Fixtures
        `test/real_code_regressions_91_{inp,out}.xsl`,
        `test/real_code_regressions_92_{inp,out}.xsd`. `make test`: 141/141
        forward + idempotency. Commit `25bd5b8`. Full 214-file re-run after
        fix: forward/idempotency 214/214 clean; syntax-check matches baseline
        (same 7 pre-existing failures); content-preservation
        (`xml_content_diff.py`) flagged 66/214 files, 484 mismatches, all
        confirmed case-insensitive-equal comment-capitalization diffs (same
        expected behavior as `apache/maven`/`w3c/svgwg`) — zero structural
        mismatches. Ant's `build.xml`-family (`<macrodef>`/`<target>`/
        `<condition>`/nested-task), XSLT report stylesheets
        (`junit-frames.xsl` etc., deeply nested `<xsl:template>`/
        `<xsl:for-each>`/HTML-in-XSLT), and the 2 `.xsd` schema files all
        exercised with no corruption beyond expected comment normalization.
      - `jenkinsci/jenkins`: 131 files found (below the "several hundred+"
        sampling threshold, full set processed: 9 `pom.xml`, 58 `config.xml`
        test fixtures, 8 `build.xml`, plus assorted other `.xml` across
        `core`, `test`, `war`, `cli`, `bom`, `websocket`, `.idea`, `.mvn`,
        `src`). Baseline `xml_sc.js`: 130/131 pass — 1 excluded,
        `core/src/test/resources/hudson/util/
        Digester2Security2147TestData.xml`, a deliberately-crafted XXE/
        entity-expansion security-test fixture with no root element
        (expected pre-existing invalid, same posture as prior repos'
        deliberately-invalid fixtures). **In-scope corpus: 130 files.**
        Forward 130/130, idempotency (`diff -rq round1 round2`) 130/130
        clean, syntax-check of round1 output 130/130 matching baseline
        exactly (no new failures). Content-preservation
        (`xml_content_diff.py`): 14/130 flagged — 11 files with 72 total
        mismatches, all confirmed case-insensitive-equal
        comment-capitalization diffs (expected `normalize-comment-start-
        case=on` behavior, same precedent as `apache/maven`/`w3c/svgwg`/
        `apache/ant`), zero structural/attribute-order/text/CDATA
        mismatches; plus 3 files (`config_1_0_with_special_chars.xml`,
        `config_1_1_with_special_chars.xml`,
        `XMLFileTest/silentlyMigrateConfigsTest/config.xml`) where
        `xml_content_diff.py` itself can't parse the original with stdlib
        `xml.dom.minidom` (embedded control characters lenient `xmldom`
        tolerates but `minidom` rejects) — a content-diff tool limitation,
        not a formatter bug; all three still passed `xml_sc.js` baseline/
        round1 and the forward/idempotency checks cleanly. **Zero bugs
        found.** Maven POM dependency-management blocks, Ant-style
        `build.xml`, XStream test-fixture data, Windows-service descriptor
        XML, and checkstyle config all exercised with no corruption beyond
        expected comment normalization. XML Test-Fixture Repos list now
        fully complete (4/4: `apache/maven`, `w3c/svgwg`, `apache/ant`,
        `jenkinsci/jenkins` all done).

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
        the forward pass crashing, 1 idempotency-only, 1 via content-
        preservation on a final re-run):
        (1) sequence-of-mapping's first key rejected a same-indent nested
        sequence child (`- apiGroups:\n    - "*"`), though plain mapping keys
        already allowed it; fixed by mirroring the rule.
        (2)/(3) quoted/unquoted scalars wrapping across physical lines with a
        more-indented continuation (common in CRD `description` fields)
        crashed the parser; fixed by capturing an unterminated quote or
        deeper non-key/non-seq continuation as an opaque multi-line scalar
        body.
        (4) a dangling comment-only item (null key) in a sequence-of-
        mapping's children threw an NPE in the colon-alignment padding
        helper; fixed by excluding dangling items from the padding key list.
        (5, idempotency-only) multi-line-scalar/block-scalar (`|`/`>`)
        continuation bodies were stored at ABSOLUTE indentation, so a second
        pass could under-indent the body relative to its re-rendered key
        line; fixed by storing indent as a delta RELATIVE to the key's
        original indent, re-anchored at render time.
        Fixture (bugs 1-5) `test/real_code_regressions_71_{inp,out}.yaml`.
        `make test`: 120/120. Commit `fff5a3f`.
        (6, found via a final 453-file content-preservation re-run — output
        stayed valid YAML): a `|`/`>` block scalar as a plain sequence item's
        own value rendered as an empty string (no-colon branch never checked
        for a block-scalar header). Fixing it surfaced a latent bug-5
        render-offset error: a block scalar's anchor must be the dash's own
        rendered column, not the "+2" offset that's only correct for
        quoted/plain scalars — only sibling keys use +2. Extended the same
        fixture. `make test`: 120/120. Commit `025af9f`. **Final numbers
        after all 6 fixes, full 453-file re-run:** forward/idempotency/
        syntax-check/content-preservation (`yaml_content_diff.py`) all
        453/453 clean (informational-only comment-capitalization diffs, not
        structural).
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
      - `ansible/ansible`: 2110 `.yaml`/`.yml` files found, sampled every 5th
        (422 files). Baseline `yaml_sc.js`: 7 fail (`!vault`/`!unsafe`
        custom scalar tags) — **in-scope corpus: 415 files.** Forward/
        idempotency 422/422 clean; syntax-check same 7 baseline failures.
        **3 bugs found, all via content-preservation** (output stayed valid
        YAML, syntax-check alone missed all three).
        (1) A plain (non-keyed) sequence item's unquoted scalar wrapping
        across physical lines (common in changelog-fragment prose) had no
        continuation handling, silently dropping lines past the first —
        unlike the keyed/seqOfMapping-firstKey cases, which already handled
        this shape. Fixed by adding the same multi-line-plain-scalar capture
        to that branch.
        (2) A comment dedented below its enclosing block's indent (e.g. a
        `# FIXME:` at column 0 between deeper sibling keys) made `parseBlock`
        break out of every enclosing block without consuming it, permanently
        orphaning it and dropping everything that followed at every level.
        Fixed by looking past the comment to the next real content line and
        attaching it to whichever block that line actually belongs to.
        (3) A bare top-level plain-scalar document (e.g. an
        `$ANSIBLE_VAULT;...` header blob spanning several opaque hex lines)
        kept only its first line — the bare-scalar-document detection never
        accounted for further wrapped lines. Fixed by emitting remaining raw
        lines verbatim once that shape is detected.
        Fixture (all 3) `test/real_code_regressions_73_{inp,out}.yaml`.
        `make test`: 122/122. Commit `9f2a80a`. **Final numbers after fixes,
        full 422-file re-run:** forward/idempotency/syntax-check clean;
        content-preservation 415/415 clean. `actions/starter-workflows`
        remains not-started for YAML.
      - `actions/starter-workflows`: 188 `.yaml`/`.yml` files, full set
        processed. Baseline `yaml_sc.js`: 2 fail (`code-scanning/
        nowsecure*.yml`, both an unquoted `group_id: {{ groupId }}`
        Handlebars placeholder js-yaml can't parse as a flow-mapping key —
        pre-existing, unrelated to the corpus's normal quoted `${{ ... }}`
        GitHub Actions syntax). **In-scope corpus: 186 files.**
        Forward/idempotency 188/188 clean; syntax-check same 2 baseline
        failures. **1 bug found+fixed, via syntax-check on round1 output**
        (the corrupted output failed to parse outright, unlike every bug in
        the three prior YAML sessions, which were content-preservation-only):
        `code-scanning/codescan.yml` uses `-   name: foo` (extra spaces after
        the dash) for its `steps:` items. `parseSeqItem` computed the
        sibling-key column as a hardcoded `ln.indent + 2` — correct only for
        exactly one space after the dash — so with extra padding the next
        sibling key (`uses:`) was misidentified as a nested child, producing
        invalid "bad indentation" output. Fixed by deriving the real
        first-key column from the dash line's actual leading whitespace and
        using it consistently for the sibling/nested-child decision and every
        multi-line-scalar continuation anchor. Fixture
        `test/real_code_regressions_75_{inp,out}.yaml`. `make test`:
        124/124. Commit `f1648c5`. **Final numbers after fix, full 188-file
        re-run:** forward/idempotency clean; syntax-check same 2 baseline
        failures; content-preservation (`yaml_content_diff.py`) 179/186
        clean, remaining 7 are the known PyYAML emoji-in-comment tool gap
        (first flagged in the `docker/compose` session) — `yaml_sc.js`/
        js-yaml parses all 7 fine on both copies; not a formatter bug.
        **This completes all 4 originally-planned YAML Test-Fixture Repos**
        (`kubernetes/kubernetes`, `docker/compose`, `ansible/ansible`,
        `actions/starter-workflows`); `prometheus/prometheus`/
        `home-assistant/core` were added later and remain not-started at this
        point.

      **`prometheus/prometheus` (added-repo session, done):**
      - Fresh shallow clone. 380 `.yml`/`.yaml` files found, full set
        processed. Baseline `yaml_sc.js`: 5 fail, all `config/testdata/
        *.bad.yml` — deliberately-invalid fixtures for Prometheus's own
        config-parser error-path tests (unknown `!!binary` tags, a duplicated
        mapping key), confirmed via `yaml_sc.js`'s actual failures not
        filename pattern-matching (152 files match `*bad.yml` by name but
        most are schema-invalid, not syntax-invalid). **In-scope corpus: 375
        files.** **4 bugs found+fixed**, all data loss, all only catchable
        via `yaml_content_diff.py` content-preservation, all in
        `YamlSpecificRule`'s sequence/mapping parsing, sharing the theme of a
        dash/key line whose "value" is absent/comment-only/anchor-only/an
        unbalanced multi-line flow opener with the real content on
        subsequent more-indented lines:
        (1) `parseKeyItem`'s flow-looking-inline-value early return didn't
        check the flow was actually closed on the same physical line,
        truncating everything after an unbalanced multi-line `[...]` opener
        (`documentation/examples/prometheus-kubernetes.yml` — 300→52 lines
        lost).
        (2) `parseSeqItem`'s `seqOfMapping` first-key handling had the same
        flow-closure gap, and separately dropped everything after a
        `- # comment`-only dash line whose real mapping keys start on
        subsequent lines.
        (3) a dash line holding only an anchor tag (`- &highalert`) followed
        by a nested mapping at an indent equal to (not just greater than) the
        dash's own key column lost its child block entirely
        (`model/rulefmt/testdata/test_aliases.yaml`).
        (4) `renderFlowValue` rendered an empty flow map/seq (`{}`/`[]`) as a
        block conversion whenever its line didn't `fits()`, but
        `renderFlowBlock` has nothing to iterate for zero entries, silently
        vanishing the value — found in `web/ui/pnpm-lock.yaml`'s dependency
        snapshot maps (most visibly when the dropped key was the file's last
        line).
        All 4 combined into `test/real_code_regressions_83_{inp,out}.yaml`.
        `make test`: 132/132 (133/133 after a concurrent session's unrelated
        fixture 84 landed alongside). **Final numbers after fixes, full
        380-file re-run:** forward/idempotency 380/380 clean; syntax-check
        same 5 baseline failures; content-preservation 375/375 clean. **This
        completes `prometheus/prometheus`**; `home-assistant/core` remains
        not-started.

      **`home-assistant/core` (added-repo session, done — completes the YAML
      Test-Fixture Repos list):**
      - Fresh shallow clone. 921 `.yaml`/`.yml` files found (excluding
        `node_modules/`), full set processed. Baseline `yaml_sc.js`: 43 fail
        — Home Assistant's own custom-tag/templating idioms
        (`!include`/`!secret`/`!env_var`, `{{ ... }}` Jinja2 blueprint
        placeholders as bare scalars), same exclusion class as
        `ansible/ansible`'s `!vault`/`!unsafe`, confirmed via `yaml_sc.js`'s
        actual failures. **In-scope corpus: 878 files.** Forward pass:
        921/921 processed, zero crashes. **1 bug found+fixed, via
        content-preservation** (output stayed valid YAML): a sequence item
        whose own value is itself another sequence, in compact single-line
        form `- - a\n  - b` (this repo's `services.yaml` uses it for
        mutually-exclusive `supported_features` pairs). `parseSeqItem` never
        recognized this shape — the inner `- ` was captured as a literal
        scalar, leaving the sibling nested-seq item on the next line
        unconsumed; its indent then didn't match the enclosing block, so
        `parseBlock` broke out early, silently dropping the rest of the
        nested sequence AND every later sibling item/key at every level (6
        files affected: `cover`/`media_player`/`overkiz`/`siren`/`valve`/
        `wmspro` `services.yaml`). Fixed by detecting the nested-dash shape
        up front via a new `parseInlineNestedSeq` helper, rendered through
        the existing generic `item.children` path, which non-lossily expands
        the compact `- -` form — same "prefer an unambiguous expanded form"
        precedent as this formatter's flow-to-block conversion elsewhere.
        Fixture `test/real_code_regressions_86_{inp,out}.yaml`. `make test`:
        135/135 forward + idempotency. Commit `e7f0334`. **Final numbers
        after fix, full 921-file re-run:** forward/idempotency 921/921
        clean; syntax-check same 43 baseline failures; content-preservation
        870/878 clean — remaining 8 are the same PyYAML emoji-in-comment
        tool gap first flagged in `docker/compose` (`yaml_sc.js`/js-yaml
        parses all 8 fine on both copies; not a formatter bug). **This
        completes `home-assistant/core`**, and with it all 6 planned YAML
        Test-Fixture Repos — the list is now fully exhausted.

      **HTML5 (2 repos done / 1 partial, first two follow-up HTML5 dogfood
      runs against the replacement candidates added 2026-07-24):**
      - `WordPress/wordpress-develop`: scoped to real markup only, per the
        candidate note's own caveat -- 303 total `.html` files found, 73
        contain no `wp:...` Gutenberg block-comment shorthand (the rest are
        thin JSON-in-comments block templates, out of scope). **In-scope
        corpus: 73 files** (`src/readme.html`, `tests/qunit/index.html` (a
        109KB real admin-UI test harness page with dozens of
        `<script type="text/html">` Underscore-template blocks), and 71
        server-rendered Gutenberg block HTML fixtures under
        `tests/phpunit/data/blocks/fixtures/**`). Forward 73/73 clean, zero
        crashes. Idempotency (`diff -rq round1 round2`) 73/73 clean.
        Baseline `html_sc.js`: 71/73 fail `missing-doctype` (the block
        fixtures are intentionally bare HTML fragments, not full documents --
        pre-existing, unrelated to the formatter); round1 output matches the
        same 71 baseline failures exactly, no new ones. **1 bug found+fixed**,
        via `html_content_diff.py` content-preservation on
        `tests/qunit/index.html` (not syntax-check -- the corrupted output
        stayed syntactically valid HTML5): `renderElement`'s multi-child
        block-closing render path (used whenever an element's children don't
        collapse to a single inline text/CDATA child, e.g. a `<div>`
        containing element children) never emitted `n.trailingComment`,
        unlike the other three render branches (self-closing, sole-text-
        child, empty-children), which all route through `appendWithTrailing`.
        A same-line trailing comment right after such a block element's
        closing tag (`</div><!-- end widget templates -->`,
        `</div><!-- end nav menu templates -->`) was silently dropped --
        real content loss, not cosmetic. Fixed by routing the multi-child
        closing-tag line through `appendWithTrailing` too. Fixture
        `test/real_code_regressions_103_{inp,out}.html`. `make test`:
        152/152 forward + idempotency. Final full 73-file re-run after the
        fix: forward/idempotency/syntax-check all clean as above;
        content-preservation (`html_content_diff.py`) 73/73 clean. **2
        further comment-text-mismatch diffs found on this run are a genuine
        open design question, not fixed** -- see Open Questions
        ("WordPress magic-comment capitalization" below); no code change
        made for those pending resolution. `web-platform-tests/wpt` not
        started.
      - `alexandersandberg/html5-elements-tester`: single 42KB `index.html`
        file, attempted as a one-file spot-check (not a corpus, per the
        candidate note). **DONE** (as of RDD_KEY_200 -- three sequential
        blockers found and fixed across three sessions, see below). Original
        state at this point in the narrative: forward pass failed outright
        (round1 never completed, no output produced):
        `XmlSpecificRule.parseElement` has no support at all for HTML5's
        optional/implied end tags, and this file deliberately exercises
        them (`<ruby><rb>旧<rb>金<rb>山<rt>jiù<rt>jīn<rt>shān<rtc>San
        Francisco</ruby>` at line 379 -- `<rb>`/`<rt>`/`<rtc>` never get
        explicit closing tags in valid ruby markup, they implicitly close at
        the next sibling or `</ruby>`). Given the size/scope-decision nature
        of implementing implied-end-tag support (a real HTML5 tree-
        construction feature covering many more elements than just the ruby
        family, not a narrow fix, and unspecified in `STYLE_DATA_FORMATS.md`
        §4), this is recorded as an open design question rather than guessed
        at -- see Open Questions ("HTML5 optional/implied end tags" above).
        No code change made at that point; this file could not be dogfooded
        further until that question was resolved.

        RDD_KEY_198 (`<ruby>` opaque-implied-end-tag support) unblocked the
        forward pass past line 379, but it then hit a second, distinct
        blocker at line 718's `<select ... size=5>` -- `parseAttr` requiring
        a quoted attribute value, fixed by RDD_KEY_199. That unblocked
        forward progress past line 718, but a third, distinct blocker
        surfaced at line 759: a `<datalist>` with bare `<option
        value="...">` tags relying on HTML5's implied-end-tag rule for
        `<option>`, fixed by RDD_KEY_200's new
        `XmlSpecificRule.IMPLIED_CLOSE_TRIGGERS` mechanism (unlike `<ruby>`,
        `<option>` is parsed as a real node, not opaque). With all three
        fixed, a fresh full run of this file was performed: forward pass now
        completes end-to-end (round1 produces output for the whole 42KB
        file, no exception); round2 (reformatting round1's output) diffs
        empty against round1 (`diff -rq`, idempotent); `html_sc.js`
        syntax-check clean on both the original and round1 output; and
        `html_content_diff.py` reports full content preservation (element
        structure, attributes, text, comments, DOCTYPE all preserved --
        spot-checked both the explicitly-closed and implied-closed
        `<option>`s render as real, correctly-indented, correctly-closed
        nodes, not opaque/verbatim spans). **This completes the
        `alexandersandberg/html5-elements-tester` dogfood spot-check.**

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
