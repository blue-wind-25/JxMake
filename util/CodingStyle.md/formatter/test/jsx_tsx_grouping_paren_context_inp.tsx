/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function Wrap() {
    const a = (<span>grouped</span>);
    const b = (<div className="b">{a}</div>);

    return a;
}

function Compare(x) {
    if (x < 1) return 0;

    return x;
}
