# STYLE_DATA_FORMATS.md — Data & Markup Format Rules (JSON/JSON5, XML, CSS, HTML5, YAML, TOML)

This file covers non-imperative data and markup formats: JSON, JSON5, XML (and its
dialects — XHTML, SVG, MathML, RSS, Atom, Android XML, Maven POM, Ant `build.xml`,
IntelliJ XML, etc.), CSS, HTML5, YAML, and TOML. Unlike [STYLE.md](STYLE.md), which applies to all
imperative languages (function signatures, switch formatting, getter/setter grouping,
and so on), most of STYLE.md does not apply here — these formats have no functions and
no control flow. Each subsection below states explicitly which STYLE.md sections it
borrows from; nothing else in STYLE.md should be assumed to apply.

For JavaScript/TypeScript specifically — including how HTML5 dispatches embedded
`<script>` content to it — see **STYLE_JS_TS.md** (referenced here by filename only,
not by section number, so this file doesn't go stale if that file's section numbers
change).

---

## 1. JSON / JSON5

Covers JSON (RFC 8259) and JSON5.

JSON5 supports both `//` and `/* */` comments, preserved exactly as written — no
normalization between the two styles. Plain JSON (RFC 8259) has no comment syntax at
all, so `JXM_CFMT_DIS`/`ENA`/`CFG` directives (which are comment-based markers) are
usable in JSON5 but not in plain JSON — a `.json` file has no way to carry them.

### 1.1 Key/Value Alignment

The `:` between key and value is column-aligned within an object, borrowing the same
group/group-break shape as STYLE.md §5 (Variable Declaration Alignment). Unlike
STYLE.md §5/§6's `=` alignment (which sticks to the identifier with no space before
it), `:` always gets at least one space before it, even in an unaligned single-key
object — the colon never sticks directly to the key:

```json5
{
    id          : 1001,
    displayName : "Widget",
    enabled     : true,
    // a comment breaks the group
    tags : ["a", "b"],
}
```

- Adjacent same-level keys form a group.
- A blank line or a comment line breaks the group (same as STYLE.md §5).
- A nesting-depth change (entering or leaving a nested object or array) also breaks
  the group — a nested object's keys align independently of its parent's.
- Within a group, keys are padded so the `:` column aligns; the padding space(s) go
  between the key and `:`, not between `:` and the value — one space after `:`
  always, regardless of group width.
- JSON5-specific syntax (unquoted keys, trailing commas, inline/block comments)
  doesn't change this alignment logic; comments simply break groups, same as blank
  lines. The tokenizer must recognize JSON5's productions before an alignment pass
  can run over them.

### 1.2 Bracket / Complexity Padding

Reuses STYLE.md §3.1's tight/loose bracket rule directly:

- An array of atoms stays tight: `[1, 2, 3]`
- An array containing objects or nested arrays goes loose:
  ```json5
  [
      { id : 1 },
      { id : 2 }
  ]
  ```

JSON/JSON5 has no function calls, but the same nesting-complexity signal that governs
"contains a call → loose" in STYLE.md §3.1 applies to "contains a nested
object/array → loose" here.

### 1.3 Multi-line Strings (Line Continuation)

