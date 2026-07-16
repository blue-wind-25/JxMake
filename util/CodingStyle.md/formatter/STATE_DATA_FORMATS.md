# STATE_DATA_FORMATS.md — Data & Markup Format JAR Support Tracker

Read `STATE_COMMON.md` first — it has the shared commit/ambiguity/testing
conventions this file assumes. `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` (the
other jobs' files) are NOT required reading for this one — only
`STATE_COMMON.md` is.

---

## Purpose

Tracks implementation of data/markup format support in the deterministic JAR
formatter (`util/CodingStyle.md/formatter/`), per `STYLE_DATA_FORMATS.md`
(JSON/JSON5, XML, CSS, HTML5). **Current status is scaffold-only: dispatch
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

No `src/` files yet — scaffold dispatch lives in the shared
`Lang.java`/`Main.java`/`ServerMode.java`/`Config.java`, described in the
routing `CLAUDE.md` table; this job's own rule classes (per-sub-format
formatters/dispatchers) do not exist yet.

---

## Resolved Design Decisions

Full text of each decision lives in `RDD_LOG.md` (shared with
`STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` — continue the existing `RDD_KEY_n`
numbering, do not restart). See `STATE_COMMON.md`'s lookup convention
(`grep -Fm1`, no `-A`).

| Key | Topic |
|---|---|
| — | none yet |

---

## Open Questions

None recorded yet in this file.

---

## Checklist

- [ ] **Implement JSON/JSON5 first (simplest grammar).** This is the literal
      first actionable item — JSON/JSON5 has the smallest grammar of the
      four sub-formats (no tags, no selectors, no dispatch problem) and is
      the natural starting point before XML/CSS/HTML5.
- [ ] Implement XML support (§2): tokenizer/parser for tag structure,
      indentation, attribute wrapping, DOCTYPE/PI/CDATA opacity handling.
- [ ] Implement CSS support (§3): property/value colon alignment, at-rule
      and native-nesting (`&`) header/group recursion.
- [ ] Implement HTML5 support (§4), including the `<script>`/`<style>`
      embedded-content dispatcher (splice out, format via CSS/JS-TS, splice
      back with correct re-indentation) — depends on both CSS support above
      and JS/TS support (tracked in `STATE_JS_TS.md`, a separate job) being
      available before the `<script>` dispatch path can be exercised
      end-to-end.
- [ ] Author local test fixture pairs per `FUTURE_TEST_FIXTURES.md`'s
      "JSON", "JSON5", "XML", "CSS", and "HTML5" sections and register in
      the Makefile's `INP_FILES` / `test/README.txt`.
- [ ] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      `STYLE_DATA_FORMATS.md`'s listed test-fixture repos per sub-format
      (`json5/json5`/`microsoft/vscode`/etc. for JSON; `apache/maven`/etc.
      for XML; `twbs/bootstrap`/etc. for CSS; `h5bp/html5-boilerplate`/etc.
      for HTML5).
