/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp;


import java.util.Map;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;

import javax.swing.JTextField;


@SuppressWarnings("serial")
public class JTextFieldPH extends JTextField {

    public static final Map<?, ?> DesktopHints = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _placeholder;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public JTextFieldPH()
    {}

    public JTextFieldPH(final String text)
    { super(text); }

    public void setPlaceholder(final String text)
    { _placeholder = text; }

    public String getPlaceholder()
    { return _placeholder; }

    @Override
    protected void paintComponent(final Graphics g)
    {
        super.paintComponent(g);

        if( _placeholder == null || _placeholder.length() == 0 || getText().length() > 0 ) return;

        final Graphics2D g2d = (Graphics2D) g;

        if(DesktopHints != null) g2d.setRenderingHints(DesktopHints);

        g2d.setColor( getDisabledTextColor() );
        g2d.drawString( _placeholder, getInsets().left, g2d.getFontMetrics().getMaxAscent() + getInsets().top );
    }
}
