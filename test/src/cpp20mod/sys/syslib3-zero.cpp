module;

#include <stdio.h>

module syslib3:zero;

import :one;
import :two;

void printSyslib3_1_impl()
{ printf( "%s", strData3_1() ); }

void printSyslib3_2_impl()
{ printf( "%s", strData3_2() ); }
