# FUTURE_TEST_FIXTURES.md — Planned Local Test Fixture Pairs

> **⚠️ Note on fixture quality:** the `inp`/`out` pairs in this file are
> hand-crafted (by an AI pass reasoning against the relevant `STYLE_*.md` rules,
> not generated or verified by a JAR), and may still contain formatting errors —
> several have already been caught and corrected through review, and more may
> remain. Check each pair against the style docs before relying on it, same
> "review every diff carefully" scrutiny `README.txt` asks of any AI-pass output.

This file is a staging area for **local dogfood test-fixture pairs** (`<name>_inp/out.<ext>`,
same convention as `formatter/test/README.txt`) for languages that don't have a JAR
implementation yet: C++26 extensions, JSON/JSON5, XML, CSS, HTML5, JavaScript,
TypeScript, and Python3. Same "notes for later, not a task list" spirit as
FUTURE_FEATURE_DISCUSSION.md — a pair listed here is not a commitment to implement
the language.

**Draft content vs. authored/registered.** Some entries below carry a pre-drafted
`inp`/`out` pair inline, hand-reasoned against the relevant `STYLE_*.md` rules by a
capable general-purpose model (the same job `AI_PREAMBLE_FULL.md` describes) since
there's no JAR to generate or verify it. This is **not** the same as "authored" in
the sense the rest of this file uses that word — draft content here has not been
run through a formatter or cross-checked, and needs the same "review every diff
carefully" scrutiny `README.txt` asks of any AI-pass output. A pair is only
"authored" once it's been reviewed, moved to `formatter/test/`, and registered in
`formatter/test/README.txt` per that file's own instructions — draft content
staying here, even pre-written, doesn't skip that step.

**PROMOTION GATE — flagged assumptions/contradictions.** Several draft pairs below
carry a **Flagged assumption** or **Flagged contradiction** note — a spot where the
draft had to guess at an unstated rule, or where two parts of the relevant
`STYLE_*.md`/`STYLE.md` file disagree with each other. These are not just FYI: **a
pair carrying one of these notes must not be moved to `formatter/test/` and
registered in `README.txt` until the flag is resolved.** Resolving means an actual
discussion with Aloysius about what the rule *should* be, followed by a fix to the
corresponding `STYLE_*.md` section — not a unilateral pick, even if the fixture's
own draft content already leans one way. If Claude (in Claude Code or any other
session) is asked to promote a fixture pair and its entry still carries a Flagged
note, stop and raise the flag for discussion before touching `formatter/test/` or
`README.txt`, same as any other unresolved open item in this project.

**Sections are named, not numbered**, and never will be — this file is meant to be
referenced from multiple `STYLE_*.md` files by section name, and numbering would go
stale every time a section is added, removed, or reordered.

**Distinct from external Test-Fixture Repos.** Each relevant `STYLE_*.md` also lists
external GitHub repos (for corpus-scale validation once a language is actually
implemented) — those lists are unaffected by this file. This file covers only local,
committed-to-the-repo dogfood pairs, same role `cpp_modern_inp/out.cpp` and
`cpp_comments_inp/out.cpp` already play for CPP20.


When a pair is actually authored, move its entry out of this file and into
`formatter/test/README.txt` alongside the existing entries, same as any other
fixture pair.

---

## CPP26

Moved here from `STYLE_CPP26.md`'s former local "Test Fixtures" section — originally
drafted there before this staging file existed.


`cpp_26ext_inp/out.cpp` and `cpp_26_comments_inp/out.cpp` have been extracted to
`formatter/test/` (registered commented-out in the Makefile pending real §1-4 rule
coverage -- see `STATE_CPP26.md`). Their draft content formerly staged here has been
removed; see `formatter/test/README.txt` for a description of what each covers.

