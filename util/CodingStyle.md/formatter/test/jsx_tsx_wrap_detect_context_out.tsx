/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

function Wide()
{
    return <VeryLongComponentName attributeOne={valueOne} attributeTwo={valueTwo} attributeThree={valueThree} />;
}

function Narrow()
{
    return <Small a={1} />;
}

function Compare(x)
{
    if(x < 1) return 0;

    return x;
}
