// g++ -Isys main.cpp -o /dev/null

// g++ -Isys -M main.cpp
// g++ -Isys -MM main.cpp

#include <stdio.h>

#include "all.h"

extern void printFromMainProgram();

// From 'atest.S/asm'
extern "C" void atest();

int main()
{
    atest();

    printf("\n"); fflush(stdout);

    printFromMainProgram();

    printf("\n"); fflush(stdout);

    printFromSharedLib();
    printFromStaticLib();

    printf("\n"); fflush(stdout);

    printFromCStaticLib();

    printf("\n"); fflush(stdout);

    printf("Test -D...='%s'\n", __MyMacro__);

    printf("\n"); fflush(stdout);

    return 0;
}
