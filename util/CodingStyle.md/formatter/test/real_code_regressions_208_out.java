/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
package com.example;

public class Repro6 {

    public void m()
    {
        if(a) {
            if(b) {
                switch(loc) {

                    case X: {
                            if(d) {
                                int alignTo;
                                if(e) {
                                    alignTo = 1;
                                }
                                else {
                                    String source        = method.print( getCursor() );
                                    int    firstArgIndex = source.indexOf(
                                        firstArg.print( getCursor() )
                                    );
                                    alignTo = firstArgIndex;
                                }
                            } // if
                            break;
                        }

                } // switch
            } // if
        } // if
    }

} // class Repro6
