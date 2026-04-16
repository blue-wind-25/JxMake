#include <stdio.h>

#ifndef __CYGWIN__
    #include "cstaticlib.h"
#else
    // ### ??? TODO : Verify if this form is needed because of a GCC Cygwin bug ??? ###
    #include "cstaticlib/cstaticlib.h"
#endif

#ifdef __cplusplus
extern "C" {
#endif

static const char* _sString = "HELLO WORLD from C static library!\n";

extern void printFromCStaticLib()
{
    printf("%s", _sString);
    fflush(stdout);
}

#ifdef __cplusplus
}
#endif
