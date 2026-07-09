# Java keyword-ambiguity examples (KEYWORDS_JAVA set)

| # | Comment text | paren? | semi? | url/num? | Label | Why |
|---|---|---|---|---|---|---|
| 1 | `static analysis flags this as dead code` | no | no | no | YES | "static" as an adjective, prose (mirrors the C example — Java's keyword set overlaps here, but the list is kept separate per RDD_KEY_96 since the two languages diverge elsewhere, e.g. `record`/`var`/`yield`). |
| 2 | `static Map<String,Integer> CACHE = new HashMap<>();` | no | yes | no | NO | Declaration restated in the comment; semicolon fires. |
| 3 | `default values are applied when the field is omitted` | no | no | no | YES | Prose describing behavior. |
| 4 | `default: throw new IllegalStateException();` | yes | yes | no | NO | `throw(`-shaped is not literally present, but paren/semi both fire from `IllegalStateException()` — clearly code. |
| 5 | `final answer to this question is "it depends"` | no | no | no | YES | "final" as an adjective ("final answer"), prose false-friend case. |
| 6 | `final long TIMEOUT_MS = 5000;` | no | yes | yes | NO | Both semicolon and the numeric literal fire alongside a declaration shape. |
| 7 | `native code path is only used on Windows` | no | no | no | YES | "native" as an adjective, prose. |
| 8 | `native void init();` | yes | yes | no | NO | Declaration fragment, both signals fire. |
| 9 | `transient state should never be persisted here` | no | no | no | YES | Prose explaining a design rule using "transient" as an adjective. |
| 10 | `transient int retryCount;` | no | yes | no | NO | Field declaration. |
