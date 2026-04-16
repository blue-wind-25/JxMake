/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


@SuppressWarnings("serial")
public class JXMAsmError extends JXMException {

    // NOTE : Do not translate this string!
    public static final String PrefixString = "ASSEMBLY ERROR: ";

    public JXMAsmError(final String msg)
    { super(PrefixString + msg); }

} // class JXMAsmError
