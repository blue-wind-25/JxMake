/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.gcomp;


import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.Timer;

import jxm.*;


@SuppressWarnings("serial")
public class JDrawChart extends JPanel implements ActionListener {

    /*
     * The code in this class is developed based on the information from:
     *     https://stackoverflow.com/a/18413639
     */

    private static final int         Padding         =  5;
    private static final int         LegendPadding   = 50;
    private static final int         XLabelPadding   = 20;
    private static final int         YLabelPadding   = 55;
    private static final int         XDivisions      = 10;
    private static final int         YDivisions      =  8;
    private static final int         TickLength      =  3;

    private static final Color       BackgroundColor = new Color(255, 255, 255);
    private static final Color       TextColor       = new Color(  0,   0,   0);
    private static final Color       TickColor       = new Color(  0,   0,   0);
    private static final Color       GridColor       = new Color(200, 200, 200);

    private static final BasicStroke GridStroke      = new BasicStroke( 1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1, new float[]{ 5 }, 0 );
    private static final Color[]     LineColors      = new Color[] {
                                                               new Color(  0,   0, 164),
                                                               new Color(  0, 164, 164),
                                                               new Color(  0, 164,   0),
                                                               new Color(164, 164,   0),
                                                               new Color(164,   0,   0),
                                                               new Color(164,   0, 164),
                                                               new Color(164, 164, 164),
                                                               new Color(  0,   0,   0)
                                                           };

    public  static final int         NumOfChannels   = LineColors.length;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final SerialConsole _sc;

    private final Timer         _timer;
    private       boolean       _refresh = true;

    private final boolean[]     _chnVis  = new boolean[] {
                                               true, true, true, true, true, true, true, true
                                           };

    private       boolean       _noDraw  = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("this-escape")
    public JDrawChart(SerialConsole sc)
    {
        _sc      = sc;
        _timer   = new Timer(100, this);
        _refresh = false;

        _timer.start();
    }

    public void setNoDraw(final boolean noDraw)
    { _noDraw = noDraw; }

    public synchronized void setChannelVisibility(final int channel, final boolean visible)
    { _chnVis[channel] = visible; }

