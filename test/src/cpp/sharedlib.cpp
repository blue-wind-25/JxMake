#include <stdio.h>

#include "sharedlib.h"

extern void printFromSharedLib()
{
    printf("%s", _dString);
    fflush(stdout);
}
