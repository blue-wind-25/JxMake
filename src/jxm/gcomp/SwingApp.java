/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp;


import java.awt.Color;
import java.awt.Cursor;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.print.PrinterException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import javax.swing.text.BadLocationException;

import javax.swing.text.html.HTMLEditorKit;

import javax.swing.text.rtf.RTFEditorKit;

import javax.print.attribute.PrintRequestAttributeSet;

import jxm.*;
import jxm.xb.*;


public abstract class SwingApp {

    @SuppressWarnings("serial")
    public static class MainWindow extends JFrame {

        private final SwingApp _swingApp;

        @SuppressWarnings("this-escape")
        public MainWindow(final SwingApp swingApp, final String title, final JRootPane rootPane)
        {
            super(title);

            _swingApp = swingApp;

            if(rootPane != null) setRootPane(rootPane);
        }

        public SwingApp swingApp()
        { return _swingApp; }

        public JRootPane rootPane()
        { return this.getRootPane(); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public boolean isVisible()
        { return super.isVisible(); }

        public void toFrontAndFocus()
        {
            super.toFront();
            super.requestFocusInWindow();
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void showGlobalWaitCursor()
        { SwingApp.showGlobalWaitCursor( getRootPane() ); }

        public void showGlobalDefaultCursor()
        { SwingApp.showGlobalDefaultCursor( getRootPane() ); }

        public void showGlobalWaitCursorFM()
        { SwingApp.showGlobalWaitCursorFM(); }

        public void showGlobalDefaultCursorFM()
        { SwingApp.showGlobalDefaultCursorFM(); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void postEvent_WINDOW_CLOSING()
        {
            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                new WindowEvent(this, WindowEvent.WINDOW_CLOSING)
            );
        }

    } // class MainWindow

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected final boolean    _useDCT;
    protected final Object     _extPar;

    protected       MainWindow _mainWindow   = null;
    protected       boolean    _windowClosed = false;

    public SwingApp(final boolean useDarkColorTheme, final Object extraParam)
    {
        _useDCT = useDarkColorTheme;
        _extPar = extraParam;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Color rgbInvertOrDarken(final Color c)
    {
        final int r = 255 - c.getRed  ();
        final int g = 255 - c.getGreen();
        final int b = 255 - c.getBlue ();

        return new Color( Math.max(r - 30, 0  ), Math.max(g - 30, 0)  , Math.max(b - 30, 0  ) );
    }

    public static Color rgbInvertOrBrighten(final Color c)
    {
        final int r = 255 - c.getRed  ();
        final int g = 255 - c.getGreen();
        final int b = 255 - c.getBlue  ();

        return new Color( Math.min(r + 30, 255), Math.min(g + 30, 255), Math.min(b + 30, 255) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * https://www.w3.org/TR/WCAG21/#dfn-relative-luminance
     *
     *     sRGB gamma decoding (IEC 61966‑2‑1)
     *         C <= 0.03928 then sC = C / 12.92
     *                      else sC = ( (C + 0.055) / 1.055 ) ^ 2.4
     *
     *     Relative luminance formula (WCAG/CIE 1931)
     *         L = 0.2126 * sR + 0.7152 * sG + 0.0722 * sB
     *
     * ----------------------------------------------------------------------------------------------------
     *
     * https://www.w3.org/TR/WCAG21/#dfn-contrast-ratio
     *
     *     Contrast ratio
     *         ratio = (L1 + 0.05) / (L2 + 0.05)
     *
     * ----------------------------------------------------------------------------------------------------
     *
     * https://www.w3.org/TR/WCAG21/#contrast-minimum
     *
     *     The visual presentation of normal text and images of text has a contrast ratio of at least 4.5:1.
     */

    public static double gammaDecoding(final int c8bit)
    {
        final double s = c8bit / 255.0;

        return (s <= 0.04045) ? (s / 12.92) : Math.pow( (s + 0.055) / 1.055, 2.4 );
    }

    public static double relativeLuminance(final Color c)
    {
        final double r = gammaDecoding( c.getRed  () );
        final double g = gammaDecoding( c.getGreen() );
        final double b = gammaDecoding( c.getBlue () );

        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    public static double contrastRatio(final Color fg, final Color bg)
    {
        final double L1 = relativeLuminance(fg);
        final double L2 = relativeLuminance(bg);

        return ( Math.max(L1, L2) + 0.05 ) / ( Math.min(L1, L2) + 0.05 );
    }

    public static boolean enoughContrastForNormalText(final Color fg, final Color bg)
    { return contrastRatio(fg, bg) >= 4.5; }

    private static Color _adjustContrast(final Color cl, final Color bg)
    {
        // Flip brightness -> if too light, darken; if too dark, lighten

        final float[] clHSB = Color.RGBtoHSB( cl.getRed(), cl.getGreen(), cl.getBlue(), null );
        final float[] bgHSB = Color.RGBtoHSB( bg.getRed(), bg.getGreen(), bg.getBlue(), null );

        if( clHSB[2] > bgHSB[2] ) clHSB[2] = Math.max( 0.0f, bgHSB[2] - 0.5f );
        else                      clHSB[2] = Math.min( 1.0f, bgHSB[2] + 0.5f );

        return Color.getHSBColor( clHSB[0], clHSB[1], clHSB[2] );
    }

    public static Color rgbDeriveColor(final Color ref, final Color trg, final Color bgr)
    {
        final int   avgR = ( ref.getRed  () + trg.getRed  () ) / 2;
        final int   avgG = ( ref.getGreen() + trg.getGreen() ) / 2;
        final int   avgB = ( ref.getBlue () + trg.getBlue () ) / 2;
        final Color avg  = new Color(avgR, avgG, avgB);

        return enoughContrastForNormalText(avg, bgr) ? avg : _adjustContrast(avg, bgr);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void showGlobalWaitCursor(final JRootPane rootPane)
    {
        final JPanel glass = new JPanel();
            glass.setOpaque(false);
            glass.setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) );

        rootPane.setGlassPane(glass);

        rootPane.getGlassPane().setVisible(true);
    }

    public static void showGlobalDefaultCursor(final JRootPane rootPane)
    { rootPane.getGlassPane().setVisible(false); }

    public void showGlobalWaitCursor()
    { showGlobalWaitCursor( rootPane() ); }

    public void showGlobalDefaultCursor()
    { showGlobalDefaultCursor( rootPane() ); }

    public static void showGlobalWaitCursorFM()
    {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                            .getActiveWindow()
                            .setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) );
    }

    public static void showGlobalDefaultCursorFM()
    {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                            .getActiveWindow()
                            .setCursor( Cursor.getDefaultCursor() );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static AudioFormat _beepAudioFormat = null;
    private static byte[]      _beepDataCache   = null;
    private static Clip        _beepClip        = null;

    public synchronized static void playBeepSound()
    {
        // Cache the audio file as needed
        if(_beepDataCache == null) {
            try(
                final AudioInputStream ais = AudioSystem.getAudioInputStream( SysUtil.getAudioURL_defLoc("point-smooth-beep.wav") )
            ) {
                // Read the bytes
                final ByteArrayOutputStream baos   = new ByteArrayOutputStream();
                final byte[]                buffer = new byte[4096];

                while(true) {
                    final int bytesRead = ais.read(buffer);
                    if(bytesRead < 0) break;
                    baos.write(buffer, 0, bytesRead);
                }

                // Cache the bytes and format
                _beepDataCache   = baos.toByteArray();
                _beepAudioFormat = ais.getFormat();
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Clear the cache, just in case
                _beepAudioFormat = null;
                _beepDataCache   = null;
            }
        }

        if(_beepDataCache == null) return;

        try {
            // Check for possible error on the previous playback
            if( _beepClip != null && _beepClip.getFramePosition() == 0 ) {
                _beepClip.close();
                _beepClip = null;
            }

            // (Re-)initialize the clip as needed
            if(_beepClip == null) {
                final AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(_beepDataCache),
                    _beepAudioFormat,
                    _beepDataCache.length / _beepAudioFormat.getFrameSize()
                );
                _beepClip = AudioSystem.getClip();
                _beepClip.open(ais);
            }

            // Check if the previous playback is still running
            if( _beepClip == null || _beepClip.isRunning() ) return;

            // (Re-)start the playback
            _beepClip.setFramePosition(0);
            _beepClip.start();
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Clear the cache, just in case
            if(_beepClip != null) {
                _beepClip.close();
                _beepClip = null;
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void _initializeMainWindow(final String title, final JRootPane customRootPane)
    {
        // Create the main window
        _mainWindow = new MainWindow(this, title, customRootPane);

        _mainWindow.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        if(customRootPane == null) _mainWindow.setLayout( new BorderLayout() );

        _mainWindow.getRootPane().setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10) );

        // Set the window size
        _mainWindow.setSize       (               900, 600  );
        _mainWindow.setMinimumSize( new Dimension(900, 600) );

        // Set the window position
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        _mainWindow.setLocation( ( dim.width - _mainWindow.getSize().width ) / 2, ( dim.height - _mainWindow.getSize().height ) / 3 );
      //_mainWindow.setLocationByPlatform(true);

        // Add event listeners to the main window
        _mainWindow.addWindowFocusListener( new WindowFocusListener_HandleAlwaysOnTop(_mainWindow) );

        _mainWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        _mainWindow.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                if( handleWindowClosing() ) _mainWindow.dispose();
            }

            @Override
            public void windowClosed(final WindowEvent e) {
                _windowClosed = true;
            }
        } );
    }

