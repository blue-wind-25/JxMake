/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp;


import java.awt.Color;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import jxm.*;
import jxm.tool.*;
import jxm.xb.*;


/*
 * ANSI Escape Sequences
 * https://gist.github.com/ConnerWill/d4b6c776b509add763e17f9f113fd25b
 *
 * XTerm Control Sequences
 * https://invisible-island.net/xterm/ctlseqs/ctlseqs.html
 *
 * VT100 Family Programming Summary
 * https://vt100.net/docs/tp83/appendixb.html
 */
public abstract class ANSIScreenBuffer {

    private static final int  DEF_TAB_SIZE = 8;

    private static final int  SSF          =  0xF000 ; // Single-Shift     flag
    private static final char PUA          = '\uE000'; // 96-character-set flag

    private static final Color[] _256ColorLUT = new Color[256];

    static {

        // 0 to 5 : system colors (standard + bright)
        final int[] _baseColors = {
            /*
            * 0 Black       8 Bright Black (Gray)
            * 1 Red         9 Bright Red
            * 2 Green      10 Bright Green
            * 3 Yellow     11 Bright Yellow
            * 4 Blue       12 Bright Blue
            * 5 Magenta    13 Bright Magenta
            * 6 Cyan       14 Bright Cyan
            * 7 White      15 Bright White
            */
            0x000000, 0xB21818, 0x18B218, 0xB26818, 0x1818B2, 0xB218B2, 0x18B2B2, 0xB2B2B2,
            0x686868, 0xFF5454, 0x54FF54, 0xFFFF54, 0x5454FF, 0xFF54FF, 0x54FFFF, 0xFFFFFF
            /*
            0x000000, 0xAF0000, 0x00AF00, 0xAF5F00, 0x0000AF, 0xAF00AF, 0x00AFAF, 0xAFAFAF,
            0x5F5F5F, 0xFF5F5F, 0x5FFF5F, 0xFFFF5F, 0x5F5FFF, 0xFF5FFF, 0x5FFFFF, 0xFFFFFF
            */
        };
        for(int i = 0; i < _baseColors.length; ++i) _256ColorLUT[i] = new Color( _baseColors[i] );

        // 16 to 231 : 6×6×6 color cube
        final int[] _steps = { 0, 95, 135, 175, 215, 255 };
              int   _index = 16;
        for(int r : _steps) {
            for(int g : _steps) {
                for(int b : _steps) _256ColorLUT[_index++] = new Color(r, g, b);
            }
        }

        // 232 to 255 : grayscale ramp
        for(int i = 0; i < 24; ++i) {
            final int level = 8 + i * 10;
            _256ColorLUT[232 + i] = new Color(level, level, level);
        }

    } // static

    private static final HashMap<Long, SimpleAttributeSet> CACHE_SimpleAttributeSet = new HashMap<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static Color DefaultBackgroundColor = _256ColorLUT[0];
    protected static Color DefaultForegroundColor = _256ColorLUT[7];

    protected static char  EmptyChar              = ' '; // '\u00A0' (NBSP)

    @SuppressWarnings("this-escape")
    public static class Char {

        public char    ch; // Java character

        public Color   bg; // Background color
        public Color   fg; // Foreground color

        public boolean b; // Bold
        public boolean i; // Italic
        public boolean u; // Underline
        public boolean s; // Strikethrough

        public boolean k; // Blink

        public Char()
        { reset(); }

        public void set(final char ch_, final Color bg_, final Color fg_, final boolean b_, final boolean i_, final boolean u_, final boolean s_, final boolean k_)
        {
            ch = (ch_ == ' ') ? EmptyChar : ch_;

            bg = bg_;
            fg = fg_;

            b  = b_;
            i  = i_;
            u  = u_;
            s  = s_;

            k  = k_;
        }

        public void setSpace(final Color bg_, final Color fg_)
        { set(' ', bg_, fg_, false, false, false, false, false); }

        public void reset()
        {
            ch = EmptyChar;

            bg = DefaultBackgroundColor;
            fg = DefaultBackgroundColor;

            b  = false;
            i  = false;
            u  = false;
            s  = false;

            k  = false;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public static final Object USER_FLAG_KEY   = new Object();
        public static final int    USER_VALUE_WIDE = 1;

        private long _makeKey(final long userFlag_7b0)
        {
            final long rgbFG = fg.getRGB() & 0x00FFFFFFL;
            final long rgbBF = bg.getRGB() & 0x00FFFFFFL;
            final long flags = (b ? 1L : 0L)
                             | (i ? 2L : 0L)
                             | (u ? 4L : 0L)
                             | (s ? 8L : 0L); // NOTE : Blink ('k') must be handled by the renderer

            return (userFlag_7b0 << 56) | (rgbFG << 32) | (rgbBF << 8) | flags;
        }

        public SimpleAttributeSet attributeSet(final int userFlag_)
        {
            final long userFlag = userFlag_ & 0x000000FFL;

            return CACHE_SimpleAttributeSet.computeIfAbsent( _makeKey(userFlag), k -> {

                final SimpleAttributeSet attrs = new SimpleAttributeSet();

                StyleConstants.setForeground(attrs, fg);
                StyleConstants.setBackground(attrs, bg);

                if(b) StyleConstants.setBold         (attrs, true);
                if(i) StyleConstants.setItalic       (attrs, true);
                if(u) StyleConstants.setUnderline    (attrs, true);
                if(s) StyleConstants.setStrikeThrough(attrs, true);

                if(userFlag != 0) attrs.addAttribute( USER_FLAG_KEY, (int) userFlag );

                return attrs;

            } );
        }

        public SimpleAttributeSet attributeSet()
        { return attributeSet(0); }

    } // class Char

    protected static class LineBuffer {

        public final Char[] cols;

        public LineBuffer(final int numCols)
        {
            cols = new Char[numCols];

            for(int i = 0; i < cols.length; ++i) cols[i] = new Char();
        }

        public void reset()
        { for(int i = 0; i < cols.length; ++i) cols[i].reset(); }

        public void setSpace(final Color bg, final Color fg)
        { for(int i = 0; i < cols.length; ++i) cols[i].setSpace(bg, fg); }

    } // class LineBuffer

    protected static class ScreenBuffer {

        public final int          numScreens;
        public final int          numRows;
        public final int          numCols;

        public final LineBuffer[] rows;
        public final boolean   [] lwrap;
        public final boolean   [] dirty;
        public       boolean      anyDirty;

        public ScreenBuffer(final int numScreens_, final int numRows_, final int numCols_)
        {
            numScreens = numScreens_;
            numRows    = numRows_;
            numCols    = numCols_;

            rows       = new LineBuffer[numScreens * numRows];
            lwrap      = new boolean   [numScreens * numRows];
            dirty      = new boolean   [numScreens * numRows];

            for(int i = 0; i < rows.length; ++i) {
                rows [i] = new LineBuffer(numCols);
                lwrap[i] = false;
                dirty[i] = false;
            }

            anyDirty = false;
        }

        public void reset()
        {
            for(int i = 0; i < rows.length; ++i) {
                rows [i].reset();
                lwrap[i] = false;
                dirty[i] = false;
            }

            anyDirty = false;
        }

    } // class ScreenBuffer

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class State {

        public boolean showCursor;

        public Color   foreground;
        public Color   background;

        public boolean bold;
        public boolean italic;
        public boolean underline;
        public boolean strikethrough;

        public boolean blink;
        public boolean dim;
        public boolean inverse;
        public boolean hidden;

        public State()
        { reset(); }

        public void reset()
        {
            showCursor = !true;

            foreground = DefaultForegroundColor;
            background = DefaultBackgroundColor;

            bold = italic = underline = strikethrough = blink = dim = inverse = hidden = false;
        }

        public void resetForegroundColor()
        { foreground = DefaultForegroundColor; }

        public void resetBackgroundColor()
        { background = DefaultBackgroundColor; }

        public void setFrom(final State ref)
        {
            showCursor    = ref.showCursor;

            foreground    = ref.foreground;
            background    = ref.background;

            bold          = ref.bold;
            italic        = ref.italic;
            underline     = ref.underline;
            strikethrough = ref.strikethrough;

            blink         = ref.blink;
            dim           = ref.dim;
            inverse       = ref.inverse;
            hidden        = ref.hidden;
        }

    } // class State

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected       ScreenBuffer _screenBuffer = null;

    private   final State        _state        = new State();
    private   final State        _stateSave    = new State();

