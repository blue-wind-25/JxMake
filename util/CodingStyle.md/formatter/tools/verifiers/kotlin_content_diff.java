/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

import java.util.ArrayList;
import java.util.List;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtImportDirective;
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
 * Run:
 *     $JDK/bin/java -cp ".:$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_content_diff <original.kt> <formatted.kt>
 *
 * Exit 0 if content is preserved, 1 with a description of each mismatch
 * otherwise.
 */
public class kotlin_content_diff {

    static KtFile parse(String source)
    {
        KotlinCoreEnvironment env = KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable(),
            new CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        );

        return new KtPsiFactory( env.getProject() ).createFile(source);
    }

    static String normalizeWhitespace(String s)
    {
        return s.trim().replaceAll("\\s+", " ");
    }

    /**
     * Canonicalize a subtree: leaf tokens only, whitespace/comment/KDoc
     *  nodes skipped, joined with single spaces. Walks the ASTNode (not
     *  PsiElement.getChildren(), which for stub-based elements like
     *  KtClass/KtProperty/KtNamedFunction only returns structurally
     *  significant composite children -- plain leaf tokens (identifiers,
     *  keywords, comments) never show up there at all).
     */
    static void collectLeafText(ASTNode n, StringBuilder sb)
    {
        PsiElement psi = n.getPsi();
        if(psi instanceof PsiWhiteSpace || psi instanceof PsiComment || psi instanceof KDoc) return;
        ASTNode[] children = n.getChildren(null);
        if(children.length == 0) {
            String t = n.getText();
            if( !t.isEmpty() ) sb.append(t).append(' ');
        }
        else {
            for(ASTNode c : children) collectLeafText(c, sb);
        }
    }

    static String canonicalize(PsiElement e)
    {
        StringBuilder sb = new StringBuilder();
        collectLeafText( e.getNode(), sb );

        return normalizeWhitespace( sb.toString() );
    }

    static List<String> importMultiset(KtFile file)
    {
        List<String> out = new ArrayList<>();
        for( KtImportDirective imp : file.getImportDirectives() ) {
            String fq = imp.getImportedFqName() == null ? "" : imp.getImportedFqName().asString();
            String alias = imp.getAliasName() == null ? "" : ( " as " + imp.getAliasName() );
            out.add( fq + ( imp.isAllUnder() ? ".*" : "" ) + alias );
        }
        out.sort(null);

        return out;
    }

    static List<String> topLevelDecls(KtFile file)
    {
        List<String> out = new ArrayList<>();
        for( KtDeclaration d : file.getDeclarations() ) out.add( canonicalize(d) );

        return out;
    }

    /**
     * Walks the ASTNode tree collecting every comment/KDoc leaf's text --
     *  see collectLeafText's comment on why PsiElement.getChildren()/
     *  PsiTreeUtil can't be used to reach these for stub-based elements.
     */
    static void collectComments(ASTNode n, List<String> out)
    {
        PsiElement psi = n.getPsi();
        if(psi instanceof PsiComment || psi instanceof KDoc) {
            out.add( stripCommentDelims( n.getText() ) );
            return;
        }
        for( ASTNode c : n.getChildren(null) ) collectComments(c, out);
    }

    static List<String> commentMultiset(KtFile file)
    {
        List<String> out = new ArrayList<>();
        collectComments( file.getNode(), out );
        out.sort(null);

        return out;
    }

    static String stripCommentDelims(String text)
    {
        String t = text.trim();
        if( t.startsWith("///") )      t = t.substring(3);
        else if( t.startsWith("//") )  t = t.substring(2);
        else if( t.startsWith("/**") ) t = t.substring( 3, Math.max( 3, t.length() - 2 ) );
        else if( t.startsWith("/*") )  t = t.substring( 2, Math.max( 2, t.length() - 2 ) );

        return normalizeWhitespace(t).toLowerCase();
    }

    static List<String> diffMultisets(String label, List<String> a, List<String> b)
    {
        List<String> mismatches = new ArrayList<>();
        List<String> bCopy      = new ArrayList<>(b);
        List<String> onlyInA    = new ArrayList<>();
        for(String s : a) {
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

    public static void main(String[] args) throws Exception
    {
        if(args.length != 2) {
            System.err.println("Usage: kotlin_content_diff <original.kt> <formatted.kt>");
            System.exit(2);
        }

        String origSrc = new String(
            java.nio.file.Files.readAllBytes( java.nio.file.Paths.get( args[0] ) )
        );
        String fmtSrc  = new String(
            java.nio.file.Files.readAllBytes( java.nio.file.Paths.get( args[1] ) )
        );

        KtFile origFile = parse(origSrc);
        KtFile fmtFile  = parse(fmtSrc);

        List<String> mismatches = new ArrayList<>();

        mismatches.addAll(
            diffMultisets( "imports", importMultiset(origFile), importMultiset(fmtFile) )
        );

        List<String> origDecls = topLevelDecls(origFile);
        List<String> fmtDecls  = topLevelDecls(fmtFile);
        if( origDecls.size() != fmtDecls.size() ) mismatches.add(
            "top-level declaration count changed: " + origDecls.size() + " -> " + fmtDecls.size()
        );
        int n = Math.min( origDecls.size(), fmtDecls.size() );
        for(int i = 0; i < n; ++i) {
            if( !origDecls.get(
                i
            ).equals(
                fmtDecls.get(i)
            ) ) mismatches.add(
                "top-level declaration #" + i + " structure/content differs"
            );
        } // for

        mismatches.addAll(
            diffMultisets( "comments", commentMultiset(origFile), commentMultiset(fmtFile) )
        );

        if( mismatches.isEmpty() ) {
            System.out.println( "OK: content preserved (" + args[0] + " == " + args[1] + ")" );
        }
        else {
            System.out.println(
                "MISMATCH: content differs between " + args[0] + " and " + args[1]
            );
            for(String m : mismatches) System.out.println("  " + m);
            System.exit(1);
        }
    }

} // class kotlin_content_diff
