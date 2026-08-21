/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.gdr;

/**
 * Tiny shared text-scan helpers used by more than one {@code gdr} sibling class -- kept here
 * instead of duplicated per-class
 */
final class GdrTextUtil {

    private GdrTextUtil()
    {
    }

    /** Counts {@code '\n'} occurrences in {@code text} -- used to advance a line counter past a token spanning multiple lines */
    static int countNewlines(String text)
    {
        int count = 0;
        for( int i = 0; i < text.length(); ++i ) {
            if( text.charAt(i) == '\n' ) count++;
        }

        return count;
    }

} // class GdrTextUtil