- **cpp_26_reflection_inp/out.cpp** — reflection (`^^`, `[:`, `:]` splicing).
  Drafted alongside the other two pairs above, same unverified-draft status as
  `cpp_26ext`/`cpp_26_comments` — see file intro. What's still gated on the
  external-corpus pass (`bloomberg/clang-p2996`, `wrocpp/cpp26-reflection-examples`,
  `simdjson/experimental_json_builder`, `stephenberry/glaze` — see
  `STYLE_CPP26.md` §5) is *trusting* the rules, not *drafting* the fixture that
  will check them. §5's rules stay provisional, and this pair's expected output
  stays flagged unverified, until that pass runs.

  <details>
  <summary>Draft content (unverified — see file intro)</summary>

  `cpp_26_reflection_inp.cpp`:
  ```cpp
  constexpr auto refl=^^SomeType;
  constexpr auto splice=[:refl:];
  constexpr auto computed=[:  computeRefl(x)  :];
  constexpr auto nested=^^(a + b);

  template<typename T>
  constexpr auto reflectMember(T&& obj) {
  return ^^obj;
  }

  void useSplice() {
  constexpr auto r = ^^int;
  auto v = [:r:];
  total += [:r:];
  }

  constexpr auto x1 = ^^Foo;
  constexpr auto x2 = ^^Bar;

  void checkReflected(int x) {
  if(isReflected(x)) return;
  }
  ```

  `cpp_26_reflection_out.cpp`:
  ```cpp
  constexpr auto refl     = ^^SomeType;
  constexpr auto splice   = [:refl:];
  constexpr auto computed = [: computeRefl(x) :];
  constexpr auto nested   = ^^(a + b);

  template<typename T>
  constexpr auto reflectMember(T&& obj)
  {
      return ^^obj;
  }

  void useSplice()
  {
      constexpr auto r = ^^int;
      auto v = [:r:];
      total += [:r:];
  }

  constexpr auto x1 = ^^Foo;
  constexpr auto x2 = ^^Bar;

  void checkReflected(int x)
  {
      if( isReflected(x) ) return;
  }
  ```

  Covers: `^^` binding tight to its operand with no space in an initializer
  (`^^SomeType`), a `return` expression (`^^obj`), and a parenthesized
  sub-expression (`^^(a + b)`) (§5); the four-member `constexpr auto` group's `=`
  alignment, extended from §5's own three-member example to confirm the
  alignment logic finds the correct longest-name column with a fourth, longer
  member added; `[:refl:]` as a bare-value splice staying tight vs.
  `[: computeRefl(x) :]` going loose because its content contains a call,
  mirroring the existing JAR-verified `[[ assume(a >= 0) ]]` precedent §5 cites;
  a single unpadded splice (`[:r:]`) outside any alignment group, confirming
  §5's tight rule holds standalone and not just inside the four-member example,
  plus that same splice reused as an operand inside a larger expression
  (`total += [:r:]`) rather than only as an initializer's sole RHS; `x1`/`x2` as
  a second, separate two-member alignment group after a blank line, per STYLE.md
  §6's grouping rule; `checkReflected`'s `if(isReflected(x))` going loose per
  §3.1's "contains a function call" row and then collapsing to an inline
  single-statement `if` per STYLE.md §10, since it has no comment forcing it to
  stay a block; both function bodies reformatted to Allman braces per
  STYLE_C_CPP.md, independent of the reflection tokens themselves. **Flagged
  status:** this pair is drafted, not validated — it still needs the same
  tokenizer-support pass (`^^`, `[:`, `:]` as new `MULTI_CHAR_OPS` entries) and the
  same external-corpus cross-check §5 already calls for before its rules can be
  trusted. Drafting the fixture now doesn't skip that work; it just means the
  expected output is written down and ready to verify against, rather than
  invented after the fact. **🚫 PROMOTION GATE — do not move this pair to
  `formatter/test/` until the tokenizer-support pass and external-corpus
  cross-check run and `STYLE_CPP26.md` §5 is updated from provisional to
  confirmed; see file intro.**
  </details>

Referenced from: `STYLE_CPP26.md`.

---

## JSON

Plain RFC 8259 JSON only — kept separate from JSON5 specifically to catch the
formatter accidentally emitting JSON5-only syntax (trailing commas, unquoted keys,
comments) into a `.json` file. No `_comments` pair — plain JSON has no comment
syntax, so there's nothing to test placement of.

The entry formerly staged here (`json_core_inp/out.json`) has been extracted,
reviewed, and registered as a real fixture in `formatter/test/` — see
`formatter/test/README.txt` and `formatter/STATE_DATA_FORMATS.md`.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## JSON5

Both entries formerly staged here (`json5_core_inp/out.json5`,
`json5_comments_inp/out.json5`) have been extracted, reviewed, and registered as
real fixtures in `formatter/test/` — see `formatter/test/README.txt` and
`formatter/STATE_DATA_FORMATS.md`.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## XML

