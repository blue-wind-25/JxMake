/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

import { readFile } from "node:fs/promises";

import { WidgetX } from "components/Widget";   // Resolves to the project's own source tree via
                                               // tsconfig `baseUrl`/`paths`, but is classified
                                               // "third-party", not "local"
import { debounce } from "lodash";

import { helper } from "./helper";

console.log(readFile, debounce, helper, WidgetX);
