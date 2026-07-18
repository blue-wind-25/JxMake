# STATE_DATA_FORMATS.md — Data & Markup Format JAR Support Tracker

Read `STATE_COMMON.md` first — it has the shared commit/ambiguity/testing
conventions this file assumes. `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` (the
other jobs' files) are NOT required reading for this one — only
`STATE_COMMON.md` is.

---

## Purpose

Tracks implementation of data/markup format support in the deterministic JAR
formatter (`util/CodingStyle.md/formatter/`), per `STYLE_DATA_FORMATS.md`
(JSON/JSON5, XML, CSS, HTML5, YAML, TOML). **JSON/JSON5 (§1) and CSS (§3) are
DONE -- real tokenizer, recursive-descent parser, and printer landed for
both, `make test` green (see Checklist; CSS still has a few deferred edge
cases, see its checklist entry). XML, HTML5, YAML, and TOML remain
scaffold-only: dispatch exists only as a "not yet implemented" error thrown
for these formats, no real formatting logic exists yet.**

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

Scaffold dispatch lives in the shared `Lang.java`/`Main.java`/
`ServerMode.java`/`Config.java`, described in the routing `CLAUDE.md`
table. This job's own per-sub-format rule classes —
`rules/JsonSpecificRule.java` (JSON/JSON5) and `rules/CssSpecificRule.java`
(CSS) have real logic; `rules/YamlSpecificRule.java`,
`rules/TomlSpecificRule.java`, and `rules/XmlSpecificRule.java` (XML/HTML5)
still exist only as boilerplate stubs (each constructor throws
`UnsupportedOperationException`) — no real logic yet.

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
| RDD_KEY_193 | XML real-logic implementation: character-cursor recursive-descent parser (no natural line boundary in tag grammar, no `TokenizerCore` reuse); independent `<!--%`-based frozen-span/comment-normalization logic; `InFileConfig` extended for `<!--% JXM_CFMT_CFG ... -->`; migrated `xml` out of `Lang.SCAFFOLD_ONLY_LANGUAGES` into `Lang.SUPPORTED_LANGUAGES` (HTML5 stays scaffold-only); unlike YAML/TOML, XML's rule constructor takes `indentStyle` (§2.1 has no ignored-setting exception); wrap-shape judgment call (closing `>` attached to last attribute line); one bug found+fixed (childless-tag overflow wrap never triggered); all 4 fixtures pass `make test`, 202/202 total. |

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
commitment to implement it — see FUTURE_FEATURE_DISCUSSION.md for status):

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

Planned local dogfood pairs (distinct from the external-repo list above,
which is for corpus-scale validation) are staged in
**FUTURE_TEST_FIXTURES.md**, under its "JSON", "JSON5", "XML", "CSS", and
"HTML5" sections — not duplicated here. See that file for the pair list and
what each covers. Once authored, register pairs in the Makefile's
`INP_FILES` / `test/README.txt`, and empty out FUTURE_TEST_FIXTURES.md's
relevant sections accordingly.