- **xml_combined_inp/out.xml** — tag/attribute formatting and wrapping, attribute
  order preservation, CDATA (including the `<script>`/`<style>` CDATA-unwrap
  exception), DOCTYPE and processing instructions, at least one non-plain-XML
  dialect (e.g. SVG or Android XML) to exercise namespace-bearing attributes.

  <details>
  <summary>Draft content (unverified — see file intro)</summary>

  `xml_combined_inp.xml`:
  ```xml
  <?xml   version="1.0"   encoding="UTF-8"?>
  <?xml-stylesheet type="text/xsl" href="catalog.xsl"?>
  <!DOCTYPE catalog   SYSTEM  "catalog.dtd">
  <catalog xmlns="http://example.com/catalog" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://example.com/catalog catalog.xsd">
  <book id="bk101" category="fiction" available="true">
  <title>The Great Adventure</title>
  <author>Jane Doe</author>
  </book>
  <book id="bk102" category="reference" available="false" publisher="Example Press" edition="3rd">
  <title>Widgets &amp; Gadgets</title>
  <author>John Smith</author>
  </book>
  <image href="cover.png"/>
  <thumbnail href="cover-thumb.png" width="100" height="150" alt="Cover thumbnail image"/>
  <notes><![CDATA[Raw <unparsed> content & symbols, left untouched.]]></notes>
  <script><![CDATA[
  function greet(name) {
  return "Hello, " + name
  }
  ]]></script>
  <style><![CDATA[
  .title{font-weight:bold;color:#333}
  ]]></style>
  </catalog>
  ```

  `xml_combined_out.xml`:
  ```xml
  <?xml   version="1.0"   encoding="UTF-8"?>
  <?xml-stylesheet type="text/xsl" href="catalog.xsl"?>
  <!DOCTYPE catalog   SYSTEM  "catalog.dtd">
  <catalog
      xmlns="http://example.com/catalog"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://example.com/catalog catalog.xsd">
      <book id="bk101" category="fiction" available="true">
          <title>The Great Adventure</title>
          <author>Jane Doe</author>
      </book>
      <book
          id="bk102"
          category="reference"
          available="false"
          publisher="Example Press"
          edition="3rd">
          <title>Widgets &amp; Gadgets</title>
          <author>John Smith</author>
      </book>
      <image href="cover.png"/>
      <thumbnail
          href="cover-thumb.png"
          width="100"
          height="150"
          alt="Cover thumbnail image"/>
      <notes><![CDATA[Raw <unparsed> content & symbols, left untouched.]]></notes>
      <script><![CDATA[
          function greet(name)
          {
              return "Hello, " + name;
          }
      ]]></script>
      <style><![CDATA[
          .title {
              font-weight : bold;
              color       : #333;
          }
      ]]></style>
  </catalog>
  ```

  Covers: the `<?xml ... ?>` PI, a second `<?xml-stylesheet ...?>` PI, and the
  `<!DOCTYPE ...>` line all kept byte-for-byte identical to the input, including
  their irregular internal spacing — §2.3's "opaque, preserved verbatim, never
  reflowed regardless of length" rule applying to every PI present, not just the
  always-present `<?xml ?>` one; `<catalog>`'s three-attribute opening tag
  overflowing the line-length limit and wrapping one attribute per line, each
  indented one level from the tag itself (§2.2) — including the `xmlns`/
  `xmlns:xsi` namespace declarations, whose relative order versus
  `xsi:schemaLocation` is preserved exactly as written per §2.2's no-reordering
  rule; `<book id="bk101">`'s three attributes fitting on one line (no wrap) sitting
  directly next to `<book id="bk102">`'s five attributes overflowing and wrapping,
  exercising both the wrap and no-wrap paths side by side; `Widgets &amp; Gadgets`
  as an entity reference inside ordinary element text (not CDATA), left untouched
  rather than re-escaped or decoded; `<image href="cover.png"/>` staying
  self-closing (§2.2 — self-closing tags are ordinary XML syntax, not an
  HTML5-only construct; the HTML5 void-element override in §4.1 is a different,
  narrower rule); `<thumbnail>` as a self-closing tag that also overflows and
  wraps, its `/>` landing on the last attribute's own line, same placement
  convention as `<catalog>`'s closing `>`; `<notes>`'s CDATA content preserved
  verbatim, untouched by the surrounding reindentation despite containing `<`/`&`
  characters that would otherwise need escaping outside CDATA (§2.4's
  default-opaque case); `<script>`'s CDATA content as the §2.4 exception —
  unwrapped, its JS content reformatted per `STYLE_JS_TS.md` (Allman brace for the
  named function, inserted semicolon on `return`), then re-wrapped and reindented
  to the `<script>` tag's own nesting depth, per §4.2's dispatch description;
  `<style>`'s CDATA content as the same §2.4 exception applied to `style` instead
  of `script`, dispatched to §3's CSS formatter (same-line brace, colon-aligned
  declarations) and re-wrapped the same way.
  </details>

