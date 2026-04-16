/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;

import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import jxm.*;


@SuppressWarnings("serial")
public abstract class JKeyValueTable extends JPanel {


    public static class State {
        public boolean checked;
        public String  value;

        public State(final boolean checked_, final String value_)
        {
            checked = checked_;
            value   = value_;
        }

        public State deepClone()
        { return new State(checked, value); }

    } // class State

    public static class StateHashMap extends HashMap<String, State> {

        public StateHashMap deepClone()
        {
            final StateHashMap copy = new StateHashMap();

            for( final Entry<String, State> entry : this.entrySet() ) copy.put( entry.getKey(), entry.getValue().deepClone() );

            return copy;
        }

    } // StateHashMap

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract boolean onCheckboxToggle(final String key, final String value, final boolean checked, final boolean newChecked);

    protected abstract boolean onBeforeKeyEdit(final String key, final String value, final boolean checked);
    protected abstract boolean onAfterKeyEdit(final String key, final String value, final boolean checked, final String oldKey);
    protected abstract boolean onDuplicateKey(final String key, final String value, final boolean checked, final String oldKey);

    protected abstract boolean onBeforeValueEdit(final String key, final String value, final boolean checked);
    protected abstract boolean onAfterValueEdit(final String key, final String value, final boolean checked, final String oldValue);

    protected abstract boolean onAddConfirmRow();
    protected abstract boolean onRemoveConfirmRow(final int row, final String key, final String value, final boolean checked);

