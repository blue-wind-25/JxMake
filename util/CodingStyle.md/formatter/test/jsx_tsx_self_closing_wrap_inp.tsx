/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function OneAttr() {
    return <VeryVeryLongComponentNameIndeedTrulyLong attributeOneAloneIsAlreadyMoreThanWideEnough={value} />;
}

function NoAttrs() {
    return <ThisSelfClosingTagHasNoAttributesAtAllSoItStaysOnOneLineNoMatterHowLongItsNameIs />;
}

function WithChildrenShort() {
    return <VeryLongComponentNameWithChildren attributeOne={valueOne} attributeTwo={valueTwo}>{child}</VeryLongComponentNameWithChildren>;
}

function WithChildrenWrapped() {
    return <VeryLongComponentNameWithChildrenIndeed attributeOneIsQuiteLong={valueOne} attributeTwoIsAlsoQuiteLong={valueTwo}>{child}</VeryLongComponentNameWithChildrenIndeed>;
}

function WithMultilineChildrenPreservedVerbatim() {
    return <VeryLongComponentNameWithMultilineChildrenHereForSure attributeOneIsQuiteLong={valueOne} attributeTwoIsAlsoQuiteLong={valueTwo}>
        <span>  weird   spacing   preserved  </span>
        {items.map(x => <li key={x.id}>{x.label}</li>)}
    </VeryLongComponentNameWithMultilineChildrenHereForSure>;
}