    protected       int          _curRow;     // Current cursor position
    protected       int          _curCol;     // ---

    private         int          _curRowSave; // Saved cursor position
    private         int          _curColSave; // ---

    protected       int          _lastWrittenRow;

    private         int          _tabSize  = DEF_TAB_SIZE;

    private         String       _iconName = "";
    private         String       _winTitle = "";
    private         String       _curTitle = "";
    private         String       _prvTitle = "";

    private         String       _curURI   = null; // ##### ??? TODO : What to do with this ??? #####

    protected void _initialize(final int numScreens, final int numRows, final int numCols)
    {
        _screenBuffer = new ScreenBuffer(numScreens, numRows, numCols);

        _resetAll();
    }

    protected int _screenRows()
    { return (_screenBuffer == null) ? 0 : _screenBuffer.numRows; }

    protected int _screenCols()
    { return (_screenBuffer == null) ? 0 : _screenBuffer.numCols; }

    protected int _totalScreen()
    { return (_screenBuffer == null) ? 0 : _screenBuffer.numScreens; }

    protected int _totalRows()
    { return (_screenBuffer == null) ? 0 : (_screenBuffer.numScreens * _screenBuffer.numRows); }

    protected int _totalCols()
    { return (_screenBuffer == null) ? 0 : _screenBuffer.numCols; }

    protected synchronized void _resetAll()
    {
        CACHE_SimpleAttributeSet.clear();

        _screenBuffer.reset();
        _state.reset();

        _curRow         = 0;
        _curCol         = 0;

        _curRowSave     = 0;
        _curColSave     = 0;

        _lastWrittenRow = -1;

        _tabSize        = DEF_TAB_SIZE;

        _iconName       = "";
        _winTitle       = "";
        _curTitle       = "";
        _prvTitle       = "<ANY>"; // Use a value different from '_curTitle' to ensure '_hasTitleChanged()' returns true

        _curURI         = null;

        _ansiRst();

        Arrays.fill(_ansiCharSelect, 'B');
        _ansiCharSelectGL = 0;
        _ansiCharSelectGR = 1;
    }

    protected synchronized void _scrollBufferOneUp()
    {
        final int endIdx = _totalRows() - 1;

        System.arraycopy(_screenBuffer.rows , 1, _screenBuffer.rows , 0, endIdx);
        System.arraycopy(_screenBuffer.lwrap, 1, _screenBuffer.lwrap, 0, endIdx);
        System.arraycopy(_screenBuffer.dirty, 1, _screenBuffer.dirty, 0, endIdx);

        _screenBuffer.rows [endIdx] = new LineBuffer(_screenBuffer.numCols);
        _screenBuffer.lwrap[endIdx] = false;
        _screenBuffer.dirty[endIdx] = false;

        _screenBuffer.anyDirty      = true;

        _curRow                     = _totalRows() - 1;
    }

    protected synchronized void _scrollBufferOneDown_ifCurRowIsZero()
    {
        if(_curRow != 0) return;

        final int endIdx = _totalRows() - 1;

        System.arraycopy(_screenBuffer.rows , 0, _screenBuffer.rows , 1, endIdx);
        System.arraycopy(_screenBuffer.lwrap, 0, _screenBuffer.lwrap, 1, endIdx);
        System.arraycopy(_screenBuffer.dirty, 0, _screenBuffer.dirty, 1, endIdx);

        _screenBuffer.rows [0     ] = new LineBuffer(_screenBuffer.numCols);
        _screenBuffer.lwrap[0     ] = false;
        _screenBuffer.dirty[0     ] = true;
        _screenBuffer.dirty[endIdx] = true;

        _screenBuffer.anyDirty      = true;

        _lastWrittenRow = Math.min( _lastWrittenRow + 1, _totalRows() - 1 );
    }

    protected boolean cursorIsVisible()
    { return _state.showCursor; }

    protected abstract void _renderBuffer();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final char[]             _ansiCharSelect    = new char[] { 'B', 'B', 'B', 'B' };
    private       int                _ansiCharSelectGL  = 0;
    private       int                _ansiCharSelectGR  = 1;

    private final int[]              _ansiParam         = new int[64];     // Numeric parameters
    private final int[][]            _ansiParamExt      = new int[64][16]; // Extra numeric parameters

    private       boolean            _ansiDCSESCSeen    = false;
    private       String             _ansiDCSPayload    = null;
    private final StringBuilder      _ansiDCSBuff       = new StringBuilder();

    private       boolean            _ansiOSCESCSeen    = false;
    private       String             _ansiOSCPayload    = null;
    private final StringBuilder      _ansiOSCBuff       = new StringBuilder();

    private       boolean            _ansiInSeq         = false;       // Are we inside an escape sequence?
    private       char               _ansiParamMode     =  0;          // Introducer; e.g. '[', ']', '(', ')', '#', etc.
    private       int                _ansiParamIdx      =  0;          // Current parameter index
    private       int                _ansiParamCount    =  0;          // Parameter count
    private       int                _ansiParamExtIdx   = -1;          // Current extra parameter index
    private       int                _ansiParamExtCount =  0;          // Extra parameter count
    private       char               _ansiFinalChar     =  0;          // Final letter (command terminator)

    private       ArrayDeque<String> _ansiReply         = new ArrayDeque<>();

    private int _paramOrDefault(final int idx, final int def)
    { return (_ansiParamCount > idx) ? _ansiParam[idx] : def; }

    private void _ansiBeg()
    {
        _ansiParam[0]      =  0;

        _ansiDCSESCSeen    =  false;
        _ansiDCSPayload    =  null;
        _ansiDCSBuff.setLength(0);

        _ansiOSCESCSeen    =  false;
        _ansiOSCPayload    =  null;
        _ansiOSCBuff.setLength(0);

        _ansiInSeq         =  true;
        _ansiParamMode     =  0;
        _ansiParamIdx      =  0;
        _ansiParamCount    =  0;
        _ansiParamExtIdx   = -1;
        _ansiParamExtCount =  0;
        _ansiFinalChar     =  0;
    }

    private void _ansiEnd(final char ansiFinalChar)
    {
        _ansiDCSESCSeen  =  false;
        _ansiDCSPayload  =  _ansiDCSBuff.toString();
        _ansiDCSBuff.setLength(0);

        _ansiOSCESCSeen  =  false;
        _ansiOSCPayload  =  _ansiOSCBuff.toString();
        _ansiOSCBuff.setLength(0);

        _ansiInSeq       =  false;
        _ansiParamIdx    =  0;
        _ansiParamExtIdx = -1;
        _ansiFinalChar   =  ansiFinalChar;

        /*
        SysUtil.stdDbg().printf("%c %d\n", _ansiFinalChar, _ansiParamCount);
        //*/
    }

    private void _ansiRst()
    {
        _ansiDCSESCSeen    =  false;
        _ansiDCSPayload    =  null;
        _ansiDCSBuff.setLength(0);

        _ansiOSCESCSeen    =  false;
        _ansiOSCPayload    =  null;
        _ansiOSCBuff.setLength(0);

        _ansiInSeq         =  false;
        _ansiParamMode     =  0;
        _ansiParamIdx      =  0;
        _ansiParamCount    =  0;
        _ansiParamExtIdx   = -1;
        _ansiParamExtCount =  0;
        _ansiFinalChar     =  0;
    }

    private boolean _ansiGot()
    { return !_ansiInSeq && _ansiParamMode != 0; }

    private synchronized void _ansiPutReply(final String msg)
    { _ansiReply.addLast(msg); }

    protected synchronized String _ansiGetReply()
    { return _ansiReply.pollFirst(); }

    protected synchronized boolean _ansiHasReply()
    { return !_ansiReply.isEmpty(); }

