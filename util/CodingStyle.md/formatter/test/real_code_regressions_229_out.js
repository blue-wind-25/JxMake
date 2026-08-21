/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

const x = 1; // Resolves to the project's own source tree via
             // tsconfig baseUrl, but is classified
             // "third-party", not "local"

console.log(x);
