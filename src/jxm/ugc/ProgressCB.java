/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.PrintStream;

import java.util.Arrays;
import java.util.function.IntConsumer;

import jxm.*;


public class ProgressCB {

    public int _pcb_bytCounter;
    public int _pcb_prvPercent;
    public int _pcb_curPercent;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _callProgressCallback_impl(final IntConsumer progressCallback, final int totalBytes, final boolean incByteCounter)
    {
        if(progressCallback == null || totalBytes <= 0) return;

        if(incByteCounter) _pcb_bytCounter += 2;

        _pcb_curPercent = ( ( _pcb_bytCounter * 100 + (totalBytes / 2) ) / totalBytes );

        if(_pcb_prvPercent != _pcb_curPercent) {
            _pcb_prvPercent = _pcb_curPercent;
            progressCallback.accept(_pcb_curPercent);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public void callProgressCallbackInitial(final IntConsumer progressCallback, final int totalBytes)
    {
        _pcb_bytCounter =  0;
        _pcb_prvPercent = -1;
        _pcb_curPercent =  0;

        _callProgressCallback_impl(progressCallback, totalBytes, false);
    }

    public void callProgressCallbackCurrent(final IntConsumer progressCallback, final int totalBytes)
    { _callProgressCallback_impl(progressCallback, totalBytes, true); }

    public void callProgressCallbackCurrentMulti(final IntConsumer progressCallback, final int totalBytes, final int repeatCount)
    {
        for(int i = 0; i < repeatCount; ++i) {
            _callProgressCallback_impl(progressCallback, totalBytes, true);
        }
    }

    public void callProgressCallbackFinal(final IntConsumer progressCallback, final int totalBytes)
    {
        _pcb_bytCounter = totalBytes;

        _callProgressCallback_impl(progressCallback, totalBytes, false);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int getNumTicks(final int totalBytes)
    { return (totalBytes < 2) ? 0 : Math.min( (totalBytes & 0xFFFFFFFE) / 2 + 1, 101 ); }

    public static int getNumTicksExt(final int totalBytes, final int multipleOf)
    {
        if(totalBytes < 2) return 0;

        final int[] ticks = getTicks(totalBytes);
        if(ticks == null) return 0;

        int n = 0;

        for(final int t : ticks) {
            if(t % multipleOf == 0) ++n;
        }

        return n;
    }

    public static int[] getTicks(final int totalBytes_)
    {
        final int totalBytes = totalBytes_ & 0xFFFFFFFE;
        final int numTicks   = getNumTicks(totalBytes);

        if(numTicks <= 0) return null;

        final int ticks[] = new int[numTicks];
              int tidx    = 0;

        ticks[tidx++] = 0;

        int pp = 0;
        int cp = 0;
        for(int i = 2; i < totalBytes; i += 2) {
            cp = ( ( i * 100 + (totalBytes / 2) ) / totalBytes );
            if(pp != cp) {
                pp = cp;
                ticks[tidx++] = cp;
            }
        }
        if( ticks[tidx - 1] != 100 ) ticks[tidx] = 100;

        return ticks;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int StdProgressMF = 5;

    public static String getStdProgressInfoString(final int totalBytes)
    {
        final int n = getNumTicksExt(totalBytes, StdProgressMF);
        if(n <= 0) return "";

        char[] array = new char[
                             (n < 4) ? (n         - 1)
                           : (n < 8) ? (n - 1 - 1 - 1)
                           :           (n - 3 - 3 - 1)
                       ];
        Arrays.fill(array, ' ');

        return   (n < 4) ? "["    + ( new String(array) ) +    "]"
               : (n < 8) ? "[:"   + ( new String(array) ) +   "#]"
               :           "[000" + ( new String(array) ) + "100]";
    }

    public static String getStdProgressInfoStringPE(final int totalBytes)
    { return "   " + getStdProgressInfoString(totalBytes); }

    public static IntConsumer getStdProgressPrinter(final PrintStream ps)
    {
        return new IntConsumer() {
            @Override
            public void accept(int cp)
            {
                     if(cp                 ==   0) ps.print  ( "[");
                else if(cp                 == 100) ps.println(".]");
                else if(cp % StdProgressMF ==   0) ps.print  ( ".");
            }
        };
    }

} // class ProgressCB
