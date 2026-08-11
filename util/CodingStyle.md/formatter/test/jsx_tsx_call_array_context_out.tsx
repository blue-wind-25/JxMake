/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function Wrap(a, b)
{
    return foo(<span>a</span>, <span className="b">{b}</span>);
}

const arr = [<div>one</div>, <div>two</div>];

function Compare(x)
{
    if(x < 1) return 0;

    return x;
}
