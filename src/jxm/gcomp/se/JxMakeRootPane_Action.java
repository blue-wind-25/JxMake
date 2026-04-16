/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp.se;


import java.util.concurrent.atomic.AtomicInteger;

import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.fife.ui.rtextarea.RecordableTextAction;
import org.fife.ui.rtextarea.RTextArea;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import jxm.*;
import jxm.annotation.*;


@SuppressWarnings("serial")
public class JxMakeRootPane_Action {

    public static final String Action_rehighlightAll   = "JxMake.rehighlightAll";
    public static final String Action_toggleLineWrap   = "JxMake.toggleLineWrap";

    public static final String Action_increaseFontSize = "JxMake.increaseFontSize";
    public static final String Action_decreaseFontSize = "JxMake.decreaseFontSize";
    public static final String Action_resetFontSize    = "JxMake.resetFontSize";

    public static final String Action_copyAsStyledText = "JxMake.copyAsStyledText";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final KeyStroke KS_rehighlightAll   = KeyStroke.getKeyStroke(MenuSpec.VK_F5      , MenuSpec.KModNone     );
    public static final KeyStroke KS_toggleLineWrap   = KeyStroke.getKeyStroke(MenuSpec.VK_F10     , MenuSpec.KModNone     );

    public static final KeyStroke KS_increaseFontSize = KeyStroke.getKeyStroke(MenuSpec.VK_ADD     , MenuSpec.KModCtrl     );
    public static final KeyStroke KS_decreaseFontSize = KeyStroke.getKeyStroke(MenuSpec.VK_SUBTRACT, MenuSpec.KModCtrl     );
    public static final KeyStroke KS_resetFontSize    = KeyStroke.getKeyStroke(MenuSpec.VK_0       , MenuSpec.KModCtrl     );

    public static final KeyStroke KS_copyAsStyledText = KeyStroke.getKeyStroke(MenuSpec.VK_C       , MenuSpec.KModCtrlShift);

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final KeyStroke KS_fileNew        = KeyStroke.getKeyStroke(MenuSpec.VK_N       , MenuSpec.KModCtrl     );
    public static final KeyStroke KS_fileOpen       = KeyStroke.getKeyStroke(MenuSpec.VK_O       , MenuSpec.KModCtrl     );
    public static final KeyStroke KS_fileOpenRecent = KeyStroke.getKeyStroke(MenuSpec.VK_O       , MenuSpec.KModCtrlShift);
    public static final KeyStroke KS_fileSave       = KeyStroke.getKeyStroke(MenuSpec.VK_S       , MenuSpec.KModCtrl     );
    public static final KeyStroke KS_fileSaveAs     = KeyStroke.getKeyStroke(MenuSpec.VK_S  ,      MenuSpec.KModCtrlShift);
    public static final KeyStroke KS_fileSaveAll    = KeyStroke.getKeyStroke(MenuSpec.VK_L  ,      MenuSpec.KModCtrl     );
    public static final KeyStroke KS_fileQuit       = KeyStroke.getKeyStroke(MenuSpec.VK_Q       , MenuSpec.KModCtrl     );

    public static final KeyStroke KS_editUndo       = KeyStroke.getKeyStroke(MenuSpec.VK_Z       , MenuSpec.KModCtrl     );
    public static final KeyStroke KS_editRedo       = KeyStroke.getKeyStroke(MenuSpec.VK_Z       , MenuSpec.KModCtrlShift);
    public static final KeyStroke KS_editCut        = KeyStroke.getKeyStroke(MenuSpec.VK_X       , MenuSpec.KModCtrl     );
    public static final KeyStroke KS_editCopy       = KeyStroke.getKeyStroke(MenuSpec.VK_C       , MenuSpec.KModCtrl     );
    public static final KeyStroke KS_editPaste      = KeyStroke.getKeyStroke(MenuSpec.VK_V       , MenuSpec.KModCtrl     );
    public static final KeyStroke KS_editIndent     = KeyStroke.getKeyStroke(MenuSpec.VK_TAB     , MenuSpec.KModNone     );
    public static final KeyStroke KS_editUnindent   = KeyStroke.getKeyStroke(MenuSpec.VK_TAB     , MenuSpec.KModShift    );
    public static final KeyStroke KS_editComment    = KeyStroke.getKeyStroke(MenuSpec.VK_G       , MenuSpec.KModCtrl     );
    public static final KeyStroke KS_editUncomment  = KeyStroke.getKeyStroke(MenuSpec.VK_G       , MenuSpec.KModCtrlShift);
    public static final KeyStroke KS_editFind       = KeyStroke.getKeyStroke(MenuSpec.VK_F       , MenuSpec.KModCtrl     );
    public static final KeyStroke KS_editReplace    = KeyStroke.getKeyStroke(MenuSpec.VK_R       , MenuSpec.KModCtrl     );

    public static final KeyStroke KS_viewFoldAll    = KeyStroke.getKeyStroke(MenuSpec.VK_SUBTRACT, MenuSpec.KModCtrlShift);
    public static final KeyStroke KS_viewUnfoldAll  = KeyStroke.getKeyStroke(MenuSpec.VK_ADD     , MenuSpec.KModCtrlShift);
    public static final KeyStroke KS_viewSpellChk   = KeyStroke.getKeyStroke(MenuSpec.VK_F7      , MenuSpec.KModNone     );
    public static final KeyStroke KS_viewChgDict    = KeyStroke.getKeyStroke(MenuSpec.VK_F7      , MenuSpec.KModCtrlShift);

