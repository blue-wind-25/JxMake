/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class Holder<E>( private val onUndeliveredElement: ( (E) -> Unit )? ) {

    @Suppress("UNUSED_PARAMETER")
    private fun processResultSelectReceiveOrNull(ignoredParam: Any?, selectResult: Any?): Any? =
        if(selectResult === CHANNEL_CLOSED) {
            if(closeCause == null) null
            else                   throw receiveException
        }
        else selectResult

    @Suppress("UNCHECKED_CAST", "UNUSED_PARAMETER", "RedundantNullableReturnType")
    private fun processResultSelectReceiveCatching(ignoredParam: Any?, selectResult: Any?): Any? =
        if(selectResult === CHANNEL_CLOSED) closed(closeCause)
        else                                success(selectResult as E)

    @Suppress("UNCHECKED_CAST")
    private val onUndeliveredElementReceiveCancellationConstructor: OnCancellationConstructor? = onUndeliveredElement?.let {
        { select: SelectInstance<*>, _: Any?, element: Any? ->
            { _, _, _ ->
                if(element !== CHANNEL_CLOSED) onUndeliveredElement.callUndeliveredElement(
                element as E, select.context
            )
            }
        }
    }

    fun other()
    {
    }

} // class Holder
