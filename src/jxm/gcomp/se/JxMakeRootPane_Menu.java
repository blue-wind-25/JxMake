/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp.se;

import java.awt.Component;

import java.awt.event.ActionEvent;

import java.lang.reflect.Method;

import java.net.URL;

import java.util.HashMap;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


@SuppressWarnings("serial")
@package_private
class JxMakeRootPane_Menu {

    private static class IMenuItem {
        public final JMenuItem            menuItem;
        public final JCheckBoxMenuItem    checkBox;
        public final JRadioButtonMenuItem radioBtn;

        public IMenuItem(final JMenuItem item)
        {
            checkBox = (item instanceof JCheckBoxMenuItem   ) ? ( (JCheckBoxMenuItem   ) item ) : null;
            radioBtn = (item instanceof JRadioButtonMenuItem) ? ( (JRadioButtonMenuItem) item ) : null;
            menuItem = (checkBox == null && radioBtn == null) ?                          item   : null;
        }
    }

    private static final HashMap<String, IMenuItem> _menuItems = new HashMap<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static ImageIcon _newImageIcon(final String name)
    { return SysUtil.newImageIcon_defLoc("menu/mnu_" + name + ".gif"); }

    private static final void _invokeMethod(final JxMakeRootPane rootPane, final String methodName)
    {
        try {
            final Method method = JxMakeRootPane.class.getDeclaredMethod(methodName);
            method.invoke(rootPane);
        }
        catch(final Exception ex) {
            ex.printStackTrace();
            SysUtil.systemExitError();
        }
    }

    private static final void _invokeAction(final JxMakeRootPane rootPane, final String menuAction)
    {
        final RSyntaxTextArea textArea = rootPane.textArea();
        final Action          action   = textArea.getActionMap().get(menuAction);

        if(action != null) action.actionPerformed( new ActionEvent(textArea, ActionEvent.ACTION_PERFORMED, menuAction) );
    }

