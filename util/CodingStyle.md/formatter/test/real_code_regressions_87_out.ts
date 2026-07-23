/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

export class Link {

    /**
     * - Before each effect run, all previous dep links' version are reset to -1
     * - During the run, a link's version is synced with the source dep on access
     */
    version : number;

    /**
     * Pointers for doubly-linked lists
     */
    nextDep? : Link;
    prevDep? : Link;

} // class Link
