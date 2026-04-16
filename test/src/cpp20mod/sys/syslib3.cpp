module;

#include <stdio.h>

export module syslib3;

import :zero;

export void printSyslib3_1()
{ printSyslib3_1_impl(); }

export void printSyslib3_2()
{ printSyslib3_2_impl(); }
