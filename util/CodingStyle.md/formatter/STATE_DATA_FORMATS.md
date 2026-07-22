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

`html_sc.js` (`parse5`, `onParseError`) also exists for HTML5, but per-spec
HTML5 parsing is deliberately error-tolerant (e.g. auto-closes mismatched
tags rather than failing), so it only catches the narrow set of conditions
the spec defines as parse errors, not general malformed-markup the way the
XML checker does — documented as a caveat directly in the script.

All six were verified against hand-crafted good/bad pairs (malformed
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
      `apache/maven`/etc. still not started for XML;
      **`twbs/bootstrap` done for CSS** (see below), `necolas/normalize.css`/
      `foundation/foundation-sites`/`primer/css` still not started for CSS;
      `h5bp/html5-boilerplate`/etc. still not started for HTML5;
      `kubernetes/kubernetes`/etc. still not started for YAML;
      `rust-lang/cargo`/etc. still not started for TOML.
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
      `necolas/normalize.css`/`foundation/foundation-sites`/`primer/css`
      remain the last not-started CSS test-fixture repos for a future
      session.
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
      No new fixtures needed (nothing to regress-test). `eslint/eslint`
      remains the last unstarted JSON/JSON5 test-fixture repo for a future
      session.
