/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp.se;


import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Toolkit;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import java.lang.reflect.InvocationTargetException;

import java.nio.CharBuffer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import java.util.function.Consumer;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.OverlayLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.JTextComponent;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.Utilities;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import javax.swing.text.rtf.RTFEditorKit;

import javax.swing.undo.UndoManager;

import jxm.*;
import jxm.annotation.*;
import jxm.gcomp.*;
import jxm.tool.*;
import jxm.xb.*;

import static jxm.tool.CommandSpecifier.*;


@SuppressWarnings("serial")
@package_private
class JxMakeConsole extends ANSIScreenBuffer {

    private static final boolean BENCHMARK            = !true;
    private static final int     COMMAND_HISTORY_SIZE = 1000;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final boolean            _useDCT;

    private final ProcessWrapperBase _pb;

    private final JPanel             _panel;
    private final JPanel             _panelOverlay;

    private final JToolBar           _toolBar;
    private final JButton            _btnCopyWLW;
    private final JButton            _btnCopyAST;
    private final JButton            _btnEOF;
    private final JButton            _btnSIGINT;
    private final JButton            _btnSIGKILL;
    private final JButton            _btnEnvVar;
    private final JButton            _btnFSzInc;
    private final JButton            _btnFSzDec;
    private final JButton            _btnFSzRst;
    private final JButton            _btnTReset;
    private final JLabel             _lblTitle;

    private final JTextPane          _textPane;
    private final StyledDocument     _textSDoc;

    private final JButton            _btnCompose;
    private final JTextField         _textField;
    private final UndoManager        _undoManager;
    private final CommandHistory     _commandHistory;
    private final JxMakeShell        _shell;

    private       int                _lastRenderedRow = -1;
    private       boolean            _blinkState      = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class TextRange {

        public final int                offset;
        public final int                length;
        public final SimpleAttributeSet attrs;

        TextRange(final int offset_, final int length_, final SimpleAttributeSet attrs_)
        {
            offset = offset_;
            length = length_;
            attrs  = attrs_;
        }

    } // class TextRange

    final XCom.MultiMap<Integer, TextRange> _blinkMap = new XCom.MultiMap<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private CharBuffer _cbRender     = null;
    private String     _strEmptyLine = null;

    private boolean _initBuffer(final JRootPane rootPane)
    {
        // No need to continue if already initialized
        if(JxMakeConsole.super._screenBuffer != null) return false;

        // Initialize the screen buffer
        final FontMetrics fontMetrics = _textPane.getFontMetrics( _textPane.getFont() );
        final int         cellWidth   = fontMetrics.charWidth('W');
        final int         cellHeight  = fontMetrics.getHeight(   );
        final int         numScreen   = 64;
        final int         numCols     = Math.min( 160, Math.max( 80, _textPane.getWidth () / cellWidth  - 8 ) & 0xFFFFFFFE );
        final int         numRows     = Math.min(  48, Math.max( 24, _textPane.getHeight() / cellHeight     ) & 0xFFFFFFFE );

        super._initialize(numScreen, numRows, numCols);

        _cbRender = CharBuffer.allocate( super._totalCols() + 1 );

        // Prepare the empty line string
        final StringBuilder sbEL = new StringBuilder();

        for(int i = 0; i < _screenCols(); ++i) sbEL.append(' ');
        sbEL.append('\n');

        _strEmptyLine = sbEL.toString();

        // Load the saved environment variables
        final SavedEnvVarState sevs = new SavedEnvVarState();

        if( sevs.loadSpecific() ) {
            if( sevs.effTable != null & !sevs.effTable.isEmpty() ) _customEnvVar = sevs.effTable;
        }

        // Done
        return true;
    }

    private void _renderRow(final int r)
    {

        try {

            // Erase first if needed
            final int totCols = super._totalCols();
            final int offset  = r * (totCols + 1); // +1 due to "\n"

            if(r <= _lastRenderedRow) {
                if( !super._screenBuffer.dirty[r] ) return;
                _textSDoc.remove(offset, totCols + 1); // +1 due to "\n"
                _blinkMap.remove(r);
            }

            // Render the row
            final LineBuffer lb = super._screenBuffer.rows [r];
            final boolean    lw = super._screenBuffer.lwrap[r];
                  int        c  = 0;

            while(c < totCols) {

                final int                startIndex   = c;
                final SimpleAttributeSet attributeSet = lb.cols[c].attributeSet();
                      boolean            wideSegment  = false;

                _cbRender.clear();

                do {

                    final char charH    = lb.cols[c++].ch;
                    final int  stepSize = UnicodeUtil.stepSize(charH);

                    if(stepSize == 0) {
                        _cbRender.put(charH);
                        continue;
                    }

                    char    charL  = 0;
                    boolean isWide = false;

                    if(stepSize == 1) {
                        isWide = UnicodeUtil.isDoubleWidth(charH);
                    }
                    else {
                        charL  = lb.cols[c++].ch;
                        isWide = UnicodeUtil.isDoubleWidth(charH, charL);
                    }

                    if( _cbRender.position() == 0 ) {
                        wideSegment = isWide;
                    }
                    else if(wideSegment != isWide) {
                        c -= stepSize; // Rewind
                        break;
                    }

                                   _cbRender.put(charH);
                    if(charL != 0) _cbRender.put(charL);

                } while( (c < totCols) && ( lb.cols[c].attributeSet() == attributeSet ) );

                if(c >= totCols) _cbRender.put('\n');

                _cbRender.flip();
                if( _cbRender.limit() == 0 ) continue;

                final SimpleAttributeSet attrsSel = Boolean.TRUE.equals(wideSegment) ? lb.cols[startIndex].attributeSet(Char.USER_VALUE_WIDE)
                                                                                     : attributeSet;

                _textSDoc.insertString( offset + startIndex, new String( _cbRender.array(), 0, _cbRender.limit() ), attrsSel );
                /*
                _textSDoc.insertString(
                    offset + startIndex,
                    CharBuffer.wrap( _cbRender.array(), 0, _cbRender.limit() ).toString(),
                    attrsSel
                );
                */

                if( lb.cols[startIndex].k ) _blinkMap.put( r, new TextRange( offset + startIndex, _cbRender.limit(), attrsSel ) );

            } // while

            // Add line wrap marker as needed
            if(lw) _textSDoc.setCharacterAttributes( offset + totCols, 1, _pilcrowIcon.simpleAttributeSet(), false );

            // Clear the dirty flag
            super._screenBuffer.dirty[r] = false;

        }
        catch(final BadLocationException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }
    }

    private void _renderUpTo(final int rowNum)
    {
        if(_screenBuffer == null) return;

        for(int r = 0; r <= rowNum; ++r) _renderRow(r);

        _lastRenderedRow = rowNum;
    }

    public synchronized void handleUpdate(final boolean updateBlink)
    {
        // Render buffer
        _renderBuffer();

        // Update blink and update the overlay panel visibility
        if(updateBlink) {

            // Update blink
            for( Map.Entry< Integer, ? extends List<TextRange> > entry : _blinkMap.entrySet() ) {

                for( final TextRange tr : entry.getValue() ) {

                    final SimpleAttributeSet attrs = new SimpleAttributeSet(tr.attrs);

                    StyleConstants.setForeground(
                        attrs,
                        _blinkState ? StyleConstants.getForeground(tr.attrs)
                                    : StyleConstants.getBackground(tr.attrs)
                    );

                    _textSDoc.setCharacterAttributes(tr.offset, tr.length, attrs, true);

                } // for

            } // for

            _blinkState = !_blinkState;

            // Update the overlay panel visibility
            _panelOverlay.setVisible(_lastWrittenRow < 1);

            // Update the title
            if( _hasTitleChanged() ) _lblTitle.setText( _getTitle() );

        }

        // Repaint
        SwingUtilities.invokeLater(_textPane::repaint);
    }

