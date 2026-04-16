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

public static class FlashD5X extends FlashOper {

    // NOTE : This class was adapted from the BOSSA source code

    private static final int  NVM_REG_CTRLA               = 0x00; // Offset from '_regBase'
    private static final int  NVM_REG_CTRLB               = 0x04; // ---
    private static final int  NVM_REG_INTFLAG             = 0x10; // ---
    private static final int  NVM_REG_STATUS              = 0x12; // ---
    private static final int  NVM_REG_ADDR                = 0x14; // ---
    private static final int  NVM_REG_RUNLOCK             = 0x18; // ---

    private static final int  CMDEX_KEY                   = 0xA500;

    private static final int  NVM_INT_STATUS_READY_MASK   = 0x0001;
    private static final int  NVM_INT_STATUS_ERROR_MASK   = 0x00CE;

    private static final int  NVM_CMD_EP                  = 0x00;
    private static final int  NVM_CMD_EB                  = 0x01;
    private static final int  NVM_CMD_WP                  = 0x03;
    private static final int  NVM_CMD_WQW                 = 0x04;
    private static final int  NVM_CMD_LR                  = 0x11;
    private static final int  NVM_CMD_UR                  = 0x12;
    private static final int  NVM_CMD_SSB                 = 0x16;
    private static final int  NVM_CMD_PBC                 = 0x15;

    private static final int  ERASE_BLOCK_PAGES           = 16;

