# STATE_DATA_FORMATS.md — Data & Markup Format JAR Support Tracker

Read `STATE_COMMON.md` first (shared commit/ambiguity/testing conventions).
`STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` are NOT required reading for this job.
Dogfood corpus status: see `STATE_DOGFOOD.md`.

---

## Purpose

Tracks data/markup format support in the deterministic JAR formatter
(`util/CodingStyle.md/formatter/`), per `STYLE_DATA_FORMATS.md` (JSON/JSON5,
XML, CSS, HTML5, YAML, TOML). **All six DONE** — real tokenizer/parser/
printer logic landed for each, `make test` green (per-format final
counts/deferred edge cases in Checklist; RDD_KEY_190/191/192/193/194 for
implementation history). HTML5's `<script>` dispatch to JS/TS is also real
(commits `a3c5c81`/`7cca3a4`/`679fafb`, landed once `STATE_JS_TS.md` got a
real JS/TS formatter) — see `XmlSpecificRule.renderScriptOrStyle` and the
HTML5 checklist entry below.

---

## Scope

`STYLE_DATA_FORMATS.md` covers non-imperative data/markup formats — no
functions/control flow, so most shared `STYLE.md` imperative-language rules
don't apply. Six sub-formats, each stating which `STYLE.md` sections it
borrows from:

1. **JSON / JSON5** (§1) — RFC 8259 JSON plus JSON5. Key/value `:` column
   alignment (declaration-alignment group/group-break shape, always a space
   before `:`, unlike declaration `=`); bracket/complexity tight-loose
   padding (reuses existing nesting-complexity signal); JSON5-only
   multi-line strings via backslash-newline continuation (opaque, preserved
   exactly, same treatment as Java text blocks/JS template literals). Plain
   `.json` has no comment syntax, so `JXM_CFMT_DIS`/`ENA`/`CFG` directives
   only work in `.json5`.
2. **XML** (§2) — XML 1.0 and dialects (XHTML, SVG, MathML, RSS, Atom,
   Android XML, Maven POM, Ant `build.xml`, IntelliJ XML). Uses existing
   global `indent-size`/`indent-style` config, no XML-specific indent
   config. Tag/attribute formatting (nesting indent, attribute wrap on
   overflow, self-closing tags preserved, attribute order preserved incl.
   `xmlns`). `<!DOCTYPE>`/PIs/CDATA opaque/preserved verbatim, except CDATA
   that is direct content of a `<script>`/`<style>` tag, which unwraps and
   dispatches per §4.2. Directives: single `<!--% ... -->` block form (no
   line-comment equivalent in XML).
