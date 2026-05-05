/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.Serializable;

import jxm.*;
import jxm.annotation.*;
import jxm.tool.fwc.*;
import jxm.xb.*;

import static jxm.ugc.USB2GPIO.IEVal;


/*
 * Please refer to the comment block before the 'ProgPIC' class definition in the 'ProgSWD.java' file for more details and information.
 */
public class ProgDSPIC30 extends ProgPIC {

    /*
     * ######################################### !!! WARNING !!! #########################################
     * 1. Due to the nature of the dsPIC30 ICSP protocol, it can only be programmed correctly (especially
     *    for the code memory) using hardware-assisted bit-banging.
     * 2. It means that this programmer class ignores the user-supplied PGC frequency in 'begin()'.
     * ######################################### !!! WARNING !!! #########################################
     */

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
            baseProgSpec.part     = Part.dsPIC30;
            baseProgSpec.subPart  = SubPart.F;
            baseProgSpec.mode     = mode;
            baseProgSpec.entrySeq = -1;
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

// ##### ??? TODO : Add more specific-part 'dsPIC30*()' functions ??? #####

// ##### !!! TODO : dsPIC30F SMPS MCUs do not support HVP and use LVEntrySeq with different keys for ICSP and EICSP !!! #####


public static Config dsPIC30Fx0xx(final int flashKSize, final int eepromBSize)
{
    // Instantiate the configuration object
    final Config config = new Config(SubPart.F, true ? Mode.HVPulsedNOP : Mode.HVSimple);

    config.memoryFlash.totalSize           = flashKSize * 1024 * 3;
    config.memoryFlash.writeBlockSize      = 32                * 3; // NOTE : dsPIC30 writes 32 words at once (96 bytes)
    config.memoryFlash.eraseBlockSize      = 32                * 3; // NOTE : It does not matter as long as it is multiple of 'writeBlockSize' and smaller than the 'totalSize'

    config.memoryEEPROM.totalSize          = eepromBSize;
    config.memoryEEPROM.writeBlockSizeE    = 16                * 2; // NOTE : dsPIC30 writes 16 words at once (32 bytes) - EICSP mode only
    config.memoryEEPROM.addressBeg         = 0x7FFC00;
    config.memoryEEPROM.addressEnd         = config.memoryEEPROM.addressBeg + config.memoryEEPROM.totalSize - 1;
    config.memoryEEPROM.addressMulFW       = 2;
    config.memoryEEPROM.addressOfsFW       = 0;

    config.memoryPE.address                = 0x800000;
    config.memoryPE.totalSize              = 736 * 3;
    config.memoryPE.pageSize               = config.memoryPE.totalSize;
    config.memoryPE.numPages               =  1;
    config.memoryPE.saveWordOffset         = -1;
    config.memoryPE.saveWordCount          =  0;

    config.cmdPE.ERASEP                    = 0x9; // This device needs erase before writing
    config.cmdPE.ERASED                    = 0x8; // ---

    config.cmdPE.waitDelay_MS_EraseProgMem = 3;
    config.cmdPE.waitDelay_MS_WriteProgMem = 3;

    config.cmdPE.waitDelay_MS_EraseDataMem = 3;
    config.cmdPE.waitDelay_MS_WriteDataMem = 3;

    // Return the configuration object
    return config;
}

public static Config dsPIC30F3010()
{
    final Config config   = dsPIC30Fx0xx(8, 1024);
    final int    cwAStart = 0xF80000;

    config.memoryConfigBytes.addressBeg   = cwAStart;
    config.memoryConfigBytes.addressEnd   = config.memoryConfigBytes.addressBeg + 14 * 2;
                                                    //   FOSC                FWDT                FBORPOR             FBS                 FSS                 FGS                 FICD
    config.memoryConfigBytes.address      = new long[] { cwAStart + 0x0    , cwAStart + 0x2    , cwAStart + 0x4    , cwAStart + 0x6    , cwAStart + 0x8    , cwAStart + 0xA    , cwAStart + 0xC     };
    config.memoryConfigBytes.size         = new int [] { 2                 , 2                 , 2                 , 2                 , 2                 , 2                 , 2                  };
    config.memoryConfigBytes.bitMask      = new long[] { 0b1100011100011111, 0b1000000000111111, 0b1000011110110011, 0b0011000100001111, 0b0011001100001111, 0b0000000000000111, 0b1100000000000011 };
    config.memoryConfigBytes.orgMask      = new long[] { 0b0000000000000000, 0b0000000000000000, 0b0000000000000000, 0b0011000100001111, 0b0011001100001110, 0b0000000000000100, 0b0000000000000000 };
    config.memoryConfigBytes.maxTotalSize = 14 * 2;
    config.memoryConfigBytes.addressMulFW = 2;
    config.memoryConfigBytes.addressOfsFW = 0;

    return config;
}

public static Config dsPIC30F3011()
{ return dsPIC30F3010(); }


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    } // class Config

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private   final ProgPIC_SIX_REGOUT _cs = new ProgPIC_SIX_REGOUT();
    private   final ProgPIC_EICSP      _pe;

    protected final Config             _config30;
    protected final boolean            _stdp;
    protected final boolean            _eicsp;
    protected       boolean            _eicspInitDone = false;

    @SuppressWarnings("this-escape")
    public ProgDSPIC30(final USB2GPIO usb2gpio, final Config config) throws Exception
    {
        // Process the superclass
        super(usb2gpio, config);

        // Store the objects
        _config30 = (Config) super._config;
        _stdp     = (_config30.baseProgSpec.mode == Mode.HVSimple                                                    );
        _eicsp    = (_config30.baseProgSpec.mode == Mode.HVSimple1                                                   ) ||
                    (_config30.baseProgSpec.mode == Mode.LVEntrySeq && _config30.baseProgSpec.entrySeq == 0x4D434850L);

        // Configure the 'ProgPIC_SIX_REGOUT'
        _cs.setC4D24Mode             ( useHardwareAssistedBitBangingSPI() );
        _cs.setResetInternalPCAddress( 0x100, 0, 1                        );

        // Check the configuration values
        if(   _config30.baseProgSpec.part    != Part   .dsPIC30    ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSPart , ProgClassName);
        if(   _config30.baseProgSpec.subPart != SubPart.F          ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSSPrt , ProgClassName);
        if(   _config30.baseProgSpec.mode    != Mode   .HVSimple
           && _config30.baseProgSpec.mode    != Mode   .HVSimple1
           && _config30.baseProgSpec.mode    != Mode   .HVPulsedNOP
           && _config30.baseProgSpec.mode    != Mode   .LVEntrySeq ) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSMode , ProgClassName);

        ProgPIC_SIX_REGOUT.checkMemoryPE(_config30.memoryPE, ProgClassName);

        /*
        // EICSP is not supported yet
        if(_eicsp) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvBPSEICSP , ProgClassName);
        //*/

        // Instantiate 'ProgPIC_EICSP' as needed
        _pe = _eicsp ? new ProgPIC_EICSP(this, USB_GPIO.SPIMode._1, USB_GPIO.SPIMode._0) : null;

        if(_pe != null) {
            _pe.adjustFlashBlockSize(_config30); // EICSP may use different flash block sizes
            _pe.setC4D24Mode( useHardwareAssistedBitBangingSPI() );
        }
    }

    // WARNING : In STDP and ICSP modes, dsPIC30 memory can only be written in hardware-assisted bit-banging SPI mode!
    @Override
    public boolean useHardwareAssistedBitBangingSPI()
    { return !_eicsp || (_eicsp && !_eicspInitDone); }

    @Override
    public boolean inSTDPMode()
    { return _stdp; }

    @Override
    public boolean inEICSPMode()
    { return _eicsp; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected boolean _pic_enterPVM_mcuSpecific_extra()
    {
        // Check for unsupported subparts
        if( _pic30_subPartIsNotSupported() ) return false;

        // Extra delay
        SysUtil.sleepMS(100);

        // EICSP
        if(_eicsp) {

            /*
            // ##### !!! TODO : REMOVE THIS LATER !!! #####
            if(!true) {
                for(int i = 0; i < 24 * 64; ++i) {
                    System.out.print( _usb2gpio.spiSetBreakExt(false, true ) );
                    System.out.print( _usb2gpio.spiSetBreakExt(false, false) );
                }
                System.out.println();
                if(true) return false;
            }

            if( !_usb2gpio.spiSetBreak(false, false) ) return false;
            if( !_usb2gpio.spiSetSPIMode(USB2GPIO.SPIMode._0) ) return false;

            final int[] test0 = new int[] { 7, 0b00000000, 7, 0b00000000 };

            if( !_usb2gpio.spiXBTransferIgnoreSS(test0) ) return false;
            if( !_usb2gpio.spiXBTransferIgnoreSS(test0) ) return false;
            if( !_usb2gpio.spiXBTransferIgnoreSS(test0) ) return false;

            if( !_usb2gpio.spiClrBreak() ) return false;

            final int[] test1 = new int[] { 0b00000000, 0b00000001 };
            final int[] test2 = new int[] { 0b11111111, 0b11111111 };
            final int[] test3 = new int[] { 0b11111111, 0b11111111 };

            final int[] test4 = new int[] { 0b10110000, 0b00000001 };
            final int[] test5 = new int[] { 0b11111111, 0b11111111 };
            final int[] test6 = new int[] { 0b11111111, 0b11111111 };

            if( !_usb2gpio.spiTransferIgnoreSS(test1) ) return false;
            if( !_usb2gpio.spiTransferIgnoreSS(test2) ) return false;
            if( !_usb2gpio.spiTransferIgnoreSS(test3) ) return false;

            if( !_usb2gpio.spiTransferIgnoreSS(test4) ) return false;
            if( !_usb2gpio.spiTransferIgnoreSS(test5) ) return false;
            if( !_usb2gpio.spiTransferIgnoreSS(test6) ) return false;

            SysUtil.stdDbg().printf("### %02X%02X | %02X%02X %02X%02X\n", test1[0], test1[1], test2[0], test2[1], test3[0], test3[1] );
            SysUtil.stdDbg().printf("### %02X%02X | %02X%02X %02X%02X\n", test4[0], test4[1], test5[0], test5[1], test6[0], test6[1] );
            SysUtil.stdDbg().println();

            if(true) return false;
            //*/

            // Clear the PGD and PGC line as needed
            if(_config30.baseProgSpec.mode == Mode.HVSimple1) {
                if( !_usb2gpio.spiSetBreak(false, false) ) return false;
            }

            // Send the entry sequence
            /*
            if( !_usb2gpio.spiSetSPIMode(USB2GPIO.SPIMode._0) ) return false;

            final int[] entry = new int[] {
                7, 0, 7, 0,
                7, 0, 7, 0,
                7, 0, 7, 0
            };

            if( !_usb2gpio.spiXBTransferIgnoreSS(entry) ) return false; // Send extra 6 zeroes
            //*/

            // Perform sanity check
            final boolean res = _pe.xbSanityCheck();

            if(!res) return false;

            // EICSP intialization done
            _eicspInitDone = true;
            _pe.setC4D24Mode( useHardwareAssistedBitBangingSPI() );

            // Done
            return true;

        }

        // ICSP or STDP
        else {

            // Send 2 NOPs if it is in STDP mode
            if(_stdp) {
                final int[] entrySTDP = new int[] {
                    0b0000, 0x000000, // NOP
                    0b0000, 0x000000  // NOP
                };
                if( !_pic_xbTransfer_c4_d24(entrySTDP) ) return false;
            }

            // Exit the reset vector
            final int[] entry = new int[] {
                0b0000, 0x040100, // GOTO 0x100
                0b0000, 0x040100, // GOTO 0x100
                0b0000, 0x000000  // NOP
            };

            return _pic_xbTransfer_c4_d24(entry);

        }

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _pic30_subPartIsNotSupported()
    {
        // For the time being, all subparts are supported
        return false;
    }

    private boolean _pic30_operationIsNotSupported_icsp()
    {
        // Check for unsupported subparts
        if( _pic30_subPartIsNotSupported() ) return true;

        // This operation is not supported in ICSP mode
        if(!_eicsp) return !USB2GPIO.notifyError(Texts.ProgXXX_FailPIC_OperICSP, ProgClassName);

        // The operation is supported in ICSP mode
        return false;
    }

    private boolean _pic30_operationIsNotSupported_eicsp()
    {
        // Check for unsupported subparts
        if( _pic30_subPartIsNotSupported() ) return true;

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
        if( _pic30_subPartIsNotSupported() ) return -1;

        /*
        System.out.printf( "### %04X\n", _cs.exeCmds_readU16Memory_dspic30(this, 0x8005BE) ); // Application ID
        // 0x00BB -> the programming executive is available for use
        //*/

        // Read and return the device ID
        return _eicsp ? _pe.ssReadDeviceID               (      0xFF0000)
                      : _cs.exeCmds_readU16Memory_dspic30(this, 0xFF0000);
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
        if( _pic30_subPartIsNotSupported() ) return false;

        // Almost all programming executives do not support full chip erase, so simply do nothing and return success
        if(_eicsp) return true;

        // Special case for dsPIC30F5011 and dsPIC30F5013
        final int devID = _picxx_readDeviceID();

        if(devID == 0x0080 || devID == 0x0081) {
            if( !_cs.exeCmds_writeCWMemory_pic30(this, 0xF8006, 0) ) return false;
            if( !_cs.exeCmds_writeCWMemory_pic30(this, 0xF8008, 0) ) return false;
        }

        // Perform chip erase
        return _cs.exeCmds_chipErase_dspic30(this);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected boolean _picxx_readFlash(final long address, final int[] dstBuff)
    {
        // Check for unsupported subparts
        if( _pic30_subPartIsNotSupported() ) return false;

        // Prepare an aligned buffer as needed
        final boolean reqPad = (dstBuff.length % _config30.memoryFlash.writeBlockSize != 0);
        final int[]   rdBuff = reqPad ? ( new int[_config30.memoryFlash.writeBlockSize] ) : dstBuff;

        // Read the code memory
        if(_eicsp) {
            if( !_pe.ssReadCodeMemory ( (int) address, rdBuff ) ) return false;
        }
        else {
            if( !_cs.exeCmds_readCodeMemory_dspic30( this, _config30.memoryFlash, (int) address, rdBuff ) ) return false;
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
        if( _pic30_subPartIsNotSupported() ) return false;

        // Write the code memory
        if(_eicsp) {
            return _pe.ssWriteCodeMemory ( (int) address, srcBuff );
        }
        else {
            return _cs.exeCmds_writeCodeMemory_dspic30( this, _config30.memoryFlash, firstCall, (int) address, srcBuff );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean _picxx_supportsEEPROMAutoErase()
    { return true; }

    @Override
    protected boolean _picxx_readEntireEEPROM(final int[] dstBuff)
    {
        // Check for unsupported subparts
        if( _pic30_subPartIsNotSupported() ) return false;

        // Read and return all the EEPROM data
        if(_eicsp) {
            return _pe.ssReadEntireDataMemory( (int) _config30.memoryEEPROM.addressBeg, dstBuff );
        }
        else {
            return _cs.exeCmds_readDataMemory_dspic30( this, (int) _config30.memoryEEPROM.addressBeg, dstBuff );
        }
    }

    @Override
    protected boolean _picxx_writeEntireEEPROM(final int[] srcBuff, final boolean[] fDirty, final boolean eepromErased)
    {
        // Check for unsupported subparts
        if( _pic30_subPartIsNotSupported() ) return false;

        // Check if at least one of the elements is dirty
        // ##### !!! TODO : Pass the 'fDirty' argument to the implementation function (for optimization)? !!! #####
        if( XCom.arrayAllElementsEqual(fDirty) && !fDirty[0] ) return true;

        // Write all the EEPROM data
        if(_eicsp) {
            return _pe.ssWriteEntireDataMemory( (int) _config30.memoryEEPROM.addressBeg, srcBuff, _config30.memoryEEPROM.writeBlockSizeE );
        }
        else {
            return _cs.exeCmds_writeDataMemory_dspic30( this, (int) _config30.memoryEEPROM.addressBeg, srcBuff, !eepromErased );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected long _picxx_readConfigByte(final long address)
    {
        // Check for unsupported subparts and operation
        if( _pic30_operationIsNotSupported_eicsp() ) return -1;

        // Read the byte
        return _cs.exeCmds_readU16Memory_dspic30(this, (int) address );
    }

    @Override
    protected boolean _picxx_writeConfigBytes(final long[] refBuff, final long[] newBuff)
    {
        // Check for unsupported subparts and operation
        if( _pic30_operationIsNotSupported_eicsp() ) return false;

        // Write the configuration bytes as needed
        for(int i = 0; i < _config30.memoryConfigBytes.address.length; ++i) {

            // Skip those that are not used
            if(_config30.memoryConfigBytes.address[i] < 0) continue;

            // Get the new value
            final long newVal = _getNewCWValue(i, refBuff, newBuff, _config30);

            // Skip those that are not changed
            if(newVal == refBuff[i]) continue;

            // Write the byte
            /*
            SysUtil.stdErr().printf("### [%08X] : %04X => %04X\n", _config30.memoryConfigBytes.address[i], refBuff[i], newVal);
            //*/
            if( !_cs.exeCmds_writeCWMemory_pic30( this, (int) _config30.memoryConfigBytes.address[i], (int) newVal ) ) return false;

        } // for

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected ProgPIC_EICSP.CmdPE _picxx_pe_cmdPE()
    { return _config30.cmdPE; }

    @Override
    protected byte[] _picxx_pe_checkAdjustPE(final byte[] data)
    {
        //*
        // ##### ??? TODO : Is this really the correct method ??? #####

        // Location of the application ID
        final int sigPos = 0x05BE;

        // Add the application ID manually if its location is not within the data
        if( _cs.cadr(data.length) < (sigPos + 1) ) {
            final int    newSize = (int) Math.ceil( (float) _cs.badr(sigPos) / 6 ) * 6;
            final byte[] newBuff = new byte[newSize];
            for(int i = 0                ; i < data.length; ++i) newBuff[ i                ] = data[i];
            for(int i = data.length; i < newSize          ; ++i) newBuff[ i                ] = (byte) 0x00;
                                                                 newBuff[ _cs.badr(sigPos) ] = (byte) 0xBB;
            return newBuff;
        }
        //*/

        // Return the original data
        return data;
    }

    @Override
    protected int[] _picxx_pe_readSavedWords()
    {
        // Check for unsupported subparts and operation
        if( _pic30_operationIsNotSupported_eicsp() ) return null;

        // Simply return an empty array because there is no word to be saved in dsPIC30
        return new int[0];
    }

    @Override
    protected boolean _picxx_pe_writeSavedWords(final int[] srcBuff)
    {
        // Check for unsupported subparts and operation
        if( _pic30_operationIsNotSupported_eicsp() ) return false;

        // Error if the buffer is null
        if(srcBuff == null) return false;

        // There is no word to be written in dsPIC30
        return true;
    }

    @Override
    protected boolean _picxx_pe_eraseArea()
    {
        // Check for unsupported subparts and operation
        if( _pic30_operationIsNotSupported_eicsp() ) return false;

        if(false) return true;

        // Perform area erase
        return _cs.exeCmds_peErase_dspic30(this);
    }

    @Override
    protected boolean _picxx_pe_writeData(final boolean firstCall, final long address, final int[] srcBuff)
    {
        // Check for unsupported subparts and operation
        if( _pic30_operationIsNotSupported_eicsp() ) return false;

        // Error if the address is out of range
        if(address + srcBuff.length > _config30.memoryPE.totalSize) return false;

        if(false) return true;

        // Write the PE memory
        return _picxx_writeFlash( firstCall, _cs.badr(_config30.memoryPE.address) + address, srcBuff );
    }

    @Override
    protected boolean _picxx_pe_readData(final long address, final int[] dstBuff)
    {
        // Check for unsupported subparts and operation
        if( _pic30_operationIsNotSupported_eicsp() ) return false;

        // Error if the address is out of range
        if(address + dstBuff.length > _config30.memoryPE.totalSize) return false;

        if(false) return true;

        // Read the PE memory
        return _picxx_readFlash( _cs.badr(_config30.memoryPE.address) + address, dstBuff );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public FWD fwDecompose(final FWComposer fwc) throws Exception
    { return _cs.fwDecompose( super.fwDecompose(fwc), fwc ); }

} // class ProgDSPIC30
