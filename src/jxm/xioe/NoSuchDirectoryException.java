/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xioe;


import java.io.IOException;


@SuppressWarnings("serial")
public class NoSuchDirectoryException extends IOException {

    public NoSuchDirectoryException(final Exception e)
    { super(e); }

    public NoSuchDirectoryException(final String msg)
    { super(msg); }

} // class NoSuchDirectoryException