**YAML/TOML are already authored** (`yaml_core_inp/out.yaml`,
`yaml_comments_inp/out.yaml`, `toml_core_inp/out.toml`,
`toml_comments_inp/out.toml`, all in `formatter/test/`, described in
`test/README.txt`), skipping the FUTURE_TEST_FIXTURES.md staging step since
there was no pre-existing draft for either format. They are hand-drafted
against `STYLE_DATA_FORMATS.md` §5/§6, not verified by a real JAR (scaffold-
only), and are **commented out** of the Makefile's `INP_FILES` so `make
test` stays green — uncomment once `YamlSpecificRule.java`/
`TomlSpecificRule.java` have real logic and the drafts have been reviewed
against it.

---

## Class Scoping (post Core/Curly/Indent/Tags refactor)

- **XML/HTML5** are tag-based (`Lang.isTagBased()`). Their eventual
  `Tokenizer`/`Formatter` extend `TokenizerTags`/`FormatterTags`. **Resolved
  (RDD_KEY_188):** no separate `HtmlTokenizer`/`HtmlFormatter` — XML and
  HTML5 share the same `*Tags` classes gated internally on `lang.isHtml5`,
  mirroring how curly classes branch on `isKotlin` today. Concrete XML/
  HTML5-only rule logic (§2/§4) lands in a single `XmlSpecificRule.java`
  (boilerplate stub created, throws `UnsupportedOperationException` until
  real logic lands), gating HTML5-only additions (void elements, the
  `<script>`/`<style>` embedded-content dispatcher) internally on
  `lang.isHtml5`.
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
  YAML/TOML-only "Flat" family (no braces at all) — do not conflate the two
  when YAML/TOML land. CSS remains a scaffold stub (`CssSpecificRule.java`)
  until its own session. Now that concrete YAML/TOML style rules exist
  (§5/§6, RDD_KEY_191), the "Flat" family placeholder name still stands as
  the working name for their eventual `Lang.isSimpleBraced`-sibling
  predicate — YAML's colon-alignment (§5.2) and TOML's `=`-alignment (§6.1)
  are structurally different enough from JSON/CSS's brace-delimited grouping
  (no enclosing `{}`/`[]` at the top level, indentation- and header-driven
  instead) that they should NOT simply join `SimpleBraced` when implemented;
  this is not yet a class-scoping decision that needs resolving before
  implementation starts, only a naming note for whoever picks up YAML/TOML's
  checklist items.
- Implementation order is unchanged by the refactor: JSON/JSON5 → CSS → XML
  → HTML5 (HTML5 last, depends on both CSS and JS/TS support).
- **HTML-before-JS/TS contingency:** if HTML5 implementation is reached
  before `STATE_JS_TS.md`'s job has landed a real JS/TS formatter, the
  `<script>` splice-out step must pass embedded script content through
  opaque (preserved verbatim, re-indented only) rather than attempting to
  dispatch to a not-yet-existing JS/TS formatter class.

## Open Questions

None recorded yet in this file.

---

## Checklist

- [x] **Implement JSON/JSON5 first (simplest grammar).** DONE. `JsonTokenizer`
      (extends new `TokenizerSimpleBraced`, RDD_KEY_190) tokenizes strings
      (incl. JSON5 single-quote and backslash-newline continuations, kept as
      one opaque token per §1.3), numbers (incl. JSON5 hex/leading sign),
      unquoted identifiers, `//`/`/* */` comments. `JsonSpecificRule` is a
      recursive-descent parser building a small object/array/scalar AST with
      per-item leading-comment/blank-line trivia, then a printer implementing
      §1.1 (colon alignment per contiguous group, broken by blank
      line/comment/dangling-trailing-comment) via the new
      `FormatterSimpleBraced.padKeysForColonAlignment` shared helper, and
      §1.2 (tight atoms-only arrays that fit `line-length`, loose otherwise
      or when containing an object/array). `FormatterJson` (new
      `FormatterCore.forLanguage` branch, `isJson || isJson5`) wires
      `line-length`/`indent-size`/`indent-style` from `Config`; whole-file
      `--format-off` returns content unchanged (JSON/JSON5 have no per-region
      frozen-span mechanism the way curly-family `//%`/`/*%` markers do).
      Malformed input throws `JsonSpecificRule.JsonParseException`, caught
      generically by `Main`'s existing per-file error handling (same as any
      other rule class's runtime failure) — exercised manually with an
      unterminated `{bad`, produced a clean non-zero-exit error, no crash.
      `Lang.isSupported`/`SUPPORTED_LANGUAGES` gained `json`/`json5`;
      `isScaffoldOnly`/`SCAFFOLD_ONLY_LANGUAGES` dropped them — no `Main.java`/
      `ServerMode.java` changes needed since both already gate generically
      off those two lists. Fixtures: `test/json_core_{inp,out}.json` (plain
      RFC 8259: colon groups, tight/loose arrays, empty object/array) and
      `test/json5_core_{inp,out}.json5` (unquoted keys, single-quoted
      strings, hex/negative numbers, comment- and blank-line-broken groups,
      opaque multi-line string, trailing comment before close), both
      registered in the Makefile's `INP_FILES` and `test/README.txt` ahead of
      `real_code_regressions_*`. `FUTURE_TEST_FIXTURES.md` (at
      `util/CodingStyle.md/FUTURE_TEST_FIXTURES.md`, one directory above
      `formatter/`) *does* exist and holds hand-drafted, explicitly
      "unverified" fixture content for not-yet-authored pairs across every
      job — check it (and strip its markdown bullet-list 2-space indent
      before transcribing) before hand-writing new fixtures from scratch.
      Follow-up session found and fixed two real bugs the doc-sourced
      fixtures exposed that the original thinner ad-hoc ones hadn't: an
      object can only render tight when it has exactly ONE member (2+-member
      objects always render loose regardless of fit/nesting, unlike arrays);
      trailing commas were being silently dropped by both tight-array and
      tight-object rendering paths. Also added: `normalize-comment-start-case`
      (`FormatterSimpleBraced.capitalizeCommentStart`) and block-comment
      reindentation (`FormatterSimpleBraced.reindentBlockComment`, shared
      with CSS), plus JSON5's `key /* comment */ : value` mid-comment
      handling (`Item.midComment`, excluded from alignment groups). Fixture:
      `test/json5_comments_{inp,out}.json5` (group broken then re-merged by
      a comment, multi-line comment reindented, comment inside an array,
      mid-comment before colon, comment-case normalization), registered the
      same way. `make test`: 95/95 forward + 95/95 idempotency, zero
      regressions.