- **xml_comments_inp/out.xml** — uncommon `<!-- -->` placement, plus a
  `JXM_CFMT_DIS`/`ENA` directive pair using XML's single block-comment directive
  syntax.

  <details>
  <summary>Draft content (unverified — see file intro)</summary>

  `xml_comments_inp.xml`:
  ```xml
  <config>
  <!-- top-level settings -->
  <timeout>30</timeout>
  <retries>3</retries><!-- inline trailing comment -->
  <!--% JXM_CFMT_DIS -->
  <weird    attr="1"   other = "2" ></weird>
  <!--% JXM_CFMT_ENA -->
  <database>
  <host>localhost</host>
  <!-- nested comment inside database block -->
  <port>5432</port>
  </database>
  <!--
  multi-line comment
  spanning several lines
  -->
  <name>worker</name>
  <!-- trailing comment right before close -->
  </config>
  ```

  `xml_comments_out.xml`:
  ```xml
  <config>
      <!-- top-level settings -->
      <timeout>30</timeout>
      <retries>3</retries><!-- inline trailing comment -->
  <!--% JXM_CFMT_DIS -->
  <weird    attr="1"   other = "2" ></weird>
  <!--% JXM_CFMT_ENA -->
      <database>
          <host>localhost</host>
          <!-- nested comment inside database block -->
          <port>5432</port>
      </database>
      <!--
  multi-line comment
  spanning several lines
  -->
      <name>worker</name>
      <!-- trailing comment right before close -->
  </config>
  ```

  Covers: a standalone leading `<!-- -->` comment reindented to its structural
  nesting depth like any other content line — unlike DOCTYPE/PI/CDATA, §2.3/§2.4
  don't list ordinary comments as opaque, so this fixture takes the position that
  they follow §2.2's normal indent handling (worth confirming against real JAR
  behavior once implemented, flagged here since the style doc doesn't spell this
  case out explicitly), now also exercised two levels deep inside `<database>`,
  not just at top level; an inline trailing comment staying on the same line as
  its preceding `<retries>` tag rather than being moved to its own line; a
  multi-line `<!-- -->` comment whose opening `<!--` reindents to its structural
  position while its interior lines and closing `-->` stay exactly as written —
  comments aren't a token stream with a defined re-indent grammar the way
  declarations are, so this fixture takes the position that only the comment's
  opening line participates in structural indentation, same "verbatim interior"
  treatment JSON5's line-continuation strings get (also flagged as inference, not
  a stated rule); a trailing comment on its own line immediately before the
  closing `</config>`, with no blank line forced above it; the
  `JXM_CFMT_DIS`/`ENA` pair suspending all reformatting — indentation and
  attribute-spacing normalization alike — for the enclosed `<weird>` tag,
  preserved byte-for-byte including its irregular internal spacing, with normal
  formatting resuming immediately for `<database>` after `JXM_CFMT_ENA`.
  </details>

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## CSS

Both entries formerly staged here (`css_combined_inp/out.css`,
`css_comments_inp/out.css`) have been extracted, reviewed, and registered as real
fixtures in `formatter/test/` — see `formatter/test/README.txt` and
`formatter/STATE_DATA_FORMATS.md`.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## HTML5

`html_combined_inp/out.html` and `html_comments_inp/out.html` have been extracted
to `formatter/test/` (registered commented-out in the Makefile pending real HTML5
dispatch/formatting logic -- see `STATE_DATA_FORMATS.md`). Their draft content
formerly staged here has been removed; see `formatter/test/README.txt` for a
description of what each covers.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## YAML

Both entries (`yaml_core_inp/out.yaml`, `yaml_comments_inp/out.yaml`) were authored
directly in `formatter/test/` rather than staged here first, since there was no
pre-existing draft to extract — see `formatter/test/README.txt` and
`formatter/STATE_DATA_FORMATS.md`. They are commented out of the Makefile's
`INP_FILES` (YAML support is scaffold-only, RDD_KEY_191) until real formatting logic
lands.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## TOML

Both entries (`toml_core_inp/out.toml`, `toml_comments_inp/out.toml`) were authored
directly in `formatter/test/` rather than staged here first, same as YAML above —
see `formatter/test/README.txt` and `formatter/STATE_DATA_FORMATS.md`. They are
commented out of the Makefile's `INP_FILES` (TOML support is scaffold-only,
RDD_KEY_191) until real formatting logic lands.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## JavaScript

`js_combined_inp/out.js` and `js_comments_inp/out.js` have been extracted to
`formatter/test/` (registered commented-out in the Makefile pending real JS
formatting logic -- see `STATE_JS_TS.md`). Their draft content formerly staged
here has been removed; see `formatter/test/README.txt` for a description of what
each covers.

Referenced from: `STYLE_JS_TS.md`.

---

## TypeScript

`ts_combined_inp/out.ts` and `ts_comments_inp/out.ts` have been extracted to
`formatter/test/` (registered commented-out in the Makefile pending real TS
formatting logic -- see `STATE_JS_TS.md`). Their draft content formerly staged
here has been removed; see `formatter/test/README.txt` for a description of what
each covers.

Referenced from: `STYLE_JS_TS.md`.

---

## Python3

`py_combined_inp/out.py` and `py_comments_inp/out.py` have been extracted to
`formatter/test/` (registered commented-out in the Makefile pending real Python3
formatting logic -- see `STATE_PYTHON3.md`). Their draft content formerly staged
here has been removed; see `formatter/test/README.txt` for a description of what
each covers.

Referenced from: `STYLE_PYTHON3.md`.
