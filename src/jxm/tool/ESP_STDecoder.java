/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * ====================================================================================================
 * The code in this file is developed based on:
 * ----------------------------------------------------------------------------------------------------
 * Command-line Decoder for Stack Trace from ESP8266
 *     https://github.com/littleyoda/EspStackTraceDecoder
 *     https://github.com/littleyoda/EspStackTraceDecoder/releases/tag/untagged-59a763238a6cedfe0362
 * ----------------------------------------------------------------------------------------------------
 * MIT License
 *
 * Copyright (C) 2016 Sven
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ====================================================================================================
 * In addition, some information from:
 *     https://github.com/tve/esp32-backtrace
 * is also used.
 * ====================================================================================================
 */


package jxm.tool;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxm.*;


public class ESP_STDecoder  {

    private static final String[] Str_Exceptions = {
        "Illegal instruction",
        "SYSCALL instruction",
        "InstructionFetchError: Processor internal physical address or data error during instruction fetch",
        "LoadStoreError: Processor internal physical address or data error during load or store",
        "Level1Interrupt: Level-1 interrupt as indicated by set level-1 bits in the INTERRUPT register",
        "Alloca: MOVSP instruction, if caller's registers are not in the register file",
        "IntegerDivideByZero: QUOS, QUOU, REMS, or REMU divisor operand is zero",
        "reserved",
        "Privileged: Attempt to execute a privileged operation when CRING ? 0",
        "LoadStoreAlignmentCause: Load or store to an unaligned address",
        "reserved",
        "reserved",
        "InstrPIFDataError: PIF data error during instruction fetch",
        "LoadStorePIFDataError: Synchronous PIF data error during LoadStore access",
        "InstrPIFAddrError: PIF address error during instruction fetch",
        "LoadStorePIFAddrError: Synchronous PIF address error during LoadStore access",
        "InstTLBMiss: Error during Instruction TLB refill",
        "InstTLBMultiHit: Multiple instruction TLB entries matched",
        "InstFetchPrivilege: An instruction fetch referenced a virtual address at a ring level less than CRING",
        "reserved",
        "InstFetchProhibited: An instruction fetch referenced a page mapped with an attribute that does not permit instruction fetch",
        "reserved",
        "reserved",
        "reserved",
        "LoadStoreTLBMiss: Error during TLB refill for a load or store",
        "LoadStoreTLBMultiHit: Multiple TLB entries matched for a load or store",
        "LoadStorePrivilege: A load or store referenced a virtual address at a ring level less than CRING",
        "reserved",
        "LoadProhibited: A load referenced a page mapped with an attribute that does not permit loads",
        "StoreProhibited: A store referenced a page mapped with an attribute that does not permit stores"
    };

    private static final Pattern _pmAST1 = Pattern.compile(  "40[0-2][0-9a-f]{5}\\b"  ); // ESP8266
    private static final Pattern _pmAST2 = Pattern.compile("0x40[0-2][0-9a-f]{5}\\b"  ); // ESP32

    private static final Pattern _pmAEX1 = Pattern.compile("Exception \\(([0-9]*)\\):"); // ESP8266
    private static final Pattern _pmAEX2 = Pattern.compile("EXCCAUSE: (0x[0-9a-f]*)"  ); // ESP32

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final String  _tool;
    private final String  _elf;
    private final String  _content;

    private final boolean _esp32;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private ESP_STDecoder(final String xtensa_addr2line, final String elf_file, final String content)
    {
        _tool    = xtensa_addr2line;
        _elf     = elf_file;
        _content = content;
        _esp32   = ( content.indexOf("Backtrace:") > 0 );
    }

    private String _analyseException()
    {
        final Matcher matcher = _esp32
                              ? _pmAEX2.matcher(_content)
                              : _pmAEX1.matcher(_content);

        final StringBuilder sb = new StringBuilder("Exception Cause: ");

        if( !matcher.find() ) {
            sb.append("Not found");
        }
        else {
            final int idx = Integer.decode( matcher.group(1) );
            sb.append( matcher.group(1) );
            sb.append( " "              );

            if( idx >= 0 && idx < Str_Exceptions.length ) sb.append(" [" + Str_Exceptions[idx] + "]");
            else                                          sb.append(" [Unknown]"                    );
        }

        return sb.toString();
    }

    private String[] _analyseStacktrace()
    {
        final ArrayList<String> list = new ArrayList<>();

        list.add(_tool   );
        list.add("-aipfC");
        list.add("-e"    );
        list.add(_elf    );

        final Matcher matcher = _esp32
                              ? _pmAST2.matcher( _content.substring( _content.indexOf("Backtrace:") + 11 ) )
                              : _pmAST1.matcher( _content                                                  );

        while( matcher.find() ) list.add( matcher.group() );

        if( list.size() == 4 ) return null;

        return list.toArray( new String[ list.size() ] );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ArrayList<String> decode(final String xtensa_addr2line, final String elf_file, final String content) throws IOException, InterruptedException
    {
        final ArrayList<String> result   = new ArrayList<>();
        final ESP_STDecoder     espSTDec = new ESP_STDecoder( SysUtil.resolveAbsolutePath(xtensa_addr2line), SysUtil.resolveAbsolutePath(elf_file), content );

        result.add( espSTDec._analyseException() );
        result.add( "" );

        final String[] commands = espSTDec._analyseStacktrace();

        if(commands == null) {
            result.add("No address found in stack trace!");
        }

        else {
            final ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);

            final Process        pr = pb.start();
            final BufferedReader br = new BufferedReader( new InputStreamReader( pr.getInputStream() ) );

            while(true) {
                final String line = br.readLine();
                if(line == null) break;
                result.add(line);
            }

            pr.waitFor();
            br.close();
        }

        return result;
    }

} // class ESP_STDecoder
