# STATE_DATA_FORMATS.md — Data & Markup Format JAR Support Tracker

Read `STATE_COMMON.md` first — it has the shared commit/ambiguity/testing
conventions this file assumes. `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` (the
other jobs' files) are NOT required reading for this one — only
`STATE_COMMON.md` is.

---

## Purpose

Tracks implementation of data/markup format support in the deterministic JAR
formatter (`util/CodingStyle.md/formatter/`), per `STYLE_DATA_FORMATS.md`
(JSON/JSON5, XML, CSS, HTML5, YAML, TOML). **All six are now DONE** -- real
tokenizer/parser/printer logic landed for each, `make test` green (see
Checklist for per-format notes and deferred edge cases; RDD_KEY_190/191/192/
193/194 for implementation history). The one remaining gap is HTML5's
`<script>` dispatch to JS/TS: real JS content must be wrapped in
`//% JXM_CFMT_DIS`/`ENA` until `STATE_JS_TS.md`'s job lands a real JS/TS
formatter (see that file's checklist and this file's HTML5 entry below).

---

## Scope

`STYLE_DATA_FORMATS.md` covers non-imperative data and markup formats — no
functions, no control flow, so most of the shared `STYLE.md` imperative-
language rules do not apply. Six sub-formats, each stating explicitly which
`STYLE.md` sections it borrows from:

1. **JSON / JSON5** (§1) — RFC 8259 JSON plus JSON5. Key/value `:` column
   alignment (borrows the group/group-break shape of declaration alignment,
   but always a space before `:`, unlike declaration `=`); bracket/
   complexity tight-loose padding (reuses the existing nesting-complexity
   signal); JSON5-only multi-line strings via backslash-newline continuation
   (opaque, preserved exactly as written, same treatment as Java text
   blocks / JS template literals). Plain `.json` has no comment syntax, so
   `JXM_CFMT_DIS`/`ENA`/`CFG` directives only work in `.json5`.
2. **XML** (§2) — XML 1.0 and dialects (XHTML, SVG, MathML, RSS, Atom,
   Android XML, Maven POM, Ant `build.xml`, IntelliJ XML). Uses existing
   global `indent-size`/`indent-style` config, no XML-specific indent
   config. Tag/attribute formatting (nesting indent, attribute wrap on
   overflow, self-closing tags preserved, attribute order preserved
   including `xmlns`). `<!DOCTYPE>`/processing instructions and CDATA are
   opaque/preserved verbatim, except CDATA that is the direct content of a
   `<script>`/`<style>` tag, which unwraps and dispatches per §4.2.
   Directives use the single `<!--% ... -->` block form (no line-comment
   equivalent in XML).
3. **CSS** (§3) — modern CSS (CSS 3+ modules). Property/value `:` column
   alignment (same group shape as JSON's, space-before-`:` convention);
   at-rules (`@media`, `@supports`, `@keyframes`, `@font-face`) and CSS
   Nesting Module `&` blocks are treated as headers that start their own
   independent alignment group, recursing per nested block. Directives use
   the single `/*% ... */` block form (CSS has no `//` line comment).
4. **HTML5** (§4) — tag formatting reuses XML's §2.2 rules directly, with
   one override: void elements (`area`, `base`, `br`, `col`, `embed`, `hr`,
   `img`, `input`, `link`, `meta`, `param`, `source`, `track`, `wbr`) never
   get a closing tag and any self-closing `/` is normalized away. The main
   design point is **dispatch**: `<style>` content splices out to the CSS
   formatter (§3) and back; `<script>` content splices out to the JS/TS
   formatter (see `STYLE_JS_TS.md`) and back, including the CDATA-wrapped
   variant; a `<script type="...">` with a non-JS/TS type is left opaque.
5. **YAML** (§5) — YAML 1.1/1.2, including multi-document streams. Key/value
   `:` column alignment (same group shape as JSON's, space-before-`:`
   convention); sequence items indent one level deeper than their parent
   mapping key; flow-style collections (`{a: 1}`, `[1, 2]`) preserved as
   written unless they'd exceed `line-length`, in which case converted to
   block style; anchors/aliases/tags and block scalars (`|`/`>`) preserved
   verbatim/opaque. Uses the existing global `indent-size`, but
   **`indent-style` is ignored for YAML** (YAML forbids tab indentation —
   always space-indented). Directives use the single `#% ...` line form (YAML
   has no block comment).
6. **TOML** (§6) — TOML v1.0. Key/value `=` column alignment reuses STYLE.md
   §5/§6's assignment-alignment shape directly (no forced space-before-`=`
   needed, unlike JSON/YAML's `:`); table/array-of-table headers
   (`[section]`/`[[array]]`) are headers, not declarations, and get no added
   indentation for the keys under them (TOML nests via dotted header names,
   not indentation); arrays reuse the tight/loose bracket rule; inline
   tables are always single-line (a TOML grammar constraint, not a style
   choice); dotted keys and string quote styles preserved as written.
   Directives use the single `#% ...` line form (TOML has no block comment).

Dispatch lives in the shared `Lang.java`/`Main.java`/`ServerMode.java`/
`Config.java`, described in the routing `CLAUDE.md` table. This job's
per-sub-format rule classes all have real logic now:
`rules/JsonSpecificRule.java` (JSON/JSON5), `rules/CssSpecificRule.java`
(CSS), `rules/YamlSpecificRule.java`, `rules/TomlSpecificRule.java`, and
`rules/XmlSpecificRule.java` (XML and HTML5, sharing one class per
RDD_KEY_188).

---

## Resolved Design Decisions

Full text of each decision lives in `RDD_LOG.md` (shared with
`STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` — continue the existing `RDD_KEY_n`
numbering, do not restart). See `STATE_COMMON.md`'s lookup convention
(`grep -Fm1`, no `-A`).

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

- **JSON/JSON5:** no new config keys beyond what's implied by §1.1's
  unconditional colon alignment (no toggle, matching STYLE.md §5/§6).
- **XML:** no new config beyond the existing global `indent-size` /
  `indent-style`.
- **CSS:** §3.1's property/value alignment is unconditional, same reasoning
  as JSON's — no config toggle.
- **HTML5:** no HTML5-specific config beyond the CSS/JSON keys above and
  whatever the JS/TS formatter defines in its own state file.
- **YAML:** uses the existing global `indent-size` (no YAML-specific
  default override — the global default of 4 stands). **`indent-style` is
  ignored/inapplicable for YAML** — YAML forbids tab indentation, so output
  is always space-indented regardless of the configured value (RDD_KEY_191).
  Local test fixtures for YAML should set `indent-size=2` via an in-file
  `#% JXM_CFMT_CFG indent-size=2` directive to exercise YAML's own
  community-standard indent width, rather than changing the tool-wide
  default (RDD_KEY_191).
- **TOML:** no new config beyond the existing global `indent-size` (unused,
  since §6.2 gives table-header keys no added indentation) /
  `indent-style` (governs any incidental indentation the same as elsewhere,
  though TOML rarely produces any).

---

## Test-Fixture Repos

Recorded here for regression testing once any of this is implemented (not a
commitment to implement it — see this file's own checklist above for current
per-language implementation status):

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
for corpus-scale validation) covering JSON, JSON5, XML, CSS, and HTML5 have
been authored and registered in `formatter/test/` — see `test/README.txt`
for the pair list and what each covers.

**YAML/TOML are also already authored** (`yaml_core_inp/out.yaml`,
`yaml_comments_inp/out.yaml`, `toml_core_inp/out.toml`,
`toml_comments_inp/out.toml`, all in `formatter/test/`, described in
`test/README.txt`), authored directly with no pre-existing draft for either
format. They are hand-drafted
against `STYLE_DATA_FORMATS.md` §5/§6, not verified by a real JAR (scaffold-
only), and are **commented out** of the Makefile's `INP_FILES` so `make
test` stays green — uncomment once `YamlSpecificRule.java`/
`TomlSpecificRule.java` have real logic and the drafts have been reviewed
against it.

---

## Class Scoping (post Core/Curly/Indent/Tags refactor)

- **XML/HTML5** are tag-based (`Lang.isTagBased()`). **Resolved
  (RDD_KEY_188):** no separate `HtmlTokenizer`/`HtmlFormatter` — XML and
  HTML5 share the same parser/renderer, gated internally on `lang.isHtml5`,
  mirroring how curly classes branch on `isKotlin` today. Concrete XML/
  HTML5-only rule logic (§2/§4) lands in a single `XmlSpecificRule.java`,
  now fully real (RDD_KEY_193/194), gating HTML5-only additions (void
  elements, the `<script>`/`<style>` embedded-content dispatcher) internally
  on `lang.isHtml5`.
- **JSON/JSON5/CSS** are neither tag-based, curly, nor indent-based per the
  `Lang.java` family predicates added in the refactor (flat/braced, no
  block-scoping semantics). **Resolved (RDD_KEY_189):** no new
  `TokenizerCore` sibling (no `TokenizerFlat`) — each extends `TokenizerCore`
  directly with a minimal override when implemented. JSON and JSON5 share
  one `JsonSpecificRule.java` (gated internally on `lang.isJson5` for
  JSON5-only additions), CSS gets its own `CssSpecificRule.java`. YAML and
  TOML (sharing the same "neither curly/indent/tag-based" characteristic,
  though not named in the original open question) get
  `YamlSpecificRule.java`/`TomlSpecificRule.java` on the same reasoning.
  **Resolved further (RDD_KEY_190), once JSON/JSON5 real logic landed:**
  JSON/JSON5 and CSS form a new "SimpleBraced" `Lang.isSimpleBraced` family —
  `TokenizerSimpleBraced` (shared `/* */` block-comment scan) and
  `FormatterSimpleBraced` (shared `padKeysForColonAlignment` group-padding
  computation, §1.1/§3.1's identical colon-alignment shape). `JsonTokenizer`
  extends `TokenizerSimpleBraced`; `FormatterJson` extends
  `FormatterSimpleBraced` and is `FormatterCore.forLanguage`'s new
  `isJson || isJson5` branch. This is distinct from the still-hypothetical
  YAML/TOML-only "Flat" family (no braces at all) — CSS later joined
  `SimpleBraced` for real (`CssSpecificRule.java` is fully implemented);
  YAML/TOML did NOT join `SimpleBraced` when they landed (RDD_KEY_192) —
  each implements its own from-scratch parser instead (line-based for YAML,
  flat single-pass line-scanner for TOML), confirming the "structurally
  different, don't conflate" naming note below was correct.
- Implementation order (all now complete): JSON/JSON5 → CSS → YAML/TOML →
  XML → HTML5 (HTML5 last, depended on both CSS and — for one exception,
  see below — JS/TS support).
- **HTML-before-JS/TS contingency — resolved differently than originally
  planned:** HTML5 landed (RDD_KEY_194) before `STATE_JS_TS.md`'s job has a
  real JS/TS formatter. Rather than the originally-planned silent opaque
  passthrough for embedded `<script>` content, `renderScriptOrStyle`
  explicitly throws `XmlParseException` for real (non-frozen) JS-type
  `<script>` content, directing the caller to wrap it in a
  `//% JXM_CFMT_DIS`/`//% JXM_CFMT_ENA` pair instead — chosen so real JS
  content is never silently mis-formatted-as-untouched. See the HTML5
  checklist entry and `STATE_JS_TS.md`'s cross-job follow-up note for what
  must happen once JS/TS support lands for real.

## Open Questions

None recorded yet in this file.

---

## Checklist

- [x] **Implement JSON/JSON5 (§1).** DONE. `JsonTokenizer` (extends
      `TokenizerSimpleBraced`, RDD_KEY_190) handles strings (incl. JSON5
      single-quote/backslash-newline continuations), numbers, unquoted
      identifiers, `//`/`/* */` comments. `JsonSpecificRule` is a
      recursive-descent parser + printer implementing §1.1 colon-alignment
      groups (via `FormatterSimpleBraced.padKeysForColonAlignment`) and §1.2
      tight/loose array padding. `FormatterJson` wires `line-length`/
      `indent-size`/`indent-style`; no frozen-span mechanism (whole-file
      `--format-off` only). Malformed input throws
      `JsonSpecificRule.JsonParseException`, handled generically by `Main`.
      `Lang.isSupported`/`SUPPORTED_LANGUAGES` gained `json`/`json5`.
      Fixtures: `test/json_core_{inp,out}.json`,
      `test/json5_core_{inp,out}.json5` (see `test/README.txt`).
      `FUTURE_TEST_FIXTURES.md` (one directory above `formatter/`) holds
      hand-drafted unverified fixture content for not-yet-authored pairs
      across every job — check it before hand-writing new fixtures.
      Bugs found+fixed: objects only render tight with exactly ONE member
      (2+ always loose, unlike arrays); trailing commas were silently
      dropped in tight-array/tight-object rendering. Also added:
      `normalize-comment-start-case`
      (`FormatterSimpleBraced.capitalizeCommentStart`), block-comment
      reindentation (`FormatterSimpleBraced.reindentBlockComment`, shared
      with CSS), JSON5 `key /* comment */ : value` mid-comment handling.
      Fixture: `test/json5_comments_{inp,out}.json5`. `make test`: 95/95
      forward + idempotency, zero regressions.
- [x] **Implement XML support (§2).** DONE. `XmlSpecificRule.java` is a
      from-scratch character-cursor recursive-descent parser (not line-based
      like YAML, no `TokenizerCore` reuse — XML's tag/attribute grammar has
      no natural line boundary). A `Node` AST (`PI`/`DOCTYPE`/`COMMENT`/
      `ELEMENT`/`TEXT`/`CDATA`/`FROZEN`) covers: PIs/`<!DOCTYPE>`/CDATA
      preserved opaque/verbatim; attribute order preserved as parsed;
      text/CDATA-only elements render inline; empty pairs unexpanded
      (`<tag></tag>`); overflowing tags wrap one attribute per line with `>`
      attached to the last attribute line (judgment call, §2.2 doesn't
      specify exact shape — follows IntelliJ/Prettier-XML convention),
      including childless overflow. `<!--% JXM_CFMT_DIS/ENA -->` frozen
      spans and comment-case normalization implemented independently (not a
      `TokenizerCore.markFrozenSpans` reuse), same per-format-family
      approach as YAML/TOML (RDD_KEY_192). `InFileConfig` extended for
      `<!--% JXM_CFMT_CFG ... -->`. `FormatterXml.java` — unlike YAML/TOML,
      XML's rule constructor takes `indentStyle` (§2.1 has no ignored-
      setting exception). `Lang.SUPPORTED_LANGUAGES`/`isSupported` gained
      `xml` (HTML5 stays scaffold-only). Fixtures: `test/xml_combined_*`,
      `test/xml_comments_*` (originally shipped as a mismatched `xml_core`
      pair, later corrected — see `test/README.txt`). `make test`: 202/202
      forward + idempotency, zero regressions. Bug found+fixed: childless
      overflowing tags never checked line length (fits/overflow check now
      runs before the empty-vs-non-empty children branch).
      **Known simplifications, not exercised by current fixtures:** no
      text reflow (only attributes wrap); mixed text+element content splits
      onto separate lines rather than staying inline; `indent-style = auto`
      not detected from existing indentation; §2.4's CDATA-inside-
      `<script>`/`<style>` dispatch exception not implemented (needs JS/TS
      or CSS dispatch from inside the XML pipeline).
