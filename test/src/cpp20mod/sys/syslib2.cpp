module;

#include <stdio.h>

export module syslib2;

export import :one;
export import :two;
       import :three;

export void printSyslib2_1()
{ printf( "%s", strData2_1() ); }

export void printSyslib2_2()
{ printf( "%s", strData2_2() ); }

export void printSyslib2_3()
{ printf( "%s", strData2_3() ); }
