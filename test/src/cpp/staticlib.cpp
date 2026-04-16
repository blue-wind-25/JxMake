#include <stdio.h>

#include "staticlib.h"

extern void printFromStaticLib()
{
    printf("%s", _sString);
    fflush(stdout);
}
