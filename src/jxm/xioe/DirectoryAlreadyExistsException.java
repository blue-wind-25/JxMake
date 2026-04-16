/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xioe;


import java.io.IOException;


@SuppressWarnings("serial")
public class DirectoryAlreadyExistsException extends IOException {

    public DirectoryAlreadyExistsException(final Exception e)
    { super(e); }

    public DirectoryAlreadyExistsException(final String msg)
    { super(msg); }

} // class DirectoryAlreadyExistsException
