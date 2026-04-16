// Clang (as of version 15.0.6) does not currently support a separated module implementation unit that do not export a module partition
#ifdef __clang__
module;
#include "userlib_impl.cpp"
#endif

export module userlib;

// Clang (as of version 15.0.6) will almost always generate object files with duplicated symbols when using system header units
#ifndef __clang__
import <iostream>;
#endif

export import syslib2;
export import syslib3;

#ifndef __clang__
extern void printUserlib_n_impl();
#endif

export void printUserlib_1()
{ std::cout << "HELLO WORLD from 'printUserlib_1()'!\n\n"; }

export void printUserlib_2()
{ std::cout << "HELLO WORLD from 'printUserlib_2()'!\n\n"; }

export void printUserlib_n()
{ printUserlib_n_impl(); }
