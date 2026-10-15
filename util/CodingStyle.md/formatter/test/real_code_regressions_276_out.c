/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

void spacing(int n, int mask, int* p, int** pp, char* s, int x)
{
    int   a = n++ & mask;
    int   b = n-- * 2;
    int   c = n * *p;
    int   d = n + *p;
    int   e = n & *p;
    int   f = (int)*p;
    int   g = sizeof(int*) * n;
    int   k = s[1] * *p;
    int   q = **pp;
    char* t = (char*)s;
    a = n++ & mask;
    c = n * *p;
    e = n - &x == 0;
    q = c ? *p : -*p;
}
