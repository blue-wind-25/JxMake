# Context-Aware Code Formatter — Design Discussion

This document captures the design exploration for a custom code formatter driven by
STYLE.md / STYLE_C_CPP.md / STYLE_JAVA.md. Continue discussion from here.

---

## Problem Statement

Existing formatters (clang-format, Prettier, astyle, uncrustify, google-java-format)
cannot implement the full style guide because they are architecturally limited to
local token-pattern rules. The style guide requires:

- **Expression-tree depth awareness** (§3.1 complexity-based padding)
- **Semantic grouping across lines** (§5 declaration alignment, §6 assignment alignment)
- **Multi-node method analysis** (§14 getter/setter group alignment)
- **Scope stack tracking** (§7 closing comment generation)

These are a fundamentally different class of problem. Even IntelliJ IDEA's formatter
(the closest existing tool) does not handle complexity-based padding or closing comment
generation. VSCode extensions, clang-format, and uncrustify cover roughly 40–50% of
the rules at best.

---

## Key Insight: No AST, No AI Required

All style rules are implementable with a **tokenizer + brace/paren depth counter +
recursive descent on bounded token slices**. No AST parser (Eclipse JDT, tree-sitter)
and no AI is needed.

### Why a tokenizer is sufficient

- **Declaration alignment** — tokenize each declaration into a fixed column grid
  (modifier priority order → type → name → size → comment), compute `max(len)` per
  column across the group, pad to align. Pure arithmetic.
- **Static reorder safety** — before moving a `static` declaration earlier, scan the
  token stream between its new and old positions for any use of its name. Token search,
  no dataflow analysis needed.
- **Closing comments** — maintain a name stack: push on `class Foo {`, pop on `}`.
  Count lines between braces with a counter. No AST needed.
- **Function vs control-flow brace detection** — look behind the `{`: if preceded by
  `)` preceded by a parameter list preceded by a name, it's a function definition.
  Token context is sufficient.
- **§3.1 complexity padding** — recursive descent on the token slice between `(` and
  `)`. Detect identifier + `(` (function call) or nested `()`/`[]`. Propagate loose
  outward. No full parse tree needed.

### §3.1 recursive descent (the hardest rule)

