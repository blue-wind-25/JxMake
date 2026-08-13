/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function SpreadAttr()
{
    return <VeryLongComponentNameForSpreadAttributeWrapTestingPurposes
        {...somePropsObjectThatIsQuiteLong}
    />;
}

function BooleanAttr()
{
    return <VeryLongComponentNameForBooleanAttributeWrapTestingPurposes
        disabledBecauseOfSomeReason
    />;
}

function ExpressionAttr()
{
    return <VeryLongComponentNameForExpressionAttributeWrapTesting
        onClick={handlers.click.bind(this, item.id)}
    />;
}

function MixedAttrs()
{
    return <VeryLongComponentNameMixingAttributeKindsForWrapTesting
        {...baseProps}
        disabledWhenLoading
        onClick={handlers.click.bind(this, item.id)}
    />;
}
