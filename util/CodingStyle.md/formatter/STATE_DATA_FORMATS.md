# STATE_DATA_FORMATS.md — Data & Markup Format JAR Support Tracker

Read `STATE_COMMON.md` first — it has the shared commit/ambiguity/testing
conventions this file assumes. `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` (the
other jobs' files) are NOT required reading for this one — only
`STATE_COMMON.md` is.

---

## Purpose

Tracks implementation of data/markup format support in the deterministic JAR
formatter (`util/CodingStyle.md/formatter/`), per `STYLE_DATA_FORMATS.md`
(JSON/JSON5, XML, CSS, HTML5). **JSON/JSON5 (§1) and CSS (§3) are DONE --
real tokenizer, recursive-descent parser, and printer landed for both,
`make test` green (see Checklist; CSS still has a few deferred edge cases,
see its checklist entry). XML and HTML5 remain scaffold-only: dispatch
exists only as a "not yet implemented" error thrown for these formats, no
real formatting logic exists yet.**

---

## Scope

`STYLE_DATA_FORMATS.md` covers non-imperative data and markup formats — no
functions, no control flow, so most of the shared `STYLE.md` imperative-
language rules do not apply. Four sub-formats, each stating explicitly which
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

Scaffold dispatch lives in the shared `Lang.java`/`Main.java`/
`ServerMode.java`/`Config.java`, described in the routing `CLAUDE.md`
table. This job's own per-sub-format rule classes —
`rules/JsonSpecificRule.java` (JSON/JSON5), `rules/YamlSpecificRule.java`,
`rules/TomlSpecificRule.java`, `rules/CssSpecificRule.java`, and
`rules/XmlSpecificRule.java` (XML/HTML5) — exist only as boilerplate stubs
(each constructor throws `UnsupportedOperationException`) — no real logic
yet.

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

---

## Test Fixtures (Local)

Planned local dogfood pairs (distinct from the external-repo list above,
which is for corpus-scale validation) are staged in
**FUTURE_TEST_FIXTURES.md**, under its "JSON", "JSON5", "XML", "CSS", and
"HTML5" sections — not duplicated here. See that file for the pair list and
what each covers. Once authored, register pairs in the Makefile's
`INP_FILES` / `test/README.txt`, and empty out FUTURE_TEST_FIXTURES.md's
relevant sections accordingly.

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
  until its own session.
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
- [ ] Implement XML support (§2): tokenizer/parser for tag structure,
      indentation, attribute wrapping, DOCTYPE/PI/CDATA opacity handling.
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
- [ ] Author local test fixture pairs for XML/CSS/HTML5 (JSON/JSON5's are
      done above) and register in the Makefile's `INP_FILES` /
      `test/README.txt`.
- [ ] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      `STYLE_DATA_FORMATS.md`'s listed test-fixture repos per sub-format
      (`json5/json5`/`microsoft/vscode`/etc. for JSON — still open, not yet
      run; `apache/maven`/etc. for XML; `twbs/bootstrap`/etc. for CSS;
      `h5bp/html5-boilerplate`/etc. for HTML5).