    protected void _initializeMainWindow(final String title)
    { _initializeMainWindow(title, null); }

    protected void _showMainWindow(final JComponent focusedComponent, final boolean maximized) throws Exception
    {
        // Make the window to be sized to fit the preferred size and layouts of its subcomponent
        _mainWindow.pack();

        // Maximize the window as needed
        if(maximized) _mainWindow.setExtendedState( _mainWindow.getExtendedState() | JFrame.MAXIMIZED_BOTH );

        // Show the window
        _mainWindow.setAlwaysOnTop(true);
        _mainWindow.setVisible    (true);

        // Set focus to the given component as needed
        if(focusedComponent != null) focusedComponent.requestFocusInWindow();
    }

    protected void _showMainWindow(final JComponent focusedComponent) throws Exception
    { _showMainWindow(focusedComponent, true); }

    protected void _showMainWindow(final boolean maximized) throws Exception
    { _showMainWindow(null, maximized); }

    protected void _showMainWindow() throws Exception
    { _showMainWindow(null, true); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static JButton _createButtonWithMnemonic(final String text)
    {
        final StringBuilder cleanText    = new StringBuilder();
              char          mnemonicChar = 0;
              boolean       mnemonicSet  = false;

        for( int i = 0; i < text.length(); ++i ) {

            final char c = text.charAt(i);
            if(c == '&') {

                if( i + 1 < text.length() ) {
                    final char next = text.charAt(i + 1);
                    if(next == '&') {
                        // Escaped ampersand - append literal '&'
                        cleanText.append('&');
                        ++i; // Skip the second &
                    }
                    else if(!mnemonicSet) {
                        // Single '&' - mark mnemonic and skip the '&'
                        mnemonicChar = next;
                        mnemonicSet  = true;
                        cleanText.append(next);
                        ++i; // Skip the mnemonic character because it is already appended
                    }
                    else {
                        // The mnemonic is already set - just drop the '&'
                        cleanText.append(next);
                        ++i;
                    }
                }
                else {
                    // Trailing '&' - treat as literal '&'
                    cleanText.append('&');
                }

            }

            else {
                // Normal character
                cleanText.append(c);
            }

        } // for

        final JButton button = new JButton( cleanText.toString() );

        if(mnemonicSet)  button.setMnemonic(mnemonicChar);

        return button;
    }

    protected void _registerDefaultAndCancelButton(final JButton btnDefault, final JButton btnCancel)
    {
        _mainWindow.getRootPane().setDefaultButton(btnDefault);

        /*
        btnCancel.setBorder( BorderFactory.createLineBorder(Color.RED, 2) );
        //*/

        _mainWindow.getRootPane().registerKeyboardAction(
            e -> btnCancel.doClick(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        /*
        _mainWindow.getRootPane().registerKeyboardAction(
            e -> btnCancel.doClick(),
            KeyStroke.getKeyStroke( KeyEvent.VK_PERIOD, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() ),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        //*/
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # All derived classes must implement '_initializeAll()' and call '_initializeMainWindow()'
     *          from inside it.
     *        # Only after '_initializeMainWindow()' is called will 'mainWindow()' return a valid object.
     */

    protected abstract void _initializeAll() throws Exception;

    protected boolean handleWindowClosing()
    { return true; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static MainWindow getWindowAncestorFrom(final JRootPane rootPane)
    { return (MainWindow) SwingUtilities.getWindowAncestor(rootPane); }

    public MainWindow mainWindow()
    { return _mainWindow; }

    public JRootPane rootPane()
    { return _mainWindow.rootPane(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void initialize()
    {
        try {
            _initializeAll();
            Toolkit.getDefaultToolkit().setDynamicLayout(true);
        }
        catch(final Exception e) {
            e.printStackTrace();
            SysUtil.systemExitError();
        }
    }

    public void waitUntilClosed(final boolean exitSystemOnClose)
    {
        // Wait until the window is closed
        while(!_windowClosed) {
            // Yield
            Thread.yield();
        }

        // Exit the program normally
        if(exitSystemOnClose) SysUtil.systemExit();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void run(final boolean exitSystemOnClose)
    {
        // Initialize the application
        SwingUtilities.invokeLater( () -> {
            initialize();
        } );

        // Wait until the window is closed
        waitUntilClosed(exitSystemOnClose);
    }

    public void run()
    { run(true); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Transferable _cliboardContents = null;

    public static void saveCliboardContents()
    { _cliboardContents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null); }

    public static void restoreCliboardContents()
    { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(_cliboardContents, null); }

    public static void clearCliboardContents()
    { Toolkit.getDefaultToolkit().getSystemClipboard().setContents( new StringSelection(""), null ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void printStringAsPlainText(final String text, final PrintRequestAttributeSet attr, final Font font, final boolean check) throws BadLocationException, ClassNotFoundException, IOException, PrinterException, UnsupportedFlavorException
    {
        // Print plain text
        final JTextPane textPane = new JTextPane();

        textPane.setFont(font);
        textPane.setOpaque(true);

        textPane.setText(text);

        if(!true && check) {
            final JScrollPane scroll = new JScrollPane(textPane);
            final JDialog     dialog = new JDialog( (JFrame) null, "Preview", true );
            dialog.getContentPane().add(scroll);
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        }

        // Print
        textPane.print(null, null, true, null, attr, true);
    }

    public static void printClipboardAsRTF(final PrintRequestAttributeSet attr, final Font font, final Color checkPaneBackgroundColor) throws BadLocationException, ClassNotFoundException, IOException, PrinterException, UnsupportedFlavorException
    {
        // Check for RTF support
        final Clipboard    clipboard    = Toolkit.getDefaultToolkit().getSystemClipboard();
        final Transferable transferable = clipboard.getContents(null);
        final DataFlavor   rtfStream    = new DataFlavor("text/rtf;class=java.io.InputStream");

        if( !transferable.isDataFlavorSupported(rtfStream) ) {
            SwingApp.playBeepSound();
            return;
        }

        // Print RTF
        try(
            final InputStream is = (InputStream) transferable.getTransferData(rtfStream)
        ) {

            final ByteArrayOutputStream baos  = new ByteArrayOutputStream();
            final byte[]                buffer = new byte[4096];
                  int                   len;

            while( ( len = is.read(buffer) ) != -1 ) baos.write(buffer, 0, len);

            String rtf = new String(baos.toByteArray(), SysUtil._CharSet);
                   rtf = rtf.replaceAll("\\\\line", "\\\\par");

            final RTFEditorKit rtfKit   = new RTFEditorKit();
            final JTextPane    textPane = new JTextPane();

            textPane.setEditorKit(rtfKit);
            textPane.setFont(font);
            textPane.setOpaque(true);
            textPane.setEditable(false);

            rtfKit.read( new StringReader(rtf), textPane.getDocument(), 0 );

            if(!true && checkPaneBackgroundColor != null) {
                final JScrollPane scroll = new JScrollPane(textPane);
                final JDialog     dialog = new JDialog( (JFrame) null, "Preview", true );
                textPane.setBackground(checkPaneBackgroundColor);
                dialog.getContentPane().add(scroll);
                dialog.setSize(800, 600);
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }

            // Print
            textPane.print(null, null, true, null, attr, true);

        }
        catch(final Exception e) {
            throw e;
        }

        // Clear clipboard
        clearCliboardContents();
    }

    public static void printClipboardAsHTML(final PrintRequestAttributeSet attr, final boolean check) throws BadLocationException, ClassNotFoundException, IOException, PrinterException, UnsupportedFlavorException
    {
        // Check for HTML support
        final Clipboard    clipboard    = Toolkit.getDefaultToolkit().getSystemClipboard();
        final Transferable transferable = clipboard.getContents(null);
        final DataFlavor   htmlFlavor   = new DataFlavor("text/html;class=java.lang.String");

        if( !transferable.isDataFlavorSupported(htmlFlavor) ) {
            SwingApp.playBeepSound();
            return;
        }

        // Print HTML
        String html = (String) transferable.getTransferData(htmlFlavor);
             //html = html.replaceAll("(&nbsp;)+<br\\s*/>", "<br/>");

        final HTMLEditorKit htmlKit  = new HTMLEditorKit();
        final JTextPane     textPane = new JTextPane();

        textPane.setEditorKit(htmlKit);
        textPane.setOpaque(true);

        htmlKit.read( new StringReader(html), textPane.getDocument(), 0 );

        if(!true && check) {
            final JScrollPane scroll = new JScrollPane(textPane);
            final JDialog     dialog = new JDialog( (JFrame) null, "Preview", true );
            dialog.getContentPane().add(scroll);
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        }

        // Print
        textPane.print(null, null, true, null, attr, true);

        // Clear clipboard
        clearCliboardContents();
    }

} // class SwingApp
