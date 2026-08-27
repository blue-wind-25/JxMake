/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;

/**
 * Content-preservation checker for Java, modeled on css_content_diff.py /
 * xml_content_diff.py / python_content_diff.py, reusing java_syntax_check's javac-AST
 * parsing infrastructure (JavacTask.parse(), no semantic/type checking).
 *
 * Unlike a naive text/token diff, this formatter *intentionally* reorders
 * import statements (java-import-order) and recapitalizes comment text
 * (normalize-comment-start-case), so:
 *     - imports are compared as a MULTISET (sorted, order-independent)
 *     - every other top-level type declaration is compared IN ORDER, via
 *       javac's own pretty-printer (Tree.toString()), whitespace-normalized --
 *       this canonicalizes away pure reindentation/alignment-padding
 *       differences while still catching a dropped/added declaration, a
 *       renamed identifier, or a changed literal value, since the pretty
 *       printer encodes structure/identifiers/literals but not original
 *       whitespace.
 *     - comments (dropped by the pretty printer, extracted separately via a
 *       raw-text scan that skips string/char literals) are compared as a
 *       MULTISET, whitespace-normalized AND case-normalized (lowercased) --
 *       a case-only change is expected (normalize-comment-start-case), so it
 *       must not be flagged, but a dropped/corrupted comment still is. Each
 *       line's own leading "* " continuation marker is stripped before
 *       collapsing whitespace, so reflowing a single-line-opening Javadoc to
 *       one-sentence-per-line doesn't read as changed text. A sole trailing
 *       "." is also stripped on both sides before comparing (normalize-
 *       comment-end-period is equally expected, intentional behavior).
 *       Closing-brace
 *       annotations ({@code } // while}, {@code } // class Foo}, ...) are
 *       genuinely new content the formatter intentionally adds, not a
 *       normalization of pre-existing text, so they're exempted from the
 *       "present in formatted, missing from original" check.
 *
 * Build:
 *     JDK=/opt/openjdk-21_linux-x64_bin/jdk-21
 *     $JDK/bin/javac java_content_diff.java
 *
 * Run (two modes -- see also Usage: in main below):
 *     Single pair:
 *         $JDK/bin/java java_content_diff <original.java> <formatted.java>
 *     Batch (one JVM invocation over a whole corpus, avoiding a JRE restart
 *     per file -- rel-path list, one path per line, relative to both base
 *     dirs alike):
 *         $JDK/bin/java java_content_diff <original_base_dir> <formatted_base_dir> <java_rel_path_file_list.txt>
 *
 * Before each pair's AST diff, a "[yyyy-MM-dd HH:mm:ss.SSS] <relative path>"
 * line is printed -- lets a hang/slow file in a large batch run be pinpointed
 * (same "print immediately before the risky step" precedent as
 * Main.main's/ServerMode.FormatHandler's own "processing <file>" stderr
 * trace, see STATE_COMMON.md). In batch mode, a rel-path missing from either
 * base dir (or both) is a warning, not a crash -- the file is skipped and
 * the run continues; the final SUMMARY line and process exit code still
 * reflect it. A parse failure for one pair in batch mode is also caught and
 * counted as a MISMATCH/ERROR rather than aborting the whole batch.
 *
 * Exit 0 if content is preserved (all pairs, in batch mode), 1 if any
 * mismatch/missing file/error is found (description printed for each), 2 on
 * a usage error (and, in single-pair mode only, if either file fails to
 * parse -- see compareOne's javadoc for why batch mode handles a parse
 * failure differently).
 */
public class java_content_diff {

    static class SourceFile extends SimpleJavaFileObject {

        private final String source;

        SourceFile(final String className, final String source)
        {
            super( URI.create("string:///" + className + Kind.SOURCE.extension), Kind.SOURCE );
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors)
        {
            return source;
        }

    } // class SourceFile

    static CompilationUnitTree parse(final String source) throws IOException
    {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if(compiler == null) throw new IllegalStateException(
            "No system compiler found (need a JDK, not a JRE)"
        );
        final JavaFileObject                          file  = new SourceFile("Test", source);
        final JavacTask                               task  = (JavacTask)compiler.getTask(
            null, null, null, Arrays.asList("-proc:none"), null, Arrays.asList(file)
        );
        final Iterable<? extends CompilationUnitTree> trees = task.parse();

        return trees.iterator().next();
    }

