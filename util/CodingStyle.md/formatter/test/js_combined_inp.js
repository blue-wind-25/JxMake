/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

import fs from "fs";
import {readFile} from "node:fs/promises";
import {debounce} from "lodash";
import express from "express";
import {Widget} from "../components";
import {helper} from "./helper";
import { WidgetX } from "components/Widget";   // resolves to the project's own source tree via
                                               // tsconfig `baseUrl`/`paths`, but is classified
                                               // "third-party", not "local"
import { WidgetY } from "components/Widget";   /* resolves to the project's own source tree via
                                                  tsconfig `baseUrl`/`paths`, but is classified
                                                  "third-party", not "local" */

@Component({selector: "app-widget"})
export class Widget {
    @Input() name
    @Output() changed = new EventEmitter()
    #cache = new Map()

    static get instanceCount() { return Widget._count }
    static set instanceCount(value) { Widget._count = value }

    get x() { return this._x }
    set x(value) { this._x = value }

    async load(id, options = {}) {
        const {id,name,...rest} = await fetchUser()
        const [first,second,...others] = await fetchItems()
        const merged = {...defaults,...overrides}
        const label = `User: ${name}`
        const len = this.profile?.bio?.length ?? 0
        const calc = (a,b) => a + b
        const withDefault = (a,b=10) => a + b
        const process = (data) => {
            return transform(data)
        }
        this.#cache.set(id, merged)
        return merged
    }

    *iterate() {
        yield 1
        yield 2
    }
}

export default Widget;
