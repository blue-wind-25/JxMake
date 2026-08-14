/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
function formatOffsetZ(offset: number, zone: number, hours: number, minusSign: string): string
{
    if(offset === 0) return 'Z';
    else             { return ( aVeryLongIdentifierNameThatPadsOutTheLineQuiteALotOnItsOwnHereReallyQuiteLongIndeedXXXX + padNumber(
        hours, 2, minusSign
    ) ); }
}
