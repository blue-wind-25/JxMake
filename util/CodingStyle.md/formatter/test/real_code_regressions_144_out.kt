/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class IncrementalCompilationContext(
    val reporter : ICReporter = DoNothingICReporter,
    /**
     * Controls whether changes in lookup cache should be tracked
     */
    val trackChangesInLookupCache : Boolean = false,
    val icFeatures : IncrementalCompilationFeatures = IncrementalCompilationFeatures.DEFAULT_CONFIGURATION,
) {

    val x : Int = 0

} // class IncrementalCompilationContext
