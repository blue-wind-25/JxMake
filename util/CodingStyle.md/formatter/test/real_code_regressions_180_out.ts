/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
function formatOffset(offset: number, zone: number, hours: number, minusSign: string): string
{
    if(offset === 0) return 'Z';
    else             return ( (zone >= 0 ? '+' : '') + padNumber(
        hours, 2, minusSign
    ) + ':' + padNumber( Math.abs(zone % 60), 2, minusSign ) );
}
