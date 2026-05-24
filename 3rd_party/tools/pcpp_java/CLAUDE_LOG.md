# CLAUDE_LOG.md — pcpp Java Translation Log

Translated by Claude Sonnet 4.6 on 2026-05-23.

## Status: COMPLETE (all Python source files translated)

---

## Completed Files

All files have been translated. The following table maps each Python source file to its Java equivalents.

| Python Source | Lines | Java Output | Notes |
|---|---|---|---|
| `pcpp/pcmd.py` | 259 | `com/pcpp/pcmd/CmdPreprocessor.java` | Full translation; argparse → hand-rolled arg parser |
| `pcpp/evaluator.py` | 729 | `com/pcpp/evaluator/Evaluator.java`<br>`com/pcpp/evaluator/Value.java` | PLY yacc grammar → Pratt recursive-descent parser |
| `pcpp/parser.py` | 394 | `com/pcpp/parser/CppLexer.java`<br>`com/pcpp/parser/CppParser.java`<br>`com/pcpp/parser/CppTokens.java`<br>`com/pcpp/parser/Macro.java`<br>`com/pcpp/parser/Action.java`<br>`com/pcpp/parser/OutputDirective.java`<br>`com/pcpp/parser/PreprocessorHooks.java` | Token defs + trigraph + hook classes |
| `pcpp/preprocessor.py` | 1405 | `com/pcpp/preprocessor/Preprocessor.java`<br>`com/pcpp/preprocessor/FileInclusionTime.java` | Full translation |
| `pcpp/lextab.py` | 10 | `com/pcpp/LexTab.java` | Generated cache → static constants |
| `pcpp/parsetab.py` | 61 | `com/pcpp/ParseTab.java` | Generated LALR tables → static maps |
| `pcpp/ply/ctokens.py` | 127 | `com/pcpp/ply/CTokens.java` | Token names + regex patterns as constants |
| `pcpp/ply/ygen.py` | 69 | `com/pcpp/ply/YGen.java` | Full translation; shutil+open → java.nio.file |
| `pcpp/ply/lex.py` | 1099 | `com/pcpp/ply/Lexer.java`<br>`com/pcpp/ply/LexToken.java`<br>`com/pcpp/ply/LexRule.java`<br>`com/pcpp/ply/LexPattern.java`<br>`com/pcpp/ply/LexError.java`<br>`com/pcpp/ply/LexerSpec.java`<br>`com/pcpp/ply/LexerReflect.java`<br>`com/pcpp/ply/LexerBuilder.java`<br>`com/pcpp/ply/TokenCallback.java`<br>`com/pcpp/ply/PlyLogger.java`<br>`com/pcpp/ply/NullLogger.java` | Module reflection → LexerSpec interface |
| `pcpp/ply/yacc.py` | 3504 | `com/pcpp/ply/YaccProduction.java`<br>`com/pcpp/ply/YaccSymbol.java`<br>`com/pcpp/ply/YaccError.java`<br>`com/pcpp/ply/YaccBuilder.java`<br>`com/pcpp/ply/LRParser.java`<br>`com/pcpp/ply/LRTable.java`<br>`com/pcpp/ply/LRItem.java`<br>`com/pcpp/ply/LRGeneratedTable.java`<br>`com/pcpp/ply/Grammar.java`<br>`com/pcpp/ply/GrammarRule.java`<br>`com/pcpp/ply/GrammarSpec.java`<br>`com/pcpp/ply/GrammarError.java`<br>`com/pcpp/ply/LALRError.java`<br>`com/pcpp/ply/Production.java`<br>`com/pcpp/ply/MiniProduction.java`<br>`com/pcpp/ply/ParseTabData.java` | Full LALR(1) engine translation |
| `pcpp/ply/cpp.py` | 974 | *(not translated — not imported by pcpp)* | Reference/legacy file only; pcpp uses preprocessor.py instead |

