/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class UndispatchedContextCollector<T>(
    downstream: FlowCollector<T>,
    private val emitContext: CoroutineContext
) {
    private val countOrElement = threadContextElements(emitContext)
    private val emitRef: suspend (T) -> Unit = { downstream.emit(it) } // Allocate suspend function ref once on creation
}