    protected String normalizeKey(final String key)
    { return key; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static ImageIcon _newImageIcon(final String name)
    { return SysUtil.newImageIcon_defLoc("toolbar/tlb_kvt_" + name + ".gif"); }

    private   DefaultTableModel _model       = null;
    private   JTable            _table       = null;

    private   JButton           _btnChkAll   = null;
    private   JButton           _btnUnchkAll = null;
    private   JButton           _btnInvChk   = null;

    protected JButton           _btnAdd      = null;
    protected JButton           _btnRemove   = null;
    protected JButton           _btnRemoveM  = null;

    private   boolean           _initialized = false;
    private   int               _chkBoxSize  = 30;
    private   float             _keyRatio    = 0.2f;
    private   float             _valueRatio  = 1.0f - _keyRatio;

    private   int               _oldRow      = -1;
    private   int               _oldCol      = -1;
    private   String            _oldKey      = null;
    private   String            _oldValue    = null;

    private static class StringCellEditor extends DefaultCellEditor {

        private int _editedRow = -1;
        private int _editedCol = -1;

        public StringCellEditor()
        { super( new JTextField() ); }

        @Override
        public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column)
        {
            _editedRow = row;
            _editedCol = column;

            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        public int getEditedRow   () { return _editedRow; }
        public int getEditedColumn() { return _editedCol; }

    } // class StringCellEditor

    private void _setAllChecked(final boolean newState)
    {
        /*
        for( int row = 0; row < _model.getRowCount(); ++row ) {
            final Boolean checked = (Boolean) _model.getValueAt(row, 0);
            final String  key     = (String ) _model.getValueAt(row, 1);
            final String  value   = (String ) _model.getValueAt(row, 2);
            if( onCheckboxToggle(key, value, checked, newState) ) _model.setValueAt(newState, row, 0);
        }
        */

        for( int viewRow = 0; viewRow < _table.getRowCount(); ++viewRow ) {

            final int     modelRow = _table.convertRowIndexToModel(viewRow);

            final Boolean checked  = (Boolean) _model.getValueAt(modelRow, 0);
            final String  key      = (String ) _model.getValueAt(modelRow, 1);
            final String  value    = (String ) _model.getValueAt(modelRow, 2);

            if( onCheckboxToggle(key, value, checked, newState) ) _model.setValueAt(newState, modelRow, 0);

        } // for
    }

    private void _invertChecked()
    {
        /*
        for( int row = 0; row < _model.getRowCount(); ++row) {
            Boolean checked  = (Boolean) _model.getValueAt(row, 0);
            String  key      = (String ) _model.getValueAt(row, 1);
            String  value    = (String ) _model.getValueAt(row, 2);
            boolean newState = !checked;
            if( onCheckboxToggle(key, value, checked, newState) ) _model.setValueAt(newState, row, 0);
        }
        */

        for( int viewRow = 0; viewRow < _table.getRowCount(); ++viewRow ) {

            final int     modelRow = _table.convertRowIndexToModel(viewRow);

            final Boolean checked  = (Boolean) _model.getValueAt(modelRow, 0);
            final String  key      = (String ) _model.getValueAt(modelRow, 1);
            final String  value    = (String ) _model.getValueAt(modelRow, 2);
            final boolean newState = !checked;

            if( onCheckboxToggle(key, value, checked, newState) ) _model.setValueAt(newState, modelRow, 0);

        } // for
    }

    @SuppressWarnings("this-escape")
    public JKeyValueTable(final String title_, final String keyCaption, final String valueCaption)
    {
        setLayout( new BorderLayout() );

        // Initialize the title
        if(title_ != null) {
            final JLabel title = new JLabel(title_);
                title.setBorder( BorderFactory.createEmptyBorder(5, 5, 0, 5) );
            add(title, BorderLayout.NORTH);
        }

        // Initialize the model
        _model = new DefaultTableModel( new Object[] { "", keyCaption, valueCaption }, 0 ) {
            @Override
            public Class<?> getColumnClass(final int columnIndex)
            {
                if(columnIndex == 0) return Boolean.class;
                                     return String .class;
            }

            @Override
            public boolean isCellEditable(final int row, final int col)
            {
                final Boolean checked = (Boolean) getValueAt(row, 0);
                final String  key     = (String ) getValueAt(row, 1);
                final String  value   = (String ) getValueAt(row, 2);

                if(col == 0) return onCheckboxToggle (key, value, checked, !checked);
                if(col == 1) return onBeforeKeyEdit  (key, value, checked          );
                if(col == 2) return onBeforeValueEdit(key, value, checked          );
                             return false;
            }
        };

        // Initialize the table
        _table = new JTable(_model);
        _table.setFillsViewportHeight(true);

        _table.getTableHeader().setReorderingAllowed(false);

        _table.getColumnModel().getColumn(0).setMinWidth(_chkBoxSize);
        _table.getColumnModel().getColumn(0).setMaxWidth(_chkBoxSize);

        _table.setRowSelectionAllowed(true);
        _table.setColumnSelectionAllowed(true);
        _table.setCellSelectionEnabled(true);

        _table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        _table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

            // Listen for user column drags
            _table.getColumnModel().addColumnModelListener( new TableColumnModelListener() {
                @Override
                public void columnMarginChanged(final ChangeEvent e)
                {
                    if( !_initialized || !_table.isShowing() ) return;

                    final int totalWidth = _table.getWidth() - _chkBoxSize;
                    if(totalWidth <= 0) return;

                    final int keyWidth   = _table.getColumnModel().getColumn(1).getWidth();
                    final int valueWidth = _table.getColumnModel().getColumn(2).getWidth();
                    _keyRatio   = (float) keyWidth   / totalWidth;
                    _valueRatio = (float) valueWidth / totalWidth;
                }

                @Override public void columnAdded           (final TableColumnModelEvent e) {}
                @Override public void columnRemoved         (final TableColumnModelEvent e) {}
                @Override public void columnMoved           (final TableColumnModelEvent e) {}
                @Override public void columnSelectionChanged(final ListSelectionEvent    e) {}
            } );

            // Resize handler uses stored ratios
            final Runnable resizeColumns = () -> {
                final int totalWidth = _table.getWidth();
                if(totalWidth <= _chkBoxSize ) return;

                final int remaining = totalWidth - _chkBoxSize;
                _table.getColumnModel().getColumn(1).setPreferredWidth( (int) (remaining * _keyRatio  ) );
                _table.getColumnModel().getColumn(2).setPreferredWidth( (int) (remaining * _valueRatio) );
            };

            _table.addComponentListener( new ComponentAdapter() {
                @Override
                public void componentResized(final ComponentEvent e)
                {
                    if( !_initialized || !_table.isShowing() ) return;

                    resizeColumns.run();
                }
            } );

            // Ensure it is properly sized on display
            _table.addHierarchyListener( e -> {
                if( (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && _table.isShowing() ) {
                    resizeColumns.run();
                    _initialized = true;
                }
            } );
            /*
            _table.addComponentListener( new ComponentAdapter() {
                @Override
                public void componentResized(final ComponentEvent e)
                {
                    resizeColumns.run();
                    _initialized = true;
                }
            } );
            */

        final JScrollPane scrollPane = new JScrollPane(_table);
            scrollPane.setBorder( BorderFactory.createEmptyBorder(5, 5, 0, 5) );
        add(scrollPane, BorderLayout.CENTER);

        // Initialize the sorter
        final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(_model);

        sorter.setSortKeys( Arrays.asList(
            new RowSorter.SortKey(1, SortOrder.ASCENDING) // Sort by column 1 initially
        ) );

        sorter.sort();

        _table.setRowSorter(sorter);

        // Attach edit handlers
        _table.addPropertyChangeListener( "tableCellEditor", e -> {
            if( !_table.isEditing() ) return;

            final int viewRow = _table.getSelectedRow   ();
            final int viewCol = _table.getSelectedColumn();

            if(viewRow < 0 || viewCol < 0) return;

            _oldRow = _table.convertRowIndexToModel   (viewRow);
            _oldCol = _table.convertColumnIndexToModel(viewCol);

            if(_oldCol == 1) _oldKey   = (String) _model.getValueAt(_oldRow, 1);
            if(_oldCol == 2) _oldValue = (String) _model.getValueAt(_oldRow, 2);
        } );

        _table.setDefaultEditor(String.class, new StringCellEditor() );
        _table.getDefaultEditor(String.class).addCellEditorListener( new CellEditorListener() {
            @Override
            public void editingStopped(final ChangeEvent e)
            {
                final StringCellEditor editor  = (StringCellEditor) e.getSource();
                final int              viewRow = editor.getEditedRow   ();
                final int              viewCol = editor.getEditedColumn();

                if(viewRow < 0 || viewCol < 0) return;

                final int modelRow = _table.convertRowIndexToModel   (viewRow);
                final int modelCol = _table.convertColumnIndexToModel(viewCol);

                if( modelRow < 0 || (modelCol != 1 && modelCol != 2) ) return;

                final Boolean checked = (Boolean) _model.getValueAt(modelRow, 0);
                final String  key     = (String ) _model.getValueAt(modelRow, 1);
                final String  value   = (String ) _model.getValueAt(modelRow, 2);

                if(modelCol == 1) {
                    final String  newKey         = normalizeKey(key);
                          boolean duplicateFound = false;
                          int     duplicateRow   = -1;
                    for( int i = 0; i < _model.getRowCount(); ++i ) {
                        if( i != modelRow && newKey.equals( _model.getValueAt(i, 1) ) ) {
                            duplicateFound = true;
                            duplicateRow   = i;
                            break;
                        }
                    }
                    if(duplicateFound) {
                        if( onDuplicateKey(newKey, value, checked, _oldKey) ) {
                            _model.setValueAt(value  , duplicateRow, 2);
                            _model.setValueAt(checked, duplicateRow, 0);
                            _model.removeRow(modelRow);
                        }
                        else {
                            _model.setValueAt(_oldKey, modelRow, 1);
                        }
                    }
                    else {
                        if( !onAfterKeyEdit(newKey, value, checked, _oldKey) ) _model.setValueAt(_oldKey, modelRow, 1);
                    }
                }

                if(modelCol == 2) {
                    if( !onAfterValueEdit(key, value, checked, _oldValue) ) _model.setValueAt(_oldValue, modelRow, 2);
                }
            }

            @Override
            public void editingCanceled(final ChangeEvent e)
            { /* No operation */ }
        } );

        // Initialize the buttons
        _btnChkAll = new JButton();
            _btnChkAll.setIcon( _newImageIcon("chk_all") );
            _btnChkAll.setToolTipText(Texts.KVT_CheckAll);
            _btnChkAll.addActionListener( e -> { _setAllChecked(true); } );

        _btnUnchkAll = new JButton();
            _btnUnchkAll.setIcon( _newImageIcon("unchk_all") );
            _btnUnchkAll.setToolTipText(Texts.KVT_UncheckAll);
            _btnUnchkAll.addActionListener( e -> { _setAllChecked(false); } );

        _btnInvChk = new JButton();
            _btnInvChk.setIcon( _newImageIcon("inv_chk") );
            _btnInvChk.setToolTipText(Texts.KVT_InvertChecked);
            _btnInvChk.addActionListener( e -> { _invertChecked(); } );

        _btnAdd = new JButton();
            _btnAdd.setIcon( _newImageIcon("add") );
            _btnAdd.setToolTipText(Texts.KVT_AddRow);
            _btnAdd.addActionListener( e -> {

                if( !onAddConfirmRow() ) return;

                final String  base    = "NEW_KEY";
                      int     counter = 0;
                      String  candidate;
                      boolean exists;

                do {

                    candidate = base + "_" + counter;
                    exists    = false;

                    for( int i = 0; i < _model.getRowCount(); ++i ) {
                        if( candidate.equals( _model.getValueAt(i, 1) ) ) {
                            exists = true;
                            break;
                        }
                    }

                    ++counter;

                } while(exists);

                _model.addRow( new Object[] { true, candidate, "" } );

                final int newModelRow = _model.getRowCount() - 1;
                final int newViewRow  = _table.convertRowIndexToView   (newModelRow);
                final int newViewCol  = _table.convertColumnIndexToView(1          );
                _table.changeSelection(newViewRow, newViewCol, false, false);

                if( _table.editCellAt(newViewRow, newViewCol) ) {
                    final Component editor = _table.getEditorComponent();
                    if(editor != null) editor.requestFocusInWindow();
                }

            } ); // addActionListener

        _btnRemove = new JButton();
            _btnRemove.setIcon( _newImageIcon("remove") );
            _btnRemove.setToolTipText(Texts.KVT_DelRow);
            _btnRemove.addActionListener( e -> {

                final ListSelectionModel rowSel  = _table.getSelectionModel();
                final int                oldLead = rowSel.getLeadSelectionIndex();
                if(oldLead < 0) return;
                final int                oldMin  = rowSel.getMinSelectionIndex();
                final int                oldMax  = rowSel.getMaxSelectionIndex();

                final ListSelectionModel colSel  = _table.getColumnModel().getSelectionModel();
                int selCol_ = colSel.getLeadSelectionIndex();
                if(selCol_ < 0) {
                    selCol_ = _table.getSelectedColumn();
                    if(selCol_ < 0) selCol_ = 0;
                }

                final int     modelRow = _table.convertRowIndexToModel   (oldLead);
                final int     modelCol = _table.convertColumnIndexToModel(selCol_);
                final Boolean checked  = (Boolean) _model.getValueAt(modelRow, 0);
                final String  key      = (String ) _model.getValueAt(modelRow, 1);
                final String  value    = (String ) _model.getValueAt(modelRow, 2);

                if( !onRemoveConfirmRow(modelRow, key, value, checked) ) return;

                _model.removeRow(modelRow);

                SwingUtilities.invokeLater( () -> {

                    final int newMin = rowSel.getMinSelectionIndex();
                    final int newMax = rowSel.getMaxSelectionIndex();
                    if(newMin < 0 || newMax < 0) return; // Nothing left selected

                    final boolean leadWasBottom = (oldLead == oldMax);
                    final int     newLead       = leadWasBottom ? newMax : newMin;

                    rowSel.setAnchorSelectionIndex(leadWasBottom ? newMin : newMax);
                    rowSel.setLeadSelectionIndex(newLead);

                    _table.changeSelection(newLead, modelCol, false, true);
                    _table.requestFocusInWindow();

                } );

            } ); // addActionListener

        _btnRemoveM = new JButton();
            _btnRemoveM.setIcon( _newImageIcon("remove_multiple") );
            _btnRemoveM.setToolTipText(Texts.KVT_DelRows);
            _btnRemoveM.addActionListener( e -> {
                final int[] selected = _table.getSelectedRows();

                if(selected.length == 0) return;

                for(int i = selected.length - 1; i >= 0; --i) {
                    final int modelRow = _table.convertRowIndexToModel( selected[i] );

                    Boolean checked = (Boolean) _model.getValueAt(modelRow, 0);
                    String  key     = (String ) _model.getValueAt(modelRow, 1);
                    String  value   = (String ) _model.getValueAt(modelRow, 2);

                    if( onRemoveConfirmRow(modelRow, key, value, checked) ) _model.removeRow(modelRow);

                } // for

            } );

        // Initialize the filter
        final JLabel     lblFiter  = new JLabel( String.format(Texts.KVT_FilterKey, keyCaption) );
        final JTextField txtFilter = new JTextField(20);
        txtFilter.getDocument().addDocumentListener( new DocumentListener() {
            private void _updateFilter()
            {
                final String text = txtFilter.getText();

                if( text.trim().length() == 0 ) {
                    // Show all
                    sorter.setRowFilter(null);
                }
                else {
                    // Column 1 -> key column
                    sorter.setRowFilter( RowFilter.regexFilter("(?i)" + text, 1) );
                }
            }
            @Override public void insertUpdate (final DocumentEvent e) { _updateFilter(); }
            @Override public void removeUpdate (final DocumentEvent e) { _updateFilter(); }
            @Override public void changedUpdate(final DocumentEvent e) { _updateFilter(); }
        } );

        // Add the buttons
        final JPanel naviPanel = new JPanel();

            naviPanel.setLayout( new BoxLayout(naviPanel, BoxLayout.X_AXIS) );
            naviPanel.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5) );

            naviPanel.add( _btnChkAll                                 ); naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) );
            naviPanel.add( _btnUnchkAll                               ); naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) );
            naviPanel.add( _btnInvChk                                 ); naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) );
            naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) ); naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) );

            naviPanel.add( _btnAdd                                    ); naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) );
            naviPanel.add( _btnRemove                                 ); naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) );
            naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) ); naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) );
            naviPanel.add( _btnRemoveM                                ); naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) );
            naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) ); naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) );

            naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) ); naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) );
            naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) ); naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) );
            naviPanel.add( Box.createHorizontalGlue()                 );

            naviPanel.add( lblFiter                                   ); naviPanel.add( Box.createRigidArea( new Dimension(5, 0) ) );
            naviPanel.add( txtFilter                                  );

        add(naviPanel, BorderLayout.SOUTH);
    }

    public void clearAll()
    { _model.setRowCount(0); }

    public void refresh()
    {
        if(_table == null) return;

        _table.revalidate();
        _table.repaint();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _rowExists(final String key)
    {
        for( int i = 0; i < _model.getRowCount(); ++i ) {
            final Object existingKey = _model.getValueAt(i, 1);
            if( key.equals(existingKey) ) return true;
        }

        return false;
    }

    public <T> void initializeFromMap(final Map<String, T> data)
    {
        clearAll();

        for( final Map.Entry<String, T> entry : data.entrySet() ) {

            final String normalizedKey = normalizeKey( entry.getKey() );

            if( _rowExists(normalizedKey) ) continue;

            if( entry.getValue() instanceof String ) {
                // Map<String, String>
                _model.addRow( new Object[] { true, normalizedKey, entry.getValue() } );
            }
            else if( entry.getValue() instanceof State ) {
                // Map<String, State>
                final State state = (State) entry.getValue();
                _model.addRow( new Object[]{ state.checked, normalizedKey, state.value } );
            }
            else {
                throw new IllegalArgumentException( entry.getValue().getClass().toString() );
            }

        } // for
    }

    public StateHashMap getAllData()
    {
        final StateHashMap result = new StateHashMap();

        for( int i = 0; i < _model.getRowCount(); ++i ) {

            final Boolean checked = (Boolean) _model.getValueAt(i, 0);
            final String  key     = (String ) _model.getValueAt(i, 1);
            final String  value   = (String ) _model.getValueAt(i, 2);

            result.put( key, new State(checked, value) );

        } // for

        return result;
    }

    public Map<String, String> getCheckedData()
    {
        final Map<String, String> result = new HashMap<>();

        for( int i = 0; i < _model.getRowCount(); ++i ) {

            final Boolean checked = (Boolean) _model.getValueAt(i, 0);
            if(!checked) continue;

            final String key   = (String) _model.getValueAt(i, 1);
            final String value = (String) _model.getValueAt(i, 2);

            result.put(key, value);

        } // for

        return result;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static StateHashMap convertMap(final Map<String, String> data)
    {
        final StateHashMap res = new StateHashMap();

        for( final Map.Entry<String, String> entry : data.entrySet() ) res.put( entry.getKey(), new State( true, entry.getValue() ) );

        return res;
    }

} // class JKeyValueTable
