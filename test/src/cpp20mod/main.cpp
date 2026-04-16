#include <stdio.h>

#include <sys_header.h>

import userlib;
import syslib1;

import "user_hu.h";

int main()
{
    printf("\n");

    printUserlib_1();
    printUserlib_2();
    printUserlib_n();

    printSysHeader();

    printSyslib1_1();
    printSyslib1_2();

    printSyslib2_1();
    printSyslib2_2();
    printSyslib2_3();

    printSyslib3_1();
    printSyslib3_2();

    return 0;
}
