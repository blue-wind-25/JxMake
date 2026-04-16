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


// ##### !!! TODO : VERIFY !!! #####

public static class FlashEFC extends FlashOper {

    // NOTE : This class was adapted from the BOSSA source code

    private static final long EFC0_FMR      = 0xFFFFFF60L;
    private static final long EFC0_FCR      = 0xFFFFFF64L;
    private static final long EFC0_FSR      = 0xFFFFFF68L;

    private static final long EFC1_FMR      = 0xFFFFFF70L;
    private static final long EFC1_FCR      = 0xFFFFFF74L;
    private static final long EFC1_FSR      = 0xFFFFFF78L;

    private static final long EFC_KEY       = 0x5A;

    private static final long EFC_FCMD_WP   = 0x01;
    private static final long EFC_FCMD_SLB  = 0x02;
    private static final long EFC_FCMD_WPL  = 0x03;
    private static final long EFC_FCMD_CLB  = 0x04;
    private static final long EFC_FCMD_EA   = 0x08;
    private static final long EFC_FCMD_SGPB = 0x0B;
    private static final long EFC_FCMD_CGPB = 0x0D;
    private static final long EFC_FCMD_SSB  = 0x0F;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FlashEFC(
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
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void _writeFCR0(final long cmd, final long arg)
    { _samba._writeU32( EFC0_FCR, (EFC_KEY << 24) | (arg << 8) | cmd ); }

    private void _writeFCR1(final long cmd, final long arg)
    { _samba._writeU32( EFC1_FCR, (EFC_KEY << 24) | (arg << 8) | cmd ); }

    private long _readFSR0()
    { return _samba._readU32(EFC0_FSR); }

    private long _readFSR1()
    { return _samba._readU32(EFC1_FSR); }

    private boolean _waitFSR_timeoutMS(final int timeoutMS)
    {
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(timeoutMS);

        long fsr0 = 0x00;
        long fsr1 = 0x01;

        while( !tms.timeout() ) {

                fsr0 = _readFSR0();
                if( (fsr0 & 0x08) != 0 ) { SysUtil.stdDbg().println("EFC0_FSR.PROGE"); return false; } // Program error
                if( (fsr0 & 0x04) != 0 ) { SysUtil.stdDbg().println("EFC0_FSR.LOCKE"); return false; } // Lock error

            if(_flashPlanes > 1) {
                fsr1 = _readFSR1();
                if( (fsr1 & 0x08) != 0 ) { SysUtil.stdDbg().println("EFC1_FSR.PROGE"); return false; } // Program error
                if( (fsr1 & 0x04) != 0 ) { SysUtil.stdDbg().println("EFC1_FSR.LOCKE"); return false; } // Lock error
            }

            if( (fsr0 & fsr1 & 0x01) != 0 ) return true;

        } // while

        SysUtil.stdDbg().println("EFCn_FSR : TIMEOUT");

        return false; // Timeout
    }

    private boolean _waitFSR_timeout01S() { return _waitFSR_timeoutMS( 1000); }
    private boolean _waitFSR_timeout02S() { return _waitFSR_timeoutMS( 2000); }
    private boolean _waitFSR_timeout32S() { return _waitFSR_timeoutMS(32000); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean eraseAll(final long startOffset)
    {
        // The start offset must be zero
        if(startOffset != 0) return false;

        if( !_waitFSR_timeout01S() ) return false;

                             _writeFCR0(EFC_FCMD_EA, 0              );
        if(_flashPlanes > 1) _writeFCR1(EFC_FCMD_EA, _flashPages / 2);

        return _waitFSR_timeout32S();
    }

    @Override
    public boolean writePage(final int pageNum, final boolean eraseBeforeWrite)
    {
        if(pageNum >= _flashPages) return false;

        if(true) {
           if( !_waitFSR_timeout01S() ) return false;

           long fmr0 = _samba._readU32(EFC0_FMR);
           long fmr1 = _samba._readU32(EFC1_FMR);

           if(eraseBeforeWrite) { fmr0 &= ~(1L << 7); fmr1 &= ~(1L << 7); }
           else                 { fmr0 |=  (1L << 7); fmr1 |=  (1L << 7); }

           _samba._writeU32(EFC0_FMR, fmr0);
           _samba._writeU32(EFC1_FMR, fmr1);
        }

        if( !_samba._applet_setSrcAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;
        if( !_samba._applet_setDstAddr(_flashBase + pageNum * _flashPageSize        ) ) return false;
        _pageBufferASel = !_pageBufferASel;

        if( !_waitFSR_timeout01S() ) return false;

        if( !_samba._applet_runT1() ) return false;

        if(_flashPlanes > 1 && pageNum >= _flashPages / 2) _writeFCR1(EFC_FCMD_WP, pageNum - _flashPages / 2);
        else                                               _writeFCR0(EFC_FCMD_WP, pageNum                  );

        if( !_waitFSR_timeout02S() ) return false;

        return true;
    }

    @Override
    public boolean readPage(final int pageNum, final int[] buffer)
    {
        if(pageNum >= _flashPages) return false;

        if( !_waitFSR_timeout01S() ) return false;

        return _samba._readBuff(_flashBase + pageNum * _flashPageSize, buffer, _flashPageSize);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### !!! TODO : Add more features (security bit, custom option bits, lock regions, etc.) !!! #####

    private boolean _modOptionBit(final String desc, final long bit, final boolean set)
    {
        // Print message
        SysUtil.stdDbg().printf( Texts.ProgXXX_InfoBLMMod("[SAM-BA]", 9, "GPNVM." + desc, set) );

        // Set the bit
        if( !_waitFSR_timeout01S() ) return false;

        _writeFCR0(set ? EFC_FCMD_SGPB : EFC_FCMD_CGPB, bit);

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
        final long gpnvm = _readFSR0();

        /*
         * Parse the GPNVM bits
         *
         * SAM7S Series
         * https://ww1.microchip.com/downloads/en/DeviceDoc/doc6175.pdf
         *
         * SAM7SE Series
         * https://ww1.microchip.com/downloads/en/DeviceDoc/doc6222.pdf
         */
        final long BIT_SEC =                  4     ; // Security
        final long BIT_BOD = _canBrownout  ?  8 : -1; // Brownout Detector Enable
        final long BIT_BOR = _canBrownout  ?  9 : -1; // Brownout Reset    Enable
        final long BIT_BMS = _canBootFlash ? 10 : -1; // Boot Mode Select

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

} // class FlashEFC


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