3. **CSS** (§3) — modern CSS (CSS 3+ modules). Property/value `:` column
   alignment (same group shape as JSON's, space-before-`:`); at-rules
   (`@media`, `@supports`, `@keyframes`, `@font-face`) and CSS Nesting
   Module `&` blocks are headers starting their own independent alignment
   group, recursing per nested block. Directives: single `/*% ... */` block
   form (CSS has no `//` line comment).
4. **HTML5** (§4) — tag formatting reuses XML's §2.2 rules directly, with
   one override: void elements (`area`, `base`, `br`, `col`, `embed`, `hr`,
   `img`, `input`, `link`, `meta`, `param`, `source`, `track`, `wbr`) never
   get a closing tag and any self-closing `/` is normalized away. Main
   design point is **dispatch**: `<style>` content splices to the CSS
   formatter (§3) and back; `<script>` content splices to the JS/TS
   formatter (`STYLE_JS_TS.md`) and back, incl. CDATA-wrapped variant; a
   `<script type="...">` with non-JS/TS type stays opaque.
5. **YAML** (§5) — YAML 1.1/1.2, incl. multi-document streams. Key/value
   `:` column alignment (same group shape as JSON's, space-before-`:`);
   sequence items indent one level deeper than their parent mapping key;
   flow-style collections (`{a: 1}`, `[1, 2]`) preserved as written unless
   they'd exceed `line-length`, in which case converted to block style;
   anchors/aliases/tags and block scalars (`|`/`>`) preserved
   verbatim/opaque. Uses the existing global `indent-size`, but
   `indent-style` is ignored for YAML (forbids tab indentation — always
   space-indented). Directives: single `#% ...` line form (no block
   comment).
6. **TOML** (§6) — TOML v1.0. Key/value `=` column alignment reuses
   STYLE.md §5/§6's assignment-alignment shape directly (no forced
   space-before-`=`, unlike JSON/YAML's `:`); table/array-of-table headers
   (`[section]`/`[[array]]`) are headers, not declarations, and get no
   added indentation for keys under them (TOML nests via dotted header
   names, not indentation); arrays reuse tight/loose bracket rule; inline
   tables always single-line (grammar constraint, not a style choice);
   dotted keys and string quote styles preserved as written. TOML doesn't
   use `indent-size`/`indent-style` at all. Directives: single `#% ...`
   line form (no block comment).

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
| RDD_KEY_200 | HTML5 `<option>` implied-closing-trigger support: new, general, reusable `XmlSpecificRule.IMPLIED_CLOSE_TRIGGERS` (`Map<String, Set<String>>`, element name -> sibling start-tag names that implicitly close it), distinct from `OPAQUE_IMPLIED_END_TAG_ELEMENTS` -- a registered element is still parsed as a REAL node (attributes/children/normal rendering), only the "when do children stop" decision changes; only `parseNodes`/`parseElement` were touched, no per-element control-flow. Registered only `option` -> `{option, optgroup}` today. `parseNodes` gained an optional trigger-set parameter that also breaks its loop on an upcoming (non-closing) start tag matching the set; `parseElement` still consumes an explicit `</tag>` when present (regression-safe), otherwise treats the element as implicitly closed with no explicit tag consumed when a trigger set is registered (covering both the sibling-trigger case and the pre-existing parent-close-via-`stopAtCloseTag` case, reused rather than reinvented) -- otherwise the pre-existing hard `XmlParseException` is unchanged. Fixture `test/real_code_regressions_108_{inp,out}.html` (explicit-close regression guard + `<datalist>`/`<optgroup>` implied-close cases). `make test` 157/157, zero regressions. This was the `alexandersandberg/html5-elements-tester` dogfood run's third and final blocker -- the full 42KB file now completes end-to-end (forward pass, round2, idempotency diff, `html_syntax_check.js` syntax-check, `html_content_diff.py` content-preservation all clean); dogfood run for this candidate is DONE. |
| RDD_KEY_224 | HTML5 commented-out markup-fragment comments (`<!--tr>...</tr-->`, `<!--p>...</p-->`) being corrupted by `normalize-comment-start-case`; new `XmlSpecificRule.isMarkupFragmentDirective`/`MARKUP_FRAGMENT_TAG_NAMES` (see Open Questions below and `RDD_LOG.md` for full evidence and reasoning) |
| RDD_KEY_232 | Multi-line `<!-- -->` comments (raw interior contains a newline) now freeze byte-for-byte verbatim, reusing the existing `commentVerbatim` render mechanism; sibling/node-tree indentation unaffected; shared XML/HTML5 code path |
| RDD_KEY_262 | `normalize-comment-end-period` wired into every data-format language for the first time (previously only curly/indent/tags families plus Makefile/Bash/PowerShell, RDD_KEY_261) -- JSON/JSON5/CSS via new `FormatterSimpleBraced.stripCommentEndPeriod`; TOML/YAML/XML/HTML5 reuse `ToolingCommentNormalizer.stripSoleTrailingPeriod` (made package-private for cross-reuse); HTML5's `<style>`/`<script>` splice paths thread the flag through too; no new config keys; 5 YAML fixtures regenerated (copyright-header trailing period) |
| RDD_KEY_264 | XML/HTML5 multi-line `<!-- -->` comments already in the conventional ` * `-per-line banner shape now get curly's `/* */` treatment (capitalize first line, strip sole trailing period, reindent) instead of RDD_KEY_232's blanket freeze-verbatim; new `XmlSpecificRule.tryBannerShape`/`Node.commentBannerLines`; closing `-->` renders flush at the comment's own indent (no curly-style alignment space -- user explicitly rejected that). Fixture `test/html_multiline_comment_banner_{inp,out}.html`. |
| RDD_KEY_265 | JSON/JSON5/CSS ("SimpleBraced") comment normalization gains curly's grouping shape: new `FormatterSimpleBraced.normalizeComment`/`normalizeCommentTrivia`/`normalizeLineCommentChain`/`normalizeBlockComment`/`tryBannerShape`; json5's consecutive standalone `//` chain-group (only first capitalized, sole trailing period stripped only across the whole chain); a `/* */` block comment in banner shape gets single-unit treatment, any other multi-line shape falls back to the pre-existing whole-comment-scan behavior (not left untouched). Fixture `test/json5_comment_banner_{inp,out}.json5`; `test/json5_comments_out.json5` regenerated. |
| RDD_KEY_266 | YAML/TOML `#`-comment chain-grouping parity with curly, shared implementation with the tooling job (RDD_KEY_267): new `ToolingCommentNormalizer.normalizeChain`; `YamlSpecificRule`/`TomlSpecificRule` each gained a `finalizeComments` deferred-normalization step (TOML also gained new per-comment blank-line tracking it lacked before). Fixtures `yaml_comment_chain_{inp,out}.yaml`/`toml_comment_chain_{inp,out}.toml`; 5 pre-existing YAML fixtures regenerated. |
| RDD_KEY_280 | YAML/TOML DRY cleanup (Tier1 item): `YamlSpecificRule`/`TomlSpecificRule`'s duplicated `=`/`:`-alignment padding, same-line `#`-comment splitting, and comment start-case/end-period normalization factored into new package-private `rules/YamlTomlSharedRule.java`. Pure pull-up, no behavior change; `make test` 278/278 forward + idempotency before and after. |
| RDD_KEY_300 | 2026-08-16 cleanup pass: `JsonSpecificRule`/`CssSpecificRule`'s `repeatChar`/`indent` (byte-identical to each other and to `YamlTomlSharedRule`'s own pre-RDD_KEY_280 copy) promoted to `FormatterSimpleBraced`; `YamlTomlSharedRule` re-routed to delegate there too. Full details/rationale in `RDD_LOG.md`. |
| RDD_KEY_302 | Follow-up to RDD_KEY_300's deferred item: `TomlSpecificRule.bracketBalance`/`findAssignmentEquals`/`YamlSpecificRule.findMappingColon`'s duplicated quote-aware bracket-depth scanner consolidated into `YamlTomlSharedRule.scanQuoteAwareBracket` (real parameterized-terminal-condition abstraction, `BracketScanStop`/`BracketScanResult`, not a packed-int return). Full details/rationale (including the packed-int-return regression found and fixed mid-task) in `RDD_LOG.md`. |

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
- **YAML:** uses the existing global `indent-size` (no YAML-specific
  override — global default of 4 stands). **`indent-style` is
  ignored/inapplicable for YAML** — YAML forbids tab indentation, output is
  always space-indented regardless of configured value (RDD_KEY_191). Local
  YAML fixtures set `indent-size=2` via an in-file
  `#% JXM_CFMT_CFG indent-size=2` directive to exercise YAML's
  community-standard indent width, rather than changing the tool-wide
  default (RDD_KEY_191).
- **TOML:** no new config; doesn't use `indent-size`/`indent-style` at all
  (§6.2 gives table-header keys no added indentation, and TOML rarely
  produces any incidental indentation).

---

## Dogfood Output Validation (syntax checkers)

"Did the formatter corrupt the file" (real-code-testing pass, see Checklist)
is checked two ways per format, both in `tools/verifiers/` (committed,
licensed project tooling; `java_syntax_check`/`kotlin_syntax_check` also
live there, unrelated to this campaign):

**Syntax checkers** (six Node.js scripts, parse-only, `line:col: message` on
error, exit 1 if any file errors else 0):
- `json_syntax_check.js` (`JSON.parse`, plain `.json` only)
- `json5_syntax_check.js` (`json5`)
- `yaml_syntax_check.js` (`js-yaml`, `loadAll()` for multi-doc streams)
- `toml_syntax_check.js` (`smol-toml`)
- `css_syntax_check.js` (`postcss`, NOT `css-tree` — css-tree silently
  auto-closes an unclosed `{` at EOF instead of erroring, confirmed by
  direct testing; `postcss.parse()` throws `CssSyntaxError` with
  line/column instead)
- `xml_syntax_check.js` (`@xmldom/xmldom`, custom `onError` to capture
  warnings the default handler only logs, plus try/catch around
  `parseFromString` since `fatalError` throws regardless of `onError`)
- `html_syntax_check.js` (`parse5`, `onParseError`) also exists for HTML5
  but is a weaker signal — HTML5 parsing is spec-mandated error-tolerant
  (auto-closes mismatched tags), so it only catches the narrow set of
  spec-defined parse errors, not general malformed markup the way the XML
  checker does (documented as a caveat in the script).

All six verified against hand-crafted good/bad pairs (malformed trailing
comma, unclosed brace, mismatched tag, etc.) before trusted for dogfood use.

**Content-preservation checkers** (Python; prove "still means the same
thing," not just "still parses" — motivated by the CSS `twbs/bootstrap`
rtlcss-comment corruption bug, fixture `real_code_regressions_69`, still
valid CSS and would have slipped past `css_syntax_check.js` alone). Each
verified against a hand-crafted good pair (whitespace-only reformat) plus
deliberately-mutated bad pair(s) before trusted for dogfood use; each caught
its bad case:

- `css_content_diff.py` — stdlib `re` only. Checks comment text
  byte-identical/in-order, comment-stripped/colon-normalized token stream,
  `!important` count, vendor-prefix counts per distinct prefix. First used
  ad hoc during `twbs/bootstrap`, promoted to permanent during
  `necolas/normalize.css`.
- `xml_content_diff.py` — stdlib `xml.dom.minidom`. Walks both DOMs in
  parallel (skipping pure-whitespace text nodes): element names + attribute
  name/value pairs **in order** (XML preserves attribute order per §2.2, so
  reordering is a real bug); text/comment content whitespace-normalized;
  CDATA byte-identical; node-type mismatch at the same tree position =
  structural mismatch. Written during `apache/maven`.
- `toml_content_diff.py` — this system's Python (3.6) has no stdlib
  `tomllib`/`toml`/`tomli`, so it shells out to an inline Node helper
  (`node -e ... -- <path>`, note `<path>` lands at `argv[1]` not `argv[2]`
  with `-e`) using `smol-toml` to parse to JSON, then deep-compares. Written
  during `rust-lang/cargo`. Needs the same `LD_LIBRARY_PATH`/`NODE_PATH`/
  `PATH` env as the `*_syntax_check.js` scripts (unlike the stdlib-only
  Python checkers).
- `yaml_content_diff.py` — PyYAML parses directly via `yaml.safe_load_all`
  (multi-doc aware) on both files and deep-compares; a best-effort
  `#`-comment-line scan reports textual diffs as informational-only.
  Written during `kubernetes/kubernetes`; caught a real bug (a block-scalar
  sequence item silently truncated) that `yaml_syntax_check.js` alone
  missed since the truncated output was still valid YAML.
- `html_content_diff.py` — stdlib can't parse real-world HTML5 (not
  XML-well-formed), so shells out to an inline Node helper using `parse5`
  into a simplified JSON tree, walked in parallel comparing tag
  names/structure, attribute pairs in order, whitespace-normalized text,
  comments, and DOCTYPE. `<script>`/`<style>` bodies deliberately NOT
  byte-compared (legitimately dispatched to JS/TS/CSS pipelines which may
  reformat them) — only checked for surviving at the same tree position
  with the same attributes and a non-empty body. Written during
  `h5bp/html5-boilerplate`. **Note:** can under-report serialization-level
  structural bugs (e.g. content dumped after `</html>`) because `parse5`
  re-parents per the HTML5 "after after body" recovery before comparing —
  spot-check raw output text directly, not just the diff verdict.

Exit codes: 0 = match, 1 = mismatch (description printed), 2 = a file
doesn't parse at all (n/a in practice — files are syntax-checked first).
Usage is uniform: `python3 <script> <original> <formatted>`.

