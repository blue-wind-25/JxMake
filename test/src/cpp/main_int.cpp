#include <stdio.h>

static const char* _mString = "HELLO WORLD from main program!\n";

extern void printFromMainProgram()
{
    printf("%s", _mString);
    fflush(stdout);
}
