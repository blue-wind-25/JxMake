/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import jxm.gcomp.*;
import jxm.gcomp.se.*;


public class ScriptEditor extends SwingApp {

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public ScriptEditor(final boolean useDarkColorTheme, final String initialFilePath)
    { super(useDarkColorTheme, initialFilePath); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void _initializeAll() throws Exception
    {
        // Create the GUI - main window
        final JxMakeRootPane rootPane = new JxMakeRootPane( _useDCT, (String) _extPar );

        _initializeMainWindow(Texts.ScrEdt_Title, rootPane);

        // Show the main window
        _showMainWindow( rootPane.getTextArea() );
    }

    @Override
    protected boolean handleWindowClosing()
    { return ( (JxMakeRootPane) rootPane() ).confirmQuit(); }

} // class ScriptEditor
