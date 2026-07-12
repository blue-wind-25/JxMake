/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

internal class BroadcastChannelImpl {

    fun tryOffer(select: SelectImplementation<*>)
    {
        val trySelectResult = select.trySelectDetailed(this@BroadcastChannelImpl, Unit)
        if(trySelectResult) println("ok")
    }

} // class BroadcastChannelImpl
