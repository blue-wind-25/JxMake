// Core imports
import fs from "fs";

import express from "express";
import { debounce } from "lodash"; // Utility for rate limiting

/* Widget component */
@Component({ selector: "app-widget" })
// Class-level implementation note
export class Widget {
    // Exposed input
    @Input() name;
    @Output() changed = new EventEmitter(); // Fired on change

    async load()
    {
        // Destructure the fetched user
        const {
            id,
            // Comment inside destructuring pattern
            name,
            ...rest
        } = await fetchUser();
        const label = `User: ${name}`; // Greeting label
        // Nullish fallback to zero
        const len = this.profile?.bio?.length ?? 0;

        return merged;
    } // async load

    // Generator for iteration
    *iterate()
    {
        yield 1; // First value
        yield 2;
    }
} // class Widget