- [x] **Implement XML support (§2).** DONE. `XmlSpecificRule.java` is a
      from-scratch character-cursor recursive-descent parser (NOT line-based
      like YAML, and NOT a `TokenizerCore` reuse -- XML's tag/attribute
      grammar has no natural line boundary and is unrelated to brace-
      delimited imperative grammars). A `Node` AST (`PI`/`DOCTYPE`/`COMMENT`/
      `ELEMENT`/`TEXT`/`CDATA`/`FROZEN`) covers: `<?...?>` PIs and
      `<!DOCTYPE ...>` (bracket-depth-aware scan so a DOCTYPE's internal
      subset, e.g. nested `<!ENTITY ...>`, doesn't terminate the scan early)
      preserved fully opaque/verbatim; `<![CDATA[ ]]>` preserved opaque/
      verbatim as an element's sole inline content; attribute order
      preserved exactly as parsed (including `xmlns`/`xmlns:foo`), rendered
      with normalized single-space separation but original quote
      characters; an element whose only child is text or CDATA renders
      inline on one line (`<tag>text</tag>`); an empty open/close pair
      renders unexpanded (`<tag></tag>`, never collapsed to self-closing);
      an element with child elements renders multi-line, recursing one
      indent level deeper per nesting level; an overflowing tag (open line
      beyond `line-length`) wraps each attribute onto its own line one
      indent deeper than the tag, with the closing `>` attached directly to
      the last attribute line (not on its own line) -- resolved by judgment
      call, since §2.2 doesn't spell out the exact wrap shape, following
      common real-world XML formatter convention (IntelliJ/Prettier-XML);
      this applies even to an empty (childless) overflowing tag, which still
      needs its own line-length check before falling into the "nothing
      between the tags" render path. `#%`-equivalent `<!--% JXM_CFMT_DIS -->`/
      `ENA` frozen spans are detected by checking whether the *current
      source line* (not the whole file) trims to exactly that marker, then
      capturing raw lines verbatim until the matching `ENA` line -- same
      independent-per-format-family approach as YAML/TOML (RDD_KEY_192), not
      a reuse of `TokenizerCore.markFrozenSpans`. A same-line trailing
      `<!-- ... -->` comment (checked via "does `-->` appear before the next
      newline") is captured separately from a comment that is its own
      sibling node, and both get comment-start-case normalization
      (`normComment` -- simpler than YAML/TOML's, since XML's comment inner
      text has no leading delimiter character like `#`/`//` to skip past).
      Also extended `InFileConfig`'s `JXM_CFMT_CFG` directive regex/preamble
      pattern to recognize the `<!--% JXM_CFMT_CFG ... -->` form (matching
      §2's stated single-directive-syntax convention), mirroring the earlier
      YAML/TOML/`#`-comment extension. `FormatterXml.java` created as the
      `FormatterCore` dispatch sibling (unlike YAML/TOML, XML's rule
      constructor *does* take `indentStyle` -- §2.1 explicitly says XML uses
      the existing global `indent-size`/`indent-style` config with no
      XML-specific override or ignored setting, unlike YAML's spaces-always
      rule). `Lang.SUPPORTED_LANGUAGES`/`isSupported` gained `xml`;
      `Lang.SCAFFOLD_ONLY_LANGUAGES`/`isScaffoldOnly` dropped it (HTML5
      stays scaffold-only -- it will share `XmlSpecificRule` internally per
      RDD_KEY_188, but its own void-element/`<script>`-`<style>`-dispatch
      additions aren't written yet). Fixtures: `test/xml_core_{inp,out}.xml`
      and `test/xml_comments_{inp,out}.xml` (described above/in
      `test/README.txt`), uncommented in the Makefile's `INP_FILES`. `make
      test`: 202/202 forward + idempotency, zero regressions. One
      implementation bug found and fixed against the fixtures: the initial
      wrap logic only handled overflow for elements with a non-empty
      `children` list, so `<longtag>`'s overflow (an empty, childless tag
      whose open-tag line alone exceeds `line-length`) fell through to the
      untouched "empty pair" render path and never wrapped at all -- fixed
      by moving the fits/overflow check above the empty-vs-non-empty
      children branch and adding a childless overflow-wrap path that skips
      straight to the closing tag with no child-rendering step in between.
      **Known simplifications, not exercised by current fixtures:** no
      content/text reflow or wrapping (only attributes wrap on overflow);
      mixed content (text interleaved with child elements) renders each
      contiguous text run as its own line rather than staying inline next to
      sibling elements; `indent-style = auto` is not detected from the
      file's existing indentation (falls back to configured spaces/tabs);
      the CDATA-inside-`<script>`/`<style>`-unwraps-and-dispatches exception
      (§2.4) is not implemented (would require JS/TS or CSS dispatch from
      inside the XML pipeline, and JS/TS is still scaffold-only).
- [x] **Implement CSS support (§3).** DONE. `CssTokenizer` (extends
      `TokenizerSimpleBraced`) is deliberately coarse-grained -- emits
      WHITESPACE/NEWLINE/`/* */` COMMENT_BLOCK/STRING/PUNCT (`{}();:,&`) and
      one contiguous OP run for everything else (selector/value text,
      property names, at-rule keywords); `CssSpecificRule`'s parser
      reconstructs header/value text by concatenating token text and
      tracking paren-depth, rather than modeling CSS's selector/value
      grammar token-by-token. A single recursive `parseBlockBody`/`Rule`/
      `Decl` AST covers plain rules, at-rules (`@media`/`@supports`/
      `@keyframes`/`@font-face`), and native-nesting `&` blocks uniformly --
      any header text terminated by `{` recurses into the same body parser,
      giving at-rules and `&` blocks their own independent colon-alignment
      group one level deeper for free, no special-casing needed per
      at-rule kind. Colon-alignment groups reuse
      `FormatterSimpleBraced.padKeysForColonAlignment`, broken by blank
      lines/comments/a Rule-vs-Decl boundary, with the item breaking a group
      itself becoming the start of the next group (same fix shape as
      JSON's group-boundary bug, applied proactively here after finding the
      identical bug live during fixture testing — see below).
      `FormatterCss` (new `FormatterCore.forLanguage` branch, `isCss`) wires
      `line-length`/`indent-size`/`indent-style` from `Config`; whole-file
      `--format-off` returns content unchanged (CSS has no per-region
      frozen-span mechanism, same posture as JSON/JSON5).
      `Lang.isSupported`/`SUPPORTED_LANGUAGES` gained `css`;
      `isScaffoldOnly`/`SCAFFOLD_ONLY_LANGUAGES` dropped it.
      Fixture: `test/css_combined_{inp,out}.css` (three-member group broken
      by a comment then re-merging into a four-member group, `--gap` custom
      property joining an ordinary group, `@media`/`@supports`/`@font-face`/
      `@keyframes` at-rules each starting an independent nested group,
      `&:hover`/`& .icon` native nesting recursing the same way), registered
      in the Makefile's `INP_FILES` and `test/README.txt` ahead of
      `real_code_regressions_*`.
      Comment-handling follow-up (same session): `normalize-comment-start-case`
      (lightweight `FormatterSimpleBraced.capitalizeCommentStart`, same as
      JSON5) applied to leading/trailing comments and header-embedded
      comments; multi-line block comments reindented to their new structural
      depth via `FormatterSimpleBraced.reindentBlockComment`; a
      `prop /* comment */ : value` mid-comment (comment wedged before the
      colon) extracted onto `Decl.midComment`, excluded from colon-alignment
      groups, rendered `prop + " " + midComment + " : " + value`; comments
      between a selector and its `{` left embedded in the header text
      as-is; `/*% JXM_CFMT_DIS */`/`ENA` per-region frozen spans implemented
      by reusing `TokenizerCore.markFrozenSpans` directly on the CSS token
      list before parsing, then capturing the frozen token run (plus the
      single line-separator newline before it, so the marker's own original
      indentation is preserved byte-for-byte) as opaque `Item.rawFrozen`
      text emitted verbatim. Fixture: `test/css_comments_{inp,out}.css`
      (multi-line comment breaking a group, only its first sentence
      capitalized; a `JXM_CFMT_DIS`/`ENA` pair freezing a declaration's
      original spacing/indentation; a trailing comment before a block's
      closing `}`; a comment between a selector and its `{`; a
      `prop /* comment */ : value` mid-comment; a comment as the sole
      content before declarations inside a native-nesting `&:hover` block),
      registered the same way. `make test`: 95/95 forward + 95/95
      idempotency, zero regressions.
      **Deferred, not yet implemented:** the curly family's heavier
      classifier-backed keyword-exclusion comment normalization
      (`MiscRuleCore`) is deliberately NOT reused here (see JSON5's own
      note) — CSS/JSON have no language keywords a comment could start with
      that would need protecting from titlecasing, so the lightweight
      version is sufficient and intentionally scoped smaller.
- [ ] Implement HTML5 support (§4), including the `<script>`/`<style>`
      embedded-content dispatcher (splice out, format via CSS/JS-TS, splice
      back with correct re-indentation) — depends on both CSS support above
      and JS/TS support (tracked in `STATE_JS_TS.md`, a separate job) being
      available before the `<script>` dispatch path can be exercised
      end-to-end.
- [x] **Implement YAML support (§5).** `YamlSpecificRule.java` implements a
      from-scratch line-based recursive-descent parser (NOT a reuse of
      `TokenizerCore`/`Token` — YAML's grammar is indentation-significant,
      not brace-delimited, per RDD_KEY_189/191): `parseBlock` recursively
      parses one homogeneous block (all-`-`-sequence or all-`key:`-mapping)
      per indentation level; `parseKeyItem`/`parseSeqItem` handle per-line
      shape (block scalars `|`/`>`, flow values, anchors + nested mappings,
      sequence-of-mappings); a custom `FlowNode`/`FlowScalar`/`FlowMap`/
      `FlowSeq`/`FlowCursor` AST+parser handles `{...}`/`[...]` flow
      collections (bare unquoted scalars like URLs containing `:`/`/` rule
      out reusing JSON's tokenizer). Colon-alignment groups reuse JSON's
      `FormatterSimpleBraced.padKeysForColonAlignment` algorithm; §5.4's
      flow-preserved-unless-overflow rule is recursive per nesting level
      (each nested flow collection gets its own independent line-length
      check at its own resulting depth); §5.3's sequence-of-mapping
      alignment uses a fixed 2-column offset past the dash (`"- "`.length()),
      independent of configured `indent-size`. `#%`-based `JXM_CFMT_DIS`/
      `ENA` frozen spans and comment-start-case normalization (`normComment`,
      starting the scan at index 1 to skip the `#`) are both implemented
      from scratch rather than reusing the `//`/`/*`-oriented
      `TokenizerCore.markFrozenSpans`/`FormatterSimpleBraced.capitalizeCommentStart`.
      `FormatterYaml.java` created as the `FormatterCore` dispatch sibling
      (deliberately omits `indentStyle` from the rule constructor — §5.1
      mandates YAML always uses spaces regardless of configured
      `indent-style`).
- [x] **Implement TOML support (§6).** `TomlSpecificRule.java` implements a
      much simpler flat, non-recursive, non-indented single-pass line
      scanner (TOML expresses nesting purely via dotted table-header names,
      e.g. `[a.b]`, never via indentation, per §6.2) — no recursive block
      parser needed, unlike YAML. A `ValueNode`/`Scalar`/`Entry`/`Arr`/`Tbl`/
      `ValueCursor` AST+parser handles array/inline-table values. §6.3's
      tight/loose array rule is purely structural (no line-length
      consideration, unlike YAML's flow-collection rule): an array is tight
      iff every element is a `Scalar` (no nested `Arr`/`Tbl`), regardless of
      length. §6.4 inline tables are always single-line (a TOML v1.0 grammar
      constraint, not a style choice — no tight/loose decision needed). `=`-
      alignment groups and `#%` frozen-span/comment-normalization logic are
      structurally identical to YAML's (duplicated rather than factored into
      a shared helper — accepted for now given time constraints, flagged as
      a possible future DRY improvement). `FormatterToml.java` created
      mirroring `FormatterYaml.java`.
      Both `YamlSpecificRule`/`TomlSpecificRule` moved out of
      `Lang.SCAFFOLD_ONLY_LANGUAGES` into `Lang.SUPPORTED_LANGUAGES` in this
      pass (`FormatterCore.forLanguage` gained `isYaml`/`isToml` dispatch
      branches), once both had real logic — same precedent as JSON/CSS's
      migration (RDD_KEY_190).
- [x] **YAML/TOML fixtures authored ahead of implementation, then verified
      against real logic and uncommented in the Makefile.**
      `test/yaml_core_{inp,out}.yaml`, `test/yaml_comments_{inp,out}.yaml`,
      `test/toml_core_{inp,out}.toml`, `test/toml_comments_{inp,out}.toml`
      (contents described in `test/README.txt`) are registered in the
      Makefile's `INP_FILES` and pass `make test` (idempotency + fixture
      diff) cleanly. Two implementation bugs were found and fixed against
      these fixtures:
      (1) **YAML silent-data-loss bug**: `parseKeyItem`'s child-block
      trigger used a strict `peek().indent > ln.indent` check, but YAML
      allows a sequence's `-` items at the *same* indent as their parent
      mapping key (e.g. `fruits:` / `- apple`, both indent 0 — a valid,
      common style). The strict check failed to trigger, leaving `- apple`/
      `- banana` unconsumed; the outer `parseBlock`'s shape-mismatch check
      then saw an unexpected sequence line and broke early, silently
      dropping the rest of the document (no exception). Fixed by allowing
      `next.indent >= ln.indent` specifically when the next line is a
      sequence item (a mapping child must still be strictly deeper, to
      avoid ambiguity with a sibling key at the parent's own indent).
      (2) **YAML idempotency bug**: `item.key = code.substring(0, colon)`
      (in both `parseKeyItem` and the sequence-of-mapping's `firstKey`) did
      not `.trim()` the key, so re-parsing the formatter's own aligned
      output (e.g. `name : widget`) captured `"name "` (trailing space
      before the colon) as the key instead of `"name"`, silently widening
      colon-alignment-group width computations on the second pass. Fixed by
      adding `.trim()` at both call sites.
      (3) **TOML idempotency bug**: the flat line scanner assumed every
      `key = value` line's value was fully contained on one physical line,
      but the formatter's own §6.3 loose-array output (e.g. `matrix = [` /
      `    [1, 2],` / `    [3, 4]` / `]`) is intentionally multi-line,
      causing `TomlParseException: unterminated array` when re-parsing it.
      Fixed by adding a quote-aware `bracketBalance` helper and consuming/
      concatenating additional physical lines (joined with a single space)
      whenever a `key = value` line's value portion has unbalanced
      brackets/braces, before calling `parseValue`.
      Also caught and fixed one fixture-authoring error during this pass:
      `test/yaml_core_out.yaml`'s nested `endpoints` flow array was
      hand-drafted expecting block conversion, but per §5.4's own
      recursive-per-depth rule its post-conversion rendering
      (`"  endpoints : [...]"`, 83 chars) fits under the default
      `line-length` of 100 and should correctly stay flow — only the outer
      `config` mapping (121 chars as one line) needed to convert. Fixture
      corrected to match the (correct) implementation rather than the other
      way around.
- [ ] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      `STYLE_DATA_FORMATS.md`'s listed test-fixture repos per sub-format
      (`json5/json5`/`microsoft/vscode`/etc. for JSON — still open, not yet
      run; `apache/maven`/etc. for XML; `twbs/bootstrap`/etc. for CSS;
      `h5bp/html5-boilerplate`/etc. for HTML5; `kubernetes/kubernetes`/etc.
      for YAML; `rust-lang/cargo`/etc. for TOML).
