/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function Foo()
{
    return <div className="a">
        <span>Hello {name}</span>
    </div>;
}

function Empty()
{
    return <br />;
}

function Compare(x)
{
    if(x < 1) return 0;

    return x;
}
