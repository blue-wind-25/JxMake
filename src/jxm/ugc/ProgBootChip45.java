/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.util.regex.Pattern;
import java.util.regex.Matcher;

import jxm.*;
import jxm.xb.*;


/*
 * Common superclass for all 'ProgBootChip45B*' classes, intended to simplify usage
 * and centralize shared utility functions.
 */
public abstract class ProgBootChip45 extends ProgBootSerial {

    @SuppressWarnings("serial")
    public static class Config extends ProgBootSerial.Config {
// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'xxx*()' functions ??? #####


public static Config ATtiny13()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 1024;
    config.memoryFlash.pageSize   =   32;
    config.memoryFlash.numPages   =   32;

    config.memoryEEPROM.totalSize =   64;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATmega8A()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.pageSize   =   64;
    config.memoryFlash.numPages   =  128;

    config.memoryEEPROM.totalSize =  512;

    return config;
}

public static Config ATmega16A()
{
    final Config config = ATmega8A();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   128;

    return config;
}

public static Config ATmega32A()
{
    final Config config = ATmega16A();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  1024;

    return config;
}

public static Config ATmega64A()
{
    final Config config = ATmega32A();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.pageSize   =   256;

    config.memoryEEPROM.totalSize =  2048;

    return config;
}

public static Config ATmega128A()
{
    final Config config = ATmega64A();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    512;

    config.memoryEEPROM.totalSize =   4096;

    return config;
}

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATmega48P()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 4096;
    config.memoryFlash.pageSize   =   64;
    config.memoryFlash.numPages   =   64;

    config.memoryEEPROM.totalSize =  256;

    return config;
}

public static Config ATmega88P()
{
    final Config config = ATmega48P();

    config.memoryFlash.totalSize  = 8192;
    config.memoryFlash.numPages   =  128;

    config.memoryEEPROM.totalSize =  512;

    return config;
}

public static Config ATmega168P()
{
    final Config config = ATmega88P();

    config.memoryFlash.totalSize  = 16384;
    config.memoryFlash.pageSize   =   128;

    return config;
}

public static Config ATmega328P()
{
    final Config config = ATmega168P();

    config.memoryFlash.totalSize  = 32768;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  1024;

    return config;
}

public static Config ArduinoUnoR3     () { return ATmega328P(); }
public static Config ArduinoUnoRev3   () { return ATmega328P(); }
public static Config ArduinoNano      () { return ATmega328P(); }
public static Config ArduinoProMini168() { return ATmega168P(); }
public static Config ArduinoProMini328() { return ATmega328P(); }
public static Config ArduinoProMini   () { return ATmega328P(); }

////////////////////////////////////////////////////////////////////////////////////////////////////

public static Config ATmega640()
{
    final Config config = new Config();

    config.memoryFlash.totalSize  = 65536;
    config.memoryFlash.pageSize   =   256;
    config.memoryFlash.numPages   =   256;

    config.memoryEEPROM.totalSize =  4096;

    return config;
}

public static Config ATmega1280()
{
    final Config config = ATmega640();

    config.memoryFlash.totalSize  = 131072;
    config.memoryFlash.numPages   =    512;

    return config;
}

public static Config ATmega2560()
{
    final Config config = ATmega1280();

    config.memoryFlash.totalSize  = 262144;
    config.memoryFlash.numPages   =   1024;

    return config;
}

public static Config ArduinoMega1280() { return ATmega1280(); }
public static Config ArduinoMega2560() { return ATmega2560(); }
public static Config ArduinoMega    () { return ATmega2560(); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    } // class Config

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected ProgBootChip45(final String progClassName, final ProgBootChip45.Config config) throws Exception
    {
        // Process the superclass
        super(progClassName, config);
    }

    public abstract boolean begin(final String serialDevice, final int baudrate);

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int GET_CONNECT_RETRY_COUNT = 3;

    protected Matcher _c45Connect(final String progClassName, final Pattern pmConnect, final boolean skipResetAndSync)
    {
        // Try to connect as several times
        for( int r = 0; r < (skipResetAndSync ? 1 : GET_CONNECT_RETRY_COUNT); ++r ) {

            if(!skipResetAndSync) {

                // Reset the MCU via DTR/RTS
                _serialResetMCU_DTR_RTS();
                SysUtil.sleepMS(25);

                // Send 'U' for autobaud detection until any byte is received
                final XCom.TimeoutMS tms = new XCom.TimeoutMS(TX_RX_TIMEOUT_MS_DEFL);
                      boolean        con = false;

                while(true) {
                    _serialTx('U');
                    if( serialRxUnreadCount() > 0 ) {
                        con = true;
                        break;
                    }
                    if( tms.timeout() ) {
                        USB2GPIO.notifyError(Texts.PDevXXX_RxTimeout, progClassName);
                        break;
                    }
                }

                if(!con) {
                    USB2GPIO.notifyError(Texts.ProgXXX_FailConnectBLTout, progClassName);
                    continue;
                }

            } // if

            // Read the bytes
            final char[] bytes = new char[128];
                  int    idx   = 0;

            while( serialRxUnreadCount() > 0 ) {
                final Integer ch = _serialRxUInt8();
                if(ch < 0) return null;
                bytes[idx++] = (char) (int) ch;
            }

            /*
            for(int i = 0; i < idx; ++i) SysUtil.stdDbg().printf( "%02X %c\n", (int) bytes[i], bytes[i] );
            //*/

            // Perform pattern matching
            final Matcher m = pmConnect.matcher( new String(bytes, 0, idx) );

            if( m.matches() ) {
                SysUtil.sleepMS(25);
                return m;
            }

        } // for

        // Error
        return null;
    }

} // class ProgBootChip45
