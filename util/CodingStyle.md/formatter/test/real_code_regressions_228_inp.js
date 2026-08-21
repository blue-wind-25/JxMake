/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

import {readFile} from "node:fs/promises";
import {debounce} from "lodash";
import {helper} from "./helper";
import { WidgetX } from "components/Widget";   // resolves to the project's own source tree via
                                               // tsconfig `baseUrl`/`paths`, but is classified
                                               // "third-party", not "local"

console.log(readFile, debounce, helper, WidgetX);
