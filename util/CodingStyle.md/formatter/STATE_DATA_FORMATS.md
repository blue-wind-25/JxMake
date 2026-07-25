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
  `web-platform-tests/wpt` (done — see Checklist; scoped to the `html/syntax/`
  subtree, 416 files, per the size-scoping precedent noted below);
  `WordPress/wordpress-develop` (263 real `.html` hits, but
  mostly thin Gutenberg block-theme templates dominated by
  `<!-- wp:... {json} -->` comment shorthand rather than dense markup —
  `src/readme.html` is a genuine standalone page; usable as a light
  supplement, not a flagship run); `alexandersandberg/html5-elements-tester`
  (a single 42KB `index.html` exercising many HTML5 elements — good
  breadth-of-tag smoke test, but one file, not a corpus; treat as a quick
  spot-check, not a full dogfood session). `apache/ant`'s `manual/` directory
  added (user, 2026-07-26) as a light supplement — verified via `gh api
  .../git/trees/master?recursive=true` to contain 226 real, committed
  `.html` files (hand-authored static docs pages, not build-generated
  output), distinct from the `.xml`/`.xsd`/`.xsl` files this same repo
  already contributed to the XML dogfood run above (that run did not touch
  `manual/`). Good complement to the existing list's two extremes (clean
  modern boilerplate vs. WPT's deliberately adversarial conformance
  fixtures) — real, slightly-imperfect hand-written legacy-style HTML.
  Reuse the checkout already cloned for the XML session rather than
  re-cloning. **Run (2026-07-26): 226/226 files forward-pass, round2
  idempotency diff empty, syntax-check (`html_sc.js`) 0/226 failures.
  Content-preservation check (`html_content_diff.py`) found 2 real bugs,
  both fixed and covered by new fixture `real_code_regressions_125`:
  (1) `<p>` missing from `IMPLIED_CLOSE_TRIGGERS` — an unclosed `<p>`
  followed by a sibling block like `<h3>` produced a spurious duplicate
  `</p>`; fixed by adding the HTML5 spec's fixed "close a p element"
  trigger-tag list for `p` (RDD_KEY_204). (2) a same-line trailing HTML
  comment inside a `<td>` (or any sole-text-child element) was silently
  dropped — `renderNode`'s `TEXT` case and `renderElement`'s
  sole-content-child inline fast path didn't route through
  `appendWithTrailing`, so `node.trailingComment` was never emitted;
  fixed both render paths (RDD_KEY_205). After both fixes, re-running
  content-diff leaves 5 files with mismatches: 4 are the accepted
  comment-capitalization-only artifact (`normalize-comment-start-case=on`,
  same non-bug pattern as `real_code_regressions_69`) — `Tasks/antlr.html`,
  `Tasks/imageio.html`, `Tasks/attrib.html`, `Tasks/image.html`. The 5th,
  `running.html`, has a genuine unfixed gap — see Known Gaps below.**
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

