// core imports
import fs from "fs";
import {debounce} from "lodash"; // utility for rate limiting
import express from "express";

/* Widget component */
@Component({selector: "app-widget"})
// class-level implementation note
export class Widget {
    // exposed input
    @Input() name
    @Output() changed = new EventEmitter() // fired on change

    async load() {
        // destructure the fetched user
        const {
            id,
            // comment inside destructuring pattern
            name,
            ...rest
        } = await fetchUser()
        const label = `User: ${name}` // greeting label
        // nullish fallback to zero
        const len = this.profile?.bio?.length ?? 0
            return merged
    }

    // generator for iteration
    *iterate() {
        yield 1 // first value
        yield 2
    }
}