    public void showGlobalWaitCursorFM()
    {
        SwingApp.showGlobalWaitCursorFM();

        _textField.setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) );
    }

    public void showGlobalDefaultCursorFM()
    {
        SwingApp.showGlobalDefaultCursorFM();

        _textField.setCursor( Cursor.getDefaultCursor() );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected synchronized void _scrollBufferOneUp()
    {
        super._scrollBufferOneUp();

        try {
            final String text         = _textSDoc.getText( 0, _textSDoc.getLength() );
            final int    firstNewline = text.indexOf('\n');

            if(firstNewline >= 0) _textSDoc.remove( 0, firstNewline + 1      );
            else                  _textSDoc.remove( 0, _textSDoc.getLength() );

            --_lastRenderedRow;
        }
        catch(final BadLocationException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }
    }

    @Override
    protected synchronized void _scrollBufferOneDown_ifCurRowIsZero()
    {
        super._scrollBufferOneDown_ifCurRowIsZero();

        try {
            _textSDoc.insertString(0, _strEmptyLine, null);
            ++_lastRenderedRow;

            while( _lastRenderedRow >= _totalRows() ) {

                final String text        = _textSDoc.getText( 0, _textSDoc.getLength() );
                final int    lastNewline = text.lastIndexOf('\n');

                if(lastNewline >= 0) _textSDoc.remove( lastNewline, _textSDoc.getLength() - lastNewline );
                else                 _textSDoc.remove( 0          , _textSDoc.getLength()               );

               --_lastRenderedRow;

            } // while
        }
        catch(final BadLocationException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }
    }

    @Override
    protected synchronized void _resetAll()
    {
        super._resetAll();

        try {
             _textSDoc.remove( 0, _textSDoc.getLength() );

             SwingUtilities.invokeLater(_textPane::repaint);
        }
        catch(final BadLocationException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

         _blinkMap.clear();

         _lastRenderedRow = -1;
         _blinkState      = false;

         _shell.resetState();

         _lastExitCode    = 0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final AtomicBoolean _rbPending = new AtomicBoolean(false);
    private final AtomicLong    _rbLastRun = new AtomicLong(0);

    private synchronized void _renderBuffer_impl()
    {
        if(_screenBuffer == null || !_screenBuffer.anyDirty) return;

        showGlobalWaitCursorFM();

        final long beg = BENCHMARK ? SysUtil.getUS() : 0;

        _renderUpTo(super._lastWrittenRow);

        if(BENCHMARK) {
            SysUtil.stdDbg().printf( "Elapsed mS for _renderUpTo(super._lastWrittenRow) = %7.2f\n", ( SysUtil.getUS() - beg ) / 1000.0f );
        }

        showGlobalDefaultCursorFM();

        _screenBuffer.anyDirty = false;
    }

    @Override
    protected synchronized void _renderBuffer()
    {
        // Do not proceed if the request is already scheduled
        if( !_rbPending.compareAndSet(false, true) ) {
            return;
        }

        // Ensure the request is not called more than once every 50mS
        final long now = SysUtil.getMS();

        if( now - _rbLastRun.get() < 50 ) {
            _rbPending.set(false);
            return;
        }

        // Schedule the task
        SwingUtilities.invokeLater( () -> {
            try {
                _renderBuffer_impl();
            }
            finally {
                _rbPending.set( false           );
                _rbLastRun.set( SysUtil.getMS() );
            }
        } );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private Map<String, String> _customEnvVar = null;

    public Map<String, String> getEffEnvVar()
    {
        Map<String, String> envVar = (_evEditor == null) ? null : _evEditor.getFinalCheckedData();

        if(envVar == null && _customEnvVar != null) envVar = _customEnvVar;
        if(envVar == null                         ) envVar = SysUtil.getAllEnv();

        if( SysUtil.osIsWindows() ) {
            if( !envVar.containsKey("TERM") ) envVar.put("TERM", "xterm-256color");
        }

        return envVar;
    }

    private ProcessHandle _pbExec(final String[] command, final String[] stdinData, final boolean buffered) throws Exception
    {
        // Determine the effective environment variables
        final Map<String, String> envVar = getEffEnvVar();

        // Set the tab size as needed
        String tabSizeStr = null;

             if( envVar.containsKey("TAB_SIZE") ) tabSizeStr = envVar.get("TAB_SIZE");
        else if( envVar.containsKey("TABSIZE" ) ) tabSizeStr = envVar.get("TABSIZE" );
        else if( envVar.containsKey("COLUMNS" ) ) tabSizeStr = envVar.get("COLUMNS" );

        if(tabSizeStr != null) {
            try {
                _setTabSize( Integer.parseInt( tabSizeStr.trim() ) );
            }
            catch(final NumberFormatException e) {
                _setDefaultTabSize();
            }
        }
        else {
            _setDefaultTabSize();
        }

        // Set parameters
        _pb.setEnvironment                                ( envVar              );
        _pb.setDirectory                                  ( SysUtil.getCWD()    );
        _pb.setCommand                                    ( command             );
        _pb.setConsole                                    ( false               );
        _pb.setInitialRows                                ( super._screenRows() );
        _pb.setInitialColumns                             ( super._screenCols() );
        _pb.setWindowsAnsiColorEnabled                    ( true                );
        _pb.setUnixOpenTtyToPreserveOutputAfterTermination( true                );

        final ProcessHandle handle = _putProcess(_pb, buffered);

        handle.start();

        if(stdinData != null) {
            for(final String s : stdinData) handle.putStdIn(s);
        }

        return handle;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class FixedCellLabelView extends LabelView {

        private final int               _cellWidth;
        private final int               _cellHeight;

        private final FontRenderContext _frc;

        public FixedCellLabelView(final Element elem, final int cellWidth, final int cellHeight, final FontRenderContext frc)
        {
            super(elem);

            _cellWidth  = cellWidth;
            _cellHeight = cellHeight;

            _frc        = frc;
        }

        private String _getText() throws BadLocationException
        {
            final int p0 = getStartOffset();
            final int p1 = getEndOffset  ();

            return getDocument().getText(p0, p1 - p0);
        }

        @Override
        public float getPreferredSpan(final int axis)
        {
            if(axis == View.Y_AXIS) {
                return _cellHeight;
            }

            else {
                try {
                    // Get the text
                    final String text = _getText();
                    if( text.isEmpty() ) return 0.0f;

                    // Remove newline character at the end of the text
                    int len = text.length();

                    if( text.charAt(len - 1) == '\n' ) --len;

                    final String ftext = text.substring(0, len);
                    final String ttext = ftext.trim();

                    // Calculate and return the total pixel size
                    final int gCnt   = UnicodeUtil.graphemeCount(ftext);
                    final int width1 = gCnt * _cellWidth;

                    /*
                    final float ref = super.getPreferredSpan(axis);
                    if(width1 != ref) {
                        SysUtil.stdDbg().printf("'%s' (%d) => %f (%d) \n", ftext, gCnt, ref, width1);
                    }
                    //*/

                    if( ttext.isEmpty() || !UnicodeUtil.containsCTL(ttext) ) return width1;

                    //*
                    final int         delta  = ftext.length() - ttext.length();
                    final GlyphVector gv     = getFont().createGlyphVector(_frc, ttext);
                    final int         width2 = (int) Math.ceil( gv.getLogicalBounds().getWidth() ) + _cellWidth * delta;

                    if(width2 < width1) {
                        final int width3 = ( (width2 + _cellWidth - 1) / _cellWidth ) * _cellWidth;
                        return width3;
                    }
                    //*/

                    /*
                    final GlyphVector gv     = getFont().createGlyphVector(_frc, ftext);
                    final int         width2 = (int) Math.ceil( gv.getVisualBounds().getWidth() );

                    if(width2 < width1) {
                        final int width3 = ( (width2 + _cellWidth - 1) / _cellWidth ) * _cellWidth;
                        return width3;
                    }
                    //*/

                    return width1;
                }
                catch(final BadLocationException e) {
                    return 0.0f;
                }
            }
        }

        @Override
        public Font getFont()
        {
            // Check for the user flag
            final AttributeSet attrs = getAttributes();
            if(attrs == null) return super.getFont();

            final Object valObject = attrs.getAttribute(Char.USER_FLAG_KEY);
            if( !(valObject instanceof Integer) ) return super.getFont();

            if( ( (Integer) valObject ) != Char.USER_VALUE_WIDE ) return super.getFont();

            // Get the font
            final Font baseFont = super.getFont();

            // Get the graphics and return the base font if graphics is null
            final Graphics graphics = getContainer().getGraphics();

            if(graphics == null) return baseFont;

            // Get the font metrics
            final FontMetrics fontMetrics = graphics.getFontMetrics(baseFont);

            try {
                // Get the text and remove newline character at the end of the text
                final String text = _getText();

                if( text.isEmpty() ) return baseFont;

                // Remove newline character at the end of the text
                int len = text.length();

                if( text.charAt(len - 1) == '\n' ) --len;

                if(true) {
                    // NOTE : This works more reliably to reduce the text length, but may result in
                    //       text being smaller than needed with extra spacing

                    // Get the widest character
                    int maxCharWidth = 0;

                    for(int i = 0; i < len;) {

                        final int codePoint = text.codePointAt(i);
                        final int charCount = Character.charCount(codePoint);
                              int width     = fontMetrics.charWidth(codePoint);

                        if( UnicodeUtil.isCTL(codePoint) ) {
                            final GlyphVector glyphVector = baseFont.createGlyphVector( _frc, new int[] { codePoint } );
                            final int         widthGV     = (int) Math.ceil( glyphVector.getVisualBounds().getWidth() );
                            if(widthGV > width) width = widthGV;
                        }

                        if(width > maxCharWidth) maxCharWidth = width;

                        i += charCount;

                    } // for

                    // Scale as needed
                    if(maxCharWidth > _cellWidth) {
                        final float scale    = ( (float) _cellWidth ) / ( (float) maxCharWidth );
                        final Font  drvdFont = baseFont.deriveFont( baseFont.getSize() * scale );
                        final int   transY   = fontMetrics.getHeight() - graphics.getFontMetrics(drvdFont).getHeight();
                        /*
                        SysUtil.stdDbg().printf("%d / %d -> %f\n", _cellWidth, maxCharWidth, scale);
                        //*/
                        return (transY == 0) ? drvdFont : drvdFont.deriveFont( AffineTransform.getTranslateInstance(0, transY) );
                    }
                }
                else {
                    // NOTE : This does not always work!

                    // Calculate the maximum pixel size, excluding the optional newline character at the end of the text
                    final int maxSize = _cellWidth * UnicodeUtil.graphemeCount(text, len);

                    if(maxSize <= 0) return baseFont;

                    // Measure the full span width
                    int spanWidth = fontMetrics.stringWidth(text);

                    if( UnicodeUtil.containsCTL(text) ) {
                        final GlyphVector glyphVector = baseFont.createGlyphVector(_frc, text);
                        final int         widthGV     = (int) Math.ceil( glyphVector.getVisualBounds().getWidth() );
                        if(widthGV > spanWidth) spanWidth = widthGV;
                    }

                    // Scale as needed
                    if(spanWidth > maxSize) {
                        final float scale    = ( (float) maxSize ) / ( (float) spanWidth );
                        final Font  drvdFont = baseFont.deriveFont( baseFont.getSize() * scale );
                        final int   transY   = fontMetrics.getHeight() - graphics.getFontMetrics(drvdFont).getHeight();
                        /*
                        SysUtil.stdDbg().printf("%d / %d ->  %f\n", spanWidth, maxSize, scale);
                        //*/
                        return (transY == 0) ? drvdFont : drvdFont.deriveFont( AffineTransform.getTranslateInstance(0, transY) );
                    }
                }
            }
            catch(final BadLocationException e) {
                // Ignore error
            }

            // Return the base font
            return baseFont;
        }

    } // class FixedCellLabelView

    private static class GridViewFactory implements ViewFactory {

        private int               _cellWidth  = 0;
        private int               _cellHeight = 0;

        private FontRenderContext _frc        = null;

        public GridViewFactory(final JTextPane textPane, final int cellWidth, final int cellHeight)
        { setCellSize(textPane, cellWidth, cellHeight); }

        public void setCellSize(final JTextPane textPane, final int cellWidth, final int cellHeight)
        {
            _cellWidth  = cellWidth;
            _cellHeight = cellHeight;

            final Graphics2D g2d = (Graphics2D) textPane.getGraphics();
            if(g2d != null) {
                _frc = g2d.getFontRenderContext();
                g2d.dispose();
            }
            else {
                if(_frc == null) _frc = new FontRenderContext(null, true, true);
            }
        }

        @Override
        public View create(final Element elem)
        {
            switch( elem.getName() ) {

                // Default view for sections
                case AbstractDocument.SectionElementName:
                    return new BoxView(elem, View.Y_AXIS);

                // Default view for paragraphs
                case AbstractDocument.ParagraphElementName:
                    return new ParagraphView(elem);

                // Default view for text content
                case AbstractDocument.ContentElementName:
                    return new FixedCellLabelView(elem, _cellWidth, _cellHeight, _frc);

                // View for embedded icons
                case StyleConstants.IconElementName:
                    return new IconView(elem);

                /*
                // View for embedded components
                case StyleConstants.ComponentElementName:
                    return new ComponentView(elem);
                //*/

                // Default fallback
                default:
                    return new FixedCellLabelView(elem, _cellWidth, _cellHeight, _frc);

            } // switch
        }

    }  // class GridViewFactory

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private class ConsoleCaret extends DefaultCaret {

        private int _cellWidth   = 0;
        private int _cellHeight  = 0;
        private int _cellAscent  = 0;
        private int _cellDescent = 0;

        public ConsoleCaret(final int cellWidth, final int cellHeight, final int cellAscent, final int cellDescent)
        { setCellSize(cellWidth, cellHeight, cellAscent, cellDescent); }

        public void setCellSize(final int cellWidth, final int cellHeight, final int cellAscent, final int cellDescent)
        {
            _cellWidth   = cellWidth;
            _cellHeight  = cellHeight;
            _cellAscent  = cellAscent;
            _cellDescent = cellDescent;
        }

        public void setCellWidth  (final int cellWidth  ) { _cellWidth   = cellWidth  ; }
        public void setCellHeight (final int cellHeight ) { _cellHeight  = cellHeight ; }
        public void setCellAscent (final int cellAscent ) { _cellAscent  = cellAscent ; }
        public void setCellDescent(final int cellDescent) { _cellDescent = cellDescent; }

        @Override
        public void install(final JTextComponent tc)
        {
            super.install(tc);

            tc.setCaretColor(DefaultForegroundColor);
        }

        @Override
        public void paint(final Graphics g)
        {
            if( !cursorIsVisible() || !_blinkState ) return;

            // NOTE : # This implementation should work properly for multi-byte Unicode graphemes.
            //        # This implementation would not support ANSI OSC 12 sequences.

            // Save old state
            final Color oldColor = g.getColor();

            // Use the caret color you set earlier
            final Color caretColor = getComponent().getCaretColor();

            // XOR against background, but draw with caretColor
            g.setXORMode( getComponent().getBackground() );
            g.setColor( caretColor );

            // Draw block caret
            final LineBuffer row   = _screenBuffer.rows[_curRow];
                  String     chStr = "#";
                  int        x     = _curCol * _cellWidth;
            final int        y     = _curRow * _cellHeight;

            if(row != null) {

                // Walk backwards until we find a base, high surrogate, or regional indicator
                int idxCh = _curCol;

                while(idxCh > 0) {

                    final char ch = row.cols[idxCh].ch;

                    if( UnicodeUtil.isBoundaryCharType(ch) ) break;

                    --idxCh;

                } // while

                // Get the number of character for the symbol
                final int step = UnicodeUtil.stepCountFW(row.cols, idxCh);

                // Build the grapheme string
                if(true) {
                    final char[] chr = new char[step];
                    for(int c = 0; c < step; ++c) chr[c] = row.cols[c + idxCh].ch;
                    chStr = new String(chr);
                }

                // Calculate the column start pixel
                if(idxCh > 0) {
                    final char[] chr = new char[idxCh];
                    for(int c = 0; c < idxCh; ++c) chr[c] = row.cols[c].ch;
                    x = UnicodeUtil.graphemeCount( new String(chr) ) * _cellWidth;
                }
            }

            final Insets insets = _textPane.getInsets();

            final int caretX = (int) Math.round(
                                    x
                                  + g.getFont().createGlyphVector(
                                        ( (Graphics2D) g ).getFontRenderContext(),
                                        chStr
                                    ).getVisualBounds().getX()
                               );
          //g.fillRect(caretX, y + _cellDescent - 1, _cellWidth, _cellHeight);
            g.fillRect(caretX + insets.left, y + insets.top, _cellWidth, _cellHeight);

            // Restore state
            g.setPaintMode();
            g.setColor(oldColor);

        }

    } // class ConsoleCaret

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class PilcrowIcon implements Icon {

        private static final String Pilcrow = "¶";

        private       BufferedImage      _cache = null;
        private final SimpleAttributeSet _sas   = new SimpleAttributeSet();

        public SimpleAttributeSet simpleAttributeSet()
        { return _sas; }

        public void updateFromTextPane(final JTextPane textPane)
        {
            final Font        font     = textPane.getFont();
            final FontMetrics fm       = textPane.getFontMetrics(font);

            final int         w        = fm.stringWidth(Pilcrow);
            final int         h        = fm.getHeight();

                              _cache   = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D  g2       = _cache.createGraphics();
            final int         baseline = fm.getAscent();
            final int         offset   = ( h - fm.getAscent() ) / 2;

            g2.setFont   (font                         );
            g2.setColor  (Color.BLACK                  );
            g2.drawString(Pilcrow, 0, baseline + offset);
            g2.dispose   (                             );

            StyleConstants.setIcon(_sas, this);
        }

        @Override
        public void paintIcon(final Component c, final Graphics g, int x, final int y)
        {
            if(_cache == null) return;

            final Graphics2D g2 = (Graphics2D) g.create();

            g2.setXORMode(Color.DARK_GRAY   );
            g2.drawImage (_cache, x, y, null);
            g2.dispose   (                  );
        }

        @Override
        public int getIconWidth()
        { return (_cache != null) ? _cache.getWidth() : 0; }

        @Override
        public int getIconHeight()
        { return (_cache != null) ? _cache.getHeight() : 0; }

    } // class PilcrowIcon

    private static final PilcrowIcon _pilcrowIcon = new PilcrowIcon();

    private static ImageIcon _newImageIcon(final String name)
    { return SysUtil.newImageIcon_defLoc("toolbar/tlb_con_" + name + ".gif"); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private JxMakeEnvVarEditor _evEditor    = null;
    private JxMakeCmdComposer  _cmdComposer = null;
    private int                _fzFactor    = 0;

    public JPanel jPanel()
    {
        //*
        new Thread( () -> {

            // Delay to ensure the requesting component has finished its processing and UI flow
            try {
                Thread.sleep(250);
            }
            catch(final InterruptedException ignored) {
                // Restore state
                Thread.currentThread().interrupt();
            }

            SwingUtilities.invokeLater( () -> {
                if( _textField == null || !_textField.isShowing() ) return;
                _textField.requestFocusInWindow();
            } );

        } ).start();
        //*/

        return _panel;
    }

    public JTextPane jTextPane()
    { return _textPane; }

    private int _computeCellWidth()
    { return _textPane.getFontMetrics( _textPane.getFont() ).charWidth('#'); }

    private int _computeCellHeight()
    { return _textPane.getFontMetrics( _textPane.getFont() ).getHeight(); }

    private int _computeCellAscent()
    { return _textPane.getFontMetrics( _textPane.getFont() ).getAscent(); }

    private int _computeCellDescent()
    { return _textPane.getFontMetrics( _textPane.getFont() ).getDescent(); }

    private void _adjustFontSize(final int factor_)
    {
        int factor = factor_;

        if(factor != 0) {
            _fzFactor += factor;
        }
        else {
            factor    = -_fzFactor;
            _fzFactor = 0;
        }

        final Font orgFont = _textPane.getFont();
        final Font newFont = orgFont.deriveFont( (float) orgFont.getSize() + factor );

      //_textField.setFont(newFont);
        _textPane .setFont(newFont);

        final GridViewFactory gvf = (GridViewFactory) _textPane.getEditorKit().getViewFactory();
        final ConsoleCaret    cc  = (ConsoleCaret) _textPane.getCaret();

        gvf.setCellSize( _textPane, _computeCellWidth(), _computeCellHeight()                                              );
        cc .setCellSize(            _computeCellWidth(), _computeCellHeight(), _computeCellAscent(), _computeCellDescent() );

        _pilcrowIcon.updateFromTextPane(_textPane);

        _setAllDirty();
        _renderBuffer();
    }

    public JxMakeConsole(final boolean useDarkColorTheme)
    {
        _useDCT = useDarkColorTheme;

        // Get the base font
        final Font baseFont = JxMakeTheme.getMonoFont();

        // Initialize the panel
        _panel = new JPanel();

            _panel.setLayout( new BorderLayout() );

        // Try to initialize Pty4J
        final ProcessWrapperPty4J pty4j    = new ProcessWrapperPty4J();
              boolean             usePty4J = false;

        if( pty4j.initialize() ) {
            _pb      = pty4j;
            usePty4J = true;
        }
        else {

            final ProcessWrapperStd ptyStd = new ProcessWrapperStd();

            if( ptyStd.initialize() ) {
                _pb = ptyStd;
            }
            else {

                final JLabel msg = new JLabel(Texts.ScrEdt_ConsoleNA, SwingConstants.CENTER);
                    msg.setFont( baseFont.deriveFont(Font.BOLD, 24) );

                _panel.add(msg, BorderLayout.CENTER);

                _pb             = null;

                _panelOverlay   = null;

                _toolBar        = null;
                _btnCopyWLW     = null;
                _btnCopyAST     = null;
                _btnEOF         = null;
                _btnSIGINT      = null;
                _btnSIGKILL     = null;
                _btnEnvVar      = null;
                _btnFSzInc      = null;
                _btnFSzDec      = null;
                _btnFSzRst      = null;
                _btnTReset      = null;
                _lblTitle       = null;

                _textPane       = null;
                _textSDoc       = null;

                _btnCompose     = null;
                _textField      = null;
                _undoManager    = null;
                _commandHistory = null;
                _shell          = null;

                return;

            } // if ptyStd

        } // if pty4j

        // Create toolbar
        _toolBar = new JToolBar();

            _toolBar.setFloatable(false);
            _toolBar.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5) );

            _btnCopyWLW = new JButton();
                _btnCopyWLW.setIcon       ( _newImageIcon("copy") );
                _btnCopyWLW.setToolTipText( MenuSpec.printKeyCombination(Texts.ScrEdt_tlbCopyWLW, "U", MenuSpec.KModCtrlShift) );
            _toolBar.add(_btnCopyWLW);

            _btnCopyAST = new JButton();
                _btnCopyAST.setIcon       ( _newImageIcon("copy_ast") );
                _btnCopyAST.setToolTipText( MenuSpec.printKeyCombination(Texts.ScrEdt_tlbCopyAST, "C", MenuSpec.KModCtrlShift) );
            _toolBar.add(_btnCopyAST);

            _toolBar.addSeparator();

            _btnEOF = new JButton();
                _btnEOF.setIcon       ( _newImageIcon("eof") );
                _btnEOF.setToolTipText( MenuSpec.printKeyCombination(Texts.ScrEdt_tlbSndEOF, "D", MenuSpec.KModCtrl) );
            _toolBar.add(_btnEOF);

            _btnSIGINT = new JButton();
                _btnSIGINT.setIcon       ( _newImageIcon("sigint") );
                _btnSIGINT.setToolTipText( MenuSpec.printKeyCombination(Texts.ScrEdt_tlbSndSIGINT, "C", MenuSpec.KModCtrl) );
            _toolBar.add(_btnSIGINT);

            _btnSIGKILL = new JButton();
                _btnSIGKILL.setIcon       ( _newImageIcon("sigkill") );
                _btnSIGKILL.setToolTipText( MenuSpec.printKeyCombination(Texts.ScrEdt_tlbSndSIGKILL, "K", MenuSpec.KModCtrlAltShift) );
            _toolBar.add(_btnSIGKILL);

            _toolBar.addSeparator();

            _btnEnvVar = new JButton();
                _btnEnvVar.setIcon       ( _newImageIcon("env_var") );
                _btnEnvVar.setToolTipText( Texts.ScrEdt_tlbEnvVars  );
                _btnEnvVar.addActionListener( new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e)
                    {
                        final ExecutorService  executor = Executors.newSingleThreadExecutor();
                        final SavedEnvVarState sevs     = new SavedEnvVarState();

                        if( !sevs.loadSpecific() ) {
                            if( !sevs.loadDefault() ) sevs.initDefault();
                        }

                        SwingUtilities.invokeLater( () -> {
                            executor.submit( () -> {
                                if( _evEditor != null && _evEditor.mainWindow().isVisible() ) {
                                    _evEditor.mainWindow().toFrontAndFocus();
                                }
                                else {
                                    _evEditor = new JxMakeEnvVarEditor(_useDCT) {
                                        @Override
                                        public void onSaveDefault(final JKeyValueTable.StateHashMap refTable, final JKeyValueTable.StateHashMap usrTable)
                                        {
                                            sevs.setDataFrom(refTable, usrTable, _evEditor);
                                            sevs.saveDefault();
                                        }

                                        @Override
                                        public void onLoadDefault()
                                        {
                                            if( !sevs.loadDefault() ) sevs.initDefault();
                                            _evEditor.initializeFromMap(sevs.refTable, sevs.usrTable);
                                        }
                                    };

                                    _evEditor.initialize();
                                    _evEditor.initializeFromMap(sevs.refTable, sevs.usrTable);
                                    _evEditor.waitUntilClosed(false);

                                    sevs.setDataFrom(_evEditor);

                                    _customEnvVar = sevs.effTable;
                                    _evEditor     = null;

                                    sevs.saveSpecific();
                                }
                            } );
                            executor.shutdown();
                            SwingUtilities.invokeLater(_textField::requestFocusInWindow);
                        } );
                    }
                } );
            _toolBar.add(_btnEnvVar);

            _toolBar.addSeparator();

            _btnFSzInc = new JButton();
                _btnFSzInc.setIcon       ( _newImageIcon("fsize_inc") );
                _btnFSzInc.setToolTipText( Texts.ScrEdt_tlbFSizeInc   );
                _btnFSzInc.addActionListener( new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e)
                    {
                        _adjustFontSize(+1);
                        SwingUtilities.invokeLater(_textField::requestFocusInWindow);
                    }
                } );
            _toolBar.add(_btnFSzInc);

            _btnFSzDec = new JButton();
                _btnFSzDec.setIcon       ( _newImageIcon("fsize_dec") );
                _btnFSzDec.setToolTipText( Texts.ScrEdt_tlbFSizeDec   );
                _btnFSzDec.addActionListener( new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e)
                    {
                        _adjustFontSize(-1);
                        SwingUtilities.invokeLater(_textField::requestFocusInWindow);
                    }
                } );
            _toolBar.add(_btnFSzDec);

            _btnFSzRst = new JButton();
                _btnFSzRst.setIcon       ( _newImageIcon("fsize_rst") );
                _btnFSzRst.setToolTipText( Texts.ScrEdt_tlbFSizeRst   );
                _btnFSzRst.addActionListener( new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e)
                    {
                        _adjustFontSize(0);
                        SwingUtilities.invokeLater(_textField::requestFocusInWindow);
                    }
                } );
            _toolBar.add(_btnFSzRst);

            _toolBar.addSeparator();

            _btnTReset = new JButton();
                _btnTReset.setIcon       ( _newImageIcon("reset") );
                _btnTReset.setToolTipText( MenuSpec.printKeyCombination(Texts.ScrEdt_tlbTermReset, "ESC ESC c", 0) );
            _toolBar.add(_btnTReset);

            _toolBar.addSeparator();

            _toolBar.add( Box.createHorizontalGlue() );
            _lblTitle = new JLabel();
            _toolBar.add(_lblTitle);
            _toolBar.add( Box.createHorizontalGlue() );

        _panel.add(_toolBar, BorderLayout.NORTH);

        // Create text pane
        final Font consoleFont = baseFont.deriveFont( baseFont.getSize() + 2.0f );

        _textPane = new JTextPane();

            _textPane.setFont(consoleFont);
            _textPane.setBackground(DefaultBackgroundColor);
            _textPane.setForeground(DefaultForegroundColor);
            _textPane.setOpaque(true);
            _textPane.setEditable(false);

            _textPane.setCaret( new ConsoleCaret( _computeCellWidth(), _computeCellHeight(), _computeCellAscent(), _computeCellDescent() ) );

            //*
            _textPane.setEditorKit( new StyledEditorKit() {
                private final ViewFactory _factory = new GridViewFactory( _textPane, _computeCellWidth(), _computeCellHeight() );

                @Override
                public ViewFactory getViewFactory()
                { return _factory; }
            } );
            //*/

        _textSDoc = _textPane.getStyledDocument();

        _pilcrowIcon.updateFromTextPane(_textPane);

        // Create scroll pane for the text pane
        final JPanel noWrapPanel = new JPanel( new BorderLayout() );

        noWrapPanel.add(_textPane);

        final JScrollPane scrollPane = new JScrollPane(
            noWrapPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        final int scInc = _textPane.getFontMetrics( _textPane.getFont() ).getHeight() * 2;

        scrollPane.getHorizontalScrollBar().setUnitIncrement(scInc);
        scrollPane.getVerticalScrollBar  ().setUnitIncrement(scInc);

        // Create layered pane
        final JLayeredPane layeredPane = new JLayeredPane();

            layeredPane.setLayout( new OverlayLayout(layeredPane) );

            // Create overlay panel and label
            _panelOverlay = new JPanel( new GridBagLayout() );
            _panelOverlay.setOpaque(false);
            _panelOverlay.setVisible(true);

                final JLabel overlayLabel = new JLabel( Texts.ScrEdt_OverlayText(usePty4J) );
                    overlayLabel.setFont( _textPane.getFont() );
                    overlayLabel.setForeground( _textPane.getForeground() );
                    overlayLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    overlayLabel.setVerticalAlignment(SwingConstants.CENTER);
                    overlayLabel.setHorizontalTextPosition(SwingConstants.CENTER);

                final GridBagConstraints gbc = new GridBagConstraints();
                    gbc.gridx   = 0;
                    gbc.gridy   = 0;
                    gbc.weightx = 1.0;
                    gbc.weighty = 1.0;
                    gbc.anchor  = GridBagConstraints.CENTER;
                    gbc.fill    = GridBagConstraints.BOTH;

                _panelOverlay.add(overlayLabel, gbc);

        // Add both components to layered panel
        layeredPane.add(scrollPane   , JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(_panelOverlay, JLayeredPane.PALETTE_LAYER);

        _panel.add(layeredPane, BorderLayout.CENTER);

        // Create bottom panel with border layout
        final JPanel bottomPanel = new JPanel( new BorderLayout() );

            _btnCompose = new JButton();
                _btnCompose.setIcon       ( _newImageIcon("edit_cmd")  );
                _btnCompose.setToolTipText( Texts.ScrEdt_tlbCmdCompose );
                _btnCompose.addActionListener( new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e)
                    {
                        final ExecutorService executor = Executors.newSingleThreadExecutor();

                        SwingUtilities.invokeLater( () -> {
                            executor.submit( () -> {
                                if( _cmdComposer != null && _cmdComposer.mainWindow().isVisible() ) {
                                    _cmdComposer.mainWindow().toFrontAndFocus();
                                }
                                else {
                                    _cmdComposer = new JxMakeCmdComposer(_useDCT) {
                                        @Override
                                        public void onPaste(final String[] command)
                                        { _textField.setText( String.join(" ", command) ); }

                                        @Override
                                        public void onPasteAndExecute(final String[] command)
                                        {
                                            onPaste(command);
                                            _textField.dispatchEvent( new KeyEvent(
                                                _textField, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, MenuSpec.VK_ENTER, '\n'
                                            ) );
                                        }
                                    };
                                    _cmdComposer.initialize();
                                    _cmdComposer.setCommandRegistry(JxMakeShell.commandRegistry);
                                    _cmdComposer.waitUntilClosed(false);
                                }
                            } );
                            executor.shutdown();
                            SwingUtilities.invokeLater(_textField::requestFocusInWindow);
                        } );
                    }
                } );
            bottomPanel.add(_btnCompose, BorderLayout.WEST);

            _undoManager = new UndoManager();
            _textField   = new JTextField();
                _textField.enableInputMethods(true);
                _textField.setFont(consoleFont);
                _textField.getDocument().addUndoableEditListener(_undoManager);
            bottomPanel.add(_textField, BorderLayout.CENTER);

        _panel.add(bottomPanel, BorderLayout.SOUTH);

        // Initialize command history and shell
        final String cchPath = JxMakeRootPane.getPath_consoleCommandHistory();

        if(cchPath == null) {
            _commandHistory = null;
        }
        else {
            // Instantiate and load
            _commandHistory = new CommandHistory(cchPath, COMMAND_HISTORY_SIZE);
            _commandHistory.load();
            // Ensure the data is saved on exit
            Runtime.getRuntime().addShutdownHook( new Thread( () -> {
                _commandHistory.save();
            } ) );
        }

        _shell = new JxMakeShell(this);

        // Add event handler to initialize the buffer
        _textPane.addComponentListener( new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e)
            {
                // No need to continue if already initialized
                if( !_initBuffer( SwingUtilities.getRootPane( (Component) e.getSource() ) ) ) return;

                _printPrompt();
                _renderBuffer_impl();

                SwingUtilities.invokeLater( () -> {
                    scrollPane.revalidate();
                    scrollPane.getViewport().setViewPosition( new Point(0, 0) );
                    SwingUtilities.invokeLater(_textPane::repaint);
                    SwingUtilities.invokeLater(_textField::requestFocusInWindow);
                } );
            }
        } );

        // Add event handler to focus to the text field when the console is displayed
        _panel.addHierarchyListener( e -> {
            final long flags = e.getChangeFlags();
            if( ( flags & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.SHOWING_CHANGED) ) != 0 ) {
                if( _panel.getParent() != null ) SwingUtilities.invokeLater(_textField::requestFocusInWindow);
            }
        } );

        // Add event handlers to handle user commands on the text pane, text field, and toolbar
        _addEHs_handleUserCommand();
    }

    public int getConsoleColumns()
    { return _screenCols(); }

    public CommandHistory commandHistory()
    { return _commandHistory; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class StyledRtfTransferable implements Transferable {

        private static final DataFlavor[] _flavors = {
                DataFlavor.stringFlavor,
            new DataFlavor("text/rtf;class=java.io.InputStream", "RTF")
        };

        private final String _plain;
        private final String _rtf;

        public StyledRtfTransferable(final String plain, final String rtf)
        {
            _plain = plain;
            _rtf   = rtf;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors()
        { return _flavors; }

        @Override
        public boolean isDataFlavorSupported(final DataFlavor flavor)
        {
            for(final DataFlavor f : _flavors) {
                if( f.equals(flavor) ) return true;
            }

            return false;
        }

        @Override
        public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException
        {
            if( flavor.equals(DataFlavor.stringFlavor) ) return _plain;

            if( flavor.getMimeType().startsWith("text/rtf") ) return new ByteArrayInputStream( _rtf.getBytes() );

            throw new UnsupportedFlavorException(flavor);
        }

    } // class StyledRtfTransferable

    private static class StyledHtmlTransferable implements Transferable {

        private static final DataFlavor[] _flavors = {
                DataFlavor.stringFlavor,
            new DataFlavor("text/html;class=java.lang.String", "HTML")
        };

        private final String _plain;
        private final String _html;

        public StyledHtmlTransferable(final String plain, final String html)
        {
            _plain = plain;
            _html  = html;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors()
        { return _flavors; }

        @Override
        public boolean isDataFlavorSupported(final DataFlavor flavor)
        {
            for(final DataFlavor f : _flavors) {
                if( f.equals(flavor) ) return true;
            }

            return false;
        }

        @Override
        public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException
        {
            if( flavor.equals(DataFlavor.stringFlavor) ) return _plain;

            if( flavor.getMimeType().startsWith("text/html") ) return _html;

            throw new UnsupportedFlavorException(flavor);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public static String toHTML(final JTextPane textPane)
        {
            final StyledDocument doc = textPane.getStyledDocument();
            final int            beg = textPane.getSelectionStart();
            final int            end = textPane.getSelectionEnd  ();
            final StringBuilder  sb  = new StringBuilder();

            // Open the <div>
            final Color dbg = textPane.getBackground();

            sb.append("<div style=\"background-color:rgb(")
              .append( dbg.getRed() ).append(",").append( dbg.getGreen() ).append(",").append( dbg.getBlue() )
              .append(");\">");

            // Generate the <span> ... </span>
            for(int i = beg; i < end;) {

                final Element      elem  = doc.getCharacterElement(i);
                final AttributeSet attrs = elem.getAttributes();
                final int          len   = elem.getEndOffset() - i;

                final Color  fg   = StyleConstants.getForeground(attrs);
                final Color  bg   = StyleConstants.getBackground(attrs);
                      String text = "";

                try {
                    text = doc.getText(i, len);
                }
                catch(final BadLocationException ignored) {}

               text = text.replace("&" , "&amp;" )
                          .replace("<" , "&lt;"  )
                          .replace(">" , "&gt;"  )
                          .replace(" " , "&nbsp;")
                          .replace("\n", "<br/>" );

                sb.append("<span style=\"");
                sb.append("font-family:").append( StyleConstants.getFontFamily(attrs) ).append(";"  );
                sb.append("font-size:"  ).append( StyleConstants.getFontSize  (attrs) ).append("pt;");

                if( StyleConstants.isBold  (attrs) ) sb.append("font-weight:bold;");
                if( StyleConstants.isItalic(attrs) ) sb.append("font-style:italic;");

                if(fg != null) sb.append("color:rgb(")
                                 .append( fg.getRed() ).append(",").append( fg.getGreen() ).append(",").append( fg.getBlue() )
                                 .append(");");

                if(bg != null) sb.append("background-color:rgb(")
                                 .append( bg.getRed() ).append(",").append( bg.getGreen() ).append(",").append( bg.getBlue() )
                                 .append(");");


                final boolean underline = StyleConstants.isUnderline    (attrs);
                final boolean strike    = StyleConstants.isStrikeThrough(attrs);

                if(underline || strike) {
                    sb.append("text-decoration:");
                    if(underline) sb.append(" underline"   );
                    if(strike   ) sb.append(" line-through");
                    sb.append(";");
                }

                sb.append("\">").append(text).append("</span>");

                i += len;

            } // for

            // Clsoe the <div>
            sb.append("</div>");

            // Return the HTML string
            return sb.toString();
        }

    } // class StyledHtmlTransferable

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String[] _tokenize(final String input, final boolean preserveTrailing)
    {
        final ArrayList<String> tokens        = new ArrayList<>();
        final StringBuilder     current       = new StringBuilder();

              boolean           inSingleQuote = false;
              boolean           inDoubleQuote = false;
              boolean           unescape      = false;
              boolean           escaping      = false;

        for(int i = 0; i < input.length(); ++i) {

            final char c = input.charAt(i);

            if(escaping) {
                if(inDoubleQuote) current.append('\\');
                current.append(c);
                escaping = false;
            }
            else if(c == '\\') {
                if(inDoubleQuote) escaping = true;      // Escape next character
                else              current.append('\\'); // Treat as literal backslash outside double quotes
            }
            else if(c == '\'' && !inDoubleQuote) {
                // Toggle single quote mode
                inSingleQuote = !inSingleQuote;
            }
            else if(c == '"' && !inSingleQuote) {
                // Set flag
                if(inDoubleQuote) unescape = true;
                // Toggle double quote mode
                inDoubleQuote = !inDoubleQuote;
            }
            else if( Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote ) {
                // Token boundary
                if( current.length() > 0 ) {
                    tokens.add( unescape ? XCom.unescapeString( current.toString() ) : current.toString() );
                    current.setLength(0);
                    unescape = false;
                }
            }
            else {
                current.append(c);
            }

        } // for

        // Add last token if any
        if( preserveTrailing || current.length() > 0 ) tokens.add( unescape ? XCom.unescapeString( current.toString() ) : current.toString() );

        // Return the tokens
        return tokens.toArray( new String[0] );
    }

    private static String[] _tokenize(final String input)
    { return _tokenize(input, false); }

    private static int _lastIndexOfSpace(final String input)
    {
        int     lastSpace     = -1;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for( int i = 0; i < input.length(); ++i ) {

            char c = input.charAt(i);

                 if(                        c == '\'' && !inDoubleQuote                   ) inSingleQuote = !inSingleQuote;
            else if(                        c == '"'  && !inSingleQuote                   ) inDoubleQuote = !inDoubleQuote;
            else if( Character.isWhitespace(c)        && !inSingleQuote && !inDoubleQuote ) lastSpace = i;

        } // for

        return lastSpace;
    }

    private static String _quotePartsWithSpaces(final String path)
    {
        if( path == null || path.isEmpty() ) return path;

        final String[] parts = path.split(SysUtil._InternalDirSepStr);

        for( int i = 0; i < parts.length; ++i ) {
            parts[i] = XCom.re_quoteSQIfContainsWhitespace( parts[i] );
        }

        final String result = String.join(SysUtil._InternalDirSepStr, parts);

        if( path.startsWith(SysUtil._InternalDirSepStr) && !result.startsWith(SysUtil._InternalDirSepStr) ) {
            return "/" + result;
        }

        return result;
    }

    private static String _normalizeQuotedPath(final String path)
    {
        if( path == null || path.isEmpty() ) return path;

        final String[] parts = path.split(SysUtil._InternalDirSepStr);

        for( int i = 0; i < parts.length; ++i ) {
            final String part = parts[i];
            if( ( part.length() >= 2 ) && ( part.startsWith("'" ) && part.endsWith("'" ) ||
                                            part.startsWith("\"") && part.endsWith("\"") )
            ) {
                parts[i] = part.substring( 1, part.length() - 1 );
            }
        }

        final String result = String.join(SysUtil._InternalDirSepStr, parts);

        if( path.startsWith(SysUtil._InternalDirSepStr) && !result.startsWith(SysUtil._InternalDirSepStr) ) {
            return "/" + result;
        }

        return result;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ProcessHandle _processHandle  = null;
    private int           _lastExitCode   = 0;
    private StringWriter  _phStringWriter = new StringWriter();
    private PrintWriter   _phPrintWriter  = new PrintWriter(_phStringWriter);

    @SuppressWarnings("deprecation")
    private void _printMsgAndPrompt_impl(final boolean msgIsErr, final String msgStr, final boolean showPrompt)
    {
        if(msgStr != null) _putString(msgStr, msgIsErr);

        if(showPrompt) {
            _putString( '[' + SysUtil.ellipsisPath( SysUtil.getCWD() ) + "]$ " );
        }

        try {
            _textPane.setCaretPosition( _textPane.getDocument().getLength() ) ;
            _textPane.scrollRectToVisible( _textPane.modelToView( _textPane.getCaretPosition() ) );
            /*
            final Shape caretShape = _textPane.modelToView2D( _textPane.getCaretPosition() );
            if(caretShape != null) {
                final Rectangle2D rect = caretShape.getBounds2D();
                _textPane.scrollRectToVisible( rect.getBounds() );
            }
            //*/
        }
        catch(final BadLocationException e) {
            // Ignore error
        }
    }

    private void _printMsgAndPrompt(final boolean msgIsErr, final String msgStr) { _printMsgAndPrompt_impl(msgIsErr, msgStr, true ); }
    private void _printMsgAndPrompt(                        final String msgStr) { _printMsgAndPrompt_impl(false   , msgStr, true ); }
    private void _printMsg         (final boolean msgIsErr, final String msgStr) { _printMsgAndPrompt_impl(msgIsErr, msgStr, false); }
    private void _printMsg         (                        final String msgStr) { _printMsgAndPrompt_impl(false   , msgStr, false); }
    private void _printPrompt      (                                           ) { _printMsgAndPrompt_impl(false   , null  , true ); }

    public int getLastExitCode()
    { return _lastExitCode; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final List< Class<? extends Throwable> > EXCEPTION_SHORT_MODE_ALLOWED = Arrays.asList(
        IllegalArgumentException.class, IllegalStateException.class, IOException.class, RuntimeException.class, SecurityException.class
    );

    private static boolean _isExeptionShortModeAllowed(Throwable e)
    {
        for( final Class<? extends Throwable> allowed : EXCEPTION_SHORT_MODE_ALLOWED ) {
            if( allowed.isAssignableFrom( e.getClass() ) ) return true;
        }

        return false;
    }

    private String _buildShortExceptionText_impl(final Throwable e)
    {
        // Collect class names and messages
        final List<String> names    = new ArrayList<>();
        final List<String> messages = new ArrayList<>();
              Throwable    eCur     = e;
              String       prevName = null;

        while(eCur != null) {

            final String name = eCur.getClass().getSimpleName();

            if( !name.equals(prevName) ) {
                names.add(name);
                prevName = name;
            }

            final String msg = eCur.getMessage();

            if( msg != null && !msg.trim().isEmpty() ) messages.add( msg.trim() );

            eCur = eCur.getCause();

        } // while

        // Build chain header
        final StringBuilder sb = new StringBuilder();

        sb.append(ASeq_Attr_RstAll).append(ASeq_Attr_SetBold).append(ASeq_Attr_SetBrightCyan);

        for( int i = 0; i < names.size(); ++i ) {

            if(i > 0) sb.append(":");

            sb.append( names.get(i) );

        } // for

        if( !messages.isEmpty() ) sb.append(":\n");

        sb.append(ASeq_Attr_SetItalic).append(ASeq_Attr_SetBrightRed);

        // Append messages, trimming duplicates
        if( !messages.isEmpty() ) {

            for( int i = 0; i < messages.size(); ++i ) {
                String msg = messages.get(i);

                // Remove any inner message text from outer
                for(int j = i + 1; j < messages.size(); ++j) {

                    final String inner = messages.get(j);

                    if( msg.contains(inner) ) {
                        msg = msg.replace(inner, "").trim();
                        if( msg.endsWith(":") ) msg = msg.substring( 0, msg.length() - 1 ).trim(); // Strip trailing colon if left
                    }

                } // for

                if( !msg.isEmpty() ) sb.append(msg).append("\n");

            } // for
        }

        sb.append(ASeq_Attr_RstAll);

        return sb.toString();
    }

    private void _printException_impl(final Exception e, final boolean shortMode)
    {
        // Generate the stack trace
        e.printStackTrace(_phPrintWriter);

        String errStr = _phStringWriter.toString();

        _phPrintWriter.flush();
        _phPrintWriter.checkError();
        _phStringWriter.getBuffer().setLength(0);

        // Print the stack trace if requested
        if( XCom.enableAllExceptionStackTrace() ) SysUtil.stdErr().print(errStr);

        // Determine whether to send the short or full exception text to the console
        if(shortMode) {
            // Get the last exception on the chain
            Throwable eCur = e;
            Throwable eEnd = e;
            while(eCur != null) {
                eEnd = eCur;
                eCur = eCur.getCause();
            }
            // Generate the short text the exception is in the list
            if( _isExeptionShortModeAllowed(eEnd) ) errStr = _buildShortExceptionText_impl(e);
        }

        // Send the stack trace to the console
        _printMsgAndPrompt(true, errStr);
    }

    private void _printException(final Exception e)
    { _printException_impl(e, false); }

    private void _printShortException(final Exception e)
    { _printException_impl(e, true ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String Console_CopyAsStyledTextHTML = "Console.CopyAsStyledTextHTML";
    public static String Console_CopyAsStyledTextRTF  = "Console.CopyAsStyledTextRTF";

    private boolean _beepIfProcessInactive()
    {
        if(_processHandle == null) {
            SwingApp.playBeepSound();
            return true;
        }

        return false;
    }

    private boolean _beepIfProcessActive()
    {
        if(_processHandle != null) {
            SwingApp.playBeepSound();
            return true;
        }

        return false;
    }

    private void _showPopupForCompletion()
    {
        // Generate the match strings
        final String       text          = _textField.getText();
        final String[]     parts         = _tokenize(text, true);

        final int          partsCount    = parts.length;
        final String       lastPart      = (partsCount > 0) ? parts[partsCount - 1].trim() : "";
        final boolean      lastIsCmdSubs = (partsCount > 1) && JxMakeShell.isCmdSubs( parts[partsCount - 2].trim() );
        final boolean      forCmd        = (partsCount < 2) || lastIsCmdSubs;
        final String       cmdFilter     = lastIsCmdSubs ? lastPart : text;
              List<String> matches       = null;
              String       parentDir     = null;

        if(forCmd) {

            final Set <String> completions = JxMakeShell.commandRegistry.keySet();

            matches = completions.stream().filter(s -> s.contains(cmdFilter) )
                                          .sorted( Comparator
                                              .comparing    ( (final String s) -> !s.equals    (cmdFilter) )
                                              .thenComparing( (final String s) -> !s.startsWith(cmdFilter) )
                                              .thenComparing( Comparator.naturalOrder()               )
                                          )
                                          .collect( Collectors.toList() );

        }
        else {

            String lsDir  = null;
            String prefix = null;

            if( lastPart.isEmpty() ) {
                lsDir  = SysUtil.getCWD();
                prefix = "";
            }
            else {
                final String checkDir = SysUtil.resolveAbsolutePath(lastPart);

                if( SysUtil.pathIsValidDirectory(checkDir) ) {
                    lsDir  = checkDir;
                    prefix = "";
                }

                else {

                    parentDir = SysUtil.getDirName(checkDir);

                    if( SysUtil.pathIsValidDirectory(parentDir) ) {
                        lsDir  = parentDir;
                        prefix = SysUtil.extractLastPathPart(checkDir);
                    }
                    else {
                        lsDir  = SysUtil.getCWD();
                        prefix = lastPart;
                    }

                }

            }

            try(
                final Stream<Path> stream = Files.list( Paths.get(lsDir) )
            ) {

                final int    MaxEntry  = 500;
                final String prefixStr = prefix;

                matches = stream.map    ( p -> SysUtil.normalizeDirectorySeparators( p.getFileName().toString() ) )
                                .filter ( name -> name.startsWith(prefixStr)                                      )
                                .limit  ( MaxEntry + 1                                                            )
                                .collect( Collectors.toList()                                                     );

                if( matches.size() > MaxEntry ) {
                    _printShortException( new IllegalStateException("⚠️ ∑⇥ > " + MaxEntry + " ⟦" + lsDir + "⟧") );
                    return;
                }

            }
            catch(final IOException e) {
                _printShortException(e);
                return;
            }
        }

        if( matches == null || matches.isEmpty() ) return;

        // Helper function to perform replacement
        final String           parentPart      = parentDir;
        final Consumer<String> applyCompletion = (selected) -> {

            selected = XCom.re_quoteSQIfContainsWhitespace(selected);

            final int    lastSpace = _lastIndexOfSpace(text);
            final String lastWord  = (lastSpace >= 0) ? text.substring(lastSpace + 1) : text;
                  String newText   = null;

            final String trimmedText = (lastSpace >= 0) ? text.substring(0, lastSpace + 1) : "";

            if( !forCmd && !lastWord.isEmpty() ) {
                if( ".".equals(lastWord) || "..".equals(lastWord) ) {
                    newText = text + SysUtil._InternalDirSep + selected;
                }
                else if( ("."  + SysUtil._InternalDirSep).equals(lastWord) || (".." + SysUtil._InternalDirSep).equals(lastWord) ) {
                    newText = text + selected;
                }

                else if(
                    lastWord.endsWith(SysUtil._InternalDirSepStr      ) ||
                    lastWord.endsWith(SysUtil._InternalDirSepStr + "'") ||
                    lastWord.endsWith(SysUtil._InternalDirSepStr + '"')
                ) {
                    newText = text + selected;
                }
                else if( SysUtil.pathIsValidDirectory( _normalizeQuotedPath(lastWord) ) ) {
                    newText = text + SysUtil._InternalDirSep + selected;
                }
                else if( parentPart != null && lastWord.contains(SysUtil._InternalDirSepStr) ) {
                                                                        newText  = trimmedText + _quotePartsWithSpaces(parentPart);
                    if( !newText.endsWith(SysUtil._InternalDirSepStr) ) newText += SysUtil._InternalDirSep;
                                                                        newText += selected;
                }
            }

            if(newText == null) {
                boolean noQuoteTrimmedText = forCmd || text.startsWith(trimmedText);
                newText = ( noQuoteTrimmedText ? trimmedText : _quotePartsWithSpaces(trimmedText) ) + selected;
                if( forCmd && !selected.isEmpty() ) newText += ' ';
            }

            _textField.setText(newText);

        }; // applyCompletion

        // Check if there is only one match
        if( matches.size() == 1 ) {
            applyCompletion.accept( matches.get(0) );
            return;
        }

        // Show selection box
        final JList<String> list = new JList<>( matches.toArray( new String[0] ) );

            list.setFocusTraversalKeysEnabled(false);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setSelectedIndex(0);

        final int MaxRows   = 25;
              int rowHeight = list.getFixedCellHeight();

        if( rowHeight <= 0 && list.getModel().getSize() > 0 ) rowHeight = list.getCellBounds(0, 0).height;
        if( rowHeight <= 0                                  ) rowHeight = list.getFontMetrics( list.getFont() ).getHeight();

        final int         maxHeight       = _textField.getLocationOnScreen().y;
        final int         preferredHeight = rowHeight * Math.min( MaxRows, list.getModel().getSize() );
        final JPopupMenu  popup           = new JPopupMenu();
        final JScrollPane scroll          = new JScrollPane(list);

            scroll.setPreferredSize( new Dimension( _textField.getWidth(), Math.min(preferredHeight, maxHeight) ) );
            popup.add(scroll);

        list.addFocusListener( new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e)
            { SwingUtilities.invokeLater( () -> list.requestFocusInWindow() ); }
        } );

        list.addMouseListener( new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e)
            {
                applyCompletion.accept( list.getSelectedValue() );
                popup.setVisible(false);
            }
        } );

        final String mkSelectItem = "selectItem";
        list.getInputMap(JComponent.WHEN_FOCUSED).put( KeyStroke.getKeyStroke(MenuSpec.VK_ENTER, MenuSpec.KModNone), mkSelectItem );
        list.getInputMap(JComponent.WHEN_FOCUSED).put( KeyStroke.getKeyStroke(MenuSpec.VK_TAB  , MenuSpec.KModNone), mkSelectItem );
        list.getActionMap().put( mkSelectItem, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                applyCompletion.accept( list.getSelectedValue() );
                popup.setVisible(false);
            }
        } );

        final String mkClosePopup = "closePopup";
        list.getInputMap(JComponent.WHEN_FOCUSED).put( KeyStroke.getKeyStroke(MenuSpec.VK_ESCAPE    , MenuSpec.KModNone), mkClosePopup );
        list.getInputMap(JComponent.WHEN_FOCUSED).put( KeyStroke.getKeyStroke(MenuSpec.VK_BACK_SPACE, MenuSpec.KModNone), mkClosePopup );
        list.getInputMap(JComponent.WHEN_FOCUSED).put( KeyStroke.getKeyStroke(MenuSpec.VK_BACK_SLASH, MenuSpec.KModNone), mkClosePopup );
        list.getInputMap(JComponent.WHEN_FOCUSED).put( KeyStroke.getKeyStroke(MenuSpec.VK_SLASH     , MenuSpec.KModNone), mkClosePopup );
        list.getActionMap().put( mkClosePopup, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            { popup.setVisible(false); }
        } );

        popup.show( _textField, 0, -popup.getPreferredSize().height );

        list.requestFocusInWindow();
    }

    private void _addEHs_handleUserCommand()
    {
        final InputMap  imPane  = _textPane .getInputMap(JComponent.WHEN_FOCUSED);
        final InputMap  imField = _textField.getInputMap(JComponent.WHEN_FOCUSED);

        final ActionMap amPane  = _textPane .getActionMap();
        final ActionMap amField = _textField.getActionMap();

        // Copy without Line Wraps - Ctrl+Shift+U
        final String    mkCtrlShiftU = "tpf_CopyWithoutLineWraps";
        final KeyStroke ksCtrlShiftU = KeyStroke.getKeyStroke(MenuSpec.VK_U, MenuSpec.KModCtrlShift);
        final Action    acCtrlShiftU = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                try {
                    String text = null;

                    if( _textField.isFocusOwner() ) {
                        text = _textField.getSelectedText();
                        if( text == null || text.isEmpty() ) text = _textField.getText();
                    }
                    else {
                        final Document doc    = _textPane.getDocument();
                        final Element  root   = doc.getDefaultRootElement();
                        final int      begRow = root.getElementIndex( _textPane.getSelectionStart() );
                        final int      endRow = root.getElementIndex( _textPane.getSelectionEnd  () );
                        final int      selBeg = _textPane.getSelectionStart();
                        final int      selEnd = _textPane.getSelectionEnd  ();

                        final StringBuilder sb = new StringBuilder();

                        for(int row = begRow; row <= endRow; ++row) {

                            final Element lineElem  = root.getElement(row);
                            final int     begOffset = Math.max( selBeg, lineElem.getStartOffset() );
                            final int     endOffset = Math.min( selEnd, lineElem.getEndOffset  () );
                                  String  lineText  = doc.getText(begOffset, endOffset - begOffset);

                            if( _screenBuffer.lwrap[row] && lineText.endsWith("\n") ) {
                                lineText = lineText.substring( 0, lineText.length() - 1 );
                            }

                            sb.append( XCom.re_rtrim(lineText) );

                        } // for

                        text = sb.toString();
                    }

                    if( text == null || text.isEmpty() ) {
                         SwingApp.playBeepSound();
                         return;
                    }

                    final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

                    clipboard.setContents( new StringSelection(text), null );

                 }
                catch(final Exception ex) {
                    _printException(ex);
                }

                SwingUtilities.invokeLater(_textField::requestFocusInWindow);
            }
        };

        imPane.put(ksCtrlShiftU, mkCtrlShiftU); imField.put(ksCtrlShiftU, mkCtrlShiftU);
        amPane.put(mkCtrlShiftU, acCtrlShiftU); amField.put(mkCtrlShiftU, acCtrlShiftU);
        _btnCopyWLW.addActionListener(acCtrlShiftU);

        // Copy as Styled Text - Ctrl+Shift+C
        final XCom.TriConsumer<ActionEvent, Boolean, Boolean> textFieldPaneCopyAction = (e, asHTML, forceTextPane) -> {

            try {
                if( !forceTextPane && _textField.isFocusOwner() ) {
                    String text = _textField.getSelectedText();

                    if( text == null || text.isEmpty() ) text = _textField.getText();

                    if( text == null || text.isEmpty() ) {
                        SwingApp.playBeepSound();
                        return;
                    }

                    final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

                    clipboard.setContents( new StringSelection(text), null );
                }
                else {
                    if(!asHTML) {
                        final int beg = _textPane.getSelectionStart();
                        final int end = _textPane.getSelectionEnd  ();
                        if(beg == end) {
                            SwingApp.playBeepSound();
                            return;
                        }

                        final StyledDocument        srcDoc = _textPane.getStyledDocument();
                        final DefaultStyledDocument tmpDoc = new DefaultStyledDocument();

                        // Copy the content
                        for(int pos = beg; pos < end; ) {

                            final Element            run       = srcDoc.getCharacterElement(pos);
                            final int                runStart  = run.getStartOffset();
                            final int                runEnd    = run.getEndOffset();

                            final int                copyStart = Math.max(runStart, beg);
                            final int                copyEnd   = Math.min(runEnd  , end);
                            final int                copyLen   = copyEnd - copyStart;

                            final String             text  = srcDoc.getText(copyStart, copyLen);
                            final AttributeSet       attrs = srcDoc.getCharacterElement(copyStart).getAttributes();
                            final SimpleAttributeSet safeAttrs = new SimpleAttributeSet(attrs);

                            if( StyleConstants.getBackground(safeAttrs) == null ) StyleConstants.setBackground( safeAttrs, _textPane.getBackground() );

                            tmpDoc.insertString( tmpDoc.getLength(), text, safeAttrs );

                            pos = copyEnd;

                        } // for

                        // Now write RTF from the temporary document
                        final ByteArrayOutputStream baos   = new ByteArrayOutputStream();
                        final RTFEditorKit          rtfKit = new RTFEditorKit();
                        rtfKit.write( baos, tmpDoc, 0, tmpDoc.getLength() );

                        final String    plainText = _textPane.getSelectedText();
                        final String    rtfText   = baos.toString(SysUtil._CharEncoding);
                        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

                        clipboard.setContents( new StyledRtfTransferable(plainText, rtfText), null );
                    }
                    else {
                        final String plainText = _textPane.getSelectedText();
                        if( plainText == null || plainText.isEmpty() ) {
                            SwingApp.playBeepSound();
                            return;
                        }

                        final String    htmlText  = StyledHtmlTransferable.toHTML(_textPane);
                        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

                        clipboard.setContents( new StyledHtmlTransferable(plainText, htmlText), null );
                    }
                }
             }
            catch(final Exception ex) {
                _printException(ex);
            }

            SwingUtilities.invokeLater(_textField::requestFocusInWindow);

        }; // textFieldPaneCopyAction

        final String    mkCtrlShiftC = "tpf." + Console_CopyAsStyledTextHTML;
        final KeyStroke ksCtrlShiftC = KeyStroke.getKeyStroke(MenuSpec.VK_C, MenuSpec.KModCtrlShift);
        final Action    acCtrlShiftC = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            { textFieldPaneCopyAction.accept(e, true , false); }
        };

        imPane.put(ksCtrlShiftC, mkCtrlShiftC); imField.put(ksCtrlShiftC, mkCtrlShiftC);
        amPane.put(mkCtrlShiftC, acCtrlShiftC); amField.put(mkCtrlShiftC, acCtrlShiftC);
        _btnCopyAST.addActionListener(acCtrlShiftC);

        amPane.put(Console_CopyAsStyledTextHTML, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            { textFieldPaneCopyAction.accept(e, true , true ); }
        } );

        amPane.put(Console_CopyAsStyledTextRTF, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            { textFieldPaneCopyAction.accept(e, false, true ); }
        } );

        // Send EOF - Ctrl+D
        final String    mkCtrlD = "tpf_SendEOF";
        final KeyStroke ksCtrlD = KeyStroke.getKeyStroke(MenuSpec.VK_D, MenuSpec.KModCtrl);
        final Action    acCtrlD = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                if( _beepIfProcessInactive() ) return;

                _printMsg("^D\n");

                try {
                    _processHandle.closeStdIn();
                }
                catch(final Exception ex) {
                    _printException(ex);
                }

                SwingUtilities.invokeLater(_textField::requestFocusInWindow);
            }
        };

        imPane.put(ksCtrlD, mkCtrlD); imField.put(ksCtrlD, mkCtrlD);
        amPane.put(mkCtrlD, acCtrlD); amField.put(mkCtrlD, acCtrlD);
        _btnEOF.addActionListener(acCtrlD);

        // Send SIGINT - Ctrl+C
        final String    mkCtrlC = "tpf_SendSIGINT";
        final KeyStroke ksCtrlC = KeyStroke.getKeyStroke(MenuSpec.VK_C, MenuSpec.KModCtrl);
        final Action    acCtrlC = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                if( _beepIfProcessInactive() ) return;

                _printMsg("^C\n");

                try {
                    _processHandle.interrupt();
                }
                catch(final Exception ex) {
                    _printException(ex);
                }

                SwingUtilities.invokeLater(_textField::requestFocusInWindow);
            }
        };

        imPane.put(ksCtrlC, mkCtrlC); imField.put(ksCtrlC, mkCtrlC);
        amPane.put(mkCtrlC, acCtrlC); amField.put(mkCtrlC, acCtrlC);
        _btnSIGINT.addActionListener(acCtrlC);

        // Send SIGKILL - Ctrl+Alt+Shift+K
        final String    mkCtrlAltShiftK = "tpf_SendSIGKILL";
        final KeyStroke ksCtrlAltShiftK = KeyStroke.getKeyStroke(MenuSpec.VK_K, MenuSpec.KModCtrlAltShift);
        final Action    acCtrlAltShiftK = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                if( _beepIfProcessInactive() ) return;

                _printMsg("^[^K\n");

                try {
                    _processHandle.kill();
                }
                catch(final Exception ex) {
                    _printException(ex);
                }

                SwingUtilities.invokeLater(_textField::requestFocusInWindow);
            }
        };

        imPane.put(ksCtrlAltShiftK, mkCtrlAltShiftK); imField.put(ksCtrlAltShiftK, mkCtrlAltShiftK);
        amPane.put(mkCtrlAltShiftK, acCtrlAltShiftK); amField.put(mkCtrlAltShiftK, acCtrlAltShiftK);
        _btnSIGKILL.addActionListener(acCtrlAltShiftK);

        // Send "ESC c" (terminal reset)
        final Action acESCc = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                if( _beepIfProcessActive() ) return;

                _putString("\u001B\u001Bc"); // Send flush + RIS
                _printPrompt();

                SwingUtilities.invokeLater(_textField::requestFocusInWindow);
            }
        };

        _btnTReset.addActionListener(acESCc);

        // Send command or stdin - Enter
        final String    mkEnter = "tpf_sendCommandOrStdin";
        final KeyStroke ksEnter = KeyStroke.getKeyStroke(MenuSpec.VK_ENTER, MenuSpec.KModNone);
        final Action    acEnter = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            { _handleCmdAndStdin(); }
        };

        imField.put(ksEnter, mkEnter);
        amField.put(mkEnter, acEnter);

        imPane.put(ksEnter, mkEnter);
        amPane.put(mkEnter, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            { SwingUtilities.invokeLater(_textField::requestFocusInWindow); }
        } );

        // Up/down arrow - command history
        final String    mkHistUp  = "history_Previous";
        final KeyStroke ksHistUp1 = KeyStroke.getKeyStroke(MenuSpec.VK_UP     , MenuSpec.KModNone);
        final KeyStroke ksHistUp2 = KeyStroke.getKeyStroke(MenuSpec.VK_KP_UP  , MenuSpec.KModNone);
        final Action    acHistUp  = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                if( _beepIfProcessActive() ) return;

                if(_commandHistory == null) return;

                // Get the previous text
                final String text = _commandHistory.getPrev();
                if(text != null) _textField.setText(text);
            }
        };

        final String    mkHistDn  = "history_Next";
        final KeyStroke ksHistDn1 = KeyStroke.getKeyStroke(MenuSpec.VK_DOWN   , MenuSpec.KModNone);
        final KeyStroke ksHistDn2 = KeyStroke.getKeyStroke(MenuSpec.VK_KP_DOWN, MenuSpec.KModNone);
        final Action    acHistDn  = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                if( _beepIfProcessActive() ) return;

                if(_commandHistory == null) return;

                // Get the next text
                final String text = _commandHistory.getNext();
                _textField.setText( (text != null) ? text : "" );
            }
        };

        imField.put(ksHistUp1, mkHistUp); imField.put(ksHistDn1, mkHistDn);
        imField.put(ksHistUp2, mkHistUp); imField.put(ksHistDn2, mkHistDn);
        amField.put(mkHistUp , acHistUp); amField.put(mkHistDn , acHistDn);

        // Ctrl+W, Alt+D, Ctrl+K, Ctrl+U - word navigation/manipulation
        final String    mkWNaviCW = "wn_DeleteWordBeforeCursor";
        final KeyStroke ksWNaviCW = KeyStroke.getKeyStroke(MenuSpec.VK_W, MenuSpec.KModCtrl);
        final Action    acWNaviCW = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                try {
                    final int caretPos      = _textField.getCaretPosition();
                    final int prevWordStart = Utilities.getPreviousWord(_textField, caretPos);

                    if(prevWordStart >= 0) _textField.getDocument().remove(prevWordStart, caretPos - prevWordStart);
                }
                catch(final BadLocationException ex) {
                    // Ignore error
                }
            }
        };

        final String    mkWNaviAD = "wn_DeleteWordAfterCursor";
        final KeyStroke ksWNaviAD = KeyStroke.getKeyStroke(MenuSpec.VK_D, MenuSpec.KModAlt);
        final Action    acWNaviAD = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                try {
                    final int    caretPos = _textField.getCaretPosition();
                    final String text     = _textField.getText();
                    final int    length   = text.length();

                    int start = caretPos;
                    while( start < length && Character.isWhitespace( text.charAt(start) ) ) ++start;

                    final int end = Utilities.getWordEnd(_textField, start);

                    if(end > caretPos) _textField.getDocument().remove(caretPos, end - caretPos);
                }
                catch(final BadLocationException ex) {
                    // Ignore error
                }
            }
        };

        final String    mkWNaviCK = "wn_DeleteFromCursorToLineEnd";
        final KeyStroke ksWNaviCK = KeyStroke.getKeyStroke(MenuSpec.VK_K, MenuSpec.KModCtrl);
        final Action    acWNaviCK = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                try {
                    final int caretPos = _textField.getCaretPosition();
                    final int lineEnd  = Utilities.getRowEnd(_textField, caretPos);

                    if(lineEnd > caretPos) _textField.getDocument().remove(caretPos, lineEnd - caretPos);
                }
                catch(final BadLocationException ex) {
                    // Ignore error
                }
            }
        };

        final String    mkWNaviCU = "wn_DeleteFromLineBeginToCursor";
        final KeyStroke ksWNaviCU = KeyStroke.getKeyStroke(MenuSpec.VK_U, MenuSpec.KModCtrl);
        final Action    acWNaviCU = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                try {
                    final int caretPos  = _textField.getCaretPosition();
                    final int lineStart = Utilities.getRowStart(_textField, caretPos);

                    if(caretPos > lineStart) _textField.getDocument().remove(lineStart, caretPos - lineStart);
                }
                catch(final BadLocationException ex) {
                    // Ignore error
                }
            }
        };

        imField.put(ksWNaviCW, mkWNaviCW); imField.put(ksWNaviAD, mkWNaviAD);
        amField.put(mkWNaviCW, acWNaviCW); amField.put(mkWNaviAD, acWNaviAD);

        imField.put(ksWNaviCK, mkWNaviCK); imField.put(ksWNaviCU, mkWNaviCU);
        amField.put(mkWNaviCK, acWNaviCK); amField.put(mkWNaviCU, acWNaviCU);

        // Ctrl+Z and Ctrl+Shift+Z - undo and redo
        final String    mkUMCZ = "te_Undo";
        final KeyStroke ksUMCZ = KeyStroke.getKeyStroke(MenuSpec.VK_Z, MenuSpec.KModCtrl);
        final Action    acUMCZ = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            { if( _undoManager.canUndo() ) _undoManager.undo(); }
        };

        final String    mkUMCSZ = "te_Redo";
        final KeyStroke ksUMCSZ = KeyStroke.getKeyStroke(MenuSpec.VK_Z, MenuSpec.KModCtrlShift);
        final Action    acUMCSZ = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            { if( _undoManager.canRedo() ) _undoManager.redo(); }
        };

        imField.put(ksUMCZ, mkUMCZ); imField.put(ksUMCSZ, mkUMCSZ);
        amField.put(mkUMCZ, acUMCZ); amField.put(mkUMCSZ, acUMCSZ);

        // Handle Tab and Shift+Tab on '_textField'
        final HashSet<AWTKeyStroke> fwKeys = new HashSet<>( _textField.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS ) );
        final HashSet<AWTKeyStroke> bwKeys = new HashSet<>( _textField.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS) );

        fwKeys.remove( KeyStroke.getKeyStroke(MenuSpec.VK_TAB, MenuSpec.KModNone ) );
        bwKeys.remove( KeyStroke.getKeyStroke(MenuSpec.VK_TAB, MenuSpec.KModShift) );

        _textField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS , fwKeys);
        _textField.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, bwKeys);

        _textField.addKeyListener( new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e)
            {
                if( e.getKeyCode() != MenuSpec.VK_TAB ) return;
                e.consume();

                if( _beepIfProcessActive() ) return;

                _showPopupForCompletion();
            }
        } );

        // Forward unmapped key events from '_textPane' to '_textField'
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher( new KeyEventDispatcher() {

            private boolean _forwarding = false;

            @Override
            public boolean dispatchKeyEvent(final KeyEvent e)
            {
                // Prevent recursive forwarding
                if(_forwarding) return false;

                // Only handle events originating from '_textPane'
                if( e.getComponent() != _textPane ) return false;

                // Skip if the event is already mapped in the input map
                final KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);

                if( imPane.get(ks) == null ) return false;

                // Forward the event to '_textField'
                _forwarding = true;

                _textField.requestFocusInWindow();
                _textField.dispatchEvent( new KeyEvent(
                    _textField, e.getID(), e.getWhen(), e.getModifiersEx(), e.getKeyCode(), e.getKeyChar()
                ) );

                _forwarding = false;

                // Consume the original event
                return true;
            }

        } );
    }

    private void _handleCmdAndStdin()
    {
        if( !_pb.available() ) return;

        // Send the user text to the process' stdin
        if(_processHandle != null) {
            // Send the user text
             final String text = _textField.getText();
            _textField.setText("");
            try {
                 _processHandle.putStdIn(text + '\n');
                 if( _shell.stdinEchoEnabled() ) {
                     _putString(text);
                     _putChar('\n');
                 }
            }
            catch(final Exception ex) {
                _printShortException(ex);
            }
            // Ensure the text field has the focus
            SwingUtilities.invokeLater(_textField::requestFocusInWindow);
            return;
        }

        // Save the text to the command history list
        final String text = _textField.getText().trim();

        if( text.isEmpty() ) return;

        if(_commandHistory != null) _commandHistory.put(text);

        // Process the user text as a new command
        String[] command = _tokenize(text);
        _textField.setText("");

        if(command.length == 0) return;

        // Print the command as needed
        if( _shell.getCmdEchoState() ) {
            for(int i = 0; i < command.length; ++i) {
                final String cmdStr = XCom.escapeControlChars( command[i] );
                _putString(
                    cmdStr.contains("\\") ? ('"' + cmdStr + '"')
                                          : XCom.re_quoteSQIfContainsWhitespace(cmdStr)
                );
                if(i < command.length - 1) _putChar(' ');
            }
            _putChar('\n');
        }

        /*
        SysUtil.stdDbg().println( String.join(" ", CommandShell.cmdInvokeFunction( "cat", new String[] { "myfile.txt"} ) ) );
        //*/

        // Create process for the new command
        try {

            // Attempt to execute as a JxMake shell command; if that fails, try translating it
            final String shellResult = _shell.execute(command);

            if(shellResult != null) {
                _printMsgAndPrompt(shellResult);
                command = null;
            }
            else {
                command = _shell.translate(command);

                if( _shell.isCaptureRequired() ) startCapture();
            }

            // Execute as normal shell command
            if(command != null) {

                _processHandle = _pbExec(command, null, !true);

                new Thread( () -> {
                    try {
                        // Wait for the activity to complete
                        while( _processHandle.isActive() ) {
                            try {
                                Thread.sleep(100);
                            }
                            catch(final InterruptedException ignored) {
                                // Restore state
                                Thread.currentThread().interrupt();
                            }
                        }
                        // Wait for the process to exit and get the exit code
                        final int exitCode = _processHandle.waitFor(100);
                        // Finish capture as needed
                        if( _shell.isCaptureRequired() ) _shell.acceptCaptureResult( finishCapture() );
                        // Inform the user about the exit code
                        SwingUtilities.invokeLater( () -> {
                            _printMsgAndPrompt( String.format(
                                                                       ( (exitCode == 0) ? Texts.ScrEdt_ProcExitCode0 : Texts.ScrEdt_ProcExitCodeN ),
                                ASeq_Attr_RstAll + ASeq_Attr_SetBold + ( (exitCode == 0) ? ASeq_Attr_SetBrightGreen   : ASeq_Attr_SetBrightRed     ),
                                exitCode                                                                                                            ,
                                ASeq_Attr_RstAll
                            ) );
                            _processHandle = null;
                            _lastExitCode  = exitCode;
                        } );
                    }
                    catch(final Exception ex) {
                        if(ex instanceof InterruptedException) Thread.currentThread().interrupt(); // Restore state if required
                        _printShortException(ex);
                        try {
                            if(_processHandle != null) _lastExitCode = _processHandle.waitFor(100);
                        }
                        catch(final Exception ignored) {
                            // Restore state if required
                            if(ignored instanceof InterruptedException) Thread.currentThread().interrupt();
                        }
                        _processHandle = null;
                    }
                } ).start();

            } // if

        }
        catch(final Exception ex) {
            _printShortException(ex);
        }

        // Ensure the text field has the focus
        SwingUtilities.invokeLater(_textField::requestFocusInWindow);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class SavedEnvVarState implements Serializable {

        private final AppConfigFile<SavedEnvVarState> _acfDefault;
        private final AppConfigFile<SavedEnvVarState> _acfSpecific;

        public JKeyValueTable.StateHashMap refTable = null;
        public JKeyValueTable.StateHashMap usrTable = null;
        public Map<String, String>         effTable = null;

        public SavedEnvVarState(final boolean noACF)
        {
            _acfDefault  = noACF ? null : new AppConfigFile<>( SavedEnvVarState.class, JxMakeRootPane.getPath_consoleEnvVarForDefault() );
            _acfSpecific = noACF ? null : new AppConfigFile<>( SavedEnvVarState.class, JxMakeRootPane.getPath_consoleEnvVar          () );
        }

        public SavedEnvVarState()
        { this(false); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void setDataFrom(final JKeyValueTable.StateHashMap refTable_, final JKeyValueTable.StateHashMap usrTable_, final JxMakeEnvVarEditor evEditor)
        {
            refTable = refTable_;
            usrTable = usrTable_;
            effTable = evEditor.getFinalCheckedData();
        }

        public void setDataFrom(final JxMakeEnvVarEditor evEditor)
        {
            refTable = evEditor.getRefTable();
            usrTable = evEditor.getUsrTable();
            effTable = evEditor.getFinalCheckedData();
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void saveDefault()
        {
            try {
                _acfDefault.save(this);
            }
            catch(final Exception e) {
                JxMakeRootPane.showExceptionDialogWithScrollPane(e);
            }
        }

        public boolean loadDefault()
        {
            try {
                final SavedEnvVarState res = _acfDefault.load();

                refTable = res.refTable;
                usrTable = res.usrTable;
                effTable = res.effTable;

                return true;
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }

            return false;
        }

        public void initDefault()
        {
            refTable = JKeyValueTable.convertMap( SysUtil.getAllEnv() );
            usrTable = new JKeyValueTable.StateHashMap();
            effTable = null;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void saveSpecific()
        {
            try {
                if( !_acfSpecific.fileIs( JxMakeRootPane.getPath_consoleEnvVarForDefault() ) ) _acfSpecific.save(this);
            }
            catch(final Exception e) {
                JxMakeRootPane.showExceptionDialogWithScrollPane(e);
            }
        }

        public boolean loadSpecific()
        {
            try {
                final SavedEnvVarState res = _acfSpecific.load();

                refTable = res.refTable;
                usrTable = res.usrTable;
                effTable = res.effTable;

                return true;
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }

            return false;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        private static final String _marker_refTable = "[refTable]";
        private static final String _marker_usrTable = "[usrTable]";
        private static final String _marker_effTable = "[effTable]";

        public static void writeObjectData(final Object instance, final OutputStreamWriter osw) throws IOException
        {
            osw.write(Texts.ScrEdt_EVDoNotModify);

            final SavedEnvVarState sevs = (SavedEnvVarState) instance;

            // Save the exclusion list for the reference table
            int exclusionCount = 0;
            for( final Map.Entry<String, JKeyValueTable.State> entry : sevs.refTable.entrySet() ) {
                if( !entry.getValue().checked ) ++exclusionCount;
            }

            osw.write(_marker_refTable + '\n');
            osw.write(exclusionCount + "\n");

            for( final Map.Entry<String, JKeyValueTable.State> entry : sevs.refTable.entrySet() ) {
                if( !entry.getValue().checked ) osw.write( ( entry.getKey() ) + "\n" );
            }

            osw.write("\n");
            osw.flush();

            // Save the user table
            osw.write(_marker_usrTable + '\n');
            osw.write( sevs.usrTable.size() + "\n" );

            for( final Map.Entry<String, JKeyValueTable.State> entry : sevs.usrTable.entrySet() ) {
                osw.write( (                     entry.getKey  ()                     ) + "\n" );
                osw.write( (                     entry.getValue().checked ? '1' : '0' ) + "\n" );
                osw.write( ( XCom.escapeNewLine( entry.getValue().value )             ) + "\n" );
            }

            osw.write("\n");
            osw.flush();

            // Save the effective table
            osw.write(_marker_effTable + '\n');
            osw.write( sevs.effTable.size() + "\n" );

            for( final Map.Entry<String, String> entry : sevs.effTable.entrySet() ) {
                osw.write( (                     entry.getKey  ()   ) + "\n" );
                osw.write( ( XCom.escapeNewLine( entry.getValue() ) ) + "\n" );
            }

            osw.write("\n");
            osw.flush();
        }

        public static Object readObjectData(final InputStreamReader isr) throws IOException
        {
            final BufferedReader   reader = new BufferedReader(isr);
            final SavedEnvVarState sevs   = new SavedEnvVarState(true);

            if( !AppConfigFile.skipMarkerLines(reader, Texts.ScrEdt_EVDoNotModify) ) return sevs;

            // Load and apply the exclusion list for the reference table
            sevs.refTable = JKeyValueTable.convertMap( SysUtil.getAllEnv() );

            final String strRefTable = reader.readLine(); // Dicard the marker
            if( !_marker_refTable.equals(strRefTable) ) return sevs;

            final String strExclusionCount = reader.readLine();
            if(strExclusionCount == null) return sevs;

            final int             exclusionCount = Integer.valueOf(strExclusionCount);
            final HashSet<String> exclusionName  = new HashSet<>();

            for(int i = 0; i < exclusionCount; ++i) {
                final String line = reader.readLine();
                if(line == null) return sevs;
                exclusionName.add(line);
            }

            for( final Map.Entry<String, JKeyValueTable.State> entry : sevs.refTable.entrySet() ) {
                if( exclusionName.contains( entry.getKey() ) ) entry.getValue().checked = false;
            }

            if( reader.readLine() == null ) return sevs; // Dicard the "\n"

            // Load the user table
            sevs.usrTable = new JKeyValueTable.StateHashMap();

            final String strUsrTable = reader.readLine(); // Dicard the marker
            if( !_marker_usrTable.equals(strUsrTable) ) return sevs;

            final String strUsrCount = reader.readLine();
            if(strUsrCount == null) return sevs;

            final int usrCount = Integer.valueOf(strUsrCount);

            for(int i = 0; i < usrCount; ++i) {

                final String strKey = reader.readLine(); if(strKey == null) return sevs;
                final String strChk = reader.readLine(); if(strChk == null) return sevs;
                final String strVal = reader.readLine(); if(strVal == null) return sevs;

                sevs.usrTable.put(
                    strKey,
                    new JKeyValueTable.State( ( Integer.valueOf(strChk) != 0 ), XCom.unescapeNewLine(strVal) )
                );

            } // for

            if( reader.readLine() == null ) return sevs; // Dicard the "\n"

            // Load the effective table
            sevs.effTable = new HashMap<>();

            final String strEffTable = reader.readLine(); // Dicard the marker
            if( !_marker_effTable.equals(strEffTable) ) return sevs;

            final String strEffCount = reader.readLine();
            if(strEffCount == null) return sevs;

            final int effCount = Integer.valueOf(strEffCount);

            for(int i = 0; i < effCount; ++i) {

                final String strKey = reader.readLine(); if(strKey == null) return sevs;
                final String strVal = reader.readLine(); if(strVal == null) return sevs;

                sevs.effTable.put( strKey, XCom.unescapeNewLine(strVal) );

            } // for

            if( reader.readLine() == null ) return sevs; // Dicard the "\n"

            // Done
            return sevs;
        }

    } // class

} // class JxMakeConsole
