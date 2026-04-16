/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool.fwc;


import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.regex.Pattern;

import jxm.*;
import jxm.annotation.*;


public class FWUtil {

    public static boolean PRINT_LOAD_SAVE_DEBUG = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Common constants for 'FWWriter_*' implementations
    public static final int MaxRecLineDByteCnt = 16;

    public static final int _04kiB             =  4 * 1024       ;
    public static final int _64kiB             = 64 * 1024       ;
    public static final int _01MiB             =      1024 * 1024;
    public static final int _16MiB             = 16 * 1024 * 1024;

    public static final int _64kiB_Min1        = _64kiB - 1;
    public static final int _01MiB_Min1        = _01MiB - 1;
    public static final int _16MiB_Min1        = _16MiB - 1;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Constants for 'ELF Binary' format
    // https://en.wikipedia.org/wiki/Executable_and_Linkable_Format
    public static final String ELFB_RName_L = "Executable and Linkable Format";
    public static final String ELFB_RName_S = "ELFBIN";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Constants for 'Raw Binary' format
    public static final String RBIN_RName_L = "Raw Binary";
    public static final String RBIN_RName_S = "RAWBIN";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Constants for 'Intel Hex' format
    // https://en.wikipedia.org/wiki/Intel_HEX
    public static final int    IHEX_RT_Data                   = 0;
    public static final int    IHEX_RT_EndOfFile              = 1;
    public static final int    IHEX_RT_ExtendedSegmentAddress = 2;
    public static final int    IHEX_RT_StartSegmentAddress    = 3;
    public static final int    IHEX_RT_ExtendedLinearAddress  = 4;
    public static final int    IHEX_RT_StartLinearAddress     = 5;

    public static final String IHEX_RName_L                   = "Intel Hex";
    public static final String IHEX_RName_S                   = "INTHEX";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Constants for 'Motorola S-Record' format
    // https://en.wikipedia.org/wiki/SREC_(file_format)
    public static final char   SREC_RT_Header         = '0';
    public static final char   SREC_RT_Data16         = '1';
    public static final char   SREC_RT_Data24         = '2';
    public static final char   SREC_RT_Data32         = '3';
    public static final char   SREC_RT_Count16        = '5';
    public static final char   SREC_RT_Count24        = '6';
    public static final char   SREC_RT_StartAddress32 = '7';
    public static final char   SREC_RT_StartAddress24 = '8';
    public static final char   SREC_RT_StartAddress16 = '9';

    public static final String SREC_RName_L           = "Motorola S-Record";
    public static final String SREC_RName_S           = "MTSREC";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Constants for 'Extended Tektronix Hex' format
    // https://en.wikipedia.org/wiki/Tektronix_hex_format
    public static final char   THEX_RT_ExtraInformation = '3';
    public static final char   THEX_RT_Data             = '6';
    public static final char   THEX_RT_Termination      = '8';

    public static final String THEX_RName_L             = "Tektronix Hex";
    public static final String THEX_RName_S             = "TEKHEX";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Constants for 'MOS Technology' format
    // https://linux.die.net/man/5/srec_mos_tech
    public static final String MOST_RName_L = "MOS Technology";
    public static final String MOST_RName_S = "MOSTEC";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Constants for 'TI-TXT Hex' format
    // https://downloads.ti.com/docs/esd/SPNU118T/Content/SPNU118T_HTML/hex-conversion-utility-description.html
    public static final String TIXH_RName_L               = "Texas Instruments Text Hex";
    public static final String TIXH_RName_S               = "TIXHEX";

    public static final int    TIXH_StdMaxRecLineDByteCnt = 16; // NOTE : This byte count is fixed; !!! DO NOT MODIFY !!!

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Constants for 'ASCII Hex' constants (support for this format is currently read-only)
    // https://linux.die.net/man/5/srec_ascii_hex
    public static final String ASCH_RName_L = "ASCII Hex";
    public static final String ASCH_RName_S = "ASCHEX";

    // Constants for 'Verilog VMEM' constants (support for this format is currently read-only)
    // https://linux.die.net/man/5/srec_vmem
    public static final String VMEM_RName_L = "Verilog VMEM";
    public static final String VMEM_RName_S = "VLVMEM";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static byte[] _readBytes(final ByteArrayInputStream bis, final int len)
    {
        final byte[] res = new byte[len];
        if( bis.read(res, 0, len) != len ) return null;

        return res;
    }

    public static int _readUInt08(final ByteArrayInputStream bis)
    { return bis.read(); }

