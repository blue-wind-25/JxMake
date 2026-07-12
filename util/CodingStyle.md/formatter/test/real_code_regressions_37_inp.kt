/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/** @suppress **/
internal fun consumesAll(vararg channels: ReceiveChannel<*>): CompletionHandler =
    { cause: Throwable? ->
        var exception: Throwable? = null
        for (channel in channels)
            try {
                channel.cancelConsumed(cause)
            } catch (e: Throwable) {
                if (exception == null) {
                    exception = e
                } else {
                    exception.addSuppressed(e)
                }
            }
        exception?.let { throw it }
    }

/** @suppress **/
internal fun ReceiveChannel<*>.consumes(): CompletionHandler = { cause: Throwable? ->
    cancelConsumed(cause)
}
