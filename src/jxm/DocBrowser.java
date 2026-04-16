/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jxm.gcomp.*;
import jxm.xb.*;


public class DocBrowser extends SwingApp {

    // List of documentation files
    private List<String>  _docFiles   = null;

    // GUI-related variables
    private JList<String> _lstFiles   = null;
    private JScrollPane   _scpDocView = null;
    private JTextArea     _txtDocView = null;
    private JButton       _btnClose   = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public DocBrowser(final boolean useDarkColorTheme)
    { super(useDarkColorTheme, null); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // List file
    private static List<String> __lsfile(final String path, final Pattern pmMatchExt)
    {
        try {
            final List<String> lst = SysUtil.cu_lsfile(path, pmMatchExt);
            final List<String> res = new ArrayList<String>();
            for(final String s : lst) res.add( SysUtil.resolvePath(s, path) );
            return res;
        }
        catch(final Exception e) {
            return null;
        }
    }

    // Read file
    private static String __readFile(final String path) throws IOException
    {
        final BufferedReader bfr = new BufferedReader( new InputStreamReader(
                                       path.startsWith("jar:") ? JxMake.class.getResourceAsStream( path.substring(4) ) : Files.newInputStream( Paths.get(path) ),
                                       SysUtil._CharEncoding
                                   ) );

        final StringBuilder sb = new StringBuilder();

        while(true) {
            final String line = bfr.readLine();
            if(line == null) break;
            sb.append(line);
            sb.append("\n");
        }

        return sb.toString();
    }

    // Change displayed text
    private void __changeDisplayedText()
    {
        String text = null;

        try {
            text = __readFile( _docFiles.get( _lstFiles.getSelectedIndex() ) );
        }
        catch(final Exception e) {
            text = e.toString();
        }

        _txtDocView.setText(text);
        _txtDocView.setCaretPosition(0);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void _initializeAll() throws Exception
    {
        // Get the documentation files
        _docFiles = SysUtil.getJARResFilePaths( SysUtil.get_JxMakeJARResDocTXTRoot() );

        if(_docFiles != null) {
            final List<String> res = new ArrayList<String>();
            for(final String s : _docFiles) {
                if( s.endsWith(".txt") ) res.add("jar:" + s);
            }
            _docFiles = res;
        }
        else {
            final String[] dirs = SysUtil.get_JxMakeDocTXTPossibleDirs();
            final Pattern  ext   = Pattern.compile("txt");
            for(final String dir : dirs) {
                _docFiles = __lsfile(dir, ext);
                if( _docFiles != null && !_docFiles.isEmpty() ) break;
            }
        }
        if( _docFiles != null && _docFiles.isEmpty() ) _docFiles = null;

        if(_docFiles == null) throw XCom.newJXMFatalLogicError(Texts.DocBro_ErrNoDoc);

        Collections.sort(_docFiles);

        // Create the GUI - main window
        _initializeMainWindow(Texts.DocBro_Title);

        // Create the GUI - list of documentation files
        final DefaultListModel<String> dlm = new DefaultListModel<>();
        for(final String s : _docFiles) {
            dlm.addElement(
                s.substring( s.lastIndexOf("/") + 1 ).replaceAll("(\\d+)-(.+)"      , "[$1] $2")
                                                     .replaceAll("-"                , " "      )
                                                     .replaceAll("(Appendix [A-Z])_", "$1 - "  )
                                                     .replaceAll("\\.txt$"          , ""       )
            );
        }

        _lstFiles = new JList<String>(dlm);
        _lstFiles.setFont( new Font(Font.MONOSPACED, Font.PLAIN, 12) );
        _lstFiles.setVisibleRowCount(7);
        _lstFiles.setSelectedIndex(0);

        if(_useDCT) {
            _lstFiles.setBackground         ( rgbInvertOrDarken   ( _lstFiles.getBackground         () ) );
            _lstFiles.setForeground         ( rgbInvertOrBrighten ( _lstFiles.getForeground         () ) );
            _lstFiles.setSelectionBackground( rgbInvertOrBrighten ( _lstFiles.getSelectionBackground() ) );
            _lstFiles.setSelectionForeground( rgbInvertOrBrighten ( _lstFiles.getSelectionForeground() ) );
        }

        final JScrollPane scpFiles = new JScrollPane(_lstFiles);
        scpFiles.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scpFiles.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        _mainWindow.add(scpFiles, BorderLayout.NORTH);

        // ##### ??? TODO : Add buttons for copy, print, etc. ??? #####

        // Create the GUI - document view
        final XCom.Pair<String, Integer> fnz = SysUtil.getTextAreaMonospacedFontSpec(
                                                   "JXMAKE_DBROWSER_FONT_NAME",
                                                   "JXMAKE_DBROWSER_FONT_SIZE"
                                               );

        final String fontName = fnz.first ();
        final int    fontSize = fnz.second();

        _txtDocView = new JTextArea(0, 0);
        _txtDocView.setEditable(false);
        _txtDocView.setLineWrap(false);
        _txtDocView.setFont( new Font(fontName, Font.PLAIN, fontSize) );

        if(_useDCT) {
            _txtDocView.setBackground       ( rgbInvertOrDarken  ( _txtDocView.getBackground       () ) );
            _txtDocView.setForeground       ( rgbInvertOrBrighten( _txtDocView.getForeground       () ) );
            _txtDocView.setCaretColor       ( rgbInvertOrBrighten( _txtDocView.getCaretColor       () ) );
            _txtDocView.setSelectionColor   ( rgbInvertOrBrighten( _txtDocView.getSelectionColor   () ) );
            _txtDocView.setSelectedTextColor( rgbInvertOrDarken  ( _txtDocView.getSelectedTextColor() ) );
        }

        /*
        SysUtil.stdDbg().println( _txtDocView.getFont().getFontName() );
        //*/

        _txtDocView.setBorder( new CompoundBorder( _txtDocView.getBorder(), new EmptyBorder(5, 5, 5, 5) ) );

        final JScrollPane scpDocView = new JScrollPane(_txtDocView);
        scpDocView.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scpDocView.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        final JPanel pnlDocView = new JPanel();
        pnlDocView.setLayout( new BorderLayout() );
        pnlDocView.setBorder( new EmptyBorder(5, 0, 5, 0) );

        pnlDocView.add(scpDocView, BorderLayout.CENTER);

        _mainWindow.add(pnlDocView, BorderLayout.CENTER);

        // Create the GUI - close button
        _btnClose = new JButton(Texts.DocBro_BtnClose);
        _btnClose.setBorder( new CompoundBorder( _btnClose.getBorder(), new EmptyBorder(2, 2, 2, 2) ) );

        _mainWindow.add(_btnClose, BorderLayout.SOUTH);

        // Add event listener to the list box
        _lstFiles.addListSelectionListener( new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent ignored)
            { __changeDisplayedText(); }
        } );

        // Add event listeners to the close button
        _btnClose.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ignored)
            { _mainWindow.dispose(); }
        } );

        // Show the main window, set focus to the input list box, and change the displayed text
        _showMainWindow(_lstFiles);
        __changeDisplayedText();
    }

} // class DocBrowser
