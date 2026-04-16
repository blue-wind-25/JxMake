/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import javax.swing.plaf.basic.BasicComboPopup;

import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import jxm.*;
import jxm.gcomp.*;
import jxm.xb.*;


/*
 * WARNING : This class does not fully support all the configuration fields that may be found in various 'boards.txt'
 *           files for various platform packages!
 */
public class ArduinoBoardsTxt extends ArduinoBoardsTxt_Dec {

    private static boolean _keyPartsEnd(final XCom.IntegerRef altLPIdx, final ArrayList<String> parts, final Object... refs)
    {
        // Clear the index first
        altLPIdx.set(-1);

        // Check if there are fewer parts than the references
        if( parts.size() < refs.length ) return false;

        // Get the delta
        final int d = parts.size() - refs.length;

        // Compare with the references
        for(int i = 0; i < refs.length; ++i) {

            // Get the reference object
            final Object ref = refs[i];

            // Check the reference object as a string?
            if(ref instanceof String) {
                if( !parts.get(i + d).equals( (String) ref ) ) return false;
            }

            // Check the reference object as a XKV_Handler list?
            else if(ref instanceof XKV_Handler_List) {
                // A string list can only appear as the last part
                if(i != refs.length - 1) {
                    XCom.newJXMFatalLogicError("_keyPartsEnd(): 'ref' type 'XKV_Handler_List' can only appear as the last part").printStackTrace();
                    SysUtil.systemExitError();
                }
                // Compare the list members
                final XKV_Handler_List list = (XKV_Handler_List) ref;
                for(int j = 0; j < list.list.size(); ++j) {
                    if( parts.get(i + d).equals( list.list.get(j).part ) ) {
                        altLPIdx.set(j);
                        break;
                    }
                }
                if( altLPIdx.get() == -1 ) return false;
            }

            // Invalid reference object type
            else {
                XCom.newJXMFatalLogicError("_keyPartsEnd(): unsupported 'ref' type '" + ref.getClass() + "'").printStackTrace();
                SysUtil.systemExitError();
            }

        } // for

        // Clear the parts that have been matched with the references
        parts.subList( d, parts.size() ).clear();

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static BoardDefList parseFromFile(final String path) throws IOException
    {
        // Load the default rule as needed
        loadDecRule();

        // Open the file
        final BufferedReader bfr = new BufferedReader(
                                           new InputStreamReader( new FileInputStream( SysUtil.resolveAbsolutePath(path) ), SysUtil._CharEncoding )
                                       );

        // Prepare the processing variables
        final MenuIDTextMap menuIDTextMap = new MenuIDTextMap();
        final BoardDefList  boardDefList  = new BoardDefList ();
              BoardDef      boardDef      = null;

        // Process until EOF
        while(true) {

            // Read one line and check for EOF
            final String line = _readLine(bfr);
            if(line == null) break;

            // Extract the parts
            final ArrayList<String> parts   = XCom.extract_kv( line               , "=" );
            final ArrayList<String> key     = XCom.explode   ( parts.get(0).trim(), "." );
            final int               keySize = key.size();
            final String            value   = ( parts.size() > 1 ) ? parts.get(1).trim() : "";

            // Get the menu texts from 'menu.*=*'
            if( key.get(0).equals("menu") ) {
                menuIDTextMap.put( key.get(1), value );
            }

            // Get the board names from '*.name=*'
            else if( key.get(1).equals("name") ) {
                boardDef = new BoardDef( key.get(0), value );
                boardDefList.add(boardDef);
            }

            // Get the menu definitions from '*'.menu.*...=*
            else if( key.get(1).equals("menu") ) {
                // Get the board definition object
                boardDef = boardDefList.getBoardDefByID( key.get(0) );
                // Remove the '*.menu' parts
                key.subList(0, 2).clear();
                // Perform key transformation
                _execDir_transformKey(key);
                // Process the remaining parts of the key using handlers
                final XCom.IntegerRef xjvLPIdx        = new XCom.IntegerRef();
                      boolean         xjvItemsHandled = false;
                for(final XKV_PartHandler partHandlers : _xkvPartHandlersList) {
                    if( _keyPartsEnd( xjvLPIdx, key, partHandlers.part, partHandlers.xkvHandlers ) ) {
                        final XKV_Consumer_List handlers = partHandlers.xkvHandlers.list.get( xjvLPIdx.get() ).handlers;
                        for(int j = 0; j < handlers.list.size(); ++j) {
                            if(boardDef == null) throw XCom.newIOException( Texts.EMsg_UnresolvedABoardsTxtID, key.get(0) );
                            handlers.list.get(j).apply(boardDef, key, value);
                        }
                        xjvItemsHandled = true;
                        break;
                    }
                }
                // If the parts were not handled, add the parts as they are
                if(!xjvItemsHandled) {
                    if(boardDef == null) throw XCom.newIOException( Texts.EMsg_UnresolvedABoardsTxtID, key.get(0) );
                    boardDef.addMenuItem(menuIDTextMap, key, value);
                }
            }

            // Get the non-menu definitions from '*.build.*=*' and '*.upload.*=*'
            else {
                // Get the board definition object
                boardDef = boardDefList.getBoardDefByID( key.get(0) );
                // Process the remaining parts of the key using handlers
                for(final XKV_PartHandler mpartHandler : _xkvPartHandlersList) {
                    if( !mpartHandler.part.equals( key.get(1) ) ) continue;
                    for(final XKV_Handler handler : mpartHandler.xkvHandlers.list) {
                        if( !handler.part.equals( key.get(2) ) ) continue;
                        for(final XKV_Consumer cons : handler.handlers.list) {
                            if(boardDef == null) throw XCom.newIOException( Texts.EMsg_UnresolvedABoardsTxtID, key.get(0) );
                            cons.apply(boardDef, null, value); // Non-menu -> the 'key' is null
                        }
                        break;
                    }
                    break;
                }
            }

        } // while true

        // Return the finalized board definition list
        return boardDefList.finalizeAll();
    }

    public static BoardDefList fromEData(final String str) throws IOException, ClassNotFoundException
    { return BoardDefList.fromEData(str); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static XCom.Mutex _ConfigureBoardMutex = new XCom.Mutex();

    private static JFrame     _mainWindow          = null;
    private static JPanel     _pnlBoardConfig      = null;
    private static boolean    _acceptResult        = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class BoundsPopupMenuListener implements PopupMenuListener {
        /*
         * The code in this subclass is developed based on the code from:
         *     https://github.com/openTCS/opentcs/blob/master/openTCS-Common/src/main/java/org/opentcs/util/gui/BoundsPopupMenuListener.java
         *         Copyright (C) The openTCS Authors.
         *         This program is free software and subject to the MIT license.
         *         For details, see the licensing information (https://github.com/openTCS/opentcs/blob/master/LICENSE.txt).
         * ~~~ Last accessed on 2023-03-12 ~~~
         */

        private JScrollPane _scrollPane;

        public BoundsPopupMenuListener()
        {}

        @Override
        public void popupMenuWillBecomeVisible(final PopupMenuEvent e)
        {
            final JComboBox<?> comboBox = (JComboBox) e.getSource();
            if( comboBox.getItemCount() == 0 ) return;

            final Object child = comboBox.getAccessibleContext().getAccessibleChild(0);

            if(child instanceof BasicComboPopup) SwingUtilities.invokeLater( () -> _customizePopup( (BasicComboPopup) child ) );
        }

        @Override
        public void popupMenuWillBecomeInvisible(final PopupMenuEvent e)
        { if(_scrollPane != null) _scrollPane.setHorizontalScrollBar(null); }

        @Override
        public void popupMenuCanceled(final PopupMenuEvent e)
        {}

        private int _getScrollBarWidth(final BasicComboPopup popup, final JScrollPane _scrollPane)
        {
            final JComboBox<?> comboBox = (JComboBox) popup.getInvoker();

            return ( comboBox.getItemCount() > comboBox.getMaximumRowCount() ) ? _scrollPane.getVerticalScrollBar().getPreferredSize().width : 0;
        }

        private void _popupWider(final BasicComboPopup popup)
        {
            final Dimension scrollPaneSize = _scrollPane.getPreferredSize();
            final JList<?>  list           = popup.getList();

            final int popupWidth = Math.max( scrollPaneSize.width, list.getPreferredSize().width + _getScrollBarWidth(popup, _scrollPane) + 5 );

            scrollPaneSize.width = popupWidth;
          //scrollPaneSize.height = 30;

            _scrollPane.setPreferredSize(scrollPaneSize);
            _scrollPane.setMaximumSize  (scrollPaneSize);
        }

        private void _customizePopup(final BasicComboPopup popup)
        {
            _scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass( JScrollPane.class, popup.getList() );
            _popupWider(popup);

            final Component comboBox = popup   .getInvoker         ();
            final Point     location = comboBox.getLocationOnScreen();
            final int       height   = comboBox.getSize().height;

            popup.setLocation(location.x, location.y + height - 1);
            popup.setLocation(location.x, location.y + height    );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ArrayList<String> _gsbc_mapEvalRef_impl(final String ref, final BoardDef boardDef) throws JXMRuntimeError
    {
        for(final ConfigItem configItem : boardDef.configItems) {
            if( configItem.type.equals(configItem.userConfigName, ref) ) return configItem.values;
        }

        for(final MenuItem menuItem : boardDef.menuItems) {
            if(menuItem.configItems != null) {
                for(final ConfigItem configItem : menuItem.configItems) {
                    if( configItem.type.equals(configItem.userConfigName, ref) ) return configItem.values;
                }
            }
            if(menuItem.submenuItems != null) {
                MenuItem submenuItem = menuItem.submenuItems.getSelectedMenuItem();
                while(submenuItem != null) {
                    if(submenuItem.configItems != null) {
                        for(final ConfigItem configItem : submenuItem.configItems) {
                            if( configItem.type.equals(configItem.userConfigName, ref) ) return configItem.values;
                        }
                    }
                    submenuItem = (submenuItem.submenuItems == null) ? null : submenuItem.submenuItems.getSelectedMenuItem();
                }
            }
        }

        throw XCom.newJXMRuntimeError(Texts.EMsg_UnresolvedABoardsTxtRef, ref);
    }

    public static ArrayList<String> _gsbc_mapEvalRef(final String ref, final BoardDef boardDef) throws JXMRuntimeError
    {
        // The part only contains one build variable
        if( ref.indexOf('{', 1) == -1 ) return _gsbc_mapEvalRef_impl(ref, boardDef);

        // The part only contains more than one build variables
        final ArrayList<String> res = new ArrayList<>();

        res.add( XCom.reReplaceTokens(
            ref, _pmJxMakeABuildVar, (token) -> {
                try {
                    return XCom.flatten( _gsbc_mapEvalRef_impl(token, boardDef), "" );
                }
                catch(final JXMException e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                }
                return token; // If it was not handled above, simply return the original value
            }
        ) );

        return res;
    }

    public static void _gsbc_mapStore(final String handle, final String key, final ArrayList<String> values, final BoardDef boardDef) throws JXMRuntimeError
    {
        // Split into tokens
        final ArrayList<String> tokens = new ArrayList<>();

        for(final String value : values) {
            for( final String t : XCom.explode(value, " ") ) {
                if( t.isEmpty(   )       ) continue;
                if( t.indexOf('{') != -1 ) tokens.addAll( _gsbc_mapEvalRef(t, boardDef) );
                else                       tokens.add   (                  t            );
            }
        }

        for(int i = 0; i < tokens.size(); ++i) tokens.set( i, _res_StrDesc.apply( tokens.get(i) ) );

        // Store the tokens
        MapList.mapPut(handle, key, tokens, true, true);
    }

    public static String getSelectedBoardConfiguration(final String bdlString) throws JXMRuntimeError
    {
        // Lock mutex to ensure that only one dialog box can be displayed at a time
        _ConfigureBoardMutex.lock();

        // For storing the selected board
        BoardDef boardDef = null;

        // Unserialize the board definition string and get the selected board definition
        try {
            boardDef = fromEData(bdlString).getSelectedBoardDef();
        }
        catch(final Exception e) {
            // Unlock mutex
            _ConfigureBoardMutex.unlock();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Re-throw the exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }

        // Unlock mutex
        _ConfigureBoardMutex.unlock();

        // Create a new map
        final String mh = MapList.mapNew();

        // Store the configuration items to the map
        for(final ConfigItem configItem : boardDef.configItems) {
            _gsbc_mapStore( mh, configItem.type.mvName(configItem.userConfigName), configItem.values, boardDef );
        }

        // Process the menu items
        for(final MenuItem menuItem : boardDef.menuItems) {
            // Store the configuration items to the map
            if(menuItem.configItems != null) {
                for(final ConfigItem configItem : menuItem.configItems) {
                    _gsbc_mapStore( mh, configItem.type.mvName(configItem.userConfigName), configItem.values, boardDef );
                }
            }

            // Process the submenu items
            if(menuItem.submenuItems != null) {
                MenuItem submenuItem = menuItem.submenuItems.getSelectedMenuItem();
                while(submenuItem != null) {
                    // Store the configuration items to the map
                    if(submenuItem.configItems != null) {
                        for(final ConfigItem configItem : submenuItem.configItems) {
                            _gsbc_mapStore( mh, configItem.type.mvName(configItem.userConfigName), configItem.values, boardDef );
                        }
                    }
                    // Process the submenu items further
                    submenuItem = (submenuItem.submenuItems == null) ? null : submenuItem.submenuItems.getSelectedMenuItem();
                } // while
            }
        } // for

        // Return the map handle
        return mh;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String boardConfigurationGUI(final String title, final String bdlString) throws JXMRuntimeError
    {
        // Lock mutex to ensure that only one dialog box can be displayed at a time
        _ConfigureBoardMutex.lock();

        // For returning the board definition string
        String bdlReturn = null;

        // Show and run the GUI
        try {
            // Unserialize the board definition string
            final BoardDefList boardDefList = fromEData(bdlString);
            // Show and run the GUI
            _bcgui_show_impl( ( title == null || title.trim().isEmpty() ) ? null : title.trim(), boardDefList );
            // Accept the change as needed
            if(_acceptResult) bdlReturn = boardDefList.toEData();
        }
        catch(final Exception e) {
            // Unlock mutex
            _ConfigureBoardMutex.unlock();
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Re-throw the exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }

        // Unlock mutex
        _ConfigureBoardMutex.unlock();

        // Return back the board definition string
        return bdlReturn;
    }

    private static void _bcgui_changeBoardDisplay(final BoardDefList boardDefList)
    {
        // Remove the previous display as needed
        if(_pnlBoardConfig != null) {
            _mainWindow.remove(_pnlBoardConfig);
            _pnlBoardConfig = null;
        }

        // Get the selected board definition
        final BoardDef boardDef = boardDefList.getSelectedBoardDef();

        // Create the new display
        _pnlBoardConfig = new JPanel();
        _pnlBoardConfig.setBorder( new CompoundBorder(
                                           new EmptyBorder   (5, 0, 5, 0),
                                           new CompoundBorder(
                                                   new EtchedBorder(EtchedBorder.LOWERED),
                                                   new EmptyBorder (5, 5, 5, 5)
                                               )
                                       )
                                 );

            if( boardDef.menuItems.isEmpty() ) {
                _pnlBoardConfig.setLayout( new BorderLayout() );
                _pnlBoardConfig.add( new JLabel("<html><i>(the selected board has no configurable options)</i></html>", JLabel.CENTER), BorderLayout.CENTER );
            }

            else {
                final JPanel pnlHolder = new JPanel();

                    final int MinRows = 3;
                    final int NumRows = Math.max( MinRows, boardDef.menuItems.size() );

                    pnlHolder.setLayout( new GridLayout(NumRows, 2, 5, 5) );

                    for(int i = 0; i < boardDef.menuItems.size(); ++i) {

                        final MenuItem          menuItem  = boardDef.menuItems.get(i);
                        final JComboBox<String> cmbSelVal = new JComboBox<>();

                        for(int j = 0; j < menuItem.submenuItems.size(); ++j) cmbSelVal.addItem( menuItem.submenuItems.get(j).text );
                        cmbSelVal.setPrototypeDisplayValue("#########################");
                        cmbSelVal.setSelectedIndex(menuItem.submenuItems.selectedMenuItemIndex);
                        cmbSelVal.setMaximumRowCount(8);
                        cmbSelVal.addPopupMenuListener( new BoundsPopupMenuListener() );

                        cmbSelVal.addItemListener( new ItemListener() {
                            @Override
                            public void itemStateChanged(final ItemEvent e)
                            {
                                if(e.getStateChange() != ItemEvent.SELECTED) return;
                                menuItem.submenuItems.selectedMenuItemIndex = cmbSelVal.getSelectedIndex();
                            }
                        } );

                        final String labelText = ( menuItem.text == null || menuItem.text.isEmpty() )
                                               ? "<html><i>(unnamed configuration field)</i></html>"
                                               : menuItem.text;

                        pnlHolder.add( new JLabel(labelText) );
                        pnlHolder.add( cmbSelVal             );

                    } // for

                    for(int i = 0; i < MinRows - boardDef.menuItems.size(); ++i) {
                        pnlHolder.add( new JLabel("") );
                        pnlHolder.add( new JLabel("") );
                    }

                _pnlBoardConfig.setLayout( new BorderLayout()                );
                _pnlBoardConfig.add      ( pnlHolder   , BorderLayout.CENTER );
                _pnlBoardConfig.add      ( new JPanel(), BorderLayout.SOUTH  );
            }

        _mainWindow.add(_pnlBoardConfig, BorderLayout.CENTER);

        // Revalidate, pack, and repaint
        _mainWindow.revalidate();
        _mainWindow.pack      ();
        _mainWindow.repaint   ();
    }

    private static void _bcgui_show_impl(final String title, final BoardDefList boardDefList) throws Exception
    {
        // Clear variables
        _mainWindow     = null;
        _pnlBoardConfig = null;
        _acceptResult   = false;

        // Create the GUI
        _mainWindow = new JFrame( (title != null) ? title : "Configure Board" );

        _mainWindow.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        _mainWindow.setLayout( new BorderLayout() );
        _mainWindow.getRootPane().setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10) );

        _mainWindow.setSize       (               500, 250  );
        _mainWindow.setMinimumSize( new Dimension(500, 250) );

            // ----- Board selection panel -----
            final JPanel pnlSelectBoard = new JPanel();
            pnlSelectBoard.setLayout( new GridLayout(1, 1, 5, 5) );

                final JComboBox<String> cmbSelectBoard = new JComboBox<>();

                    for(final BoardDef boardDef : boardDefList) cmbSelectBoard.addItem(boardDef.name);
                    cmbSelectBoard.setSelectedIndex(boardDefList.selectedBoardDefIndex);
                    cmbSelectBoard.setMaximumRowCount( Math.min( Math.max( 3, boardDefList.size() / 4 ), 8 ) );
                  //cmbSelectBoard.addPopupMenuListener( new BoundsPopupMenuListener() );

                    cmbSelectBoard.addItemListener( new ItemListener() {
                        @Override
                        public void itemStateChanged(final ItemEvent e)
                        {
                            if(e.getStateChange() != ItemEvent.SELECTED) return;
                            boardDefList.selectedBoardDefIndex = cmbSelectBoard.getSelectedIndex();
                            _bcgui_changeBoardDisplay(boardDefList);
                        }
                    } );

                pnlSelectBoard.add(cmbSelectBoard);

            _mainWindow.add(pnlSelectBoard, BorderLayout.NORTH);

            // ----- Board configuration panel -----
            _bcgui_changeBoardDisplay(boardDefList);

            // ----- Buttons panel -----
            final JPanel pnlButton = new JPanel();
            pnlButton.setLayout( new GridLayout(1, 2, 5, 5) );

                final JButton btnOK = new JButton("OK");
                    btnOK.addActionListener( new ActionListener() {
                        @Override
                        public void actionPerformed(final ActionEvent ignored) {
                            _acceptResult = true;
                            _mainWindow.dispose();
                        }
                    } );
                pnlButton.add(btnOK);

                final JButton btnCancel = new JButton("Cancel");
                    btnCancel.addActionListener( new ActionListener() {
                        @Override
                        public void actionPerformed(final ActionEvent ignored) {
                            _mainWindow.dispose();
                        }
                    } );
                pnlButton.add(btnCancel);

            _mainWindow.add(pnlButton, BorderLayout.SOUTH);

        // Add focus listener to the main window to remove the always-on-top state
        _mainWindow.addWindowFocusListener( new WindowFocusListener_HandleAlwaysOnTop(_mainWindow) );

        // Add action listener to the main window to close the window when the Escape key is pressed
        _mainWindow.getRootPane().registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    _mainWindow.dispose();
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        // Call once more to ensure the initial window size is correct
        _bcgui_changeBoardDisplay(boardDefList);

        // Set the initial window position
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        _mainWindow.setLocation( ( dim.width - _mainWindow.getSize().width ) / 2, ( dim.height - _mainWindow.getSize().height ) / 3 );
      //_mainWindow.setLocationByPlatform(true);

        // Show the window
        _mainWindow.setResizable  (false);
        _mainWindow.setAlwaysOnTop(true );
        _mainWindow.setVisible    (true );

        // Process event
        while( _mainWindow.isVisible() ) Thread.yield();
    }

} // class ArduinoBoardsTxt