Requires the same `LD_LIBRARY_PATH`/`NODE_PATH`/`PATH` env as
`STATE_JS_TS.md`'s "Tools/compiler used" section (same `node` binary, same
`~/mynpm`-installed package location — see that file for the exact export
lines and why `LD_LIBRARY_PATH` is required here). Install each script's
package once via `npm install --prefix ~/mynpm <pkg>` (json5, js-yaml,
smol-toml, postcss, @xmldom/xmldom, parse5) before first use.

---

## Test-Fixture Repos

Recorded for regression testing (all now dogfooded — see Checklist for
outcomes):

- **JSON/JSON5:** `json5/json5`, `microsoft/vscode`, `babel/babel`, `eslint/eslint`
- **XML:** `apache/maven`, `apache/ant`, `jenkinsci/jenkins`, `w3c/svgwg`
- **CSS:** `twbs/bootstrap`, `necolas/normalize.css`, `foundation/foundation-sites`,
  `primer/css`
- **HTML5:** `h5bp/html5-boilerplate`; `twbs/bootstrap` docs site,
  `mdn/content`, `whatwg/html`, `kangax/html-minifier` investigated and
  dropped — no real committed HTML5 corpus (Astro/MDX, Markdown, a giant
  non-HTML preprocessed spec source, JS unit tests with inline HTML strings
  respectively; see Open Questions). Three replacements added (user,
  2026-07-24), verified via `gh api` to contain real `.html`:
  `web-platform-tests/wpt` (scoped to `html/syntax/`, 416 files);
  `WordPress/wordpress-develop` (263 real `.html` hits, mostly thin
  Gutenberg block-theme templates — light supplement, not flagship);
  `alexandersandberg/html5-elements-tester` (single 42KB `index.html`, wide
  tag coverage — spot-check, not a corpus). `apache/ant`'s `manual/`
  directory added (user, 2026-07-26) as a light supplement — 226 real
  hand-authored static docs `.html` files, distinct from the `.xml`/`.xsd`/
  `.xsl` files the same repo already contributed to the XML run.
- **YAML:** `kubernetes/kubernetes`, `docker/compose`, `ansible/ansible`,
  `actions/starter-workflows`, `prometheus/prometheus`,
  `home-assistant/core`
- **TOML:** `rust-lang/cargo`, `python-poetry/poetry`, `pola-rs/polars`,
  `toml-lang/toml`

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
  (RDD_KEY_193/194), HTML5-only additions (void elements, `<script>`/
  `<style>` dispatcher) gated on `lang.isHtml5`.
- **JSON/JSON5/CSS** are neither tag-based, curly, nor indent-based per
  `Lang.java`'s family predicates. **Resolved (RDD_KEY_189):** no new
  `TokenizerCore` sibling (no `TokenizerFlat`) — each extends `TokenizerCore`
  directly with a minimal override: JSON/JSON5 share one
  `JsonSpecificRule.java` (gated on `lang.isJson5`), CSS gets
  `CssSpecificRule.java`, YAML/TOML get `YamlSpecificRule.java`/
  `TomlSpecificRule.java`. **Resolved further (RDD_KEY_190), once JSON/JSON5
  real logic landed:** JSON/JSON5 and CSS form a new "SimpleBraced"
  `Lang.isSimpleBraced` family — `TokenizerSimpleBraced` (shared `/* */`
  block-comment scan) and `FormatterSimpleBraced` (shared
  `padKeysForColonAlignment` group-padding, §1.1/§3.1's identical colon-
  alignment shape); `JsonTokenizer` extends `TokenizerSimpleBraced`,
  `FormatterJson` extends `FormatterSimpleBraced`
  (`FormatterCore.forLanguage`'s `isJson || isJson5` branch) — CSS later
  joined `SimpleBraced` too. Distinct from the still-hypothetical YAML/TOML-
  only "Flat" family (no braces at all): YAML/TOML did NOT join
  (RDD_KEY_192), each implementing its own from-scratch parser (line-based
  for YAML, flat single-pass line-scanner for TOML).
- Implementation order (all complete): JSON/JSON5 → CSS → YAML/TOML → XML →
  HTML5 (HTML5 last, depended on CSS and, for one exception below, JS/TS).
- **HTML-before-JS/TS contingency — historical, fully superseded.** HTML5
  landed (RDD_KEY_194) before `STATE_JS_TS.md` had a real JS/TS formatter, so
  `renderScriptOrStyle` initially threw for real JS-type `<script>` content
  instead of dispatching it — superseded (commits
  `a3c5c81`/`7cca3a4`/`679fafb`) once JS/TS shipped; see the HTML5 checklist
  entry below for current dispatch behavior.

## Open Questions

- **SimpleBraced (json, json5, css) comment-grouping parity with curly —
  RESOLVED (RDD_KEY_265, 2026-08-08 session).** `FormatterSimpleBraced`
  gained `normalizeComment`/`normalizeCommentTrivia`/
  `normalizeLineCommentChain`/`normalizeBlockComment` (+ private
  `tryBannerShape`): consecutive standalone JSON5 `//` comments (no blank
  line between) chain-group like curly's `//` (only first capitalized, sole
  trailing `.` stripped only across the whole chain); a multi-line `/* */`
  already in ` * `-per-line banner shape gets the same single-unit
  treatment; any other multi-line shape falls back to the pre-existing
  whole-comment-scan behavior (an early version left it untouched instead —
  caught by `css_comments` regressing). Wired into
  `JsonSpecificRule.collectTrivia` (defers normalization until the whole
  leading-trivia span is collected, via a parallel raw-text + blank-before
  list) and `CssSpecificRule` (block-comment-only — CSS never lexes `//`).
  `test/json5_comments_out.json5` regenerated (two adjacent `//` comments
  with no `.` between now legitimately merge into one chain); new fixture
  `test/json5_comment_banner_{inp,out}.json5`. `make test`: 254/254 ->
  255/255 forward + idempotency, zero regressions.

- **YAML/TOML `#`-chain-grouping parity with curly — RESOLVED (RDD_KEY_266,
  2026-08-08 session).** New `ToolingCommentNormalizer.normalizeChain`
  (shared cross-job with the tooling job's makefile/bash/powershell
  `#`-grouping, RDD_KEY_267): consecutive standalone `#` comments with no
  blank line between chain-group like curly's `//`. `YamlSpecificRule`
  reused its existing per-comment `CommentLine.blankBefore` tracking via a
  new `finalizeComments` deferred-normalization step; `TomlSpecificRule`
  gained a parallel `pendingCommentBlank` list it lacked before, plus its
  own `finalizeComments`. New fixtures `yaml_comment_chain_{inp,out}.yaml`,
  `toml_comment_chain_{inp,out}.toml`; 5 pre-existing YAML fixtures
  regenerated for correctly-grouped output. `make test`: 255/255 ->
  257/257 forward + idempotency, zero regressions. Closes decisions #1 and
  #2 of the 2026-08-08 brief for this job — no open items remain from it.

- **HTML5 Test-Fixture Repos winnowing — RESOLVED (user, 2026-07-24).**
  `twbs/bootstrap` docs (Astro/MDX), `mdn/content` (Markdown), `whatwg/html`
  (Nim-macro source), `kangax/html-minifier` (JS unit tests with inline
  HTML strings) all dropped — no real HTML. Lesson: verify actual file
  contents via `gh api` before trusting a candidate's name/reputation.
  Three replacements added: `web-platform-tests/wpt`,
  `WordPress/wordpress-develop`, `alexandersandberg/html5-elements-tester`.

