/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import jxm.*;
import jxm.xb.*;


/*
 * Simple implementation of the XTEA encryption.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written mostly based on the algorithms and information found from:
 *
 *     XTEA
 *     https://en.wikipedia.org/wiki/XTEA
 *
 *     chip45boot3
 *     https://github.com/eriklins/chip45boot3
 *     https://github.com/eriklins/chip45boot3/blob/main/chip45boot3_windows_encrypt_tool/XtaeTools.cs
 *     Copyright (C) 2023 ER!K
 *     MIT License
 */
public class XTEA {

    // 128-bit XTEA key in four 32-bit unsigned integers
    private        final long[] _key;

    // Buffers
    private static final int    BufferSize = 8 * 1024 * 1024; // 8 megabytes
    private static final long   Delta      = 0x9E3779B9L;

    private        final int[]  _resBuff   = new int[BufferSize];
    private        final int[]  _v         = new int[2];
    private        final int[]  _e         = new int[2];

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public XTEA(final long[] key)
    { _key = key.clone(); }

    private void _encBlock(final int[] v) // Encrypt a block of 64 bits
    {
        long v0  = v[0] & 0xFFFFFFFFL;
        long v1  = v[1] & 0xFFFFFFFFL;
        long sum = 0;

        for(int rounds = 0; rounds < 32; ++rounds) {

            final long part1 = ( (v1 << 4) ^ (v1 >>> 5) ) + v1;
            final long key1  = _key[ (int) (  sum         & 3 ) ];
                       v0    = ( v0 + ( part1 ^ (sum + key1) ) ) & 0xFFFFFFFFL;

                       sum   = (sum + Delta) & 0xFFFFFFFFL;

            final long part2 = ( (v0 << 4) ^ (v0 >>> 5) ) + v0;
            final long key2  = _key[ (int) ( (sum >>> 11) & 3 ) ];
                       v1    = ( v1 + ( part2 ^ (sum + key2) ) ) & 0xFFFFFFFFL;

        } // for

        _e[0] = (int) (v0 & 0xFFFFFFFFL);
        _e[1] = (int) (v1 & 0xFFFFFFFFL);
    }

    public synchronized int[] encrypt(final int[] buff, final int length) // Encrypt a buffer with XTEA
    {
        if( _key.length  != 4 ) return null;
        if( (length % 8) != 0 ) return null;

        for(int i = 0; i < length; i += 8) {

            // Compose a block of 64 bits into two 32-bit unsigned integers
            _v[0] = (buff[i + 0] << 24) + (buff[i + 1] << 16) + (buff[i + 2] << 8) + (buff[i + 3] << 0);
            _v[1] = (buff[i + 4] << 24) + (buff[i + 5] << 16) + (buff[i + 6] << 8) + (buff[i + 7] << 0);

            // Encrypt this block
            _encBlock(_v);

            // Write the result into the global result buffer
            _resBuff[i + 0] = (_e[0] >> 24) & 0xFF;
            _resBuff[i + 1] = (_e[0] >> 16) & 0xFF;
            _resBuff[i + 2] = (_e[0] >>  8) & 0xFF;
            _resBuff[i + 3] = (_e[0] >>  0) & 0xFF;
            _resBuff[i + 4] = (_e[1] >> 24) & 0xFF;
            _resBuff[i + 5] = (_e[1] >> 16) & 0xFF;
            _resBuff[i + 6] = (_e[1] >>  8) & 0xFF;
            _resBuff[i + 7] = (_e[1] >>  0) & 0xFF;

        } // for

        return _resBuff;
    }

    public synchronized int[] encrypt(final int[] buff)
    { return encrypt(buff, buff.length); }

} // class XTEA
