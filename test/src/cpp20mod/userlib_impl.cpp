// Clang (as of version 15.0.6) does not currently support a separated module implementation unit that do not export a module partition
#ifndef __clang__
module userlib;
#endif

// Clang (as of version 15.0.6) will almost always generate object files with duplicated symbols when using system header units
#ifndef __clang__
import <iostream>;
#else
#include <iostream>
#endif

extern void printUserlib_n_impl()
{ std::cout << "HELLO WORLD from 'printUserlib_n()'!\n\n"; }
