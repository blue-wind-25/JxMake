/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.Serializable;
import java.util.Arrays;
import java.util.ArrayList;

import jxm.*;
import jxm.annotation.*;
import jxm.tool.fwc.*;
import jxm.xb.*;

import static jxm.ugc.USB2GPIO.IEVal;


/*
 * Please refer to the comment block before the 'ProgPIC' class definition in the 'ProgPIC.java' file for more details and information.
 */
public class ProgDSPIC33 extends ProgPIC {

    @SuppressWarnings("serial")
    public static class Config extends ProgPIC.Config {

        // JxMake use a special field name for serial version UID
        @DataFormat.Hex16 public static final long __0_JxMake_SerialVersionUID__ = SysUtil.extSerialVersionUID(0x00000001);

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final ProgPIC_SIX_REGOUT.MemoryPE memoryPE = new ProgPIC_SIX_REGOUT.MemoryPE();
        public final ProgPIC_EICSP     .CmdPE    cmdPE    = new ProgPIC_EICSP     .CmdPE   ();

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public Config(final SubPart subPart, final Mode mode)
        {
            // Process the superclass
            super( new BaseProgSpec() );

            // Set the standard configuration values
            baseProgSpec.part     = Part.dsPIC33;
            baseProgSpec.subPart  = subPart;
            baseProgSpec.mode     = mode;
            baseProgSpec.entrySeq = 0x4D434851L; // NOTE : Do not use EICSP by default
        }

        public Config(final SubPart subPart)
        { this(subPart, Mode.Default); }

        public Config(final Mode mode)
        { this(SubPart.F, mode); }

