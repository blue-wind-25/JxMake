/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp.se;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowEvent;

import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.lang.reflect.Field;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import java.util.regex.Matcher;

import java.util.zip.ZipFile;

import java.net.URL;

import java.security.NoSuchAlgorithmException;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;

import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.OrientationRequested;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.fife.com.swabunga.spell.engine.SpellDictionaryHashMap;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;

import org.fife.ui.rsyntaxtextarea.spell.SpellingParser;
import org.fife.ui.rsyntaxtextarea.spell.SpellCheckableTokenIdentifier;

import org.fife.ui.rtextarea.FoldIndicatorStyle;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;

import jxm.*;
import jxm.annotation.*;
import jxm.gcomp.*;
import jxm.tool.*;
import jxm.xb.*;

import static jxm.gcomp.se.JxMakeFileList.FileItem;
import static jxm.gcomp.se.JxMakeFileList.FileState;
import static jxm.gcomp.se.JxMakeTheme.TType;
import static jxm.gcomp.se.JxMakeTokenMaker_Utils.*;


/*
 * JxMake Script Editor with Syntax Highlighting and Code Folding
 *
 * Limitations:
 *     # The editor uses token-based highlighting and therefore cannot fully distinguish between valid
 *       and invalid JxMake script constructs. As a result, it may even highlight invalid constructs as
 *       if they were not errors.
 *     # Support for extra whitespace(s) and line continuation using the backslash character (\) at the
 *       end of a line are currently very limited. As a result, the editor may not correctly highlight
 *       such constructs. Future releases will attempt to improve this behavior; however, some cases may
 *       remain unsupported due to inherent tokenizer limitations.
 *     # The editor may intermittently render incorrect highlighting for certain constructs. This issue
 *       is typically fixable by triggering a full refresh and rehighlight operation, either via the
 *       menu or the appropriate shortcut.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * https://github.com/bobbylight/RSyntaxTextArea
 * https://github.com/bobbylight/RSTAUI
 * https://github.com/bobbylight/AutoComplete
 * https://github.com/bobbylight/SpellChecker
 */
@SuppressWarnings("serial")
public class JxMakeRootPane extends JRootPane implements SearchListener {

    private static String APP_DATA_DIRECTORY  = "jxmake_script_editor" + SysUtil._InternalDirSep;

    private static String SCRIPT_EDITOR_STATE = APP_DATA_DIRECTORY + "script_editor_state";
    private static String SCRIPT_FILE_STATE   = APP_DATA_DIRECTORY + "script_file_state";
    private static String CONSOLE_CMD_HIST    = APP_DATA_DIRECTORY + "console_command_history";
    private static String CONSOLE_CMD_ALIAS   = APP_DATA_DIRECTORY + "console_alias";
    private static String CONSOLE_ENV_PREFIX  = APP_DATA_DIRECTORY + "console_env_";

    private static void _mkdir_AppDataDir() throws IOException
    { SysUtil.cu_mkdir( SysUtil.resolvePath( APP_DATA_DIRECTORY, SysUtil.getADD() ) ); }

    private static String _getPath_scriptEditorState()
    {
        try                      { _mkdir_AppDataDir(); return SysUtil.resolvePath( SCRIPT_EDITOR_STATE, SysUtil.getADD() ); }
        catch(final Exception e) { return null; }
    }

    public static String getPath_scriptFileState()
    {
        try                      { _mkdir_AppDataDir(); return SysUtil.resolvePath( SCRIPT_FILE_STATE  , SysUtil.getADD() ); }
        catch(final Exception e) { return null; }
    }

    public static String getPath_consoleCommandHistory()
    {
        try                      { _mkdir_AppDataDir(); return SysUtil.resolvePath( CONSOLE_CMD_HIST   , SysUtil.getADD() ); }
        catch(final Exception e) { return null; }
    }

    public static String getPath_consoleAlias()
    {
        try                      { _mkdir_AppDataDir(); return SysUtil.resolvePath( CONSOLE_CMD_ALIAS  , SysUtil.getADD() ); }
        catch(final Exception e) { return null; }
    }

    private static String _getPath_consoleEnvVarFor_impl(final String filePath)
    {
        try                      { _mkdir_AppDataDir(); return SysUtil.resolvePath( AppConfigFile.makeFileNameKeyFor(CONSOLE_ENV_PREFIX, filePath), SysUtil.getADD() ); }
        catch(final Exception e) { return null; }
    }

    public static String getPath_consoleEnvVarForDefault()
    { return _getPath_consoleEnvVarFor_impl(AppConfigFile.DEFAULT_KEY_NAME); }

