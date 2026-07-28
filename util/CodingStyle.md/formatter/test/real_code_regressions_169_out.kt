/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class Foo {

    fun bar(fragments: List<String>, metadataVersion: Int)
    {
        val groupedFragments = fragments
            .groupBy { it }
        val packageFragmentNames : List<String> = groupedFragments.map { it.key }
        val emptyPackageFragmentNames : List<String> = groupedFragments.filter { it.value.all(
            KmModuleFragment::isEmpty
        ) }.map { it.key }
        val versionExt = KlibMetadataVersionWriteExtension(metadataVersion)
    }

} // class Foo

internal class DummyJavaFileCodeStyleFacadeFactory : JavaFileCodeStyleFacadeFactory {

    private class DummyJavaFileCodeStyleFacade : JavaFileCodeStyleFacade {

        override fun getNamesCountToUseImportOnDemand()        : Int     = 0
        override fun isToImportOnDemand(qualifiedName: String) : Boolean = false
        override fun useFQClassNames()                         : Boolean = false
        override fun isJavaDocLeadingAsterisksEnabled()        : Boolean = false
        override fun isGenerateFinalParameters()               : Boolean = false
        override fun isGenerateFinalLocals()                   : Boolean = false
        override fun withLanguage(
            language: Language
        ): CodeStyleSettingsFacade = DummyJavaFileCodeStyleFacade()
        override fun getTabSize()                       : Int     = 4
        override fun getIndentSize()                    : Int     = 4
        override fun isSpaceBeforeComma()               : Boolean = false
        override fun isSpaceAfterComma()                : Boolean = false
        override fun isSpaceAroundAssignmentOperators() : Boolean = false

    } // class DummyJavaFileCodeStyleFacade

    override fun createFacade(
        psiFile: PsiFile
    ): JavaFileCodeStyleFacade = DummyJavaFileCodeStyleFacade()

} // class DummyJavaFileCodeStyleFacadeFactory
