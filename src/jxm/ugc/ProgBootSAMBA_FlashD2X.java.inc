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


public static class FlashD2X extends FlashOper {

    // NOTE : This class was adapted from the BOSSA source code

    private static final int  NVM_REG_CTRLA              = 0x00; // Offset from '_regBase'
    private static final int  NVM_REG_CTRLB              = 0x04; // ---
    private static final int  NVM_REG_INTFLAG            = 0x14; // ---
    private static final int  NVM_REG_STATUS             = 0x18; // ---
    private static final int  NVM_REG_ADDR               = 0x1C; // ---
    private static final int  NVM_REG_LOCK               = 0x20; // ---

    private static final int  CMDEX_KEY                  = 0xA500;

    private static final int  NVM_INT_STATUS_READY_MASK  = 0x0001;
    private static final int  NVM_INT_STATUS_ERROR_MASK  = 0x0002;
    private static final int  NVM_CTRL_STATUS_MASK       = 0xFFEB;

    private static final int  NVM_CMD_ER                 = 0x02;
    private static final int  NVM_CMD_WP                 = 0x04;
    private static final int  NVM_CMD_EAR                = 0x05;
    private static final int  NVM_CMD_WAP                = 0x06;
    private static final int  NVM_CMD_LR                 = 0x40;
    private static final int  NVM_CMD_UR                 = 0x41;
    private static final int  NVM_CMD_SSB                = 0x45;
    private static final int  NVM_CMD_PBC                = 0x44;

    private static final int  ERASE_ROW_PAGES            = 4;

