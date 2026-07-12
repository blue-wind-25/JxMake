/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
public open class JobSupport {
    internal fun makeCompletingOnce(proposedUpdate: Any?): Any? {
        loopOnState { state ->
            val finalState = tryMakeCompleting(state, proposedUpdate)
            when {
                finalState === COMPLETING_ALREADY ->
                    throw IllegalStateException(
                        "Job $this is already complete or completing, " +
                            "but is being completed with $proposedUpdate", proposedUpdate.exceptionOrNull)
                finalState === COMPLETING_RETRY -> return@loopOnState
                else -> return finalState
            }
        }
    }
} // class JobSupport
