/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.grid.CppModifierPriority;
import com.jxmake.formatter.grid.JavaModifierPriority;
import com.jxmake.formatter.grid.ModifierPriority;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeclarationAlignmentRule {

    private static final Set<String> TYPE_KEYWORDS_C = setOf(
            "void", "char", "short", "int", "long", "float", "double", "signed",
            "unsigned", "struct", "enum", "union");

    private static final Set<String> TYPE_KEYWORDS_CPP = union(TYPE_KEYWORDS_C,
            setOf("bool", "wchar_t", "char16_t", "char32_t", "auto", "class"));

    private static final Set<String> TYPE_KEYWORDS_JAVA = setOf(
            "boolean", "byte", "char", "double", "float", "int", "long", "short", "void");

    private final String language;
    private final ModifierPriority modifierPriority;
    private final Set<String> typeKeywords;

    public DeclarationAlignmentRule(String language) {
        this.language = language;
        if ("java".equals(language)) {
            this.modifierPriority = new JavaModifierPriority();
            this.typeKeywords = TYPE_KEYWORDS_JAVA;
        } else {
            this.modifierPriority = new CppModifierPriority();
            this.typeKeywords = "cpp".equals(language) ? TYPE_KEYWORDS_CPP : TYPE_KEYWORDS_C;
        }
    }

    private static Set<String> setOf(String... words) {
        return new HashSet<>(Arrays.asList(words));
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }

    /** One parsed declaration statement. */
    public static final class Declaration {
        public final List<Token> modifiers;
        public final List<Token> typeTokens;
        public final Token name;
        public final List<Token> sizeTokens;
        public final List<Token> initTokens;
        public final Token trailingComment; // nullable
        public final boolean blankLineBefore;

        Declaration(List<Token> modifiers, List<Token> typeTokens, Token name,
                List<Token> sizeTokens, List<Token> initTokens, Token trailingComment,
                boolean blankLineBefore) {
            this.modifiers = modifiers;
            this.typeTokens = typeTokens;
            this.name = name;
            this.sizeTokens = sizeTokens;
            this.initTokens = initTokens;
            this.trailingComment = trailingComment;
            this.blankLineBefore = blankLineBefore;
        }
    }

    /**
     * Splits one scope's direct-content tokens (already extracted by the caller --
     * e.g. one class/struct body at a fixed braceDepth, with no deeper-nested
     * tokens included) into groups of consecutive declaration statements. A blank
     * line, or any statement not recognized as a variable/field declaration, breaks
     * the current group (STYLE.md §5).
     */
    public List<List<Declaration>> groupDeclarations(List<Token> scopeTokens) {
        List<List<Token>> statements = splitStatements(scopeTokens);

        List<List<Declaration>> groups = new ArrayList<>();
        List<Declaration> current = new ArrayList<>();

        for (List<Token> stmt : statements) {
            boolean blankBefore = hasBlankLineBefore(stmt);
            Declaration decl = parseDeclaration(stmt, blankBefore);

            if (decl == null) {
                if (!current.isEmpty()) {
                    groups.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }

            if (blankBefore && !current.isEmpty()) {
                groups.add(current);
                current = new ArrayList<>();
            }
            current.add(decl);
        }

        if (!current.isEmpty()) {
            groups.add(current);
        }
        return groups;
    }

    // ── Statement splitting ─────────────────────────────────────────────────────
    private List<List<Token>> splitStatements(List<Token> scopeTokens) {
        List<List<Token>> statements = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        int n = scopeTokens.size();
        int idx = 0;

        while (idx < n) {
            Token t = scopeTokens.get(idx);
            current.add(t);
            idx++;

            if (t.type == TokenType.PUNCT && ";".equals(t.text)) {
                // Pull in a same-line trailing comment so it stays with this
                // statement instead of becoming the next statement's leading token.
                while (idx < n) {
                    Token next = scopeTokens.get(idx);
                    if (next.type == TokenType.WHITESPACE || next.type == TokenType.COMMENT_LINE
                            || next.type == TokenType.COMMENT_BLOCK) {
                        current.add(next);
                        idx++;
                    } else {
                        break;
                    }
                }
                statements.add(current);
                current = new ArrayList<>();
            }
        }

        if (!current.isEmpty()) {
            statements.add(current);
        }
        return statements;
    }

    private boolean hasBlankLineBefore(List<Token> stmt) {
        int newlineRun = 0;
        for (Token t : stmt) {
            if (t.type == TokenType.NEWLINE) {
                newlineRun++;
                if (newlineRun >= 2) {
                    return true;
                }
            } else if (t.type == TokenType.WHITESPACE) {
                // ignore -- doesn't break or extend the newline run
            } else if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                newlineRun = 0; // a comment line consumes that line's content slot
            } else {
                break;
            }
        }
        return false;
    }

    // ── Declaration parsing ──────────────────────────────────────────────────────
    private Declaration parseDeclaration(List<Token> stmt, boolean blankBefore) {
        Token trailingComment = findTrailingComment(stmt);
        List<Token> sig = significantOnly(stmt);

        if (sig.isEmpty() || !isPunct(sig.get(sig.size() - 1), ";")) {
            return null;
        }
        List<Token> body = sig.subList(0, sig.size() - 1);
        if (body.isEmpty()) {
            return null;
        }

        int i = 0;
        List<Token> modifiers = new ArrayList<>();
        while (i < body.size() && body.get(i).type == TokenType.KEYWORD
                && modifierPriority.isModifier(body.get(i).text)) {
            modifiers.add(body.get(i));
            i++;
        }
        if (i >= body.size()) {
            return null;
        }

        int eqIdx = -1;
        for (int j = i; j < body.size(); j++) {
            if (isOp(body.get(j), "=")) {
                eqIdx = j;
                break;
            }
        }
        List<Token> initTokens;
        int end;
        if (eqIdx >= 0) {
            initTokens = new ArrayList<>(body.subList(eqIdx + 1, body.size()));
            end = eqIdx;
        } else {
            initTokens = new ArrayList<>();
            end = body.size();
        }

        List<Token> sizeTokens = new ArrayList<>();
        while (end > i && isPunct(body.get(end - 1), "]")) {
            int depth = 0;
            int openIdx = -1;
            for (int k = end - 1; k >= i; k--) {
                Token t = body.get(k);
                if (isPunct(t, "]")) {
                    depth++;
                } else if (isPunct(t, "[")) {
                    depth--;
                    if (depth == 0) {
                        openIdx = k;
                        break;
                    }
                }
            }
            if (openIdx < 0) {
                break; // unbalanced -- bail, don't touch this statement
            }
            sizeTokens.addAll(0, body.subList(openIdx, end));
            end = openIdx;
        }

        if (end <= i) {
            return null;
        }
        Token name = body.get(end - 1);
        if (name.type != TokenType.IDENTIFIER) {
            return null;
        }
        List<Token> typeTokens = new ArrayList<>(body.subList(i, end - 1));
        if (typeTokens.isEmpty()) {
            return null;
        }
        Token firstType = typeTokens.get(0);
        if (firstType.type == TokenType.KEYWORD && !typeKeywords.contains(firstType.text)) {
            return null; // e.g. return/throw/assert/break -- not a declaration
        }

        return new Declaration(modifiers, typeTokens, name, sizeTokens, initTokens,
                trailingComment, blankBefore);
    }

    private Token findTrailingComment(List<Token> stmt) {
        for (int k = stmt.size() - 1; k >= 0; k--) {
            Token t = stmt.get(k);
            if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                return t;
            }
            if (t.type != TokenType.WHITESPACE) {
                break;
            }
        }
        return null;
    }

    private List<Token> significantOnly(List<Token> stmt) {
        List<Token> sig = new ArrayList<>();
        for (Token t : stmt) {
            switch (t.type) {
                case WHITESPACE:
                case NEWLINE:
                case COMMENT_LINE:
                case COMMENT_BLOCK:
                    continue;
                default:
                    sig.add(t);
            }
        }
        return sig;
    }

    private boolean isPunct(Token t, String text) {
        return t.type == TokenType.PUNCT && text.equals(t.text);
    }

    private boolean isOp(Token t, String text) {
        return t.type == TokenType.OP && text.equals(t.text);
    }
}
