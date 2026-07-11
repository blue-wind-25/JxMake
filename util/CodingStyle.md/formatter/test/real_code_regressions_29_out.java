/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

package test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

public class RealCodeRegressions29 {

    private Timer  _jumpTimer;
    private String _jumpTarget;

    private void _jumpToTarget(String target)
    {
        _jumpTarget = target;
        _jumpTimer  = new Timer(
            0, new ActionListener() {
                public void actionPerformed(ActionEvent event)
                {
                    scrollToReference(_jumpTarget);
                    _jumpTimer.stop();
                    _jumpTimer = null;
                }
            } // class
        );
        _jumpTimer.start();
    }

    private void scrollToReference(String target)
    {
    }

} // class RealCodeRegressions29
