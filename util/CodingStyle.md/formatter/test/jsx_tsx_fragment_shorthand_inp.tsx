/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function BareExpressionFragment() {
    return <>{returnFunction({ ref: this.context.onToggleRef })}</>;
}

function MultiChildFragment() {
    return <>
        <h1>Title</h1>
        <p>Body</p>
    </>;
}

function NestedFragmentInsideElement() {
    return <div>
        <>{items.map(x => <span key={x.id}>{x.label}</span>)}</>
    </div>;
}