JSON5-only — plain RFC 8259 JSON has no line-continuation syntax, so a string in a
`.json` file can never contain a raw newline. JSON5 allows a string to span
multiple source lines via a backslash immediately followed by a line terminator;
only that `\`+newline sequence is elided from the string's value — any other
character on the continuation line, including leading whitespace, is literal string
content, not structural indentation:

```json5
{
    message: "First line \
second line \
third line",
}
```

The formatter treats the entire string — opening quote through closing quote,
including every embedded continuation line — as **opaque, preserved exactly as
written**, same treatment as STYLE_JAVA17.md §4's text blocks and STYLE_JS_TS.md
§4's template literals. This means:

- Whitespace at the start of a continuation line is **never** stripped, added, or
  realigned, even though it visually looks like it could be structural indentation.
- This holds even when the surrounding object's indentation level changes (e.g. the
  string moves one nesting level deeper during reformatting) — the string's internal
  content doesn't follow the new indent level, exactly as STYLE_JAVA17.md §4
  specifies for text blocks moving inside differently-indented surrounding code.
- Only the *position of the opening quote* participates in §1.1's colon alignment;
  nothing after it, across all continuation lines, is touched.

§1.1's key/value alignment is unconditional, same as STYLE.md §5/§6's declaration
and assignment alignment — no config toggle, for the same reason those have none:
alignment is baseline formatting behavior in this project, not an opt-in feature.

---

## 2. XML

Covers XML 1.0 and its dialects (XHTML, SVG, MathML, RSS, Atom, Android XML, Maven
POM, Ant `build.xml`, IntelliJ XML, etc.).

XML comments (`<!-- ... -->`) are the only comment form — no line-comment equivalent —
so `JXM_CFMT_DIS`/`ENA`/`CFG` directives use a single syntax, not the two-form
(line/block) pattern C-family files use: `<!--% JXM_CFMT_DIS -->`,
`<!--% JXM_CFMT_ENA -->`, `<!--% JXM_CFMT_CFG ... -->`. This applies to HTML5 as well
(§4), since HTML5 shares XML's `<!-- -->` comment syntax.

### 2.1 Indentation

Uses the formatter's existing global `indent-size` / `indent-style` config keys — the
same ones every other supported language uses (see README.md's config table). There is
no XML-specific indent config, and no special-casing of tabs vs. spaces for XML: an
`indent-style = auto` setting detects from the file's existing indentation exactly as
it would for any other language.

### 2.2 Tag / Attribute Formatting

Standard formatting only — no alignment or other new rule class:

- Nesting indents one level per open tag, same shape as STYLE.md's general
  bracket/indent handling applied to tags instead of braces.
- An overflowing tag (beyond STYLE.md §2's line-length limit) wraps its attributes,
  one per line, indented one level from the tag.
- Self-closing tags (`<br/>`) are preserved as self-closing; a tag with no content is
  not expanded into an open/close pair.
- Attribute order is preserved exactly as written — no sorting or reordering. This
  includes `xmlns` declarations: since a namespace declaration's scope is the element
  it appears on (and its descendants), moving it — even just relative to other
  attributes on the same tag — is never done, consistent with not reordering
  attributes at all.

### 2.3 Doctype / Processing Instructions

`<!DOCTYPE ...>` and processing instructions (`<?xml version="1.0"?>`,
`<?xml-stylesheet ...?>`) are **opaque, preserved verbatim on their own line** —
same treatment as CDATA (§2.4) — never reflowed or reindented, regardless of length.
A DOCTYPE's internal subset (nested `<!ENTITY ...>`/`<!ELEMENT ...>` declarations) has
a different grammar than tag attributes, so §2.2's attribute-wrap rule does not apply
to it; rather than inventing separate DTD-formatting logic for a rare legacy
construct, it's simply left untouched.

### 2.4 CDATA

`<![CDATA[ ... ]]>` content is **opaque by default** — preserved verbatim, never
reindented or reflowed, regardless of what it looks like inside. This matches how most
real-world XML formatters treat CDATA, since it exists specifically to protect content
from being parsed as markup.

**Exception:** CDATA that is the direct content of a `<script>` or `<style>` tag (the
old XHTML idiom `<script><![CDATA[ ... ]]></script>`) is unwrapped, and its inner text
is handed to the same script/style dispatch logic described in §4.2 below, then
re-wrapped in `<![CDATA[ ]]>` on output. This is a check inside the existing
dispatcher, not a separate CDATA formatter class.

### 2.5 Multi-line Comments

A `<!-- ... -->` comment whose raw interior (the text between `<!--` and `-->`, before
any trimming) contains a newline is **opaque, preserved verbatim** — same treatment as
the DOCTYPE/PI (§2.3) and CDATA (§2.4) content above: no reindentation, no reflow, no
`normalize-comment-start-case`/single-word-directive detection or any other
content-inspecting normalization applied to it. This includes each interior line's own
leading whitespace and the position of the closing `-->`:

```xml
<!--
    Copyright (C) 2024 Example Corp.
    SPDX-License-Identifier: MIT
-->
```

renders byte-for-byte identically to how it appears in the source. This applies to
HTML5 as well (§4), since HTML5 shares XML's comment syntax and parser.

**Scoping — this only affects the comment's own interior content.** A single-line
comment (no interior newline, regardless of unusual internal spacing/dashes) is
unaffected and still goes through the normal path (trim, case-normalize, etc.). The
comment node's own placement/indentation (where `<!--` itself starts) is unaffected —
it's still placed at the current indent depth like any other sibling node; sibling
nodes immediately before or after the comment reindent normally, exactly as if the
comment weren't there.

---

## 3. CSS

Covers modern CSS (CSS 3+ modules).

CSS supports only `/* */` block comments — no `//` line-comment form — so
`JXM_CFMT_DIS`/`ENA`/`CFG` directives use the single block form:
`/*% JXM_CFMT_DIS */`, `/*% JXM_CFMT_ENA */`, `/*% JXM_CFMT_CFG ... */`, same syntax
C-family files already use for their block-comment directive form, just without a
line-comment counterpart.