    private static final long NVM_UR_ADDR                = 0x00804000L;
    private        final int  NVM_UR_SIZE                             ;
    private static final int  NVM_UR_BOD33_ENABLE_OFFSET = 0x01;
    private static final int  NVM_UR_BOD33_ENABLE_MASK   = 0x06;
    private static final int  NVM_UR_BOD33_RESET_OFFSET  = 0x01;
    private static final int  NVM_UR_BOD33_RESET_MASK    = 0x07;
    private static final int  NVM_UR_NVM_LOCK_OFFSET     = 0x06;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FlashD2X(
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

        // Calculate the USER_ROW size
        NVM_UR_SIZE = flashBytePageSize * ERASE_ROW_PAGES;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private long _readReg(final int regOfs)
    { return _samba._readU32(_regBase + regOfs); }

    private void _writeReg(final int regOfs, final long value)
    { _samba._writeU32(_regBase + regOfs, value); }

    private boolean _waitReady()
    {
        while(true) {

            final long res = _readReg(NVM_REG_INTFLAG);
            if(res < 0) return false;

            if( (res & NVM_INT_STATUS_READY_MASK) != 0 ) break;
        }

        return true;
    }

    private boolean _execCmd(final int cmd)
    {
        if( !_waitReady() ) return false;
        _writeReg(NVM_REG_CTRLA, CMDEX_KEY | cmd);
        if( !_waitReady() ) return false;

        final long res = _readReg(NVM_REG_INTFLAG);
        if(res < 0) return false;

        if( (res & NVM_INT_STATUS_ERROR_MASK) != 0 ) {
            _writeReg(NVM_REG_INTFLAG, NVM_INT_STATUS_ERROR_MASK); // Clear the error bit
            return false;
        }

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _inBLArea(final long start, final int size)
    {
        final long blStart = _samba._configAppletInfo().flashBLStart;
        final int  blSize  = _samba._configAppletInfo().flashBLSize;

        if(blStart < 0 || blSize <= 0) return false;

        return (start >= blStart) && ( (start + size) <= (blStart + blSize) );
    }

    private boolean _erase_impl(final long start_, final long size_)
    {
        // Check the start and size
        final int blockSize = _flashPageSize * ERASE_ROW_PAGES;

        if( (start_ % blockSize) != 0 ) return false;

        if( (start_ + size_) > _samba._configMemoryFlash().totalSize ) return false;

        // Trim leading and trailing blocks that fall inside bootloader area
        long start = start_;
        long size  = size_;

        while( size > 0 && _inBLArea(start, blockSize) ) {
            start += blockSize;
            size  -= blockSize;
        }

        while( size > 0 && _inBLArea(start + size - blockSize, blockSize) ) {
            size -= blockSize;
        }

        if(size <= 0) return true; // Nothing left to erase - skip silently

        // Perform erase
        final long endBlock = (start + size + blockSize - 1) / blockSize;

        if( !_waitReady() ) return false;

        for(long eraseBlock = start / blockSize; eraseBlock < endBlock; ++eraseBlock) {

            // Clear the error bits
            final long res = _readReg(NVM_REG_STATUS);
            if(res < 0) return false;

            _writeReg(NVM_REG_STATUS, NVM_CTRL_STATUS_MASK);

            // Issue erase command
            _writeReg( NVM_REG_ADDR, (eraseBlock * blockSize) / 2 );
            if( !_execCmd(NVM_CMD_ER) ) return false;

            if( !_waitReady() ) return false;

        } // for

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean eraseAll(final long startOffset)
    { return _erase_impl( startOffset, _samba._configMemoryFlash().totalSize - startOffset ); }

    @Override
    public boolean writePage(final int pageNum, final boolean eraseBeforeWrite)
    {
        if(pageNum >= _flashPages) return false;

        // Set the CACHEDIS and MANW bits (disable cache and enable manual page write)
        final long resCTRLB = _readReg(NVM_REG_CTRLB);
        if(resCTRLB < 0) return false;

        _writeReg( NVM_REG_CTRLB, resCTRLB | (1L << 18) | (1L << 7) );

        // Perform erase if writing at the start of the erase page
        if(eraseBeforeWrite && pageNum % ERASE_ROW_PAGES == 0) {
            if( !_erase_impl(pageNum * _flashPageSize, ERASE_ROW_PAGES * _flashPageSize) ) return false;
        }

        // Clear page buffer
        if( !_execCmd(NVM_CMD_PBC) ) return false;

        // Calculate and check the address
        final long dstAddr = _flashBase + (pageNum * _flashPageSize);

        if( _inBLArea(dstAddr, _flashPageSize) ) return true; // Skip silently

        // Perform the write
        if( !_samba._applet_setSrcAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;
        if( !_samba._applet_setDstAddr(dstAddr                                      ) ) return false;
        _pageBufferASel = !_pageBufferASel;

        if( !_waitReady() ) return false;

        if( !_samba._applet_runT2() ) return false;

        _writeReg(NVM_REG_ADDR, dstAddr / 2);
        if( !_execCmd(NVM_CMD_WP) ) return false;

        // Done
        return true;
    }

    @Override
    public boolean readPage(final int pageNum, final int[] buffer)
    {
        if(pageNum >= _flashPages) return false;

        return _samba._readBuff( _flashBase + (pageNum * _flashPageSize), buffer, _flashPageSize );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### !!! TODO : Add more features (security bit, custom option bits, lock regions, etc.) !!! #####

    public boolean writeOptions()
    {
        // NOTE : # This class will never set the 'Security' bit and lock region(s)!
        //        # The '_canBootFlash' flag is unused in this class.

        // Read the entire USER_ROW
        final int[] orgUserRow = new int[NVM_UR_SIZE];

        if( !_samba._readBuff(NVM_UR_ADDR, orgUserRow) ) return false;

        /* Modify the USER_ROW (set the default option bits)
         *
         * SAM D21/DA1 Family
         * https://ww1.microchip.com/downloads/aemDocuments/documents/MCU32/ProductDocuments/DataSheets/SAM-D21-DA1-Family-Data-Sheet-DS40001882.pdf
         *
         * SAM R21E/R21G
         * https://ww1.microchip.com/downloads/en/DeviceDoc/SAM-R21_Datasheet.pdf
         */
        final int[] modUserRow = Arrays.copyOf(orgUserRow, orgUserRow.length);

        if(_canBrownout) {
            modUserRow[NVM_UR_BOD33_ENABLE_OFFSET] |= NVM_UR_BOD33_ENABLE_MASK; // Enable BOD
            modUserRow[NVM_UR_BOD33_RESET_OFFSET ] |= NVM_UR_BOD33_RESET_MASK ; // Enable BOR
        }

        if( Arrays.equals(modUserRow, orgUserRow) ) return true;

        // Write the USER_ROW
        if(true) {

            // Set the CACHEDIS and MANW bits (disable cache and enable manual page write)
            final long resCTRLB = _readReg(NVM_REG_CTRLB);
            if(resCTRLB < 0) return false;

            _writeReg( NVM_REG_CTRLB, resCTRLB | (1L << 18) | (1L << 7) );

            // Erase user row
            _writeReg(NVM_REG_ADDR, NVM_UR_ADDR / 2);
            if( !_execCmd(NVM_CMD_EAR) ) return false;

            for(int ofs = 0; ofs < NVM_UR_SIZE; ofs += _flashPageSize) {

                // Load the buffer with the chunk
                final int[] slice = Arrays.copyOfRange(modUserRow, ofs, ofs + _flashPageSize);
                if( !loadBuffer(slice) ) return false;

                // Clear page buffer
                if( !_execCmd(NVM_CMD_PBC) ) return false;

                // Perform the write
                if( !_samba._applet_setSrcAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;
                if( !_samba._applet_setDstAddr(NVM_UR_ADDR + ofs                            ) ) return false;
                _pageBufferASel = !_pageBufferASel;

                if( !_waitReady() ) return false;

                if( !_samba._applet_runT2() ) return false;

                _writeReg( NVM_REG_ADDR, (NVM_UR_ADDR + ofs) / 2 );
                if( !_execCmd(NVM_CMD_WAP) ) return false;

            } // for

        } // if

        // Done
        return true;
    }

} // class FlashD2X


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
