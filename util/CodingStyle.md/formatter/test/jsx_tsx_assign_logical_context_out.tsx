/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function Assign(cond)
{
    let el;
    el  = <div>plain</div>;
    el += <span>appended</span>;

    const fallback = cond && <div className="a">{cond}</div>;
    const other    = cond || <span>b</span>;
    const nullish  = cond ?? <span>c</span>;

    return el;
} // Assign

function Compare(x)
{
    if(x < 1) return 0;

    return x;
}