        public Config()
        { this(SubPart.F, Mode.Default); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////////////////
// NOTE : Do not forget to update '../../../../docs/txt/en_US/99-Appendix-X_Built-In-Function-Parameters.txt' (and its translations) when adding entries here!

// ##### ??? TODO : Add more specific-part 'dsPIC33*()' functions ??? #####


public static Config dsPIC33EPxxGS202(final int flashBSize, final int cwAStart)
{
    // Instantiate the configuration object
    final Config config = new Config(SubPart.EP, Mode.LVEntrySeqS);

    // Set the specification
    /* NOTE: #  ICSP erases the entire device at once and writes  2 words at once.
     *       # EICSP erases 512 words at once         and writes 64 words at once.
     *
     * ##### ??? TODO : Exclude the last page from this specifier because it contains the configuration bits ??? #####
     */
    config.memoryFlash.totalSize          = flashBSize * 3;
    config.memoryFlash.writeBlockSize     =   2        * 3; // NOTE : In  ICSP mode, dsPIC33 writes 2 words at once (6 bytes)
    config.memoryFlash.eraseBlockSize     =   2        * 3; // NOTE : In  ICSP mode, it does not matter as long as it is multiple of 'writeBlockSize' and smaller than the 'totalSize'
    config.memoryFlash.writeBlockSizeE    =  64        * 3; // NOTE : In EICSP mode, dsPIC33 writes  64 words at once ( 192 bytes)
    config.memoryFlash.eraseBlockSizeE    = 512        * 3; // NOTE : In EICSP mode, dsPIC33 erases 512 words at once (1536 bytes)

    config.memoryEEPROM.totalSize         = 0;

    config.memoryConfigBytes.addressBeg   = cwAStart;
    config.memoryConfigBytes.addressEnd   = config.memoryConfigBytes.addressBeg + 64 * 2;

    /*
     * NOTE : # The configuration bits of this dsPIC33 series are implemented as volatile memory.
     *          Each time the device is powered up, they will be loaded from a location at the end
     *          of the program memory space.
     *        # However, in the HEX file, the data are located at a DIFFERENT ADDRESS (higher than
     *          the end address of the program memory space. Therefore, they will NOT be programmed
     *          simultaneously when programming the flash.
     *
     * ##### ??? TODO : Is there a better method ??? #####
     */
    config.memoryConfigBytes.address       = new long[] { cwAStart + 0x00   , cwAStart + 0x10   , cwAStart + 0x14   , cwAStart + 0x18   , cwAStart + 0x1C   , cwAStart + 0x20   , cwAStart + 0x24   , cwAStart + 0x28   , cwAStart + 0x2C   , cwAStart + 0x30    };
    config.memoryConfigBytes.size          = new int [] { 2                 , 2                 , 2                 , 2                 , 2                 , 2                 , 2                 , 2                 , 2                 , 2                  };
    config.memoryConfigBytes.bitMask       = new long[] { 0b1000111111101111, 0b0001111111111111, 0b1000000000000000, 0b0000000010000111, 0b0000000111100111, 0b0000001111111111, 0b0000000000000001, 0b0000000010100011, 0b0000000000000101, 0b0000000001110111 };
    config.memoryConfigBytes.clrMask       = new long[] { 0b0000000000000000, 0b0000000000000000, 0b1000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000 };
    config.memoryConfigBytes.setMask       = new long[] { 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0000000000000001, 0b0000000010000000, 0b0000000000000100, 0b0000000000000000 };
    config.memoryConfigBytes.maxTotalSize  = 64 * 3;  // NOTE : The configuration words are not contiguous within the address space!
    config.memoryConfigBytes.addressMulFW  = 2;
    config.memoryConfigBytes.addressOfsFW  = 0;

    config.memoryPE.address                = 0x800000;
    config.memoryPE.totalSize              = 1536 * 3;
    config.memoryPE.pageSize               =  512 * 3;
    config.memoryPE.numPages               =  3;
    config.memoryPE.saveWordOffset         = -1;
    config.memoryPE.saveWordCount          =  0;

    config.cmdPE.ERASEP                    = 0x9; // This device needs erase before writing

    config.cmdPE.READD                     = -1;  // Not available in this device
    config.cmdPE.PROGD                     = -1;  // ---

    config.cmdPE.waitDelay_MS_EraseProgMem = 25;
    config.cmdPE.waitDelay_MS_WriteProgMem =  1;

    // Return the configuration object
    return config;
}

public static Config dsPIC33EP16GS202() { return dsPIC33EPxxGS202( 5312 + 320, 0x2B80); }
public static Config dsPIC33EP32GS202() { return dsPIC33EPxxGS202(10944 + 320, 0x5780); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    } // class Config

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private   final ProgPIC_SIX_REGOUT _cs = new ProgPIC_SIX_REGOUT();
    private   final ProgPIC_EICSP      _pe;

    protected final Config             _config33;
    protected final boolean            _eicsp;
    protected       boolean            _eicspInitDone = false;

    @SuppressWarnings("this-escape")
    public ProgDSPIC33(final USB2GPIO usb2gpio, final Config config) throws Exception
    {
        // Process the superclass
        super(usb2gpio, config);

        // Store the objects
        _config33 = (Config) super._config;
        _eicsp    = (_config33.baseProgSpec.entrySeq == 0x4D434850L);

        // Configure the 'ProgPIC_SIX_REGOUT'
        _cs.setC4D24Mode             ( useHardwareAssistedBitBangingSPI() );
        _cs.setResetInternalPCAddress( 0x200, 3, 3                        );

        // Check the configuration values
        if(_config33.baseProgSpec.part    != Part   .dsPIC33    ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSPart, ProgClassName);
        if(_config33.baseProgSpec.subPart != SubPart.EP         ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSSPrt, ProgClassName);
        if(_config33.baseProgSpec.mode    != Mode   .LVEntrySeqS) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSMode, ProgClassName);

        ProgPIC_SIX_REGOUT.checkMemoryPE(_config33.memoryPE, ProgClassName);

        /*
        // EICSP is not supported yet
        if(_eicsp) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSEICSP , ProgClassName);
        //*/

        // Instantiate 'ProgPIC_EICSP' as needed
        // ##### !!! TODO : VERIFY : (_1, _0) or (_0, _0) !!! #####
        _pe = _eicsp ? new ProgPIC_EICSP(this, USB_GPIO.SPIMode._1, USB_GPIO.SPIMode._0) : null;

        if(_pe != null) {
            _pe.adjustFlashBlockSize(_config33); // EICSP may use different flash block sizes
            _pe.setC4D24Mode( useHardwareAssistedBitBangingSPI() );
        }
    }

    // WARNING : In ICSP mode, dsPIC33 memory can only be written in hardware-assisted bit-banging SPI mode!
    @Override
    public boolean useHardwareAssistedBitBangingSPI()
    { return !_eicsp || (_eicsp && !_eicspInitDone); }

