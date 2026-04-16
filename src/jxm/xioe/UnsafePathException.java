/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xioe;


import java.io.IOException;


@SuppressWarnings("serial")
public class UnsafePathException extends IOException {

    public UnsafePathException(final Exception e)
    { super(e); }

    public UnsafePathException(final String msg)
    { super(msg); }

} // class UnsafePathException
