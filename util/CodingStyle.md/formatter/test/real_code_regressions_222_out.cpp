/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
void doIt()
{
    invoke( []() {
    int x = 1;
    int y = 2;
    foo(x);
    } );
} // doIt