```
function isLoose(tokens between the brackets):
    for each token:
        if token is identifier AND next token is `(`:
            return loose          // function call found
        if token is `(` or `[`:
            recurse into nested bracket content
            if inner result is loose:
                return loose      // propagates outward per rule
    return tight
```

The §3.1 table maps exactly to this — no ambiguous cases remain.

---

## Rule Tier Classification

### Tier 1 — Fully mechanical (local token rules)

Handle locally, deterministically:

- §1 Indentation (4 spaces, consistency)
- §2 Line length (100 char soft limit)
- §3.2 Keyword spacing (`if(`, `while(`, no space before `(`)
- §3.3 `{}` initializer padding (single vs nested vs empty)
- §4 Pre/post increment (`++i` unless post-semantics required)
- §8 Function signature line breaking
- §9 Blank line before `return` (at function scope, multi-line body)
- §10 Single-expression blocks (omit `{}`)
- §11 K&R vs Allman brace style (control-flow vs function definition)
- §12 `else`/`else if` placement (own line after `}`)
- §13 `switch` formatting (one-liner vs multi-line cases, fallthrough)
- C/C++ §1 `void` in empty params (C vs C++ disambiguation)
- C/C++ §2 Function brace Allman style
- C/C++ §3 Template `<>` nesting padding (correctness rule)
- C/C++ §4 `*` and `const` placement
- C/C++ §9 Section dividers (100-char `////...`)
- C/C++ §10 Header file structure (zones, 2-blank-line separation, guard name)
- C/C++ §11 Include ordering (angle bracket vs quote groups, own-header placement)
- Java §2 Method vs class brace style
- Java §4 Double-brace init tight
- Java §7 Import ordering and grouping

### Tier 2 — Tokenizer + grouping + recursive descent (all deterministic)

- **§3.1 Complexity-based padding** — recursive descent on bounded token slice
- **§5 Declaration alignment** — modifier priority grid, max-width columns,
  static reorder via token-search dependency scan
- **§6 Assignment alignment** — operator column alignment including compound
  operators (`|=`, `&=`, `>>=`)
- **§7 Closing comments** — brace counter + name stack, configurable line threshold
- **§14 Getter/setter group alignment** — column grid, exclude outliers by
  line-length check (> 100 chars when inline → exclude)

### Tier 3 — AI-assisted (deferred — not implemented in the JAR)

The JAR implements only Tier 1 and Tier 2. Tier 3 rules exist but are deliberately
out of scope for the deterministic formatter. See the **Future: AI-Assisted Formatting**
section below for the design and rationale.

---

## Architecture: Single Java CLI Tool

### One JAR, three run modes

```
style-fmt --server [--port N]     Start formatting server (default port 17173)
style-fmt --stop                  Kill server via lockfile PID
style-fmt File.java               Auto: use server if present, else standalone
style-fmt --standalone File.java  Force standalone (bypass server check)
```

### Server mode

- Writes `~/.config/style-fmt/server.lock` containing PID and port on startup
- Client mode reads lockfile, checks PID still alive, connects if yes
- Falls back to standalone silently if lockfile absent or PID dead
- Benefit: JVM startup paid once; tokenizer and rule engine stay warm across
  a batch of files — significant speedup for large directories

### Port discovery

- Default port: `17173` (configurable)
- Lockfile contains the actual port used, so client always connects to the right one
- Localhost only — no mDNS, no network discovery

### Data flow

```
style-fmt (client mode)
    │
    ├── server present? ──yes──► HTTP localhost:17173
    │                                │
    │                                ▼
    │                         style-fmt (server mode)
    │                                │
    └── no ──────────────────► in-process
                                     │
                              ┌──────▼──────┐
                              │  Tokenizer  │
                              │ (C/C++/Java │
                              │  unified)   │
                              └──────┬──────┘
                                     │
                              ┌──────▼──────┐
                              │ Rule Engine │
                              │ Tier 1 + 2  │
                              └─────────────┘
```

### No external dependencies

- No Eclipse JDT (AST not needed)
- No tree-sitter (tokenizer is sufficient)
- No AI API (all rules are deterministic)
- No Ollama, no SBC, no mDNS
- Single JAR, runs on any JVM 21+

---

## Tokenizer Design

### Unified tokenizer core

One tokenizer handles C, C++, and Java. Token types differ slightly by language
(keyword sets, `->` vs `.`, etc.) but the tokenizer structure is shared. Language
is passed as a parameter at construction.

### Token types

```
KEYWORD, IDENTIFIER, NUMBER, STRING, CHAR,
OP, PUNCT, COMMENT_LINE, COMMENT_BLOCK,
WHITESPACE, NEWLINE,
PREPROCESSOR,        // C/C++ only: single-line #-directive (opaque)
MACRO_DEF,           // C/C++ only: #define + all \ continuation lines (opaque)
ANGLE_BRACKET_OPEN,  // < in generic/template context (not comparison)
ANGLE_BRACKET_CLOSE  // > in generic/template context
```

### Brace/paren depth counter

Maintained alongside tokenization. Used by:
- Closing comment rule (§7) — line count within a brace pair
- Name stack for named constructs
- §3.1 recursion entry/exit points
- Preprocessor conditional tracking (separate counter, never mixed with code counter)

---

## Language Edge Cases

### Java generics

`<` and `>` are ambiguous — angle brackets in generics vs comparison operators.

**Disambiguation heuristic** (sufficient for formatting, not full type resolution):
- If `<` follows an identifier and the content until the matching `>` contains only
  identifiers, commas, and other `<>` pairs → treat as generic angle brackets
- If the content contains operators (`+`, `-`, `==`, `&&`, etc.) → treat as comparison

This heuristic is correct for well-formed code. For §3.1 complexity padding: nested
generics (`List<Map<String,Integer>>`) are angle brackets, not `()` or `[]`, so they
do not trigger loose padding. Only `()` and `[]` nesting triggers loose.

### C/C++ `extern "C"` blocks

Token sequence: `extern` + string literal `"C"` + `{`. The name stack pushes
`extern "C"` as a literal string (not derived from an identifier). Closing comment:

```cpp
} // extern "C"
```

The string literal context means the brace counter handles it identically to any
other named block — no special case needed beyond recognizing the token sequence.

### C/C++ preprocessor directives

All `#`-directives except `#define` are tokenized as opaque `PREPROCESSOR` tokens
and passed through unchanged:

```
#include, #pragma, #ifdef, #ifndef, #if, #elif, #else, #endif, #undef, #error
```

The brace counter **ignores** braces inside preprocessor conditionals to avoid
mismatched-brace counting when `#ifdef` wraps partial code (e.g. half a struct).
Preprocessor conditionals maintain a separate depth counter used only for
section-divider context (C/C++ §9 triple divider before `#endif`).

### C/C++ macros

**Single-line `#define`** — normalize spacing around `#define` and macro name only.
Body preserved as-is.

**Multiline `#define`** (any `#define` with `\` line continuations) — collected into
a single opaque `MACRO_DEF` token. The entire block is preserved character-for-character.
No formatting rules are applied inside.

Rationale: macro bodies may contain token concatenation (`##`), stringification (`#`),
intentionally mismatched braces, or code that is only valid after preprocessing.
Formatting blindly risks breaking macro expansion.

**Opt-in macro formatting** (`--format-macros` flag):

When explicitly requested, apply only the safest subset:
- Continuation `\` alignment (mechanical, safe)
- Nothing else

A warning is emitted for every multiline macro touched, listing the macro name.

**Macro invocations in code** — `FOO(x, y)` is treated as a function call for §3.1
(identifier + `(` → loose). Multiline macro invocations — preserve internal formatting,
only adjust indentation of the first line.

### Syntax errors in input

If the brace counter goes negative at any point, abort formatting for that file,
emit an error, leave the file untouched. Do not attempt recovery.

---

## Declaration Alignment Algorithm (§5, §6)

### Column grid model

Each declaration in a logical group is parsed into columns:

```
C/C++:  [static] [volatile] [const] [type] [*] [const] [name] [[size]] [comment]
Java:   [access] [static] [volatile] [final] [type] [name] [[]] [comment]
```

Modifier priority is fixed left-to-right. Within each column, `max(len)` across
all rows gives the column width. Each cell is padded to its column width.

### Group boundary detection

A blank line resets alignment — each group aligns independently.

### Static reorder algorithm

1. Parse group into grid
2. Separate statics and non-statics
3. For each static to be moved earlier:
   - Collect token names from lines between target position and original position
   - If static's name appears in any of those tokens → dependency, do not move
   - Otherwise → safe to move, place at top of statics block
4. Within statics block and non-statics block, preserve relative order
5. Apply column alignment to final order

### Assignment alignment (§6)

Find the `=` or compound operator token (`|=`, `&=`, `>>=`, etc.).
Column width = `max(len(lhs + operator))` across the group.
Blank line resets group.

---

## Configuration

### Precedence (low to high)

```
Built-in defaults
    ↓
~/.config/style-fmt/config        user global
    ↓
STYLEFMT_* environment variables  overrides global, overridden by project
    ↓
.style-fmt in project root        per-project (commit to repo)
    ↓
.style-fmt in subdirectory        per-subdir (inherits from parent .style-fmt)
    ↓
CLI flags                         always win
```

Env vars sit below project-level config so a committed `.style-fmt` wins over
CI environment settings, preventing silent CI overrides.

### Config keys

Named by **intent**, not mechanism. The 5-line closing comment threshold is part of
the style definition but is exposed as a simple count because it is genuinely
project-variable.

```properties
# ── Structural constants ──────────────────────────────────────────────────────
line-length                = 100
indent-size                = 4
indent-style               = spaces          # spaces | tabs
server-port                = 17173

# ── Behavior ──────────────────────────────────────────────────────────────────
closing-comment-min-lines  = 5
format-macros              = off             # off | on (multiline #define opt-in)

# ── C/C++ ─────────────────────────────────────────────────────────────────────
include-sort               = off             # off | on (alphabetical within group)
header-guard-rename        = off             # off | on (warn only vs auto-rename)
header-guard-style         = preserve        # preserve | ifndef | pragma-once

# ── Java ──────────────────────────────────────────────────────────────────────
java-import-order          = static, java, org, com, local
java-import-sort           = on              # alphabetical within group
java-import-depth          = 2              # package components defining "local"
java-import-blank-lines    = 1              # blank lines between import groups
```

### What is NOT configurable

Rules that are fixed by the style guide definition and have no legitimate
project-level variation:

- Column alignment behavior (always grid + max-width)
- Brace styles (K&R for control-flow, Allman for functions/methods)
- Complexity padding algorithm (§3.1)
- Blank line counts in header zones (always 2)
- `extern "C"` closing comment text
- Guard name derivation algorithm (filename → uppercase + underscores)

The goal is intentional: making these configurable would recreate uncrustify —
a tool where the config file becomes a second style guide.

---

## Output Modes

```
style-fmt File.java              In-place edit (default)
style-fmt --diff File.java       Print unified diff, do not edit
style-fmt --check File.java      Exit 1 if file would change (CI mode)
style-fmt --out DIR File.java    Write to DIR/File.java instead
```

---

## Build System Integration

Single JAR on PATH. For JxMake / GNU Make:

```makefile
fmt:
    style-fmt --server &    # idempotent: exits immediately if already running
    style-fmt $(SRCS)

fmt-check:
    style-fmt --check $(SRCS)
```

---

## Open Questions / Next Discussion Topics

- [x] Tokenizer implementation — write fresh in Java (resolved: fresh Java tokenizer,
      no external lexer library)
- [x] Rule engine as data vs code — resolved: direct Java methods, grouped into logical
      rule classes (not one class per rule)
- [x] Multi-module Java projects — resolved: `java-import-depth = 2`, top-N components
      of `package` declaration for pre-Java-9 module-less projects
- [x] `.style-fmt` inheritance from parent subdirectory — resolved: full inheritance,
      child keys override parent
- [x] Server mode idempotency — resolved: check lockfile first; if PID not alive treat
      as stale, delete and start fresh (`ProcessHandle.of(pid).isPresent()`)

### Open — minimalist on-device AI prompt design

The JAR's `ai-assist` config hook (see "AI extension design" below) is intended to
invoke a local on-device model (e.g. Ollama, a small quantized model on embedded
hardware) for Tier-3 judgment calls. Testing with Qwen3 1.7B and Qwen-Coder2.5 2B
on a Raspberry Pi CM5 shows:

- Small models **can** make layout *decisions* (inline vs. split params, one-line vs.
  multi-line body) reliably
- Small models **cannot** reformat source correctly by themselves — output quality
  degrades badly without a precise prompt, and even then mechanical execution is error-prone
- The prompt must be extremely precise or the model generates garbage

This suggests the correct design for the on-device path is a **decision-only prompt**:
the JAR sends a minimal context (the expression or signature, the line budget, and a
few-sentence rule summary) and asks the model to return a single token decision
(`inline` / `split` / `split-grouped`). The JAR then executes the chosen form
mechanically. The model never touches source text directly.

- [ ] **Open:** design the minimalist decision-only prompt for the on-device AI path.
      The general-model prompt (`AI_PREAMBLE.md` + full style rules) is **not** suitable
      here — it asks the model to reformat source, which small models do poorly. The
      on-device prompt needs: (1) the candidate expression/signature text, (2) the
      current line-length budget, (3) a one-paragraph rule summary (not the full style
      guide), and (4) an instruction to respond with exactly one decision token.
      This prompt does not exist yet — it is a separate design artifact from
      `AI_PREAMBLE.md` and will live in `SPECIAL_STYLE.md` or a dedicated
      `AI_DECISION_PROMPT.md` once designed.

---

## Future: AI-Assisted Formatting (NOT implemented in the JAR)

> **This section is for future reference only. Do not implement any of this in the
> current JAR.** The JAR is a deterministic, zero-AI tool. Everything here belongs
> to a separate tool or a future extension pass.

### Why some rules cannot be deterministic

A small class of formatting decisions requires judgment that no token-level rule can
supply correctly across all code conventions:

- **Function call line-breaking** — whether a multi-line call should be collapsed
  (it fits in 100 chars) or split further (one argument per line for clarity) depends
  on what the arguments *mean*, not on their length. The formatter cannot know if the
  author broke the call intentionally for readability or accidentally because it was
  long at the time.
- **Getter/setter group boundaries with non-standard naming** — standard prefixes
  (`get`, `set`, `is`, `has`, `not`) are recognizable, but projects use arbitrary
  conventions (`abc()` / `abc(val)`, `xxxHasFeature()`, `notWriteable()`, etc.). Any
  naming-aware grouping heuristic will be wrong for some project. The current rule
  (blank line = group break, comment = group break) is the only safe deterministic
  choice.
- **Comment placement and blank line intent** — a missing blank line before a comment
  may be a formatting oversight or intentional. The formatter cannot distinguish the two.

The common thread: these are cases where the author's *intent* is the input, and intent
is not recoverable from tokens alone.

### SPECIAL_STYLE.md (future)

Rules that require AI assistance will live in `SPECIAL_STYLE.md`, separate from
`STYLE.md` / `STYLE_C_CPP.md` / `STYLE_JAVA.md`. The boundary is intentional and
document-level:

- `STYLE.md` and language extensions → mechanical, implemented in the JAR
- `SPECIAL_STYLE.md` → judgment-call rules, implemented via AI pass only

This keeps the JAR's rule set provably complete and its behavior fully predictable.

### AI extension design (future)

When implemented, the AI pass should be a separate tool or an opt-in flag, not part
of the default JAR invocation. Suggested config hook (not currently in `Config.java`):

```properties
# Future — not implemented
ai-assist   = off                        # off | local | remote
ai-endpoint = http://localhost:11434     # any OpenAI-compatible endpoint (Ollama, etc.)
ai-model    = llama3
```

Using a generic OpenAI-compatible endpoint means the extension works with Ollama
locally, a self-hosted server, or a remote API without changing the formatter code.

**Two distinct invocation paths — different prompt designs:**

**Path A — general/capable model (remote API or large local model):**
The AI receives a single call expression or signature, a few lines of surrounding
context, the relevant `SPECIAL_STYLE.md` rules, and the current line-length budget,
and returns the preferred formatted form. Operates at finer granularity than
`reformat_chunks.py` (which sends 500-line chunks). The JAR splices the result back.
Prompt basis: a trimmed version of `AI_PREAMBLE.md` scoped to `SPECIAL_STYLE.md`
rules only.

**Path B — minimalist on-device model (small quantized model, e.g. on embedded hardware):**
Small models can make layout *decisions* reliably but cannot reformat source correctly.
The JAR sends a decision-only prompt: the candidate expression, the line budget, and a
one-paragraph rule summary — and expects exactly one decision token back (`inline` /
`split` / `split-grouped`). The JAR then executes the chosen form mechanically using
its existing token-level rules. The model never touches source text directly.
Prompt design for this path is an open question — see Open Questions above.

### Current workaround

For one-off style migration of files with many judgment-call decisions, use
`reformat_chunks.py` with the Anthropic API. It is already the recommended path for
files over 500 lines and handles the same class of problem, at coarser granularity.
Note: `reformat_chunks.py` uses Path A semantics (model reformats source directly) and
is not suitable for small on-device models.

---

## Key Decisions

| Decision | Choice | Reason |
|---|---|---|
| Parsing | Tokenizer + recursive descent | AST not needed for any style rule |
| AI dependency | None | All rules deterministic via grid + recursion |
| Language | Java 21+ single JAR | Runs everywhere, no native deps |
| Server mode | Localhost HTTP + lockfile | Amortize JVM startup across batch |
| Port | 17173 default, configurable | Fixed default, lockfile carries actual port |
| Config precedence | defaults → global → env → project → subdir → CLI | Env below project so CI can't override committed style |
| Config naming | Intent-based, not mechanism-based | Avoid uncrustify's unreadable knob-per-rule problem |
| Closing comment threshold | Configurable (`closing-comment-min-lines`) | Simple count, legitimately project-variable |
| C/C++ macros | Opaque by default, opt-in `--format-macros` | Macro bodies not valid C in general |
| Multiline macro opt-in | `\` alignment only | Only change that cannot break expansion |
| Java generics disambiguation | Content heuristic (no operators = angle bracket) | Sufficient for formatting, no type resolution needed |
| Preprocessor brace counting | Separate counter, ignored for code rules | Avoids miscount on partial-struct `#ifdef` blocks |
| `extern "C"` closing comment | `// extern "C"` literal | Name stack pushes string literal content |
| Header zone separation | 2 blank lines between each zone | Clear visual separation of copyright / guard / body |
| Include groups | Angle bracket vs quote, 1 blank line between | Universal C/C++ convention |
| Include sorting | Off by default | Include order can affect behavior via macro deps |
| Java import groups | static / java / org / com / local, 1 blank line | Conventional Java ordering |
| Java local detection | Top-N components of own `package` declaration | No filesystem walk needed |
| SBC component | Dropped | Was solving a problem that no longer exists |
| AI provider rotation | Dropped | No Tier 3 rules in the JAR |
| Ollama | Dropped (for now) | §3.1 solved by recursive descent; future AI pass uses OpenAI-compatible endpoint |
| tree-sitter | Dropped | Tokenizer sufficient |
| Eclipse JDT | Dropped | AST not needed |
| mDNS discovery | Dropped | Localhost only, lockfile sufficient |
| Getter/setter group detection | Adjacent one-liners, broken by blank line or comment | Naming conventions are unbounded; author proximity is the only safe signal |
| Non-standard getter/setter naming | No special handling | `abc()`/`abc(val)`, `xxxHasFeature()`, etc. are all handled by the same adjacent-run rule; no naming-aware heuristic added |
| Line rejoining (under 100 chars) | Not implemented | 100-char limit is a split trigger only, not a join trigger; intent of manual breaks is unknowable |
| Function call line-breaking decisions | Deferred to AI pass / `SPECIAL_STYLE.md` | Requires understanding argument meaning, not just token shape; wrong policy in either direction hurts readability |
| AI extension | Deferred, OpenAI-compatible endpoint | Separate tool / opt-in flag; not part of default JAR; finer granularity than `reformat_chunks.py` |