### 3.1 Property/Value Alignment

The `:` between property and value is column-aligned within a rule block, same
grouping/group-break shape as §1.1's JSON colon alignment (itself borrowed from
STYLE.md §5), including §1.1's space-before-`:` convention — the colon never sticks
directly to the property name:

```css
.widget {
    display     : flex;
    align-items : center;
    color       : #333;
    /* a comment breaks the group */
    margin : 0 auto;
}
```

- Adjacent declarations in the same rule form a group.
- A blank line, a comment, or a nested at-rule/selector boundary breaks the group.
- An at-rule keyword (`@media`, `@supports`, `@keyframes`, `@font-face`, etc.) is a
  rule *header*, not a declaration — its condition (e.g. `(min-width: 600px)`) never
  joins an alignment group, and stacked at-rules never align against each other's
  conditions, the same way sibling selectors (`.a`/`.b`) never align against each
  other. An at-rule's block content recurses into normal rule-block handling: nested
  selectors (`@media`, `@supports`, `@keyframes`) get their own independent
  colon-alignment groups per nested block, while a flat-declaration at-rule
  (`@font-face`, which has no nested selector) colon-aligns directly like any other
  rule block.

```css
@media (min-width: 600px) {
    .widget {
        display     : flex;
        align-items : center;
    }
}

@font-face {
    font-family : "Custom";
    src         : url("custom.woff2") format("woff2");
}
```

**Native nesting (`&`)** — a nested plain-selector block (CSS Nesting Module, not
an at-rule) is the same "selector boundary" case named above, not a new mechanism:
a `&`-prefixed nested rule is a *header* exactly like an at-rule is, so it never
joins its parent's alignment group, and its own declarations start a fresh,
independently-aligned group one level deeper — same recursion §3.1 already applies
to `@media`'s block content:

```css
.widget {
    display : flex;
    color   : #333;

    &:hover {
        color  : #555;
        cursor : pointer;
    }

    & .icon {
        margin-right : 4px;
    }
}
```

A blank line before a nested `&` block is not required, same optional-grouping
posture as STYLE.md §12's `else` blank-line rule — shown above for readability, not
because the rule mandates it.

### 3.2 Bracket / Indentation

