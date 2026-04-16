/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.util.ArrayList;

import java.util.stream.Collectors;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;

import java.beans.XMLDecoder;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import jxm.xb.*;


@SuppressWarnings("serial")
public class XMLFrame extends JFrame {

    public static class Result {
        public final String type;
        public final String value;

        // NOTE : It only supports storing values from JCheckBox, JComboBox, JList, JRadioButton, JSlider, JSpinner, JTextArea, and JTextField
        public Result(final String type_, final String value_)
        {
            type  = type_;
            value = value_;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static XCom.Mutex _globalXMLFrameMutex = new XCom.Mutex();

    public static ArrayList<Result> _execute_impl(final XMLDecoder xmlDecoder) throws Exception
    {
        /* Please refer to:
         *     https://docs.oracle.com/javase/8/docs/api/java/beans/XMLEncoder.html
         *     https://www.oracle.com/technical-resources/articles/java/persistence3.html
         *     https://stackoverflow.com/questions/49276419/build-gui-in-swing-using-xml-file
         * for more details
         */

        // Lock mutex to ensure that only one dialog box can be displayed at a time
        _globalXMLFrameMutex.lock();

        // Prepare the result variable
        ArrayList<Result> result = null;

        // Decode the XML and handle events
        try {
            // Decode the XML
            final XMLFrame frame = (XMLFrame) xmlDecoder.readObject();
            xmlDecoder.close();
            // Handle events until the window is closed
            result = frame._handle();
        }
        catch(final Exception e) {
            // Unlock mutex
            _globalXMLFrameMutex.unlock();
            // Re-throw the exception
            throw e;
        }

        // Unlock mutex
        _globalXMLFrameMutex.unlock();

        // Return the result
        return result;
    }

    public static ArrayList<Result> executeFromXMLFile(final String xmlFilePath) throws Exception
    {
        return _execute_impl( new XMLDecoder(
            new BufferedInputStream( new FileInputStream( SysUtil.resolveAbsolutePath(xmlFilePath) ) )
        ) );
    }

    public static ArrayList<Result> executeFromXMLString(final String xmlString) throws Exception
    {
        return _execute_impl( new XMLDecoder(
            new ByteArrayInputStream( xmlString.getBytes(SysUtil._CharEncoding) )
        ) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String escapeEntity(String str)
    {
        /*
         * The code below is developed based on the code from:
         *     https://stackoverflow.com/a/66432094
         *     https://www.ascii-code.com
         */

        return str.codePoints().mapToObj(
            c -> ( c <= 31 || c >= 127 || "\"'<>&".indexOf(c) != -1 ) ? "&#" + c + ";"
                                                                      : new String( Character.toChars(c) )
        )
        .collect( Collectors.joining() );
    }

    public static ImageIcon getImageIcon(final String path)
    { return new ImageIcon( JxMake.class.getResource(path) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final ArrayList<Result> _results                 = new ArrayList<>();

    private       EventHandler      _ehSCutEnterCloseWindow  = null;
    private       boolean           _enSCutEscapeCloseWindow = false;

    private ArrayList<Result> _handle()
    {
        // Add shortcuts
        final JFrame mainFrame = this;

        getRootPane().registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    if(_ehSCutEnterCloseWindow != null) _ehSCutEnterCloseWindow.handle();
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        getRootPane().registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    if(_enSCutEscapeCloseWindow) processWindowEvent( new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING) );
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        // Set the window position
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation( ( dim.width - getSize().width ) / 2, ( dim.height - getSize().height ) / 3 );
      //setLocationByPlatform(true);

        // Show the window
        setAlwaysOnTop(true);
        setVisible    (true);

        // Process event
        while( isVisible() ) Thread.yield();

        // Return the results
        return _results;
    }

    public void enSCut_keyEnter_closeWindow(final EventHandler eventHandler)
    { _ehSCutEnterCloseWindow = eventHandler; }

    public void enSCut_keyEscape_closeWindow(final boolean enabled)
    { _enSCutEscapeCloseWindow = enabled; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static abstract class EventHandler {
        protected final XMLFrame _mainWindow;
        protected final String   _ownerName;

        public EventHandler(final XMLFrame mainWindow, final String ownerName)
        {
            _mainWindow = mainWindow;
            _ownerName  = ownerName;
        }

        public abstract void handle();
    }

    public static class WindowListener extends EventHandler {
        public WindowListener(final XMLFrame mainWindow, final String ownerName)
        { super(mainWindow, ownerName); }

        @Override
        public void handle()
        {
            final StackTraceElement[] ste = Thread.currentThread().getStackTrace();

            for(int i = 0; i < ste.length; ++i) {

                final String methodName = ste[i].getMethodName();

                if( methodName.equals("windowClosed") ) {
                    _mainWindow.dispose();
                    break;
                }

            } // for
        }
    }

    public static class WindowFocusListener extends EventHandler {
        private long    _mwfl_lastTime = 0;
        private boolean _mwfl_done     = false;

        public WindowFocusListener(final XMLFrame mainWindow, final String ownerName)
        { super(mainWindow, ownerName); }

        @Override
        public void handle()
        {
            if(_mwfl_done) return;

            final StackTraceElement[] ste = Thread.currentThread().getStackTrace();

            for(int i = 0; i < ste.length; ++i) {

                final String methodName = ste[i].getMethodName();

                if( methodName.equals("windowGainedFocus") ) {
                    _mwfl_lastTime = SysUtil.getMS();
                    break;
                }

                if( methodName.equals("windowLostFocus") ) {
                    final long newTime = SysUtil.getMS();
                    if(newTime - _mwfl_lastTime > 250) {
                        _mainWindow.setAlwaysOnTop(false);
                        _mwfl_done = true;
                    }
                    _mwfl_lastTime = newTime;
                    break;
                }

            } // for

        }
    }

    public static class ButtonExitListener extends EventHandler {
        public ButtonExitListener(final XMLFrame mainWindow, final String ownerName)
        { super(mainWindow, ownerName); }

        @Override
        public void handle()
        {
            _mainWindow._results.add( new Result("[ButtonExit]", _ownerName) );
            _mainWindow.dispose();
        }
    }

    public static class ButtonAcceptListener extends EventHandler {
        public ButtonAcceptListener (final XMLFrame mainWindow, final String ownerName)
        { super(mainWindow, ownerName); }

        private void _putComponent(final Object obj)
        {
                 if(obj instanceof JCheckBox   ) _mainWindow._results.add( new Result("JCheckBox"   ,                 ( (JCheckBox   ) obj ).isSelected      () ? "1" : "0" ) );
            else if(obj instanceof JComboBox   ) _mainWindow._results.add( new Result("JComboBox   ", String.valueOf( ( (JComboBox   ) obj ).getSelectedIndex() + 1 )       ) );
            else if(obj instanceof JList       ) _mainWindow._results.add( new Result("JList"       , String.valueOf( ( (JList       ) obj ).getSelectedIndex() + 1 )       ) );
            else if(obj instanceof JRadioButton) _mainWindow._results.add( new Result("JRadioButton",                 ( (JRadioButton) obj ).isSelected      () ? "1" : "0" ) );
            else if(obj instanceof JSlider     ) _mainWindow._results.add( new Result("JSlider"     , String.valueOf( ( (JSlider     ) obj ).getValue        ()     )       ) );
            else if(obj instanceof JSpinner    ) _mainWindow._results.add( new Result("JSpinner",                     ( (JSpinner    ) obj ).getValue        ().toString()  ) );
            else if(obj instanceof JTextArea   ) _mainWindow._results.add( new Result("JTextArea"   ,                 ( (JTextArea   ) obj ).getText         ()             ) );
            else if(obj instanceof JTextField  ) _mainWindow._results.add( new Result("JTextField"  ,                 ( (JTextField  ) obj ).getText         ()             ) );
        }

        private void _prcComponents(final Component[] components)
        {
            for(final Component component : components) {
                if(component instanceof JPanel) {
                    _prcComponents( ( (JPanel) component ).getComponents() );
                }
                else if(component instanceof JScrollPane) {
                    final Component vvComponent = ( (JScrollPane) component ).getViewport().getView();
                    if(vvComponent instanceof JPanel) _prcComponents( ( (JPanel) vvComponent ).getComponents() );
                    else                              _putComponent (            vvComponent                   );
                }
                else {
                    _putComponent(component);
                }
            }
        }

        @Override
        public void handle()
        {
            _mainWindow._results.add( new Result("[ButtonAccept]", _ownerName) );
            _prcComponents( _mainWindow.getContentPane().getComponents() );
            _mainWindow.dispose();
        }
    }

} // class XMLFrame