    static String normalizeWhitespace(final String s)
    {
        return s.trim().replaceAll("\\s+", " ");
    }

    /**
     * Strips a single trailing "." -- like the lowercasing this class already
     * does for {@code normalize-comment-start-case}, a sole-trailing-period
     * removal is expected, intentional behavior ({@code
     * normalize-comment-end-period}, per STYLE.md #15: single-sentence
     * comments must not end with a period) and must not be flagged as
     * dropped content. An ellipsis ("...") is untouched since its dot count
     * isn't 1, matching {@code MiscRuleCore.stripSoleTrailingPeriod}.
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
     * Strips a leading Javadoc/block-comment continuation marker ("<ws>* ")
     * from every line before whitespace-collapsing, so reflowing a comment
     * from a single opening line ({@code /** Foo...}) to one-sentence-per-
     * line ({@code /**\n * Foo...}) does not read as changed content --
     * without this, the collapsed text picks up a spurious doubled "*"
     * (the line's own marker plus the following line's) that isn't part of
     * the actual comment text.
     */
    static String normalizeCommentBody(final String s)
    {
        final String[]      lines = s.split("\n", -1);
        final StringBuilder out   = new StringBuilder();
        for(final String line : lines) out.append(
            line.replaceFirst("^\\s*\\*\\s?", "")
        ).append(
            ' '
        );

        return normalizeWhitespace( out.toString() );
    }

    /**
     * A closing-brace annotation trailer ({@code } // while}, {@code } //
     * class Foo}) is new content the formatter is intentionally adding, not
     * a normalization of pre-existing comment text, so it must not be
     * flagged as an unexplained addition
     */
    static final java.util.regex.Pattern BRACE_ANNOTATION = java.util.regex.Pattern.compile(
        "^(while|for|if|else|do|switch|try|catch|finally)( \\S+)?$"
        + "|^(class|interface|enum) \\S+$");

    /**
     * Raw-text comment scan: skips string/char literals so a "//" or "/*"
     * inside a literal is never mistaken for a comment start
     */
    static List<String> extractComments(final String src)
    {
        final List<String> out = new ArrayList<>();
        final int          n   = src.length();
              int          i   = 0;
        while(i < n) {
            final char c = src.charAt(i);
            if(c == '"') {
                ++i;
                while( i < n && src.charAt(i) != '"' ) {
                    if( src.charAt(i) == '\\' ) i++;
                    ++i;
                }
                ++i;
            } // if
            else if(c == '\'') {
                ++i;
                while( i < n && src.charAt(i) != '\'' ) {
                    if( src.charAt(i) == '\\' ) i++;
                    ++i;
                }
                ++i;
            }
            else if( c == '/' && i + 1 < n && src.charAt(i + 1) == '/' ) {
                final int start = i + 2;
                      int end   = src.indexOf('\n', start);
                if(end < 0) end = n;
                out.add(
                    normalizeTrailingPeriod( normalizeCommentBody( src.substring(start, end) ).toLowerCase() )
                );
                i = end;
            }
            else if( c == '/' && i + 1 < n && src.charAt(i + 1) == '*' ) {
                final int start = i + 2;
                      int end   = src.indexOf("*/", start);
                if(end < 0) end = n;
                out.add(
                    normalizeTrailingPeriod( normalizeCommentBody( src.substring(start, end) ).toLowerCase() )
                );
                i = (end == n) ? n : end + 2;
            }
            else {
                ++i;
            }
        } // while

        return out;
    }

    static List<String> importMultiset(final CompilationUnitTree cu)
    {
        final List<String> out = new ArrayList<>();
        for( final ImportTree imp : cu.getImports() ) out.add(
            ( imp.isStatic() ? "static " : "" ) + imp.getQualifiedIdentifier().toString()
        );
        out.sort(null);

        return out;
    }

    static List<String> topLevelDecls(final CompilationUnitTree cu)
    {
        final List<String> out = new ArrayList<>();
        for( final Tree t : cu.getTypeDecls() ) out.add( normalizeWhitespace( t.toString() ) );

        return out;
    }

    static List<String> diffMultisets(
        final String       label,
        final List<String> a,
        final List<String> b
    )
    {
        return diffMultisets(label, a, b, s -> false);
    }

