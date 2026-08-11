/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

const Render = (name) => <div className="a">Hello {name}</div>;

function Cond(ok) {
    return ok ? <span>Yes</span> : <span>No</span>;
}

function CondNested(ok, name) {
    return ok ? <div className="a"><span>{name}</span></div> : <br />;
}

function Compare(x) {
    if (x < 1) return 0;

    return x;
}
