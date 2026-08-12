/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function Widget(ok, name)
{
    const Render = (x) => ok ? <div className="a">{x}</div> : <span>{x}</span>;

    if(ok) {
        return <div className="wrap">
            <span>Hello {name}</span>
            {ok ? <b>Yes</b> : <i>No</i>}
        </div>;
    }

    return <br />;
} // Widget
