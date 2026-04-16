/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp;


import java.awt.BorderLayout;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JSplitPane;
import javax.swing.JPanel;


@SuppressWarnings("serial")
public abstract class JRefUsrKeyValueTables extends JPanel {

    private JReadOnlyKeyValueTable _refTable = null;
    private JKeyValueTable         _usrTable = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract boolean onDuplicateUsrKey(final String key, final String value, final boolean checked, final String oldKey);

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("this-escape")
    public JRefUsrKeyValueTables(final String refTitle, final String usrTitle, final String keyCaption, final String valueCaption, final Map<String, JKeyValueTable.State> refData, final Map<String, JKeyValueTable.State> usrData)
    {
        setLayout( new BorderLayout() );

        // Reference table (read-only except checkbox)
        _refTable = new JReadOnlyKeyValueTable(refTitle, keyCaption, valueCaption);

        if(refData != null) _refTable.initializeFromMap(refData);

        // User table (full access)
        _usrTable = new JKeyValueTable(usrTitle, keyCaption, valueCaption) {

            @Override
            protected boolean onCheckboxToggle(final String key, final String value, final boolean checked, final boolean newChecked)
            { return true; }

            @Override
            protected boolean onBeforeKeyEdit(final String key, final String value, final boolean checked)
            { return true; }

            @Override
            protected boolean onAfterKeyEdit(final String key, final String value, final boolean checked, final String oldKey)
            { return true; }

            @Override
            protected boolean onDuplicateKey(final String key, final String value, final boolean checked, final String oldKey)
            { return JRefUsrKeyValueTables.this.onDuplicateUsrKey(key, value, checked, oldKey); }

            @Override
            protected boolean onBeforeValueEdit(final String key, final String value, final boolean checked)
            { return true; }

            @Override
            protected boolean onAfterValueEdit(final String key, final String value, final boolean checked, final String oldValue)
            { return true; }

            @Override
            protected boolean onAddConfirmRow()
            { return true; }

            @Override
            protected boolean onRemoveConfirmRow(final int row, final String key, final String value, final boolean checked)
            { return true; }

            @Override
            protected String normalizeKey(final String key)
            { return key.toUpperCase(Locale.ROOT); }
        };

        if(usrData != null) _usrTable.initializeFromMap(usrData);

        // Vertical split
        final JSplitPane split = new JSplitPane(
                                     JSplitPane.VERTICAL_SPLIT,
                                     _refTable,
                                     _usrTable
                                 );

        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);
    }

    public void refresh()
    {
        if(_refTable != null) _refTable.refresh();
        if(_usrTable != null) _usrTable.refresh();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public JReadOnlyKeyValueTable refTable()
    { return _refTable; }

    public JKeyValueTable usrTable()
    { return _usrTable; }

    public Map<String, String> getFinalCheckedData()
    {
        Map<String, String> result = new HashMap<>();

        _refTable.getCheckedData().forEach(result::put);
        _usrTable.getCheckedData().forEach(result::put);

        return result;
    }

} // class JRefUsrKeyValueTables