- **HTML5 deep tree-construction edge cases (adoption agency, foreign-content
  foster-parenting, tag-name case-folding [since fixed separately, see
  below], implicit `<body>` start-tag insertion) -- NOT fixed except
  tag-name case-folding, same "genuinely too large a scope, don't force
  it" posture as the earlier general implied-end-tag question for the
  remaining three.** Found
  during the final
  `web-platform-tests/wpt` (`html/syntax/`) dogfood run: after the 4 bugs
  fixed that session (EOF-implied-close, `<image>`->`<img>` rewrite,
  `<head>`/`<body>` implied-close-trigger, `<xmp>` raw-text), 9 of the 386
  in-scope files still failed the forward pass. Following user review of
  that write-up, a follow-up pass (`real_code_regressions_110`) generalized
  the crash-avoidance posture: (1) the single hardcoded
  `"image".equals(...)` check was turned into a `TAG_NAME_REWRITES` map
  (still just the one entry -- no other spec tag-name rewrite is known to
  be needed, but any future one is now a one-line addition); (2) the
  `parseElement`-level tolerant-close fallback was broadened from
  EOF-only to *any* mismatched/unrecognized closing tag, and a matching
  fallback was added at the sole top-level `parseNodes(stopAtCloseTag=
  false)` call site in `format()`, so a stray closing tag with no matching
  open element anywhere in the document -- e.g. a made-up `<bogus>` tag
  that's never explicitly closed -- is now silently discarded rather than
  crashing, even when it bubbles all the way up to the document root. This
  confirmed-fixed 3 of the original 9 files (both "bogus"-tag-mismatch
  cases, `charset/after-bogus.html` and `after-bogus-after-1kb.html`, and
  the `&`-named fragment tag case, `serializing-html-fragments/
  serializing.html`) and, via a synthetic MathML repro
  (`<math><mtext></math></div>`), the same fix also silently resolves the
  `parsing/math-parse03.html`-shaped case (stray closing tag after a
  case-mismatched/foreign-content element). Real network access to
  re-fetch the original WPT files was not available this session to
  re-confirm the exact remaining file count against the live corpus, so
  the following are catalogued as still-open by category rather than
  re-verified 1:1: foster-parenting-driven tree reshaping
  (`parsing/foreign_content_009.html`, `_010.html` -- may no longer crash
  per the same stray-closing-tag fix, but the *resulting tree shape* is
  still not spec-accurate, since foster-parenting itself is not
  implemented); misnested `<form>` reconstruction inside `<template>`
  (`parsing/misnested-form-in-template.html`); an *implicit `<body>` start
  tag* -- content appearing directly after an implicitly-closed `<head>`
  with no `<body>` start tag ever written at all
  (`parsing/meta-inhead-insertion-mode.html`) -- the spec's separate
  "optional start tag" insertion behavior, distinct from the `<head>`/
  `<body>` implied-**close**-trigger already fixed. Implementing
  foster-parenting/case-folding/implicit-start-tag insertion properly
  remains adoption-agency/foster-parenting-scale tree-construction work --
  flagged here for a future pass rather than forced now. Does not block:
  `web-platform-tests/wpt` is otherwise recorded DONE below (this is a
  scope caveat on that run, not a blocking defect), and no other candidate
  in the HTML5 Test-Fixture Repos list is affected.

  **A separate, genuinely distinct crash site -- FIXED
  (`real_code_regressions_111`).** Raw-text elements (`<script>`/`<style>`/
  `<pre>`/`<xmp>`) previously threw `"expected closing tag </script>"` etc.
  from `finishRawElement`/`finishRawTextElement` when a raw-text element's
  literal closing tag never appears at all before real end-of-input
  (confirmed via a synthetic `<svg><script>...</s>` repro; this is the
  likely shape of the WPT `parsing/unclosed-svg-script.html` fixture, not
  re-verified against the live corpus since no network access was
  available). Unlike the deep tree-construction gaps above, this was a
  narrow, low-risk fix consistent with the same EOF-tolerance principle
  `parseElement` already applies to ordinary elements: both helpers (only
  ever called on the `lang.isHtml5` path) now capture whatever remains
  verbatim through EOF instead of throwing. `make test`: 160/160 forward +
  idempotency, zero regressions. This was the last crash site found while
  investigating this Open Question -- all HTML5 parsing paths reachable
  from real WPT dogfood input are now crash-free; only the tree-shape
  accuracy of the deep tree-construction gaps above remains open.

  **Tag-name case-folding (item 3 of the 4 deep tree-construction gaps
  above) -- DONE, fixed separately as a small, standalone item**
  (`real_code_regressions_112`, commit `10b20cf`, user, 2026-07-25). New
  `XmlSpecificRule.SVG_TAG_NAME_CASE_FIXUP` map holds the spec's full
  "Adjust SVG tag names" table (`altglyph`->`altGlyph`,
  `lineargradient`->`linearGradient`, `fegaussianblur`->`feGaussianBlur`,
  `foreignobject`->`foreignObject`, etc. -- the well-known, spec-stable
  full list), gated the *opposite* way from the existing single-entry
  `TAG_NAME_REWRITES` map: `svgDepth > 0` (inside real SVG foreign content)
  instead of `TAG_NAME_REWRITES`'s `svgDepth == 0` (outside it), since the
  two conditions are mutually exclusive and folding both cases into one map
  would have been wrong. Fixture proves the gate works both directions: SVG
  -nested lowercase-source elements are rewritten to spec mixed-case, and a
  same-named `<foreignobject>` used as plain HTML content outside any
  `<svg>` is left untouched. Applying the fixup surfaced a real latent bug
  in `parseElement`'s closing-tag match, fixed in the same commit: once
  `n.tagName` is case-rewritten, the old literal-case `closeTok` built from
  it no longer matches the source's own (differently-cased) closing tag,
  silently mis-parsing the element as unclosed via the existing
  tolerant-close fallback and corrupting the tree. Fixed with a new
  case-insensitive closing-tag check, `startsWithCloseTagIgnoreCase`,
  applied only on the `lang.isHtml5` path (other languages keep the
  exact-case check, since they never rewrite tag names). No dependency on
  the insertion-mode/open-elements-stack foundation the other three deep
  tree-construction gaps need. MathML's small attribute-only case-fixup
  (`definitionurl`->`definitionURL`) was intentionally left open -- this
  parser has no MathML-depth-equivalent tracking today (only `svgDepth`),
  and building one from scratch was judged scope creep beyond this
  standalone fix; revisit only if/when MathML foreign content gets its own
  tracking (e.g. as part of the grouped future job below, if it ever
  touches MathML). `make test`: 161/161 forward + idempotency, zero
  regressions.

  **TODO -- adoption agency, foster-parenting, implicit `<body>`
  insertion, and misnested `<form>`-in-`<template>` (items 1/2/4/5 above)
  are grouped as one future multi-session job, not four separate fixes**
  (user-reviewed sizing, 2026-07-25). All four ultimately need the same
  prerequisite the current recursive-descent `XmlSpecificRule` parser
  doesn't have: an explicit open-elements-stack + HTML5 insertion-mode
  state machine (currently "what insertion mode / what's open" is implicit
  in the Java call stack, not an inspectable structure) -- building that
  foundation is the dominant cost, comparable in size/risk to
  `STATE_COMMON.md`'s "general scope-depth reindentation" Architectural
  TODO (its own riskiest-change precedent). Once the foundation exists,
  each algorithm on top is a smaller incremental add. Recommended landing
  order once undertaken: **implicit `<body>` insertion first** (narrowest,
  self-contained -- auto-open `<body>` if content appears with none
  written), then **foster-parenting** (needed before foreign-content trees
  are shape-accurate), then **misnested `<form>`-in-`<template>`**
  (template-scoped, narrow once insertion modes exist), then **adoption
  agency last** (the spec's own most notoriously fiddly algorithm --
  attempt only once the foundation has proven solid on the other three).
  Real-world impact is low in the meantime: every actual dogfood corpus run
  so far (`h5bp/html5-boilerplate`, `WordPress/wordpress-develop`,
  `alexandersandberg/html5-elements-tester`, `web-platform-tests/wpt`
  `html/syntax/`) formatted cleanly with zero structural loss -- these four
  gaps only bite on the kind of deliberately pathological
  misnesting/foreign-content edge cases WPT's own conformance fixtures are
  built to exercise, not on markup anyone hand-writes. Not blocking any
  current checklist item; flagged here as a scoped future job, per
  `STATE_COMMON.md`'s ambiguity/open-question convention.

  **Investigation (2026-07-26): can implicit `<body>` insertion be pulled
  out of the grouped job and implemented standalone?** No. Checked directly
  against `XmlSpecificRule.java`: `<html>`/`<head>`/`<body>` get exactly one
  special case today (`IMPLIED_CLOSE_TRIGGERS`'s `head`->`{body}`, closing
  `</head>` when `<body>` is literally seen); otherwise they parse as
  ordinary elements. This TODO item is the opposite case — a document with
  **no** `<body>` start tag anywhere — which requires *fabricating* a tag
  absent from the source, the first tag-synthesis path in a formatter
  whose every existing rule is preserve-as-written. Correct synthesis also
  needs to know whether a body was already implicitly opened earlier (to
  avoid double-insertion), i.e. threading state across recursive
  `parseNodes`/`parseElement` calls — a lightweight version of the same
  open-elements-stack the grouped job already requires, not an escape from
  it. Decision: not separable/low-risk standalone; stays folded into the
  grouped future job, landing order unchanged. Status quo without any fix
  (`RDD_KEY_185`: bare top-level content reindents as an ordinary sibling
  at whatever depth the source implies) doesn't corrupt output, just isn't
  spec-faithful tag synthesis.

  **Known gap found (2026-07-26, `apache/ant` `manual/` dogfood run):**
  `manual/running.html` loses one `<p>` in content-diff (82 vs 81 body
  children; source lines ~508-513). Root cause: an orphan `</p>` with no
  matching open `<p>` sits directly against bare top-level text with no
  wrapping tag at all — a different, deeper malformed-markup shape than
  the ordinary unclosed-`<p>`-before-a-sibling-block case RDD_KEY_204
  fixed (that fix does not cover this). Affects 1 of 226 files in the
  corpus; deferred rather than fixed this session — falls under the same
  "deep tree-construction gaps" bucket documented just above (no
  open-elements-stack / insertion-mode tracking yet), not a new standalone
  gap. Not blocking; revisit alongside the grouped future tree-construction
  job.

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
- [x] Real-code testing pass per `STATE_COMMON.md`'s methodology against
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
      TOML Test-Fixture Repos list now fully complete); **HTML5 DONE**
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
      clean), and `web-platform-tests/wpt` DONE (scoped to `html/syntax/`,
      416 files, 4 bugs found+fixed — see per-repo entry below). This
      completes the HTML5 Test-Fixture Repos list and, with it, the overall
      real-code-testing pass across all six sub-formats (JSON/JSON5, CSS,
      XML, YAML, TOML, HTML5).

      **Shared methodology (every run below):** clone fresh (or reuse a
      prior-session checkout) per `STATE_COMMON.md`; exclude build/vendor/
      generated dirs; sample above a per-format threshold (JSON/CSS:
      "several thousand+"; XML/YAML: "several hundred+"), else process the
      full set. Every run: baseline syntax-check (separates pre-existing-
      invalid/template fixtures from in-scope files) → round1 format
      (`--preserve-tree --root`) → round2 → `diff -rq` idempotency →
      syntax-check round1 vs baseline → format-specific content-preservation
      check (`css_content_diff.py`/`xml_content_diff.py`/
      `yaml_content_diff.py`/`toml_content_diff.py`/`html_content_diff.py`,
      per "Dogfood Output Validation" above). A comment-capitalization-only
      diff (`normalize-comment-start-case=on`) recurs across nearly every
      run below and is expected/correct per the `real_code_regressions_69`
      precedent, not a bug.

      **CSS (4/4 done):** `twbs/bootstrap` (31 in-scope `.css`, rest `.scss`)
      — 1 bug: rtlcss directive comments (`/* rtl:begin:ignore */`) were
      capitalized to `/* Rtl:... */`, breaking rtlcss but still valid CSS
      (`css_sc.js` missed it); fixed via `isSingleTokenDirective` (skip
      capitalization when a comment's first-line body is one whitespace-free
      `:`/`-`-containing token). Fixture `real_code_regressions_69`, commit
      `8f5f597`. Final 31/31 clean. `necolas/normalize.css` (1 file) — zero
      bugs, 1/1 clean; `css_content_diff.py` written this session.
      `foundation/foundation-sites` — 100% `.scss`, 0 in-scope files, no run.
      `primer/css` (2 files) — zero bugs, 2/2 clean (one expected comment
      capitalization).

      **JSON/JSON5 (4/4 done):** `json5/json5` (6 files) — zero bugs.
      `microsoft/vscode` (1272 in-scope of 1377) — 1 idempotency bug:
      `parseContainer` kept a dangling placeholder `Item` for a
      comment-less blank line before `}`, defeating the tight-`{}`
      short-circuit only on round2; fixed to keep the placeholder only with
      a real leading comment. Fixture `real_code_regressions_68`, commit
      `e2a6f0e`. `babel/babel` (810 sampled of 9245) — zero bugs.
      `eslint/eslint` (91 in-scope of 98) — zero bugs.

      **XML (4/4 done):** `apache/maven` (398 sampled of 3158) — zero bugs;
      wrote `xml_content_diff.py` this session. `w3c/svgwg` (294 in-scope of
      298, incl. `.svg`) — 1 bug: `Lang.infer` never mapped `.svg` to `xml`;
      fixed by adding the extension. Fixture `real_code_regressions_74`,
      commit `3408acd`. `apache/ant` (214 in-scope of 558, incl. `.xsd`/
      `.xsl`) — 1 bug, same shape: `.xsd`/`.xsl` also unmapped in
      `Lang.infer`; fixed. Fixtures `real_code_regressions_91`/`_92`, commit
      `25bd5b8`. `jenkinsci/jenkins` (130 in-scope of 131) — zero bugs (3
      files hit a `xml_content_diff.py` stdlib-`minidom` parsing limitation
      on embedded control chars, not a formatter bug). All four final
      re-runs: forward/idempotency clean, content-preservation diffs are all
      comment-capitalization-only.

      **TOML (4/4 done):** `rust-lang/cargo` (670 in-scope of 672) — 2 bugs,
      both forward-pass crashes: (1) an interior per-array-element comment
      (`"target/", # exclude ...`) — the continuation-joining loop only
      stripped a trailing `#` comment from the fully-assembled line, so an
      interior line's comment swallowed the array's `]`; fixed by stripping
      each continuation line's own comment before joining. (2) `"""..."""`
      multi-line strings had no handling in the flat line-scanner; fixed by
      capturing them as an opaque verbatim span (same treatment as JSON5's
      multi-line strings). Fixture `real_code_regressions_70`, commit
      `d56eb3a`. `python-poetry/poetry` (106 files), `pola-rs/polars` (57
      files), `toml-lang/toml` (1 in-scope file — repo is spec prose only,
      no compliance corpus) — zero bugs each. All final re-runs clean on all
      four checks.

      **YAML (6/6 done):** `kubernetes/kubernetes` (453 sampled of 6366) —
      6 bugs, all in `YamlSpecificRule`'s sequence/mapping parsing: (1)
      same-indent sequence child under its first mapping key not allowed
      (mirrored the already-allowed case); (2)/(3) quoted/unquoted scalars
      wrapping across lines with a deeper continuation crashed the parser,
      fixed by capturing as an opaque multi-line scalar; (4) a dangling
      comment-only (null-key) item NPE'd the colon-alignment padding helper,
      fixed by excluding it; (5) block-scalar continuation indent was
      stored absolute instead of relative to the key, breaking idempotency;
      (6, found via content-preservation) a block scalar as a plain
      sequence item's value rendered empty (no-colon branch never checked
      for a block-scalar header), which also surfaced a related dash-anchor
      render-offset bug. Fixture `real_code_regressions_71` (extended for
      bug 6), commits `fff5a3f`/`025af9f`. Final 453/453 clean all checks.
      `docker/compose` (250 in-scope of 261) — 1 bug via content-
      preservation: a blank line after a keyed line with no inline value
      made all four "does this key have a child block" sites (plain
      `peek()`) miss the nested block entirely, silently dropping it; fixed
      with a `peekNonBlank()` helper. Fixture `real_code_regressions_72`,
      commit `2640cf2`. Final 250-file re-run clean (1 pre-existing
      PyYAML-emoji tool-gap flagged, not a formatter bug).
      `ansible/ansible` (415 in-scope of 2110, sampled) — 3 bugs, all via
      content-preservation: (1) a plain sequence item's unquoted scalar
      wrapping across lines had no continuation handling, unlike the keyed
      case; fixed by adding the same capture. (2) a comment dedented below
      its enclosing block's indent made `parseBlock` break out without
      consuming it, orphaning everything after at every level; fixed by
      looking past the comment to the real next line. (3) a bare top-level
      plain-scalar document (`$ANSIBLE_VAULT;...` blob) kept only its first
      line; fixed by emitting the remaining raw lines verbatim. Fixture
      `real_code_regressions_73`, commit `9f2a80a`. Final 415/415 clean.
      `actions/starter-workflows` (186 in-scope of 188) — 1 bug via
      syntax-check (output actually invalid, unlike the prior 3 YAML
      sessions): `parseSeqItem` hardcoded the sibling-key column as
      `ln.indent + 2`, so extra padding after a sequence dash
      (`-   name: foo`) misidentified the next sibling key as a nested
      child; fixed by deriving the real column from the dash line's actual
      leading whitespace. Fixture `real_code_regressions_75`, commit
      `f1648c5`. Final re-run clean (7 files hit the known PyYAML-emoji gap).
      `prometheus/prometheus` (375 in-scope of 380) — 4 bugs, all data loss
      only caught by content-preservation, all sharing the "dash/key line
      whose value is absent/comment-only/anchor-only, real content on a
      deeper subsequent line" shape: (1)/(2) `parseKeyItem`/`parseSeqItem`'s
      flow-looking-inline-value checks didn't confirm the flow actually
      closed on the same line, truncating an unbalanced multi-line `[...]`
      opener (also (2): a `- # comment`-only dash line dropped its
      subsequent real mapping keys). (3) an anchor-only dash line
      (`- &highalert`) followed by a nested mapping at an equal (not just
      greater) indent lost its child block. (4) `renderFlowValue` rendered
      an empty flow map/seq (`{}`/`[]`) as a block conversion when it didn't
      fit, but `renderFlowBlock` has nothing to iterate for zero entries,
      silently vanishing the value. Fixture `real_code_regressions_83`.
      Final 375/375 clean. `home-assistant/core` (878 in-scope of 921) — 1
      bug via content-preservation: compact single-line nested-sequence
      items (`- - a\n  - b`) weren't recognized by `parseSeqItem`, causing
      the sibling item's indent to mismatch and `parseBlock` to break out
      early, dropping the rest of the sequence and every later sibling at
      every level (6 files affected); fixed via a new
      `parseInlineNestedSeq` helper rendered through the existing
      `item.children` path. Fixture `real_code_regressions_86`, commit
      `e7f0334`. Final 870/878 clean (8 files hit the known PyYAML-emoji
      gap). **This completes all 6 planned YAML Test-Fixture Repos.**

      **HTML5 (all candidates done or dropped):** `twbs/bootstrap` docs
      site, `mdn/content`, `whatwg/html`, `kangax/html-minifier` were all
      investigated and dropped — none has a real committed HTML5 corpus
      (Astro/MDX, Markdown, a custom-macro spec source, and JS-string unit
      tests respectively; see Open Questions for the investigation detail).
      `h5bp/html5-boilerplate` (4 files) — zero bugs; wrote
      `html_content_diff.py` this session.
      `WordPress/wordpress-develop` (73 in-scope of 303, real markup only —
      rest are thin Gutenberg block-comment templates) — 1 bug via content-
      preservation: `renderElement`'s multi-child block-closing render path
      never emitted `n.trailingComment` (unlike the other three render
      branches), silently dropping a same-line trailing comment right after
      a block element's closing tag. Fixed by routing that path through
      `appendWithTrailing` too. Fixture `real_code_regressions_103`. Final
      73/73 clean (2 comment-text-mismatch diffs left as an open design
      question — see "WordPress magic-comment capitalization" below).
      `alexandersandberg/html5-elements-tester` (1 file, 42KB) — 3
      sequential blockers found and fixed across sessions: `<ruby>`'s
      implied-end-tags (RDD_KEY_198, opaque-verbatim-span capture),
      unquoted attribute values (RDD_KEY_199), and `<option>`'s
      implied-close-trigger via the new general `IMPLIED_CLOSE_TRIGGERS`
      map (RDD_KEY_200). File now completes end-to-end on all four checks.
      `web-platform-tests/wpt` (scoped to `html/syntax/`, 416 files pulled
      via `gh api` tree-walk + `raw.githubusercontent.com`, since the repo
      is ~2.6GB/6552 files and this system's `git` can't `--filter=blob:none
      --sparse`; 386 in-scope after baseline) — 4 bugs found in the initial
      session (fixture `real_code_regressions_109`): (1) EOF no longer
      implicitly closes every still-open element (dominant pattern, 73 of
      85 initial failures — many `syntax/speculative-parsing/**` fixtures
      validly omit `</body>`/`</html>` at EOF per the spec's "stopped
      parsing" step); (2) `<image>`→`<img>` tag-name rewrite, scoped to HTML
      content only via a `svgDepth` counter (real SVG `<image>` is a
      distinct legitimate element); (3) `<head>` registered in
      `IMPLIED_CLOSE_TRIGGERS` to close on a sibling `<body>` start tag; (4,
      found via content-preservation) `<xmp>` wasn't recognized as a
      raw-text element like `<pre>`/`<script>`/`<style>`, so literal
      `<script>...</script>` text inside it was mis-parsed as a real nested
      element (64 files). A follow-up session (fixture
      `real_code_regressions_110`, after user review) generalized two of
      these further: the single `<image>` check became a reusable
      `TAG_NAME_REWRITES` map, and the tolerant-close fallback was broadened
      from EOF-only to any mismatched/unrecognized closing tag (at both the
      per-element level and the sole document-root `parseNodes` call site),
      so an unclosed made-up tag (e.g. `<bogus>`) never crashes even if the
      stray closing tag bubbles to the document root — this fixed 3 of the
      9 residual failures from the first session. Final full re-run (377
      in-scope files after the first session's fixes): forward/idempotency/
      syntax-check 377/377 clean; content-preservation 127/377 show a diff,
      every mismatch comment-capitalization-only. A second follow-up
      (fixture `real_code_regressions_111`) fixed a further, genuinely
      distinct crash site found while investigating the residual failures:
      raw-text elements (`<script>`/`<style>`/`<pre>`/`<xmp>`) whose literal
      closing tag never appears before real EOF now capture whatever
      remains verbatim instead of throwing (same EOF-tolerance principle as
      ordinary elements) -- this is the likely shape of
      `parsing/unclosed-svg-script.html`, not re-verified against the live
      corpus (no network access either follow-up session). Remaining
      residual failures/deep tree-construction gaps (foster-parenting,
      case-folding, implicit start-tag insertion) are cataloged in the
      "HTML5 deep tree-construction edge cases" Open Question below. **This
      completes the HTML5 Test-Fixture Repos list and the overall
      real-code-testing pass across all six sub-formats.**
      `apache/ant`'s `manual/` directory (226 files, added later as a light
      supplement, run 2026-07-26): forward pass, round2, and idempotency
      diff all 226/226 clean; syntax-check (`html_sc.js`) 0/226 failures.
      Content-preservation found 2 real bugs, both fixed: `<p>` missing
      from `IMPLIED_CLOSE_TRIGGERS` causing a spurious duplicate `</p>`
      after an unclosed `<p>` (RDD_KEY_204), and a same-line trailing
      comment inside a sole-text-child element (e.g. `<td>`) silently
      dropped because `renderNode`'s `TEXT` case and `renderElement`'s
      sole-content-child inline fast path skipped `appendWithTrailing`
      (RDD_KEY_205). Fixture `real_code_regressions_125`. Final re-run:
      221/226 clean, 4 comment-capitalization-only (accepted non-bug
      artifact), 1 genuine unfixed gap (`running.html`, orphan `</p>`
      against bare top-level text — see "deep tree-construction gaps"
      Open Question above).