    public static final void _dlgWarningNotImplemented(final JxMakeRootPane rootPane)
    { JOptionPane.showMessageDialog( SwingUtilities.getWindowAncestor(rootPane), Texts.EMsg_NotImplementedYet, Texts.ScrEdt_Error, JOptionPane.WARNING_MESSAGE); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _usedMnemonic = "";

    private static void _createMenu(final JxMakeRootPane rootPane, final JMenuBar menuBar, final MenuSpec[] menuSpec, final String menuSpecName) throws JXMFatalLogicError
    {
        if( !menuSpec[0].isMenu() ) throw XCom.newJXMFatalLogicError("!" + menuSpecName + "[0].isMenu()");

        final String text     = menuSpec[0].text;
        final String utext    = text.toUpperCase();
              int    mnemonic = menuSpec[0].mnemonic;

        //*
        if( utext.indexOf( (char) mnemonic ) < 0 ) {

            char    candidate = utext.charAt(0);
            boolean found     = !_usedMnemonic.contains("" + candidate);

            if(!found) {
                for( int j = 1; j < utext.length(); ++j ) {

                    final char check = utext.charAt(j);

                    if(    ( (check >= 'A' && check <= 'Z') || (check >= '0' && check <= '9') )
                        && ( !_usedMnemonic.contains("" + check)                              )
                    ) {
                        candidate = check;
                        found     = true;
                        break;
                    }

                } // for
            } // if

            if(found) mnemonic = candidate;

        } // if
        //*/

        ButtonGroup buttonGroup = null;

        final JMenu menu = new JMenu(text);
            menu.setMnemonic(mnemonic);
            _usedMnemonic += (char) mnemonic;

            if(menuSpec[0].action != null) {
                if( menuSpec[0].action.startsWith("#") ) {
                    final String methodName = menuSpec[0].action.substring(1);
                    if( !methodName.isEmpty() ) {
                        menu.addMenuListener( new MenuListener() {
                            @Override public void menuSelected  (final MenuEvent e) { _invokeMethod(rootPane, methodName); }
                            @Override public void menuDeselected(final MenuEvent e) {}
                            @Override public void menuCanceled  (final MenuEvent e) {}
                        } );
                    }
                    else {
                          menu.addMenuListener( new MenuListener() {
                            @Override public void menuSelected  (final MenuEvent e) { _dlgWarningNotImplemented(rootPane); }
                            @Override public void menuDeselected(final MenuEvent e) {}
                            @Override public void menuCanceled  (final MenuEvent e) {}
                        } );
                    }
                }
                else {
                    menu.addMenuListener( new MenuListener() {
                        @Override public void menuSelected  (final MenuEvent e) { _invokeAction(rootPane, menuSpec[0].action); }
                        @Override public void menuDeselected(final MenuEvent e) {}
                        @Override public void menuCanceled  (final MenuEvent e) {}
                    } );
                }
            }

            if(menuSpec[0].id != null) {
                menu.setActionCommand(menuSpec[0].id);
                _menuItems.put( menuSpec[0].id, new IMenuItem(menu) );
            }

            for(int i = 1; i < menuSpec.length; ++i) {

                if(menuSpec[i] == null) break;

                if( menuSpec[i].isSeparator() ) {
                    menu.addSeparator();
                    buttonGroup = null;
                }

                else if( menuSpec[i].isItem() ) {

                    final String menuAction = menuSpec[i].action;
                    if(menuAction == null) throw XCom.newJXMFatalLogicError(menuSpecName + "[" + i + "].action == null");

                    String  itemText      = menuSpec[i].text;
                    boolean itemDisabled  = false;
                    boolean itemCheckBox  = false;
                    boolean itemRadioBtn  = false;
                    boolean selectedState = false;

                    if( itemText.startsWith("~") ) {
                        itemDisabled = true;
                        itemText     = itemText.substring(1);
                    }
                    if( itemText.startsWith("*") ) {
                        itemCheckBox = true;
                        itemText     = itemText.substring(1);
                    }
                    if( itemText.startsWith("^") ) {
                        itemRadioBtn = true;
                        itemText     = itemText.substring(1);
                    }
                    if(itemCheckBox || itemRadioBtn) {
                        if( itemText.startsWith("+") ) {
                            selectedState = true;
                            itemText      = itemText.substring(1);
                        }
                        else if( itemText.startsWith("-") ) {
                            selectedState = false;
                            itemText      = itemText.substring(1);
                        }
                    }

                    final JMenuItem menuItem = itemCheckBox ? new JCheckBoxMenuItem   ( itemText, _newImageIcon(menuSpec[i].icon), selectedState )
                                             : itemRadioBtn ? new JRadioButtonMenuItem( itemText, _newImageIcon(menuSpec[i].icon), selectedState )
                                             :                new JMenuItem           ( itemText, _newImageIcon(menuSpec[i].icon)                );

                        if(menuSpec[i].mnemonic    >  0   ) menuItem.setMnemonic   (menuSpec[i].mnemonic);
                        if(menuSpec[i].accelerator != null) menuItem.setAccelerator(menuSpec[i].accelerator);
                        if(itemDisabled                   ) menuItem.setEnabled(false);

                        if(itemRadioBtn) {
                            if(buttonGroup == null) buttonGroup = new ButtonGroup();
                            buttonGroup.add(menuItem);
                        }

                        if( menuAction.startsWith("#") ) {
                            final String methodName = menuAction.substring(1);
                            if( !methodName.isEmpty() ) {
                                menuItem.addActionListener( e -> { _invokeMethod(rootPane, methodName); } );
                            }
                            else {
                                menuItem.addActionListener( e -> { _dlgWarningNotImplemented(rootPane); } );
                            }
                        }
                        else {
                            menuItem.addActionListener( e -> { _invokeAction(rootPane, menuAction); } );
                        }

                    menu.add(menuItem);

                    if(menuSpec[i].id != null) {
                        menuItem.setActionCommand(menuSpec[i].id);
                        _menuItems.put( menuSpec[i].id, new IMenuItem(menuItem) );
                    }

                }

                else {
                    throw XCom.newJXMFatalLogicError("!" + menuSpecName + "[" + i + "].isSeparator() && !" + menuSpecName + "[" + i + "].isItem()");
                }

            } // for

        menuBar.add(menu);
     }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void createMenuBar(final JxMakeRootPane rootPane)
    {
        try {

            final JMenuBar menuBar = new JMenuBar();

                for(final XCom.Pair<MenuSpec[], String> menuSpec : Texts.ScrEdt_MenuSpecs) {
                    if(menuSpec == null) menuBar.add( Box.createHorizontalGlue() );
                    else _createMenu( rootPane, menuBar, menuSpec.first(), menuSpec.second() );
                }

            rootPane.setJMenuBar(menuBar);

        }
        catch(final Exception e) {
            e.printStackTrace();
            SysUtil.systemExitError();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean itmIsEnabled(final String id)
    { return _menuItems.get(id).menuItem.isEnabled(); }

    public static void itmSetEnabled(final String id, final boolean state)
    { _menuItems.get(id).menuItem.setEnabled(state); }

    public static void itmSetEnableAllExcept(final String id, final boolean state, final String... exception)
    {
        final JMenu menu  = (JMenu) _menuItems.get(id).menuItem;
        final int   count = menu.getMenuComponentCount();

        for(int i = 0; i < count; ++i) {

            final Component comp = menu.getMenuComponent(i);
            if( !(comp instanceof JMenuItem) ) continue;

            final JMenuItem item = (JMenuItem) comp;

            boolean isException = false;

            if(exception != null) {
                for(final String ex : exception) {
                    if( !ex.equals( item.getActionCommand() ) ) continue;
                    isException = true;
                    break;
                }
            }

            if(!isException) item.setEnabled(state);

        } // for
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean chkIsEnabled(final String id)
    { return _menuItems.get(id).checkBox.isEnabled(); }

    public static void chkSetEnabled(final String id, final boolean state)
    { _menuItems.get(id).checkBox.setEnabled(state); }

    public static boolean chkIsSelected(final String id)
    { return _menuItems.get(id).checkBox.isSelected(); }

    public static void chkSetSelected(final String id, final boolean state)
    { _menuItems.get(id).checkBox.setSelected(state); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean rdoIsEnabled(final String id)
    { return _menuItems.get(id).radioBtn.isEnabled(); }

    public static void rdoSetEnabled(final String id, final boolean state)
    { _menuItems.get(id).radioBtn.setEnabled(state); }

    public static boolean rdoIsSelected(final String id)
    { return _menuItems.get(id).radioBtn.isSelected(); }

    public static void rdoSetSelected(final String id, final boolean state)
    { _menuItems.get(id).radioBtn.setSelected(state); }

} // class JxMakeRootPane_Menu
