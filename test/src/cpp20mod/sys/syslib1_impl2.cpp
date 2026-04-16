#ifndef __clang__
module;
#endif

#include <stdio.h>

// Clang (as of version 15.0.6) does not currently support a separated module implementation unit that do not export a module partition
#ifndef __clang__
module syslib1;
#endif

extern void printSyslib1_2_impl()
{ printf("HELLO WORLD from 'printSyslib1_2()'!\n\n"); }
