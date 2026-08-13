/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

class Example
{
    private int scanNestedThings(final int limit)
    {
        int outer = 0;
        while( outer < limit ) {
            boolean closed = false;
            while( !closed ) {
                final int probeResult = probe(outer);
                if( probeResult > 0 ) {
                    closed = true;
                }
                else {
                    closed = probeResult == 0;
                }
            } // while !closed
            ++outer;
        }

        return outer;
    }

    private boolean probe(final int value)
    {
        int depth = 0;
        while(depth < value) ++depth;
        if(depth == 0) ++depth;
        else if(depth > 0) {
            depth += value;
        }

        return depth > value;
    }

}