    public static int[] _readUInt08(final ByteArrayInputStream bis, final int len)
    {
        final byte[] res = new byte[len];
        if( bis.read(res, 0, len) != len ) return null;

        final int[] ints = new int[len];
        for(int i = 0; i < res.length; ++i) ints[i] = res[i] & 0xFF;

        return ints;
    }

    public static int _readUInt16LE(final ByteArrayInputStream bis)
    {
        final int b0 = bis.read(); if(b0 < 0) return -1;
        final int b1 = bis.read(); if(b1 < 0) return -1;

        return (b1 << 8) | b0;
    }

    public static int _readUInt16BE(final ByteArrayInputStream bis)
    {
        final int b1 = bis.read(); if(b1 < 0) return -1;
        final int b0 = bis.read(); if(b0 < 0) return -1;

        return (b1 << 8) | b0;
    }

    public static int _readUInt16(final ByteArrayInputStream bis, final boolean LE)
    { return LE ? _readUInt16LE(bis) :_readUInt16BE(bis); }

    public static long _readUInt32LE(final ByteArrayInputStream bis)
    {
        final long b0 = bis.read(); if(b0 < 0) return -1;
        final long b1 = bis.read(); if(b1 < 0) return -1;
        final long b2 = bis.read(); if(b2 < 0) return -1;
        final long b3 = bis.read(); if(b3 < 0) return -1;

        return (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    public static long _readUInt32BE(final ByteArrayInputStream bis)
    {
        final long b3 = bis.read(); if(b3 < 0) return -1;
        final long b2 = bis.read(); if(b2 < 0) return -1;
        final long b1 = bis.read(); if(b1 < 0) return -1;
        final long b0 = bis.read(); if(b0 < 0) return -1;

        return (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    public static long _readUInt32(final ByteArrayInputStream bis, final boolean LE)
    { return LE ? _readUInt32LE(bis) :_readUInt32BE(bis); }

    public static long _readUInt64LE(final ByteArrayInputStream bis)
    {
        final long b0 = bis.read(); if(b0 < 0) return -1;
        final long b1 = bis.read(); if(b1 < 0) return -1;
        final long b2 = bis.read(); if(b2 < 0) return -1;
        final long b3 = bis.read(); if(b3 < 0) return -1;
        final long b4 = bis.read(); if(b4 < 0) return -1;
        final long b5 = bis.read(); if(b5 < 0) return -1;
        final long b6 = bis.read(); if(b6 < 0) return -1;
        final long b7 = bis.read(); if(b7 < 0) return -1;

        return (b7 << 56) | (b6 << 48) | (b5 << 40) | (b4 << 32) | (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    public static long _readUInt64BE(final ByteArrayInputStream bis)
    {
        final long b7 = bis.read(); if(b7 < 0) return -1;
        final long b6 = bis.read(); if(b6 < 0) return -1;
        final long b5 = bis.read(); if(b5 < 0) return -1;
        final long b4 = bis.read(); if(b4 < 0) return -1;
        final long b3 = bis.read(); if(b3 < 0) return -1;
        final long b2 = bis.read(); if(b2 < 0) return -1;
        final long b1 = bis.read(); if(b1 < 0) return -1;
        final long b0 = bis.read(); if(b0 < 0) return -1;

        return (b7 << 56) | (b6 << 48) | (b5 << 40) | (b4 << 32) | (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    public static long _readUInt64(final ByteArrayInputStream bis, final boolean LE)
    { return LE ? _readUInt64LE(bis) :_readUInt64BE(bis); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Parse the given string as two-digit hexadecimal value(s)
    private static final Pattern _pmSplit2Chr = Pattern.compile("(?<=\\G..)");

    public static byte[] parse2Hexs(final String str)
    {
        if( str.isEmpty() ) return null;

        /*
        final String[] parts = _pmSplit2Chr.split(str);
        final byte[]   partn = new byte[parts.length];

        for(int i = 0; i < parts.length; ++i) partn[i] = (byte) ( Integer.parseInt(parts[i], 16) & 0xFF );
        //*/

        //*
        final byte[] partn = new byte[ str.length() / 2 ];

        for(int i = 0; i < partn.length; ++i) {
            partn[i] = (byte) ( ( Character.digit( str.charAt(i * 2    ), 16 ) << 4 ) |
                                ( Character.digit( str.charAt(i * 2 + 1), 16 )      ) );
        }
        //*/

        return partn;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Convert the given two-digit big-endian hexadecimal value (in an array of bytes) to a numeric address
    public static long cnv2BEHexsToAddress(final byte[] data)
    {
        final long d1 = data[0] & 0xFF;
        final long d0 = data[1] & 0xFF;

        return (d1 << 8) | d0;
    }

    // Convert the given three-digit big-endian hexadecimal value (in an array of bytes) to a numeric address
    public static long cnv3BEHexsToAddress(final byte[] data)
    {
        final long d2 = data[0] & 0xFF;
        final long d1 = data[1] & 0xFF;
        final long d0 = data[2] & 0xFF;

        return (d2 << 16) | (d1 << 8) | d0;
    }

    // Convert the given four-digit big-endian hexadecimal value (in an array of bytes) to a numeric address
    public static long cnv4BEHexsToAddress(final byte[] data)
    {
        final long d3 = data[0] & 0xFF;
        final long d2 = data[1] & 0xFF;
        final long d1 = data[2] & 0xFF;
        final long d0 = data[3] & 0xFF;

        return (d3 << 24) | (d2 << 16) | (d1 << 8) | d0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Convert hexadecimal character to decimal
    private static final String _HexDigits = "0123456789ABCDEF";

    public static int hexChrToDec(final char hexChr)
    { return _HexDigits.indexOf( Character.toUpperCase(hexChr) ); }

    public static int hexChr2ToDec(final char hexChr1, final char hexChr0)
    { return hexChrToDec(hexChr1) * 16 + hexChrToDec(hexChr0); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final char[] _Dec2Hex = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    // Get the hexadecimal value of the given nibble
    public static char _nibble2Hexs(final int value)
    { return _Dec2Hex[value & 0x0F]; }

    public static char _nibble2Hexs(final byte value)
    { return _Dec2Hex[value & 0x0F]; }

    // Store a two-digit hexadecimal value into the given 'StringBuilder'
    public static void _put2Hexs(final StringBuilder dst, final int value)
    {
        dst.append( _Dec2Hex[value >> 4   ] );
        dst.append( _Dec2Hex[value &  0x0F] );
    }

    public static void _put2Hexs(final StringBuilder dst, final byte value)
    { _put2Hexs(dst, value & 0xFF); }

    // Store the given data as two-digit hexadecimal value(s) into the given 'StringBuilder'
    public static void _putValues2Hexs(final StringBuilder dst, final int... value)
    { for(final int v : value) _put2Hexs(dst, v); }

    public static void _putData2Hexs(final StringBuilder dst, final byte[] data)
    { for(final byte v : data) _put2Hexs(dst, v); }

    // Store the given data and checksum as two-digit hexadecimal values into the given 'StringBuilder'
    public static void _putData2Hexs(final StringBuilder dst, final byte[] data, final int... checksum)
    {
        for(final byte v : data    ) _put2Hexs(dst, v);
        for(final int  v : checksum) _put2Hexs(dst, v);
    }

    // Store a two-digit hexadecimal value into the given 'BufferedWriter'
    public static void _put2Hexs(final BufferedWriter dst, final int value) throws IOException
    {
        dst.write( _Dec2Hex[value >> 4   ] );
        dst.write( _Dec2Hex[value &  0x0F] );
    }

    public static void _put2Hexs(final BufferedWriter dst, final byte value) throws IOException
    { _put2Hexs(dst, value & 0xFF); }

    // Store the given data and checksum as two-digit hexadecimal values with spaces into the given 'BufferedWriter'
    public static void _putData2HexsWithSpaces(final BufferedWriter dst, final byte[] data) throws IOException
    {
        boolean first = true;

        for(final byte v : data) {

            // Put space as needed
            if(first) first = false;
            else      dst.write(' ');

            // Put the data
            _put2Hexs(dst, v);

        } // for
    }

    // Store a two-digit hexadecimal value into the given integer array
    public static int _put2Hexs(final int[] dst, final int idx, final int value)
    {
        dst[idx + 0] = _Dec2Hex[value >> 4   ];
        dst[idx + 1] = _Dec2Hex[value &  0x0F];

        return idx + 2;
    }

    public static int _put2Hexs(final int[] dst, final int idx, final byte value)
    { return _put2Hexs(dst, idx, value & 0xFF); }

    // Store the given data as two-digit hexadecimal value(s) into the given integer array
    public static int _putData2Hexs(final int[] dst, final int idx, final byte[] data)
    {
        int idx_ = idx;

        for(final byte v : data) idx_ = _put2Hexs(dst, idx_, v);

        return idx_;
    }

} // class FWUtil
