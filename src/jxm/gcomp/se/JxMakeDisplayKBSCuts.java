/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp.se;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.net.URL;

import java.util.HashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import jxm.*;
import jxm.gcomp.*;
import jxm.xb.*;


@SuppressWarnings("serial")
public class JxMakeDisplayKBSCuts extends SwingApp {

    // GUI-related variables
    private JScrollPane _scpDocView = null;
    private JTextPane   _txtDocView = null;
    private JButton     _btnClose   = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public JxMakeDisplayKBSCuts(final boolean useDarkColorTheme)
    { super(useDarkColorTheme, null); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    private static final HashMap<String, String> _tagMap = new HashMap<String, String>() {{
        put(
            "Caption",
                "<tr><td colspan='8' align='center'><h5><b>%s</b></h5></td></tr>"
        );

        put(
            "MacOS",
                "<tr>\n"
          + "        <td><b>MacOS</b></td><td></td>"
        );

        put(
            "Others",
                "</tr><tr>\n"
          + "        <td><b>Others</b></td><td></td>"
            );

        put(
            "Separator-TR",
                "</tr>\n"
          + "    <tr><td colspan='8'>&nbsp;</td></tr>"
        );

        put(
            "Separator-SA",
                "<tr><td colspan='8'>&nbsp;</td></tr>"
        );

        put(
            "Img-None",
                    "<td>                             </td><td>                            </td><td>                              </td>"
        );

        put(
            "Img-Shift",
                    "<td>                             </td><td>                            </td><td><img src='../image/shift.png'></td>"
        );

        put(
            "Img-Ctrl",
                    "<td><img src='../image/ctrl.png'></td><td>                            </td><td>                              </td>"
        );
        put(
            "Img-Ctrl-Alt",
                    "<td><img src='../image/ctrl.png'></td><td><img src='../image/alt.png'></td><td>                              </td>"
        );
        put(
            "Img-Ctrl-Shift",
                    "<td><img src='../image/ctrl.png'></td><td>                            </td><td><img src='../image/shift.png'></td>"
        );
        put(
            "Img-Ctrl-Alt-Shift",
                    "<td><img src='../image/ctrl.png'></td><td><img src='../image/alt.png'></td><td><img src='../image/shift.png'></td>"
        );
        put(
            "Img-Alt",
                    "<td>                             </td><td><img src='../image/alt.png'></td><td>                              </td>"
        );
        put(
            "Img-Alt-Shift",
                    "<td>                             </td><td><img src='../image/alt.png'></td><td><img src='../image/shift.png'></td>"
        );

        put(
            "Img-Meta",
                    "<td><img src='../image/meta.png'></td><td>                            </td><td>                              </td>"
        );
        put(
            "Img-Meta-Opt",
                    "<td><img src='../image/meta.png'></td><td><img src='../image/opt.png'></td><td>                              </td>"
        );
        put(
            "Img-Meta-Shift",
                    "<td><img src='../image/meta.png'></td><td>                            </td><td><img src='../image/shift.png'></td>"
        );
        put(
            "Img-Meta-Opt-Shift",
                    "<td><img src='../image/meta.png'></td><td><img src='../image/opt.png'></td><td><img src='../image/shift.png'></td>"
        );
        put(
            "Img-Opt",
                    "<td>                             </td><td><img src='../image/opt.png'></td><td>                              </td>"
        );
        put(
            "Img-Opt-Shift",
                    "<td>                             </td><td><img src='../image/opt.png'></td><td><img src='../image/shift.png'></td>"
        );

        put(
            "Key",
                    "<td rowspan='2'>\u00A0%-53s</td><td rowspan='2'>                  </td>"
        );

        put(
            "Dsc",
                    "<td rowspan='2'>\u00A0%-92s</td>"
        );
    }};

    // {{block:Type(:Value)?}}
    private static final Pattern _reBlock = Pattern.compile("\\{\\{block:([A-Za-z0-9_-]+)(?::([^}]+))?\\}\\}");

    @SuppressWarnings("serial")
    @Override
    protected void _initializeAll() throws Exception
    {
        // Create the GUI - main window
        _initializeMainWindow(Texts.ScrEdt_KSCTitle);

        // Create the GUI - document view
        final URL    htmlURL = SysUtil.getTextResourceURL_defDocHTML("Script-Editor-Keyboard-Shortcuts.html");
              String htmlDoc = null;

        try {
            // Load the document
            htmlDoc = SysUtil.loadTextResource(htmlURL);
            // Perform block replacement
            final Matcher      matcher = _reBlock.matcher(htmlDoc);
            final StringBuffer result  = new StringBuffer();
            while( matcher.find() ) {
                final String type        = matcher.group(1);
                final String value       = matcher.group(2);
                      String replacement = "";
                if( _tagMap.containsKey(type) ) {
                    if(value != null) replacement = String.format( _tagMap.get(type), value );
                    else              replacement =                _tagMap.get(type);
                }
                matcher.appendReplacement( result, Matcher.quoteReplacement(replacement) );
            }
            matcher.appendTail(result);
            htmlDoc = result.toString();
            /*
            SysUtil.stdDbg().println(htmlDoc);
            //*/
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Show the error message
            htmlDoc = SysUtil.htmlStringFromStackTrace(e);
        }

        final Font baseFont = JxMakeTheme.getMonoFont();
        final Font kscbFont = baseFont.deriveFont( baseFont.getSize() + 3.0f );

        _txtDocView = new JTextPane();

            _txtDocView.setBorder( new CompoundBorder( _txtDocView.getBorder(), new EmptyBorder(5, 5, 5, 5) ) );
          //_txtDocView.setCaret(null);
            _txtDocView.setHighlighter(null);
            _txtDocView.setEditable(false);

            _txtDocView.setFont(kscbFont);

        if(_useDCT) {
            _txtDocView.setBackground       ( rgbInvertOrDarken  ( _txtDocView.getBackground       () ) );
            _txtDocView.setForeground       ( rgbInvertOrBrighten( _txtDocView.getForeground       () ) );
            _txtDocView.setCaretColor       ( rgbInvertOrBrighten( _txtDocView.getCaretColor       () ) );
            _txtDocView.setSelectionColor   ( rgbInvertOrBrighten( _txtDocView.getSelectionColor   () ) );
            _txtDocView.setSelectedTextColor( rgbInvertOrDarken  ( _txtDocView.getSelectedTextColor() ) );
        }

            final Color bg = _txtDocView.getBackground();
            final Color fg = _txtDocView.getForeground();

            _txtDocView.setEditorKit( new HTMLEditorKit() {
                private final StyleSheet _ss = new StyleSheet() {{
                    addRule(
                        "body { " +
                            "font-family: '"         + kscbFont.getFamily() + "';"  +
                            "font-size: "            + kscbFont.getSize()   + "pt;" +
                            "color: rgb("            + fg.getRed() + ","  + fg.getGreen() + "," + fg.getBlue() + ");" +
                            "background-color: rgb(" + bg.getRed() + ","  + bg.getGreen() + "," + bg.getBlue() + "); }"
                    );
                }};
                @Override
                public StyleSheet getStyleSheet()
                { return _ss; }
            } );

            _txtDocView.setContentType("text/html");
            ( (HTMLDocument) _txtDocView.getDocument() ).setBase(htmlURL);

            _txtDocView.setText(htmlDoc);
            _txtDocView.setCaretPosition(0);

        final JScrollPane scpDocView = new JScrollPane(_txtDocView);
        scpDocView.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scpDocView.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        final JPanel pnlDocView = new JPanel();
        pnlDocView.setLayout( new BorderLayout() );
        pnlDocView.setBorder( new EmptyBorder(5, 0, 5, 0) );

        pnlDocView.add(scpDocView, BorderLayout.CENTER);

        _mainWindow.add(pnlDocView, BorderLayout.CENTER);

        // Create the GUI - close button
        _btnClose = new JButton(Texts.ScrEdt_KSCBtnClose);
        _btnClose.setBorder( new CompoundBorder( _btnClose.getBorder(), new EmptyBorder(2, 2, 2, 2) ) );

        _mainWindow.add(_btnClose, BorderLayout.SOUTH);

        // Add event listeners to the close button
        _btnClose.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ignored) {
                _mainWindow.dispose();
            }
        } );

        // Show the main window
        _showMainWindow(_txtDocView, false);
    }

} // class JxMakeDisplayKBSCuts
