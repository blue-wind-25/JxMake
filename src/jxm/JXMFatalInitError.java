/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


@SuppressWarnings("serial")
public class JXMFatalInitError extends JXMException {

    // NOTE : Do not translate these strings!
    public static final String PrefixString = "##### ##### ##### FATAL INITIALIZATION ERROR: ";
    public static final String SuffixString = " ##### ##### #####";

    public JXMFatalInitError(final String msg)
    { super(PrefixString + msg + SuffixString); }

} // class JXMFatalInitError