- [x] **Implement CSS support (§3).** DONE. `CssTokenizer` (extends
      `TokenizerSimpleBraced`) is deliberately coarse-grained — emits
      WHITESPACE/NEWLINE/COMMENT_BLOCK/STRING/PUNCT and one OP run for
      everything else; `CssSpecificRule`'s parser reconstructs header/value
      text from token concatenation + paren-depth tracking rather than
      modeling CSS grammar token-by-token. A single recursive
      `parseBlockBody`/`Rule`/`Decl` AST covers plain rules, at-rules
      (`@media`/`@supports`/`@keyframes`/`@font-face`), and native-nesting
      `&` blocks uniformly (any `{`-terminated header recurses, giving
      at-rules/`&` blocks their own alignment group for free). Colon-
      alignment reuses `FormatterSimpleBraced.padKeysForColonAlignment`.
      `FormatterCss` wires `line-length`/`indent-size`/`indent-style`; no
      frozen-span mechanism initially (same posture as JSON/JSON5).
      `Lang.isSupported`/`SUPPORTED_LANGUAGES` gained `css`. Fixture:
      `test/css_combined_{inp,out}.css` (group-break/re-merge, at-rule
      nesting, `&` nesting — see `test/README.txt`).
      Comment-handling follow-up (same session): `normalize-comment-start-
      case` (`FormatterSimpleBraced.capitalizeCommentStart`), block-comment
      reindentation, `prop /* comment */ : value` mid-comment handling
      (`Decl.midComment`, excluded from alignment groups); `/*% JXM_CFMT_DIS
      */`/`ENA` per-region frozen spans implemented by reusing
      `TokenizerCore.markFrozenSpans` on the CSS token list before parsing.
      Fixture: `test/css_comments_{inp,out}.css`. `make test`: 95/95 forward
      + idempotency, zero regressions.
      **Deferred:** the curly family's classifier-backed keyword-exclusion
      comment normalization (`MiscRuleCore`) is deliberately NOT reused —
      CSS/JSON have no keywords needing titlecasing protection, so the
      lightweight version suffices.