    public static final String    ID_file           = "mnu_file";
    public static final String    ID_fileOpenRecent = "mnu_fileOpenRecent";
    public static final String    ID_filePrintFmt   = "mnu_filePrintFmt";
    public static final String    ID_filePrintTxt   = "mnu_filePrintTxt";
    public static final String    ID_fileQuit       = "mnu_fileQuit";

    public static final String    ID_edit           = "mnu_edit";
    public static final String    ID_editUndo       = "mnu_editUndo";
    public static final String    ID_editRedo       = "mnu_editRedo";
    public static final String    ID_editClear      = "mnu_editClear";
    public static final String    ID_editCut        = "mnu_editCut";
    public static final String    ID_editCopy       = "mnu_editCopy";
    public static final String    ID_editPaste      = "mnu_editPaste";

    public static final String    ID_view           = "mnu_view";
    public static final String    ID_viewLineWrap   = "mnu_viewLineWrap";
    public static final String    ID_viewSpellChk   = "mnu_editSpellChk";
    public static final String    ID_viewChgDict    = "mnu_editChgDict";


    public static final String    ID_help           = "mnu_help";
    public static final String    ID_helpAbout      = "mnu_helpAbout";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void mapKeyboardAction(final JxMakeRootPane rootPane)
    {
        final RSyntaxTextArea textArea    = rootPane.textArea    (                       );
        final InputMap        inputMap    = textArea.getInputMap (JComponent.WHEN_FOCUSED);
        final ActionMap       actionMap   = textArea.getActionMap(                       );
        final AtomicInteger   fzIncDecCnt = new AtomicInteger(0);

        inputMap.put( KS_rehighlightAll, Action_rehighlightAll );
        actionMap.put( Action_rehighlightAll, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                final String currentStyle = textArea.getSyntaxEditingStyle();
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                textArea.setSyntaxEditingStyle(currentStyle                     );
            }
        } );

        inputMap.put( KS_toggleLineWrap, Action_toggleLineWrap );
        actionMap.put( Action_toggleLineWrap, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                textArea.setLineWrap( !textArea.getLineWrap() );
                JxMakeRootPane_Menu.chkSetSelected( JxMakeRootPane_Action.ID_viewLineWrap, textArea.getLineWrap() );
            }
        } );

        inputMap.put( KS_increaseFontSize, Action_increaseFontSize );
        actionMap.put( Action_increaseFontSize, new RSyntaxTextAreaEditorKit.IncreaseFontSizeAction() {
            @Override
            public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea)
            {
                super.actionPerformedImpl(e, textArea);
                fzIncDecCnt.getAndIncrement();
            }
        } );

        inputMap.put( KS_decreaseFontSize, Action_decreaseFontSize );
        actionMap.put( Action_decreaseFontSize, new RSyntaxTextAreaEditorKit.DecreaseFontSizeAction() {
            @Override
            public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea)
            {
                super.actionPerformedImpl(e, textArea);
                fzIncDecCnt.getAndDecrement();
            }
        } );

        inputMap.put( KS_resetFontSize, Action_resetFontSize );
        actionMap.put( Action_resetFontSize, new RecordableTextAction(null) {
            @Override
            public final String getMacroID()
            { return Action_resetFontSize; }

            @Override
            public void actionPerformedImpl(final ActionEvent e, final RTextArea textArea)
            {
                int delta = fzIncDecCnt.getAndSet(0);

                if(delta == 0) return;

                final RecordableTextAction act = (delta > 0)
                                               ? new RSyntaxTextAreaEditorKit.DecreaseFontSizeAction()
                                               : new RSyntaxTextAreaEditorKit.IncreaseFontSizeAction();

                for( int i = 0; i < Math.abs(delta); ++i ) act.actionPerformedImpl(e, textArea);
            }
        } );

        inputMap.put( KS_copyAsStyledText, Action_copyAsStyledText );
        actionMap.put( Action_copyAsStyledText, new RSyntaxTextAreaEditorKit.CopyCutAsStyledTextAction(false) );
    }

    public static void mapMouseAction(final JxMakeRootPane rootPane)
    {
        final RSyntaxTextArea textArea = rootPane.textArea();

        textArea.addMouseWheelListener( new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(final MouseWheelEvent e)
            {
                final int mod = e.getModifiersEx();

                if( (mod & MenuSpec.KModCtrl) != 0 ) {
                    final String actionKey = (e.getWheelRotation() > 0) ? Action_decreaseFontSize : Action_increaseFontSize;
                    final Action action    = textArea.getActionMap().get(actionKey);
                    if(action != null) action.actionPerformed( new ActionEvent(textArea, ActionEvent.ACTION_PERFORMED, actionKey) );
                }

                else if(mod == 0) {
                    SwingUtilities.getAncestorOfClass(JScrollPane.class, textArea).dispatchEvent(
                        SwingUtilities.convertMouseEvent( textArea, e, rootPane.textAreaScrollPane() )
                    );
                }
            }
        } );
    }

} // class JxMakeRootPane_Action