    @Override
    public boolean inEICSPMode()
    { return _eicsp; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected USB2GPIO.SPIMode _pic_spiMode()
    { return USB2GPIO.SPIMode._0; }

    @Override
    protected boolean _pic_enterPVM_mcuSpecific_extra()
    {
        // Check for unsupported subparts
        if( _pic33_subPartIsNotSupported() ) return false;

        // Extra delay
        SysUtil.sleepMS(100);

        // EICSP
        if(_eicsp) {

            // Perform sanity check
            final boolean res = _pe.xbSanityCheck();

            if(!res) return false;

            // EICSP intialization done
            // ##### !!! TODO : VERIFY !!! #####
            _eicspInitDone = true;
            _pe.setC4D24Mode( useHardwareAssistedBitBangingSPI() );

            // Done
            return true;

        }

        // ICSP
        else {

            // Send extra 5 clocks
            if( !_usb2gpio.spiXBTransferIgnoreSS( USB2GPIO.IEVal._X, USB2GPIO.IEVal._X, 0, USB2GPIO.IEVal._X, USB2GPIO.IEVal._X, new int[] { 4, 0x00 } ) ) return false;

            // Exit the reset vector
            final int[] entry = (_config33.baseProgSpec.subPart != SubPart.EP)
                              ? new int[] {
                                    0b0000, 0x040200, // GOTO 0x200
                                    0b0000, 0x040200, // GOTO 0x200
                                    0b0000, 0x000000  // NOP
                                }
                              : new int[] {
                                    0b0000, 0x000000, // NOP
                                    0b0000, 0x000000, // NOP
                                    0b0000, 0x000000, // NOP
                                    0b0000, 0x040200, // GOTO 0x200
                                    0b0000, 0x000000, // NOP
                                    0b0000, 0x000000, // NOP
                                    0b0000, 0x000000  // NOP
                              };


            return _pic_xbTransfer_c4_d24(entry);

        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pic33_writeCWMemory(final int address24, final int value16_1, final int opt_value16_2)
    {
             if(_config33.baseProgSpec.subPart == SubPart.AK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CH) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.EP) return _cs.exeCmds_writeCWMemory_dspic33ep(this, address24, value16_1, opt_value16_2);
        else if(_config33.baseProgSpec.subPart == SubPart.FV) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.FJ) return false; // ##### !!! TODO !!! #####
        else                                                  return false; // Invalid sub-part
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pic33_chipErase()
    {
             if(_config33.baseProgSpec.subPart == SubPart.AK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CH) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.EP) return _cs.exeCmds_chipErase_dspic33ep(this);
        else if(_config33.baseProgSpec.subPart == SubPart.FV) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.FJ) return false; // ##### !!! TODO !!! #####
        else                                                  return false; // Invalid sub-part
    }