- [x] **Implement HTML5 support (§4)** (RDD_KEY_194). `XmlSpecificRule.java`
      extended (not a new class — HTML5 shares XML's parser internally per
      RDD_KEY_188, gated on `lang.isHtml5`): §4.1 void elements parsed as
      self-closing leaves rendered with a bare `>`; bare boolean attributes
      accepted; §4.3 `<pre>` content captured as a new `RAW` node type,
      preserved byte-for-byte; `<script>`/`<style>` content captured as raw
      text via a new `finishRawTextElement` helper. §4.2 dispatch: `<style>`
      splices out to `CssSpecificRule.format` and back (fully real, CSS
      already implemented). `<script>`: a non-JS-MIME `type="..."` stays
      opaque; a recognized JS-type block has no JS/TS formatter to dispatch
      to yet (`STATE_JS_TS.md` still scaffold-only), so `renderScriptOrStyle`
      throws `XmlParseException` directing the caller to freeze the block
      instead of silently passing it through (deliberate refinement of the
      earlier "HTML-before-JS/TS contingency" note, which had proposed
      silent passthrough). Escape hatch: a script-content-specific frozen-
      span detector, `isFrozenScriptContent`, recognizing a `//%
      JXM_CFMT_DIS`/`//% JXM_CFMT_ENA` line pair anywhere in the raw content
      (not just the first line, to accommodate the CDATA-wrapped idiom's
      literal `<![CDATA[` first line) — see the cross-job follow-up note in
      `STATE_JS_TS.md`'s checklist for what must happen when JS/TS support
      lands (remove the two local fixtures' directive-wrapping, wire a real
      dispatch call, re-verify). Bug found+fixed (general, not HTML5-
      specific): self-closing/void elements never checked line length or
      wrapped attributes on overflow (RDD_KEY_193 gap) — fixed with the same
      fits-check + wrap branch used for non-self-closing tags.
      `Lang.SUPPORTED_LANGUAGES`/`isSupported` gained `html5`;
      `FormatterCore.forLanguage` routes `isHtml5` to `FormatterXml`
      alongside `isXml`. Two fixture-authoring mismatches corrected to match
      verified-real behavior (mixed content line-splitting per RDD_KEY_193;
      trailing comment spacing per `xml_comments_out.xml` precedent).
      `make test`: 212/212 PASS (up from 202, +2 HTML5 fixture pairs, zero
      regressions).