**Total Python lines translated: 7657 of 8631** (ply/cpp.py = 974 lines intentionally skipped)

---

## Java Package Structure

```
com/pcpp/
├── LexTab.java              ← from lextab.py
├── ParseTab.java            ← from parsetab.py
├── evaluator/
│   ├── Evaluator.java       ← from evaluator.py (expression evaluator)
│   └── Value.java           ← from evaluator.py (integer value type)
├── parser/
│   ├── Action.java          ← from parser.py
│   ├── CppLexer.java        ← from parser.py (t_XXX rules → Java regex lexer)
│   ├── CppParser.java       ← from parser.py (trigraph + factory)
│   ├── CppTokens.java       ← from parser.py (token type constants)
│   ├── Macro.java           ← from parser.py
│   ├── OutputDirective.java ← from parser.py
│   └── PreprocessorHooks.java ← from parser.py
├── pcmd/
│   └── CmdPreprocessor.java ← from pcmd.py (CLI entry point)
├── preprocessor/
│   ├── FileInclusionTime.java ← from preprocessor.py
│   └── Preprocessor.java    ← from preprocessor.py (main preprocessor)
└── ply/
    ├── CTokens.java         ← from ply/ctokens.py
    ├── Grammar.java         ← from ply/yacc.py
    ├── GrammarError.java    ← from ply/yacc.py
    ├── GrammarRule.java     ← from ply/yacc.py
    ├── GrammarSpec.java     ← from ply/yacc.py
    ├── LALRError.java       ← from ply/yacc.py
    ├── LRGeneratedTable.java ← from ply/yacc.py
    ├── LRItem.java          ← from ply/yacc.py
    ├── LRParser.java        ← from ply/yacc.py (runtime LALR parser)
    ├── LRTable.java         ← from ply/yacc.py
    ├── LexError.java        ← from ply/lex.py
    ├── LexPattern.java      ← from ply/lex.py
    ├── LexRule.java         ← from ply/lex.py
    ├── LexToken.java        ← from ply/lex.py
    ├── Lexer.java           ← from ply/lex.py (runtime lexer)
    ├── LexerBuilder.java    ← from ply/lex.py (lex() factory)
    ├── LexerReflect.java    ← from ply/lex.py (LexerReflect class)
    ├── LexerSpec.java       ← from ply/lex.py (module spec interface)
    ├── MiniProduction.java  ← from ply/yacc.py
    ├── NullLogger.java      ← from ply/lex.py
    ├── ParseTabData.java    ← from ply/yacc.py
    ├── PlyLogger.java       ← from ply/lex.py
    ├── Production.java      ← from ply/yacc.py
    ├── TokenCallback.java   ← from ply/lex.py
    ├── YaccBuilder.java     ← from ply/yacc.py (yacc() factory)
    ├── YaccError.java       ← from ply/yacc.py
    ├── YaccProduction.java  ← from ply/yacc.py (p[] in grammar actions)
    ├── YaccSymbol.java      ← from ply/yacc.py
    └── YGen.java            ← from ply/ygen.py
```

---

## Key Architectural Differences (Python → Java)

### 1. PLY Lexer (ply/lex.py)
- **Python**: Scans a module for `t_XXX` functions/strings using `inspect`; docstrings are regex patterns.
- **Java**: Rules supplied via `LexerSpec` interface with explicit `@LexRule`-annotated methods, or via programmatic `LexerBuilder`. `CppLexer` hard-codes all C preprocessor rules as named regex groups in a single master pattern.

### 2. PLY Parser (ply/yacc.py) + Expression Evaluator (evaluator.py)
- **Python**: Grammar rules are Python functions; docstrings define BNF productions; PLY generates LALR(1) tables.
- **Java**: Two approaches used:
  - `com.pcpp.ply.*` provides a full LALR(1) engine translation for general use.
  - `com.pcpp.evaluator.Evaluator` replaces PLY yacc with a hand-written Pratt parser for the fixed C preprocessor expression grammar (faster, no table generation needed at runtime).