    private void _processSGR()
    {
        // Convert ESC[m to ESC[0m
        if(_ansiParamCount == 0) {
            _ansiParam[0]   = 0;
            _ansiParamCount = 1;
        }

        int idx = 0;

        while(idx < _ansiParamCount) {

            final int type = _ansiParam[idx++];

                /*
                 * Foreground color
                 *     ESC[3<n>m    <n> ∈ [0-7]
                 *     ESC[9<n>m    <n> ∈ [0-7]
                 */
                if( (type >= 30 && type <= 37) || (type >= 90 && type <= 97) ) {
                    final int offset = (type < 90) ? (type - 30) : (type - 90 + 8);
                    _state.foreground = _256ColorLUT[offset];
                    continue;
                }

                /*
                 * Background color
                 *     ESC[4<n>m     <n> ∈ [0-7]
                 *     ESC[10<n>m    <n> ∈ [0-7]
                 */
                if( (type >= 40 && type <= 47) || (type >= 100 && type <= 107) ) {
                    final int offset = (type < 100) ? (type - 40) : (type - 100 + 8);
                    _state.background = _256ColorLUT[offset];
                    continue;
                }

                switch(type) {

                    /*
                     * 256 color or 24-bit foreground color
                     *     ESC[38;5;<n>m            <n>             ∈ [0-255]
                     *     ESC[38;2;<r>;<g>;<b>m    <r>, <g> , <b>  ∈ [0-255]
                     *
                     *     ESC[38:5:<n>m            <n>             ∈ [0-255]
                     *     ESC[38:2:<r>:<g>:<b>m    <r>, <g> , <b>  ∈ [0-255]
                     */
                    case 38: {
                        if(_ansiParamExtCount > 0) {
                            --idx;
                            final int mode = _ansiParamExt[idx][0];
                                 if(mode == 5) _state.foreground = _256ColorLUT[ _ansiParamExt[idx][1] ];
                            else if(mode == 2) _state.foreground = new Color( _ansiParamExt[idx][1], _ansiParamExt[idx][2], _ansiParamExt[idx][3] );
                            ++idx;
                        }
                        else {
                            final int mode = _ansiParam[idx++];
                                 if(mode == 5) _state.foreground = _256ColorLUT[ _ansiParam[idx++] ];
                            else if(mode == 2) _state.foreground = new Color( _ansiParam[idx++], _ansiParam[idx++], _ansiParam[idx++] );
                        }
                        break;
                    }

                    /*
                     * 256 color or 24-bit background color
                     *     ESC[48;5;<n>m            <n>             ∈ [0-255]
                     *     ESC[48;2;<r>;<g>;<b>m    <r>, <g> , <b>  ∈ [0-255]
                     *
                     *     ESC[48:5:<n>m            <n>             ∈ [0-255]
                     *     ESC[48:2:<r>:<g>:<b>m    <r>, <g> , <b>  ∈ [0-255]
                     */
                    case 48: {
                        if(_ansiParamExtCount > 0) {
                            --idx;
                            final int mode = _ansiParamExt[idx][0];
                                 if(mode == 5) _state.background = _256ColorLUT[ _ansiParamExt[idx][1] ];
                            else if(mode == 2) _state.background = new Color( _ansiParamExt[idx][1], _ansiParamExt[idx][2], _ansiParamExt[idx][3] );
                            ++idx;
                        }
                        else {
                            final int mode = _ansiParam[idx++];
                                 if(mode == 5) _state.background = _256ColorLUT[ _ansiParam[idx++] ];
                            else if(mode == 2) _state.background = new Color( _ansiParam[idx++], _ansiParam[idx++], _ansiParam[idx++] );
                        }
                        break;
                    }

                    /*
                     * Text attributes
                     *     ESC[<n>m    n ∈ 0..5 ∪ 7..9 ∪ 21..25 ∪ 27..29
                     */
                    case  0: _state.reset();                   break;
                    case  1: _state.bold              = true ; break;
                    case  2: _state.dim               = true ; break;
                    case  3: _state.italic            = true ; break;
                  /*case  4: _state.underline         = true ; break;*/    // NOTE : Implemented below
                    case  5: _state.blink             = true ; break;
                    case  7: _state.inverse           = true ; break;
                    case  8: _state.hidden            = true ; break;
                    case  9: _state.strikethrough     = true ; break;
                    case 21: _state.bold              = false; break;
                    case 22: _state.bold = _state.dim = false; break;
                    case 23: _state.italic            = false; break;
                    case 24: _state.underline         = false; break;
                    case 25: _state.blink             = false; break;
                    case 27: _state.inverse           = false; break;
                    case 28: _state.hidden            = false; break;
                    case 29: _state.strikethrough     = false; break;

                    /*
                     * Underline styles
                     *     ESC[4:1m    Single underline
                     *     ESC[4:2m    Double underline
                     *     ESC[4:3m    Curly  underline
                     *     ESC[4:4m    Dotted underline
                     *     ESC[4:5m    Dashed underline
                     */
                    case 4:
                        if(_ansiParamExtCount > 0) {

                            switch(_ansiParamExt[idx - 1][0]) {

                                case 1: _state.underline = true; break;
                                case 2:                          break;    // ##### ??? TODO : Implement (very low priority) ??? #####
                                case 3:                          break;    // ##### ??? TODO : Implement (very low priority) ??? #####
                                case 4:                          break;    // ##### ??? TODO : Implement (very low priority) ??? #####
                                case 5:                          break;    // ##### ??? TODO : Implement (very low priority) ??? #####

                            } // switch

                        }
                        else {
                                        _state.underline = true;
                        }
                        break;

                    /*
                     * Underline color
                     *     ESC[58:5:<n>m            <n>             ∈ [0-255]
                     *     ESC[58:2:<r>:<g>:<b>m    <r>, <g> , <b>  ∈ [0-255]
                     */
                     // ##### ??? TODO : Implement (very low priority) ??? #####

                    /*
                     * Reset colors
                     *     ESC[39m    Reset foreground
                     *     ESC[49m    Reset background
                     *     ESC[59m    Reset underline color    // ##### ??? TODO : Implement (very low priority) ??? #####
                     */
                    case 39: _state.resetForegroundColor(); break;
                    case 49: _state.resetBackgroundColor(); break;

                    /*
                     * Other style extensions
                     *     ESC[51m    Framed
                     *     ESC[52m    Encircled
                     *     ESC[53m    Overlined
                     *     ESC[55m    Reset overline
                     */
                     // ##### ??? TODO : Implement (very low priority) ??? #####

                } // switch

        } // while
    }