Standard indent handling for rule blocks (§2.1's global indent config), no bespoke
bracket-complexity rule needed beyond the alignment above.

§3.1's property/value alignment is unconditional, same reasoning as §1.1's JSON
colon alignment above — no config toggle, matching STYLE.md §5/§6 having none.

---

## 4. HTML5

### 4.1 Tag Formatting

No alignment or other new rule class — HTML5 tag/attribute formatting reuses §2.2's
XML tag rules directly (indentation, attribute wrapping), with one HTML5-specific
override:

**Void elements** — `area`, `base`, `br`, `col`, `embed`, `hr`, `img`, `input`,
`link`, `meta`, `param`, `source`, `track`, `wbr` — never have a closing tag by the
HTML5 spec, and the formatter normalizes away any self-closing `/`, matching the
WHATWG Living Standard's own example syntax (the slash is legal but has no effect,
so it's noise):

```html
<br>
<img src="photo.jpg" alt="A photo">
<input type="text" name="q">
```

not `<br/>`, `<img src="photo.jpg" />`, `<input type="text" />`. This overrides
§2.2's general "self-closing tags are preserved as self-closing" rule specifically
for this element list — everywhere else in HTML5 (and all of XML/XHTML), §2.2's
preserve-as-written behavior is unchanged.

### 4.2 Embedded `<style>` / `<script>` Dispatch

The main design point for HTML5 is dispatch, not new formatting rules:

- Content of a `<style>` tag is spliced out, formatted by §3 (CSS) above, and spliced
  back in with correct re-indentation for its nesting depth in the surrounding markup.
- Content of a `<script>` tag is spliced out, formatted by the JS/TS formatter (see
  **STYLE_JS_TS.md**), and spliced back in the same way. This includes the CDATA-wrapped
  variant described in §2.4.
- A `<script type="...">` with a non-JS/TS type (e.g. `application/json`,
  `text/template`) is not dispatched to the JS/TS formatter; treat as opaque unless a
  future rule specifically recognizes that type.

### 4.3 `<pre>` Content and Bare Text Nodes

**`<pre>` is opaque**, same treatment as CDATA (§2.4's default case) — its content is
preserved byte-for-byte, including exact whitespace and line breaks, since reflowing
or reindenting it would change the rendered output (`<pre>` disables normal HTML
whitespace collapsing). The `<pre>` tag itself is not reindented to the surrounding
structural depth the way an ordinary content line would be — only the tags
immediately before/after it participate in normal indentation.

**Bare text nodes** (text sitting directly between element siblings, not inside a
tag) reindent to their parent's structural depth like any other content line, same
as an element child would — by analogy with §2.2's normal tag-indentation rule,
applied to text content rather than a nested tag (RDD_KEY_185).

---

## 5. YAML

Covers YAML 1.1/1.2, including multi-document streams.

YAML has only `#` line comments — no block-comment form — so `JXM_CFMT_DIS`/`ENA`/`CFG`
directives use a single line-comment syntax, not the two-form (line/block) pattern
C-family files use: `#% JXM_CFMT_DIS`, `#% JXM_CFMT_ENA`, `#% JXM_CFMT_CFG ...`. This is
the same "only one comment form exists, so only one directive syntax exists" posture
as XML/CSS (§2/§3), just with a line form instead of a block form.

### 5.1 Indentation

Uses the formatter's existing global `indent-size` config key, same as every other
supported language — no YAML-specific default. **`indent-style` does not apply to
YAML and is ignored**: the YAML spec forbids tab characters for structural
indentation entirely, so YAML output is always space-indented regardless of the
configured `indent-style` (spaces/tabs/auto). This is the one config key in this
entire file that a format has to explicitly opt out of, rather than just reusing —
called out here so it isn't mistaken for an oversight.

### 5.2 Key/Value Alignment

The `:` between a mapping key and its value is column-aligned within a group, same
grouping/group-break shape as §1.1's JSON colon alignment (itself borrowed from
STYLE.md §5), including §1.1's space-before-`:` convention — the colon never sticks
directly to the key:

```yaml
name        : Widget
displayName : Widget Extended
enabled     : true
# a comment breaks the group
tags        : [a, b]
```

- Adjacent same-level mapping keys form a group.
- A blank line, a comment line, or a nesting-depth change breaks the group, same as
  §1.1.
- A sequence item that is itself a mapping (`- name: Widget`) starts its own
  independent alignment group, same recursion §3.1 already applies to nested
  selector/at-rule blocks.

### 5.3 Sequences (Lists)

A `-` sequence item indents **one level deeper** than its parent mapping key:

```yaml
fruits:
    - apple
    - banana

items:
    - name  : Widget
      price : 9.99
    - name  : Gadget
      price : 4.5
```

A sequence item that is itself a mapping keeps its first key inline after the `-`;
subsequent keys in that same item-mapping align one column past the `-` and space,
under the first key — shown above with `items`.

### 5.4 Flow-Style Collections

Flow-style mappings/sequences (`{a: 1}`, `[1, 2]`) are **preserved as written** by
default — a block-style construct in the source stays block, a flow-style construct
stays flow. Internal spacing within a flow collection that stays flow-style follows
the same tight-atom spacing STYLE.md/§1.2 already use (single space after `,` and
`:`).

**Exception:** if a flow-style collection would exceed the configured `line-length`
if kept on one line, it is converted to block style instead — the same "does it fit"
overflow signal that drives XML's attribute-wrapping (§2.2) and JSON's tight/loose
array rule (§1.2), applied here as a flow→block conversion rather than a wrap:

```yaml
# fits within line-length -> stays flow, as written
point: {x: 1, y: 2}

# exceeds line-length as flow -> converted to block
config:
    timeout: 30
    retries: 5
    endpoints:
        - https://a.example.com
        - https://b.example.com
```

A block-style construct is never converted to flow, regardless of how short it is —
this rule is one-directional (overflow forces flow→block; nothing forces block→flow).

### 5.5 Anchors, Aliases, and Tags

`&anchor`, `*alias`, and explicit tags (`!!str`, `!!int`, custom `!Tag`) are
**preserved verbatim** wherever they appear — no special formatting logic beyond
normal alignment/indentation of the surrounding key or sequence item they're attached
to. An anchor/tag sitting in a value position does not change that key's
participation in §5.2's colon-alignment group.

### 5.6 Block Scalars

Literal (`|`) and folded (`>`) block scalars, with their optional chomping (`-`, `+`)
and explicit indentation indicators, are **opaque, preserved exactly as written**
beyond the header line — same treatment as JSON5's line-continuation strings (§1.3)
and XML's CDATA (§2.4):

```yaml
description: |
    First line.
      Indented on purpose.
    Third line.
```

Only the header line (`key: |`) participates in §5.2's colon alignment; nothing in
the block scalar's body is reindented, reflowed, or realigned, even if the
surrounding mapping's indentation level changes.

### 5.7 Multi-Document Streams

`---` document separators and `...` end markers are preserved as written. Each
document's indentation is independent — structural depth resets at every `---`
boundary, it does not carry over or compound across documents in the same stream.

§5.2's key/value alignment is unconditional, same reasoning as §1.1's JSON colon
alignment — no config toggle.

---

## 6. TOML

Covers TOML v1.0.

TOML has only `#` line comments — same single-form directive posture as YAML (§5):
`#% JXM_CFMT_DIS`, `#% JXM_CFMT_ENA`, `#% JXM_CFMT_CFG ...`.

### 6.1 Key/Value Alignment

The `=` between a key and its value is column-aligned within a group, reusing
STYLE.md §5/§6's assignment-alignment shape directly — unlike JSON/YAML's `:`,
TOML's `=` needs no forced space-before-it rule, since STYLE.md §5/§6's own
convention already pads up to (and never sticks to) the identifier:

```toml
name        = "widget"
version     = "1.0.0"
description = "A sample package"
```

- Adjacent top-level or same-table key/value lines form a group.
- A blank line, a comment line, or a table-header boundary (§6.2) breaks the group.

### 6.2 Tables and Array-of-Tables Headers

`[section]` and `[[array-of-table]]` headers are rule *headers*, not declarations —
same distinction as CSS's at-rules/selectors (§3.1) — so a header never joins an
alignment group.

Key/value pairs under a table header get **no added indentation**: TOML expresses
nesting through dotted header names (`[section.subsection]`), not through
indentation, so top-level keys under any `[section]` sit at column 0, matching
real-world TOML tooling conventions (e.g. `taplo`, `cargo fmt`-adjacent formatters)
rather than this project's usual "nesting indents one level" default:

```toml
[package]
name    = "widget"
version = "1.0.0"

[package.metadata]
color = "blue"

[[bin]]
name = "widget-cli"
path = "src/main.rs"
```

Blank lines between table sections are preserved as written, not mandated or
stripped — same optional-grouping posture as STYLE.md §12's blank-line-before-`else`
guidance and CSS's blank-line-before-`&` (§3.1).

### 6.3 Arrays

Reuses STYLE.md §3.1/§1.2's tight/loose bracket rule directly: an array of atoms
stays tight (`[1, 2, 3]`); an array containing an inline table or a nested array goes
loose, one element per line, same as JSON's §1.2 treatment.

### 6.4 Inline Tables

`{ key = "value" }` inline tables are **always single-line** — this is a TOML v1.0
grammar constraint, not a style choice (the spec forbids a line break inside an
inline table), so unlike arrays there is no tight/loose decision to make here. Only
internal spacing is normalized: one space after `{`, before `}`, and after each `,`.

### 6.5 Dotted Keys and String Types

Dotted keys (`a.b.c = 1`) are left exactly as written — never expanded into a nested
`[a.b]`-style table header, and never the reverse. A dotted key and a nested table
header are semantically distinct TOML constructs; this formatter does not rewrite
one into the other.

Basic strings (`"..."`), literal strings (`'...'`), and their multi-line forms
(`"""..."""`, `'''...'''`) are preserved verbatim, including quote style — no forced
normalization, same "preserve as written" posture as JSON5's per-key quote-style
preservation (§1.1). A multi-line string's body is opaque, preserved exactly as
written, same treatment as YAML block scalars (§5.6) and JSON5's line-continuation
strings (§1.3).

§6.1's key/value alignment is unconditional, same reasoning as §1.1's JSON colon
alignment — no config toggle.
