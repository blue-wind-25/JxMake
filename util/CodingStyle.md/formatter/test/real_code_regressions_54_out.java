/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

public class Range {

    public Range(Object begin, Object end)
    {
        if(begin == null) throw new IllegalArgumentException("begin can't be null");
        if(end == null) throw new IllegalArgumentException("end can't be null");
    }

    /*
     *  return true if at least one of the method's type parameters has the same type as the specified type
     */
    private boolean hasParameterwithSameTypeThanResultType()
    {
        return true;
    }

} // class Range