- [x] Author local test fixture pairs per `FUTURE_TEST_FIXTURES.md`'s "HTML5"
      section and register in the Makefile's `INP_FILES` / `test/README.txt`.
      Done: `html_combined_inp/out.html` and `html_comments_inp/out.html`
      extracted to `test/`, registered live in the Makefile (real logic now
      implemented, see the HTML5 entry above), documented in
      `test/README.txt`. Both fixtures' `<script>` blocks are temporarily
      wrapped in `//% JXM_CFMT_DIS`/`//% JXM_CFMT_ENA` since real JS/TS
      formatting doesn't exist yet — see `STATE_JS_TS.md`'s checklist for
      the required follow-up (remove the wrapping and re-verify) once that
      job lands.
- [x] **Implement YAML support (§5).** `YamlSpecificRule.java` is a
      from-scratch line-based recursive-descent parser (not `TokenizerCore`
      reuse — indentation-significant grammar, RDD_KEY_189/191):
      `parseBlock` recurses per indentation level; `parseKeyItem`/
      `parseSeqItem` handle block scalars, flow values, anchors, sequence-
      of-mappings; a custom `FlowNode`/`FlowScalar`/`FlowMap`/`FlowSeq`/
      `FlowCursor` AST+parser handles `{...}`/`[...]` flow collections.
      Colon-alignment reuses `FormatterSimpleBraced.padKeysForColonAlignment`;
      §5.4 flow-preserved-unless-overflow is recursive per nesting level;
      §5.3 sequence-of-mapping alignment uses a fixed 2-column dash offset.
      `#%`-based frozen spans and comment normalization implemented from
      scratch (not `TokenizerCore.markFrozenSpans` reuse). `FormatterYaml
      .java` omits `indentStyle` from its rule constructor (§5.1 mandates
      spaces always).