- **WordPress magic-comment capitalization — RESOLVED 2026-07-30.** Found:
  `normalize-comment-start-case=on`'s `normComment` unconditionally
  capitalized lowercase-starting comments (no directive-shape exclusion,
  unlike CSS's `isSingleTokenDirective`), rewriting WordPress magic comments
  `<!--more-->`/`<!--nextpage-->` to `<!--More-->`/`<!--Nextpage-->`
  (`WordPress/wordpress-develop` HTML5 dogfood, 2026-07-24). Fix (user,
  2026-07-30): went broad rather than an allow-list —
  `XmlSpecificRule.isSingleWordDirective` now skips capitalization whenever
  a comment's entire trimmed body is one word with no interior whitespace
  (no `:`/`-` requirement, unlike CSS's narrower rule), after a real-corpus
  check across `WordPress/wordpress-develop`/`web-platform-tests/wpt`/
  `alexandersandberg/html5-elements-tester` found zero genuine one-word
  prose comments. Accepted false-negative risk (e.g. a genuine `<!--todo-->`
  now also stays lowercase), documented in `README.md`'s Known Limitations.
  `make test`: 219/219 forward, 219/219 idempotency.

- **WordPress magic-comment padding — RESOLVED (RDD_KEY_329, 2026-08-21).**
  Found: even after the capitalization fix above, `isSingleWordDirective`
  comments were still being padded with interior spaces at render time
  (`<!--more-->` -> `<!-- more -->`), rewriting the exact literal byte
  sequence the capitalization exception was meant to preserve — same
  third-party-consumer breakage, just via padding instead of case. Root
  cause was in `XmlSpecificRule`'s render layer (four separate hardcoded
  `" <!-- " + text + " -->"` call sites), not `normComment` (which only
  ever controlled text content, never padding). Fix: added a single
  `wrapComment(String)` helper that reuses the existing
  `isSingleWordDirective` check and renders such comments tight
  (`<!--more-->`), routed all four render sites through it. Fixture:
  `test/real_code_regressions_225_{inp,out}.html`. `make test`: 336/336
  forward + 336/336 idempotency, zero regressions. `README.md`'s Known
  Limitations §2 updated to reflect that these comments are now genuinely
  byte-identical, not just uncapitalized.

- **HTML5 optional/implied end tags — RESOLVED in stages (RDD_KEY_198/199/
  200; `alexandersandberg/html5-elements-tester` dogfood, 3 sequential
  blockers, user decisions 2026-07-24).** (1) RDD_KEY_198:
  `OPAQUE_IMPLIED_END_TAG_ELEMENTS` (`ruby`) captures the whole element
  opaque-verbatim through its matching close tag. (2) RDD_KEY_199:
  `parseAttr`'s `lang.isHtml5` branch accepts unquoted attribute values.
  (3) RDD_KEY_200: `IMPLIED_CLOSE_TRIGGERS` table (element -> sibling
  start-tags that implicitly close it), `option` -> `{option, optgroup}`.
  File now completes end-to-end on all four checks (forward, round2,
  idempotency, syntax-check, content-diff).

- **HTML5 multi-line `<!-- -->` comments collapse to a tight single/
  pulled-up-`-->` form — RESOLVED (RDD_KEY_232, 2026-08-03).** Fix: detect
  a newline in the comment's raw (pre-trim) interior and freeze that
  interior byte-for-byte, reusing existing `Node.commentVerbatim`/
  verbatim-`commentText` (originally for `%`-prefixed directive comments) —
  its `COMMENT` render case already emits `<!--` + text + `-->` with zero
  added spacing. Only the comment's own interior freezes; node placement/
  indentation and sibling reindentation unaffected. Single-line comments
  unaffected (still trim/normalize). Shared `XmlSpecificRule.java` path
  (XML and HTML5). Updated `test/xml_comments_out.xml` and every other
  pre-existing fixture whose copyright-header comment had pinned the old
  collapsed-form bug; new fixture
  `test/html_multiline_comment_verbatim_{inp,out}.html`. `make test`:
  237/237 forward + idempotency, zero regressions. Full text: `RDD_KEY_232`.

- **HTML5 deep tree-construction edge cases — item 1 was split off to
  `STATE_HTML5_TCG.md` and has since been fully implemented there; items 2
  and 3 below are both FIXED here. Full diagnosis kept for history.**

  1. **`web-platform-tests/wpt` residual gaps — DEFERRED, then IMPLEMENTED
     in the grouped job `STATE_HTML5_TCG.md` (split off 2026-08-02, tc gap
     job in `CLAUDE.md`'s routing table; now authoritative for this gap —
     kept here only as a compact history pointer.** After 4 bugs fixed in
     the initial `html/syntax/` session (EOF-implied-close; `<image>`->
     `<img>` rewrite; `<head>`/`<body>` implied-close-trigger; `<xmp>`
     raw-text) and a follow-up (`real_code_regressions_110`: generalized
     `<image>` into `TAG_NAME_REWRITES`, broadened the tolerant-close
     fallback from EOF-only to any mismatched/unrecognized closing tag,
     fixing 3 of 9 residual files), remaining gaps (catalogued by category,
     not re-verified live — no network access): foster-parenting-driven tree
     reshaping (`foreign_content_009/010.html`); misnested `<form>`
     reconstruction inside `<template>`; implicit `<body>` start-tag
     insertion (content after an implicitly-closed `<head>` with no
     `<body>` start tag ever written — distinct from the already-fixed
     `<head>`/`<body>` implied-close trigger). A separate crash site
     (raw-text elements whose literal closing tag never appears before EOF)
     was found and FIXED (`real_code_regressions_111`) — capture-verbatim
     instead of throwing. All three remaining gaps share one prerequisite:
     an explicit open-elements-stack + HTML5 insertion-mode state machine
     (currently implicit in the Java call stack) — comparable in size/risk
     to `STATE_COMMON.md`'s "general scope-depth reindentation"
     Architectural TODO (recommended order: implicit `<body>` insertion
     first/narrowest, then foster-parenting, then misnested
     `<form>`-in-`<template>`, then adoption agency last). Real-world impact
     low — every dogfood corpus run so far formatted cleanly with zero
     structural loss; these gaps only bite WPT's deliberately pathological
     conformance fixtures. 2026-07-26 investigation confirmed implicit
     `<body>` insertion can't be peeled off standalone (requires
     fabricating a tag absent from source — first tag-synthesis path in an
     otherwise preserve-as-written formatter — and threading state across
     recursive `parseNodes`/`parseElement` calls to avoid
     double-insertion); status quo (RDD_KEY_185: bare top-level content
     reindents as an ordinary sibling) doesn't corrupt output, just isn't
     spec-faithful. Tag-name case-folding was fixed standalone (item 3
     below). **UPDATE:** all three gaps were subsequently implemented in
     `STATE_HTML5_TCG.md` as levels 1-3 (implicit `<body>` insertion,
     foster-parenting, misnested `<form>`-in-`<template>`) — landed and
     full-suite dogfood re-validated with zero regressions, opt-in behind
     `html5-tc-gap-level` (default `0`, off). No longer an open gap here;
     see `STATE_HTML5_TCG.md` for each level's implementation notes/known
     limitations.

  2. **`apache/ant` `manual/running.html` — FIXED (RDD_KEY_223, 2026-08-01).**
     Narrower self-contained fix, not item 1's full insertion-mode-state
     prerequisite.
     **Found:** bare top-level text after `<h2>` closes, ending in an orphan
     `</p>` with no matching open `<p>` anywhere (different shape than
     RDD_KEY_204's *open*-`<p>`-closed-by-sibling case). `parseNodes`'s
     `stopAtCloseTag` was unconditional, so the orphan `</p>` broke out of
     `<body>`'s children loop into the tolerant-close fallback (same
     mechanism WPT's `charset/after-bogus.html` needs), implicitly closing
     `<body>` without consuming the `</p>` token — cascaded to `<html>` too,
     so output closed `</body></html>` at the orphan `</p>` and dumped the
     rest of the real document as raw siblings after `</html>`. Bigger than
     the "1 `<p>` lost" content-diff originally reported, since
     `html_content_diff.py` (via `parse5`) re-parents post-`</html>` content
     back inside `<body>` almost transparently, masking magnitude — **note
     for anyone touching `html_content_diff.py`: it can under-report this
     class of serialization-level structural bug**, spot-check raw output
     text directly.
     **Fix:** `XmlSpecificRule` gained an `openTagStack` (`Deque<String>` of
     currently-open lowercased tag names, pushed/popped around
     `parseElement`) and `peekCloseTagNameLower`; `parseNodes`'s
     `stopAtCloseTag` branch now cascade-closes only if the closing tag's
     name is found anywhere in the stack (`lang.isHtml5` only — strict
     XML/XHTML unchanged), else discards the stray/orphan closing tag in
     place and continues. Discard-only (no `<p></p>` synthesis), not full
     per-spec behavior — accepted, matches "preserve as written, don't
     fabricate tags" posture (RDD_KEY_185); full reasoning in RDD_KEY_223.
     Fixtures `test/real_code_regressions_173_{inp,out}.html` (minimal
     orphan-`</p>` repro) and `test/real_code_regressions_174_{inp,out}.html`
     (regression guard for the mismatched-tag-cascades-to-ancestor path,
     `charset/after-bogus.html` idiom via unclosed `<bogus>`/`</div>`).
     `make test`: 221/221 -> 223/223 forward + idempotency, zero
     regressions. `apache/ant manual/` 226-file corpus re-run: 226/226
     forward + idempotency + `html_syntax_check.sh` clean; content-diff
     confirms structural corruption gone (`</body></html>` now at genuine
     end) — pre-existing "1 `<p>` lost" (`<body>` child count 82 vs 81,
     accepted discard-vs-synthesize gap) unchanged residual, not a
     regression; 4 other files' pre-existing comment-capitalization
     mismatches confirmed identical against a pre-fix baseline.

     **Residual gap closed too (RDD_KEY_236, 2026-08-03, user-directed).**
     The discard-only choice above was superseded: browsers don't discard an
     orphan `</p>` — the real HTML5 "p end tag" algorithm synthesizes an
     empty `<p></p>` at that point (spec-mandated for parser-state
     interoperability, not primarily rendering). New
     `XmlSpecificRule.synthesizeEmptyElement` builds a synthetic empty
     `ELEMENT` node; both orphan-close-tag discard sites in `parseNodes`
     (the `stopAtCloseTag` branch and the document-root-level stray-tag
     branch) now call it when the discarded tag name is `"p"`, other names
     unchanged. Fixture `real_code_regressions_173_out.html` updated (gained
     the synthesized `<p></p>`). `make test`: 238/238 forward + idempotency.
     Full `apache/ant manual/` 226-file re-run: 226/226 forward +
     idempotency + `html_syntax_check.sh` clean; direct `<p` tag-count check
     on `running.html` confirms exactly one `<p></p>` synthesized (60 vs.
     original 59) — `<body>` child count now matches the browser's 82,
     closing the "1 `<p>` lost" residual.

  3. **Tag-name case-folding — DONE, fixed standalone**
     (`real_code_regressions_112`, commit `10b20cf`, user, 2026-07-25). New
     `XmlSpecificRule.SVG_TAG_NAME_CASE_FIXUP` map holds the spec's "Adjust
     SVG tag names" table (`altglyph`->`altGlyph`, `lineargradient`->
     `linearGradient`, `fegaussianblur`->`feGaussianBlur`, `foreignobject`->
     `foreignObject`, etc.), gated on `svgDepth > 0` (mutually exclusive with
     `TAG_NAME_REWRITES`'s `svgDepth == 0`). Surfaced a latent bug in
     `parseElement`'s closing-tag match (rewritten `tagName` no longer
     matched the source's differently-cased closing tag), fixed via new
     case-insensitive `startsWithCloseTagIgnoreCase`, HTML5-gated only.
     **MathML's `definitionurl`->`definitionURL` fixup: FIXED** — new
     `mathmlDepth` tracking mirrors the `svgDepth` pattern; fixture
     `test/html_mathml_case_fixup_{inp,out}.html`. `make test`: 161/161 ->
     282/282 (cumulative with other fixtures added since).

- **HTML5 commented-out markup-fragment comment corruption — RESOLVED
  (RDD_KEY_224, 2026-08-01).** Found: re-verifying `real_code_regressions_125`'s
  4 "comment-capitalization-only" diffs from the `apache/ant manual/` corpus
  showed 2 of 4 were NOT benign: `Tasks/antlr.html`/`Tasks/attrib.html` each
  contain a commented-out HTML fragment (`<!--tr> <td>fork</td>...</tr-->`,
  `<!--p>By default...</p-->`) where the author's `<!--`/`<` boundary landed
  mid-tag, leaving the comment content starting with a bare tag-name-open
  fragment (`tr>`/`p>`); `normalize-comment-start-case` capitalized these to
  `Tr>`/`P>`, corrupting the commented-out markup (3 mismatches). Different
  shape from both existing directive-shape precedents (CSS's
  `isSingleTokenDirective`, HTML5's `isSingleWordDirective`) — multi-word,
  starting with markup syntax rather than prose.
  **Fix:** new `XmlSpecificRule.isMarkupFragmentDirective(text)` — true iff
  `text`'s leading run of lowercase letters is immediately followed by `>`
  (no interior whitespace) AND that run is a member of a new closed
  `MARKUP_FRAGMENT_TAG_NAMES` set of real HTML5 tag names (a set
  restriction, not "any lowercase word immediately followed by `>`",
  deliberately avoiding false positives on coincidental short words); wired
  into `normComment` alongside `isSingleWordDirective`. Evidence: grepped
  `apache/ant manual/`, `WordPress/wordpress-develop`, and
  `alexandersandberg/html5-elements-tester` for `<!--[a-z]+>` — exactly 2
  `<!--tr>` + 1 `<!--p>` corpus-wide, matching the 3 real diffs, zero
  false-positive-shaped hits (`web-platform-tests/wpt` not re-checked,
  orthogonal to its tree-construction gaps). The other 2 of the original 4
  diffs (`Tasks/imageio.html`/`Tasks/image.html`, identical string
  `attributes inherited from MatchingTask` -> `Attributes inherited from
  MatchingTask`) checked separately and confirmed genuine (if coincidentally
  lowercase-starting) prose, correctly excluded from this fix and left to
  ordinary capitalization. New fixture
  `test/real_code_regressions_175_{inp,out}.html` (both fragment shapes plus
  real `<p>`/`<tr>` regression guards). `make test`: 223/223 -> 224/224
  forward + idempotency, zero regressions. `apache/ant manual/` 226-file
  corpus re-run: 226/226 forward + idempotency + `html_syntax_check.sh`;
  content-diff mismatches down from 4 to 3 (see Real-Code Testing Results
  below).

## Checklist

All six sub-formats DONE, real logic, `make test` green. Per-format
implementation summary (bugs found during initial local-fixture
development, not dogfood — see "Real-Code Testing Results" below for
per-repo dogfood bugs):

- [x] **JSON/JSON5 (§1).** `JsonTokenizer` (extends `TokenizerSimpleBraced`,
      RDD_KEY_190): strings (incl. JSON5 single-quote/backslash-newline
      continuations), numbers, unquoted identifiers, `//`/`/* */` comments.
      `JsonSpecificRule`: recursive-descent parser+printer for §1.1
      colon-alignment and §1.2 tight/loose array padding. No frozen-span
      mechanism (whole-file `--format-off` only); malformed input throws
      `JsonParseException`. Fixtures: `json_core_*`, `json5_core_*`,
      `json5_comments_*`. Local bugs found+fixed: objects only render tight
      with exactly ONE member (2+ always loose, unlike arrays); trailing
      commas silently dropped in tight rendering. Also added:
      `normalize-comment-start-case`, block-comment reindentation (shared
      w/ CSS), JSON5 `key /* comment */ : value` mid-comment handling.
      `make test`: 95/95.
- [x] **XML (§2).** `XmlSpecificRule.java`: character-cursor recursive-
      descent parser (no natural line boundary in tag grammar, no
      `TokenizerCore` reuse). `Node` AST (`PI`/`DOCTYPE`/`COMMENT`/
      `ELEMENT`/`TEXT`/`CDATA`/`FROZEN`): PIs/DOCTYPE/CDATA opaque/verbatim;
      attribute order preserved; text/CDATA-only elements inline; empty
      pairs unexpanded; overflow wraps one attribute/line, `>` attached to
      last attribute line (judgment call, follows IntelliJ/Prettier-XML
      convention). `<!--% JXM_CFMT_DIS/ENA -->` frozen spans + comment-case
      normalization implemented independently. Rule constructor takes
      `indentStyle` (unlike YAML/TOML — §2.1 has no ignored-setting
      exception). Fixtures: `xml_combined_*`, `xml_comments_*`. Local bug
      found+fixed: childless overflowing tags never checked line length.
      `make test`: 202/202. **Known simplification, not exercised by
      current fixtures:** no text reflow (only attributes wrap) — see
      below, this one is intentionally out of scope, do not implement.
      **Disposition (2026-08-10):** documented in `README.md`'s Known
      Limitations → "Tag-based family (XML/HTML5)"; removed from `XL.txt`
      TIER 9 Dogfood (it was never actually a dogfood-corpus finding, and is
      permanent, not a live TODO).

      **Mixed text+element content splitting onto separate lines — FIXED
      (2026-08-04).** Real correctness bug, not just style: an element
      interleaving a non-whitespace TEXT node and an ELEMENT node (e.g.
      `<p>Click <a href="x">here</a> to continue.</p>`) rendered each child
      on its own indented line, inserting newlines/indentation into content
      whose whitespace can be semantically significant (XHTML-like prose,
      Android string resources with embedded `<b>`/`<a>` markup, DocBook,
      SVG `<text>`). **Design decision (with the user):** mixed content now
      renders **inline, exactly as originally written, with no reflow**,
      even past `line-length` — mirrors the existing opaque/preserve-
      verbatim posture for DOCTYPE/PI (§2.3), CDATA (§2.4 default case), and
      multi-line comments (§2.5); an overflowing mixed-content line is an
      accepted limitation.
      **Fix:** `XmlSpecificRule.parseElement` captures a new
      `Node.mixedContentRaw` field: remembers the cursor position right
      after the opening tag (`childStart`); once `parseNodes` returns, if
      the children are "mixed" (new `isMixedContent` helper — at least one
      non-whitespace TEXT node AND one ELEMENT node among the same
      siblings) AND the raw span from `childStart` to the closing tag has no
      newline, that trimmed span is stored verbatim as `mixedContentRaw`.
      `renderElement` checks this field before its `soleContentChild` fast
      path and, when set, emits `<tag attrs>` + the verbatim raw span +
      `</tag>` as one line — reconstructing from the original source span
      (not re-deriving child strings via the pretty-printer) is what makes
      nested mixed content (e.g. `<i>` containing its own `<em>`) fall out
      naturally. **The no-newline-in-original-span condition is
      deliberate** — without it a block container whose bare text-node
      siblings already spanned multiple source lines (e.g. a `<div>` with a
      prose line before a `<ul>`) would also match "mixed" and incorrectly
      collapse onto one long line (`test/html_combined_*` caught this when
      the condition was first omitted); restricting to already-single-line
      content means only genuine inline text-flow prose collapses,
      preserving RDD_KEY_185's pre-existing block-level reindent behavior.
      **Interaction found+fixed with HTML5 tc-gap level 4 (adoption
      agency):** preserving a misnested formatting element's raw span
      verbatim (e.g. `<b>one<i>two</b>` from `<b>1<i>2</b>3</i>`) leaves the
      same misnesting literally present in round1 output, so round2 could
      re-trigger `reconstructFormattingElement` against the sibling round1
      already produced, wrapping it in ANOTHER synthetic clone and breaking
      idempotency (`html_tc_gap_level4_adoption_agency_out.html` caught
      this). Fixed in `parseNodes`'
      `pendingReconstructFormattingTemplate` consumption: before calling
      `reconstructFormattingElement`, skip whitespace and check (via
      `startsWithTriggerTag`) whether the cursor already sits at a literal
      start tag matching the template's tag name — if so, skip synthesizing
      a new wrapper. Fixtures updated to correct (inline, not split) output:
      `xml_mixed_content_*` (new), plus `html_combined_out.html`,
      `html_tc_gap_level3_adoption_unchanged_out.html`, and
      `html_tc_gap_level4_adoption_agency_out.html`. `make test`: 242/242 ->
      243/243 forward + idempotency, zero regressions.

      **§2.4 CDATA-inside-`<style>` dispatch exception — IMPLEMENTED
      (2026-08-04).** The `<script>` side (unwrap CDATA, dispatch to JS/TS,
      re-wrap) already landed alongside the `<script>`/`<style>` dispatcher
      itself (HTML5 checklist entry below, commits
      `a3c5c81`/`7cca3a4`/`679fafb`); the `<style>` branch had no matching
      check and fed a `<style><![CDATA[ ... ]]></style>` block's literal
      CDATA markers into the CSS parser as bogus content. Fixed by giving
      `<style>` the same detect/unwrap-CDATA/dispatch-to-`CssSpecificRule`/
      re-wrap treatment `<script>` already had, matching that precedent
      rather than inventing a second convention. Known limitation carried
      over (not new): formatted CSS output containing the literal sequence
      `]]>` would prematurely terminate the CDATA section on re-wrap —
      accepted as an extremely rare edge case, not worth escaping machinery.
      Fixture: extended `test/html_comments_inp.html`/`_out.html` with a
      `<style id="cdata-style"><![CDATA[ ... ]]></style>` block (that
      fixture already covered the CDATA-wrapped `<script>` idiom and opaque
      CDATA elsewhere, closing the last of the three cases). `make test`:
      242/242 forward + idempotency, zero regressions.

      **`indent-style = auto` — IMPLEMENTED (2026-08-04).** Previously
      unhandled: `FormatterXml.formatOne` passed `config.indentStyle()`
      straight through to `XmlSpecificRule`'s constructor (only checked
      `"tabs".equals(indentStyle)`), so a literal `"auto"` silently fell
      through to spaces; `Main.java`'s own `formatStandalone` "auto"
      resolution doesn't cover XML either (`IndentationDetector.detect`'s
      directory sampling never looks at `.xml`). Fixed by resolving `"auto"`
      at the `XmlSpecificRule` construction call site: when
      `config.indentStyle()` is `"auto"`, calls the previously-unused
      `IndentationDetector.detectFromContent(content)` (scans for the first
      indented line's leading character, falls back to
      `Config.DEFAULT_INDENT_STYLE` if none found/on I/O error) and passes
      the resolved concrete style to the rule constructor. New fixtures
      `test/xml_indent_auto_tabs_{inp,out}.xml` (tab-indented, tabs
      preserved), `test/xml_indent_auto_spaces_{inp,out}.xml`
      (space-indented, spaces preserved), `test/xml_indent_auto_fallback_
      {inp,out}.xml` (minified, no hint, falls back to
      `Config.DEFAULT_INDENT_STYLE` = spaces), all via
      `<!--% JXM_CFMT_CFG indent-style=auto -->`. `make test`: 239/239 ->
      242/242 forward + idempotency, zero regressions.

      **Multi-line `<!-- -->` banner-shape parity with curly's `/* */` —
      IMPLEMENTED (RDD_KEY_264, 2026-08-08).** See RDD_KEY_264 above.
      `make test`: 253/253 -> 254/254 forward + idempotency, zero
      regressions.
- [x] **CSS (§3).** `CssTokenizer` (extends `TokenizerSimpleBraced`)
      deliberately coarse-grained (WHITESPACE/NEWLINE/COMMENT_BLOCK/
      STRING/PUNCT + one OP run for everything else); `CssSpecificRule`'s
      parser reconstructs header/value text from token concatenation +
      paren-depth tracking. One recursive `parseBlockBody`/`Rule`/`Decl`
      AST covers plain rules, at-rules, and native-nesting `&` blocks
      uniformly. Colon-alignment reuses `padKeysForColonAlignment`. No
      frozen-span mechanism initially (same posture as JSON/JSON5).
      Fixture: `css_combined_*`. Follow-up (same session):
      `normalize-comment-start-case`, block-comment reindentation,
      `prop /* comment */ : value` mid-comment handling (excluded from
      alignment groups); `/*% JXM_CFMT_DIS */`/`ENA` frozen spans via
      `TokenizerCore.markFrozenSpans` reuse on the CSS token list.
      Fixture: `css_comments_*`. `make test`: 95/95. **Deferred:** the
      curly family's classifier-backed keyword-exclusion comment
      normalization (`MiscRuleCore`) deliberately NOT reused — CSS/JSON
      have no keywords needing titlecasing protection.
- [x] **HTML5 (§4)** (RDD_KEY_194). `XmlSpecificRule.java` extended (shares
      XML's parser internally per RDD_KEY_188, gated on `lang.isHtml5`):
      §4.1 void elements self-closing with bare `>`; bare boolean
      attributes accepted; §4.3 `<pre>` content captured as `RAW` node,
      preserved byte-for-byte; `<script>`/`<style>` content captured raw
      via `finishRawTextElement`. §4.2 dispatch: `<style>` splices to
      `CssSpecificRule.format` and back. `<script>`: non-JS-MIME
      `type="..."` stays opaque; originally (before `STATE_JS_TS.md`
      shipped) threw `XmlParseException` directing the caller to freeze
      JS-type blocks via `//% JXM_CFMT_DIS`/`ENA`, with
      `isFrozenScriptContent` detecting that pair anywhere in the raw
      content (not just first line, for the CDATA-wrapped idiom).
      **Since superseded** (commits `a3c5c81`/`7cca3a4`/`679fafb`):
      `renderScriptOrStyle` now actually dispatches JS-type blocks to
      `FormatterCore.forLanguage("js")` and splices the result back (CDATA
      unwrap/rewrap, Config-threading handled); `isFrozenScriptContent`
      remains only as an explicit opt-out. Local bug found+fixed (general,
      not HTML5-specific): self-closing/void elements never checked line
      length or wrapped attributes on overflow (RDD_KEY_193 gap) — fixed
      with the same fits-check + wrap branch used elsewhere. Fixtures:
      `html_combined_*`, `html_comments_*` (both `<script>` blocks
      temporarily `//%`-frozen until JS/TS landed, since removed/
      re-verified real). `make test`: 212/212 (+2 HTML5 pairs from 202,
      zero regressions).
- [x] **YAML (§5).** `YamlSpecificRule.java`: from-scratch line-based
      recursive-descent parser (indentation-significant grammar,
      RDD_KEY_189/191): `parseBlock` recurses per indentation level;
      `parseKeyItem`/`parseSeqItem` handle block scalars, flow values,
      anchors, sequence-of-mappings; custom `FlowNode`/`FlowScalar`/
      `FlowMap`/`FlowSeq`/`FlowCursor` AST+parser for `{...}`/`[...]`.
      Colon-alignment reuses `padKeysForColonAlignment`; §5.4
      flow-preserved-unless-overflow recursive per nesting level; §5.3
      sequence-of-mapping alignment uses fixed 2-column dash offset. `#%`
      frozen spans/comment normalization from scratch. `FormatterYaml`
      omits `indentStyle` from its rule constructor (§5.1 mandates spaces
      always).
- [x] **TOML (§6).** `TomlSpecificRule.java`: simpler flat, non-recursive,
      non-indented single-pass line scanner (nesting via dotted
      table-header names, e.g. `[a.b]` — no recursive block parser
      needed). `ValueNode`/`Scalar`/`Entry`/`Arr`/`Tbl`/`ValueCursor`
      AST+parser handles array/inline-table values. §6.3 tight/loose is
      purely structural (tight iff every element is a `Scalar`, no
      line-length check unlike YAML). §6.4 inline tables always
      single-line (grammar constraint). `=`-alignment and `#%` frozen-span/
      comment logic was structurally identical to YAML's; the shared parts
      (comment splitting/normalization, alignment-group padding) were
      factored into `rules/YamlTomlSharedRule.java` — **RESOLVED,
      RDD_KEY_280** (see Resolved Design Decisions). `FormatterToml` mirrors
      `FormatterYaml`.
      **2026-08-16 cleanup-pass candidate — RESOLVED, RDD_KEY_302** (full
      detail in the RDD table above): `TomlSpecificRule.bracketBalance`/
      `findAssignmentEquals` and `YamlSpecificRule.findMappingColon`
      consolidated into `YamlTomlSharedRule.scanQuoteAwareBracket`. A first
      attempt using a packed-int return caused a real regression (unmatched
      `]` drove `depth` negative, colliding with a legitimate found-index,
      silently truncating output — `real_code_regressions_83_out.yaml`);
      fixed via an explicit `BracketScanResult` holder instead. Verified:
      `make test`/`make test-server` 323/323 clean; real-code dogfood
      idempotency pass against `cpython` (86 `.toml` + 38 `.yaml`) and
      `azure-pipelines-tasks` (55 `.yaml`) showed byte-identical round1
      output pre/post-consolidation, with one pre-existing (reproduces on
      unmodified code too) non-idempotency case,
      `cpython/Misc/stable_abi.toml` (blank-line-count drift), left out of
      scope.
- [x] **YAML/TOML local fixtures** authored ahead of implementation, then
      verified against real logic and uncommented in the Makefile:
      `yaml_core_*`, `yaml_comments_*`, `toml_core_*`, `toml_comments_*`.
      Bugs found+fixed: (1) **YAML silent-data-loss**: `parseKeyItem`'s
      child-block trigger required strictly-deeper indent, but YAML allows
      a sequence's `-` items at the *same* indent as their parent mapping
      key — the miss silently dropped the rest of the document; fixed by
      allowing `next.indent >= ln.indent` for sequence-item children. (2)
      **YAML idempotency**: key extraction didn't `.trim()`, so re-parsing
      the formatter's own aligned output widened alignment on the second
      pass; fixed with `.trim()`. (3) **TOML idempotency**: the flat line
      scanner assumed one physical line per `key = value`, but §6.3's
      loose-array output is intentionally multi-line, causing
      `unterminated array` on re-parse; fixed with a quote-aware
      `bracketBalance` helper that consumes additional physical lines when
      brackets are unbalanced. Also corrected one fixture-authoring error
      (`yaml_core_out.yaml`'s nested `endpoints` flow array — per §5.4's
      recursive-per-depth rule it fits under `line-length` and should stay
      flow; fixture corrected to match the correct implementation).
- [x] **Real-code testing pass** per `STATE_COMMON.md`'s methodology,
      against every Test-Fixture Repo above. **ALL SIX FORMATS DONE, every
      listed repo dogfooded.** Shared methodology: clone fresh (or reuse a
      prior-session checkout); exclude build/vendor/generated dirs; sample
      above a per-format threshold (JSON/CSS: "several thousand+"; XML/
      YAML: "several hundred+"), else process the full set. Every run:
      baseline syntax-check → round1 format (`--preserve-tree --root`) →
      round2 → `diff -rq` idempotency → syntax-check round1 vs baseline →
      format-specific content-preservation check. A
      comment-capitalization-only diff (`normalize-comment-start-case=on`)
      recurs across nearly every run below and is expected/correct per the
      `real_code_regressions_69` precedent, not a bug.

      **Results, per format/repo (final state — bug fixture IDs and
      commits kept for traceability, debugging narration trimmed):**

      **JSON/JSON5 (4/4 done):** `json5/json5` (6 files) — zero bugs.
      `microsoft/vscode` (1272/1377) — 1 idempotency bug (`parseContainer`
      dangling placeholder for a comment-less blank line before `}`
      defeated the tight-`{}` short-circuit only on round2), fixed,
      `real_code_regressions_68`, `e2a6f0e`. `babel/babel` (810/9245
      sampled), `eslint/eslint` (91/98) — zero bugs.

      **XML (4/4 done):** `apache/maven` (398/3158) — zero bugs (wrote
      `xml_content_diff.py` this session). `w3c/svgwg` (294/298, incl.
      `.svg`) — 1 bug: `Lang.infer` never mapped `.svg`→`xml`, fixed,
      `real_code_regressions_74`, `3408acd`. `apache/ant` (214/558, incl.
      `.xsd`/`.xsl`) — same shape bug for `.xsd`/`.xsl`, fixed,
      `real_code_regressions_91`/`_92`, `25bd5b8`. `jenkinsci/jenkins`
      (130/131) — zero bugs (3 files hit an unrelated `xml_content_diff.py`
      minidom limitation on embedded control chars). All final re-runs
      clean; content-preservation diffs comment-capitalization-only.

      **CSS (4/4 done):** `twbs/bootstrap` (31 in-scope) — 1 bug: rtlcss
      directive comments (`/* rtl:begin:ignore */`) capitalized to
      `/* Rtl:... */`, breaking rtlcss though still valid CSS
      (`css_syntax_check.js` missed it); fixed via `isSingleTokenDirective`
      (skip capitalization when a comment's first-line body is one
      whitespace-free `:`/`-`-containing token). `real_code_regressions_69`,
      `8f5f597`. Final 31/31 clean. `necolas/normalize.css` (1 file),
      `primer/css` (2 files) — zero bugs. `foundation/foundation-sites` —
      100% `.scss`, no run.

      **TOML (4/4 done):** `rust-lang/cargo` (670/672) — 2 forward-pass
      crash bugs: (1) interior per-array-element comment swallowed the
      array's `]` (continuation-joining loop only stripped a trailing `#`
      comment from the fully-assembled line); fixed by stripping each
      continuation line's own comment before joining. (2) `"""..."""`
      multi-line strings unhandled in the flat scanner; fixed by capturing
      as an opaque verbatim span (same as JSON5's). `real_code_regressions_70`,
      `d56eb3a`. `python-poetry/poetry` (106), `pola-rs/polars` (57),
      `toml-lang/toml` (1, spec prose only) — zero bugs each. All final
      re-runs clean.

      **YAML (6/6 done, completes all 6 planned repos):**
      - `kubernetes/kubernetes` (453/6366 sampled) — 6 sequence/mapping
        bugs, all fixed: same-indent sequence child under its first
        mapping key not allowed; wrapping quoted/unquoted scalars crashed
        the parser (fixed via opaque multi-line-scalar capture); dangling
        comment-only (null-key) item NPE'd colon-alignment padding;
        block-scalar continuation indent stored absolute not relative to
        key (idempotency); block scalar as a plain sequence item's value
        rendered empty (no-colon branch missed block-scalar header),
        surfacing a related dash-anchor render-offset bug.
        `real_code_regressions_71`, `fff5a3f`/`025af9f`. Final 453/453 clean.
      - `docker/compose` (250/261) — 1 bug: blank line after a keyed line
        with no inline value made all four "has child block" checks miss
        it via plain `peek()`; fixed with `peekNonBlank()`.
        `real_code_regressions_72`, `2640cf2`. Final clean (1 pre-existing
        PyYAML-emoji tool gap, not a formatter bug).
      - `ansible/ansible` (415/2110 sampled) — 3 bugs, all fixed: plain
        sequence item's unquoted scalar wrapping had no continuation
        handling (mirrors keyed case); comment dedented below enclosing
        block's indent made `parseBlock` break without consuming it,
        orphaning everything after (fixed by looking past the comment);
        bare top-level plain-scalar document (`$ANSIBLE_VAULT;...` blob)
        kept only its first line (fixed by emitting remaining raw lines
        verbatim). `real_code_regressions_73`, `9f2a80a`. Final 415/415 clean.
      - `actions/starter-workflows` (186/188) — 1 bug (output-invalid,
        caught by syntax-check): `parseSeqItem` hardcoded sibling-key
        column as `ln.indent + 2`; extra padding after a dash misidentified
        the next sibling key as a nested child; fixed deriving the real
        column from the dash line's actual leading whitespace.
        `real_code_regressions_75`, `f1648c5`. Final clean (7 files hit
        the known PyYAML-emoji gap).
      - `prometheus/prometheus` (375/380) — 4 data-loss bugs, all caught
        only by content-preservation and all fixed, sharing the
        "dash/key line whose value is absent/comment-only/anchor-only,
        real content on a deeper subsequent line" shape: flow-looking
        inline-value checks didn't confirm the flow actually closed on the
        same line, truncating an unbalanced multi-line `[...]` opener
        (also a `- # comment`-only dash line dropped subsequent real
        mapping keys); anchor-only dash line followed by a nested mapping
        at equal (not greater) indent lost its child block;
        `renderFlowValue` rendered an empty flow map/seq as a block
        conversion when it didn't fit, but `renderFlowBlock` has nothing
        to iterate for zero entries, vanishing the value.
        `real_code_regressions_83`. Final 375/375 clean.
      - `home-assistant/core` (878/921) — 1 bug: compact single-line
        nested-sequence items (`- - a\n  - b`) not recognized by
        `parseSeqItem`, mismatching sibling indent, causing `parseBlock`
        to break out early and drop rest of sequence + later siblings at
        every level (6 files); fixed via new `parseInlineNestedSeq` helper
        reusing the existing `item.children` render path.
        `real_code_regressions_86`, `e7f0334`. Final 870/878 clean (8
        files hit the known PyYAML-emoji gap).

      **HTML5 (all candidates done or dropped, completes the repo list and
      the overall six-format real-code-testing pass):**
      - `twbs/bootstrap` docs, `mdn/content`, `whatwg/html`,
        `kangax/html-minifier` — dropped, no real HTML5 corpus (see Open
        Questions).
      - `h5bp/html5-boilerplate` (4 files) — zero bugs (wrote
        `html_content_diff.py` this session).
      - `WordPress/wordpress-develop` (73/303, real markup only) — 1 bug:
        `renderElement`'s multi-child block-closing render path never
        emitted `n.trailingComment` (unlike the other three render
        branches), dropping a same-line trailing comment right after a
        block element's closing tag; fixed by routing that path through
        `appendWithTrailing` too. `real_code_regressions_103`. Final 73/73
        clean (2 comment-text-mismatch diffs left open at the time — since
        RESOLVED 2026-07-30, see the "WordPress magic-comment
        capitalization — RESOLVED" entry above; not an independently open
        item, do not re-flag).
      - `alexandersandberg/html5-elements-tester` (1 file, 42KB) — 3
        sequential blockers fixed across sessions (RDD_KEY_198/199/200,
        detailed above). File completes end-to-end on all four checks.
      - `web-platform-tests/wpt` (scoped to `html/syntax/`, 416 files
        pulled via `gh api` tree-walk + `raw.githubusercontent.com` since
        the repo is ~2.6GB/6552 files and this system's `git` can't
        `--filter=blob:none --sparse`; 386 in-scope after baseline) — 4
        bugs in the initial session, `real_code_regressions_109`: EOF no
        longer implicitly closes every still-open element (dominant
        pattern, 73/85 initial failures); `<image>`→`<img>` rewrite scoped
        to HTML content only via `svgDepth` counter; `<head>` registered
        in `IMPLIED_CLOSE_TRIGGERS` to close on a sibling `<body>` start
        tag; `<xmp>` wasn't recognized as raw-text like
        `<pre>`/`<script>`/`<style>`, mis-parsing literal
        `<script>...</script>` text inside it as a real nested element (64
        files). Follow-up (`real_code_regressions_110`) generalized
        `<image>` into `TAG_NAME_REWRITES` and broadened the tolerant-close
        fallback from EOF-only to any mismatched/unrecognized closing tag,
        fixing 3 of 9 residual failures. Final full re-run (377 in-scope):
        forward/idempotency/syntax-check 377/377 clean; content-
        preservation 127/377 diffs, all comment-capitalization-only.
        Second follow-up (`real_code_regressions_111`) fixed a distinct
        crash site: raw-text elements whose literal closing tag never
        appears before real EOF now capture verbatim instead of throwing.
        Remaining residual/deep tree-construction gaps: see Open Questions
        above.
      - `apache/ant`'s `manual/` (226 files, light supplement, run
        2026-07-26): forward/round2/idempotency 226/226 clean,
        `html_syntax_check.js` 0/226 failures. Content-preservation found 2
        bugs, both fixed: `<p>` missing from `IMPLIED_CLOSE_TRIGGERS`
        causing a spurious duplicate `</p>` after an unclosed `<p>`
        (RDD_KEY_204); a same-line trailing comment inside a sole-text-
        child element (e.g. `<td>`) dropped because `renderNode`'s `TEXT`
        case and `renderElement`'s sole-content-child inline fast path
        skipped `appendWithTrailing` (RDD_KEY_205). `real_code_regressions_125`.
        Re-run: 221/226 clean, 4 comment-capitalization-only, 1 genuine
        unfixed gap (`running.html` — Open Questions item 2). **2026-08-01
        re-verification (RDD_KEY_224):** direct re-check of the 4
        "comment-capitalization-only" diffs found 2 of them
        (`Tasks/antlr.html`, `Tasks/attrib.html`) were actually
        commented-out markup fragments being corrupted, not benign prose —
        fixed, see Open Questions below. Re-run after the fix: 226/226
        forward + idempotency + `html_syntax_check.sh`; content-diff
        mismatches down from 4 to 3 (`antlr.html`/`attrib.html` now clean;
        `imageio.html`/`image.html`'s matching-string diff confirmed a
        genuine, unrelated doc-authoring convention; `running.html`'s
        RDD_KEY_223 residual gap unchanged).
