/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function Wrap(items)
{
    return <div>{<span>nested</span>}</div>;
}

function Spread(items)
{
    return foo(...items, <span>tail</span>);
}

function SpreadArr(items)
{
    return [...items, <span>tail</span>];
}

function Compare(x)
{
    if(x < 1) return 0;

    return x;
}
