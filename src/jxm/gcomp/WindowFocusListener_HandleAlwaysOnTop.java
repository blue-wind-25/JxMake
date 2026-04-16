/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp;


import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jxm.*;


/*
 * Some Window Managers (WM) have bugs that may cause a window launched from a console to
 * not be visible unless AlwaysOnTop is set.
 *
 * However, in certain conditions the window may still be treated as non-focused even when
 * the user clicks on it.
 *
 * This class removes AlwaysOnTop after:
 *     1. The window successfully gains focus.
 *     2. A timeout expires (to handle the non-focus bug).
 */
public class WindowFocusListener_HandleAlwaysOnTop implements WindowFocusListener {

    private final JFrame _mainWindow;
    private       long   _lastTime;

    private final Timer  _timer;

    @SuppressWarnings("this-escape")
    public WindowFocusListener_HandleAlwaysOnTop(final JFrame mainWindow)
    {
        _mainWindow = mainWindow;
        _lastTime   = SysUtil.getMS();

        _timer = new Timer( 250, e -> _checkTimeout() );
        _timer.start();
    }

    @Override
    public synchronized void windowGainedFocus(final WindowEvent ignored)
    {
        final long newTime = SysUtil.getMS();

        if(newTime - _lastTime < 250) {

                _mainWindow.setFocusableWindowState(false);

            SwingUtilities.invokeLater( () -> {
                _mainWindow.setFocusableWindowState(true );
            } );

        }

        _lastTime = newTime;
    }

    @Override
    public synchronized void windowLostFocus(final WindowEvent ignored)
    {
        final long newTime = SysUtil.getMS();

        if(newTime - _lastTime > 250) _removeHandlers(false);

        _lastTime = newTime;
    }

    private synchronized void _checkTimeout()
    {
        final long newTime = SysUtil.getMS();

        if( !_mainWindow.isFocused() && newTime - _lastTime > 2500 ) _removeHandlers(true);
    }

    private void _removeHandlers(final boolean requestFocus)
    {
        _timer.stop();

        if(requestFocus) {
            _mainWindow.toFront();
            _mainWindow.requestFocus();
        }

        SwingUtilities.invokeLater( () -> {
            _mainWindow.setAlwaysOnTop(false);
            _mainWindow.removeWindowFocusListener(this);
        } );
    }

} // class WindowFocusListener_HandleAlwaysOnTop
