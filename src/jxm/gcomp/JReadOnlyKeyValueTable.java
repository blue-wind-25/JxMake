/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp;


@SuppressWarnings("serial")
public class JReadOnlyKeyValueTable extends JKeyValueTable {

    public JReadOnlyKeyValueTable(final String title, final String keyCaption, final String valueCaption)
    {
        super(title, keyCaption, valueCaption);

        _btnAdd    .setEnabled(false);
        _btnRemove .setEnabled(false);
        _btnRemoveM.setEnabled(false);
    }

    @Override
    protected boolean onCheckboxToggle(final String key, final String value, final boolean checked, final boolean newChecked)
    { return true; }

    @Override
    protected boolean onBeforeKeyEdit(final String key, final String value, final boolean checked)
    { return false; }

    @Override
    protected boolean onAfterKeyEdit(final String key, final String value, final boolean checked, final String oldKey)
    { return false; }

    @Override
    protected boolean onDuplicateKey(final String key, final String value, final boolean checked, final String oldKey)
    { return false; }

    @Override
    protected boolean onBeforeValueEdit(final String key, final String value, final boolean checked)
    { return false; }

    @Override
    protected boolean onAfterValueEdit(final String key, final String value, final boolean checked, final String oldValue)
    { return false; }

    @Override
    protected boolean onAddConfirmRow()
    { return false; }

    @Override
    protected boolean onRemoveConfirmRow(final int row, final String key, final String value, final boolean checked)
    { return false; }

} // class JReadOnlyKeyValueTable