    private static final long NVM_UP_ADDR                 = 0x00804000L;
    private        final int  NVM_UP_SIZE                              ;
    private static final int  NVM_UP_BOD33_DISABLE_OFFSET = 0x00;
    private static final int  NVM_UP_BOD33_DISABLE_MASK   = 0x01;
    private static final int  NVM_UP_BOD33_RESET_OFFSET   = 0x01;
    private static final int  NVM_UP_BOD33_RESET_MASK     = 0x02;
    private static final int  NVM_UP_NVM_LOCK_OFFSET      = 0x08;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FlashD5X(
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

        // Set the USER_PAGE size
        NVM_UP_SIZE = flashBytePageSize;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _readRegU16(final int regOfs)
    {
        if(!true) {
            return _samba._readU16(_regBase + regOfs);
        }
        else {
            return _samba._readU08(_regBase + regOfs) | ( _samba._readU08(_regBase + regOfs + 1) << 8 );
        }
    }

    private void _writeRegU16(final int regOfs, final int value)
    {
        if(!true) {
            _samba._writeU16(_regBase + regOfs, value);
        }
        else {
            _samba._writeU08( _regBase + regOfs    ,  value       & 0xFF );
            _samba._writeU08( _regBase + regOfs + 1, (value >> 8) & 0xFF );
        }
    }

    private long _readRegU32(final int regOfs)
    { return _samba._readU32(_regBase + regOfs); }

    private void _writeRegU32(final int regOfs, final long value)
    { _samba._writeU32(_regBase + regOfs, value); }

    private boolean _waitReady()
    {
        while(true) {

            final int res = _readRegU16(NVM_REG_STATUS);
            if(res < 0) return false;

            if( (res & NVM_INT_STATUS_READY_MASK) != 0 ) break;
        }

        return true;
    }

    private boolean _execCmd(final int cmd)
    {
        if( !_waitReady() ) return false;
        _writeRegU32(NVM_REG_CTRLB, CMDEX_KEY | cmd);
        if( !_waitReady() ) return false;

        final int res = _readRegU16(NVM_REG_INTFLAG);
        if(res < 0) return false;

        if( (res & NVM_INT_STATUS_ERROR_MASK) != 0 ) {
            _writeRegU16(NVM_REG_INTFLAG, NVM_INT_STATUS_ERROR_MASK); // Clear the error bit
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
        final int blockSize = _flashPageSize * ERASE_BLOCK_PAGES;

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

            // Issue erase command
            _writeRegU32(NVM_REG_ADDR, eraseBlock * blockSize);
            if( !_execCmd(NVM_CMD_EB) ) return false;

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

        // Set the CACHEDIS[1:0] and clear the WMODE[1:0] bits (disable cache and enable manual page write)
        final int resCTRLA = _readRegU16(NVM_REG_CTRLA);
        if(resCTRLA < 0) return false;

        _writeRegU16( NVM_REG_CTRLA, ( resCTRLA | (0x3 << 14) ) & 0xFFCF );

        // Perform erase if writing at the start of the erase page
        if(eraseBeforeWrite && pageNum % ERASE_BLOCK_PAGES == 0) {
            if( !_erase_impl(pageNum * _flashPageSize, ERASE_BLOCK_PAGES * _flashPageSize) ) return false;
        }

        // Clear page buffer
        if( !_execCmd(NVM_CMD_PBC) ) return false;

        // Calculate and check the address
        final long dstAddr = _flashBase + (pageNum * _flashPageSize);

        if( _inBLArea(dstAddr, _flashPageSize) ) return true; // Skip silently

        // Perform the write
        if( !_samba._applet_setSrcAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;
        if( !_samba._applet_setDstAddr(dstAddr                                      ) ) return false;
        if( !_samba._applet_setWords  (_flashPageSize / 4                           ) ) return false;
        _pageBufferASel = !_pageBufferASel;

        if( !_waitReady() ) return false;

        if( !_samba._applet_runT2() ) return false;

        _writeRegU32(NVM_REG_ADDR, dstAddr);
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

        // Read the entire USER_PAGE
        final int[] orgUserPage = new int[NVM_UP_SIZE];

        if( !_samba._readBuff(NVM_UP_ADDR, orgUserPage) ) return false;

        /* Modify the USER_PAGE (set the default option bits)
         *
         * SAM D5x/E5x Family
         * https://ww1.microchip.com/downloads/aemDocuments/documents/MCU32/ProductDocuments/DataSheets/SAM-D5x-E5x-Family-Data-Sheet-DS60001507.pdf
         */
        final int[] modUserPage = Arrays.copyOf(orgUserPage, orgUserPage.length);

        if(_canBrownout) {
            modUserPage[NVM_UP_BOD33_DISABLE_OFFSET] &= ~NVM_UP_BOD33_DISABLE_MASK; // Enable BOD
            modUserPage[NVM_UP_BOD33_RESET_OFFSET  ] |=  NVM_UP_BOD33_RESET_MASK  ; // Enable BOR
        }

        if( Arrays.equals(modUserPage, orgUserPage) ) return true;

        // Write the USER_PAGE
        if(true) {

            // Set the CACHEDIS[1:0] and clear the WMODE[1:0] bits (disable cache and enable manual page write)
            final int resCTRLA = _readRegU16(NVM_REG_CTRLA);
            if(resCTRLA < 0) return false;

            _writeRegU16( NVM_REG_CTRLA, ( resCTRLA | (0x3 << 14) ) & 0xFFCF );

            // Erase user page
            _writeRegU32(NVM_REG_ADDR, NVM_UP_ADDR);
            if( !_execCmd(NVM_CMD_EP) ) return false;

            for(int ofs = 0; ofs < NVM_UP_SIZE; ofs += 16) {

                // Load the buffer with the chunk
                final int[] slice = Arrays.copyOfRange(modUserPage, ofs, ofs + 16);
                if( !loadBuffer(slice) ) return false;

                // Clear page buffer
                if( !_execCmd(NVM_CMD_PBC) ) return false;

                // Perform the write
                if( !_samba._applet_setSrcAddr(_pageBufferASel ? _pageBufferA : _pageBufferB) ) return false;
                if( !_samba._applet_setDstAddr(NVM_UP_ADDR + ofs                            ) ) return false;
                if( !_samba._applet_setWords  (4                                            ) ) return false;
                _pageBufferASel = !_pageBufferASel;

                if( !_waitReady() ) return false;

                if( !_samba._applet_runT2() ) return false;

                _writeRegU32(NVM_REG_ADDR, NVM_UP_ADDR + ofs);
                if( !_execCmd(NVM_CMD_WQW) ) return false;

            } // for

        } // if

        // Done
        return true;
    }

} // class FlashD5X


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
