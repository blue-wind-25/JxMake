/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
class Foo {

    private fun obtainTaskOrDeallocateWorker(): Runnable?
    {
        while(true) {
            when( val nextTask = queue.removeFirstOrNull() ) {

                null -> synchronized(workerAllocationLock) {
                    runningWorkers.decrementAndGet()
                    if(queue.size == 0) return null
                    runningWorkers.incrementAndGet()
                }
                else -> return nextTask

            } // when val nextTask = queue.removeFirstOrNull()
        } // while
    }

} // class Foo
