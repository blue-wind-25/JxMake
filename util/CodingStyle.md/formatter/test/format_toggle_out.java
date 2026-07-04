package com.example.toggle;

// Exercises the JXM_CFMT_DIS/JXM_CFMT_ENA formatting-toggle markers (STATE.md Task A):
// Both the line-comment and block-comment marker forms, in the middle of a class body,
// Must leave everything between DIS and ENA byte-for-byte untouched while normal
// Formatting still applies immediately before and after each frozen region.

public class FormatToggle {
    private int before = 1;

    //% JXM_CFMT_DIS
    void   frozenMethod(  int a,int b  ) {
    int   c=a+b;
        if(c>0){
    return;
        }
    }
    //% JXM_CFMT_ENA

    private int between = 2;

    /*% JXM_CFMT_DIS */
    int    frozenField   =   42;
    void frozenTwo( )
    {
            int   x = 1;
    }
    /*% JXM_CFMT_ENA */

    private int after = 3;
}