    private boolean _pic33_peErase()
    {
             if(_config33.baseProgSpec.subPart == SubPart.AK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CH) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.EP) return _cs.exeCmds_peErase_dspic33ep( this, (int) _config33.memoryPE.address, _cs.cadr(_config33.memoryPE.pageSize), _config33.memoryPE.numPages );
        else if(_config33.baseProgSpec.subPart == SubPart.FV) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.FJ) return false; // ##### !!! TODO !!! #####
        else                                                  return false; // Invalid sub-part
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _pic33_readU16MemoryPage(final int address24)
    {
             if(_config33.baseProgSpec.subPart == SubPart.AK) return -1; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CH) return -1; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CK) return -1; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.EP) return _cs.exeCmds_readU16Memory_dspic33ep(this, address24);
        else if(_config33.baseProgSpec.subPart == SubPart.FV) return -1; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.FJ) return -1; // ##### !!! TODO !!! #####
        else                                                  return -1; // Invalid sub-part
    }

    private boolean _pic33_readU16MemoryPage(final int address24, final int[] dstBuff)
    {
             if(_config33.baseProgSpec.subPart == SubPart.AK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CH) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.EP) return _cs.exeCmds_readU16Memory_dspic33ep(this, address24, dstBuff);
        else if(_config33.baseProgSpec.subPart == SubPart.FV) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.FJ) return false; // ##### !!! TODO !!! #####
        else                                                  return false; // Invalid sub-part
    }

    private boolean _pic33_writeU16MemoryPage(final int address24, final int[] srcBuff)
    {
             if(_config33.baseProgSpec.subPart == SubPart.AK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CH) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.EP) return _cs.exeCmds_writeU16MemoryPage_dspic33ep(this, address24, srcBuff);
        else if(_config33.baseProgSpec.subPart == SubPart.FV) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.FJ) return false; // ##### !!! TODO !!! #####
        else                                                  return false; // Invalid sub-part
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pic33_readCodeMemory(final int address24, final int[] dstBuff)
    {
             if(_config33.baseProgSpec.subPart == SubPart.AK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CH) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.EP) return _cs.exeCmds_readCodeMemory_dspic33ep(this, _config33.memoryFlash, address24, dstBuff);
        else if(_config33.baseProgSpec.subPart == SubPart.FV) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.FJ) return false; // ##### !!! TODO !!! #####
        else                                                  return false; // Invalid sub-part
    }

    private boolean _pic33_writeCodeMemory(final boolean firstCall, final int address24, final int[] srcBuff)
    {
             if(_config33.baseProgSpec.subPart == SubPart.AK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CH) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.CK) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.EP) return _cs.exeCmds_writeCodeMemory_dspic33ep(this, _config33.memoryFlash, firstCall, address24, srcBuff);
        else if(_config33.baseProgSpec.subPart == SubPart.FV) return false; // ##### !!! TODO !!! #####
        else if(_config33.baseProgSpec.subPart == SubPart.FJ) return false; // ##### !!! TODO !!! #####
        else                                                  return false; // Invalid sub-part
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pic33_subPartIsNotSupported()
    {
        // For the time being, these subparts are not supported
        return _config33.baseProgSpec.subPart == SubPart.AK ||
               _config33.baseProgSpec.subPart == SubPart.CH || _config33.baseProgSpec.subPart == SubPart.CK ||
               _config33.baseProgSpec.subPart == SubPart.FV || _config33.baseProgSpec.subPart == SubPart.FJ;
    }

    private boolean _pic33_operationIsNotSupported_icsp()
    {
        // Check for unsupported subparts
        if( _pic33_subPartIsNotSupported() ) return true;

        // This operation is not supported in ICSP mode
        if(!_eicsp) return !USB2GPIO.notifyError(Texts.ProgXXX_FailPIC_OperICSP, ProgClassName);

        // The operation is supported in ICSP mode
        return false;
    }

    private boolean _pic33_operationIsNotSupported_eicsp()
    {
        // Check for unsupported subparts
        if( _pic33_subPartIsNotSupported() ) return true;

        // This operation is not supported in EICSP mode
        if(_eicsp) return !USB2GPIO.notifyError(Texts.ProgXXX_FailPIC_OperEICSP, ProgClassName);

        // The operation is supported in EICSP mode
        return false;
    }

    @Override
    protected int _picxx_minSANBAlignSize()
    { return 3; }

    @Override
    protected int _picxx_configByteSize()
    { return 2; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected int _picxx_readDeviceIDFull()
    {
        // Check for unsupported subparts
        if( _pic33_subPartIsNotSupported() ) return -1;

        /*
        System.out.printf( "### %04X\n", _pic33_readU16MemoryPage(0x800BFE) ); // Application ID
        // 0x00DF -> the programming executive is available for use
        //*/

        // Read and return the device ID
        return _eicsp ? _pe.ssReadDeviceID      (0xFF0000)
                      : _pic33_readU16MemoryPage(0xFF0000);
    }

    @Override
    protected int _picxx_readDeviceID()
    {
        // There is no device revision code in the device ID
        return _picxx_readDeviceIDFull();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected boolean _picxx_chipErase()
    {
        // Check for unsupported subparts
        if( _pic33_subPartIsNotSupported() ) return false;

        // Most programmer executive does not support full chip erase, so assume good here
        if(_eicsp) return true;

        // Perform chip erase
        return _pic33_chipErase();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected boolean _picxx_readFlash(final long address, final int[] dstBuff)
    {
        // Check for unsupported subparts
        if( _pic33_subPartIsNotSupported() ) return false;

        // Prepare an aligned buffer as needed
        final boolean reqPad = (dstBuff.length % _config33.memoryFlash.writeBlockSize != 0);
        final int     blkCnt = (dstBuff.length / _config33.memoryFlash.writeBlockSize) + (reqPad ? 1 : 0);
        final int[]   rdBuff = reqPad ? ( new int[blkCnt * _config33.memoryFlash.writeBlockSize] ) : dstBuff;

        // Read the code memory
        if(_eicsp) {
            if( !_pe.ssReadCodeMemory ( (int) address, rdBuff ) ) return false;
        }
        else {
            if( !_pic33_readCodeMemory( (int) address, rdBuff ) ) return false;
        }

        // Copy the result as needed
        if(reqPad) XCom.arrayCopy(dstBuff, rdBuff, 0, dstBuff.length);

        // Done
        return true;
    }

    @Override
    protected boolean _picxx_writeFlash(final boolean firstCall, final long address, final int[] srcBuff)
    {
        // Check for unsupported subparts
        if( _pic33_subPartIsNotSupported() ) return false;

        // Write the code memory
        if(_eicsp) {
            final boolean noERASEP = ( _config33.memoryFlash.eraseBlockSize > _config33.memoryFlash.writeBlockSize )
                                   ? ( (address % _config33.memoryFlash.eraseBlockSize) != 0 )
                                   : ( false                                                 );
            return _pe.ssWriteCodeMemory ( (int) address, srcBuff, noERASEP );
        }
        else {
            return _pic33_writeCodeMemory( firstCall, (int) address, srcBuff );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : It seems that none of the dsPIC33 MCUs have built-in EEPROM!

    @Override
    public boolean _picxx_supportsEEPROMAutoErase()
    { return false; }

    @Override
    protected boolean _picxx_readEntireEEPROM(final int[] dstBuff)
    { return false; }

    @Override
    protected boolean _picxx_writeEntireEEPROM(final int[] srcBuff, final boolean[] fDirty, final boolean eepromErased)
    { return false; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected long _picxx_readConfigByte(final long address)
    {
        // Check for unsupported subparts and operation
        if( _pic33_operationIsNotSupported_eicsp() ) return -1;

        // Read the byte
        return _pic33_readU16MemoryPage( (int) address );
    }

    @Override
    protected boolean _picxx_writeConfigBytes(final long[] refBuff, final long[] newBuff)
    {
        // Check for unsupported subparts and operation
        if( _pic33_operationIsNotSupported_eicsp() ) return false;

        // Simply return if the fuses are part of the program memory space
        // ##### ??? TODO : Make the this configurable via 'Config.BaseProgSpec' ??? #####
        if( _fusesInProgramMemorySpace() ) return true;

        // Write the configuration bytes as needed
        final int emptyValue = ( (FlashMemory_EmptyValue & 0xFF) << 8 ) | (FlashMemory_EmptyValue & 0xFF);

        for(int i = 0; i < _config33.memoryConfigBytes.address.length; ++i) {

            // Skip those that are not used
            if(_config33.memoryConfigBytes.address[i] < 0) continue;

            // Get the new value
            final long newVal = _getNewCWValue(i, refBuff, newBuff, _config33);

            // Skip those that are not changed
            if(newVal == refBuff[i]) continue;

            // Write the byte
            /*
            SysUtil.stdErr().printf("### [%08X] : %04X => %04X\n", _config33.memoryConfigBytes.address[i], refBuff[i], newVal);
            //*/
            if( !_pic33_writeCWMemory( (int) _config33.memoryConfigBytes.address[i], (int) newVal, emptyValue ) ) return false;

        } // for

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected ProgPIC_EICSP.CmdPE _picxx_pe_cmdPE()
    { return _config33.cmdPE; }

    @Override
    protected byte[] _picxx_pe_checkAdjustPE(final byte[] data)
    { return data; }

    @Override
    protected int[] _picxx_pe_readSavedWords()
    {
        // Check for unsupported subparts and operation
        if( _pic33_operationIsNotSupported_eicsp() ) return null;

        // Simply return an empty array because there is no word to be saved in dsPIC30
        return new int[0];
    }

    @Override
    protected boolean _picxx_pe_writeSavedWords(final int[] srcBuff)
    {
        // Check for unsupported subparts and operation
        if( _pic33_operationIsNotSupported_eicsp() ) return false;

        // Error if the buffer is null
        if(srcBuff == null) return false;

        // There is no word to be written in dsPIC33
        return true;
    }

    @Override
    protected boolean _picxx_pe_eraseArea()
    {
        // Check for unsupported subparts and operation
        if( _pic33_operationIsNotSupported_eicsp() ) return false;

        if(false) return true;

        // Perform area erase
        return _pic33_peErase();
    }

    @Override
    protected boolean _picxx_pe_writeData(final boolean firstCall, final long address, final int[] srcBuff)
    {
        // Check for unsupported subparts and operation
        if( _pic33_operationIsNotSupported_eicsp() ) return false;

        // Error if the address is out of range
        if(address + srcBuff.length > _config33.memoryPE.totalSize) return false;

        if(false) return true;

        // Write the PE memory
        _cs.setExtraNOPs(true ); // NOTE : Needed as stated in the 'Flash Programming Specification' // ##### ??? TODO : IS IT REALLY ??? ####

        final boolean res = _picxx_writeFlash( firstCall, _cs.badr(_config33.memoryPE.address) + address, srcBuff );

        _cs.setExtraNOPs(false);

        return res;
    }

    @Override
    protected boolean _picxx_pe_readData(final long address, final int[] dstBuff)
    {
        // Check for unsupported subparts and operation
        if( _pic33_operationIsNotSupported_eicsp() ) return false;

        // Error if the address is out of range
        if(address + dstBuff.length > _config33.memoryPE.totalSize) return false;

        if(false) return true;

        // Read the PE memory
        return _picxx_readFlash( _cs.badr(_config33.memoryPE.address) + address, dstBuff );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public FWD fwDecompose(final FWComposer fwc) throws Exception
    { return _cs.fwDecompose( super.fwDecompose(fwc), fwc ); }

} // class ProgDSPIC33
