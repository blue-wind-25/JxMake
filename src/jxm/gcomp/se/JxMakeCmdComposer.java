/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp.se;


import java.awt.BorderLayout;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import jxm.*;
import jxm.gcomp.*;

import static jxm.tool.CommandSpecifier.*;


public abstract class JxMakeCmdComposer extends SwingApp {

    private JCommandComposerPanel _pnlCmdComp = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public JxMakeCmdComposer(final boolean useDarkColorTheme)
    { super(useDarkColorTheme, null); }

    public abstract void onPaste(final String[] command);
    public abstract void onPasteAndExecute(final String[] command);

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    @Override
    protected void _initializeAll() throws Exception
    {
        // Create the GUI - main window
        _initializeMainWindow(Texts.ScrEdt_CCTitle);

        // Create the GUI - the command composer
        _pnlCmdComp = new JCommandComposerPanel(null);

        _mainWindow.add(_pnlCmdComp, BorderLayout.CENTER);

        // Create the GUI - 'Paste' and 'Close' buttons
        final JPanel buttonPanel = new JPanel( new GridLayout(1, 4, 10, 0) );

            final JButton btnPaste     = _createButtonWithMnemonic(Texts.ScrEdt_CCPaste    );
            final JButton btnPasteExec = _createButtonWithMnemonic(Texts.ScrEdt_CCPasteExec);
            final JButton btnClose     = _createButtonWithMnemonic(Texts.ScrEdt_CCClose    );

            buttonPanel.add(btnPaste    );
            buttonPanel.add(btnPasteExec);
            buttonPanel.add(btnClose    );

            buttonPanel.setBorder( BorderFactory.createEmptyBorder(10, 0, 0, 0) );

        _mainWindow.add(buttonPanel, BorderLayout.SOUTH);

        // Register fast access for the default and cancel buttons
        _registerDefaultAndCancelButton(btnPaste, btnClose);

        // Add event listeners to the 'Paste' button
        btnPaste.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                final String[] cmdArray = _pnlCmdComp.getCommandArray();
                if(cmdArray.length > 0) onPaste(cmdArray);
            }
        } );

        // Add event listeners to the 'Paste and Execute' button
        btnPasteExec.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                final String[] cmdArray = _pnlCmdComp.getCommandArray();
                if(cmdArray.length > 0) onPasteAndExecute(cmdArray);
            }
        } );

        // Add event listeners to the 'Close' button
        btnClose.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ignored)
            { _mainWindow.postEvent_WINDOW_CLOSING(); }
        } );

        // Show the main window
        _showMainWindow(_pnlCmdComp, false);
    }

    @Override
    protected boolean handleWindowClosing()
    {
        if( _pnlCmdComp.isModified() ) {
            if( !JxMakeRootPane.showYesNoQuestionDialog(Texts.ScrEdt_Warning, Texts.ScrEdt_YN_CmdCWClose) ) return false;
        }

        return true;
    }

    public void setCommandRegistry(final CommandRegistry cmdRegistry)
    { _pnlCmdComp.setCommandRegistry(cmdRegistry); }

} // class JxMakeCmdComposer
