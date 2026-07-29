/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

// Found via dogfood testing against `JetBrains/kotlin` (C6 cluster,
// `AnalyzerFacade.kt`'s `LazyModuleDependencies` class): a typed `val`/`var` property with a
// `by`-delegate initializer (`val x: Type by someDelegate { ... }`) has no `=` token at all, so
// KotlinDeclarationAlignmentRule.parseKotlinDeclaration's type-token scan loop (which only
// stopped on `=`) ran to the end of the statement, sweeping the entire delegate expression --
// including a multi-line, multi-statement trailing-lambda body -- into `typeTokens`. That got
// silently flattened onto one line by the alignment renderer, fusing unrelated statements
// together with no separator and producing invalid Kotlin.
class Foo {

    val x: Set<Y> by storageManager.createLazyValue {
        foo(bar).mapTo(HashSet<Y>()) {
            a()
            @Suppress("X")
            b()
        }
    }

} // class Foo
