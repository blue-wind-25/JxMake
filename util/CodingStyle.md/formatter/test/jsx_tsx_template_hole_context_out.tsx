/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function Bare()
{
    return `text ${<Foo/>} more`;
}

function PlainInterpolation(a, b)
{
    return `sum ${a + b} end`;
}

function Fallback(x)
{
    return `val ${x < 1 ? <A/> : <B/>} end`;
}
