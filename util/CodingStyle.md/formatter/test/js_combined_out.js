import fs from "fs";
import { readFile } from "node:fs/promises";

import express from "express";
import { debounce } from "lodash";

import { Widget } from "../components";
import { helper } from "./helper";

@Component({ selector: "app-widget" })
export class Widget {

    @Input() name;
    @Output() changed = new EventEmitter();
    #cache = new Map();

    static get instanceCount(     ) { return Widget._count;  }
    static set instanceCount(value) { Widget._count = value; }

    get x(     ) { return this._x;  }
    set x(value) { this._x = value; }

    async load(id, options = {})
    {
        const { id, name, ...rest }      = await fetchUser();
        const [first, second, ...others] = await fetchItems();
        const merged                     = { ...defaults, ...overrides };
        const label                      = `User: ${name}`;
        const len                        = this.profile?.bio?.length ?? 0;
        const calc                       = (a, b) => a + b;
        const withDefault                = (a, b = 10) => a + b;
        const process                    = (data) => {
            return transform(data);
        };
        this.#cache.set(id, merged);

        return merged;
    } // load

    *iterate()
    {
        yield 1;
        yield 2;
    }

} // class Widget

export default Widget;
