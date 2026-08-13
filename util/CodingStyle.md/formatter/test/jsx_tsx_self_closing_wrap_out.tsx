/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function OneAttr()
{
    return <VeryVeryLongComponentNameIndeedTrulyLong
        attributeOneAloneIsAlreadyMoreThanWideEnough={value}
    />;
}

function NoAttrs()
{
    return <ThisSelfClosingTagHasNoAttributesAtAllSoItStaysOnOneLineNoMatterHowLongItsNameIs />;
}

function WithChildren()
{
    return <VeryLongComponentNameWithChildren attributeOne={valueOne} attributeTwo={valueTwo}>{child}</VeryLongComponentNameWithChildren>;
}
