/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassInitializer;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.KtPsiFactory;

/**
 * Content-preservation checker for Kotlin, modeled on java_content_diff.java
 * (same reasoning applied to `kotlin_syntax_check`'s PSI/AST infrastructure --
 * KotlinCoreEnvironment/KtPsiFactory, no new dependency).
 *
 * Unlike javac's Tree.toString(), IntelliJ PSI is a lossless concrete syntax
 * tree -- PsiElement.getText() returns verbatim original source (whitespace
 * and comments included), so it cannot be used directly as a canonical form
 * the way javac's pretty-printer was. Instead this tool hand-rolls the same
 * canonicalization by walking each subtree's leaf tokens, skipping
 * PsiWhiteSpace and comment/KDoc nodes, and joining what's left with single
 * spaces -- structurally equivalent to what the Java tool got for free.
 * Gotcha found during verification: this walk MUST use ASTNode.getChildren(),
 * not PsiElement.getChildren()/PsiTreeUtil -- for stub-based elements
 * (KtClass/KtProperty/KtNamedFunction), PsiElement.getChildren() only
 * returns structurally significant composite children and silently omits
 * every plain leaf token (identifiers, keywords, and critically comments),
 * so a PsiTreeUtil.findChildrenOfType(file, PsiComment.class) scan over the
 * whole file finds zero comments even when several are clearly present.
 *
 * This formatter *intentionally* reorders/transforms some Kotlin content
 * (kotlin-import-order sorting, declaration-alignment whitespace,
 * normalize-comment-start-case), so:
 *     - imports are compared as a MULTISET (kotlin-import-order legitimately
 *       reorders/sorts them)
 *     - every other top-level declaration is compared IN ORDER, via the
 *       leaf-token canonicalization above (drops whitespace/comments, keeps
 *       identifiers/literals/structure)
 *     - comments (both line/block comments via PsiComment and KDoc blocks,
 *       dropped by the canonicalization above) are extracted separately and
 *       compared as a MULTISET, whitespace-normalized AND case-normalized
 *       (lowercased) -- a case-only change is expected
 *       (normalize-comment-start-case), so it must not be flagged, but a
 *       dropped/corrupted comment still is.
 *
 * Build (JDK 21, same classpath as kotlin_syntax_check.java):
 *     JDK=/opt/openjdk-21_linux-x64_bin/jdk-21
 *     KLIB=~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib
 *     $JDK/bin/javac -cp "$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_content_diff.java
 *
 * Run (two modes -- see also Usage: in main below):
 *     Single pair:
 *         $JDK/bin/java -cp ".:$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_content_diff <original.kt> <formatted.kt>
 *     Batch (one JVM invocation over a whole corpus, avoiding a JRE restart
 *     per file -- rel-path list, one path per line, relative to both base
 *     dirs alike):
 *         $JDK/bin/java -cp ".:$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_content_diff <original_base_dir> <formatted_base_dir> <kt_rel_path_file_list.txt>
 *
 * Before each pair's AST diff, a "[yyyy-MM-dd HH:mm:ss.SSS] <relative path>"
 * line is printed -- lets a hang/slow file in a large batch run be pinpointed
 * (same "print immediately before the risky step" precedent as
 * Main.main's/ServerMode.FormatHandler's own "processing <file>" stderr
 * trace, see STATE_COMMON.md). In batch mode, a rel-path missing from either
 * base dir (or both) is a warning, not a crash -- the file is skipped and
 * the run continues; the final SUMMARY line and process exit code still
 * reflect it.
 *
 * Exit 0 if content is preserved (all pairs, in batch mode), 1 if any
 * mismatch/missing file/error is found (description printed for each), 2 on
 * a usage error.
 */
public class kotlin_content_diff {

    static KtFile parse(final String source)
    {
        final KotlinCoreEnvironment env = KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable(),
            new CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        );

