// Clang (as of version 15.0.6) does not currently support a separated module implementation unit that do not export a module partition
#ifdef __clang__
module;
#include "syslib1_impl1.cpp"
#include "syslib1_impl2.cpp"
#endif

export module syslib1;

#ifndef __clang__
extern void printSyslib1_1_impl();
extern void printSyslib1_2_impl();
#endif

export void printSyslib1_1()
{ printSyslib1_1_impl(); }

export void printSyslib1_2()
{ printSyslib1_2_impl(); }
