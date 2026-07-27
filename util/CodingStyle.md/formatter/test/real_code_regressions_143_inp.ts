/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
function combine(a: boolean, b: boolean, c: string | undefined): void {
    a ||= b;
    b &&= a;
    c ??= "default";
}
