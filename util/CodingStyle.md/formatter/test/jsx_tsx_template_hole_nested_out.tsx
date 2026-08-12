/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function PlainNested(x)
{
    return `a ${`b ${x + 1}`} d`;
}

function JsxNested(x)
{
    return `a ${`b ${<X/>}`}`;
}

function SafetyNet(x)
{
    if(x < 1) return `not jsx here`;

    return `still fine`;
}
