/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

export class Widget {
    #cache = new Map()

    static get instanceCount() { return Widget._count }
    static set instanceCount(value) { Widget._count = value }

    get x() { return this._x }
    set x(value) { this._x = value }
}