    public static String getPath_consoleEnvVar()
    {
        if( !_instance._fileList.mainFile.isFileSpecified() ) return getPath_consoleEnvVarForDefault();

        return _getPath_consoleEnvVarFor_impl( _instance._fileList.mainFile.absFullFilePath() );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getPath_mainScriptFile()
    {
        if( _instance._fileList.mainFile.isFileSpecified() ) {
            return SysUtil.resolveRelativePath( _instance._fileList.mainFile.absFullFilePath() );
        }

        return "";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    public static class Config extends SerializableDeepClone<Config> {

        // JxMake use a special field name for serial version UID
        @DataFormat.Hex16 public static final long __0_JxMake_SerialVersionUID__ = SysUtil.extSerialVersionUID(0x00000001);

        public String  recentFile0WorkDir = null;
        public String  recentFile0AbsPath = null;
        public String  recentFile1WorkDir = null;
        public String  recentFile1AbsPath = null;
        public String  recentFile2WorkDir = null;
        public String  recentFile2AbsPath = null;
        public String  recentFile3WorkDir = null;
        public String  recentFile3AbsPath = null;
        public String  recentFile4WorkDir = null;
        public String  recentFile4AbsPath = null;
        public String  recentFile5WorkDir = null;
        public String  recentFile5AbsPath = null;
        public String  recentFile6WorkDir = null;
        public String  recentFile6AbsPath = null;
        public String  recentFile7WorkDir = null;
        public String  recentFile7AbsPath = null;
        public String  recentFile8WorkDir = null;
        public String  recentFile8AbsPath = null;
        public String  recentFile9WorkDir = null;
        public String  recentFile9AbsPath = null;

        public boolean spellCheckEnabled  = false;
        public String  spellCheckSelDic   = null;

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void save()
        {
            try(
                final FileWriter writer = new FileWriter( _getPath_scriptEditorState() )
            ) {
                writer.write( SerializableDeepClone.toJSON(this) );
            }
            catch(final IOException e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }
        }

        public static Config load()
        {
            try {
                final String content = new String( Files.readAllBytes( Paths.get( _getPath_scriptEditorState() ) ) );
                final Config inst    = SerializableDeepClone.fromJSON(content, Config.class);
                if(inst != null) return inst;
            }
            catch(final IOException e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }

            return new Config();
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private static final int N = 10; // recentFile[0-9][WorkDir|AbsPath]

        private XCom.Pair<Field[], Field[]> _getRecentFileFields()
        {
            try {
                final Field[] wdFields = new Field[N];
                final Field[] apFields = new Field[N];

                for(int i = 0; i < N; ++i) {
                    wdFields[i] = this.getClass().getField( "recentFile" + i + "WorkDir");
                    apFields[i] = this.getClass().getField( "recentFile" + i + "AbsPath");
                }

                return new XCom.Pair<Field[], Field[]>(wdFields, apFields);
            }
            catch(final NoSuchFieldException e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }

            return null;
        }

        private String _getField(final Field field)
        {
            try {
                return (String) field.get(this);
            }
            catch(final IllegalAccessException e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }

            return null;
        }

        private void _setField(final Field field, final String value)
        {
            try {
                field.set(this, value);
            }
            catch(final IllegalAccessException e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public boolean isRecentFileEmpty()
        { return recentFile1WorkDir == null || recentFile1AbsPath == null; }

        public void clearRecentFiles()
        {
            // Collect fields into plain array
            XCom.Pair<Field[], Field[]> fp = _getRecentFileFields();
            if(fp == null) return;

            final Field[] wdFields = fp.first ();
            final Field[] apFields = fp.second();

            // Clear
            for(int i = 0; i < N; ++i) {
                _setField( wdFields[i], null );
                _setField( apFields[i], null );
            }

            // Save
            save();
        }

        private void _putRecentFile_impl(final String workDir, final String absPath)
        {
            // Collect fields into plain array
            XCom.Pair<Field[], Field[]> fp = _getRecentFileFields();
            if(fp == null) return;

            final Field[] wdFields = fp.first ();
            final Field[] apFields = fp.second();

            // Remove duplicates
            for(int i = 0; i < N; ++i) {
                final String ap = _getField( apFields[i] );
                if( Objects.equals(absPath, ap) ) {
                    _setField( wdFields[i], null );
                    _setField( apFields[i], null );
                }
            }

            // Compact (shift non-nulls left)
            int pos = 0;
            for(int i = 0; i < N; ++i) {
                final String wd = _getField( wdFields[i] );
                final String ap = _getField( apFields[i] );
                if(wd != null && ap != null) {
                    _setField( wdFields[pos], wd );
                    _setField( apFields[pos], ap );
                    ++pos;
                }
            }
            while(pos < N) {
                _setField( wdFields[pos], null );
                _setField( apFields[pos], null );
                ++pos;
            }

            // Shift right
            for(int i = N - 1; i > 0; --i) {
                final String wd = _getField( wdFields[i - 1] );
                final String ap = _getField( apFields[i - 1] );
                _setField( wdFields[i], wd );
                _setField( apFields[i], ap );
            }

            // Store new value at 'recentFile1WorkDir' and 'recentFile1AbsPath'
            _setField( wdFields[0], workDir );
            _setField( apFields[0], absPath );

            // Save
            save();
        }

        public void putRecentFile(final String filePath)
        {
            if(filePath == null) return;

            final String dirPath = SysUtil.getCWD();
            final String absPath = SysUtil.resolveAbsolutePath(filePath);

            _putRecentFile_impl(dirPath, absPath);
        }

        public Map<String, String> getRecentFiles()
        {
            // Collect fields into plain array
            XCom.Pair<Field[], Field[]> fp = _getRecentFileFields();
            if(fp == null) return null;

            final Field[] wdFields = fp.first ();
            final Field[] apFields = fp.second();

            // Store non-null
            final LinkedHashMap<String, String> res = new LinkedHashMap<>();

            for(int i = 0; i < N; ++i) {
                final String wd = _getField( wdFields[i] );
                final String ap = _getField( apFields[i] );
                if(wd != null && ap != null) res.put(ap, wd);
            }

            return res;
        }

    } // class Config

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int       ANSIScreenUpdateRate_MS =  100; // Update the screen every 100mS
    private static final int       ANSITextBlinkDelayCnt   =    5; // Blink text every 500mS

    private static final int       UpdateIncludeDelay_MS   =  750; // Schedule the update task after 750mS
    private static final int       UpdateThrottleWindow_MS = 1500; // Throttle window is 1500mS

    private static final int       FileWatcherPollDelay_MS = 2500; // Poll every 2500mS

    private static final ImageIcon IconJxMakeLogo          = SysUtil.newImageIcon_defLoc("jxmake_logo.png"       );
    private static final ImageIcon IconBookmark            = SysUtil.newImageIcon_defLoc("indicator/bookmark.png");
    private static final String    SYNTAX_STYLE_JXMAKEFILE = "text/JxMakeFile";

    static {
        final AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping( SYNTAX_STYLE_JXMAKEFILE, JxMakeTokenMaker.class.getName() );

        FoldParserManager.get().addFoldParserMapping( SYNTAX_STYLE_JXMAKEFILE, new JxMakeFoldParser() );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private KeyEvent _newKeyEvent(final int modifiers, final int keyCode, final char keyChar)
    { return new KeyEvent(_textArea, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, keyCode, keyChar); }

    private final void _invokeAction(final String actionName)
    { _textArea.getActionMap().get(actionName).actionPerformed( new ActionEvent(_textArea, ActionEvent.ACTION_PERFORMED, actionName) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean anyVisibleWindows()
    { return Arrays.stream( Window.getWindows() ).anyMatch(Window::isShowing); }

    private static void _showOKDialogWithScrollPane(final String title, final String htmlMessage, final int type, final ImageIcon icon)
    {
        final JPanel panel = new JPanel( new BorderLayout() );

            panel.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5) );
            panel.add( new JLabel(htmlMessage), BorderLayout.CENTER );

        final JScrollPane scrollPane = new JScrollPane(panel);

            final Dimension sz = Toolkit.getDefaultToolkit().getScreenSize();
            final int       w  = Math.max(800, sz.width  / 3);
            final int       h  = Math.max(450, sz.height / 3);

            scrollPane.setPreferredSize( new Dimension(w, h) );

        final Runnable showDialog = () -> JOptionPane.showMessageDialog(null, scrollPane, title, type, icon);

        if( anyVisibleWindows() ) showDialog.run();
        else                      SwingUtilities.invokeLater(showDialog);
    }

    private static void _showExceptionDialogWithScrollPane(final Exception e)
    {
        // Print the stack trace if requested
        if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();

        // Show the dialog
        final String htmlMessage = SysUtil.htmlStringFromStackTrace(e);

        _showOKDialogWithScrollPane(Texts.ScrEdt_Error, htmlMessage, JOptionPane.ERROR_MESSAGE, null);
    }

    private static void _showAboutDialogWithScrollPane()
    { _showOKDialogWithScrollPane( Texts.ScrEdt_About, SysUtil.jxmAboutStringHTML(), JOptionPane.INFORMATION_MESSAGE, IconJxMakeLogo ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _showOKDialog(final String title, final String htmlMessage, final int type)
    {
        final boolean anyVisible = Arrays.stream( Window.getWindows() ).anyMatch(Window::isShowing);

        if( anyVisibleWindows() ) {
                JOptionPane.showMessageDialog(null, htmlMessage, title, type);
        }
        else {
            SwingUtilities.invokeLater( () -> {
                JOptionPane.showMessageDialog(null, htmlMessage, title, type);
            } );
        }
    }

    private static void _showOKErrorDialog(final String title, final String htmlMessage)
    { _showOKDialog(title, htmlMessage, JOptionPane.ERROR_MESSAGE); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean _showYesNoDialog(final String title, final String htmlMessage, final int type)
    {
        final boolean anyVisible = Arrays.stream( Window.getWindows() ).anyMatch(Window::isShowing);

        final XCom.IntegerRef yes = new XCom.IntegerRef(0);

        if( anyVisibleWindows() ) {
                final int res = JOptionPane.showConfirmDialog(null, htmlMessage, title, JOptionPane.YES_NO_OPTION, type);
                if(res == JOptionPane.YES_OPTION) yes.set(1);
        }
        else {
            SwingUtilities.invokeLater( () -> {
                final int res = JOptionPane.showConfirmDialog(null, htmlMessage, title, JOptionPane.YES_NO_OPTION, type);
                if(res == JOptionPane.YES_OPTION) yes.set(1);
            } );
        }

        return yes.get() == 1;
    }

    private static boolean _showYesNoWarningDialog(final String title, final String htmlMessage)
    { return _showYesNoDialog(title, htmlMessage, JOptionPane.WARNING_MESSAGE); }

    private static boolean _showYesNoQuestionDialog(final String title, final String htmlMessage)
    { return _showYesNoDialog(title, htmlMessage, JOptionPane.QUESTION_MESSAGE); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void showExceptionDialogWithScrollPane(final Exception e)
    { _showExceptionDialogWithScrollPane(e); }

    public static void showOKErrorDialog(final String title, final String htmlMessage)
    { _showOKErrorDialog(title, htmlMessage); }

    public static boolean showYesNoWarningDialog(final String title, final String htmlMessage)
    { return _showYesNoWarningDialog(title, htmlMessage); }

    public static boolean showYesNoQuestionDialog(final String title, final String htmlMessage)
    { return _showYesNoQuestionDialog(title, htmlMessage); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class ExtListCellRenderer extends JLabel implements ListCellRenderer<String> {

        public static final String ItemNAMarker  = "\000!\000";
        public static final String SeparatorLine = "\000─\000";
        public static final String NBSP3         = "\u00A0\u00A0\u00A0";

        private Font  _txtFont    = null;
        private Font  _sepFont    = null;
        private Color _bgCol      = null;
        private Color _fgCol      = null;
        private Color _fgColNA    = null;
        private Color _bgSelCol   = null;
        private Color _fgSelCol   = null;
        private Color _fgSelColNA = null;

        public ExtListCellRenderer()
        { setOpaque(true); }

        @Override
        public Component getListCellRendererComponent
        (
            final JList<? extends String> list,
            final String                  value,
            final int                     index,
            final boolean                 isSelected,
            final boolean                 cellHasFocus
        ) {

            if(_txtFont == null) {
                _txtFont    = list.getFont();
                _sepFont    = _txtFont.deriveFont( _txtFont.getSize() / 2.0f );
                _bgCol      = list.getBackground();
                _fgCol      = list.getForeground();
                _fgColNA    = SwingApp.rgbDeriveColor(_fgCol   , Color.RED, _bgCol   );
                _bgSelCol   = list.getSelectionBackground();
                _fgSelCol   = list.getSelectionForeground();
                _fgSelColNA = SwingApp.rgbDeriveColor(_fgSelCol, Color.RED, _bgSelCol);
            }

            final boolean isSepLine = SeparatorLine.equals(value);

            if(isSepLine) {
                setFont       (_sepFont);
                setBackground (_bgCol  );
                setForeground (_fgCol  );
                setText       (NBSP3   );
                setToolTipText(""      );
            }
            else {
                    setFont       ( _txtFont                                 );
                    setBackground ( isSelected ? _bgSelCol   : _bgCol        );

                if( value.startsWith(ItemNAMarker) ) {
                    setForeground ( isSelected ? _fgSelColNA : _fgColNA      );
                    setText       ( value.substring( ItemNAMarker.length() ) );
                    setToolTipText( isSelected ? Texts.ScrEdt_dlNA : ""      );
                }

                else {
                    setForeground ( isSelected ? _fgSelCol   : _fgCol        );
                    setText       ( value                                    );
                    setToolTipText( ""                                       );
                }
            }

            return this;
        }

        @Override
        protected void paintComponent(final Graphics g)
        {
            super.paintComponent(g);

            if( !NBSP3.equals( getText() ) ) return;

            g.setColor( getForeground() );

            final int y = getHeight() / 2;
            g.drawLine(0, y, getWidth(), y);
        }

    } // class ExtListCellRenderer

    private static void _findButtons(final Container container, final List<JButton> found)
    {
        for( final Component c : container.getComponents() ) {
                 if(c instanceof JButton  ) found.add   ( (JButton  ) c        );
            else if(c instanceof Container) _findButtons( (Container) c, found );
        }
    }

    public XCom.Pair<Integer, String> showOkCancelJListDialog(
            final String   title,
            final String[] values,
            final int      initialSelIdx,
            final JButton  extraButton,
            final boolean  useExtListCellRenderer
    ) {
        // Create the list
        final DefaultListModel<String> model = new DefaultListModel<>();
        for(final String v : values) model.addElement(v);

        final JList<String> list = new JList<>(model);
        final Font          font = list.getFont();
            list.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5) );
            list.setFont( new Font( "Monospaced", font.getStyle(), font.getSize() ) );
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setSelectedIndex(initialSelIdx);
            list.setVisibleRowCount(15);

        final JScrollPane scrollPane = new JScrollPane(list);

        // Build the dialog
        JOptionPane optionPane = null;
        JDialog     dialog     = null;

        if(extraButton == null) {
            optionPane = new JOptionPane(scrollPane, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null );
            dialog     = optionPane.createDialog(title);
        }

        else {
            // Arange the layout
            final JPanel mainPanel   = new JPanel( new BorderLayout(0, 10) );
            final JPanel southPanel  = new JPanel(                         );
            final JPanel centerPanel = new JPanel( new BorderLayout(5,  5) );

            centerPanel.add(scrollPane , BorderLayout.CENTER);
            centerPanel.add(extraButton, BorderLayout.SOUTH );

            mainPanel  .add(centerPanel, BorderLayout.CENTER);
            mainPanel  .add(southPanel , BorderLayout.SOUTH );

            optionPane = new JOptionPane(mainPanel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null );
            dialog     = optionPane.createDialog(title);

            extraButton.putClientProperty("list", list);

            // Move the default buttons
            final String        okText      = UIManager.getString("OptionPane.okButtonText"    );
            final String        cancelText  = UIManager.getString("OptionPane.cancelButtonText");

            final Container     contentPane = dialog.getContentPane();
            final List<JButton> buttons     = new ArrayList<>();

            _findButtons(contentPane, buttons);

            JButton btnOK     = null;
            JButton btnCancel = null;
            for(final JButton button : buttons) {

                String text = button.getText();

                if( okText    .equals(text) ) btnOK     = button;
                if( cancelText.equals(text) ) btnCancel = button;

                if( btnOK != null && btnCancel != null ) break;

            } // for

            if(btnOK != null && btnCancel != null) {

                final Rectangle okBounds     = btnOK.getBounds();
                final Rectangle cancelBounds = btnCancel.getBounds();
                final int       distance     = cancelBounds.x - (okBounds.x + okBounds.width);

                southPanel.setLayout( new GridLayout(1, 2, distance, 0) );

                btnOK    .getParent().remove(btnOK    ); southPanel.add(btnOK    );
                btnCancel.getParent().remove(btnCancel); southPanel.add(btnCancel);
            }

            contentPane.revalidate();
        }

        // Add event handler on the list
        final JOptionPane optionPane_ = optionPane;
        final JDialog     dialog_     = dialog;

        // keep track of last valid selection
        final int[] lastValidIndex = { initialSelIdx };

        if(useExtListCellRenderer) {

            list.setCellRenderer( new ExtListCellRenderer() );

            list.addListSelectionListener( e -> {

                if( !e.getValueIsAdjusting() ) {
                    final int idx = list.getSelectedIndex();
                    if(idx < 0) return;

                    final String val = list.getModel().getElementAt(idx);
                    if( val == null || val.trim().isEmpty() ) {
                        if( lastValidIndex[0] >= 0 ) list.setSelectedIndex(lastValidIndex[0]);
                    }
                    else {
                        lastValidIndex[0] = idx;
                    }

                }
            } );

            list.addKeyListener( new KeyAdapter() {
                @Override
                public void keyPressed(final KeyEvent e)
                {
                    final int idx = list.getSelectedIndex();
                    if(idx < 0) return;

                    if( e.getKeyCode() == KeyEvent.VK_DOWN ) {
                        int next = idx + 1;
                        while( next < list.getModel().getSize() && list.getModel().getElementAt(next).trim().isEmpty() ) {
                            ++next;
                        }
                        if( next < list.getModel().getSize() ) list.setSelectedIndex(next);
                        else                                   list.setSelectedIndex(lastValidIndex[0]);
                        e.consume();
                    }
                    else if( e.getKeyCode() == KeyEvent.VK_UP ) {
                        int prev = idx - 1;
                        while( prev >= 0 && list.getModel().getElementAt(prev).trim().isEmpty() ) {
                            --prev;
                        }
                        if(prev >= 0) list.setSelectedIndex(prev);
                        else          list.setSelectedIndex(lastValidIndex[0]);
                        e.consume();
                    }
                }
            } );

            list.addMouseListener( new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e)
                {
                    final int pntIdx = list.locationToIndex( e.getPoint() );
                    if( pntIdx < 0 || !list.getCellBounds(pntIdx, pntIdx).contains( e.getPoint() ) ) return;

                    ToolTipManager.sharedInstance().mouseMoved( new MouseEvent(
                        list,
                        MouseEvent.MOUSE_MOVED,
                        System.currentTimeMillis(),
                        0,
                        e.getX(), e.getY(),
                        e.getXOnScreen(), e.getYOnScreen(),
                        e.getClickCount(),
                        false,
                        e.getButton()
                   ) );
                }
            } );

        } // if

        list.addMouseListener( new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {

                if( e.getClickCount() != 2 ) return;

                final int pntIdx = list.locationToIndex( e.getPoint() );
                if( pntIdx < 0 || !list.getCellBounds(pntIdx, pntIdx).contains( e.getPoint() ) ) return;

                if( list.locationToIndex( e.getPoint() ) < 0 ) return;

                if( list.isSelectionEmpty() ) {
                    optionPane_.setValue(JOptionPane.CLOSED_OPTION);
                }
                else {
                     final int idx = list.locationToIndex( e.getPoint() );
                     if(idx < 0) return;

                     final String sel = list.getModel().getElementAt(idx);
                     if( sel == null || sel.trim().isEmpty() ) return;
                }

                optionPane_.setValue(JOptionPane.OK_OPTION);
                dialog_.dispose();

            }
        } );

        // Show the dialog
        dialog.setVisible(true);

        if( optionPane.getValue() == null || ( (Integer) optionPane.getValue() ) != JOptionPane.OK_OPTION ) return null;

        // Return the selected item
        return new XCom.Pair<Integer, String>( list.getSelectedIndex(), list.getSelectedValue() );
    }

    public XCom.Pair<Integer, String> showOkCancelJListDialog(final String title, final String[] values, final int initialSelIdx, final JButton extraButton)
    { return showOkCancelJListDialog(title, values, initialSelIdx, extraButton, false                 ); }

    public XCom.Pair<Integer, String> showOkCancelJListDialog(final String title, final String[] values, final int initialSelIdx, final boolean useExtListCellRenderer)
    { return showOkCancelJListDialog(title, values, initialSelIdx, null       , useExtListCellRenderer); }

    public XCom.Pair<Integer, String> showOkCancelJListDialog(final String title, final String[] values, final int initialSelIdx)
    { return showOkCancelJListDialog(title, values, initialSelIdx, null       , false                 ); }

    public XCom.Pair<Integer, String> showOkCancelJListDialog(final String title, final String[] values)
    { return showOkCancelJListDialog(title, values, 0            , null       , false                 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean                _useDCT         = false;

    private Config                 _config         = null;

    private JScrollPane            _fileListScroll = null;
    private JxMakeFileList         _fileList       = null;

    private JSplitPane             _splitPane      = null;
    private JxMakeConsole          _console        = null;
    private RTextScrollPane        _textAreaScroll = null;
    private RSyntaxTextArea        _textArea       = null;
    private ErrorStrip             _errorStrip     = null;

    private JPanel                 _statusBar      = null;
    private JLabel                 _sbLabel        = null;

    private FindDialog             _findDialog     = null;
    private ReplaceDialog          _replaceDialog  = null;

    private SpellingParser         _spellingParser = null;

    private void _createNewEditor_impl()
    {
        // Initialize RSyntaxTextArea
        _textArea = new RSyntaxTextArea(0, 0) {
            @Override
            public int getLineHeight()
            { return super.getLineHeight() * 12 / 10; }

            {
                // Instance initializer block runs after construction
                getInputMap ().put( KeyStroke.getKeyStroke(MenuSpec.VK_C, MenuSpec.KModCtrl), "rstaPlainCopy" );
                getActionMap().put( "rstaPlainCopy", new AbstractAction() {
                    @Override
                    public void actionPerformed(final ActionEvent e)
                    { copy(); }
                } );
            }

        };

            _textArea.enableInputMethods               (true                         );
            _textArea.setTabSize                       (4                            );
            _textArea.setCaretPosition                 (0                            );
            _textArea.setMarkOccurrences               (true                         );
            _textArea.setCodeFoldingEnabled            (true                         );
            _textArea.setClearWhitespaceLinesEnabled   (false                        );
            _textArea.setSyntaxEditingStyle            (SYNTAX_STYLE_JXMAKEFILE      );

            _textArea.addMouseMotionListener           (_handle_textAreaMouseMoved   );
            _textArea.addMouseListener                 (_handle_textAreaMouseClicked );
            _textArea.addCaretListener                 (_handle_textAreaCaretMove    );
            _textArea.getDocument().addDocumentListener(_handle_textAreaDocumentEvent);

        _textAreaScroll = new RTextScrollPane(_textArea, true);

            _textAreaScroll.setWheelScrollingEnabled(true);

        final Gutter gutter = _textAreaScroll.getGutter();

            gutter.setBookmarkingEnabled(true);
            gutter.setBookmarkIcon(IconBookmark);
            gutter.setFoldIndicatorStyle(true ? FoldIndicatorStyle.MODERN : FoldIndicatorStyle.CLASSIC);

        _errorStrip = new ErrorStrip(_textArea);

        // Apply JxMake theme
        JxMakeTheme.apply(this, _useDCT);
    }

    private void _initializeNewEditor()
    {
        _createNewEditor_impl();

        if(_spellingParser != null && _config.spellCheckEnabled) _textArea.addParser(_spellingParser); // Add the spell checker

        _splitPane.setRightComponent(_textAreaScroll);

        getContentPane().add(_splitPane , BorderLayout.CENTER  );
      //getContentPane().add(_errorStrip, BorderLayout.LINE_END);
        getContentPane().add(_errorStrip, BorderLayout.EAST    );

        _textArea.requestFocusInWindow();
    }

    private void _switchEditor_impl()
    {
        _splitPane.setRightComponent(_textAreaScroll);

      //getContentPane().add(_errorStrip, BorderLayout.LINE_END);
        getContentPane().add(_errorStrip, BorderLayout.EAST    );

        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private void _switchEditorToNew()
    {
        if(_spellingParser != null) _textArea.removeParser(_spellingParser); // Remove the spell checker

        _createNewEditor_impl();

        if(_spellingParser != null && _config.spellCheckEnabled) _textArea.addParser(_spellingParser); // Add the spell checker

        _switchEditor_impl();
    }

    private void _switchEditorTo(final FileState fileState)
    {
        getContentPane().remove(_errorStrip);

        if(_spellingParser != null) _textArea.removeParser(_spellingParser); // Remove the spell checker

        _textArea       = fileState.textArea;
        _textAreaScroll = fileState.textAreaScroll;
        _errorStrip     = fileState.errorStrip;

        if(_spellingParser != null && _config.spellCheckEnabled) _textArea.addParser(_spellingParser); // Add the spell checker

        _switchEditor_impl();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _spDicDir = null;

    private static boolean _isAmericas(final String country)
    {
        return new String[] {

            "AR","BR","CL","CO","PE","VE","EC","UY","BO","PY",
            "CR","GT","HN","NI","PA","DO","CU"

        }.equals(country);
    }

    private static boolean _isEastAsia(final String country)
    {
        return new String[] {
            "CN","JP","KR","TW"
        }.equals(country);
    }

    public static boolean _use_en_US()
    {
        final String country = SysUtil._JxMakeCountry;

        switch(country) {

            // en_US regions
            case "US" : /* FALLTHROUGH */
            case "CA" : /* FALLTHROUGH */
            case "PH" : /* FALLTHROUGH */
            case "MX" : /* FALLTHROUGH */
            case "JP" : /* FALLTHROUGH */
            case "KR" : /* FALLTHROUGH */
            case "TW" : return true; // en_US

            // en_GB regions
            case "GB" : /* FALLTHROUGH */
            case "IE" : /* FALLTHROUGH */
            case "AU" : /* FALLTHROUGH */
            case "NZ" : /* FALLTHROUGH */
            case "ZA" : /* FALLTHROUGH */
            case "IN" : /* FALLTHROUGH */
            case "PK" : /* FALLTHROUGH */
            case "SG" : /* FALLTHROUGH */
            case "HK" : return false; // en_GB

            default   :
                if( _isAmericas(country) || _isEastAsia(country) ) return true ; // en_US
                else                                               return false; // en_GB

        } // switch
    }

    private static void _configureSpellingParser(final SpellingParser spellingParser) throws IOException
    {
        spellingParser.setAllowAdd(false);
        spellingParser.setAllowIgnore(false);

        spellingParser.setSpellCheckableTokenIdentifier( new SpellCheckableTokenIdentifier() {
            @Override
            public void begin()
            {}

            @Override
            public void end()
            {}

            @Override
            public boolean isSpellCheckable(final Token t)
            { return t.getType() == TType.Comment || t.getType() == TType.String; }
        } );
    }

    private static SpellingParser _createSpellingParser(final String zipFileName, final String[] dicNames, final boolean showErrorDialog)
    {
        // Initialize spell checker
        try {

            // Find the dictionary directory
            if(_spDicDir == null) _spDicDir = SysUtil.findSpellingParserDicDir();
            if(_spDicDir == null) return null;

            // Load the dictionary
            final String                 zipFilePath = SysUtil.resolvePath(zipFileName, _spDicDir);
            final SpellDictionaryHashMap dic         = new SpellDictionaryHashMap();

            try( final ZipFile zf = new ZipFile( new File(zipFilePath) ) ) {

                for(final String name : dicNames) {
                    final InputStream is = zf.getInputStream( zf.getEntry( ( name.indexOf('.') != -1 ) ? name : (name + ".dic") ) );
                    try( final BufferedReader br = new BufferedReader( new InputStreamReader(is) ) ) {
                        dic.addDictionary(br);
                    }
                }

            } // try

            // Create, configure, and return the spell checker
            final SpellingParser spellingParser = new SpellingParser(dic);

            _configureSpellingParser(spellingParser);

            return spellingParser;

        }
        catch(final Exception e) {
            if(showErrorDialog) {
                _showExceptionDialogWithScrollPane(e);
            }
            else {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }
        }

        return null;
    }

    private static SpellingParser _createSpellingParser_DefaultEnglish_impl(final boolean enUS, final boolean showErrorDialog)
    {
        // https://github.com/bobbylight/SpellChecker/tree/master/SpellChecker/src/main/dist/english_dic

        return _createSpellingParser(
            "default_english_dic.zip"
            ,
            enUS ? new String[] {
                       "eng_com"    ,
                       "color"      , "labeled" , "center", "ize", "yze", // American English
                       "programming"
                   }
                 : new String[] {
                       "eng_com"    ,
                       "colour"     , "labelled", "centre", "ise", "yse", // British  English
                       "programming"
                   }
            ,
            showErrorDialog
        );
    }

    private static SpellingParser _createSpellingParser_DefaultEnglish_auto(final boolean showErrorDialog)
    { return _createSpellingParser_DefaultEnglish_impl( _use_en_US(), showErrorDialog ); }

    private static SpellingParser _createSpellingParser_DefaultEnglish_US(final boolean showErrorDialog)
    { return _createSpellingParser_DefaultEnglish_impl( true        , showErrorDialog ); }

    private static SpellingParser _createSpellingParser_DefaultEnglish_GB(final boolean showErrorDialog)
    { return _createSpellingParser_DefaultEnglish_impl( false       , showErrorDialog ); }

    /*
    ##### ??? TODO : Is it even possible to do this using 'SpellDictionaryHashMap' and 'SpellingParser' ??? #####

    ja_JP_kana
    日本語（かなのみ）
    日本
    https://github.com/Ajatt-Tools/hunspell-ja/tree/v1

    ja_JP_kana
    Japanese (Kana Only)
    Japan
    https://github.com/Ajatt-Tools/hunspell-ja/tree/v1

        ja_JP_kana.zip
        https://github.com/Ajatt-Tools/hunspell-ja/archive/refs/tags/v1.tar.gz
            License not specified in upstream repository (please contact me if the author wishes this entry to be removed)
            https://github.com/Ajatt-Tools/hunspell-ja/blob/v1/README.md
            The upstream repository is based on:
                https://github.com/MrCorn0-0/hunspell_ja_JP
                https://github.com/epistularum/hunspell-ja-deinflection
    */

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static JxMakeRootPane           _instance         = null;

    private final  ScheduledExecutorService _update_scheduler = Executors.newScheduledThreadPool(1);

    @SuppressWarnings("this-escape")
    public JxMakeRootPane(final boolean useDarkColorTheme, final String initialFilePath)
    {
        super();

        _instance = this;
        _useDCT   = useDarkColorTheme;

        // Load the config
        _config = Config.load();

        // Initialize menu bar
        JxMakeRootPane_Menu.createMenuBar(this);

        if( _createSpellingParser_DefaultEnglish_auto(false) == null ) {
            JxMakeRootPane_Menu.chkSetEnabled (JxMakeRootPane_Action.ID_viewSpellChk, false);
            JxMakeRootPane_Menu.itmSetEnabled (JxMakeRootPane_Action.ID_viewChgDict , false);
        }
        else {
            JxMakeRootPane_Menu.chkSetSelected(JxMakeRootPane_Action.ID_viewSpellChk, _config.spellCheckEnabled);
        }

        // Initialize the spell checker after the window is shown
        this.addComponentListener( new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e)
            {
                if( JxMakeRootPane.this.isShowing() ) {
                    SwingUtilities.invokeLater( () -> {
                        _handle_mnuViewChangeDict_impl(_config.spellCheckSelDic, 0);
                    } );
                    JxMakeRootPane.this.removeComponentListener(this);
                }
            }
        } );

        // Initialize split pane
        _splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

            _splitPane.setOneTouchExpandable(true);
            _splitPane.setContinuousLayout(false);
          //_splitPane.setDividerSize( _splitPane.getDividerSize() * 2 );

            _splitPane.addPropertyChangeListener( JSplitPane.DIVIDER_LOCATION_PROPERTY, e -> {
                final int pos = (Integer) e.getNewValue();
                //if( pos > 0 && _fileListScroll != null && !_fileList.isEmpty() ) _lastDividerLocation = pos;
            } );

        // Initialize file browser
        _fileList = new JxMakeFileList(FileWatcherPollDelay_MS);

            _fileList.tree.addTreeSelectionListener(_handle_fileListTreeSelectionListener);
            _fileList.selectAndScrollToConsole();

        _fileListScroll = new JScrollPane(_fileList.tree);

        _splitPane.setLeftComponent(_fileListScroll);

        // Initialize console
        _console = new JxMakeConsole(_useDCT);

        // Initialize new editor
        _initializeNewEditor();

        // Initialize the status bar
        _statusBar = new JPanel();

            _statusBar.setLayout( new BoxLayout(_statusBar, BoxLayout.X_AXIS) );
            _statusBar.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5) );

            _sbLabel = new JLabel();

            _statusBar.add( _sbLabel                   );
            _statusBar.add( Box.createHorizontalGlue() );
            _statusBar.add( new JLabel("\u00A0")       );

        getContentPane().add( new JScrollPane(_statusBar), BorderLayout.SOUTH );

        // Initialize find dialog and replace dialog
        _findDialog    = new FindDialog   ( mainWindow(), this );
        _replaceDialog = new ReplaceDialog( mainWindow(), this );

        _replaceDialog.setSearchContext( _findDialog.getSearchContext() );

        // Map actions
        JxMakeRootPane_Action.mapKeyboardAction(this);
        JxMakeRootPane_Action.mapMouseAction   (this);

        // Load the initial file as needed
        loadMainFile(initialFilePath);

        // Adjust the split pane position later
        SwingUtilities.invokeLater(_adjust_splitPaneDivider);

        // Initialize update scheduler
        final XCom.IntegerRef updateCtr = new XCom.IntegerRef(0);

        _update_scheduler.scheduleAtFixedRate(
            () -> {
                SwingUtilities.invokeLater( () -> {
                    _console.handleUpdate( ( updateCtr.get() % ANSITextBlinkDelayCnt ) == 0 );
                    updateCtr.inc();
                } );
            },
            0, ANSIScreenUpdateRate_MS, TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void doLayout()
    {
        final Dimension size = getSize();

        getLayeredPane().setBounds(0, 0, size.width, size.height);
        getGlassPane  ().setBounds(0, 0, size.width, size.height);

        int menuHeight = 0;
        if( getJMenuBar() != null && getJMenuBar().isVisible() ) {
            final Dimension menuSize = getJMenuBar().getPreferredSize();
            getJMenuBar().setBounds(0, 0, size.width, menuSize.height);
            menuHeight = menuSize.height;
        }

        if( getContentPane() != null )  getContentPane().setBounds(0, menuHeight, size.width, size.height - menuHeight);
    }

    public RSyntaxTextArea getTextArea()
    { return _textArea; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public SwingApp.MainWindow mainWindow()
    { return SwingApp.getWindowAncestorFrom(this); }

    public JxMakeFileList fileList()
    { return _fileList; }

    public RSyntaxTextArea textArea()
    { return _textArea; }

    public RTextScrollPane textAreaScrollPane()
    { return _textAreaScroll; }

    public ErrorStrip errorStrip()
    { return _errorStrip; }

    public SpellingParser spellingParser()
    { return _spellingParser; }

    @Override
    public String getSelectedText()
    { return _textArea.getSelectedText(); }

    @Override
    public void searchEvent(final SearchEvent e)
    {
        final SearchEvent.Type type    = e.getType();
        final SearchContext    context = e.getSearchContext();
              SearchResult     result  = null;

        switch(type) {

            default: /* FALLTHROUGH */
            case MARK_ALL: {
                    result = SearchEngine.markAll(_textArea, context);
                }
                break;

            case FIND: {
                    result = SearchEngine.find(_textArea, context);
                    if( !result.wasFound() || result.isWrapped() ) UIManager.getLookAndFeel().provideErrorFeedback(_textArea);
                }
                break;

            case REPLACE: {
                    result = SearchEngine.replace(_textArea, context);
                    if( !result.wasFound() || result.isWrapped() ) UIManager.getLookAndFeel().provideErrorFeedback(_textArea);
                }
                break;

            case REPLACE_ALL: {
                    final String currentStyle = _textArea.getSyntaxEditingStyle();
                    _textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);

                    result = SearchEngine.replaceAll(_textArea, context);

                    _textArea.setSyntaxEditingStyle(currentStyle);
                }
                break;

        } // switch

        _sbLabel.setText("");

        if( result.wasFound() ) _sbLabel.setText( Texts.ScrEdt_FindMarked + result.getMarkedCount() );
        else                    _sbLabel.setText( Texts.ScrEdt_FindNone                             );
    }

    public boolean confirmQuit()
    {
        // Confirm first
        if( (  _fileList.mainFile.isModifiedSS()                                             ) ||
            ( !_fileList.mainFile.isFileSpecified() && !_textArea.getText().trim().isEmpty() )
        ) {
            if( !_showYesNoWarningDialog(Texts.ScrEdt_Warning, Texts.ScrEdt_YN_ModQuitSS) ) return false;
        }

        // Shutdown all schedulers
        _update_scheduler.shutdown();
        _updateInclude_scheduler.shutdown();

        // Ensure the saved file state is up-to-date
        final FileItem fileItem = _fileList.getSelectedFileItem();
        if( fileItem.isFileSpecified() ) fileItem.saveFileState(JxMakeRootPane.this);

        _fileList.clearAllFileState();
        _fileList.shutdown();

        // Confirm quit
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _lastDividerLocation = -1;

    private final Runnable _adjust_splitPaneDivider = new Runnable() {
        @Override
        public void run()
        {
            if( _fileListScroll == null || mainWindow() == null || !mainWindow().isVisible() ) return;

            final int autoPos = _fileListScroll.getPreferredSize().width;

            _splitPane.revalidate();

                 if( _lastDividerLocation != -1 ) _splitPane.setDividerLocation(_lastDividerLocation);
            else if( _fileList.isEmpty()        ) _splitPane.setDividerLocation(0.2f                );
            else                                  _splitPane.setDividerLocation(autoPos             );

           _lastDividerLocation = _splitPane.getDividerLocation();
        }
    };

    private final TreeSelectionListener _handle_fileListTreeSelectionListener = new TreeSelectionListener() {
        @Override
        public void valueChanged(final TreeSelectionEvent e)
        {
            synchronized(JxMakeRootPane.this) {

                if(_fileList == null || _splitPane == null || _errorStrip == null) return;

                final TreePath               oldPath = e.getOldLeadSelectionPath();
                final TreePath               newPath = e.getPath(); //getNewLeadSelectionPath();

                final DefaultMutableTreeNode prvNode = JxMakeFileList.getTreeNodeFrom(oldPath);
                final DefaultMutableTreeNode curNode = JxMakeFileList.getTreeNodeFrom(newPath);

                /*
                final TreePath[] paths = _fileList.tree.getSelectionPaths();
                if( paths != null && paths.length > 1 ) _fileList.tree.setSelectionPath( paths[paths.length - 1] );
                _fileList.tree.invalidate();
                _fileList.tree.repaint();
                //*/

                final String[] mnuFileExcepts = {
                    JxMakeRootPane_Action.ID_filePrintFmt,
                    JxMakeRootPane_Action.ID_filePrintTxt,
                    JxMakeRootPane_Action.ID_fileQuit
                };

                final String[] mnuHelpExcepts = {
                    JxMakeRootPane_Action.ID_helpAbout
                };

                if( curNode == _fileList.console.treeNode() ) {
                    if(_console != null) {
                        // Change the file item
                        _changeFileItem( JxMakeFileList.getFileItemNodeFor(prvNode), JxMakeFileList.getFileItemNodeFor(curNode) );
                        // Change the visible components
                        _splitPane.setRightComponent( _console.jPanel() );
                        _errorStrip.setVisible(false);
                        // Disable most menu items in console mode
                        JxMakeRootPane_Menu.itmSetEnableAllExcept(JxMakeRootPane_Action.ID_file, false, mnuFileExcepts);
                        JxMakeRootPane_Menu.itmSetEnableAllExcept(JxMakeRootPane_Action.ID_help, false, mnuHelpExcepts);
                        JxMakeRootPane_Menu.itmSetEnabled        (JxMakeRootPane_Action.ID_edit, false                );
                        JxMakeRootPane_Menu.itmSetEnabled        (JxMakeRootPane_Action.ID_view, false                );
                        // Adjust the divider
                        _adjust_splitPaneDivider.run();
                    }
                }

                else {
                        // Change the file item
                        _changeFileItem( JxMakeFileList.getFileItemNodeFor(prvNode), JxMakeFileList.getFileItemNodeFor(curNode) );
                        // Change the visible components
                        _splitPane.setRightComponent(_textAreaScroll);
                        _errorStrip.setVisible(true);
                        // Re-enable the menu items that were disabled in console mode
                        JxMakeRootPane_Menu.itmSetEnableAllExcept(JxMakeRootPane_Action.ID_file, true, mnuFileExcepts);
                        JxMakeRootPane_Menu.itmSetEnableAllExcept(JxMakeRootPane_Action.ID_help, true, mnuHelpExcepts);
                        JxMakeRootPane_Menu.itmSetEnabled        (JxMakeRootPane_Action.ID_edit, true                );
                        JxMakeRootPane_Menu.itmSetEnabled        (JxMakeRootPane_Action.ID_view, true                );
                        // Adjust the divider
                        _adjust_splitPaneDivider.run();
                }

            } // synchronized
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final ScheduledExecutorService _updateInclude_scheduler   = Executors.newSingleThreadScheduledExecutor();
    private       ScheduledFuture<?>       _updateInclude_pendingTask = null;
    private       long                     _updateInclude_lastRunTime = 0;

    private void _runIncludeScan_impl()
    {
        synchronized (JxMakeRootPane.this) {

            if( !_fileList.mainFile.isFileSpecified() && !_textArea.getText().trim().isEmpty() ) {
                // Set the main file icon in the file list to modified even it is not saved yet
                if( !_fileList.mainFileUnsavedModified ) {
                    _fileList.mainFileUnsavedModified = true;
                    _fileList.mainFile.refreshNodeData();
                }
            }

            try {
                // Scan include file(s)
                _scanIncludeFiles();
                // Update the last run timestamp
                _updateInclude_lastRunTime = SysUtil.getMS();
                _updateInclude_pendingTask = null;
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }

        }
    }

    private void _scheduleIncludeScan_impl(final long delay)
    {
        if( _updateInclude_pendingTask != null && !_updateInclude_pendingTask.isDone() ) {
            _updateInclude_pendingTask.cancel(false);
        }

        _updateInclude_pendingTask = _updateInclude_scheduler.schedule(
            this::_runIncludeScan_impl, delay, TimeUnit.MILLISECONDS
        );
    }

    private final CaretListener _handle_textAreaCaretMove = new CaretListener() {
        @Override
        public void caretUpdate(final CaretEvent event)
        {
            synchronized (JxMakeRootPane.this) {

                final long scheduledAt = SysUtil.getMS();
                final long elapsed     = scheduledAt - _updateInclude_lastRunTime;

                // If within throttle window, reschedule after the remaining time
                if(elapsed < UpdateThrottleWindow_MS) {
                    _scheduleIncludeScan_impl(UpdateThrottleWindow_MS - elapsed);
                }
                // Schedule new task in UpdateIncludeDelay_MS
                else {
                    _scheduleIncludeScan_impl(UpdateIncludeDelay_MS);
                }

            } // synchronized
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final MouseMotionAdapter _handle_textAreaMouseMoved = new MouseMotionAdapter() {
        @Override
        public void mouseMoved(final MouseEvent e)
        {
            boolean handCursor = ( ( e.getModifiersEx() & MenuSpec.KModCtrl ) != 0 ) ? true : false;

            if(handCursor) {
                final Token token = _textArea.viewToToken( e.getPoint() );
                if( token == null || token.getType() != TType.IncLibName || !token.isHyperlink() ) handCursor = false;
            }

            if(handCursor) _textArea.setCursor( Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) );
            else           _textArea.setCursor( Cursor.getDefaultCursor()                      );
        }
    };

    private final MouseAdapter _handle_textAreaMouseClicked = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e)
        {
            // Get the include name
            if( ( e.getModifiersEx() & MenuSpec.KModCtrl ) == 0 ) return;

            final Token token = _textArea.viewToToken( e.getPoint() );
            if( token == null || token.getType() != TType.IncLibName || !token.isHyperlink() ) return;

            final String tok = token.getLexeme();
            if( tok == null || tok.isEmpty() || tok.charAt(0) == '<' ) return;

            final String incName = tok.substring( 1, tok.length() - 1 );

            synchronized(JxMakeRootPane.this) {

                final FileItem pfi = _fileList.getSelectedFileItem();
                      FileItem cfi = pfi.getSubFileFor(incName);

                if(cfi == null) cfi = pfi.getParentFileFor(incName);

                if(cfi == null) {

                    final String absNewFilePath = SysUtil.resolvePath( incName, pfi.absDirPath() );

                    if( !_showYesNoQuestionDialog(Texts.ScrEdt_Warning, String.format(Texts.ScrEdt_YN_CrtNIFile, incName, absNewFilePath) ) ) return;

                    try {
                        SysUtil.cu_touch(absNewFilePath);
                        _scanIncludeFiles();
                    }
                    catch(final Exception ex) {
                        _showExceptionDialogWithScrollPane(ex);
                        return;
                    }
                }

                _fileList.selectAndScrollTo(cfi);
                _changeFileItem(pfi, cfi);

            } // synchronized
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final DocumentListener _handle_textAreaDocumentEvent = new DocumentListener() {
        @Override public void insertUpdate (final DocumentEvent e) { _markModified(); }
        @Override public void removeUpdate (final DocumentEvent e) { _markModified(); }
        @Override public void changedUpdate(final DocumentEvent e) {                  }

        private void _markModified()
        {
            final FileItem fileItem = _fileList.getSelectedFileItem();

            if( fileItem.isModified() ) return;

            fileItem.setModified();
            fileItem.refreshNodeData();
        }
    };

    private static class PlainTextPrintable implements Printable {

        private final String[] _lines;
        private final Font     _font;

        public PlainTextPrintable(final String text, final Font font) {
            _lines = text.split("\n");
            _font  = font;
        }

        @Override
        public int print(final Graphics g, final PageFormat pf, final int pageIndex) throws PrinterException
        {
            final Graphics2D g2 = (Graphics2D) g;

            g2.setFont(_font);
            g2.setColor(Color.BLACK);

            final FontMetrics fm         = g2.getFontMetrics();
            final int         lineHeight = fm.getHeight();

            // Calculate how many lines fit per page
            final int usableHeight = (int) pf.getImageableHeight();
            final int linesPerPage = usableHeight / lineHeight;

            if(linesPerPage <= 0) return NO_SUCH_PAGE;

            final int totalPages = (int) Math.ceil( (float) _lines.length / linesPerPage );

            if(pageIndex >= totalPages) return NO_SUCH_PAGE;

            int x = (int) pf.getImageableX();
            int y = (int) pf.getImageableY() + fm.getAscent();

            final int startLine = pageIndex * linesPerPage;
            final int endLine   = Math.min(startLine + linesPerPage, _lines.length);

            for(int i = startLine; i < endLine; ++i) {
                g2.drawString(_lines[i], x, y);
                y += lineHeight;
            }

            return PAGE_EXISTS;
        }

    } // class PlainTextPrintable

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final String _SpcInd_OpenFile_ = "\0_SpcInd_OpenFile_\0";
    public void handle_mnuFile()
    {
        if( _fileList.getSelectedTreePath().getLastPathComponent() == _fileList.console.treeNode() ) return;

        JxMakeRootPane_Menu.itmSetEnabled( JxMakeRootPane_Action.ID_fileOpenRecent, !_config.isRecentFileEmpty() );
    }

    public void handle_mnuFileNew()
    { loadMainFile(null); }

    public void handle_mnuFileOpen()
    { loadMainFile(_SpcInd_OpenFile_); }

    public void handle_mnuFileOpenRecent()
    {
        // Create the list
        final Map<String, String> rf = _config.getRecentFiles();
        if( rf == null || rf.isEmpty() ) return;

        final String[] displayFiles = new String[ rf.size() ];

        int pidx = 0;
        int sidx = 0;
        for( final String ap : rf.keySet() ) {
            if( ap.equals( _fileList.mainFile.absFullFilePath() ) ) sidx = pidx;
            displayFiles[pidx++] = ap;
        }

        // Show the dialog
        final JButton clearButton = new JButton(Texts.ScrEdt_OpnRecBtnClrL);

        clearButton.addActionListener( e -> {
            final JList<?> list = (JList<?>) clearButton.getClientProperty("list");
            if(list != null) ( (DefaultListModel) list.getModel() ).clear();
        } );

        final XCom.Pair<Integer, String> res = showOkCancelJListDialog(Texts.ScrEdt_OpnRecMScript, displayFiles, sidx, clearButton);
        if(res == null) return;

        if( res.first() < 0 || res.second() == null ) {
            _config.clearRecentFiles();
            return;
        }

        final String selectedAbsPath = res.second();
        final String selectedWorkDir = rf.get(selectedAbsPath);

        // Change directory as needed
        try {
            if( !selectedWorkDir.equals( SysUtil.getCWD() ) ) SysUtil.setCWD(selectedWorkDir);
        }
        catch(final Exception e) {
            _showExceptionDialogWithScrollPane(e);
        }

        // Load the main file
        loadMainFile(selectedAbsPath);
    }

    public void handle_mnuFileSave()
    { saveCurrentTextFile(); }

    public void handle_mnuFileSaveAll()
    { saveAllTextFiles(); }

    public void handle_mnuFileSaveAs()
    { saveCurrentTextFileAs(false); }

    public void handle_mnuFileReload()
    { reloadCurrentTextFile(); }

    public void handle_mnuFilePrintFmt()
    {
        /*
         * NOTE : PostScript output can be tested using:
         *            https://online2pdf.com/convert-ps-to-pdf
         *            https://cloudconvert.com/ps-to-pdf
         */

        SwingApp.saveCliboardContents();

        try {

            final boolean        selConsole    = _fileList.getSelectedFileItem() == _fileList.console;
            final JTextComponent textComponent = selConsole ? _console.jTextPane() : _textArea;

            if( selConsole && textComponent.getSelectedText() == null ) {
                _showOKErrorDialog(Texts.ScrEdt_Error, Texts.ScrEdt_NoPrintAllCon);
                return;
            }

            final PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
            attr.add(Chromaticity.COLOR);

            if(selConsole) {
                // Copy to clipboard
                final boolean html    = true;
                final Font    orgFont = _console.jTextPane().getFont();

                if(html) {
                    final Font derFont = orgFont.deriveFont( orgFont.getSize() * 0.5f );
                    _console.jTextPane().setFont(derFont);
                    _console.jTextPane().getActionMap().get(JxMakeConsole.Console_CopyAsStyledTextHTML).actionPerformed(
                        new ActionEvent(_textArea, ActionEvent.ACTION_PERFORMED, JxMakeConsole.Console_CopyAsStyledTextHTML)
                    );
                    _console.jTextPane().setFont(orgFont);
                }
                else { // RTF - NOTE : The result is not good!
                    _console.jTextPane().getActionMap().get(JxMakeConsole.Console_CopyAsStyledTextRTF).actionPerformed(
                        new ActionEvent(_textArea, ActionEvent.ACTION_PERFORMED, JxMakeConsole.Console_CopyAsStyledTextRTF)
                    );
                }

                // Print the clipboard
                if(html) SwingApp.printClipboardAsHTML(attr         , true       );
                else     SwingApp.printClipboardAsRTF (attr, orgFont, Color.BLACK);
            }

            else {
                // Copy to clipboard
                final boolean noSelection = _textArea.getSelectionStart() == _textArea.getSelectionEnd();

                if(noSelection) _textArea.selectAll();

                _textArea.getActionMap().get(JxMakeRootPane_Action.Action_copyAsStyledText).actionPerformed(
                    new ActionEvent( _textArea, ActionEvent.ACTION_PERFORMED, JxMakeRootPane_Action.Action_copyAsStyledText )
                );

                if(noSelection) _textArea.select( _textArea.getCaretPosition(), _textArea.getCaretPosition() );

                // Print the clipboard
                SwingApp.printClipboardAsRTF( attr, _textArea.getFont(), _useDCT ? Color.BLACK : Color.WHITE );
            }

        }
        catch(final Exception e) {
            _showExceptionDialogWithScrollPane(e);
        }

        SwingApp.restoreCliboardContents();
    }

    public void handle_mnuFilePrintTxt()
    {
        SwingApp.saveCliboardContents();

        try {
            final boolean        selConsole    = _fileList.getSelectedFileItem() == _fileList.console;
            final JTextComponent textComponent = selConsole ? _console.jTextPane() : _textArea;

            String text = textComponent.getSelectedText();
            if( text == null || text.isEmpty() ) {
                if(selConsole) {
                    _showOKErrorDialog(Texts.ScrEdt_Error, Texts.ScrEdt_NoPrintAllCon);
                    return;
                }
                text = textComponent.getDocument().getText( 0, textComponent.getDocument().getLength() );
            }

            if( text == null || text.isEmpty() ) {
                _showOKErrorDialog(Texts.ScrEdt_Error, Texts.ScrEdt_NoTextToPrint);
                return;
            }

            final PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
            attr.add(Chromaticity.MONOCHROME);

            final Font orgFont = textComponent.getFont();
            final Font derFont = selConsole ? orgFont.deriveFont( orgFont.getSize() * 0.5f ) : orgFont;

            SwingApp.printStringAsPlainText(text, attr, derFont, true);
        }
        catch(final Exception e) {
            _showExceptionDialogWithScrollPane(e);
        }

        SwingApp.restoreCliboardContents();
    }

    public void handle_mnuFile_Quit()
    { mainWindow().postEvent_WINDOW_CLOSING(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void handle_mnuEdit()
    {
        JxMakeRootPane_Menu.itmSetEnabled( JxMakeRootPane_Action.ID_editUndo, _textArea.canUndo() );
        JxMakeRootPane_Menu.itmSetEnabled( JxMakeRootPane_Action.ID_editRedo, _textArea.canRedo() );

        final boolean hasSel = _textArea.getSelectionStart() != _textArea.getSelectionEnd();
        JxMakeRootPane_Menu.itmSetEnabled(JxMakeRootPane_Action.ID_editClear, hasSel);
        JxMakeRootPane_Menu.itmSetEnabled(JxMakeRootPane_Action.ID_editCut  , hasSel);
        JxMakeRootPane_Menu.itmSetEnabled(JxMakeRootPane_Action.ID_editCopy , hasSel);

        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        final boolean   canPaste  = clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor);
        JxMakeRootPane_Menu.itmSetEnabled(JxMakeRootPane_Action.ID_editPaste, canPaste);
    }

    public void handle_mnuEdit_Undo()
    { _invokeAction(RSyntaxTextAreaEditorKit.rtaUndoAction); }

    public void handle_mnuEdit_Redo()
    { _invokeAction(RSyntaxTextAreaEditorKit.rtaRedoAction); }

    public void handle_mnuEdit_Clear()
    { _textArea.dispatchEvent( _newKeyEvent(MenuSpec.KModNone , KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED ) ); }

    public void handle_mnuEdit_Cut()
    { _textArea.cut(); }

    public void handle_mnuEdit_Copy()
    { _textArea.copy(); }

    public void handle_mnuEdit_Paste()
    { _textArea.paste(); }

    public void handle_mnuEdit_Indent()
    { _textArea.dispatchEvent( _newKeyEvent(MenuSpec.KModNone , KeyEvent.VK_TAB   , '\t'                    ) ); }

    public void handle_mnuEdit_Unindent()
    { _textArea.dispatchEvent( _newKeyEvent(MenuSpec.KModShift, KeyEvent.VK_TAB   , '\t'                    ) ); }

    public void handle_mnuEditComment()
    {
        try {
            final String   cmtStr    = TokenMakerFactory.getDefaultInstance().getTokenMaker( _textArea.getSyntaxEditingStyle() ).getLineCommentStartAndEnd(0)[0];
            final Document doc       = _textArea.getDocument();
            final Element  root      = doc.getDefaultRootElement();
            final int      startLine = root.getElementIndex( _textArea.getSelectionStart() );
            final int      endLine   = root.getElementIndex( _textArea.getSelectionEnd  () );
            for(int i = startLine; i <= endLine; ++i) {
                doc.insertString( root.getElement(i).getStartOffset(), cmtStr, null );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }
    }

    public void handle_mnuEditUncomment()
    {
        try {
            final String   cmtStr    = TokenMakerFactory.getDefaultInstance().getTokenMaker( _textArea.getSyntaxEditingStyle() ).getLineCommentStartAndEnd(0)[0];
            final Document doc       = _textArea.getDocument();
            final Element  root      = doc.getDefaultRootElement();
            final int      startLine = root.getElementIndex( _textArea.getSelectionStart() );
            final int      endLine   = root.getElementIndex( _textArea.getSelectionEnd  () );
            for(int i = startLine; i <= endLine; ++i) {
                final Element lineElem  = root.getElement(i);
                final int     lineStart = lineElem.getStartOffset();
                final int     lineEnd   = lineElem.getEndOffset  ();
                final String  lineText  = _textArea.getText(lineStart, lineEnd - lineStart);
                if( lineText.startsWith(cmtStr) ) doc.remove( lineStart, cmtStr.length() );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }
    }

    public void handle_mnuEditFind()
    {
         if( _replaceDialog.isVisible() ) _replaceDialog.setVisible(false);
        _findDialog.setVisible(true);
    }

    public void handle_mnuEditReplace()
    {
        if( _findDialog.isVisible() ) _findDialog.setVisible(false);
        _replaceDialog.setVisible(true);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _foldLevelRecursive(final Fold fold, int currentLevel, int targetLevel, final boolean collapsed)
    {
        if(targetLevel < 0 || currentLevel == targetLevel) fold.setCollapsed(collapsed);

        for( int i = 0; i < fold.getChildCount(); ++i ) _foldLevelRecursive( fold.getChild(i), currentLevel + 1, targetLevel, collapsed );
    }

    private void _foldLevel(final int targetLevel, final boolean collapsed)
    {
        final FoldManager fm = _textArea.getFoldManager();
     // fm.reparse();

        for( int i = 0; i < fm.getFoldCount(); ++i ) {
            Fold fold = fm.getFold(i);
            _foldLevelRecursive( fm.getFold(i), 0, targetLevel, collapsed );
        }

        _textArea.repaint();
    }

    public void handle_mnuView_FoldAll     () { _foldLevel(-1, true ); }
    public void handle_mnuView_FoldLevel1  () { _foldLevel( 0, true ); }
    public void handle_mnuView_FoldLevel2  () { _foldLevel( 1, true ); }
    public void handle_mnuView_FoldLevel3  () { _foldLevel( 2, true ); }

    public void handle_mnuView_UnfoldAll   () { _foldLevel(-1, false); }
    public void handle_mnuView_UnfoldLevel1() { _foldLevel( 0, false); }
    public void handle_mnuView_UnfoldLevel2() { _foldLevel( 1, false); }
    public void handle_mnuView_UnfoldLevel3() { _foldLevel( 2, false); }

    public void handle_mnuViewSpellCheck()
    {
        if( JxMakeRootPane_Menu.chkIsSelected(JxMakeRootPane_Action.ID_viewSpellChk) ) {
            _textArea.addParser(_spellingParser);
            _config.spellCheckEnabled = true;
        }
        else {
            _textArea.removeParser(_spellingParser);
            _config.spellCheckEnabled = false;
        }

        // Save the configuration
        _config.save();
    }

    private static String[] _getDic_parts(final String strEntry)
    { return strEntry.split("\\|", 2); }

    private static String _getDic_function(final String[] strParts)
    { return strParts[0].trim(); }

    private static String _getDic_displayOption(final String[] strParts)
    { return strParts[1]; }

    private void _handle_mnuViewChangeDict_impl(final String loadDic, final int recursiveDepth)
    {
        // Load from the dictionary list
        final ArrayList<String> dicList = new ArrayList<>();
        final String            spDir   = SysUtil.findSpellingParserDicDir();

        if(spDir != null) {

            final String dicListFile = SysUtil.resolvePath("LIST.txt", spDir);

            if( SysUtil.pathIsValidFile(dicListFile) ) {

                try( final InputStreamReader isr = new InputStreamReader( new FileInputStream(dicListFile) ) ) {

                    final BufferedReader reader = new BufferedReader(isr);

                    while(true) {

                        // Read the data
                        final String strFile  = reader.readLine(); if( strFile  == null || strFile .trim().isEmpty() ) break;
                        final String strLName = reader.readLine(); if( strLName == null || strLName.trim().isEmpty() ) break;
                        final String strCName = reader.readLine(); if( strCName == null || strCName.trim().isEmpty() ) break;
                        final String strDesc  = reader.readLine(); if( strDesc  == null || strDesc .trim().isEmpty() ) break;

                        // Check if the file exists
                        final boolean fileExists = SysUtil.pathIsValidFile( SysUtil.resolvePath(strFile + ".zip", spDir) );

                        // Add the data
                        String str = String.format( Texts.ScrEdt_dlfmtStr, strFile.trim(), strLName.trim(), strCName.trim(), strDesc.trim() );

                        if(!fileExists) {
                            final int idx = str.indexOf(Texts.ScrEdt_dlfmtSep);
                            str = str.substring( 0, idx + Texts.ScrEdt_dlfmtSep.length() )
                                + ExtListCellRenderer.ItemNAMarker
                                + str.substring(    idx + Texts.ScrEdt_dlfmtSep.length() );
                        }

                        dicList.add(str);

                        // Skip the empty line
                        if( reader.readLine() == null ) break;

                    } // while

                     Collections.sort(
                         dicList,
                         Comparator.comparing( s -> {
                             final int    idx = s.indexOf(Texts.ScrEdt_dlfmtSep);
                             final String str = (idx >= 0) ? s.substring(idx + 1) : s;
                             return str.startsWith(ExtListCellRenderer.ItemNAMarker) ? str.substring( ExtListCellRenderer.ItemNAMarker.length() ) : str;
                         } )
                     );

                }
                catch(final IOException e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                }

            }
        }

        // Generate the dictionary entries
        final int      defDicCnt   = Texts.ScrEdt_dictList.length;
        final int      extDicCnt   = dicList.isEmpty() ? 0 : ( 1 + dicList.size() );
        final String[] dictEntries = new String[defDicCnt + extDicCnt];

            for(int i = 0; i <  defDicCnt     ; ++i) dictEntries[i            ] = Texts.ScrEdt_dictList[i];
        if(extDicCnt > 0) {
                                                     dictEntries[    defDicCnt] = " " + Texts.ScrEdt_dlfmtSep + ExtListCellRenderer.SeparatorLine;
            for(int i = 1; i <= dicList.size(); ++i) dictEntries[i + defDicCnt] = dicList.get(i - 1);
        }

        // Split dictionary entries into 'function' and display text only
        final String[] dicFunctions   = new String[dictEntries.length];
        final String[] displayOptions = new String[dictEntries.length];

        for(int i = 0; i < dictEntries.length; ++i) {
            final String[] parts = _getDic_parts(dictEntries[i]);
            dicFunctions  [i] = _getDic_function     (parts);
            displayOptions[i] = _getDic_displayOption(parts);
        }

        // Find the index of the last and new dictionaries to load
        int lastIndex =  0;
        int loadIndex = -1;

        for(int i = 0; i < displayOptions.length; ++i) {
            if( dicFunctions[i].equals(_config.spellCheckSelDic) ) lastIndex = i;
            if( dicFunctions[i].equals(loadDic                 ) ) loadIndex = i;
        }

        // Show dialog as needed
        int    selectedIndex = 0;
        String selectedText  = null;

        if(loadDic == null) {
            final XCom.Pair<Integer, String> res = showOkCancelJListDialog(
                                                       Texts.ScrEdt_SelectDict, displayOptions, lastIndex, true
                                                   );
            if(res != null) {
                selectedIndex = res.first ();
                selectedText  = res.second();
            }
        }
        else {
            if(loadIndex <  0                 ) loadIndex = 0;
            if(loadIndex >= dictEntries.length) loadIndex = dictEntries.length - 1;
            selectedText  = displayOptions[loadIndex];
            selectedIndex = loadIndex;
        }

        if( selectedText == null || selectedText.trim().isEmpty() ) return;

        // If user made a choice, find the corresponding function name
        String selectedFunc = null;

        for(int i = 0; i < displayOptions.length; ++i) {

            if( !displayOptions[i].equals(selectedText) ) continue;
            selectedFunc = dicFunctions[i];
            break;

        } // for

        if(selectedFunc == null) return;

        // Remove the spell checker as needed
        if(_spellingParser != null && _textArea != null) _textArea.removeParser(_spellingParser);

        _spellingParser = null;

        // Show wait cursor
        SwingApp.showGlobalWaitCursor(this);

        // Create a new spelling parser
        final String selFunc = selectedFunc;

        SwingUtilities.invokeLater( () -> {
             SwingUtilities.invokeLater( () -> {

                // Call the function
                switch(selFunc) {

                    case "DefaultEnglish_auto" : _spellingParser = _createSpellingParser_DefaultEnglish_auto(true); break;
                    case "DefaultEnglish_US"   : _spellingParser = _createSpellingParser_DefaultEnglish_US  (true); break;
                    case "DefaultEnglish_GB"   : _spellingParser = _createSpellingParser_DefaultEnglish_GB  (true); break;

                    default :
                        _spellingParser = _createSpellingParser( selFunc + ".zip", new String[] { "word_list.dic" }, true );

                        break;

                } // switch

                // Fallback to the last dictionary or the default dictionary
                if(_spellingParser == null) {
                    if( recursiveDepth == 0 && _config.spellCheckSelDic != null && !_config.spellCheckSelDic.isEmpty() ) {
                        _handle_mnuViewChangeDict_impl(_config.spellCheckSelDic, recursiveDepth + 1);
                    }
                    if(_spellingParser == null) {
                        _spellingParser          = _createSpellingParser_DefaultEnglish_auto(false); // This one will have an index of 0
                        _config.spellCheckSelDic = dicFunctions[0];
                    }
                }
                else {
                    _config.spellCheckSelDic = selFunc;
                }

                // Apply theme
                JxMakeTheme.applySpellingParser(this, _useDCT);

                // Save as the last selected dictionary as needed
                if(loadDic == null) _config.save();

                // Add the new spell checker as needed
                if(_config.spellCheckEnabled && _textArea != null) _textArea.addParser(_spellingParser);

                // Show default cursor
                SwingUtilities.invokeLater( () -> {
                    SwingApp.showGlobalDefaultCursor(this);
                } );

            } );
        } );
    }

    public void handle_mnuViewChangeDict()
    { _handle_mnuViewChangeDict_impl(null, 0); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private JxMakeDisplayKBSCuts _dspKBSCuts = null;
    private DocBrowser           _docBrowser = null;

    public void handle_mnuHelpKbSCuts()
    {
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        SwingUtilities.invokeLater( () -> {

            executor.submit( () -> {

                if( _dspKBSCuts != null && _dspKBSCuts.mainWindow().isVisible() ) {
                    _dspKBSCuts.mainWindow().toFrontAndFocus();
                }
                else {
                    _dspKBSCuts = new JxMakeDisplayKBSCuts(_useDCT);
                    _dspKBSCuts.run(false);
                }

            } );

            executor.shutdown();

        } );
    }

    public void handle_mnuHelpDocBrowser()
    {
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        SwingUtilities.invokeLater( () -> {

            executor.submit( () -> {

                if( _docBrowser != null && _docBrowser.mainWindow().isVisible() ) {
                    _docBrowser.mainWindow().toFrontAndFocus();
                }
                else {
                    _docBrowser = new DocBrowser(_useDCT);
                    _docBrowser.run(false);
                }

            } );

            executor.shutdown();

        } );
    }

    public void handle_mnuHelpAbout()
    { _showAboutDialogWithScrollPane(); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _changeFileItem(final FileItem prvFileItem, final FileItem newFileItem)
    {
        if(prvFileItem == newFileItem) return;

        try {
            // Save the state
            if(prvFileItem != null) {
                _textArea.requestFocusInWindow();
                prvFileItem.saveFileState(JxMakeRootPane.this);
            }

            // Load the state and change the file
            final FileState fileState = newFileItem.loadFileState(JxMakeRootPane.this);

            if(fileState != null) {
                _switchEditorTo(fileState);
            }
            else {
                _switchEditorToNew();
                _loadTextFile_impl( newFileItem.absFullFilePath(), false );
            }

            // Focus to the text area
            _textArea.requestFocusInWindow();

            // Scan include files
            _scanIncludeFiles();

            // Adjust the divider
            _adjust_splitPaneDivider.run();
        }
        catch(final Exception e) {
            _showExceptionDialogWithScrollPane(e);
        }
    }

    private void _scanIncludeFiles()
    {
        // Get the selected file item
        final FileItem fi = _fileList.getSelectedFileItem();
        if(fi == null) return;

        // Get the includes
        final ArrayList<String> relPaths = new ArrayList<>();

        for( final String line : _pmd_LineBreak.split( _textArea.getText() ) ) {

            final Matcher m = _pmd_LocalIncVal.matcher(line);
            if( !m.find() || m.group(1) == null ) continue;

            final String v = m.group(1);
            if( !relPaths.contains(v) ) relPaths.add(v);

        } // for

        // Sort the includes
        Collections.sort( relPaths, new Comparator<String>() {
            private int _countParentSegments(final String path)
            {
                int count = 0;
                int idx   = -1;
                while( (idx = path.indexOf ("../", idx + 1) ) != -1 ) ++count;
                return count;
            }

            @Override
            public int compare(final String a, final String b)
            {
                final int countA = _countParentSegments(a);
                final int countB = _countParentSegments(b);

                if(countA != countB) return Integer.compare(countB, countA);

                return a.compareTo(b);
            }
        } );

        // Modify the tree
        fi.modSubFiles(relPaths);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _showOpenSaveDialog_impl(final boolean openMode)
    {
        final JFileChooser fileChooser = new JFileChooser();

            fileChooser.setCurrentDirectory( new File( SysUtil.getCWD() ) );

            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            fileChooser.addChoosableFileFilter( new FileFilter() {
                @Override
                public boolean accept(final File f)
                {
                    if( f.isDirectory() ) return true;
                    final String name = f.getName();
                    return name.equals(ArgParser.JMX_DefaultSpecFile) || name.endsWith(".jxm");
                }

                @Override
                public String getDescription()
                { return "JxMake Specification/Script Files (*.jxm or exact '" + ArgParser.JMX_DefaultSpecFile + "')"; }
            } );

            fileChooser.addChoosableFileFilter( new FileNameExtensionFilter("Any Text Files (*.txt)", "txt") );

            fileChooser.setAcceptAllFileFilterUsed(true);

        final int res = openMode ? fileChooser.showOpenDialog( mainWindow() )
                                 : fileChooser.showSaveDialog( mainWindow() );

        if(res != JFileChooser.APPROVE_OPTION) return null;

        return fileChooser.getSelectedFile().toString();
    }

    private String _showOpenDialog()
    { return _showOpenSaveDialog_impl(true ); }

    private String _showSaveDialog()
    { return _showOpenSaveDialog_impl(false); }

    private void _loadTextFile_impl(final String absFullFilePath, final boolean forReload) throws IOException
    {
        // Save the original caret position
        final int orgCaretPos = _textArea.getCaretPosition();

        // If the path is null, clear the text
        if(absFullFilePath == null) {
            _textArea.setText("");
        }

        // Otherwise, load the file
        else {
            final BufferedReader br = new BufferedReader( new FileReader(absFullFilePath) );
            _textArea.read(br, null);
            br.close();
        }

        // Load the saved file state
        final JxMakeFileList.SavedFileState sfs = _fileList.getSavedFileState(absFullFilePath);

        final int newCaretPos = Math.min( forReload ? orgCaretPos : sfs.caretPos, _textArea.getDocument().getLength() );

        // Reset state
        _textArea.setCaretPosition(newCaretPos);
        _textArea.discardAllEdits();

        final FileItem fileItem = _fileList.getSelectedFileItem();

        fileItem.clrModified();
        if(forReload) fileItem.refreshNodeData();
        else          fileItem.refreshNode    ();
        _fileList.expandAllNodes();
    }

    private void _saveTextFile_impl(final String absFullFilePath, final String text) throws IOException
    {
        final BufferedWriter bw = new BufferedWriter( new FileWriter(absFullFilePath) );

        bw.write( XCom.re_removeAllTrailingSpaces(text) );

        bw.close();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void reloadCurrentTextFile()
    {
        try {
            final FileItem fileItem = _fileList.getSelectedFileItem();

            if( !fileItem.isFileSpecified() ) {
                _showOKErrorDialog(Texts.ScrEdt_Error, Texts.ScrEdt_OK_NoReload);
                return;
            }

            if( fileItem.isModifiedSS() ) {
                /*
                if(fileItem == _fileList.mainFile) {
                    if( !_showYesNoWarningDialog(Texts.ScrEdt_Warning, Texts.ScrEdt_YN_ModRldMnSS) ) return;
                }
                else {
                    if( !_showYesNoWarningDialog(Texts.ScrEdt_Warning, Texts.ScrEdt_YN_ModReload ) ) return;
                }
                //*/
                if( !_showYesNoWarningDialog(Texts.ScrEdt_Warning, Texts.ScrEdt_YN_ModReload ) ) return;
            }

            _loadTextFile_impl( fileItem.absFullFilePath(), true );

            _textArea.requestFocusInWindow(); // Focus to the text area

            fileItem.setClean();
            fileItem.refreshNodeData();
        }
        catch(final Exception e) {
            _showExceptionDialogWithScrollPane(e);
        }
    }

    private void saveCurrentTextFile()
    {
        try {
            final FileItem fileItem = _fileList.getSelectedFileItem();

            if( !fileItem.isFileSpecified() ) {
                saveCurrentTextFileAs(true);
                return;
            }

            if( fileItem.isModifiedByOther() ) {
                if( !_showYesNoWarningDialog(Texts.ScrEdt_Warning, Texts.ScrEdt_YN_ModByExt) ) return;
            }

            // Ensure the state of the current file has been saved
            fileItem.saveFileState(JxMakeRootPane.this);

            // Suspend the file watcher
            _fileList.suspendFileWatcher();

            // Save the file
            _saveTextFile_impl( fileItem.absFullFilePath(), _textArea.getText() );

            // Focus to the text area
            _textArea.requestFocusInWindow();

            // Reset state
            fileItem.setClean();
            fileItem.refreshNodeData();
            _fileList.expandAllNodes();
        }
        catch(final Exception e) {
            _showExceptionDialogWithScrollPane(e);
        }
        finally {
            // Resume the file watcher
            SwingUtilities.invokeLater( () -> { _fileList.resumeFileWatcher(); } );
        }
    }

    private void saveCurrentTextFileAs(final boolean calledFromSave)
    {
        try {
            final FileItem fileItem       = _fileList.getSelectedFileItem();
                  boolean  reloadMainFile = false;

            if(calledFromSave) {
                if(fileItem == _fileList.mainFile) reloadMainFile = true;
            }
            else {
                if(fileItem == _fileList.mainFile) {
                    if( _fileList.mainFile.isModifiedSS() ) {
                        if( !_showYesNoQuestionDialog(Texts.ScrEdt_Warning, Texts.ScrEdt_YN_SAsMainMod) ) return;
                    }
                    else {
                        if( !_showYesNoWarningDialog (Texts.ScrEdt_Warning, Texts.ScrEdt_YN_SAsMainNfo) ) return;
                    }
                    reloadMainFile = true;
                }
                else {
                        if( !_showYesNoQuestionDialog(Texts.ScrEdt_Warning, Texts.ScrEdt_YN_SAsFileNfo) ) return;
                }
            }

            // Get the file path
            String resFilePath = _showSaveDialog();
            if(resFilePath == null) return;

            final String fileName = SysUtil.getFileName(resFilePath);

            if( !fileName.contains(".") ) {
                if( fileName.equalsIgnoreCase(ArgParser.JMX_DefaultSpecFile) ) {
                    // Normalize case if not exactly equal
                    if( !fileName.equals(ArgParser.JMX_DefaultSpecFile) ) {
                        resFilePath = resFilePath.substring( 0, resFilePath.length() - fileName.length() )
                                    + ArgParser.JMX_DefaultSpecFile;
                    }
                }
                else {
                    resFilePath += ".jxm";
                }
            }

            if( SysUtil.pathIsValid(resFilePath) ) {
                if( !_showYesNoWarningDialog(Texts.ScrEdt_Warning, Texts.ScrEdt_YN_SAsFileOvr) ) return;
            }

            final String filePath = resFilePath;

            // Suspend the file watcher
            if(reloadMainFile) _fileList.suspendFileWatcher();

            // Save the file
            _saveTextFile_impl( filePath, _textArea.getText() );

            // Reset state
            if(!reloadMainFile) {
                fileItem.setClean();
                fileItem.refreshNodeData();
            }

            // Focus to the text area
            _textArea.requestFocusInWindow();

            // Reload all if it was the main script file
            if(reloadMainFile) {
                SwingUtilities.invokeLater( () -> {
                    // Load the main file
                    loadMainFile(filePath);
                    // Clear and resume the file watcher
                    SwingUtilities.invokeLater( () -> {
                        _fileList.clearFileWatcher();
                        _fileList.resumeFileWatcher();
                    } );
                } );
                return;
            }
        }
        catch(final Exception e) {
            _showExceptionDialogWithScrollPane(e);
        }
    }

    private void saveAllTextFiles()
    {
        try {
            // Error if the main script file has never been saved
            if( !_fileList.mainFile.isFileSpecified() ) {
                _showOKErrorDialog(Texts.ScrEdt_Error, Texts.ScrEdt_OK_NoSaveAll);
                return;
            }

            // Confirm
            if( _fileList.mainFile.isModifiedByOtherSS() ) {
                if( !_showYesNoWarningDialog(Texts.ScrEdt_Warning, Texts.ScrEdt_YN_ModByExtSS) ) return;
            }

            // Ensure the state of the current file has been saved
            final FileItem fileItem = _fileList.getSelectedFileItem();

            fileItem.saveFileState(JxMakeRootPane.this);

            // Get the absolute path and text data
            final HashMap<String, String> fileDataMap = new HashMap<>();

            _fileList.mainFile.getAllTextDataSS( (final String absFullFilePath, final String textData) -> {
                fileDataMap.put(absFullFilePath, textData);
            } );

            // Suspend the file watcher
            _fileList.suspendFileWatcher();

            // Save the files
            for( Map.Entry<String, String> entry : fileDataMap.entrySet() ) {

                _saveTextFile_impl( entry.getKey(), entry.getValue() );

            } // for

            // Reset state
            _fileList.mainFile.setCleanSS();
            _fileList.mainFile.refreshNodeDataSS();
            _fileList.expandAllNodes();
        }
        catch(final Exception e) {
            _showExceptionDialogWithScrollPane(e);
        }
        finally {
            // Resume the file watcher
            SwingUtilities.invokeLater( () -> { _fileList.resumeFileWatcher(); } );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean loadMainFile(final String filePath_)
    {
        if( _fileList.mainFile.isModifiedSS() ) {
            if(filePath_ == null) {
                if( !_showYesNoWarningDialog(Texts.ScrEdt_Warning, Texts.ScrEdt_YN_ModNewSS) ) return false;
            }
            else {
                if( !_showYesNoWarningDialog(Texts.ScrEdt_Warning, Texts.ScrEdt_YN_ModLoadSS) ) return false;
            }
        }

        String filePath = null;

        if(filePath_ == null) {
            // Create a new main script file
            /* Nothing to do here */
        }
        else if( filePath_.equals(_SpcInd_OpenFile_) ) {
            // Get the file path
            filePath = _showOpenDialog();
            if(filePath == null) return false;
        }
        else {
            // Open the specified main script file
            filePath = filePath_;
        }

        if( _fileList.mainFile.isFileSpecified() ) _fileList.selectAndScrollToMainFile();

        try {

            filePath = (filePath != null) ? SysUtil.resolveAbsolutePath(filePath) : null;
            _loadTextFile_impl(filePath, false);

            _textArea.requestFocusInWindow(); // Focus to the text area

            _fileList.setMainFile(filePath);
            _fileList.selectAndScrollToMainFile();

            _fileList.clearAllFileState();

            _config.putRecentFile(filePath);

            _scanIncludeFiles();

            return true;
        }
        catch(final Exception e) {
            _showExceptionDialogWithScrollPane(e);
        }

        return false;
    }

} // class JxMakeRootPane
