/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/*% JXM_CFMT_CFG curly-general-scope-reindent=on;curly-general-scope-reindent-multipass=on;curly-general-scope-reindent-postpass=on;line-length=40 */

package com.example.core

class WidgetRegistrationCollectionHolder {

    var displayName : String  = ""
    var enabled     : Boolean = false

} // class WidgetRegistrationCollectionHolder

class WidgetRegistry {

    private fun allRegisteredWidgetEntriesAreCurrentlyReadyForActivationRightNow(
        entries : List<WidgetRegistrationCollectionHolder>
    ): Boolean = entries.all { entry ->
        entry.enabled
    }

    private fun instantiateAndConfigureWidgetFromRegistrationSourceHolderNow(
        source : WidgetRegistrationCollectionHolder
    ): WidgetRegistrationCollectionHolder =
        source.let {
        WidgetRegistrationCollectionHolder().apply {
            displayName = it.displayName
            enabled = true
        }.also { built ->
            built.displayName = built.displayName.trim()
        }
        }

    private fun singleMatchingWidgetOrNullFromCollection(
        entries : List<WidgetRegistrationCollectionHolder>
    ): WidgetRegistrationCollectionHolder? =
        entries.singleOrNull { it.owner.hasCandidateShape(
            requiresActivation = true, minimumEntryCount = 1
        ) }

    fun register(
        w : WidgetRegistrationCollectionHolder
    ): Boolean
    {
        return w.enabled && checkSomethingReallyLongMethodNameThatWillDefinitelyForceLineWrap(
            w
        ) == true
    }

    private fun checkSomethingReallyLongMethodNameThatWillDefinitelyForceLineWrap(
        w: WidgetRegistrationCollectionHolder
    ): Boolean = true

} // class WidgetRegistry
