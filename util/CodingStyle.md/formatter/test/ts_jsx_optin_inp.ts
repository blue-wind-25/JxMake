/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

/*% JXM_CFMT_CFG jsx-in-ts=on */

function Widget(ok: boolean, name: string) {
    const Render = (x: number) => ok ? <div className="a">{x}</div> : <span>{x}</span>;

    if (ok) {
        return <div className="wrap">
            <span>Hello {name}</span>
            {ok ? <b>Yes</b> : <i>No</i>}
        </div>;
    }

    return <br />;
}
