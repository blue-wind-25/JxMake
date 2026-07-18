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

- **cpp_26ext_inp/out.cpp** — pack indexing (`T...[i]`), `= delete("reason")`,
  placeholder `_`, contracts (`pre`/`post`/`contract_assert`).

  <details>
  <summary>Draft content (unverified — see file intro)</summary>

  `cpp_26ext_inp.cpp`:
  ```cpp
  template<typename... T>
  using Nth = T ...[N];

  template<typename... T>
  using Selected = T...[ computeIndex() ];

  template<typename... T>
  using Rebased = T...[ offsets[i] ];

  void oldApi() = delete( "use newApi() instead" );
  void reallyOldApi() = delete;

  auto [_,count] = getResult();
  auto [_, _, total] = getTriple();
  if(auto _ = acquireLock(); true) { doWork(); }

  int divide(int a, int b)
  pre(b!=0)
  post(r:r*b==a)
  {
  return a / b;
  }

  int clampSimple(int x) pre(x >= 0) { return x; }

  int clampFull(int x, int lo, int hi)
  pre(lo <= hi)
  pre(x >= lo)
  post(r: r <= hi)
  {
  if (x < lo) return lo;
  if (x > hi) return hi;
  return x;
  }

  void process(int x) {
  contract_assert(x>=0);
  contract_assert(x < 1000);
  }
  ```

  `cpp_26ext_out.cpp`:
  ```cpp
  template<typename... T>
  using Nth = T...[N];

  template<typename... T>
  using Selected = T...[ computeIndex() ];

  template<typename... T>
  using Rebased = T...[ offsets[i] ];

  void oldApi() = delete("use newApi() instead");
  void reallyOldApi() = delete;

  auto [_, count]    = getResult();
  auto [_, _, total] = getTriple();
  if( auto _ = acquireLock(); true ) doWork();

  int divide(int a, int b)
      pre(b != 0)
      post(r: r * b == a)
  {
      return a / b;
  }

  int clampSimple(int x) pre(x >= 0) { return x; }

  int clampFull(int x, int lo, int hi)
      pre(lo <= hi)
      pre(x >= lo)
      post(r: r <= hi)
  {
      if(x < lo) return lo;
      if(x > hi) return hi;
      return x;
  }

  void process(int x)
  {
      contract_assert(x >= 0);
      contract_assert(x < 1000);
  }
  ```

  Covers: `T...[N]` as a constant index staying tight, no space between the pack
  name/`...`/`[` (§1); `T...[ computeIndex() ]` as a call-inside-index case going
  loose, per the general tight/loose convention every other bracket-complexity
  section in this project uses (loose = spaces added inside the bracket, e.g.
  `STYLE_PYTHON3.md` §1.1's `a[ callSomething(x) ]`) — confirmed and fixed in
  `STYLE_CPP26.md` §1 itself (RDD_KEY_181); `T...[ offsets[i] ]` as a
  nested-bracket-inside-index case going loose for the same reason a call does
  (§3.1's "contains a nested `()` or `[]`" row), not just the call case. Also
  covers: `= delete("reason")`'s string argument taking ordinary call-argument
  spacing, no special padding for the parens themselves, contrasted with bare
  `= delete;` taking no parens at all (§2); placeholder `_` as an ordinary
  identifier in both a structured-binding slot (including a slot reused twice in
  one binding) and an `if`-init statement, comma spacing unaffected (§3); the
  contract clauses (`pre`/`post`) each getting their own line indented one level
  from the function signature when the whole signature doesn't fit inline, but
  staying inline when it does (`clampSimple`) per §4's overflow-triggered-wrap
  allowance; `post(r: ...)`'s result-binding `:` taking normal identifier/colon
  spacing; `contract_assert(...)` formatted as an ordinary function-call-shaped
  statement inside a block body (§4); the two-member `auto [_, count]`/
  `auto [_, _, total]` group's `=` alignment (STYLE.md §6); `if(auto _ =
  acquireLock(); true)` going loose because `acquireLock()` is a call inside the
  condition, with the whole thing collapsing to an inline single-statement `if`
  per STYLE.md §10 since it has no comment forcing it to stay a block (contrast
  with `cpp_26_comments`'s version of this same construct, which keeps the block
  because it has comments inside it); `clampFull`'s body using un-braced
  single-statement `if`s, also per STYLE.md §10, and no space after `if` (STYLE.md
  §3.2 — control-flow keywords never get a space before `(`, regardless of
  whether the condition itself is tight or loose).
  </details>

- **cpp_26_comments_inp/out.cpp** — uncommon comment placement around the above
  constructs, same purpose as `cpp_comments_inp/out.cpp` for CPP20.

  <details>
  <summary>Draft content (unverified — see file intro)</summary>

  `cpp_26_comments_inp.cpp`:
  ```cpp
  // Pack indexing examples
  template<typename... T>
  // comment between template<> and the using-declaration
  using Nth = T...[N];
  template<typename... T>
  using Selected = T...[computeIndex()];  // call inside index
  using Skipped = T...[0];  // zero-based

  // Deprecated API marker
  void oldApi() = delete(
      "use newApi() instead" // reason, trailing on the arg itself
  );

  /* Placeholder examples */
  auto [_, count] = getResult();  // structured binding, trailing
  if(auto _ = acquireLock(); true) {
      // comment as the sole content before real work starts
      doWork();
      // trailing comment right before close, no blank line
  }

  // Contract clauses
  int divide(int a, int b)
  // pre-condition: divisor nonzero
  pre(b != 0)
  post(r: r * b == a)  // post-condition: result matches, moved inline
  {
  return a / b;
  }

  int clamp(int x, int lo, int hi)
  pre(lo <= hi)
  /*
   * Multi-sentence rationale for this contract.
   * Kept as a block comment between two contract clauses.
   */
  pre(x >= lo && x <= hi)
  {
  return x;
  }

  void process(int x) {
  // runtime assertion
  contract_assert(x >= 0);

  // trailing note with a blank line above it, still inside the block
  }
  ```

  `cpp_26_comments_out.cpp`:
  ```cpp
  // Pack indexing examples
  template<typename... T>
  // Comment between template<> and the using-declaration
  using Nth = T...[N];

  template<typename... T>
  using Selected = T...[ computeIndex() ];  // Call inside index

  using Skipped = T...[0];  // Zero-based

  // Deprecated API marker
  void oldApi() = delete(
      "use newApi() instead"  // Reason, trailing on the arg itself
  );

  /* Placeholder examples */
  auto [_, count] = getResult();  // Structured binding, trailing
  if( auto _ = acquireLock(); true )
  {
      // Comment as the sole content before real work starts
      doWork();
      // Trailing comment right before close, no blank line
  }

  // Contract clauses
  int divide(int a, int b)
      // Pre-condition: divisor nonzero
      pre(b != 0)
      post(r: r * b == a)  // Post-condition: result matches, moved inline
  {
      return a / b;
  }

  int clamp(int x, int lo, int hi)
      pre(lo <= hi)
      /*
       * Multi-sentence rationale for this contract.
       * Kept as a block comment between two contract clauses.
       */
      pre(x >= lo && x <= hi)
  {
      return x;
  }

  void process(int x)
  {
      // Runtime assertion
      contract_assert(x >= 0);

      // Trailing note with a blank line above it, still inside the block
  }
  ```

  Covers: a standalone leading `//` comment preceding a pack-indexing/deprecated-
  API/placeholder construct, position preserved exactly as written; a comment
  wedged **between** `template<...>` and its `using` line, splitting a
  declaration that would otherwise be treated as one unit; a trailing `//`
  comment on the same line as `T...[computeIndex()]`, surviving the §1
  loose-bracket reformatting without being disturbed; `T...[0]` staying **tight**
  even with a trailing comment present — a comment does not itself trigger
  looseness, only the bracket's actual content does (§1/§3.1); a trailing
  comment attached to a call argument itself, inside a wrapped multi-line call;
  each contract clause's own leading `//` comment (`// pre-condition: ...`)
  reindented to match its clause's own indent level (one level from the
  signature, same as the clause itself) rather than staying at column 0 as
  originally written, plus a trailing comment on `post(...)` (the inverse
  placement) — a comment attaches to and follows the indentation of the line it
  precedes, same convention this project applies everywhere else; a `/* */`
  block comment standing alone before the placeholder examples, and a second,
  multi-sentence `/* */` block comment sitting **between two `pre(...)`
  clauses** in `clamp`, checking attachment when the comment isn't adjacent to
  the signature; `if( auto _ = acquireLock(); true )` going loose because
  `acquireLock()` is a call in the condition (§3.1), staying a braced block
  rather than collapsing to inline (STYLE.md §10) because it has comments
  inside it — contrast with `cpp_26ext`'s comment-free version of the same
  construct, which does inline; a comment as the sole content before the first
  real statement in a block, and a trailing comment with no blank line right
  before `}`; a blank line before a trailing comment at the end of a block; and
  `normalize-comment-start-case` visibly firing throughout — every
  sentence-fragment comment is written lowercase-start in `_inp` and
  capitalized in `_out` (labels/closers, if any were present, would stay as-is
  per STYLE.md §15, but none of these comments are that kind).
  </details>
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

Must include a small embedded `<style>` block and a small embedded `<script>` block
in both pairs (not just the combined one) — the dispatcher to CSS/JS formatting is
the main design point for this language and needs coverage in both the construct
pass and the comment-placement pass, not just one.

- **html_combined_inp/out.html** — void element normalization (`<br>`, `<img>`, no
  self-closing slash), tag/attribute wrapping, a small embedded `<style>` block
  (dispatches to CSS combined fixture's constructs at small scale) and a small
  embedded `<script>` block (dispatches to JS combined fixture's constructs at small
  scale), re-indentation after splice-back.

  <details>
  <summary>Draft content (unverified — see file intro)</summary>

  `html_combined_inp.html`:
  ```html
  <!DOCTYPE html>
  <html lang="en">
  <head>
  <meta charset="UTF-8">
  <link rel="stylesheet" href="style.css">
  <title>Demo</title>
  <style>
  .box{display:flex;color:#333;}
  </style>
  </head>
  <body>
  <div class="container" data-testid="root-container" data-analytics-id="demo-page-root">
  <p>Welcome to the <span class="highlight">demo</span> page.</p>
  Here is a list of items:
  <ul>
  <li>First item</li>
  <li>Second item</li>
  <li>Third item</li>
  </ul>
  <pre>
  function raw() {
    return 1;
  }
  </pre>
  </div>
  <img src="photo.jpg" alt="A photo" />
  <input type="checkbox" checked disabled>
  <input type="text" name="q"/>
  <br/>
  <script>
  function greet(name) {
  return "Hello, " + name
  }
  </script>
  End of demo page.
  </body>
  </html>
  ```

  `html_combined_out.html`:
  ```html
  <!DOCTYPE html>
  <html lang="en">
      <head>
          <meta charset="UTF-8">
          <link rel="stylesheet" href="style.css">
          <title>Demo</title>
          <style>
              .box {
                  display : flex;
                  color   : #333;
              }
          </style>
      </head>
      <body>
          <div
              class="container"
              data-testid="root-container"
              data-analytics-id="demo-page-root">
              <p>Welcome to the <span class="highlight">demo</span> page.</p>
              Here is a list of items:
              <ul>
                  <li>First item</li>
                  <li>Second item</li>
                  <li>Third item</li>
              </ul>
              <pre>
  function raw() {
    return 1;
  }
  </pre>
          </div>
          <img src="photo.jpg" alt="A photo">
          <input type="checkbox" checked disabled>
          <input type="text" name="q">
          <br>
          <script>
              function greet(name)
              {
                  return "Hello, " + name;
              }
          </script>
          End of demo page.
      </body>
  </html>
  ```

  Covers: `<img>`/`<input>`/`<br>` void elements losing their self-closing `/`
  (§4.1), alongside `<link>` as a void element that never had a slash to strip
  in the first place, contrasting the two; `<input type="checkbox" checked
  disabled>` — value-less boolean attributes preserved as bare tokens, not
  synthesized into `checked=""`/`disabled=""` (a gap neither §4.1 nor the §2.2
  rule it borrows spells out explicitly, since every other example attribute in
  this project's fixtures has a value — treated as inference until confirmed);
  `<div class="container" data-testid="..." data-analytics-id="...">` — three
  ordinary-length attributes whose combined width overflows the line-length
  limit, wrapping one per line the same way a single long attribute would
  (`html_comments`'s `<img data:...>` case), confirming the wrap trigger is
  total line length, not any one attribute's individual length; `<style>`
  content spliced out, formatted per §3's CSS colon-alignment with a same-line
  opening brace (`.box {`, matching `css_combined`'s established brace
  convention, not Allman), spliced back reindented to its own nesting depth;
  `<script>` content spliced to the JS/TS formatter — Allman brace on the named
  function, inserted semicolon on `return` — then reindented on splice-back;
  `<div>`/`<ul>`/`<li>` as ordinary block-level nesting, each reindented one
  level deeper per §2.2's normal tag-indentation rule; `<p>` with an inline
  `<span>` child staying on one line since it fits the line-length limit, no
  forced wrap just because a child tag is present; bare text nodes (`Here is a
  list of items:` and `End of demo page.`) sitting between element siblings,
  reindented to their parent's structural depth like any other content line;
  `<pre>` content left byte-for-byte untouched, including its exact indentation
  and line breaks, with the tag itself not reindented to the surrounding
  structural depth — `<pre>` opaque like CDATA, bare text nodes reindent to
  parent structural depth like any other content line, both now confirmed and
  spelled out explicitly in `STYLE_DATA_FORMATS.md` §4.3 (RDD_KEY_185).
  </details>
- **html_comments_inp/out.html** — uncommon `<!-- -->` placement, the
  `<script><![CDATA[ ... ]]></script>` CDATA-wrapped script idiom (§2.3 exception),
  a `<script type="application/json">` block that must stay opaque (not dispatched).

  <details>
  <summary>Draft content (unverified — see file intro)</summary>

  `html_comments_inp.html`:
  ```html
  <div>
  <!-- top banner -->
  <!-- second banner line, stacked -->
  <p>Hello</p><!-- inline note -->
  <notes><![CDATA[Raw <config> data & "quoted" bits, <left> untouched.]]></notes>
  <img class="icon" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=" alt="icon">
  <style>
  .icon {
  /* comment as sole content before declarations */
  width:16px;
  }
  </style>
  <script><![CDATA[
  function tick() {
  var now = Date.now();
  var elapsed = now - startTime;
  console.log("tick", elapsed);
  return now
  }

  function reset() {
  startTime = Date.now()
  }
  ]]></script>
  <script type="application/json">
  {"a":1,"b":2}
  </script>
  <!-- trailing comment right before close, no blank line -->
  </div>
  ```

  `html_comments_out.html`:
  ```html
  <div>
      <!-- Top banner -->
      <!-- Second banner line, stacked -->
      <p>Hello</p><!-- Inline note -->
      <notes><![CDATA[Raw <config> data & "quoted" bits, <left> untouched.]]></notes>
      <img
          class="icon"
          src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
          alt="icon">
      <style>
          .icon {
              /* Comment as sole content before declarations */
              width : 16px;
          }
      </style>
      <script><![CDATA[
          function tick()
          {
              var now = Date.now();
              var elapsed = now - startTime;
              console.log("tick", elapsed);
              return now;
          }

          function reset()
          {
              startTime = Date.now();
          }
      ]]></script>
      <script type="application/json">
  {"a":1,"b":2}
  </script>
      <!-- Trailing comment right before close, no blank line -->
  </div>
  ```

  Covers: two stacked leading `<!-- -->` comments before `<p>`, both surviving
  in order; a standalone leading `<!-- -->` comment reindented to its structural
  nesting depth, same treatment as any other content line (§2.2); an inline
  trailing comment staying on the same line as its preceding `<p>` tag rather
  than being moved to its own line; `<notes>`'s CDATA content preserved
  byte-for-byte — untouched by the surrounding reindentation despite containing
  `<`/`&`/`"` characters that would otherwise need escaping outside CDATA — the
  §2.4 **default** opaque case, placed directly next to the `<script>` CDATA
  below specifically to contrast the two: same `<![CDATA[ ]]>` syntax, opposite
  treatment depending on the enclosing tag; a `data:` URI as an attribute value,
  long enough on its own to push `<img>` past STYLE.md §2's line-length limit,
  so the tag wraps one attribute per line (§2.2's overflow rule, triggered here
  by attribute *length* rather than attribute *count* — contrast with
  `html_combined`'s `<div>` case, which wraps from combined attribute count),
  while the base64 string itself stays intact on its own line, never split
  mid-value; `<style>` with a comment as the sole content before its
  declarations, checking the CSS-dispatch splice preserves comment placement
  *inside* the spliced content, not just around it, plus the same-line brace
  convention (`.icon {`) on splice-back; the CDATA-wrapped `<script>` idiom
  (§2.4 **exception**) unwrapped, its two-function JS content (including the
  blank line between them) dispatched to the JS/TS formatter — Allman brace on
  both named functions, inserted semicolon on every statement including the
  `var`-less `startTime = Date.now()` — then re-wrapped in `<![CDATA[ ]]>` and
  reindented to the `<script>` tag's own nesting depth; `<script
  type="application/json">` left byte-for-byte opaque — not dispatched to the
  JS/TS formatter and not reindented, since §4.2 only dispatches script content
  whose type is JS/TS (or the type attribute is absent), and `application/json`
  is explicitly called out as staying opaque; a trailing comment on its own
  line immediately before `</div>`, no blank line forced above it.
  </details>

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## JavaScript

Plain `.js` only — no TypeScript-only constructs (those belong in the TypeScript
section below), same separation C/C++ already have across `.c`/`.cpp`/`.hpp`.

- **js_combined_inp/out.js** — destructuring/spread, template literals, arrow
  functions, optional chaining/nullish coalescing, `async`/`await`, decorators
  (stage-3 JS decorators, not TS-only usage), getter/setter accessors (`get`/`set` —
  plain ES6, not TS-only, so it belongs here rather than in the TypeScript pair),
  always-explicit semicolon insertion, import ordering/grouping.

  <details>
  <summary>Draft content (unverified — see file intro)</summary>

  `js_combined_inp.js`:
  ```javascript
  import fs from "fs";
  import {readFile} from "node:fs/promises";
  import {debounce} from "lodash";
  import express from "express";
  import {Widget} from "../components";
  import {helper} from "./helper";

  @Component({selector: "app-widget"})
  export class Widget {
  @Input() name
  @Output() changed = new EventEmitter()
  #cache = new Map()

  static get instanceCount() { return Widget._count }
  static set instanceCount(value) { Widget._count = value }

  get x() { return this._x }
  set x(value) { this._x = value }

  async load(id, options = {}) {
  const {id,name,...rest} = await fetchUser()
  const [first,second,...others] = await fetchItems()
  const merged = {...defaults,...overrides}
  const label = `User: ${name}`
  const len = this.profile?.bio?.length ?? 0
  const calc = (a,b) => a + b
  const withDefault = (a,b=10) => a + b
  const process = (data) => {
  return transform(data)
  }
  this.#cache.set(id, merged)
  return merged
  }

  *iterate() {
  yield 1
  yield 2
  }
  }

  export default Widget;
  ```

  `js_combined_out.js`:
  ```javascript
  import fs from "fs";
  import { readFile } from "node:fs/promises";

  import express from "express";
  import { debounce } from "lodash";

  import { Widget } from "../components";
  import { helper } from "./helper";

  @Component({ selector: "app-widget" })
  export class Widget {
      @Input() name;
      @Output() changed = new EventEmitter();
      #cache = new Map();

      static get instanceCount(     ) { return Widget._count; }
      static set instanceCount(value) { Widget._count = value; }

      get x(     ) { return this._x; }
      set x(value) { this._x = value; }

      async load(id, options = {})
      {
          const { id, name, ...rest }      = await fetchUser();
          const [first, second, ...others] = await fetchItems();
          const merged                     = { ...defaults, ...overrides };
          const label                      = `User: ${name}`;
          const len                        = this.profile?.bio?.length ?? 0;
          const calc                       = (a, b) => a + b;
          const withDefault                = (a, b = 10) => a + b;
          const process                    = (data) => {
              return transform(data);
          };
          this.#cache.set(id, merged);

          return merged;
      } // async load

      *iterate()
      {
          yield 1;
          yield 2;
      }
  } // class Widget

  export default Widget;
  ```

  Covers: import grouping/sorting (builtin → third-party alphabetical → local,
  blank line between groups), including a `node:`-prefixed builtin import
  (`node:fs/promises`) joining the builtin group alongside plain `fs`, sorted
  alphabetically within that group; inline decorator placement preserved on
  `@Input`/`@Output`; own-line decorator placement preserved on `@Component`;
  `#cache = new Map()` as a private class field, getting the same
  semicolon-insertion treatment as any other field; a **static** getter/setter
  pair (`instanceCount`) forming its own one-liner alignment group, separate
  from the instance `x` pair since a blank line breaks the group — confirming
  §14's alignment logic isn't scoped to instance members only; getter/setter
  one-liner group alignment (§14) — `x`/`x` being equal-width names needs no
  name-side padding, while the empty `()` vs. `(value)` parameter lists differ
  in width and get the internal-parens padding instead, keeping the `)`/`{`
  columns aligned; object/array destructuring with rest, spread merge, template
  literal, optional chaining/nullish coalescing, both arrow forms
  (single-expression and K&R block body), including a default parameter value
  inside an arrow (`withDefault = (a, b = 10) => ...`), spaced like an ordinary
  assignment; always-explicit semicolons throughout, including after the arrow
  block's `}`; closing comments on both the class and the Allman-brace
  `async load` method (§1's inherited STYLE.md §7), contrasted with the
  2-statement `*iterate()` generator method, which stays under the 5-line
  closing-comment threshold and gets none; the run of eight consecutive `const`
  declarations forming a single `=`-aligned group (§6's own `const add =
  .../const isEven = .../const process = ...` example confirms consts align as
  a group) — padded to the widest LHS, which here is the array-destructuring
  pattern `const [first, second, ...others]` — until `this.#cache.set(id,
  merged);` (an ordinary statement, not a `const`) breaks the group; a
  destructuring-pattern LHS joins the group like any ordinary declaration,
  regardless of LHS shape — now confirmed and spelled out explicitly in
  `STYLE_JS_TS.md` §3 (RDD_KEY_182); STYLE.md §9's **mandatory blank line
  before `return`** correctly firing before `return merged;`, since the
  function body is multi-line and the `return` is at function scope — `*`
  binding tight to the generator method name with no space.
  </details>
- **js_comments_inp/out.js** — uncommon `//`/`/* */` placement around the above
  constructs.

  <details>
  <summary>Draft content (unverified — see file intro)</summary>

  `js_comments_inp.js`:
  ```javascript
  // core imports
  import fs from "fs";
  import {debounce} from "lodash"; // utility for rate limiting
  import express from "express";

  /* Widget component */
  @Component({selector: "app-widget"})
  // class-level implementation note
  export class Widget {
  // exposed input
  @Input() name
  @Output() changed = new EventEmitter() // fired on change

  async load() {
  // destructure the fetched user
  const {
  id,
  // comment inside destructuring pattern
  name,
  ...rest
  } = await fetchUser()
  const label = `User: ${name}` // greeting label
  // nullish fallback to zero
  const len = this.profile?.bio?.length ?? 0
  return merged
  }

  // generator for iteration
  *iterate() {
  yield 1 // first value
  yield 2
  }
  }
  ```

  `js_comments_out.js`:
  ```javascript
  // Core imports
  import fs from "fs";

  import express from "express";
  import { debounce } from "lodash"; // Utility for rate limiting

  /* Widget component */
  @Component({ selector: "app-widget" })
  // Class-level implementation note
  export class Widget {
      // Exposed input
      @Input() name;
      @Output() changed = new EventEmitter(); // Fired on change

      async load()
      {
          // Destructure the fetched user
          const {
              id,
              // Comment inside destructuring pattern
              name,
              ...rest
          } = await fetchUser();
          const label = `User: ${name}`; // Greeting label
          // Nullish fallback to zero
          const len = this.profile?.bio?.length ?? 0;

          return merged;
      } // async load

      // Generator for iteration
      *iterate()
      {
          yield 1; // First value
          yield 2;
      }
  } // class Widget
  ```

  Covers: a leading `//` comment surviving import-group reordering (it stays
  attached to `fs`, which stays in group 1 regardless of sort); a trailing `//`
  comment on an import line surviving the alphabetical resort within its group; a
  `/* */` block comment preceding the decorated class, plus a second, distinct
  `//` comment sitting **between** the own-line `@Component` decorator and the
  class declaration it precedes — checking it stays its own leading line rather
  than merging into the decorator or getting dropped; a leading `//` inside the
  class body reindented to member depth; trailing `//` comments on a decorator
  line and on statements inside `async load` staying attached through semicolon
  insertion and reindentation; a comment sitting **inside** a destructuring
  pattern, forcing it onto multiple lines since there's nowhere else for the
  comment to live (same forced-multiline effect established for JSON5's `tags`
  array and CSS's `.tooltip` block — flagged as inference, not a stated rule);
  that now-multi-line `const { ... } = await fetchUser();` declaration **not**
  joining an `=`-alignment group with what follows, since there's no single-line
  `=` position to align against — leaving `label` to start its own group, which
  the very next leading comment (`// Nullish fallback to zero`) immediately
  breaks again, so `len` ends up in a single-member group of its own, no padding
  against `label` (also inference — STYLE_JS_TS.md §3/RDD_KEY_182 only confirms
  grouping for the ordinary single-line destructuring case); `*iterate()` with a
  leading comment before the generator method and a trailing comment on one
  `yield` but not the other; `normalize-comment-start-case` firing throughout —
  every sentence-fragment comment lowercase-start in `_inp`, capitalized in
  `_out`.
  </details>

Referenced from: `STYLE_JS_TS.md`.

---

## TypeScript

Plain `.ts` only — constructs with no valid JS equivalent, so they can't share the
JavaScript pair above.

- **ts_combined_inp/out.ts** — type annotations, union/intersection type wrapping
  (both break-before and break-after styles), generics, `interface`/`type` alias
  declarations, enums (both value-less and explicit-value forms), class field
  modifiers (all six priority-table slots exercised, including a mixed-modifier-
  length alignment group), decorators (own-line and inline placement, plus the
  two-step overflow cascade).

  <details>
  <summary>Draft content (unverified — see file intro)</summary>

  `ts_combined_inp.ts`:
  ```typescript
  type Status = "active"|"inactive"|"pending";
  type Combined = Base&Extra;

  type LongUnion = FirstOptionName |
  SecondOptionName |
  ThirdOptionName;

  type AnotherLongUnion = FirstOptionName
  | SecondOptionName
  | ThirdOptionName;

  function identity<T>(value:T):T {
  return value
  }

  class Container<T extends Comparable<T> = DefaultItem> {}

  interface BaseProps {
  id:string
  }

  interface Props extends BaseProps {
  label:string
  onSelect?:(id:string)=>void
  tags:readonly string[]
  }

  type Point = {
  x:number
  y:number
  }

  type Keys = keyof Point;

  enum Color {
  Red,
  Green,
  Blue
  }

  enum Status2 {
  Active=1,
  Inactive=2,
  Pending=3
  }

  class Widget extends Base {
  declare public static readonly MAX_COUNT:number
  protected override readonly cache:Map<string,number>
  private static instance:Widget
  }

  class Config {
  private static readonly DEFAULT:string="en"
  private locale:string
  protected count:number
  }

  @Injectable() export class UserAuthenticationAndAuditLoggingServiceForEnterpriseApplications {}

  class MetricsHost {
  @LogPerformanceMetricsAndReportDetailedTimingInformation({threshold: 500, unit: "ms", verbose: true}) process(): void {}
  }
  ```

  `ts_combined_out.ts`:
  ```typescript
  type Status   = "active" | "inactive" | "pending";
  type Combined = Base & Extra;

  type LongUnion = FirstOptionName |
                   SecondOptionName |
                   ThirdOptionName;

  type AnotherLongUnion = FirstOptionName
                        | SecondOptionName
                        | ThirdOptionName;

  function identity<T>(value: T): T
  {
      return value;
  }

  class Container<T extends Comparable<T> = DefaultItem> {} // class Container

  interface BaseProps {
      id : string;
  } // interface BaseProps

  interface Props extends BaseProps {
      label     : string;
      onSelect? : (id: string) => void;
      tags      : readonly string[];
  } // interface Props

  type Point = {
      x : number;
      y : number;
  };

  type Keys = keyof Point;

  enum Color {
      Red,
      Green,
      Blue,
  } // enum Color

  enum Status2 {
      Active   = 1,
      Inactive = 2,
      Pending  = 3,
  } // enum Status2

  class Widget extends Base {
      declare public static readonly MAX_COUNT: number;
      protected override readonly cache: Map<string, number>;
      private static instance: Widget;
  } // class Widget

  class Config {
      private static readonly DEFAULT : string = "en";
      private                 locale  : string;
      protected               count   : number;
  } // class Config

  @Injectable()
  export class UserAuthenticationAndAuditLoggingServiceForEnterpriseApplications {} // class UserAuthenticationAndAuditLoggingServiceForEnterpriseApplications

  class MetricsHost {
      @LogPerformanceMetricsAndReportDetailedTimingInformation(
          { threshold: 500, unit: "ms", verbose: true }
      )
      process(): void {}
  } // class MetricsHost
  ```

  Covers: tight union/intersection spacing on one line, with `type Status` and
  `type Combined` forming a two-member `=`-aligned group since nothing separates
  them (same declaration-alignment grid as the `const` group in `js_combined`).
  Consecutive `type` aliases forming their own `=`-aligned group is now
  confirmed and spelled out explicitly in `STYLE_JS_TS.md` §11.1 (RDD_KEY_183).
  Both break-before and break-after continuation styles preserved exactly as
  written, with only the continuation column re-aligned per §6/§11.1 —
  break-**after** (`LongUnion`) aligns its continuation to the column
  immediately after `=` (where the first operand began), while break-**before**
  (`AnotherLongUnion`) aligns its leading `|` operator to the same column the
  `=` character itself occupies, not one column past it (a correction from an
  earlier off-by-one draft); generics on both a function and a class, including
  a generic default type parameter (`Container<T extends Comparable<T> =
  DefaultItem>`) spaced like an ordinary assignment; `interface`/`type`-alias
  `:` alignment (§14's shape, reused via §11); `interface Props extends
  BaseProps` — interface extension, the `extends` clause sitting on the same
  line as the interface name; a single-member interface (`BaseProps`) still
  getting the mandatory space-before-`:` even alone; an **optional** interface
  member (`onSelect?`) joining its group with `?` counted as part of the padded
  name column (same treatment Kotlin gives nullable-marker characters), and a
  `readonly` array-type member (`tags: readonly string[]`) left untouched since
  no bracket-complexity rule governs type syntax itself; `type Keys = keyof
  Point;` as a single-line `keyof` type alias starting its own one-member group
  after a blank line, not folded into `Point`'s block above it; both enum
  forms — one-per-line always, `=` column-alignment only when explicit values
  are present (§12); the full six-slot modifier order on `Widget` (`declare` →
  visibility → `static` → `abstract`/`override` → `readonly`), and the
  mixed-modifier-length alignment group on `Config` where the shorter modifier
  phrases are padded as a unit so the type column still aligns (§11.2);
  decorator overflow's step 1 only (`@Injectable()` — the combined
  decorator+class line is 111 chars, but dropping the decorator to its own line
  brings the target down to 97 chars, so it stops there) vs. step 2
  (`@LogPerformanceMetricsAndReportDetailedTimingInformation(...)` is 103 chars
  even alone on its own line, so its single object-literal argument gets the
  normal dropped-form call-argument wrap per STYLE.md §3.1) (§9); `identity`'s
  1-line body staying under the 5-line closing-comment threshold, so it gets no
  `// identity` comment — functions aren't exempt from §7's closing-comment
  rule, they just don't clear the length threshold here (a correction from an
  earlier draft that added one anyway).
  </details>
- **ts_comments_inp/out.ts** — uncommon comment placement around the above
  constructs.

  <details>
  <summary>Draft content (unverified — see file intro)</summary>

  `ts_comments_inp.ts`:
  ```typescript
  // Status values
  type Status = "active"|"inactive"|"pending";

  /* Long union, author broke after each operator */
  type LongUnion = FirstOptionName |
  SecondOptionName | // middle option
  ThirdOptionName;

  function identity<T /* the value's type */>(value:T):T {
  return value
  }

  interface Props {
  id:string // unique identifier
  // display label
  label:string
  }

  enum Color {
  Red, // primary
  Green,
  Blue
  }

  enum Status2 {
  Active=1,
  // paused state
  Inactive=2,
  Pending=3 // terminal state
  }

  class Widget extends Base {
  // ambient max count
  declare public static readonly MAX_COUNT:number
  protected override readonly cache:Map<string,number> // lookup cache
  }

  class MetricsHost {
  @LogPerformanceMetricsAndReportDetailedTimingInformation({threshold: 500, unit: "ms", verbose: true}) // heavy metrics decorator
  process(): void {}
  }
  ```

  `ts_comments_out.ts`:
  ```typescript
  // Status values
  type Status = "active" | "inactive" | "pending";

  /* Long union, author broke after each operator */
  type LongUnion = FirstOptionName |
                   SecondOptionName | // middle option
                   ThirdOptionName;

  function identity<T /* The value's type */>(value: T): T
  {
      return value;
  }

  interface Props {
      id : string; // Unique identifier
      // Display label
      label : string;
  } // interface Props

  enum Color {
      Red, // Primary
      Green,
      Blue,
  } // enum Color

  enum Status2 {
      Active = 1,
      // Paused state
      Inactive = 2,
      Pending  = 3, // Terminal state
  } // enum Status2

  class Widget extends Base {
      // Ambient max count
      declare public static readonly MAX_COUNT: number;
      protected override readonly cache: Map<string, number>; // Lookup cache
  } // class Widget

  class MetricsHost {
      @LogPerformanceMetricsAndReportDetailedTimingInformation(
          { threshold: 500, unit: "ms", verbose: true }
      ) // Heavy metrics decorator
      process(): void {}
  } // class MetricsHost
  ```

  Covers: a trailing comment on a middle line of a wrapped union surviving the
  continuation-column realignment; a comment inside a generic type-parameter
  list (`<T /* the value's type */>`), staying tight — a comment alone doesn't
  trigger §13's loose complexity, same stance already established for `T...[0]`
  (CPP26) and `.badge`'s selector-brace comment (CSS); `identity`'s 1-line body
  staying under the 5-line closing-comment threshold, so no `// identity`
  comment is added — functions aren't exempt from §7, they just don't clear the
  length threshold here; a trailing comment on `id` plus a leading comment on
  `label` breaking `interface Props`'s alignment into two single-member groups
  instead of one two-member group (nothing left to align `id`/`label` against
  each other once the comments interrupt the run); a trailing comment on an
  enum member with no explicit value (`Color`'s `Red`); a comment breaking an
  **explicit-value** enum's alignment group (`Status2`'s `Active` isolated as
  its own single-member group; `Inactive`/`Pending` then re-form a fresh
  two-member group after the comment) — contrasted directly with `Color`'s
  value-less case, where a trailing comment doesn't break any alignment since
  there was no `=` to align in the first place; a leading comment inside a
  class body reindented to member depth, and a trailing comment on a
  modifier-heavy field declaration surviving the full six-slot reordering; a
  trailing comment on a decorator that's itself overflow-wrapped (step 2's
  dropped-argument cascade), staying attached to the decorator's closing `)`
  rather than getting stranded or moved onto the method signature it precedes.
  </details>

Referenced from: `STYLE_JS_TS.md`.

---

## Python3

- **py_combined_inp/out.py** — bracket complexity categories (comprehensions,
  slicing, star-unpacking, dict-vs-set disambiguation, walrus-in-comprehension),
  assignment alignment (including augmented-assignment and both continuation-break
  styles), import ordering/grouping (including the non-import-statement group-split
  rule and `from __future__ import` promotion), decorators (including the
  two-step-absent overflow case, and an actually-exercised `@dataclass` class),
  f-strings (expression spacing vs. opaque format-spec), function signature
  wrapping with type hints (`:`/`=` alignment, a bare-no-hint parameter in the same
  group, positional-only/keyword-only markers), structural pattern matching
  (`match`/`case` — type/sequence/mapping/class-deconstruction patterns,
  or-patterns, guard clauses, wildcard `_`), single-statement compound bodies
  (compact `if x: return y` form for `if`/`elif`/`else`/`while`/`for`/`case`, the
  overflow-triggered expansion to block form, and `:`-column alignment across a run
  of compact `case` lines), control-flow blank lines (function-scope-only blank
  line before `return`, blank line before `elif`/`else` triggered by a preceding
  nested `return`/`break`/`continue`), `async`/`await`, and a `@property`/
  `@x.setter` pair (to confirm it's just two ordinary decorated methods with no
  special alignment, per §4's note).

  <details>
  <summary>Draft content (unverified — see file intro)</summary>

  Non-empty `{}` (dict/set) is always loose per §3.3, with no unpacking-only
  carve-out — `config`/`merged`/`a_set`/`a_dict` below are all loose,
  now confirmed and spelled out explicitly in `STYLE_PYTHON3.md` §1.4/§1.5
  (RDD_KEY_184).

  `py_combined_inp.py`:
  ```python
  from __future__ import annotations
  import sys
  import os

  if platform.system() == "Windows":
      import winreg

  import json
  from . import sibling
  from os import path,sep

  flags=0x01
  flags|=0x02
  timeout=100
  retries=3
  # a comment breaks the group
  name="worker"

  total = (something
  + something_else)

  total = (
  something +
  something_else
  )

  squares = [x*x for x in range(10)]
  evens = [x for x in range(10) if x%2==0]
  lookup = {k:v for k,v in items.items()}
  filtered = [y for x in data if (y := transform(x)) is not None]

  a_slice = data[i+1:j-1]
  b_slice = data[ i+1:(j*k)-1 ]
  merged = [*a,*b,*c]
  config = {**defaults,**overrides}
  a_set = {1,2,3}
  a_dict = {"a":1,"b":2}

  @app.route("/users/<int:user_id>/orders/<int:order_id>/items/<int:item_id>/details", methods=["GET","POST"])
  def get_user_order_items(user_id: int, order_id: int, item_id: int):
      ...

  @property
  def x(self) -> int:
      return self._x

  @x.setter
  def x(self, value: int) -> None:
      self._x = value

  @dataclass
  class Point:
      x: int
      y: int
      label: str = "origin"

  def process(extra, x: int, y: "List[int]", name: str = "default", desc: str = "default") -> Optional[str]:
      ...

  def slice_params(pos, /, mid, *, kw: int = 0):
      ...

  def greet(user):
      label = f"Hello {user.first} {user.last}"
      formatted = f"{user.score:.2f}"
      raw = f"{user.score !r}"
      nested = f"{user.score+1:>{width}}"
      return label

  async def fetch_all(ids):
      results = [await fetch(i) for i in ids]
      return results

  def run_command(command):
      match command.split():
          case [action]:
              run(action)
          case [action,obj]:
              run(action,obj)
          case Point(x=0,y=0):
              print("Origin")
          case Point(x=x,y=y) if x==y:
              print("Diagonal")
          case 1|2|3:
              print("small")
          case [1,2,*rest]:
              handle(rest)
          case {"action":action,**rest}:
              handle(action,rest)
          case _:
              unknown()

  def classify(code):
      match code:
          case 1: return "one"
          case 2: return "two"
          case _: return "unknown"

  def check(x):
      if x < 0:
          return None

      if x == 0:
          return 0

      return x * 2

  def process_data(data):
      result = transform(data)
      validate(result)
      return result

  def small(x):
      if x: return x
      while x: x -= 1

  def guarded(some_long_condition_that_is_already_quite_verbose_and_overflows_the_limit_here):
      if some_long_condition_that_is_already_quite_verbose_and_overflows_the_limit_here:
          do_something()
  ```

  `py_combined_out.py`:
  ```python
  from __future__ import annotations

  import os
  import sys

  if platform.system() == "Windows":
      import winreg

  import json
  from . import sibling
  from os import path, sep

  flags    = 0x01
  flags   |= 0x02
  timeout  = 100
  retries  = 3
  # a comment breaks the group
  name = "worker"

  total = (something
        + something_else)

  total = (
      something +
      something_else
  )

  squares  = [ x * x for x in range(10) ]
  evens    = [ x for x in range(10) if x % 2 == 0 ]
  lookup   = { k: v for k, v in items.items() }
  filtered = [ y for x in data if( y := transform(x) ) is not None ]

  a_slice = data[i+1:j-1]
  b_slice = data[ i+1:(j*k)-1 ]
  merged  = [*a, *b, *c]
  config  = { **defaults, **overrides }
  a_set   = { 1, 2, 3 }
  a_dict  = { "a": 1, "b": 2 }

  @app.route(
      "/users/<int:user_id>/orders/<int:order_id>/items/<int:item_id>/details",
      methods=["GET", "POST"],
  )
  def get_user_order_items(user_id: int, order_id: int, item_id: int):
      ...

  @property
  def x(self) -> int:
      return self._x

  @x.setter
  def x(self, value: int) -> None:
      self._x = value

  @dataclass
  class Point:
      x     : int
      y     : int
      label : str = "origin"

  def process(
      extra,
      x    : int,
      y    : "List[int]",
      name : str = "default",
      desc : str = "default"
  ) -> Optional[str]:
      ...

  def slice_params(pos, /, mid, *, kw: int = 0):
      ...

  def greet(user):
      label     = f"Hello {user.first} {user.last}"
      formatted = f"{user.score:.2f}"
      raw       = f"{user.score !r}"
      nested    = f"{user.score + 1:>{width}}"

      return label

  async def fetch_all(ids):
      results = [ await fetch(i) for i in ids ]

      return results

  def run_command(command):
      match command.split():
          case [action]:
              run(action)
          case [action, obj]:
              run(action, obj)
          case Point(x=0, y=0):
              print("Origin")
          case Point(x=x, y=y) if x == y:
              print("Diagonal")
          case 1 | 2 | 3:
              print("small")
          case [1, 2, *rest]:
              handle(rest)
          case { "action": action, **rest }:
              handle(action, rest)
          case _:
              unknown()
      # match command.split()

  def classify(code):
      match code:
          case 1: return "one"
          case 2: return "two"
          case _: return "unknown"
      # match code

  def check(x):
      if x < 0:
          return None

      if x == 0:
          return 0

      return x * 2

  def process_data(data):
      result = transform(data)
      validate(result)

      return result

  def small(x):
      if x: return x
      while x: x -= 1

  def guarded(some_long_condition_that_is_already_quite_verbose_and_overflows_the_limit_here):
      if some_long_condition_that_is_already_quite_verbose_and_overflows_the_limit_here:
          do_something()
  ```

  Covers: import grouping split by the `if platform...`/`winreg` non-import
  statement, `__future__` promoted to its own top group, `os`/`sys` sorted
  alphabetically; an assignment-alignment group with an augmented-assignment
  operator in the same group, and both break-before/break-after continuation-
  alignment targets — break-before's operator now correctly aligned to the exact
  `=` column (a correction from an earlier off-by-one draft, same class of fix as
  `AnotherLongUnion` in `ts_combined`); a fourth member (`filtered`) joining the
  comprehension-assignment group via a walrus operator (`y := transform(x)`)
  inside an `if(...)` filter clause — the comprehension's `if` keyword getting
  the same tight no-space-before-`(` treatment as a statement-level `if` (§3.2),
  with the walrus's wrapping parens going loose because they contain a call,
  nesting-propagating per §3.1; all five bracket-complexity categories
  (comprehensions always loose, slicing colons never spaced even under an
  outer-loose bracket, star-unpacking not itself forcing looseness, dict/set
  disambiguation by top-level `:`); decorator overflow triggering the
  one-per-line drop (109 chars alone, exceeding STYLE.md §2's 100-char limit);
  `@property`/`@x.setter` as two ordinary decorated defs, no special alignment;
  `@dataclass class Point` as the construct §4 already names but the fixture
  never previously exercised, extended with a defaulted field (`label`) to pair
  `:`/`=` alignment at class-body scope (inference — §4's own example never
  shows a default); function signature one-per-line wrap with a bare no-hint
  parameter (`extra`) not influencing the `:` alignment column; `slice_params`'s
  positional-only/keyword-only markers (`/`, `*`) as bare separators
  participating in a signature that fits inline, needing no wrap; f-string
  expression spacing (`user.score + 1`) vs. opaque format-spec/conversion
  (`.2f`, ` !r`, `>{width}` all untouched); `fetch_all`'s `async`/`await`, with
  `await fetch(i)` inside a comprehension staying loose per the ordinary
  comprehension rule and STYLE.md §9's mandatory blank line before `return`
  correctly firing (multi-line body, function-scope return); every
  pattern-matching shape including an or-pattern (`1 | 2 | 3`, not in the style
  doc's own example) plus the closing `# match ...` comment; the compact
  `case N: return ...` run's `:`-column alignment; blank-line-before-`return` at
  function scope only (`check`, `process_data`) vs. never for the compact
  one-liner form (`small`); blank line before the second `if` in `check` since
  the first block ends in `return`; the single-statement-body overflow
  expansion (`guarded`'s condition alone is long enough to force block form even
  though it's a single simple statement).
  </details>
- **py_comments_inp/out.py** — uncommon `#` comment placement around the above
  constructs.

  <details>
  <summary>Draft content (unverified — see file intro)</summary>

  `py_comments_inp.py`:
  ```python
  # Module setup
  import sys
  import os
  # local helper
  from . import sibling

  flags=0x01
  flags|=0x02
  # comment breaks the group
  timeout=100

  # Build the lookup table
  lookup = {k:v for k,v in items.items()} # inline note
  filtered = [y for x in data if (y := transform(x)) is not None] # keep truthy values

  @dataclass
  class Point:
      x: int  # horizontal position
      # vertical position
      y: int

  def process(extra, x: int,  # required
  y: "List[int]") -> Optional[str]:
      ...

  @app.route("/status") # health check endpoint
  def status():
      """
      Health check endpoint.
          Always returns "ok" for now.
      """
      return "ok"

  def dispatch(event):
      match event:
          case "start":
              begin()
          # stopping the process
          case "stop":
              end()
          case _:
              pass

  def check(x):
      if x < 0:
          # negative case
          return None

      # zero case
      if x == 0:
          return 0

      return x * 2

  def classify(code):
      match code:
          case 1: return "one" # first
          case 2: return "two"
          # fallback
          case _: return "unknown"
  ```

  `py_comments_out.py`:
  ```python
  # Module setup
  import os
  import sys

  # local helper
  from . import sibling

  flags  = 0x01
  flags |= 0x02
  # Comment breaks the group
  timeout = 100

  # Build the lookup table
  lookup   = { k: v for k, v in items.items() } # Inline note
  filtered = [ y for x in data if( y := transform(x) ) is not None ] # Keep truthy values

  @dataclass
  class Point:
      x : int  # Horizontal position
      # Vertical position
      y : int

  def process(
      extra,
      x : int,  # Required
      y : "List[int]"
  ) -> Optional[str]:
      ...

  @app.route("/status") # Health check endpoint
  def status():
      """
      Health check endpoint.
          Always returns "ok" for now.
      """
      return "ok"

  def dispatch(event):
      match event:
          case "start":
              begin()
          # Stopping the process
          case "stop":
              end()
          case _:
              pass

  def check(x):
      if x < 0:
          # Negative case
          return None

      # Zero case
      if x == 0:
          return 0

      return x * 2

  def classify(code):
      match code:
          case 1: return "one" # First
          case 2: return "two"
          # Fallback
          case _: return "unknown"
      # match code
  ```

  Covers: a leading `#` comment surviving import-group resort; a comment inside
  an assignment run breaking it into two alignment groups (`flags`/`flags` vs.
  lone `timeout`) — the surviving `flags`/`flags` pair's padding now correctly
  sized to that 2-member group alone (just enough to align the compound `|=`'s
  `=` with the plain `=`), not inflated by padding carried over from a wider
  group elsewhere (a correction from an earlier draft); trailing comments on
  two adjacent comprehension assignments (`lookup`, `filtered`, the latter
  carrying a walrus operator) neither breaking their shared group, since
  trailing comments don't break grouping, only leading/standalone ones do;
  `Point` — a trailing comment on `x` followed by a leading comment before `y`,
  isolating `y` into its own single-member group at class-body annotation
  scope, same leading-comment-breaks-group behavior as everywhere else in this
  project; `process(...)` — a comment placed mid-parameter-list, forcing an
  otherwise-inline-fittable signature into the one-per-line wrapped form purely
  because the comment needs somewhere to live (same forced-multiline effect
  established for JSON5/CSS/JS comment placements), with the trailing comment
  staying attached to `x`'s line and the bare `extra` parameter still
  participating with no `:` to align; a trailing comment on a decorator line; a
  triple-quoted docstring's content — including its own inconsistent internal
  indentation — preserved exactly byte-for-byte, not reflowed or reindented
  beyond its opening `"""` line; `dispatch` as a new function demonstrating a
  comment sitting **between two `case` blocks**, kept separate from
  `classify`'s own case-alignment contrast so that existing test isn't
  disturbed, and — since its match body is short and under the 5-line
  closing-comment threshold — getting no `# match ...` comment, contrasted
  directly with `classify`'s longer body, which keeps its `# match code`
  comment; `# Negative case`/`# Zero case` capitalized as sentence-fragment
  comments per STYLE.md §15 (not left as labels, since they read as descriptive
  fragments, not markers like `// for i`); a trailing comment on a compact
  `case` line, with `case 1`/`case 2` colons landing in the same column with
  zero padding (equal-width patterns need none), contrasted with `case _`
  sitting outside that group, unaligned, due to the preceding `# Fallback`
  comment per §7's all-or-nothing rule. Docstring/multiline-string opaque
  preservation is now confirmed and spelled out explicitly in new
  `STYLE_PYTHON3.md` §10 (RDD_KEY_186).
  </details>

Referenced from: `STYLE_PYTHON3.md`.