- [x] **Implement TOML support (§6).** `TomlSpecificRule.java` is a simpler
      flat, non-recursive, non-indented single-pass line scanner (nesting is
      via dotted table-header names, e.g. `[a.b]`, per §6.2 — no recursive
      block parser needed). A `ValueNode`/`Scalar`/`Entry`/`Arr`/`Tbl`/
      `ValueCursor` AST+parser handles array/inline-table values. §6.3
      tight/loose is purely structural (tight iff every element is a
      `Scalar`, no line-length check unlike YAML). §6.4 inline tables are
      always single-line (grammar constraint). `=`-alignment and `#%`
      frozen-span/comment logic are structurally identical to YAML's
      (duplicated, not factored into a shared helper — flagged as a possible
      future DRY improvement). `FormatterToml.java` mirrors `FormatterYaml
      .java`. Both moved from `Lang.SCAFFOLD_ONLY_LANGUAGES` into
      `SUPPORTED_LANGUAGES` (`FormatterCore.forLanguage` gained `isYaml`/
      `isToml` branches), same precedent as JSON/CSS's migration
      (RDD_KEY_190).
- [x] **YAML/TOML fixtures authored ahead of implementation, then verified
      against real logic and uncommented in the Makefile.**
      `test/yaml_core_{inp,out}.yaml`, `test/yaml_comments_{inp,out}.yaml`,
      `test/toml_core_{inp,out}.toml`, `test/toml_comments_{inp,out}.toml`
      (see `test/README.txt`) registered in the Makefile's `INP_FILES`,
      pass `make test` cleanly. Bugs found+fixed against these fixtures:
      (1) **YAML silent-data-loss**: `parseKeyItem`'s child-block trigger
      required strictly-deeper indent, but YAML allows a sequence's `-`
      items at the *same* indent as their parent mapping key — the miss
      silently dropped the rest of the document. Fixed by allowing
      `next.indent >= ln.indent` specifically for sequence-item children.
      (2) **YAML idempotency**: key extraction didn't `.trim()`, so
      re-parsing the formatter's own aligned output (`name : widget`)
      captured `"name "` as the key, widening alignment on the second pass.
      Fixed with `.trim()`.
      (3) **TOML idempotency**: the flat line scanner assumed each
      `key = value` was one physical line, but §6.3's loose-array output is
      intentionally multi-line, causing `unterminated array` on re-parse.
      Fixed with a quote-aware `bracketBalance` helper that consumes
      additional physical lines when brackets are unbalanced.
      Also corrected one fixture-authoring error: `yaml_core_out.yaml`'s
      nested `endpoints` flow array was hand-drafted expecting block
      conversion, but per §5.4's recursive-per-depth rule it fits under
      `line-length` and should stay flow — fixture corrected to match the
      (correct) implementation.
- [ ] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      `STYLE_DATA_FORMATS.md`'s listed test-fixture repos per sub-format
      (`json5/json5`/`microsoft/vscode`/etc. for JSON — still open, not yet
      run; `apache/maven`/etc. for XML; `twbs/bootstrap`/etc. for CSS;
      `h5bp/html5-boilerplate`/etc. for HTML5; `kubernetes/kubernetes`/etc.
      for YAML; `rust-lang/cargo`/etc. for TOML).