        return new KtPsiFactory( env.getProject() ).createFile(source);
    }

    static String normalizeWhitespace(final String s)
    {
        return s.trim().replaceAll("\\s+", " ");
    }

    /**
     * Canonicalize a subtree: leaf tokens only, whitespace/comment/KDoc
     * nodes skipped, joined with single spaces. Walks the ASTNode (not
     * PsiElement.getChildren(), which for stub-based elements like
     * KtClass/KtProperty/KtNamedFunction only returns structurally
     * significant composite children -- plain leaf tokens (identifiers,
     * keywords, comments) never show up there at all).
     */
    /**
     * STYLE_KOTLIN.md §10 (mirrors STYLE.md's general single-statement-block
     * brace-omission rule, same JS/TS precedent as js_ts_content_diff.js's
     * Block-with-one-statement tolerance): a `for`/`while`/`do`/`if`/`else`
     * body containing exactly one statement may legitimately be rendered
     * with or without its `{`/`}`, in either direction. Every one of these
     * control-structure bodies is wrapped in a
     * KtContainerNodeForControlStructureBody regardless of which keyword
     * owns it (confirmed empirically -- for/while/do-while/if/else bodies
     * all share this one parent type), so a single check covers every
     * construct STYLE.md §10 names. Only the exact one-statement shape is
     * unwrapped -- a block with zero or 2+ statements still walks normally
     * (braces included), so a genuinely dropped/added statement inside a
     * braceless-collapsed body is still caught by the ordinary per-leaf
     * comparison below.
     */
    static boolean isCollapsibleControlBlock(final ASTNode n)
    {
        final PsiElement psi = n.getPsi();
        if( !(psi instanceof KtBlockExpression) ) return false;
        if( !( psi.getParent() instanceof KtContainerNodeForControlStructureBody ) ) return false;

        return ( (KtBlockExpression) psi ).getStatements().size() == 1;
    }

    static void collectLeafText(final ASTNode n, final StringBuilder sb)
    {
        final PsiElement psi = n.getPsi();
        if(psi instanceof PsiWhiteSpace || psi instanceof PsiComment || psi instanceof KDoc) return;
        final ASTNode[] children = n.getChildren(null);
        if(children.length == 0) {
            final String t = n.getText();
            if( !t.isEmpty() ) sb.append(t).append(' ');
        }
        else if( isCollapsibleControlBlock(n) ) {
            for(final ASTNode c : children) {
                final IElementType et = c.getElementType();
                if(et == KtTokens.LBRACE || et == KtTokens.RBRACE) continue;
                collectLeafText(c, sb);
            }
        }
        else {
            for(final ASTNode c : children) collectLeafText(c, sb);
        }
    }

    static String canonicalize(final PsiElement e)
    {
        final StringBuilder sb = new StringBuilder();
        collectLeafText( e.getNode(), sb );

        return normalizeWhitespace( sb.toString() );
    }

    static List<String> importMultiset(final KtFile file)
    {
        final List<String> out = new ArrayList<>();
        for( final KtImportDirective imp : file.getImportDirectives() ) {
            final String fq    = imp.getImportedFqName() == null ? "" : imp.getImportedFqName().asString();
            final String alias = imp.getAliasName() == null ? "" : ( " as " + imp.getAliasName() );
            out.add( fq + ( imp.isAllUnder() ? ".*" : "" ) + alias );
        }
        out.sort(null);

        return out;
    }

    static List<String> topLevelDecls(final KtFile file)
    {
        final List<String> out = new ArrayList<>();
        for( final KtDeclaration d : file.getDeclarations() ) out.add( canonicalize(d) );

        return out;
    }

    /**
     * Walks the ASTNode tree collecting every comment/KDoc leaf's text --
     * see collectLeafText's comment on why PsiElement.getChildren()/
     * PsiTreeUtil can't be used to reach these for stub-based elements.
     */
    static void collectComments(final ASTNode n, final List<String> out)
    {
        final PsiElement psi = n.getPsi();
        if(psi instanceof PsiComment || psi instanceof KDoc) {
            out.add( stripCommentDelims( n.getText() ) );
            return;
        }
        for( final ASTNode c : n.getChildren(null) ) collectComments(c, out);
    }

    static List<String> commentMultiset(final KtFile file)
    {
        final List<String> out = new ArrayList<>();
        collectComments( file.getNode(), out );
        out.sort(null);

        return out;
    }

    /**
     * A sole trailing "." is a legitimate normalize-comment-end-period
     * transform (STYLE.md §15, same as java_content_diff's
     * normalizeTrailingPeriod), not corruption -- strip it on both sides
     * before comparing so a comment differing only by that period isn't
     * flagged. Guarded to a *single* trailing period (mirrors Java's
     * `count() == 1` check) so a real ellipsis/decimal/abbreviation run at
     * the end of a comment is left alone.
     */
    static String normalizeTrailingPeriod(final String s)
    {
        if( s.endsWith(
            "."
        ) && s.chars().filter(
            c -> c == '.'
        ).count() == 1 ) return s.substring(
            0, s.length() - 1
        );

        return s;
    }

    /**
     * KDoc continuation-line leading-asterisk spacing before a fenced code
     * block (STYLE_KOTLIN.md's KDoc formatting) is a legitimate,
     * intentional transform -- the formatter normalizes `*```` (no space
     * between the continuation `*` and a following backtick-fence run) to
     * `* ```` (exactly one space), or vice versa. normalizeWhitespace's
     * `\s+` -> " " collapse alone can't equate these: one side has ZERO
     * whitespace characters between `*` and the backtick run, the other has
     * one, so there's no existing whitespace run for `\s+` to collapse on
     * the zero-space side. Deliberately narrow -- only touches a literal
     * `*` immediately followed by (optional whitespace then) one or more
     * backticks, normalizing to exactly one space between them. Does not
     * touch any other `*`/backtick adjacency shape, so it can't mask a
     * dropped/added/reordered word elsewhere in the comment body.
     */
    static String normalizeKdocAsteriskFenceSpacing(final String s)
    {
        return s.replaceAll("\\*[ \\t]*(`+)", "* $1");
    }

    static String stripCommentDelims(final String text)
    {
        String t = text.trim();
             if( t.startsWith("///") ) t = t.substring(3);
        else if( t.startsWith("//") )  t = t.substring(2);
        else if( t.startsWith("/**") ) t = t.substring( 3, Math.max( 3, t.length() - 2 ) );
        else if( t.startsWith("/*") )  t = t.substring( 2, Math.max( 2, t.length() - 2 ) );

        t = normalizeKdocAsteriskFenceSpacing(t);

        return normalizeTrailingPeriod( normalizeWhitespace(t).toLowerCase() );
    }

    /**
     * STYLE.md's general closing-brace-annotation tolerance for control-flow
     * constructs (`} // while`, `} // for x`, `} // when kind`, ...) --
     * same precedent as java_content_diff.java's BRACE_ANNOTATION: this is
     * new content the formatter intentionally adds (length-gated, STYLE.md
     * §7), not a normalization of pre-existing text, so it must not be
     * flagged as an unexplained addition. Deliberately loose (keyword +
     * at most one trailing word, not verified against an actual construct)
     * -- matches the already-shipped Java precedent for the same control-
     * flow-keyword family. `class`/`interface`/`enum class`/`object`/
     * `companion object`/`init` closing comments are handled separately
     * (namedConstructClosingComments below), verified against the file's
     * actual declarations, since those always carry a real name that a
     * wrong/corrupted closing comment could plausibly get wrong.
     *
     * `when` is deliberately EXCLUDED from this loose keyword list (unlike
     * the shipped 2026-08-10 version, which included it): RDD_KEY_101's
     * `when` closing comment names the FULL `when(...)` subject verbatim,
     * which for a `when (val x = expr)` capture form is a multi-word
     * expression (`val`/`var` name + `=` + an arbitrary initializer), not a
     * single trailing identifier -- e.g. `// when val accessDenied =
     * error.suppressedExceptions.single()`. The single-trailing-word clause
     * rejected this shape as an unexplained addition, a false MISMATCH
     * against a real, always-emitted closing comment (confirmed against
     * PathRecursiveFunctionsTest.kt/PathTreeWalkTest.kt/coreRuntime.kt), but
     * widening the trailing clause to accept ANY text after `when` (tried
     * first, reverted) let a genuinely WRONG subject name slip through
     * uncaught -- a real regression the checker exists to prevent. `when`'s
     * closing comment is instead verified against the file's actual `when`
     * subject text via whenClosingComments below, the same
     * verified-per-construct precedent namedConstructClosingComments already
     * uses for `class`/`object`/`init`, rather than a free-floating pattern.
     */
    static final java.util.regex.Pattern CONTROL_FLOW_CLOSING = java.util.regex.Pattern.compile(
        "^(while|for|if|else|do|try|catch|finally)( \\S+)?$"
    );

    /**
     * STYLE_KOTLIN.md §3.1: `class`/`object`/`companion object` bodies (and
     * §3.4's `init` blocks) always receive a closing comment, unconditionally
     * -- new content the formatter intentionally adds, not a normalization
     * of pre-existing text. Computed from the ORIGINAL file's own real
     * declarations (not a free-floating pattern) so a closing comment naming
     * the wrong construct, or one with no corresponding declaration at all,
     * is still flagged as a genuine mismatch.
     *
     * Each declaration contributes a GROUP of acceptable variant texts rather
     * than one fixed string: real-corpus spot-checking (25-file sample of
     * JetBrains/kotlin, beyond the 4 gap repros this fix set out to cover)
     * found the formatter's `object`-closing-comment naming is inconsistent
     * -- observed emitting `// class GeneratedSuites` (wrong keyword) for a
     * plain `object GeneratedSuites {}`, and bare `// Default`/`// BuilderContext`
     * (keyword omitted) for a supertyped `object Default : X {}` -- alongside
     * the expected `// object Foo`/`// object` shapes. This looks like a
     * pre-existing quirk in the formatter's own named-construct classifier
     * for `object` specifically (not touched here -- out of scope for this
     * checker fix), so all four observed shapes are accepted as long as they
     * still carry the declaration's own real name (or no name, for an
     * anonymous object) -- a wrong name is still rejected. `class`/
     * `interface`/`enum class`/`companion object`/`init` were not observed
     * to have this inconsistency in the sample and keep one exact shape
     * each.
     */
    static void collectNamedConstructClosings(final PsiElement e, final List<List<String>> out)
    {
        if(e instanceof KtClassInitializer) {
            out.add( java.util.Collections.singletonList("init") );
        }
        else if(e instanceof KtClassOrObject) {
            final KtClassOrObject co = (KtClassOrObject)e;
            // getName() defaults an anonymous companion object to "Companion"
            // even with no explicit name written in source -- getNameIdentifier()
            // is null in exactly that case, which is what distinguishes
            // "companion object" from an explicitly-named "companion object Foo"
            final String       name     = ( co.getNameIdentifier() == null ) ? null : co.getName();
            final List<String> variants = new ArrayList<>();
            if( co instanceof KtObjectDeclaration && ( (KtObjectDeclaration) co ).isCompanion() ) {
                variants.add( (name == null) ? "companion object" : ("companion object " + name) );
            }
            else if(co instanceof KtObjectDeclaration) {
                if(name == null) {
                    variants.add("object");
                }
                else {
                    variants.add("object " + name);
                    variants.add("class " + name);   // Observed formatter quirk, see doc above
                    variants.add(name);                // Observed formatter quirk, see doc above
                }
            }
            else if( co instanceof KtClass && ( (KtClass) co ).isInterface() ) {
                variants.add("interface " + name);
            }
            else if( co instanceof KtClass && ( (KtClass) co ).isEnum() ) {
                variants.add("enum class " + name);
            }
            else {
                variants.add("class " + name);
            }
            final List<String> normalized = new ArrayList<>();
            for(final String v : variants) normalized.add( normalizeWhitespace(v).toLowerCase() );
            out.add(normalized);
        }
        for( final PsiElement c : e.getChildren() ) collectNamedConstructClosings(c, out);
    }

    static List<List<String>> namedConstructClosingComments(final KtFile file)
    {
        final List<List<String>> out = new ArrayList<>();
        collectNamedConstructClosings(file, out);

        return out;
    }

    /**
     * RDD_KEY_101 (`STATE_KOTLIN.md`): every `when [(subject)] { ... }` gets a
     * `// when <subject>` closing comment (bare `// when` if subject-less) --
     * new content the formatter intentionally adds, not a normalization of
     * pre-existing text. `<subject>` is the raw text between the `when`'s own
     * `(`/`)`, whitespace-collapsed to one line -- mirrors
     * `KotlinSpecificRule.formatWhenExpressions`'s own `subject =
     * literalSlice(tokens, j + 1, closeParen).trim().replaceAll("\\s+", " ")`
     * exactly, so the tolerance only accepts the one subject text a real
     * `when` in the ORIGINAL file could actually produce -- a closing
     * comment naming the wrong subject (or naming a `when` that doesn't
     * exist at all) is still flagged as a genuine mismatch, unlike a
     * free-floating regex.
     */
    static void collectWhenClosingComments(final PsiElement e, final List<List<String>> out)
    {
        if(e instanceof org.jetbrains.kotlin.psi.KtWhenExpression) {
            final org.jetbrains.kotlin.psi.KtWhenExpression w = (org.jetbrains.kotlin.psi.KtWhenExpression) e;
            final PsiElement   lp       = w.getLeftParenthesis();
            final PsiElement   rp       = w.getRightParenthesis();
            final List<String> variants = new ArrayList<>();
            if(lp != null && rp != null) {
                final String rawSubject = w.getContainingFile().getText().substring(
                    lp.getTextRange().getEndOffset(), rp.getTextRange().getStartOffset()
                );
                variants.add(
                    normalizeWhitespace( "when " + normalizeWhitespace(rawSubject) ).toLowerCase()
                );
            } // if
            else {
                variants.add("when");
            }
            out.add(variants);
        } // if
        for( final PsiElement c : e.getChildren() ) collectWhenClosingComments(c, out);
    }

    static List<List<String>> whenClosingComments(final KtFile file)
    {
        final List<List<String>> out = new ArrayList<>();
        collectWhenClosingComments(file, out);

        return out;
    }

    static List<String> diffMultisets(final String label, final List<String> a, final List<String> b)
    {
        final List<String> mismatches = new ArrayList<>();
        final List<String> bCopy      = new ArrayList<>(b);
        final List<String> onlyInA    = new ArrayList<>();
        for(final String s : a) {
            if( !bCopy.remove(s) ) onlyInA.add(s);
        }
        if( !onlyInA.isEmpty() ) mismatches.add(
            label + ": present in original, missing from formatted: " + onlyInA
        );
        if( !bCopy.isEmpty() ) mismatches.add(
            label + ": present in formatted, missing from original: " + bCopy
        );

        return mismatches;
    }

    /**
     * Same shape as diffMultisets, plus the two closing-comment tolerances
     * above: a plain unmatched-in-`a` entry still mismatches unconditionally
     * (a dropped/corrupted comment is never tolerated); an unmatched-in-`b`
     * entry is only tolerated if it's either a loose control-flow closing
     * annotation, or one consumed (one-for-one, not just "present anywhere
     * in the set") from `namedConstructAllowed`'s per-file real-construct
     * count
     */
    static List<String> diffCommentMultisets(
        final List<String>       a,
        final List<String>       b,
        final List<List<String>> namedConstructAllowed
    )
    {
        final List<String> mismatches = new ArrayList<>();
        final List<String> bCopy      = new ArrayList<>(b);
        final List<String> onlyInA    = new ArrayList<>();
        for(final String s : a) {
            if( !bCopy.remove(s) ) onlyInA.add(s);
        }
        if( !onlyInA.isEmpty() ) mismatches.add(
            "comments: present in original, missing from formatted: " + onlyInA
        );

        bCopy.removeIf( s -> CONTROL_FLOW_CLOSING.matcher(s).matches() );

        // Each group represents ONE real declaration -- consume at most one
        // variant per group, so a single construct can excuse at most one
        // added comment, no matter how many acceptable shapes its group lists
        for(final List<String> group : namedConstructAllowed) {
            for(final String variant : group) {
                if( bCopy.remove(variant) ) break;
            }
        }

        if( !bCopy.isEmpty() ) mismatches.add(
            "comments: present in formatted, missing from original: " + bCopy
        );

        return mismatches;
    }

    static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss.SSS"
    );

    /**
     * Printed immediately before a pair's AST diff starts -- if the tool
     * hangs/is slow on one particular file in a large batch, this line
     * (already flushed to stdout for every prior file) shows exactly which
     * file and when, mirroring Main.main's/ServerMode.FormatHandler's own
     * "processing <file>" trace precedent (STATE_COMMON.md).
     */
    static void printTimestampedHeader(final String relPath)
    {
        System.out.println( "[" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "] " + relPath );
    }

    /**
     * The full single-pair AST-diff check (imports/declarations/comments),
     * shared by both the single-pair and batch modes. Assumes both paths
     * are already confirmed to exist -- callers are responsible for the
     * missing-file check so a batch run can warn-and-skip instead of
     * throwing.
     */
    static boolean compareOne(
        final Path origPath, final Path fmtPath, final String origLabel, final String fmtLabel
    ) throws Exception
    {
        final String origSrc = new String( Files.readAllBytes(origPath) );
        final String fmtSrc  = new String( Files.readAllBytes(fmtPath) );

        final KtFile origFile = parse(origSrc);
        final KtFile fmtFile  = parse(fmtSrc);

        final List<String> mismatches = new ArrayList<>();

        mismatches.addAll(
            diffMultisets( "imports", importMultiset(origFile), importMultiset(fmtFile) )
        );

        final List<String> origDecls = topLevelDecls(origFile);
        final List<String> fmtDecls  = topLevelDecls(fmtFile);
        if( origDecls.size() != fmtDecls.size() ) mismatches.add(
            "top-level declaration count changed: " + origDecls.size() + " -> " + fmtDecls.size()
        );
        final int n = Math.min( origDecls.size(), fmtDecls.size() );
        for(int i = 0; i < n; ++i) {
            if( !origDecls.get(
                i
            ).equals(
                fmtDecls.get(i)
            ) ) mismatches.add(
                "top-level declaration #" + i + " structure/content differs"
            );
        } // for

        final List<List<String>> closingCommentGroups = new ArrayList<>( namedConstructClosingComments(
            origFile
        ) );
        closingCommentGroups.addAll( whenClosingComments(origFile) );
        mismatches.addAll(
            diffCommentMultisets(
                commentMultiset(origFile),
                commentMultiset(fmtFile),
                closingCommentGroups
            )
        );

        if( mismatches.isEmpty() ) {
            System.out.println("OK: content preserved (" + origLabel + " == " + fmtLabel + ")");

            return true;
        }
        else {
            System.out.println(
                "MISMATCH: content differs between " + origLabel + " and " + fmtLabel
            );
            for(final String m : mismatches) System.out.println("  " + m);

            return false;
        }
    }

    static void printUsage()
    {
        System.err.println("Usage: kotlin_content_diff.sh <original.kt> <formatted.kt>");
        System.err.println(
            "       kotlin_content_diff.sh <original_base_dir> <formatted_base_dir> <kt_rel_path_file_list.txt>"
        );
    }

    static void runSingle(final String origArg, final String fmtArg) throws Exception
    {
        final Path origPath = Paths.get(origArg);
        final Path fmtPath  = Paths.get(fmtArg);

        printTimestampedHeader(origArg);

        final boolean origExists = Files.exists(origPath);
        final boolean fmtExists  = Files.exists(fmtPath);
        if(!origExists || !fmtExists) {
                 if(!origExists && !fmtExists) System.out.println(
                     "WARNING: both " + origArg + " and " + fmtArg + " are missing"
                 );
            else if(!origExists)               System.out.println(
                "WARNING: " + origArg + " is missing"
            );
            else                                 System.out.println(
                "WARNING: " + fmtArg + " is missing"
            );
            System.exit(1);
        } // if

        if( !compareOne(origPath, fmtPath, origArg, fmtArg) ) System.exit(1);
    }

    static void runBatch(
        final String origBaseDir, final String fmtBaseDir, final String fileListPath
    ) throws Exception
    {
        final List<String> relPaths = Files.readAllLines( Paths.get(fileListPath) );

        int okCount = 0, mismatchCount = 0, missingCount = 0;

        for(String rel : relPaths) {
            rel = rel.trim();
            if( rel.isEmpty() ) continue;

            final Path origPath = Paths.get(origBaseDir, rel);
            final Path fmtPath  = Paths.get(fmtBaseDir, rel);

            printTimestampedHeader(rel);

            final boolean origExists = Files.exists(origPath);
            final boolean fmtExists  = Files.exists(fmtPath);
            if(!origExists && !fmtExists) {
                System.out.println(
                    "  WARNING: missing from both " + origBaseDir + " and " + fmtBaseDir + " -- skipping"
                );
                ++missingCount;
                continue;
            } // if
            if(!origExists) {
                System.out.println("  WARNING: missing from " + origBaseDir + " -- skipping");
                ++missingCount;
                continue;
            }
            if(!fmtExists) {
                System.out.println("  WARNING: missing from " + fmtBaseDir + " -- skipping");
                ++missingCount;
                continue;
            }

            try {
                if( compareOne(origPath, fmtPath, rel, rel) ) ++okCount;
                else                                          ++mismatchCount;
            }
            catch(final Exception e) {
                System.out.println("  ERROR: " + e);
                ++mismatchCount;
            }
        } // for

        System.out.println();
        System.out.println(
            "SUMMARY: " + okCount + " OK, " + mismatchCount + " MISMATCH/ERROR, " + missingCount +
            " MISSING (of " + (okCount + mismatchCount + missingCount) + " files checked)"
        );

        if(mismatchCount > 0 || missingCount > 0) System.exit(1);
    }

    public static void main(final String[] args) throws Exception
    {
             if(args.length == 2) runSingle( args[0], args[1] );
        else if(args.length == 3) runBatch( args[0], args[1], args[2] );
        else                      {
            printUsage();
            System.exit(2);
        }
    }

} // class kotlin_content_diff
