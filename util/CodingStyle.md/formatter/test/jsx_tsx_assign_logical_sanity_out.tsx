/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function Card(cond, name)
{
    const x  = cond && <div>{name}</div>;
    let   y;
    y = <Foo />;

    function Fallback(items)
    {
        return items ?? <div className="default">{name}</div>;
    }

    const list   = [x, <span>{name}</span>];
    const render = (n) => n ? <b>{n}</b> : <i>none</i>;

    return list.length ? y : render(1);
} // Card
