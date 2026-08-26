/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

import java.util.Collection;

import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;

/**
 * Lightweight PSI-based Kotlin syntax-only checker.
 *
 * Parses a .kt source file to an AST via KotlinCoreEnvironment/KtPsiFactory
 * and reports any PsiErrorElement nodes (parse errors). This does NOT do
 * semantic/type checking and does NOT need android.* / AndroidX to resolve
 * (unlike a full `kotlinc`/Gradle compile) since it only builds the AST.
 *
 * Classpath needed (all bundled in the single shaded kotlin-compiler.jar):
 *     ~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib/kotlin-compiler.jar
 *     ~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib/kotlin-stdlib.jar
 *
 * Build:
 *     JDK=/opt/openjdk-21_linux-x64_bin/jdk-21
 *     KLIB=~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib
 *     $JDK/bin/javac -cp "$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_syntax_check.java
 *
 * Run:
 *     $JDK/bin/java -cp ".:$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_syntax_check <file.kt> [file2.kt ...]
 */
public class kotlin_syntax_check {

    public static boolean hasSyntaxError(String source)
    {
        KotlinCoreEnvironment env = KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable(),
            new CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        );

        KtFile file = new KtPsiFactory( env.getProject() ).createFile(source);

        Collection<PsiErrorElement> errors = PsiTreeUtil.findChildrenOfType(
            file, PsiErrorElement.class
        );

        boolean anyErrors = false;
        for(PsiErrorElement e : errors) {
            anyErrors = true;
            System.out.println(
                e.getErrorDescription() +
                " at " + e.getTextRange()
            );
        } // for

        return anyErrors;
    }

    public static void main(String[] args) throws Exception
    {
        if(args.length < 1) {
            System.err.println("Usage: kotlin_syntax_check.sh <file.kt> [file2.kt ...]");
            System.exit(2);
        }

        boolean anyError = false;

        for(String arg : args) {
            String  source   = new String(
                java.nio.file.Files.readAllBytes( java.nio.file.Paths.get(arg) )
            );
            if( hasSyntaxError(source) ) anyError = true;
        } // for

        if(anyError) System.exit(1);
    }

} // class kotlin_syntax_check
