#pragma once

#if defined(_MSC_VER) || defined (__CYGWIN__) || defined(__MINGW32__) || defined(__MINGW64__) || defined(__MSYS__)
    #define USERLIB_DLL_IMPORT __declspec(dllimport)
    #define USERLIB_DLL_EXPORT __declspec(dllexport)
    #define USERLIB_DLL_LOCAL
#else
    #define USERLIB_DLL_IMPORT __attribute__(( visibility ("default") ))
    #define USERLIB_DLL_EXPORT __attribute__(( visibility ("default") ))
    #define USERLIB_DLL_LOCAL  __attribute__(( visibility ("hidden" ) ))
#endif

#ifdef USERLIB_EXPORTS
    #define USERLIB_API   USERLIB_DLL_EXPORT
#else
    #define USERLIB_API   USERLIB_DLL_IMPORT
#endif
    #define USERLIB_LOCAL USERLIB_DLL_LOCAL

extern USERLIB_LOCAL const char* _dString;

extern USERLIB_API void printFromSharedLib();
