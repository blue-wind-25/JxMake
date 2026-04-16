/***** PCPP_BEG *****/

/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * NOTE : # This file is included from 'ProgBootSAMBA.java.in'.
 *        # Use PCPP and GNU sed to generate (preprocess) the final code (please refer to
 *          the '../../../README' file for more details).
 */

/***** PCPP_END *****/


public static class FlashEEFC extends FlashOper {

    // NOTE : This class was adapted from the BOSSA source code

    private static final long EEFC0_FMR(final long base) { return base + 0x0000; }
    private static final long EEFC0_FCR(final long base) { return base + 0x0004; }
    private static final long EEFC0_FSR(final long base) { return base + 0x0008; }
    private static final long EEFC0_FRR(final long base) { return base + 0x000C; }

    private static final long EEFC1_FMR(final long base) { return base + 0x0200; }
    private static final long EEFC1_FCR(final long base) { return base + 0x0204; }
    private static final long EEFC1_FSR(final long base) { return base + 0x0208; }
    private static final long EEFC1_FRR(final long base) { return base + 0x020C; }

    private static final long  EEFC_KEY        = 0x5A;

    private static final long  EEFC_FCMD_GETD  = 0x00;
    private static final long  EEFC_FCMD_WP    = 0x01;
    private static final long  EEFC_FCMD_WPL   = 0x02;
    private static final long  EEFC_FCMD_EWP   = 0x03;
    private static final long  EEFC_FCMD_EWPL  = 0x04;
    private static final long  EEFC_FCMD_EA    = 0x05;
    private static final long  EEFC_FCMD_EPA   = 0x07;
    private static final long  EEFC_FCMD_SLB   = 0x08;
    private static final long  EEFC_FCMD_CLB   = 0x09;
    private static final long  EEFC_FCMD_GLB   = 0x0A;
    private static final long  EEFC_FCMD_SGPB  = 0x0B;
    private static final long  EEFC_FCMD_CGPB  = 0x0C;
    private static final long  EEFC_FCMD_GGPB  = 0x0D;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FlashEEFC(
        final ProgBootSAMBA instProgBootSAMBA,
        final long          flashBaseAddress,
        final int           flashNumberOfPages,
        final int           flashBytePageSize,
        final int           flashNumberOfPlanes,
        final int           flashNumberOfLockRegions,
        final long          sramAppletStartAddress,
        final long          sramAppletStackAddress,
        final long          regBase,
        final boolean       canBrownout,
        final boolean       canBootFlash
    )
    {
        // Call the superclass constructor
        super(
            instProgBootSAMBA     ,
            flashBaseAddress      , flashNumberOfPages    , flashBytePageSize, flashNumberOfPlanes, flashNumberOfLockRegions,
            sramAppletStartAddress, sramAppletStackAddress,
            regBase               ,
            canBrownout           , canBootFlash
        );

        /* SAM3 Errata (EEFCn_FMR.FWS must be 6)
         *
         * https://ww1.microchip.com/downloads/en/DeviceDoc/Atmel-11057-32-bit-Cortex-M3-Microcontroller-SAM3X-SAM3A_Datasheet.pdf
         *
         *     When writing data to the Flash memory plane the data may not be written correctly.
         *     To ensure proper programming, set the number of Wait States (WS) to 6 (i.e., FWS = 6)
         *     during the write operation.
         */
                             _samba._writeU32( EEFC0_FMR(_regBase), 0x06 << 8 );
        if(_flashPlanes > 1) _samba._writeU32( EEFC1_FMR(_regBase), 0x06 << 8 );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _waitFSR_timeoutMS(final int timeoutMS)
    {
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(timeoutMS);

        long fsr0 = 0x00;
        long fsr1 = 0x01;

        while( !tms.timeout() ) {

                fsr0 = _samba._readU32( EEFC0_FSR(_regBase) );
                if( (fsr0 & 0x02) != 0 ) { SysUtil.stdDbg().println("EEFC0_FSR.FCMDE" ); return false; } // Command error
                if( (fsr0 & 0x04) != 0 ) { SysUtil.stdDbg().println("EEFC0_FSR.FLOCKE"); return false; } // Lock error

            if(_flashPlanes > 1) {
                fsr1 = _samba._readU32( EEFC1_FSR(_regBase) );
                if( (fsr1 & 0x02) != 0 ) { SysUtil.stdDbg().println("EEFC1_FSR.FCMDE" ); return false; } // Command error
                if( (fsr1 & 0x04) != 0 ) { SysUtil.stdDbg().println("EEFC1_FSR.FLOCKE"); return false; } // Lock error
            }

            if( (fsr0 & fsr1 & 0x01) != 0 ) return true;

        } // while

        SysUtil.stdDbg().println("EEFCn_FSR : TIMEOUT");

        return false; // Timeout
    }

    private boolean _waitFSR_timeout01S() { return _waitFSR_timeoutMS( 1000); }
    private boolean _waitFSR_timeout02S() { return _waitFSR_timeoutMS( 2000); }
    private boolean _waitFSR_timeout32S() { return _waitFSR_timeoutMS(32000); }

    private void _writeFCR0(final long cmd, final long arg)
    { _samba._writeU32( EEFC0_FCR(_regBase), (EEFC_KEY << 24) | (arg << 8) | cmd ); }

    private void _writeFCR1(final long cmd, final long arg)
    { _samba._writeU32( EEFC1_FCR(_regBase), (EEFC_KEY << 24) | (arg << 8) | cmd ); }

    private long _readFRR0()
    { return _samba._readU32( EEFC0_FRR(_regBase) ); }

    private long _readFRR1()
    { return _samba._readU32( EEFC1_FRR(_regBase) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean eraseAll(final long startOffset)
    {
        // The start offset must be on an erase page boundary
        final int PagesPerErase = 8;

        if( ( startOffset % (_flashPageSize * PagesPerErase) ) != 0 ) return false;

        // Erase each PagesPerErase set of pages
        if( !_waitFSR_timeout01S() ) return false;

        final int startPage     = (int) (startOffset / _flashPageSize);
        final int pagesPerPlane = _flashPages / 2;

        for(int pageNum = startPage; pageNum < _flashPages; pageNum += PagesPerErase) {

            if(_flashPlanes == 1 || pageNum < pagesPerPlane) _writeFCR0( EEFC_FCMD_EPA, ( (pageNum                ) << 0 ) | 0x01 );
            else                                             _writeFCR1( EEFC_FCMD_EPA, ( (pageNum % pagesPerPlane) << 0 ) | 0x01 );

            if( !_waitFSR_timeout02S() ) {
                // In case of error, try EEFC_FCMD_EA if both 'startOffset' and 'pageNum' are zero
                if(startOffset == 0 && pageNum == 0) {
                                         _writeFCR0(EEFC_FCMD_EA, 0);
                    if(_flashPlanes > 1) _writeFCR1(EEFC_FCMD_EA, 0);
                    return _waitFSR_timeout32S();
                }
                else {
                    return false;
                }
            }

        } // for

        // Done
        return true;
    }

    @Override
    public boolean writePage(final int pageNum, final boolean eraseBeforeWrite)
    {
        if(pageNum >= _flashPages) return false;

        if( !_samba._applet_setSrcAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;
        if( !_samba._applet_setDstAddr(_flashBase + pageNum * _flashPageSize        ) ) return false;
        _pageBufferASel = !_pageBufferASel;

        if( !_waitFSR_timeout01S() ) return false;

        if( !_samba._applet_runT2() ) return false;

        if(_flashPlanes > 1 && pageNum >= _flashPages / 2) _writeFCR1(eraseBeforeWrite ? EEFC_FCMD_EWP : EEFC_FCMD_WP, pageNum - _flashPages / 2);
        else                                               _writeFCR0(eraseBeforeWrite ? EEFC_FCMD_EWP : EEFC_FCMD_WP, pageNum                  );

        if( !_waitFSR_timeout02S() ) return false;

        return true;
    }

    @Override
    public boolean readPage(final int pageNum, final int[] buffer)
    {
        if(pageNum >= _flashPages) return false;

        if( !_samba._applet_setSrcAddr(_flashBase + pageNum * _flashPageSize        ) ) return false;
        if( !_samba._applet_setDstAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;

        if( !_waitFSR_timeout01S() ) return false;

        if( !_samba._applet_runT2() ) return false;

        return _samba._readBuff(_pageBufferASel ? _pageBufferA : _pageBufferB, buffer, _flashPageSize);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### !!! TODO : Add more features (security bit, custom option bits, lock regions, etc.) !!! #####

    private boolean _modOptionBit(final String desc, final long bit, final boolean set)
    {
        // Print message
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMMod("[SAM-BA]", 9, "GPNVM." + desc, set) );

        // Set the bit
        if( !_waitFSR_timeout01S() ) return false;

        _writeFCR0(set ? EEFC_FCMD_SGPB : EEFC_FCMD_CGPB, bit);

        if( !_waitFSR_timeout01S() ) return false;

        // Done
        return true;
    }

    private boolean _setOptionBit(final String desc, final long bit)
    { return _modOptionBit(desc, bit, true ); }

    private boolean _clrOptionBit(final String desc, final long bit)
    { return _modOptionBit(desc, bit, false); }

    public boolean writeOptions()
    {
        // NOTE : This class will never set the 'Security' bit and lock region(s)!

        // Read the GPNVM bits
        if( !_waitFSR_timeout01S() ) return false;
        _writeFCR0(EEFC_FCMD_GGPB, 0);
        if( !_waitFSR_timeout01S() ) return false;

        final long gpnvm = _readFRR0();

        /*
         * Parse the GPNVM bits
         *
         * SAM3X / SAM3A Series
         * https://ww1.microchip.com/downloads/en/DeviceDoc/Atmel-11057-32-bit-Cortex-M3-Microcontroller-SAM3X-SAM3A_Datasheet.pdf
         *
         * SAM9XE Series
         * https://ww1.microchip.com/downloads/en/DeviceDoc/Atmel-6254-32-bit-ARM926EJ-S-Embedded-Microprocessor-SAM9XE_Datasheet.pdf
         */
        final long BIT_SEC =                                 0          ; // Security
        final long BIT_BOD = _canBrownout  ?                 1      : -1; // Brownout Detector Enable
        final long BIT_BOR = _canBrownout  ?                 2      : -1; // Brownout Reset    Enable
        final long BIT_BMS = _canBootFlash ? (_canBrownout ? 3 : 1) : -1; // Boot Mode Select

        final long MSK_SEC = (BIT_SEC >= 0) ? (1 << BIT_SEC) : 0;
        final long MSK_BOD = (BIT_BOD >= 0) ? (1 << BIT_BOD) : 0;
        final long MSK_BOR = (BIT_BOR >= 0) ? (1 << BIT_BOR) : 0;
        final long MSK_BMS = (BIT_BMS >= 0) ? (1 << BIT_BMS) : 0;

        final boolean bitSEC = (gpnvm & MSK_SEC) != 0;
        final boolean bitBOD = (gpnvm & MSK_BOD) != 0;
        final boolean bitBOR = (gpnvm & MSK_BOR) != 0;
        final boolean bitBMS = (gpnvm & MSK_BMS) != 0;

        // Set the default option bits
        if(BIT_BOD >= 0 && !bitBOD) { if( !_setOptionBit("BOD", BIT_BOD) ) return false; }
        if(BIT_BOR >= 0 && !bitBOR) { if( !_setOptionBit("BOR", BIT_BOR) ) return false; }
        if(BIT_BMS >= 0 && !bitBMS) { if( !_setOptionBit("BMS", BIT_BMS) ) return false; }

        // Done
        return true;
    }

} // class FlashEEFC


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