### 3. Python `copy.copy(tok)` → `tok.copy()`
- All `LexToken.copy()` calls produce shallow copies with independent `expanded_from` lists.

### 4. Python generators (`yield`) → Java `List<LexToken>` accumulation
- `parsegen()` in Python uses `yield`; in Java it accumulates into a `List<LexToken>` returned at the end.

### 5. Python exceptions as control flow
- `OutputDirective` extends `RuntimeException` (unchecked) to mirror Python's exception-as-control-flow pattern.

### 6. argparse → hand-rolled argument parser
- `CmdPreprocessor` implements a simple switch-based arg parser.

---

## TODOs / Notes for Future Work

- `ply/cpp.py` (974 lines): Not translated. This is the original PLY C preprocessor (reference implementation). pcpp's `preprocessor.py` is the actively-used translation. If needed, create `com/pcpp/ply/Cpp.java`.
- `Preprocessor.define(List<LexToken>)`: The `tokens` local variable shadowing needs review (line ~310 in Preprocessor.java) — the method re-initialises `tokens` from the parameter copy.
- `LRGeneratedTable`: LALR table generation for `ply/yacc.py` produces tables at parse time from grammar rules. Full support for the `write_tables` mode (serialising generated tables to disk) is marked TODO in `LexerBuilder` and `YaccBuilder`.
- `CmdPreprocessor.write()` uses `PrintWriter`; `Writer` variant is also provided.
- Java 11+ required (uses `var`, `List.of`, etc.).

---

## Build

### Maven
```bash
cd 3rd_party/tools/pcpp_java
mvn compile
mvn package   # produces target/pcpp-java-1.30.jar
```

### Makefile (alternative to Maven; no mvn required)
```bash
cd 3rd_party/tools/pcpp_java
make          # produces target/pcpp-java-1.30.jar
make JAVAC=/path/to/javac   # use a specific JDK
make clean
```

Run:
```bash
java -jar target/pcpp-java-1.30.jar input.c -o output.c -DFOO=1

# Multi-file mode (equal number of inputs and outputs):
java -jar target/pcpp-java-1.30.jar --line-directive '' --passthru-comments \
    {src1.in,src2.in} -o {out1.tmp,out2.tmp}

# If the shell does not expand braces, the program performs expansion itself.
```

---

## 2026-05-24 — Stage 2 updates

- **Java 8 compatibility**: replaced all Java 11+ APIs with Java 8 equivalents:
  - `var` → explicit types (`FileInclusionTime`, `Map.Entry<...>`)
  - `String.repeat(n)` → `spaces(n)` helper (StringBuilder loop)
  - `String.strip()` → `String.trim()` in `YGen.java`
  - `String.stripTrailing()` → `rtrim()` helper in `YGen.java`
  - `List.of()` → `Collections.<T>emptyList()` in `LexerBuilder.java`
  - Added `import java.util.Collections` to `LexerBuilder.java`
  - `pom.xml`: `maven.compiler.source/target` changed from 11 to 8

- **Multi-file support** in `CmdPreprocessor`:
  - New `expandBraces(String[] argv)`: expands `{a,b,c}` single-arg into multiple args
  - `-o` now accepts multiple consecutive non-flag values (one per input)
  - When N inputs + N outputs (N ≥ 2), the program processes each pair independently with a fresh `CmdPreprocessor` instance
  - `singlePairArgs(Args, int)` helper clones the flag state for one pair
  - Backward-compatible: original single-file invocation unchanged

- **Makefile**: added `Makefile` with configurable `JAVAC` variable (top of file); supports `all` (builds JAR) and `clean` targets; mirrors pom.xml layout.

- **`# OptimIzed by Claude Sonnet 4.6` comment**: this exact line was not found in any Java source file (the prior session used `// Translated by Claude Sonnet 4.6` instead). No removal was performed.
