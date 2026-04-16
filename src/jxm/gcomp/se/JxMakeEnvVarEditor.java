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

import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import jxm.*;
import jxm.gcomp.*;


public abstract class JxMakeEnvVarEditor extends SwingApp {

    // GUI-related variables
    private JRefUsrKeyValueTables _pnlRUKVTable = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public JxMakeEnvVarEditor(final boolean useDarkColorTheme)
    { super(useDarkColorTheme, null); }

    public abstract void onSaveDefault(final JKeyValueTable.StateHashMap refTable, final JKeyValueTable.StateHashMap usrTable);
    public abstract void onLoadDefault();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    @Override
    protected void _initializeAll() throws Exception
    {
        // Create the GUI - main window
        _initializeMainWindow(Texts.ScrEdt_EVETitle);

        // Create the GUI - the environment variable editor
        _pnlRUKVTable = new JRefUsrKeyValueTables(
                                Texts.ScrEdt_EVESysTitle, Texts.ScrEdt_EVEUsrTitle, Texts.ScrEdt_EVEVarName, Texts.ScrEdt_EVEVarValue,
                                null, null
                            ) {
            @Override
            protected boolean onDuplicateUsrKey(final String key, final String value, final boolean checked, final String oldKey)
            { return JxMakeRootPane.showYesNoQuestionDialog(Texts.ScrEdt_Warning, Texts.ScrEdt_EVEDupKey); }
        };

        _mainWindow.add(_pnlRUKVTable, BorderLayout.CENTER);

        // Create the GUI - 'OK', 'Save as Default', 'Reset to Default', and 'Cancel' buttons
        final JPanel buttonPanel = new JPanel( new GridLayout(1, 2, 10, 0) );

            final JButton btnOK     = _createButtonWithMnemonic(Texts.ScrEdt_EVEOK        );
            final JButton btnSaveAD = _createButtonWithMnemonic(Texts.ScrEdt_EVESaveAsDef );
            final JButton btnRstTD  = _createButtonWithMnemonic(Texts.ScrEdt_EVEResetToDef);
            final JButton btnCancel = _createButtonWithMnemonic(Texts.ScrEdt_EVECancel    );

            buttonPanel.add(btnOK    );
            buttonPanel.add(btnSaveAD);
            buttonPanel.add(btnRstTD );
            buttonPanel.add(btnCancel);

            buttonPanel.setBorder( BorderFactory.createEmptyBorder(10, 0, 0, 0) );

        _mainWindow.add(buttonPanel, BorderLayout.SOUTH);

        // Register fast access for the default and cancel buttons
        _registerDefaultAndCancelButton(btnOK, btnCancel);

        // Add event listeners to the 'OK' button
        btnOK.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ignored)
            {
                // Save the tables
                _savedRefTable = _pnlRUKVTable.refTable().getAllData().deepClone();
                _savedUsrTable = _pnlRUKVTable.usrTable().getAllData().deepClone();

                // Close window
                _mainWindow.postEvent_WINDOW_CLOSING();
            }
        } );

        // Add event listeners to the 'Reset to Default' button
        btnRstTD.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ignored)
            {
                onLoadDefault();

                _pnlRUKVTable.refresh();
            }
        } );

        // Add event listeners to the 'Save as Default' button
        btnSaveAD.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ignored)
            { onSaveDefault( _pnlRUKVTable.refTable().getAllData(), _pnlRUKVTable.usrTable().getAllData() ); }
        } );

        // Add event listeners to the 'Cancel' button
        btnCancel.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ignored)
            {
                // Restore the tables
                _pnlRUKVTable.refTable().initializeFromMap(_savedRefTable);
                _pnlRUKVTable.usrTable().initializeFromMap(_savedUsrTable);

                // Close window
                _mainWindow.postEvent_WINDOW_CLOSING();
            }
        } );

        // Show the main window
        _showMainWindow(_pnlRUKVTable, false);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private JKeyValueTable.StateHashMap _savedRefTable = null;
    private JKeyValueTable.StateHashMap _savedUsrTable = null;

    public void initializeFromMap(final JKeyValueTable.StateHashMap refTable, final JKeyValueTable.StateHashMap usrTable)
    {
        _savedRefTable = refTable.deepClone();
        _savedUsrTable = usrTable.deepClone();

        _pnlRUKVTable.refTable().initializeFromMap(refTable);
        _pnlRUKVTable.usrTable().initializeFromMap(usrTable);
    }

    public JKeyValueTable.StateHashMap getRefTable()
    { return _savedRefTable; }

    public JKeyValueTable.StateHashMap getUsrTable()
    { return _savedUsrTable; }

    public Map<String, String> getFinalCheckedData()
    { return _pnlRUKVTable.getFinalCheckedData(); }

} // class JxMakeEnvVarEditor
