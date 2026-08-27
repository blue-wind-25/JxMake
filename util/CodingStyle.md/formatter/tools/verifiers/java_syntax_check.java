/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;

/**
 * Lightweight Java syntax checker.
 *
 * Parses a .java source file into an AST and reports syntax errors.
 * This does NOT require referenced classes to exist and does NOT
 * perform type checking.
 *
 * Build:
 *     JDK=/opt/openjdk-21_linux-x64_bin/jdk-21
 *     $JDK/bin/javac java_syntax_check.java
 *
 * Run:
 *     $JDK/bin/java java_syntax_check <file.java> [file2.java ...]
 */
public class java_syntax_check {

    static class SourceFile extends SimpleJavaFileObject {

        private final String source;

        SourceFile(final String className, final String source)
        {
            super(
                URI.create( "string:///" +
                    className.replace('.', '/') +
                    Kind.SOURCE.extension ),
                Kind.SOURCE
            );
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors)
        {
            return source;
        }

    } // class SourceFile

    public static boolean hasSyntaxError(final String source) throws IOException
    {

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if(compiler == null) throw new IllegalStateException(
            "No system compiler found (are you using a JDK instead of a JRE?)"
        );

        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        final JavaFileObject file = new SourceFile("Test", source);

        final JavacTask task = (JavacTask)compiler.getTask(
            null, null, diagnostics, Arrays.asList("-proc:none"), null, Arrays.asList(file)
        );

        // Parse only
        final Iterable<? extends CompilationUnitTree> trees = task.parse();

        // Prevent "unused"
        trees.iterator().hasNext();

        boolean anyErrors = false;

        for( final Diagnostic<? extends JavaFileObject> d :
                diagnostics.getDiagnostics() ) {

            if( d.getKind() == Diagnostic.Kind.ERROR ) {
                anyErrors = true;

                System.out.printf(
                    "%d:%d: %s%n",
                    d.getLineNumber(),
                    d.getColumnNumber(),
                    d.getMessage(null)
                );
            } // if
        } // for

        return anyErrors;
    }

    public static void main(final String[] args) throws Exception
    {

        if(args.length < 1) {
            System.err.println("Usage: java_syntax_check.sh <file.java> [file2.java ...]");
            System.exit(2);
        }

        boolean anyError = false;

        for(final String arg : args) {
            final String source = Files.readString( Paths.get(arg) );

            if( hasSyntaxError(source) ) anyError = true;
        } // for

        if(anyError) System.exit(1);
    }

} // class java_syntax_check
