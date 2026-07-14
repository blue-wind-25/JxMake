# STYLE_DATA_FORMATS.md — Data & Markup Format Rules (JSON/JSON5, XML, CSS, HTML5)

This file covers non-imperative data and markup formats: JSON, JSON5, XML (and its
dialects — XHTML, SVG, MathML, RSS, Atom, Android XML, Maven POM, Ant `build.xml`,
IntelliJ XML, etc.), CSS, and HTML5. Unlike [STYLE.md](STYLE.md), which applies to all
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

### 1.1 Key/Value Alignment

The `:` between key and value is column-aligned within an object, borrowing the same
group/group-break shape as STYLE.md §5 (Variable Declaration Alignment):

```json5
{
    id:          1001,
    displayName: "Widget",
    enabled:     true,
    // a comment breaks the group
    tags:        ["a", "b"],
}
```

- Adjacent same-level keys form a group.
- A blank line or a comment line breaks the group (same as STYLE.md §5).
- A nesting-depth change (entering or leaving a nested object or array) also breaks
  the group — a nested object's keys align independently of its parent's.
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
      { id: 1 },
      { id: 2 }
  ]
  ```

JSON/JSON5 has no function calls, but the same nesting-complexity signal that governs
"contains a call → loose" in STYLE.md §3.1 applies to "contains a nested
object/array → loose" here.

### 1.3 Config

- `json-colon-align` (on/off) — toggles §1.1's key/value alignment.

---

## 2. XML

Covers XML 1.0 and its dialects (XHTML, SVG, MathML, RSS, Atom, Android XML, Maven
POM, Ant `build.xml`, IntelliJ XML, etc.).

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

### 2.3 CDATA

`<![CDATA[ ... ]]>` content is **opaque by default** — preserved verbatim, never
reindented or reflowed, regardless of what it looks like inside. This matches how most
real-world XML formatters treat CDATA, since it exists specifically to protect content
from being parsed as markup.

**Exception:** CDATA that is the direct content of a `<script>` or `<style>` tag (the
old XHTML idiom `<script><![CDATA[ ... ]]></script>`) is unwrapped, and its inner text
is handed to the same script/style dispatch logic described in §4.2 below, then
re-wrapped in `<![CDATA[ ]]>` on output. This is a check inside the existing
dispatcher, not a separate CDATA formatter class.

### 2.4 Config

No new config beyond the existing global `indent-size` / `indent-style`.

---

## 3. CSS

Covers modern CSS (CSS 3+ modules).

### 3.1 Property/Value Alignment

The `:` between property and value is column-aligned within a rule block, same
grouping/group-break shape as §1.1's JSON colon alignment (itself borrowed from
STYLE.md §5):

```css
.widget {
    display:    flex;
    align-items: center;
    color:      #333;
    /* a comment breaks the group */
    margin:     0 auto;
}
```

- Adjacent declarations in the same rule form a group.
- A blank line, a comment, or a nested at-rule/selector boundary breaks the group.

### 3.2 Bracket / Indentation

Standard indent handling for rule blocks (§2.1's global indent config), no bespoke
bracket-complexity rule needed beyond the alignment above.

### 3.3 Config

- `css-colon-align` (on/off) — toggles §3.1's property/value alignment.

---

## 4. HTML5

### 4.1 Tag Formatting

No alignment or other new rule class — HTML5 tag/attribute formatting reuses §2.2's
XML tag rules directly (indentation, attribute wrapping, self-closing tags).

### 4.2 Embedded `<style>` / `<script>` Dispatch

The main design point for HTML5 is dispatch, not new formatting rules:

- Content of a `<style>` tag is spliced out, formatted by §3 (CSS) above, and spliced
  back in with correct re-indentation for its nesting depth in the surrounding markup.
- Content of a `<script>` tag is spliced out, formatted by the JS/TS formatter (see
  **STYLE_JS_TS.md**), and spliced back in the same way. This includes the CDATA-wrapped
  variant described in §2.3.
- A `<script type="...">` with a non-JS/TS type (e.g. `application/json`,
  `text/template`) is not dispatched to the JS/TS formatter; treat as opaque unless a
  future rule specifically recognizes that type.

### 4.3 Config

No HTML5-specific config beyond the CSS/JSON keys above and whatever the JS/TS
formatter defines in its own file.

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