    private void _processCSI()
    {
        switch(_ansiFinalChar) {

            /*
             * ESC[?25l    Hide caret
             * ESC[?25h    Show caret
             *
             * ESC[?1l     DEC private mode : application keypad off - arrow keys send simple   sequences like   ESC[A   ESC[B   ESC[C   ESC[D
             * ESC[?1h     DEC private mode : application keypad on  - arrow keys send prefixed sequences like   ESCOA   ESCOB   ESCOC   ESCOD
             */
            case 'l': /* FALLTHROUGH */
            case 'h': {
                if(_ansiParamCount != 1) break;
                final int n = _paramOrDefault(0, 1);
                if(n == 25) {
                    _state.showCursor = (_ansiFinalChar == 'h');
                }
                else if(n == 1) {
                    // ##### ??? TODO : Implement (very low priority) ??? #####
                }
                break;
            }

            /*
             * ESC[nA    Move cursor up n rows (default 1)
             * ESC[nF    Move cursor up n rows (default 1) and to column 1
             */
            case 'A': /* FALLTHROUGH */
            case 'F': {
                final int n = _paramOrDefault(0, 1);
                _curRow = Math.max(_curRow - n, 0);
                if(_ansiFinalChar == 'F') _curCol = 1 - 1;
                break;
            }

            /*
             * ESC[nB    Move cursor down n rows (default 1)
             * ESC[nE    Move cursor down n rows (default 1) and to column 1
             */
            case 'B': /* FALLTHROUGH */
            case 'E': {
                final int n = _paramOrDefault(0, 1);
                _curRow = Math.min( _curRow + n, _totalRows() - 1 );
                if(_ansiFinalChar == 'E') _curCol = 1 - 1;
                break;
            }

            /*
             * ESC[nC    Move cursor forward n columns (default 1)
             */
            case 'C': {
                final int n = _paramOrDefault(0, 1);
                /*
                      int s = 0;
                for(int i = 0; i < n; ++i) {
                     s += UnicodeUtil.stepCountFW(_screenBuffer.rows[_curRow].cols, _curCol + s);
                }
                */
                final int s = UnicodeUtil.stepCountRangeFW(_screenBuffer.rows[_curRow].cols, _curCol, n);
                _curCol = Math.min(_curCol + s, _screenBuffer.numCols - 1);
                break;
            }


            /*
             * ESC[nD    Move cursor backward n columns (default 1)
             */
            case 'D': {
                final int n = _paramOrDefault(0, 1);
                /*
                      int s = 0;
                for(int i = 0; i < n; ++i) {
                     s += UnicodeUtil.stepCountBW(_screenBuffer.rows[_curRow].cols, _curCol - s);
                }
                */
                final int s = UnicodeUtil.stepCountRangeBW(_screenBuffer.rows[_curRow].cols, _curCol, n);
                _curCol = Math.max(_curCol + s, 0);
                break;
            }

            /*
             * ESC[nG    Move cursor to column n (default 1)
             */
            case 'G': {
                final int n = _paramOrDefault(0, 1);
                /*
                      int s = 0;
                for(int i = 0; i < n; ++i) {
                     s += UnicodeUtil.stepCountFW(_screenBuffer.rows[_curRow].cols, s);
                }
                */
                final int s = UnicodeUtil.stepCountRangeFW(_screenBuffer.rows[_curRow].cols, 0, n);
                _curCol = Math.min(s - 1, _screenBuffer.numCols - 1);
                break;
            }

            /*
             * ESC[n;mH    Move cursor to row n (default 1), column m (default 1)
             * ESC[n;mf
             */
            case 'H': /* FALLTHROUGH */
            case 'f': {
                // Get the parameters
                int r = 1;
                int c = 1;
                if(_ansiParamCount > 1) {
                    r = (_ansiParam[0] > 1) ? _ansiParam[0] : 1;
                    c = (_ansiParam[1] > 1) ? _ansiParam[1] : 1;
                }
                else if(_ansiParamCount > 0) {
                    r = (_ansiParam[0] > 1) ? _ansiParam[0] : 1;
                }
                // Handle row
                _curRow = Math.min( r - 1, _totalRows() - 1 );
                // Handle column
                /*
                int s = 0;
                for(int i = 0; i < c; ++i) {
                     s += UnicodeUtil.stepCountFW(_screenBuffer.rows[_curRow].cols, s);
                }
                */
                final int s = UnicodeUtil.stepCountRangeFW(_screenBuffer.rows[_curRow].cols, 0, c);
                _curCol = Math.min(s - 1, _screenBuffer.numCols - 1);
                break;
            }

            /*
             * ESC[s    Save cursor position
             */
            case 's': {
                _curRowSave = _curRow;
                _curColSave = _curCol;
                break;
            }

            /*
             * ESC[u    Restore cursor position
             */
            case 'u': {
                _curRow = _curRowSave;
                _curCol = _curColSave;
                break;
            }

            /*
             * ESC[0n    Query for device status   - terminal replies with ESC[0n
             * ESC[6n    Query for cursor position - terminal replies with ESC[row;colR
             */
            case 'n': {
                     if(_ansiParam[0] == 5) _ansiPutReply("\033[0n"                                          );
                else if(_ansiParam[0] == 6) _ansiPutReply("\033[" + (_curRow + 1) + ';' + (_curCol + 1) + 'R');
                break;
            }

            /*
             * ESC[0J    Clear from cursor to end of screen
             * ESC[1J    Clear from cursor to beginning of screen
             * ESC[2J    Clear entire screen
             * ESC[3J    Clear entire screen + scrollback buffer
             */
            case 'J': {
                final int   type = _ansiParam[0];
                final Color bgc  =     (_state.inverse ? _state.foreground : _state.background            );
                final Color fgc  = _dim(_state.inverse ? _state.background : _state.foreground, _state.dim);

                switch(type) {

                    case 0: /* FALLTHROUGH */
                    case 1: {
                        // Clear
                        final int   begC = (type == 0) ? _curCol               : 0;
                        final int   endC = (type == 0) ? _screenBuffer.numCols : (_curCol + 1);
                        final int   begR = (type == 0) ? (_curRow + 1)         : 0;
                        final int   endR = (type == 0) ? _lastWrittenRow       : _curRow;
                        for(int i = begC; i < endC; ++i) _screenBuffer.rows[_curRow].cols[i].setSpace(bgc, fgc);
                        for(int i = begR; i < endR; ++i) _screenBuffer.rows[i].setSpace(bgc, fgc);
                        // Mark the row(s) as dirty and mark the global dirty flag
                                                         _screenBuffer.dirty[_curRow] = true;
                        for(int i = begR; i < endR; ++i) _screenBuffer.dirty[i      ] = true;
                                                         _screenBuffer.anyDirty       = true;
                        break;
                    }

                    case 2: {
                        // Clear
                        final int scrI =         (       _lastWrittenRow / _screenRows()     ) - 1;
                        final int begR =         (     Math.max(0, scrI) * _screenRows()     );
                        final int endR = Math.min( _lastWrittenRow, begR + _screenRows() * 2 );
                        for(int i = begR; i <= endR; ++i) _screenBuffer.rows[i].setSpace(bgc, fgc);
                        // Mark the row(s) as dirty and mark the global dirty flag
                        for(int i = begR; i <= endR; ++i) _screenBuffer.dirty[i] = true;
                                                          _screenBuffer.anyDirty = true;
                        break;
                    }

                    case 3: {
                        // Clear
                        for(int i = 0; i <= _lastWrittenRow; ++i) _screenBuffer.rows[i].setSpace(bgc, fgc);
                        // Mark the row(s) as dirty and mark the global dirty flag
                        for(int i = 0; i <= _lastWrittenRow; ++i) _screenBuffer.dirty[i] = true;
                                                                  _screenBuffer.anyDirty = true;
                        break;
                    }

                } // switch
                break;
            }

            /*
             * ESC[0K    Clear from cursor to end of line
             * ESC[1K    Clear from cursor to beginning of line
             * ESC[2K    Clear entire line
             */
            case 'K': {
                final int   type = _ansiParam[0];
                final Color bgc  =     (_state.inverse ? _state.foreground : _state.background            );
                final Color fgc  = _dim(_state.inverse ? _state.background : _state.foreground, _state.dim);

                // Clear
                switch(type) {

                    case 0: /* FALLTHROUGH */
                    case 1: {
                        final int begC = (type == 0) ? _curCol               : 0;
                        final int endC = (type == 0) ? _screenBuffer.numCols : (_curCol + 1);
                        for(int i = begC; i < endC; ++i) _screenBuffer.rows[_curRow].cols[i].setSpace(bgc, fgc);
                        break;
                    }

                    case 2: {
                        _screenBuffer.rows[_curRow].setSpace(bgc, fgc);
                        break;
                    }

                } // switch

                // Mark the row as dirty and mark the global dirty flag
                _screenBuffer.dirty[_curRow] = true;
                _screenBuffer.anyDirty       = true;

                break;
            }

        } // switch
    }

