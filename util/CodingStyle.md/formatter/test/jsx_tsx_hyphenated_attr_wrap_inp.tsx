/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function HyphenatedAttrs() {
    return <VeryLongComponentNameForcingAWrapHereForSure data-foo={value} aria-label="hello world this is quite long" onClick={handler}></VeryLongComponentNameForcingAWrapHereForSure>;
}