    private synchronized boolean _getChannelVisibility(final int channel)
    { return _chnVis[channel]; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized void refresh()
    { _refresh = true; }

    private synchronized boolean _isRefreshTime()
    {
        final boolean r = _refresh;
        _refresh = false;
        return r;
    }

    @Override
    public synchronized void actionPerformed(final ActionEvent actionEvent)
    {
        if( actionEvent.getSource() != _timer ) return;
        if( _isRefreshTime() ) repaint();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized void saveImage(final String filePath) throws Exception
    {
        final int           scl = 2;
        final BufferedImage img = new BufferedImage( getWidth() * scl, getHeight() * scl, BufferedImage.TYPE_INT_ARGB );
        final Graphics2D    g2d = (Graphics2D) img.getGraphics();

        g2d.scale(scl, scl);
        paint(g2d);

        ImageIO.write( img, "png", new FileOutputStream(filePath) );

        g2d.dispose();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Pattern _reRemoveZeroes = Pattern.compile("(.*?)(\\..*?)(?:0*?)(e.+)?$");
    private static final Pattern _reRemoveEndDot = Pattern.compile("\\.$"                       );

    private static double _alignImpl_bn(double n)
    {
        n *= 10.0;

        final long digits = (long) Math.floor( Math.log10(n) );
        final long factor = (long) Math.pow  ( 10, digits    );
              long aval   = (long) (n / factor) * factor;

             if(aval <  n) aval += factor;
        else if(aval == 0) aval = 1;

        final long dval   = (long) Math.ceil(aval / 20.0);
              long nval   = (long) Math.ceil(n    / dval) * dval;

        while(aval > nval) aval -= dval;

        return (double) aval / 10.0;
    }

    private static double _alignImpl_sn(final double n)
    {
        double itrVal = n;
        int    digits = 0;

        while(itrVal < 1.0) {
            itrVal *= 10.0;
            ++digits;
        }

        final double factor = Math.pow(10, digits);

        return _alignImpl_bn(n * factor) / factor;
    }

    private static double _align(double n)
    {
        if(n > 0.0)  return (n >= 1.0) ? _alignImpl_bn(n) : _alignImpl_sn(n);
        return 1.0;
    }

    private static String _format(double n)
    { return _reRemoveEndDot.matcher( _reRemoveZeroes.matcher( String.format("%.6g", n) ).replaceAll("$1$2$3") ).replaceAll(""); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected synchronized void paintComponent(final Graphics g)
    {
        // Ensure the flag is reset
        _isRefreshTime();

        // Prepare the graphics
        super.paintComponent(g);

        if(_noDraw) return;

        final Graphics2D g2d = (Graphics2D) g; // g.create();

        if(JTextFieldPH.DesktopHints != null) g2d.setRenderingHints(JTextFieldPH.DesktopHints);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        /*
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING       , RenderingHints.VALUE_ANTIALIAS_ON                  );
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED     );
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING    , RenderingHints.VALUE_COLOR_RENDER_SPEED            );
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING          , RenderingHints.VALUE_DITHER_DISABLE                );
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS  , RenderingHints.VALUE_FRACTIONALMETRICS_OFF         );
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION      , RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING          , RenderingHints.VALUE_RENDER_SPEED                  );
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL     , RenderingHints.VALUE_STROKE_NORMALIZE              );
        //*/

        // Get and check the data set
        final SerialConsole.SPDataSets ds = _sc.spGetDataSets();

        if( ds.isEmpty() ) return;

        // Align the minimum and maximum values
        final double largest = Math.max( Math.abs(ds.minValue), Math.abs(ds.maxValue) );
        final double aligned = _align(largest);

        if(ds.maxValue >= 0 && ds.minValue >= 0) {
            ds.maxValue = aligned;
            ds.minValue = 0;
        }
        else if(ds.maxValue <= 0 && ds.minValue <= 0) {
            ds.maxValue = 0;
            ds.minValue = -aligned;
        }
        else {
            ds.maxValue = (ds.maxValue >= 0) ? aligned : -aligned;
            ds.minValue = (ds.minValue >= 0) ? aligned : -aligned;
        }

        // Get the width and height of the drawing area
        final int Padding2 = Padding * 2;
        final int aw       = getWidth ();
        final int ah       = getHeight() - LegendPadding;

        // Get the number of points and calculate the scale
        final int    pCount = ds.values.get(0).size();
        final double xScale = ( (double) aw - Padding2 - YLabelPadding ) / (pCount - 1               );
        final double yScale = ( (double) ah - Padding2 - XLabelPadding ) / (ds.maxValue - ds.minValue);

        // Draw the background
        g2d.setColor(BackgroundColor);
        g2d.fillRect(Padding + YLabelPadding, Padding, aw - Padding2 - YLabelPadding, ah - Padding2 - XLabelPadding);

        // Get the original stroke and font metrics
        final Stroke      origStroke  = g2d.getStroke     ();
        final FontMetrics fontMetrics = g2d.getFontMetrics();

        g2d.setStroke(GridStroke);

        // Create hatch marks, grid lines, and labels for Y axis
        for(int i = 0; i < YDivisions + 1; ++i) {
            // Calculate the coordinates
            final int x0 = Padding + YLabelPadding - TickLength;
            final int x1 = Padding + YLabelPadding + TickLength;
            final int y0 = ah - ( ( i * (ah - Padding2 - XLabelPadding) ) / YDivisions + Padding + XLabelPadding );
            final int y1 = y0;
            // Draw the grid
            final boolean center = (i == YDivisions / 2);
            g2d.setColor(GridColor);
            if(!center) g2d.setStroke(GridStroke);
            g2d.drawLine(Padding + YLabelPadding + 1 + TickLength, y0, aw - Padding, y1);
            if(!center) g2d.setStroke(origStroke);
            // Draw the tick
            g2d.setColor(TickColor);
            g2d.drawLine(x0, y0, x1, y1);
            // Draw the label
            final String yLabel = _format( ds.minValue + (ds.maxValue - ds.minValue) * ( (i * 1.0) / YDivisions ) );
            final int    width  = fontMetrics.stringWidth(yLabel);
            g2d.setColor(TextColor);
            g2d.drawString( yLabel, x0 - width - 4, y0 + fontMetrics.getAscent() - ( fontMetrics.getHeight() / 2 ) );
        } // for

        // Create hatch marks, grid lines, and labels for X axis
        if(pCount > 1) {
            for( int i = 0; i < pCount; i += (pCount / XDivisions) ) {
                // Calculate the coordinates
                final int x0 = i * (aw - Padding2 - YLabelPadding) / (pCount - 1) + Padding + YLabelPadding;
                final int x1 = x0;
                final int y0 = ah - Padding - XLabelPadding - TickLength;
                final int y1 = ah - Padding - XLabelPadding + TickLength;
                // Draw the grid
                g2d.setColor(GridColor);
                g2d.drawLine(x0, ah - Padding - XLabelPadding - 1 - TickLength, x1, Padding);
                // Draw the tick
                g2d.setColor(TickColor);
                g2d.drawLine(x0, y0, x1, y1);
                // Draw the label
                if(i < pCount) {
                    final long   index  = ds.index.get(i);
                    final String xLabel = (index < 0) ? "" : String.valueOf(index);
                    final int    width  = fontMetrics.stringWidth(xLabel);
                    g2d.setColor(TextColor);
                    g2d.drawString( xLabel, x0 - width / 2, y1 + fontMetrics.getAscent() + 3 );
                }
            } // for
        }

        // Create X and Y axes
        g2d.setColor(TickColor);
        g2d.drawLine(Padding + YLabelPadding, ah - Padding - XLabelPadding, Padding + YLabelPadding, Padding                     );
        g2d.drawLine(Padding + YLabelPadding, ah - Padding - XLabelPadding, aw - Padding           , ah - Padding - XLabelPadding);

        // Draw the line chart
        for( int s = 0; s < Math.min( NumOfChannels, ds.values.size() ); ++s ) {
            // Skip if the channel is invisible
            if( !_getChannelVisibility(s) ) continue;
            // Draw the lines
            g2d.setColor( LineColors[s] );
            for(int i = 0; i < pCount - 1; ++i) {
                // Calculate the coordinates
                final int j  = i + 1;
                final int x1 = (int) ( ( i * xScale + Padding + YLabelPadding  )                    );
                final int y1 = (int) ( ( ds.maxValue - ds.values.get(s).get(i) ) * yScale + Padding );
                final int x2 = (int) ( ( j * xScale + Padding + YLabelPadding  )                    );
                final int y2 = (int) ( ( ds.maxValue - ds.values.get(s).get(j) ) * yScale + Padding );
                // Draw the line
                g2d.drawLine(x1, y1, x2, y2);
            } // for
        } // for

        // Draw the legends' background
        final int lw = (aw - Padding2 - YLabelPadding);

        g2d.setColor(BackgroundColor);
        g2d.fillRect(Padding + YLabelPadding, ah, aw - Padding2 - YLabelPadding, ah - Padding2 - XLabelPadding + LegendPadding);

        // Draw the legends
        final Font origFont = g2d.getFont();
        final Font boldFont = origFont.deriveFont(Font.BOLD);

        for(int s = 0; s < NumOfChannels; ++s) {
            // Calculate the coordinates
            final int x = Padding2 + YLabelPadding + lw / 4 * (s % 4);
            final int y = ah       + XLabelPadding * ( (s < 4) ? 1 : 2 );
            // Draw the legend
            g2d.setColor( LineColors[s] );
            if( s < ds.names.size() ) {
                g2d.setFont(boldFont);
                g2d.drawString( ds.names.get(s), x, y );
                g2d.setFont(origFont);
            }
            else {
                g2d.drawString("─━─", x, y);
            }
        } // for

        /*
        // Dispose the graphics context
        g2d.dispose();
        //*/
    }

} // JDrawChart