    private void _putANSIChar(final char ch)
    {
        // Not currently parsing an escape sequence
        if(!_ansiInSeq) {
            if(ch == 0x1B) { // Check for ESC - mark start of potential sequence
                _flushNormalChar();
                _ansiBeg();
            }
            else {
                _putNormalChar(ch); // Normal character handling
            }
            return;
        }

        // Currently inside an escape sequence
        if(_ansiParamMode == 0) {
            switch(ch) {

                /*
                 * Expect introducer after ESC
                 *     '['                         : SGR or CSI
                 *     ']'                         : OSC
                 *     'P'                         : DCS
                 *     '(' ')' '*' '+' '-' '.' '/' : Charset select
                 */
                case '[': /* FALLTHROUGH */
                case ']': /* FALLTHROUGH */
                case 'P': /* FALLTHROUGH */
                case '(': /* FALLTHROUGH */
                case ')': /* FALLTHROUGH */
                case '*': /* FALLTHROUGH */
                case '+': /* FALLTHROUGH */
                case '-': /* FALLTHROUGH */
                case '.': /* FALLTHROUGH */
                case '/':
                    _ansiParamMode = ch;
                    break;

                // Another ESC - restart the sequence
                case 0x1B:
                    _ansiBeg();
                    break;

                /*
                 * Atomic single‑character escape sequences
                 *     ESC 7   Save cursor & attributes
                 *     ESC 8   Restore cursor & attributes
                 *     ESC c   Reset to Initial State
                 *     ESC M   Reverse Index (cursor up one line, scroll if needed)
                 *
                 *     ESC =   Enter alternate keypad mode                            Equivalent : ESC[?1h (DEC private mode : application keypad on)
                 *     ESC >   Exit alternate keypad mode                             Equivalent : ESC[?1l (DEC private mode : application keypad off)
                 *     ESC D   Index (cursor down one line)                           Equivalent : ESC[B   (cursor down, default = 1 line)
                 *     ESC E   Next Line (move to beginning of next line)             Equivalent : ESC[E   (CSI NEL - Next Line)
                 *     ESC H   Set tab stop at current cursor position                Equivalent : ESC[H   (CSI HTS - Horizontal Tab Set)
                 */
                case '7': /* FALLTHROUGH */
                case '8': /* FALLTHROUGH */
                case 'c': /* FALLTHROUGH */
                case 'M':
                    _ansiParamMode = '#';
                    _ansiEnd(ch);
                    break;

                case '=': /* FALLTHROUGH */
                case '>':
                    _ansiParamMode  = '?';
                    _ansiParamIdx   = 0;
                    _ansiParamCount = 1;
                    _ansiParam[0]   = 1;
                    _ansiEnd( (ch == '=') ? 'h' : 'l' );
                    break;

                case 'D': /* FALLTHROUGH */
                case 'E': /* FALLTHROUGH */
                case 'H':
                    _ansiParamMode  = '[';
                    _ansiParamIdx   = 0;
                    _ansiParamCount = 0;
                    switch(ch) {
                        case 'D': _ansiEnd('B'); break;
                        case 'E': _ansiEnd('E'); break;
                        case 'H': _ansiEnd('H'); break;
                    }
                    break;

                /*
                 * GL single shifts (one‑shot)
                 *     ESC N        Single-Shift G2 into GL → this affects the next one character only
                 *     ESC O        Single-Shift G3 into GL → this affects the next one character only
                 *
                 *     SS2 (0x8E)   Same as ESC N      // NOTE : Implemented in '_putNormalChar_handleControlChar()'
                 *     SS3 (0x8F)   Same as ESC O      // NOTE : Implemented in '_putNormalChar_handleControlChar()'
                 *
                 * GL locking shifts (persistent)
                 *     SI  (0x0F)   Lock G0 into GL    // NOTE : Implemented in '_putNormalChar_handleControlChar()'
                 *     SO  (0x0E)   Lock G1 into GL    // NOTE : Implemented in '_putNormalChar_handleControlChar()'
                 *
                 *     ESC n        Lock G2 into GL
                 *     ESC o        Lock G3 into GL
                 *
                 * GR locking shifts (persistent)
                 *     ESC ~        Lock G1 into GR
                 *     ESC }        Lock G2 into GR
                 *     ESC |        Lock G3 into GR
                 */
                case 'n': _ansiCharSelectGL =  2                                  ; _ansiEnd('\0'); break;
                case 'N': _ansiCharSelectGL = (2 | SSF) | (_ansiCharSelectGL << 8); _ansiEnd('\0'); break;

                case 'o': _ansiCharSelectGL =  3                                  ; _ansiEnd('\0'); break;
                case 'O': _ansiCharSelectGL = (3 | SSF) | (_ansiCharSelectGL << 8); _ansiEnd('\0'); break;

                case '~': _ansiCharSelectGR =  1                                  ; _ansiEnd('\0'); break;
                case '}': _ansiCharSelectGR =  2                                  ; _ansiEnd('\0'); break;
                case '|': _ansiCharSelectGR =  3                                  ; _ansiEnd('\0'); break;

                // Other characters - restart the sequence
                default:
                    _ansiRst();

            } // switch
            return;
        }

        // Inside SGR or CSI sequence
        if(_ansiParamMode == '[' || _ansiParamMode == '?') {
            if(ch >= '0' && ch <= '9') {
                if(_ansiParamExtIdx >= 0) {
                    if(_ansiParamExtCount == 0) _ansiParamExtCount = 1;
                    if(_ansiParamIdx < _ansiParamExt.length && _ansiParamExtIdx < _ansiParamExt[_ansiParamIdx].length) {
                        _ansiParamExt[_ansiParamIdx][_ansiParamExtIdx] = _ansiParamExt[_ansiParamIdx][_ansiParamExtIdx] * 10 + (ch - '0');
                    }
                }
                else {
                    if(_ansiParamCount == 0) _ansiParamCount = 1;
                    if(_ansiParamIdx < _ansiParam.length) {
                        _ansiParam[_ansiParamIdx] = _ansiParam[_ansiParamIdx] * 10 + (ch - '0');
                    }
                }
            }
            else if(ch == '?') {
                _ansiParamMode = '?';
            }
            else if(ch == ';') {
                if(++_ansiParamIdx < _ansiParam.length) {
                    _ansiParam[_ansiParamIdx] = 0;
                    _ansiParamCount = _ansiParamIdx + 1;
                }
                _ansiParamExtIdx = -1;
            }
            else if(ch == ':') {
                if(++_ansiParamExtIdx < _ansiParamExt[_ansiParamIdx].length) {
                    _ansiParamExt[_ansiParamIdx][_ansiParamExtIdx] = 0;
                    _ansiParamExtCount = _ansiParamExtIdx + 1;
                }
            }
            else if(ch == 0x1B) {
                // Another ESC - restart the sequence
                _ansiBeg();
            }
            else {
                // Final letter reached
                _ansiEnd(ch);
            }
            return;
        }

        // Inside OSC sequence
        if(_ansiParamMode == ']') {
            // OSC sequences usually end with BEL (0x07) or ESC \
            if(ch == 0x07) {
                // BEL termination
                _ansiEnd(ch);
            }
            else if(ch == 0x1B) {
                if(_ansiOSCESCSeen) {
                    // Another ESC - restart the sequence
                    _ansiBeg();
                    return;
                }
                _ansiOSCESCSeen = true;
            }
            else if(_ansiOSCESCSeen && ch == '\\') {
                _ansiEnd(ch);
                _ansiOSCESCSeen = false;
            }
            else {
                // Collect OSC payload
                _ansiOSCBuff.append(ch);
                _ansiOSCESCSeen = false;
            }
            return;
        }

        // Inside DCS sequence
        if(_ansiParamMode == 'P') {

            // DCS sequences usually end with ST (0x9C) or ESC \
            if(ch == 0x9C || ch == 0x07) { // Older implementations may use BEL
                // ST termination
                _ansiEnd( (char) 0x9C );
            }
            else if(ch == 0x1B) {
                if(_ansiDCSESCSeen) {
                    // Another ESC - restart the sequence
                    _ansiBeg();
                    return;
                }
                _ansiDCSESCSeen = true;
            }
            else if(_ansiDCSESCSeen && ch == '\\') {
                _ansiEnd(ch);
                _ansiDCSESCSeen = false;
            }
            else {
                // Collect DCS payload
                _ansiDCSBuff.append(ch);
                _ansiDCSESCSeen = false;
            }
            return;
        }

        // Inside charset select
        if( _ansiParamMode == '(' || _ansiParamMode == ')' || _ansiParamMode == '*' || _ansiParamMode == '+' ||
            _ansiParamMode == '-' || _ansiParamMode == '.' || _ansiParamMode == '/'
        ) {
            if(ch == 0x1B) {
                // Another ESC - restart the sequence
                _ansiBeg();
            }
            else {
                // Next char selects charset (e.g. '0' for line drawing)
                _ansiEnd(ch);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final   StringBuffer _sbCapture_stdout = new StringBuffer();
    private final   StringBuffer _sbCapture_stderr = new StringBuffer();
    private boolean              _inCaptureMode    = false;

    protected void startCapture()
    {
        _sbCapture_stdout.setLength(0);
        _sbCapture_stderr.setLength(0);

        _inCaptureMode = true;
    }

    protected String[] finishCapture()
    {
        _inCaptureMode = false;

        return new String[] { _sbCapture_stdout.toString(), _sbCapture_stderr.toString() };
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    private static final HashMap<Character, Character> DEC_MAP_LINE_DRAWING = new HashMap<Character, Character>() {
        {
            put('`', '◆' ); // Diamond
            put('a', '▒' ); // Checkerboard
            put('b', '␉' ); // HT
            put('c', '␌' ); // FF
            put('d', '␍' ); // CR
            put('e', '␊' ); // LF
            put('f', '°' ); // Degree
            put('g', '±' ); // Plus/minus
            put('h', '\n'); // NL
            put('i', '␋' ); // VT
            put('j', '┘' ); // Line drawing
            put('k', '┐' );
            put('l', '┌' );
            put('m', '└' );
            put('n', '┼' );
            put('o', '⎺' );
            put('p', '⎻' );
            put('q', '─' );
            put('r', '⎼' );
            put('s', '⎽' );
            put('t', '├' );
            put('u', '┤' );
            put('v', '┴' );
            put('w', '┬' );
            put('x', '│' );
            put('y', '≤' );
            put('z', '≥' );
            put('{', 'π' );
            put('|', '≠' );
            put('}', '£' );
            put('~', '·' );
        }

        @Override public Character get(final Object key)
        {
            final Character ch = super.get(key);

            return (ch != null) ? ch : ( (Character) key );
        }
    };

    private static final char[] REPLACEMENT_CHAR = new char[] { '�' };

    private static char[] _tch(final char ch, final char gl, final char gr_)
    {
        char gr = gr_;

        if( (gr & PUA) != 0 ) {
            gr = (char) (gr & ~PUA);
            if(gr == '0') {
                if(ch == ' '     ) return new char[] { ch };
                if(ch == '\u007F') return REPLACEMENT_CHAR;
            }
        }

        if( (ch & 0x0080) != 0 ) {
            return (gr == '0') ? new char[] { DEC_MAP_LINE_DRAWING.get( (char) (ch & 0x7F) ) } : UnicodeUtil.composeNFC(ch);
        }
            return (gl == '0') ? new char[] { DEC_MAP_LINE_DRAWING.get(         ch         ) } : UnicodeUtil.composeNFC(ch);
    }

    private static Color _dim(final Color c, boolean dim)
    { return (!dim) ? c : ( new Color( c.getRed() / 2, c.getGreen() / 2, c.getBlue() / 2 ) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _wrapAndAdvanceRow()
    {
        _screenBuffer.lwrap[_curRow] = true;
        ++_curRow;
        _curCol = 0;
    }

    private void _putCluster(final char[] cluster)
    {
        final int remainingCols = _screenBuffer.numCols - _curCol;
        if(cluster.length > remainingCols) {
            if(cluster.length > _screenBuffer.numCols) return; // Discard if it somehow exceeds the number of columns
            _wrapAndAdvanceRow();
        }

        for(final char c : cluster) {
            _putNormalChar_storeNormalChar(c);
            _putNormalChar_adjustRow();
        }
    }

    private void _translateAndPutNormalChar(final char ch, final char gl, final char gr)
    { _putCluster( _tch(ch, gl, gr) ); }

    private void _flushNormalChar()
    { _putCluster( UnicodeUtil.flushNFC() ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final HashSet<Character> ANSI_CONTROL_CHARS = new HashSet<>( Arrays.asList(
        '\n'    ,  // LF
        '\r'    ,  // CR
        '\t'    , // TAB
        '\u000F', // SI
        '\u000E', // SO
        '\u008E', // SS2
        '\u008F'  // SS3
    ) );

    private boolean _putNormalChar_handleControlChar(final char ch)
    {
        if( !ANSI_CONTROL_CHARS.contains(ch) ) return false;

        _flushNormalChar();

        switch(ch) {

            // Handle newline (LF)
            case '\n':
                _screenBuffer.lwrap[_curRow] = false;
                ++_curRow;
                _curCol = 0;
                return true;

            // Handle carriage return (CR)
            case '\r':
                _curCol = 0;
                return true;

            // Handle tab character
            case '\t':
                // Align the column
                _curCol = ( (_curCol / _tabSize) + 1 ) * _tabSize;
                // Increment column and autowrap if it exceeds the width
                if(_curCol >= _screenBuffer.numCols) {
                    _screenBuffer.lwrap[_curRow] = true;
                    ++_curRow;
                    _curCol = 0;
                }
                return true;

            // Handle SI (0x0F) and SO (0x0E)
            case '\u000F':
                _ansiCharSelectGL = 0;
                return true;
            case '\u000E':
                _ansiCharSelectGL = 1;
                return true;

            // Handle SS2 (0x8E) and SS3 (0x8F)
            case '\u008E':
                _ansiCharSelectGL = (2 | SSF) | (_ansiCharSelectGL << 8);
                return true;

            case '\u008F':
                _ansiCharSelectGL = (3 | SSF) | (_ansiCharSelectGL << 8);
                return true;

        } // switch

        // It should never get here (unhandled control character)
        SysUtil.stdErr().printf("IllegalStateException: _putNormalChar_handleControlChar('\\u%04X')\n", ch);
        return false;
    }

    private void _putNormalChar_storeNormalChar(final char ch)
    {
        // Place character into buffer
        if(_state.hidden) {
            _screenBuffer.rows[_curRow].cols[_curCol].set(
                ch,
                _state.background, // Background color
                _state.background, // Foreground color
                false,             // Bold
                false,             // Italic
                false,             // Underline
                false,             // Strikethrough
                false              // Blink
            );
        }
        else {
            _screenBuffer.rows[_curRow].cols[_curCol].set(
                ch,
                    (_state.inverse ? _state.foreground : _state.background            ), // Background color
                _dim(_state.inverse ? _state.background : _state.foreground, _state.dim), // Foreground color
                _state.bold,
                _state.italic,
                _state.underline,
                _state.strikethrough,
                _state.blink
            );
        }

        // Mark the global dirty flag
        _screenBuffer.anyDirty = true;

        // Mark the row as dirty
        _screenBuffer.dirty[_curRow] = true;

        // Increment column and autowrap if it exceeds the width
        if(++_curCol >= _screenBuffer.numCols) _wrapAndAdvanceRow();
    }

    private void _putNormalChar_adjustRow()
    {
        // Scroll if the row index exceeds the maximum number
        if( _curRow >= _totalRows() ) _scrollBufferOneUp();

        // Update the last written row
        _lastWrittenRow = Math.max(_lastWrittenRow, _curRow);
    }

    private void _putNormalChar(final char ch)
    {
        if( _putNormalChar_handleControlChar(ch) ) {
            _putNormalChar_adjustRow();
        }
        else {
            // Get the translation map
            int selGL = _ansiCharSelectGL;
            if( (selGL & SSF) != 0 ) {
                selGL             = selGL & 0x000F;
                _ansiCharSelectGL = (_ansiCharSelectGL >> 8) & 0x000F;
            }
            final char gl = _ansiCharSelect[selGL            ];
            final char gr = _ansiCharSelect[_ansiCharSelectGR];
            // Translate and put the character
            _translateAndPutNormalChar(ch, gl, gr);
        }
    }

    protected synchronized void _putChar(final char ch, final boolean fromStderr)
    {
        if(_inCaptureMode) {
            if(fromStderr) _sbCapture_stderr.append(ch);
            else           _sbCapture_stdout.append(ch);
            return;
        }

        _putANSIChar(ch);

        if( _ansiGot() ) {


            switch(_ansiParamMode) {

                // SGR or CSI
                case '[': /* FALLTHROUGH */
                case '?':
                    if(_ansiFinalChar == 'm') _processSGR();
                    else                      _processCSI();
                    break;

                /*
                 * OSC → ESC ] <command> ; <string> BEL
                 *       ESC ] <command> ; <string> ESC \
                 *
                 * Common OSC commands
                 *        0 ; text                 → Set icon name + window title
                 *        1 ; text                 → Set icon name only
                 *        2 ; text                 → Set window title only
                 *        3 ; prop=value           → Set or delete X property on top-level window  → delete using ESC]3;<prop>ST
                 *        4 ; index ; rgb:RR/GG/BB → Set or query RGB         color palette entry  → for query use ? instead of RGB hex
                 *        5 ; index ; rgb:RR/GG/BB → Set or query RGB special color palette entry  → for query use ? instead of RGB hex
                 *        6 ; rgb:RR/GG/BB         → Set or query RGB         color for background → for query use ? instead of RGB hex
                 *        8 ; ; uri                → Hyperlink                                     → ESC]8;;<uri>ST<text>ESC]8;;ST
                 *       10 ; rgb:RR/GG/BB         → Set default foreground color                  → for query use ? instead of RGB hex
                 *       11 ; rgb:RR/GG/BB         → Set default background color                  → for query use ? instead of RGB hex
                 *       12 ; rgb:RR/GG/BB         → Set default cursor color                      → for query use ? instead of RGB hex
                 *       52 ; c ; data             → clipboard operations (copy text)              → the data must be encoded in Base64
                 *     1337 ; ... ST               → iTerm2 extensions (images, inline files, etc.)
                 *
                 * Common OSC  1337 example payloads
                 *     Inline Images → File=name=<filename>;inline=1;width=auto;height=auto;preserveAspectRatio=1:<base64-data>
                 *     Font change   → SetFont=Monaco-12
                 *     File Transfer → File=name=test.txt;size=123:<base64-data>
                 *
                 * ST is BEL or ESC \
                 */
                case ']':
                    if( (_ansiFinalChar == 0x07 || _ansiFinalChar == '\\') && _ansiOSCPayload != null ) {

                        final int sep = _ansiOSCPayload.indexOf(';');

                        if(sep >= 0) {

                            final String   cmd    = _ansiOSCPayload.substring(0, sep);
                            final String   data   = _ansiOSCPayload.substring(sep + 1);

                            final int      cmdNum = Integer.parseInt(cmd);
                            final String   nfcStr = (cmdNum <= 2) ? UnicodeUtil.composeNFC( data.trim() ) : null;
                            final String[] parts  = (cmdNum >  2) ? data.split(";")                       : null;

                            switch(cmdNum) {

                                case 0 : _iconName = nfcStr; _winTitle = nfcStr; break;
                                case 1 : _iconName = nfcStr;                     break;
                                case 2 :                     _winTitle = nfcStr; break;

                                case 8 : if(parts.length == 2) {
                                            _curURI = parts[1].trim();
                                            if( _curURI.isEmpty() ) _curURI = null;
                                         }
                                         else {
                                             _curURI = null;
                                         }
                                         break;

                                // ##### ??? TODO : Implement others (very low priority) ??? #####

                            } // switch

                            if( _iconName.equals(_winTitle) ) _curTitle = _winTitle;
                            else                              _curTitle = _winTitle + " — " + _iconName;

                        } // if
                    }// if
                    break;

                /*
                 * DCS → ESC P ... ST
                 *       ESC P ... ESC \
                 */
                case 'P':
                    if( (_ansiFinalChar == 0x9C || _ansiFinalChar == '\\') && _ansiDCSPayload != null ) {
                        // ##### ??? TODO : Implement (very very low priority) ??? #####
                    }
                    break;

                /*
                 * Charset select → ESC ( <char>   (G0 94-character-set)
                 *                  ESC ) <char>   (G1 94-character-set)
                 *                  ESC * <char>   (G2 94-character-set)
                 *                  ESC + <char>   (G3 94-character-set)
                 *                  ESC - <char>   (G1 96-character-set)
                 *                  ESC . <char>   (G2 96-character-set)
                 *                  ESC / <char>   (G3 96-character-set)
                 *
                 * Common selectors for 94-character-set
                 *     A → VT100 UK ASCII (currently treated the same as B below)    // ##### ??? TODO : Implement it properly (very low priority) ??? #####
                 *     B → VT100 US ASCII
                 *     0 → VT100 DEC special character and line drawing set, e.g:
                 *             q → horizontal line        ─
                 *             x → vertical line          │
                 *             m → bottom left corner     └
                 *             j → bottom right corner    ┘
                 *             k → top right corner       ┐
                 *             l → top left corner        ┌
                 *
                 * Common selectors for 96-character-set
                 *     Currently treated in a very similar way to the 94-character-set    // ##### ??? TODO : Implement it properly (very low priority) ??? #####
                 *
                 * Usage examples   // NOTE : Implemented in '_putANSIChar()' and '_putNormalChar_handleControlChar()'
                 *     SI (Shift In , 0x0F) → select G0
                 *     SO (Shift Out, 0x0E) → select G1
                 *
                 *     ESC n                → select G2 until changed
                 *     ESC N                → select G2 for the next character only
                 *
                 *     ESC o                → select G3 until changed
                 *     ESC O                → select G3 for the next character only
                 */
                case '(': /* FALLTHROUGH */
                case ')': /* FALLTHROUGH */
                case '*': /* FALLTHROUGH */
                case '+': /* FALLTHROUGH */
                case '-': /* FALLTHROUGH */
                case '.': /* FALLTHROUGH */
                case '/':
                    if(_ansiFinalChar == 'A' || _ansiFinalChar == 'B' || _ansiFinalChar == '0') {
                        if(_ansiParamMode == '(') _ansiCharSelect[0] =        (      _ansiFinalChar); // 94-character-set
                        if(_ansiParamMode == ')') _ansiCharSelect[1] =        (      _ansiFinalChar); // 94-character-set
                        if(_ansiParamMode == '*') _ansiCharSelect[2] =        (      _ansiFinalChar); // 94-character-set
                        if(_ansiParamMode == '+') _ansiCharSelect[3] =        (      _ansiFinalChar); // 94-character-set
                        if(_ansiParamMode == '-') _ansiCharSelect[1] = (char) (PUA | _ansiFinalChar); // 96-character-set
                        if(_ansiParamMode == '.') _ansiCharSelect[2] = (char) (PUA | _ansiFinalChar); // 96-character-set
                        if(_ansiParamMode == '/') _ansiCharSelect[3] = (char) (PUA | _ansiFinalChar); // 96-character-set
                    }
                    break;

                /*
                 * Atomic single‑character escape sequences
                 *     ESC 7   Save cursor & attributes
                 *     ESC 8   Restore cursor & attributes
                 *     ESC c   Reset to Initial State
                 *     ESC M   Reverse Index (cursor up one line, scroll if needed)
                 */
                case '#':
                    switch(_ansiFinalChar) {

                        case '7': {
                            _curRowSave = _curRow;
                            _curColSave = _curCol;
                            _stateSave.setFrom(_state);
                            break;
                        }

                        case '8': {
                            _curRow = _curRowSave;
                            _curCol = _curColSave;
                            _state.setFrom(_stateSave);
                            break;
                        }

                        case 'c': {
                            _resetAll();
                            break;
                        }

                        case 'M': {
                            if(_curRow == 0) _scrollBufferOneDown_ifCurRowIsZero();
                            else             _curRow = Math.max(_curRow - 1, 0);
                        }

                    } // switch
                    break;

                // Invalid
                default:
                    break;

            } // switch

            _ansiRst();

        } // if
    }

    protected void _putChars(final char[] chars, final boolean fromStderr)
    { for(char ch : chars) _putChar(ch, fromStderr); }

    protected void _putString(final String str, final boolean fromStderr)
    { _putChars( str.toCharArray(), fromStderr ); }

    protected void _putChar(final char ch)
    { _putChar(ch, false); }

    protected void _putChars(final char[] chars)
    { _putChars(chars, false); }

    protected void _putString(final String str)
    { _putString(str, false); }

    protected void _setAllDirty()
    {
        for(int i = 0; i < _lastWrittenRow; ++i) _screenBuffer.dirty[i] = true;
        _screenBuffer.anyDirty = true;
    }

    protected void _setTabSize(final int tabSize)
    { _tabSize = tabSize; }

    protected void _setDefaultTabSize()
    { _tabSize = DEF_TAB_SIZE; }

    protected boolean _hasTitleChanged()
    { return !_prvTitle.equals(_curTitle); }

    protected String _getTitle()
    {
        _prvTitle = _curTitle;

        return _curTitle;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static class ProcessHandle {

        public  final ProcessWrapperBase pwb;
        public  final Object             process;

        public  final OutputStreamWriter swStdIn;
        public  final InputStreamReader  srStdOut;
        public  final InputStreamReader  srStdErr;

        public  final BufferedWriter     bwStdIn;
        public  final BufferedReader     brStdOut;
        public  final BufferedReader     brStdErr;

        public  final Thread             threadStdIn;
        public  final Thread             threadStdOut;
        public  final Thread             threadStdErr;

        private       boolean            _stdinClosed;

        public ProcessHandle(
            final ProcessWrapperBase pwb,
            final Object             process,
            final OutputStreamWriter swStdIn,
            final InputStreamReader  srStdOut,
            final InputStreamReader  srStdErr,
            final BufferedWriter     bwStdIn,
            final BufferedReader     brStdOut,
            final BufferedReader     brStdErr,
            final Thread             threadStdIn,
            final Thread             threadStdOut,
            final Thread             threadStdErr
        )
        {
            this.pwb          = pwb;
            this.process      = process;

            this.swStdIn      = swStdIn;
            this.srStdOut     = srStdOut;
            this.srStdErr     = srStdErr;

            this.bwStdIn      = bwStdIn;
            this.brStdOut     = brStdOut;
            this.brStdErr     = brStdErr;

            this.threadStdIn  = threadStdIn;
            this.threadStdOut = threadStdOut;
            this.threadStdErr = threadStdErr;

            this._stdinClosed = false;
        }

        public boolean isBuffered()
        { return bwStdIn != null && brStdOut != null && brStdErr != null; }

        public void start()
        {
            threadStdIn .start();
            threadStdOut.start();
            threadStdErr.start();
        }

        public boolean isActive() throws IOException
        {
            if( brStdOut != null && brStdOut.ready() ) return true;
            if( srStdOut != null && srStdOut.ready() ) return true;

            if( brStdErr != null && brStdErr.ready() ) return true;
            if( srStdErr != null && srStdErr.ready() ) return true;
            if( pwb.isAlive(process)                 ) return true;

            return false;
        }

        public void interrupt()
        { pwb.destroy(process); }

        public void kill()
        { pwb.destroyForcibly(process); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public void putStdIn(final char chr) throws IOException
        {
            if(_stdinClosed) return;

            if(bwStdIn != null) {
                bwStdIn.write(chr);
                bwStdIn.flush();
            }
            else {
                swStdIn.write(chr);
                swStdIn.flush();
            }
        }

        public void putStdIn(final String string) throws IOException
        {
            if(_stdinClosed) return;

            if(bwStdIn != null) {
                bwStdIn.write(string);
                bwStdIn.flush();
            }
            else {
                swStdIn.write(string);
                swStdIn.flush();
            }
        }

        public void closeStdIn()
        {
            if(_stdinClosed) return;

            try {
                if(bwStdIn != null) bwStdIn.close();
                else                swStdIn.close();
            }
            catch(final IOException e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }

            _stdinClosed = true;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public int waitFor() throws Exception
        {
            // Timeout
            final int PROCESS_WAIT_TIMEOUT_MS = 10000;
            final int KILL_WAIT_TIMEOUT_MS    =  3000;
            final int THREAD_JOIN_TIMEOUT_MS  =  2000;

            // Wait the process
            int exitCode = -1;

            if( pwb.waitFor(process, PROCESS_WAIT_TIMEOUT_MS) ) {
                exitCode = pwb.exitValue(process);
            }
            else {
                pwb.destroy(process);
                if( pwb.waitFor(process, KILL_WAIT_TIMEOUT_MS) ) {
                    exitCode = pwb.exitValue(process);
                }
                else {
                    pwb.destroyForcibly(process);
                    if( pwb.waitFor(process, KILL_WAIT_TIMEOUT_MS) ) {
                        exitCode = pwb.exitValue(process);
                    }
                }
            }

            // Wait the stdin thread
            try {
                threadStdIn.join(THREAD_JOIN_TIMEOUT_MS);
            }
            catch(final InterruptedException e) {
                // Restore state
                Thread.currentThread().interrupt();
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }

            // Wait the stdout thread
            try {
                threadStdOut.join(THREAD_JOIN_TIMEOUT_MS);
            }
            catch(final InterruptedException e) {
                // Restore state
                Thread.currentThread().interrupt();
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }

            // Wait the stderr thread
            try {
                threadStdErr.join(THREAD_JOIN_TIMEOUT_MS);
            }
            catch(final InterruptedException e) {
                // Restore state
                Thread.currentThread().interrupt();
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }

            // Close stdout
            try {
                if(brStdOut != null) brStdOut.close();
                else                 srStdOut.close();
            }
            catch(final IOException e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }

            // Close stderr
            try {
                if(brStdErr != null) brStdErr.close();
                else                 srStdErr.close();
            }
            catch(final IOException e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }

            // Return the exit code
            return exitCode;
        }

        public int waitFor(final int delayBeforeClosingStdin_MS) throws Exception
        {
            // Close stdin
            SysUtil.sleepMS(delayBeforeClosingStdin_MS);

            closeStdIn();

            // Wait the process
            return waitFor();
        }

    } // class ProcessHandle

    protected ProcessHandle _putProcess(final ProcessWrapperBase pwb, final boolean buffered) throws Exception
    {
        final int                BuffSize = 10 * 1024 * 1024;

        final Object             process  = pwb.start();

        final OutputStreamWriter swStdIn  = pwb.getOutputStreamWriter(process);
        final InputStreamReader  srStdOut = pwb.getInputStreamReader (process);
        final InputStreamReader  srStdErr = pwb.getErrorStreamReader (process);

        final BufferedWriter     bwStdIn  = buffered ? new BufferedWriter(swStdIn , BuffSize) : null;
        final BufferedReader     brStdOut = buffered ? new BufferedReader(srStdOut, BuffSize) : null;
        final BufferedReader     brStdErr = buffered ? new BufferedReader(srStdErr, BuffSize) : null;

        final Thread threadStdIn = new Thread( () -> {
            try {
                do {

                    final String msg = _ansiGetReply();

                    if(msg != null) {

                        if(buffered) {
                            bwStdIn.write(msg);
                            bwStdIn.flush();
                        }
                        else {
                            swStdIn.write(msg);
                            swStdIn.flush();
                        }

                    }

                } while( pwb.isAlive(process) );
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }
        } );

        final Thread threadStdOut = new Thread( () -> {
            try {
                if(buffered) {
                    do {
                        final String line = brStdOut.readLine();
                        if(line == null) break;
                        /*
                        SysUtil.stdOut().println("[STDOUT] " + line);
                        //*/
                        _putString(line, false);
                        _putChar('\n', false);

                    } while( pwb.isAlive(process) || brStdOut.ready() );
                }
                else {
                    do {
                        final int ch = srStdOut.read();
                        if(ch == -1) break;
                        /*
                        SysUtil.stdOut().print( (char) ch );
                        //*/
                        _putChar( (char) ch, false );
                    } while( pwb.isAlive(process) || srStdOut.ready() );

                }
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }
        } );

        final Thread threadStdErr = new Thread( () -> {
            try {
                if(buffered) {
                    do {
                        final String line = brStdErr.readLine();
                        if(line == null) break;
                        /*
                        SysUtil.stdOut().println("[STDERR] " + line);
                        //*/
                        _putString(line, true);
                        _putChar('\n', true);

                    } while( pwb.isAlive(process) || brStdErr.ready() );
                }
                else {
                    do {
                        final int ch = srStdErr.read();
                        if(ch == -1) break;
                        /*
                        SysUtil.stdOut().print( (char) ch );
                        //*/
                        _putChar( (char) ch, true );
                    } while( pwb.isAlive(process) || srStdErr.ready() );
                }
            }
            catch(final Exception e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            }
        } );

        return new ProcessHandle(pwb, process, swStdIn, srStdOut, srStdErr, bwStdIn, brStdOut, brStdErr, threadStdIn, threadStdOut, threadStdErr);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String ASeq_ClearScreenAndScrollback = "\033[3;J\033[H"; // "\033[3;J\033[H\\x1B[2J"
    public static final String ASeq_ResetToInitialState      = "\033c";

    public static final String ASeq_Attr_SetRed              = "\033[31;40m"; // NOTE : Ensure the background matches 'DefaultBackgroundColor'
    public static final String ASeq_Attr_SetGreen            = "\033[32;40m"; // ---
    public static final String ASeq_Attr_SetYellow           = "\033[33;40m"; // ---
    public static final String ASeq_Attr_SetBlue             = "\033[34;40m"; // ---
    public static final String ASeq_Attr_SetMagenta          = "\033[35;40m"; // ---
    public static final String ASeq_Attr_SetCyan             = "\033[36;40m"; // ---
    public static final String ASeq_Attr_SetWhite            = "\033[37;40m"; // ---

    public static final String ASeq_Attr_SetBrightRed        = "\033[91;40m"; // NOTE : Ensure the background matches 'DefaultBackgroundColor'
    public static final String ASeq_Attr_SetBrightGreen      = "\033[92;40m"; // ---
    public static final String ASeq_Attr_SetBrightYellow     = "\033[93;40m"; // ---
    public static final String ASeq_Attr_SetBrightBlue       = "\033[94;40m"; // ---
    public static final String ASeq_Attr_SetBrightMagenta    = "\033[95;40m"; // ---
    public static final String ASeq_Attr_SetBrightCyan       = "\033[96;40m"; // ---
    public static final String ASeq_Attr_SetBrightWhite      = "\033[97;40m"; // ---

    public static final String ASeq_Attr_SetBold             = "\033[1m";
    public static final String ASeq_Attr_RstBold             = "\033[21m";

    public static final String ASeq_Attr_SetItalic           = "\033[3m";
    public static final String ASeq_Attr_RstItalic           = "\033[23m";

    public static final String ASeq_Attr_SetBlink            = "\033[5m";
    public static final String ASeq_Attr_RstBlink            = "\033[25m";

    public static final String ASeq_Attr_RstAll              = "\033[0m";

} // class ANSIScreenBuffer
