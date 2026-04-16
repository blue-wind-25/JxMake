/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp;


import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import jxm.*;
import jxm.xb.*;

import static jxm.tool.CommandSpecifier.*;


@SuppressWarnings("serial")
public class JCommandComposerPanel extends JPanel {

    private static final int MAX_REPEAT =   8; // Maximum repeat slots for each repeatable option
    private static final int MAX_N      = 128; // Maximum total options

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // GUI-related variables
    private final JComboBox<String>   _commandBox;
    private       JComboBox<String>[] _optionBoxes = null;
    private       JTextField[]        _valueFields = null;
    private       JComboBox<String>[] _comboFields;

    private final JPanel              _columnsPanel;
    private final JScrollPane         _scrollPane;
    private final JTextArea           _previewArea;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private CommandRegistry _cmdRegistry = null;

    private void _addToolTipVisibilityWorkaround(final JComboBox<?> comboBox)
    {
        comboBox.addPopupMenuListener( new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e)
            {
                ToolTipManager.sharedInstance().mouseExited(
                    new MouseEvent( comboBox, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, 0, 0, 0, false )
                );
            }

            @Override public void popupMenuWillBecomeVisible(final PopupMenuEvent e)
            {}

            @Override public void popupMenuCanceled(final PopupMenuEvent e)
            {}

        } );
    }

    @SuppressWarnings("this-escape")
    public JCommandComposerPanel(final CommandRegistry cmdRegistry)
    {
        setCommandRegistry(cmdRegistry);

        setLayout( new BorderLayout() );

        // Command row
        final JPanel cmdRow = new JPanel( new BorderLayout() );

            cmdRow.setBorder( BorderFactory.createEmptyBorder(0, 0, 5, 0) );

            _commandBox = new JComboBox<>();
            if(_cmdRegistry != null) for( final String cmdName : _cmdRegistry.sortedKeySet() ) _commandBox.addItem(cmdName);
            _commandBox.addActionListener( e -> {
                // Rebuild when command changes
                _rebuildColumns();
                _modified = true;
            } );
            _commandBox.setRenderer(_cmdRenderer);
            _addToolTipVisibilityWorkaround(_commandBox);

        cmdRow.add( new JLabel(Texts.ScrEdt_CCCommand), BorderLayout.WEST   );
        cmdRow.add( _commandBox                       , BorderLayout.CENTER );

        add(cmdRow, BorderLayout.NORTH);

        // Options area
        _columnsPanel = new JPanel();
            _columnsPanel.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5) );

        _scrollPane = new JScrollPane(_columnsPanel);
            /*
            _scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            _scrollPane.setVerticalScrollBarPolicy  (JScrollPane.VERTICAL_SCROLLBAR_ALWAYS  );
            //*/
        add(_scrollPane, BorderLayout.CENTER);

        // Live preview
        final JPanel previewPanel = new JPanel( new BorderLayout() );

            previewPanel.setBorder( BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 0, 0, 0),
                BorderFactory.createTitledBorder(Texts.ScrEdt_CCPreview)
            ) );

            _previewArea = new JTextArea(5, 0);
                _previewArea.setBorder( BorderFactory.createEmptyBorder(3, 3, 3, 3) );
                _previewArea.setLineWrap(true);
                _previewArea.setWrapStyleWord(true);
                _previewArea.setEditable(false);
                _previewArea.setFont( _previewArea.getFont().deriveFont(Font.ITALIC) );

            final JScrollPane scrollPane = new JScrollPane(_previewArea);
                scrollPane.setBorder( BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(5, 5, 5, 5),
                    scrollPane.getBorder()
                ) );
            previewPanel.add(scrollPane, BorderLayout.CENTER);

        add(previewPanel, BorderLayout.SOUTH);

        // Rebuild when resized
        final Timer resizeTimer = new Timer( 200, e -> _rebuildColumns() );
        resizeTimer.setRepeats(false);

        addComponentListener( new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e)
            {
                if( resizeTimer.isRunning() ) resizeTimer.restart();
                else                          resizeTimer.start  ();
            }
        } );

        // Initial layout
        addHierarchyListener( e -> {
            if( (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && this.isShowing() ) {
                _rebuildColumns();
            }
        } );
    }

    public void setCommandRegistry(final CommandRegistry cmdRegistry)
    {
        if(_cmdRegistry != null) _commandBox.removeAllItems();

        _cmdRegistry = cmdRegistry;

        if(_cmdRegistry != null) for( final String cmdName : _cmdRegistry.sortedKeySet() ) _commandBox.addItem(cmdName);

        _modified = false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public CommandConfig getCommandConfig(final String cmdName)
    { return (_cmdRegistry != null) ? _cmdRegistry.getCommand(cmdName) : null; }

    public synchronized String[] getCommandArray()
    {
        final ArrayList<String> result = new ArrayList<>();

        result.add( (String) _commandBox.getSelectedItem() );

        final CommandConfig cfg = getCommandConfig( (String) _commandBox.getSelectedItem() );

        for( int i = 0; i < _optionBoxes.length; ++i ) {

            /*
            if(_optionBoxes[i] == null) continue;
            //*/

            final String opt = (String) _optionBoxes[i].getSelectedItem();
            if( opt == null || opt.equals(Text_Option_NA) ) continue;

            final OptionConfig oc = cfg.getOption(opt);
            if( oc != null && oc.requiresValue ) {

                if(oc.possibleValues != null && _comboFields[i] == null) continue;

                String val;
                if(oc.possibleValues == null) {
                    val = _valueFields[i].getText();
                }
                else {
                    val = (String) _comboFields[i].getSelectedItem();
                    if(val == null) val = "";
                }
                val = XCom.re_quoteSQIfContainsWhitespace(val);

                switch(oc.style) {

                    case VALUE:
                        result.add(val);
                        break;

                    case SPACE:
                        result.add(opt);
                        result.add(val);
                        break;

                    case EQUALS:
                        result.add(opt + "=" + val);
                        break;

                } // switch

            }
            else {
                result.add(opt);
            }

        } // for

        _modified = false;

        return result.toArray( new String[0] );
    }

    public boolean isModified()
    { return _modified; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final StringBuilder _sbPreview = new StringBuilder();
    private       boolean       _modified  = false;

    private synchronized void _updatePreview()
    {
        if( _cmdRegistry == null || _cmdRegistry.isEmpty() ) return;

        _sbPreview.setLength(0);

        final boolean saveModified = _modified;
        for( String s : getCommandArray() ) _sbPreview.append(s).append(" ");
        _modified = saveModified;

        _previewArea.setText( _sbPreview.toString().trim() );
    }

    private synchronized void _updateFieldState(final JPanel panel, final CommandConfig cfg, final int idx)
    {
        final CardLayout cl  = (CardLayout) panel.getLayout();
        final String     opt = (String) _optionBoxes[idx].getSelectedItem();

        if( opt == null || opt.equals(Text_Option_NA) ) {
            _valueFields[idx].setText("");
            _valueFields[idx].setEnabled(false);
            _comboFields[idx].setEnabled(false);
            cl.show(panel, "TF");
        }
        else {
            final OptionConfig oc = cfg.getOption(opt);
            final boolean      en = oc != null && oc.requiresValue;
            _valueFields[idx].setEnabled(en);
            _comboFields[idx].setEnabled(en);
            if(oc.possibleValues == null) {
                cl.show(panel, "TF");
            }
            else {
                // Remove the listener(s)
                final ActionListener[] listeners = _comboFields[idx].getActionListeners();
                for(final ActionListener l : listeners) _comboFields[idx].removeActionListener(l);
                // Modify the contents
                _comboFields[idx].putClientProperty("oc", oc);
                _comboFields[idx].setModel( new DefaultComboBoxModel<>(oc.possibleValues) );
              //_comboFields[idx].setSelectedIndex(0);
                cl.show(panel, "CB");
                // Add the listener(s) back
                for(final ActionListener l : listeners) _comboFields[idx].addActionListener(l);
            }
        }

        _updatePreview();
    }

    private synchronized int _computeRowCount(final CommandConfig cfg)
    {
        int count = 0;

        final Set<String> keys = cfg.keySet();
        final int         incf = Math.round( MAX_REPEAT * ( 3.0f / Math.min( 3, keys.size() ) ) );

        for(final String optName : keys) {

            final OptionConfig oc = cfg.getOption(optName);

            if(oc.repeatable) count += incf;
            else              count += 1;

        } // for

        return Math.min(count, MAX_N);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int CRMode_Cmd = 0;
    private static final int CRMode_Opt = 1;
    private static final int CRMode_Val = 2;

    private class ToolTipListCellRenderer implements ListCellRenderer<String> {

        private final JComboBox<String> _cmbOptBox;
        private final int               _mode;

        public ToolTipListCellRenderer(final JComboBox<String> forOptionBox, final int mode)
        {
            _cmbOptBox = forOptionBox;
            _mode      = mode;
        }

        public ToolTipListCellRenderer(final int mode)
        { this(null, mode); }

        public ToolTipListCellRenderer forOptionBox(final JComboBox<String> forOptionBox)
        {
            return (_mode == CRMode_Val) ? new ToolTipListCellRenderer(forOptionBox, CRMode_Val)
                                         : this;
        }

        @Override
        public Component getListCellRendererComponent(
            final JList<? extends String> list,
            final String                  value,
            final int                     index,
            final boolean                 isSelected,
            final boolean                 cellHasFocus
        ) {
            String ttStr = null;

            if(_mode == CRMode_Cmd) {
                final CommandConfig cmdConf = getCommandConfig(value);
                ttStr = (cmdConf != null) ? cmdConf.getDescriptionHTML() : null;
            }
            else if(_mode == CRMode_Opt) {
                final String        cmdName = (String) _commandBox.getSelectedItem();
                final CommandConfig cmdConf = (cmdName != null) ? getCommandConfig (cmdName) : null;
                final OptionConfig  optConf = (cmdConf != null) ? cmdConf.getOption(value  ) : null;
                ttStr = (optConf != null) ? optConf.getDescriptionHTML(-1) : null;
                if( ttStr == null && Text_Option_NA.equals(value) ) {
                    ttStr = (cmdConf != null) ? cmdConf.optNADescription : null;
                }
            }
            else { // CRMode_Val
                final String        cmdName = (String) _commandBox.getSelectedItem();
                final String        optName = (String) _cmbOptBox .getSelectedItem();
                final CommandConfig cmdConf = (cmdName != null) ? getCommandConfig (cmdName) : null;
                final OptionConfig  optConf = (cmdConf != null) ? cmdConf.getOption(optName) : null;
                ttStr = (optConf != null) ? optConf.getDescriptionHTML(index) : null;
            }

            final JLabel label = new JLabel(value);

            if(ttStr != null) label.setToolTipText(ttStr);

            if(isSelected) {
                label.setBackground( list.getSelectionBackground() );
                label.setForeground( list.getSelectionForeground() );
                label.setOpaque(true);
            }

            return label;
        }

    } // class ToolTipListCellRenderer

    private final ToolTipListCellRenderer _cmdRenderer = new ToolTipListCellRenderer(CRMode_Cmd);
    private final ToolTipListCellRenderer _optRenderer = new ToolTipListCellRenderer(CRMode_Opt);
    private final ToolTipListCellRenderer _valRenderer = new ToolTipListCellRenderer(CRMode_Val);

    private final DocumentListener _previewUpdater = new DocumentListener() {
        @Override public void insertUpdate (final DocumentEvent e) { _updatePreview(); _modified = true; }
        @Override public void removeUpdate (final DocumentEvent e) { _updatePreview(); _modified = true; }
        @Override public void changedUpdate(final DocumentEvent e) { _updatePreview(); _modified = true; }
    };

    @SuppressWarnings( { "rawtypes", "unchecked" } )
    private synchronized void _rebuildColumns_impl()
    {
        if( _cmdRegistry == null || _cmdRegistry.isEmpty() ) return;

        _columnsPanel.removeAll();

        final String        selCmd = (String) _commandBox.getSelectedItem();
        final CommandConfig cfg    = getCommandConfig(selCmd);
        if(cfg == null) return;

        final int N = _computeRowCount(cfg);

        _optionBoxes = new JComboBox [N];
        _valueFields = new JTextField[N];
        _comboFields = new JComboBox [N];

        if(N <= 0) {
            final JLabel msg = new JLabel(Texts.ScrEdt_CCNoArg, SwingConstants.CENTER);
            _columnsPanel.add(msg);
            _columnsPanel.revalidate();
            _columnsPanel.repaint();
            _updatePreview();
            return;
        }

        int availableHeight = _scrollPane.getViewport().getHeight();
        if(availableHeight <= 0) availableHeight = getHeight();

        // Measure height
        final JTextField probeField = new JTextField();
        final JComboBox<?> probeBox = new JComboBox<>();

        probeField.setFont( _columnsPanel.getFont() );
        probeBox.setFont  ( _columnsPanel.getFont() );

        final int fieldHeight = probeField.getPreferredSize().height;
        final int boxHeight   = probeBox.getPreferredSize  ().height;
        final int rowHeight   = Math.max(fieldHeight, boxHeight);

        final int rowsPerCol = Math.max(1, availableHeight / rowHeight);
        final int colCount   = (int) Math.ceil( (float) N / rowsPerCol );

        _columnsPanel.setLayout( new GridLayout(1, colCount, 10, 10) );

        // Measure width
        final JLabel    probe   = new JLabel("#999: ");
        final int       width   = probe.getPreferredSize().width;
        final Dimension uniform = new Dimension( width, probe.getPreferredSize().height );

        // Create the controls
        final ArrayList<Runnable> deferredTrigger = new ArrayList<>();

        int index = 0;

        for(int col = 0; col < colCount; ++col) {

            final JPanel colPanel = new JPanel();
            colPanel.setLayout( new BoxLayout(colPanel, BoxLayout.Y_AXIS) );

            colPanel.setBorder( BorderFactory.createTitledBorder(Texts.ScrEdt_CCOptions) );

            final int end = Math.min(index + rowsPerCol, N);

            for(; index < end; ++index) {

                final int                idx = index;
                final JPanel             row = new JPanel( new GridBagLayout() );
                final GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 2, 2, 2);

                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.setAlignmentY(Component.TOP_ALIGNMENT );
                row.setMaximumSize( new Dimension( Integer.MAX_VALUE, row.getPreferredSize().height ) );

                final JPanel valuePanel = new JPanel( new CardLayout() );

                _optionBoxes[index] = new JComboBox<>();
                if(!cfg.optAlwaysMandatory) _optionBoxes[index].addItem(Text_Option_NA);
                for( final String optName : cfg.keySet() ) _optionBoxes[index].addItem(optName);
              //_optionBoxes[index].setSelectedIndex(0);
                _optionBoxes[index].setMaximumSize( new Dimension( Integer.MAX_VALUE, _optionBoxes[index].getPreferredSize().height ) );
                _optionBoxes[index].addActionListener( e -> {
                    _updateFieldState(valuePanel, cfg, idx);
                    _modified = true;
                } );
                _optionBoxes[index].setRenderer(_optRenderer);
                _addToolTipVisibilityWorkaround( _optionBoxes[index] );

                _valueFields[index] = new JTextField();
                _valueFields[index].setEnabled(false);
                if(true) {
                    final Dimension minSize = new Dimension( 250, _valueFields[index].getPreferredSize().height );
                    _valueFields[index].setMinimumSize(minSize);
                    _valueFields[index].setPreferredSize(minSize);
                    _valueFields[index].setMaximumSize( new Dimension( Integer.MAX_VALUE, _valueFields[index].getPreferredSize().height ) );
                }
                _valueFields[index].getDocument().addDocumentListener(_previewUpdater);
                valuePanel.add(_valueFields[index], "TF");

                _comboFields[index] = new JComboBox<>();
                _comboFields[index].setEnabled(false);
                if(true) {
                    final Dimension minSize = new Dimension( 250, _comboFields[index].getPreferredSize().height );
                    _comboFields[index].setMinimumSize(minSize);
                    _comboFields[index].setPreferredSize(minSize);
                    _comboFields[index].setMaximumSize( new Dimension( Integer.MAX_VALUE, _comboFields[index].getPreferredSize().height ) );
                }
                _comboFields[index].addActionListener( e -> {
                    _updatePreview();
                    _modified = true;
                } );
                _comboFields[index].setRenderer( _valRenderer.forOptionBox( _optionBoxes[index] ) );
                _addToolTipVisibilityWorkaround( _comboFields[index] );
                valuePanel.add(_comboFields[index], "CB");

                final JLabel lblIndex = new JLabel( String.format("#%03d: ", index + 1) );
                    lblIndex.setPreferredSize(uniform);
                gbc.gridy   = index;
                gbc.gridx   = 0;
                gbc.weighty = 0.0;
                gbc.weightx = 0.0;
                gbc.anchor  = GridBagConstraints.WEST;
                gbc.fill    = GridBagConstraints.NONE;
                row.add(lblIndex, gbc);

                gbc.gridy   = index;
                gbc.gridx   = 1;
                gbc.weighty = 0.0;
                gbc.weightx = 0.0;
                gbc.anchor  = GridBagConstraints.WEST;
                gbc.fill    = GridBagConstraints.NONE;
                row.add(_optionBoxes[index], gbc);

                gbc.gridy   = index;
                gbc.gridx   = 2;
                gbc.weighty = 0.0;
                gbc.weightx = 1.0;
                gbc.anchor  = GridBagConstraints.WEST;
                gbc.fill    = GridBagConstraints.HORIZONTAL;
                row.add(valuePanel, gbc);

                colPanel.add(row);

                // After initialization, register the deferred initial trigger as needed
                if(cfg.optAlwaysMandatory) {
                    deferredTrigger.add( () -> {
                        try {
                            _updateFieldState(valuePanel, cfg, idx);
                            _modified = true;
                        }
                        catch(final Exception e) {
                            // Print the stack trace if requested
                            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                        }
                    } );
                }

            } // for

            colPanel.add( Box.createVerticalGlue() );

            _columnsPanel.add(colPanel);

        } // for

        // Preselect some options as needed
        if( !cfg.preselectOptions.isEmpty() ) {
            final int ofs = cfg.optAlwaysMandatory ? 0 : 1;
            for( int i = 0; i < cfg.preselectOptions.size(); ++i ) {
                _optionBoxes[i].setSelectedIndex( ofs + cfg.preselectOptions.get(i) );
            }
        }

        // Run the deferred initial triggers
        for(final Runnable r : deferredTrigger) r.run();

        // Preselect some values
        if( !cfg.preselectValues.isEmpty() ) {
            for( int i = 0; i < cfg.preselectValues.size(); ++i ) {

                final OptionConfig oc    = cfg.getOption( (String) _optionBoxes[i].getSelectedItem() );
                final boolean      combo = (oc.possibleValues != null) && (oc.possibleValues.length != 0);

                if(combo) _comboFields[i].setSelectedIndex( Integer.parseInt( cfg.preselectValues.get(i) ) );
                else      _valueFields[i].setText         (                   cfg.preselectValues.get(i)   );

            } // for
        }

        _columnsPanel.revalidate();
        _columnsPanel.repaint();

        _updatePreview();
    }

    private synchronized void _rebuildColumns()
    {
        final Component glassPane = ( (JFrame) SwingUtilities.getWindowAncestor(this) ).getRootPane().getGlassPane();
        glassPane.setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) );
        glassPane.setVisible(true);
        glassPane.addKeyListener( new KeyAdapter() {} );
        glassPane.addMouseListener( new MouseAdapter() {} );

        final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception
            {
                _rebuildColumns_impl();
                return null;
            }

            @Override
            protected void done()
            {
                glassPane.setCursor( Cursor.getDefaultCursor() );
                glassPane.setVisible(false);
            }
        };

        worker.execute();
      //SwingUtilities.invokeLater(worker::execute);
    }

} // class JCommandComposerPanel