    /**
     * {@code ignorableAddition} lets a caller exempt entries that are only
     * in {@code b} but are known, intentional new content (e.g. closing-
     * brace annotations) rather than a genuine unexplained addition
     */
    static List<String> diffMultisets(
        final String                               label,
        final List<String>                         a,
        final List<String>                         b,
        final java.util.function.Predicate<String> ignorableAddition
    )
    {
        final List<String> mismatches = new ArrayList<>();
        final List<String> bCopy      = new ArrayList<>(b);
        final List<String> onlyInA    = new ArrayList<>();
        for(final String s : a) {
            if( !bCopy.remove(s) ) onlyInA.add(s);
        }
        bCopy.removeIf(ignorableAddition);
        if( !onlyInA.isEmpty() ) mismatches.add(
            label + ": present in original, missing from formatted: " + onlyInA
        );
        if( !bCopy.isEmpty() ) mismatches.add(
            label + ": present in formatted, missing from original: " + bCopy
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
     * The full single-pair AST-diff check (package/imports/declarations/
     * comments), shared by both the single-pair and batch modes. Assumes
     * both paths are already confirmed to exist -- callers are responsible
     * for the missing-file check so a batch run can warn-and-skip instead
     * of throwing.
     *
     * A javac parse failure is let propagate rather than caught here (unlike
     * kotlin_content_diff.compareOne, which never throws on a parse
     * failure -- Kotlin's PSI parser tolerates malformed input and returns
     * a best-effort tree instead of throwing): {@code JavacTask.parse()}
     * can throw for a genuinely malformed file, and single-pair mode's
     * caller (main, via runSingle) treats that as a hard usage-adjacent
     * error (exit 2, matching this tool's pre-existing behavior). In batch
     * mode, runBatch wraps its call to this method in a try/catch so one
     * file's parse failure is counted as a MISMATCH/ERROR and does not
     * abort the rest of the batch -- the same generic-catch shape
     * kotlin_content_diff.runBatch already uses, even though the two
     * tools' parsers fail in different ways.
     */
    static boolean compareOne(
        final Path   origPath,
        final Path   fmtPath,
        final String origLabel,
        final String fmtLabel
    ) throws Exception
    {
        final String origSrc = Files.readString(origPath);
        final String fmtSrc  = Files.readString(fmtPath);

        final CompilationUnitTree origCu = parse(origSrc);
        final CompilationUnitTree fmtCu  = parse(fmtSrc);

        final List<String> mismatches = new ArrayList<>();

        // Package name must be unchanged
        final String origPkg = origCu.getPackageName() == null ? "" : origCu.getPackageName().toString();
        final String fmtPkg  = fmtCu.getPackageName() == null ? "" : fmtCu.getPackageName().toString();
        if( !origPkg.equals(
            fmtPkg
        ) ) mismatches.add(
            "package declaration changed: '" + origPkg + "' -> '" + fmtPkg + "'"
        );

        // Imports: multiset (legitimately reordered/sorted by java-import-order)
        mismatches.addAll(
            diffMultisets( "imports", importMultiset(origCu), importMultiset(fmtCu) )
        );

        // Top-level type declarations: in original relative order, canonicalized
        // via javac's pretty-printer (drops whitespace/comments, keeps structure/
        // identifiers/literals)
        final List<String> origDecls = topLevelDecls(origCu);
        final List<String> fmtDecls  = topLevelDecls(fmtCu);
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

        // Comments: multiset, whitespace- and case-normalized (case-only changes
        // are expected behavior per normalize-comment-start-case)
        mismatches.addAll( diffMultisets( "comments",
            extractComments(origSrc), extractComments(fmtSrc),
            s -> BRACE_ANNOTATION.matcher(s).matches() ) );

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
        System.err.println("Usage: java_content_diff.sh <original.java> <formatted.java>");
        System.err.println(
            "       java_content_diff.sh <original_base_dir> <formatted_base_dir> <java_rel_path_file_list.txt>"
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

        final boolean ok;
        try {
            ok = compareOne(origPath, fmtPath, origArg, fmtArg);
        }
        catch(final Exception e) {
            System.err.println("ERROR: failed to parse " + origArg + " or " + fmtArg + ": " + e);
            System.exit(2);
            return;
        }
        if(!ok) System.exit(1);
    }

    static void runBatch(
        final String origBaseDir,
        final String fmtBaseDir,
        final String fileListPath
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

} // class java_content_diff
