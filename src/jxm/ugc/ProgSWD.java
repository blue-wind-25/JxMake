/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.Serializable;

import java.util.Arrays;
import java.util.function.IntConsumer;

import jxm.*;
import jxm.annotation.*;
import jxm.tool.*;
import jxm.ugc.fl.*;
import jxm.xb.*;


/*
 * This class and its related classes are written partially based on the algorithms and information found from:
 *
 *     ARM(R) Debug Interface v5 Architecture Specification
 *     https://documentation-service.arm.com/static/5f900a61f86e16515cdc0610
 *
 *     ARM(R) Debug Interface Architecture Specification - ADIv5.0 to ADIv5.2
 *     https://documentation-service.arm.com/static/622222b2e6f58973271ebc21
 *
 *     ARM(R) Debug Interface Architecture Specification - ADIv6.0
 *     https://documentation-service.arm.com/static/62221ef4e6f58973271ebc1d
 *
 *     ----------------------------------------------------------------------------------------------------
 *
 *     ARM(R) Architecture Reference Manual
 *     https://developer.arm.com/documentation/ddi0100/latest
 *     https://documentation-service.arm.com/static/5f8dacc8f86e16515cdb865a
 *
 *     ARM(R) v6-M Architecture Reference Manual
 *     https://developer.arm.com/documentation/ddi0419/latest
 *     https://documentation-service.arm.com/static/5f8ff05ef86e16515cdbf826
 *
 *     ARM(R) v7-M Architecture Reference Manual
 *     https://developer.arm.com/documentation/ddi0403/latest
 *     https://documentation-service.arm.com/static/606dc36485368c4c2b1bf62f
 *
 *     ARM(R) v8-M Architecture Reference Manual
 *     https://developer.arm.com/documentation/ddi0553/latest
 *     https://documentation-service.arm.com/static/65816177b52744113be5e971
 *
 *     ----------------------------------------------------------------------------------------------------
 *
 *     AN0062
 *     Programming Internal Flash Over the Serial Wire Debug Interface
 *     https://www.silabs.com/documents/public/application-notes/an0062.pdf
 *
 *     AN11553
 *     Serial Wire Debug (SWD) programming specification
 *     https://community.nxp.com/pwmxy87654/attachments/pwmxy87654/lpc/55224/1/SWD%2520Programming%2520AN11553.pdf
 *
 *     ----------------------------------------------------------------------------------------------------
 *
 *     Making My Own Programmer/Debugger using ARM SWD
 *     https://qcentlabs.com/posts/swd_banger
 *
 *     Programming Internal SRAM over SWD
 *     https://github.com/MarkDing/swd_programing_sram/blob/master/README.md
 *
 *     SWD with Bidirectional SPI on Raspberry Pi
 *     https://docs.google.com/spreadsheets/d/12oXe1MTTEZVIbdmFXsOgOXVFHCQnYVvIw6fRpIQZybg/htmlview#
 *
 *     OpenOCD on Raspberry Pi: Better with SWD on SPI
 *     https://www.pcbway.com/blog/technology/OpenOCD_on_Raspberry_Pi__Better_with_SWD_on_SPI.html
 *
 *     ARM-ASM-Tutorial
 *     https://www.mikrocontroller.net/articles/ARM-ASM-Tutorial
 *
 *     ----------------------------------------------------------------------------------------------------
 *
 *     OpenOCD Tcl Scripts
 *     https://github.com/openocd-org/openocd/tree/master/tcl
 *
 *     LibSWD
 *     Serial Wire Debug Open Framework for Low-Level Embedded Systems Access
 *     https://annals-csis.org/proceedings/2012/pliks/279.pdf
 *
 *     STM32Flash
 *     Open Source Flash Program for STM32 Osing the ST Serial Bootloader
 *     https://sourceforge.net/projects/stm32flash
 *
 *     ----------------------------------------------------------------------------------------------------
 *
 *     UF2 Bootloader
 *     https://github.com/ladyada/uf2-samd21
 *
 *     USB DFU Bootloader for SAMD11/SAMD21
 *     https://github.com/majbthrd/SAMDx1-USB-DFU-Bootloader
 *
 *     DueFlashStorage
 *     https://github.com/sebnil/DueFlashStorage
 *
 *     Arduino Core for SAMD21 CPU
 *     https://github.com/arduino/ArduinoCore-samd
 *
 *     Arduino Core for SAM3X CPU
 *     https://github.com/arduino/ArduinoCore-sam/tree/master
 *     https://github.com/arduino/ArduinoCore-sam/tree/master/system/libsam
 *
 * ~~~ Last accessed & checked on 2024-07-12 ~~~
 */
public class ProgSWD extends ProgSWDLowLevel {

    // ##### !!! TODO : Optimize speed further by combining one or more related commands into a single USB communication !!! #####

    /*
     * Transfer speed:
     *     # Using USB_ISS            : up to   ~20 ...    ~55 bytes per second (depending on the target and operation) (too slow to be usable!)
     *     # Using JxMake DASA        : up to   ~60 ...   ~170 bytes per second (depending on the target and operation) (too slow to be useful!)
     *     # Using JxMake USB-GPIO    : up to ~1800 ...  ~5500 bytes per second (depending on the target and operation); if 'ProgSWDLowLevel.USE_MULTI_CMD_FOR_WR_CORE_MEM' and ' ProgSWDLowLevel.USE_MULTI_CMD_FOR_RD_CORE_MEM' are set to 'false'
     *                                  up to ~3600 ... ~12900 bytes per second (depending on the target and operation); if 'ProgSWDLowLevel.USE_MULTI_CMD_FOR_WR_CORE_MEM' and ' ProgSWDLowLevel.USE_MULTI_CMD_FOR_RD_CORE_MEM' are set to 'true'
     *     # Using JxMake USB-GPIO II : similar to 'JxMake USB-GPIO'            (depending on the target and operation)
     *
     * NOTE : This programmer may not be suitable (too slow) for devices with larger flash (at least until further optimizations).
     */

    public ProgSWD(final USB2GPIO usb2gpio, final Config config) throws Exception
    { super(usb2gpio, config); }

    public Config config()
    { return _config; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static enum SWDClockFrequency {

           Minimum(USB2GPIO.MinimumFrequency),

         _125000Hz(  125000                 ),
         _156250Hz(  156250                 ),
         _187500Hz(  187500                 ),
         _250000Hz(  250000                 ),
         _312500Hz(  312500                 ),
         _375000Hz(  375000                 ),
         _500000Hz(  500000                 ),
         _625000Hz(  625000                 ),
         _750000Hz(  750000                 ),

           _125kHz(  125000                 ),
           _250kHz(  250000                 ),
           _375kHz(  375000                 ),
           _500kHz(  500000                 ),
           _625kHz(  625000                 ),
           _750kHz(  750000                 ),
          _1000kHz( 1000000                 ),
          _1250kHz( 1250000                 ),
          _1500kHz( 1500000                 ),
          _2000kHz( 2000000                 ),
          _2500kHz( 2500000                 ),
          _3000kHz( 3000000                 ),
          _4000kHz( 4000000                 ),
          _5000kHz( 5000000                 ),
          _6000kHz( 6000000                 ),
          _8000kHz( 8000000                 ),

             _1MHz( 1000000                 ),
             _2MHz( 2000000                 ),
             _3MHz( 3000000                 ),
             _4MHz( 4000000                 ),
             _5MHz( 5000000                 ),
             _6MHz( 6000000                 ),
             _8MHz( 8000000                 ),
            _10MHz(10000000                 ),
            _12MHz(12000000                 ),
            _16MHz(16000000                 ),
            _20MHz(20000000                 ),
            _24MHz(24000000                 ),
            _32MHz(32000000                 ),
            _48MHz(48000000                 ),

           Maximum(USB2GPIO.MaximumFrequency)

        ;

        private int _value;

        private SWDClockFrequency(final int value)
        { _value = value; }

    } // enum SWDClockFrequency

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private long[] _armCPUID   = null;

    private int [] _int_mcuID1 = null; // The use of this variable depends on the specific MCU part
    private int [] _int_mcuID2 = null; // ---
    private int [] _int_mcuID3 = null; // ---
    private long[] _lng_mcuID1 = null; // ---
    private long[] _lng_mcuID2 = null; // ---
    private long[] _lng_mcuID3 = null; // ---

    /*
     * NOTE : # Some MCUs (e.g. RP2040) expose multiple Debug Ports (DPs), typically one per core.
     *          In this case, specify their ID values in 'multidropIDs[]'. For example:
     *
     *              new long[] { 0x01002927L,   // Core #0
     *                           0x11002927L  } // Core #1
     *
     *        # Some MCUs (e.g. RP2350) expose only a single DP, with multiple cores accessible via
     *          Mem-AP offsets. In this case, specify negative values for the IDs in 'multidropIDs[]'
     *          to indicate that it uses Mem-AP offsets. For example:
     *
     *              new long[] { -0x00040927L, 0x00002000L,   // ARM Core #0
     *                           -0x00040927L, 0x00004000L  } // ARM Core #1
     */

    // NOTE : By default, do not allow bit banging SPI as it is too slow to be useful for ARM-based MCUs
    //        which typically have larger flash memory sizes.
    public boolean begin(final long[] multidropIDs, final int idxDefMultidropID)
    { return begin(multidropIDs, idxDefMultidropID, false             , SWDClockFrequency._8MHz._value); }

    public boolean begin(final long[] multidropIDs, final int idxDefMultidropID, final boolean allowBitBangingSPI)
    { return begin(multidropIDs, idxDefMultidropID, allowBitBangingSPI, SWDClockFrequency._8MHz._value); }

    public boolean begin(final long[] multidropIDs, final int idxDefMultidropID, final boolean allowBitBangingSPI, final SWDClockFrequency swdClockFrequency)
    { return begin(multidropIDs, idxDefMultidropID, allowBitBangingSPI, swdClockFrequency      ._value); }

    public boolean begin(final long[] multidropIDs_, final int idxDefMultidropID, final boolean allowBitBangingSPI, final int swdClockFrequency)
    {
        // Error if already in programming mode
        if(_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_InProgMode, ProgClassName);

        // Clear flag
        _chipErased = false;

        // Enable mode
        if(_usb2gpio instanceof USB_GPIO) {
            if( !( (USB_GPIO) _usb2gpio ).pcf8574Enable_SWD() ) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitPCF8574, ProgClassName);
        }

        // Initialize the SPI
        final int spiClkDiv = _usb2gpio.spiClkFreqToClkDiv(swdClockFrequency);

        for(int i = 0; i < 2; ++i) {
            // Use hardware SPI mode unless the user specifically allow bit banging SPI
            if( allowBitBangingSPI || _usb2gpio.spiSetImplMode(USB2GPIO.ImplMode.Hardware) ) {
                // Initialize the SPI
                if( _usb2gpio.spiBegin(USB2GPIO.SPIMode._2, USB2GPIO.SSMode.ActiveLow, spiClkDiv) ) {
                    break;
                }
                // Error initializing the SPI
                else {
                    // Exit if this is the 2nd initialization attempt
                    if(i > 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailInitSPI, ProgClassName);
                }
            }
            // Error selecting hardware SPI mode
            else {
                // Exit if this is the 2nd initialization attempt
                if(i > 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailSelHWSPI, ProgClassName);
            }
            // Uninitialize the SPI and try again
            _usb2gpio.spiEnd();
        }

        _swdClockFrequency = _usb2gpio.spiGetSupportedClkFreqs()[spiClkDiv];

        // Intialize
        try {

            // Select the device
            if( !_usb2gpio.spiSelectSlave() ) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSelSlave, ProgClassName);

            // Split the multidrop IDs and the Mem-AP offsets
            long[] multidropIDs = null;
            long[] memAPOffsets = null;

            if(multidropIDs_ != null) {
                if(multidropIDs_[0] >= 0) {
                    // Error if the other IDs are not >= 0
                    for(final long v : multidropIDs_) {
                        if(v < 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_InvMDID, ProgClassName);
                    }
                    // Copy the IDs
                    multidropIDs = XCom.arrayCopy(multidropIDs_);
                }
                else {
                    // Error if the other IDs are not < 0
                    for(int m = 0; m < multidropIDs_.length; m += 2) {
                        if(multidropIDs_[m] >= 0) return USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_InvMDID, ProgClassName);
                    }
                    // Extract and copy the IDs and the offsets
                    multidropIDs = new long[multidropIDs_.length / 2];
                    memAPOffsets = new long[multidropIDs .length    ];
                    for(int m = 0; m < multidropIDs.length; ++m) {
                        multidropIDs[m] = (multidropIDs_[m * 2 + 0] == -1) ? -1 : Math.abs( multidropIDs_[m * 2 + 0] );
                        memAPOffsets[m] =                                         Math.abs( multidropIDs_[m * 2 + 1] );
                    }
                }
            }

            // Copy the multidrop IDs and allocate arrays for IDCODE and DAP version
            final int midCnt = (multidropIDs != null) ? multidropIDs.length : 1;

            _multidropIDs = multidropIDs;
            _memAPOffsets = memAPOffsets;

            _idcode       = new long[midCnt];
            _dapVer       = new int [midCnt];

            _armCPUID     = new long[midCnt];

            _int_mcuID1   = new int [midCnt];
            _int_mcuID2   = new int [midCnt];
            _int_mcuID3   = new int [midCnt];
            _lng_mcuID1   = new long[midCnt];
            _lng_mcuID2   = new long[midCnt];
            _lng_mcuID3   = new long[midCnt];

            Arrays.fill(_armCPUID  , -1);

            Arrays.fill(_int_mcuID1, -1);
            Arrays.fill(_int_mcuID2, -1);
            Arrays.fill(_int_mcuID3, -1);
            Arrays.fill(_lng_mcuID1, -1);
            Arrays.fill(_lng_mcuID2, -1);
            Arrays.fill(_lng_mcuID3, -1);

            // Initialize all
            for(int midIdx = 0; midIdx < midCnt; ++midIdx) {

                // Select the multidrop ID
                if(_multidropIDs != null) _idxSelMultidropID = midIdx;

                // Initialize SWD
                _swd_init();

                // Read the IDCODE as needed
                _idcode[midIdx] = _swdRdDP_IDCODE();
                _dapVer[midIdx] = (int) ( (_idcode[midIdx] & 0x00007800L) >> 12 );

                // Power up the debug interface
                _swd_puDbgIfc();

                // Read and check IDR
                final long idr = _swdRdMemAP(MemAP.IDR);

                if(idr <= 0) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_RdRegAPV, ProgClassName, idr, MemAP.IDR.name);

                // Configure the transfer size and address increment
                for(int i = 0; i < 2; ++i) {
                    try {

                        // Configure the transfer size and address increment                           ┌─────→ ADDRINC = 0b01  -> increment single
                        //                                                                             ├┐ ┌┬┬→ SIZE    = 0b010 -> word size (32 bits)
                        //                                                           0b11001000    0b00010010
                        //                                                           ├┐            ├┐
                        final long cswRef = _swdRdMemAP(MemAP.CSW        ) & 0xFFFFFFC8L | 0x00000012L;
                                            _swdWrMemAP(MemAP.CSW, cswRef);
                        final long cswChk = _swdRdMemAP(MemAP.CSW        );

                        if(cswChk != cswRef) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_RdRegAPV, ProgClassName, cswChk, MemAP.CSW.name);

                        // Done
                        break;
                    }
                    catch(final Exception e) {
                        // Exit if this is the 2nd attempt
                        if(i > 0) throw e;
                        // Maybe the core is halted? Try to un-halt it
                        _swd_reinit();
                        _swd_unhaltCore(true, true);
                        _swd_reinit();
                    }
                } // for

                /*
                SysUtil.stdDbg().printf( "IDR = %08X\n", _swdRdMemAP(MemAP.IDR) );
                SysUtil.stdDbg().printf( "CSW = %08X\n", _swdRdMemAP(MemAP.CSW) );
                SysUtil.stdDbg().printf( "IDR = %08X\n", _swdRdMemAP(MemAP.IDR) );
                SysUtil.stdDbg().printf( "CSW = %08X\n", _swdRdMemAP(MemAP.CSW) );
                //*/

            } // for

            // Select the user-specified multidrop ID
            _idxSelMultidropID = idxDefMultidropID;

            // Halt all cores
            _swd_haltAllCores();

            // ##### ##### ##### EXPERIMENT ##### ##### #####
            /*
             *  NRF52 CTRL-AP (AP #1)
             *
             *  RESET             0x00   Soft reset triggered through CTRL-AP
             *  ERASEALL          0x04   Erase all
             *  ERASEALLSTATUS    0x08   Status register for the ERASEALL operation
             *  APPROTECTSTATUS   0x0C   Status register for access port protection
             *  IDR               0xFC   CTRL-AP identification register, IDR
             */
            /*
            SysUtil.stdDbg().println("##### ##### ##### EXPERIMENT - A ##### ##### #####");
            try {
                _swd_resetAndUnhaltAllCores();

                SysUtil.stdDbg().printf( "%08X\n", _swdRdRawMemAP(1, 0xFC, "CTRL-AP:IDR", false) ); // 02880000
                SysUtil.stdDbg().printf( "%08X\n", _swdRdRawMemAP(1, 0x0C, "CTRL-AP:IDR", false) ); // 00000001

                SysUtil.stdDbg().println("RESET"  ); _swdWrRawMemAP(1, 0x00, 1, "CTRL-AP:IDR", false);
                SysUtil.sleepMS(100);
                SysUtil.stdDbg().println("UNRESET"); _swdWrRawMemAP(1, 0x00, 0, "CTRL-AP:IDR", false);
            }
            catch(final USB2GPIO.TansmitError e) { e.printStackTrace(); }
            SysUtil.stdDbg().println("##### ##### ##### EXPERIMENT - B ##### ##### #####\n\n");
            // ##### ##### ##### EXPERIMENT ##### ##### #####
            //*/

        } // try
        catch(final USB2GPIO.TansmitError e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Uninitialize the SPI
            _usb2gpio.spiEnd();
            // Notify error
            return USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Init, ProgClassName);
        }

        // Set flag
        _inProgMode = true;

        // Done
        return true;
    }

    @Override
    public boolean end()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Commit EEPROM, unhalt all cores, deselect the device, and uninitialize all
        final boolean resCommitEEPROM  = commitEEPROM();
        final boolean resCommitFsLBs   = _commitFLB();
        final boolean resUninitSytem   = _uninitSystemExit( _getFlashLoaderSpecifier() );
        final boolean resResetUnhalt   = _swd_resetAndUnhaltAllCores();
        final boolean resDeselectSlave = _usb2gpio.spiDeselectSlave();
        final boolean resSPIEnd        = _usb2gpio.spiEnd();

        // Disable mode
        boolean resDisMode = true;

        if(_usb2gpio instanceof USB_GPIO) {
            resDisMode = ( (USB_GPIO) _usb2gpio ).pcf8574Disable();
        }

        // Clear flags and data
        _inProgMode   = false;
        _chipErased   = false;

        _idcode       = null;
        _dapVer       = null;

        _armCPUID     = null;

        _int_mcuID1   = null;
        _int_mcuID2   = null;
        _int_mcuID3   = null;
        _lng_mcuID1   = null;
        _lng_mcuID2   = null;
        _lng_mcuID3   = null;

        _mcuSignature = null;

        _flSpecNP     = null;
        _flSpecPM     = null;

        _eepromBuffer = null;
        _eepromFDirty = null;

        // Check for error(s)
        if(!resCommitEEPROM || !resCommitFsLBs || !resUninitSytem || !resResetUnhalt || !resDeselectSlave || !resSPIEnd || !resDisMode) {
            if(!resCommitEEPROM ) USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_CmEEPROM , ProgClassName);
            if(!resCommitFsLBs  ) USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_CmFsLBs  , ProgClassName);
            if(!resUninitSytem  ) USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_UninitSys, ProgClassName);
            if(!resResetUnhalt  ) USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_RstUnhCr , ProgClassName);
            if(!resDeselectSlave) USB2GPIO.notifyError(Texts.ProgXXX_FailDselSlave    , ProgClassName);
            if(!resSPIEnd       ) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitSPI    , ProgClassName);
            if(!resDisMode      ) USB2GPIO.notifyError(Texts.ProgXXX_FailUninitPCF8574, ProgClassName);
            return false;
        }

        // Done
        return true;
    }

    public int dapVersion()
    { return _dapVer[ (_idxSelMultidropID >= 0) ? _idxSelMultidropID : 0 ]; }

    public boolean inProgMode()
    { return _inProgMode; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### !!! TODO : Extend the signature with 'armCPUID()'? !!! #####
    // ##### !!! TODO : Extend the signature with the MCU's specific values (some MCUs support it)? !!! #####

    private int[] _mcuSignature = null;

    @Override
    public boolean supportSignature()
    { return false; }

    @Override
    public boolean readSignature()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Generate signature bytes from the IDCODE
        final long idcode = _idcode[ (_idxSelMultidropID >= 0) ? _idxSelMultidropID : 0 ];

        _mcuSignature = new int[] {
            (int) ( (idcode >> 24) & 0xFF ),
            (int) ( (idcode >> 16) & 0xFF ),
            (int) ( (idcode >>  8) & 0xFF ),
            (int) ( (idcode >>  0) & 0xFF )
        };

        // Done
        return true;
    }

    @Override
    public boolean verifySignature(final int[] signatureBytes)
    {
        // Error if the signature has not been read
        if(_mcuSignature == null) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Compare the signature
        if(signatureBytes.length != _mcuSignature.length) return false;

        for(int i = 0; i < _mcuSignature.length; ++i) {
            if( signatureBytes[i] < 0                 ) continue; // Skip if the user does not want to check this byte
            if( signatureBytes[i] != _mcuSignature[i] ) return false;
        }

        return true;
    }

    @Override
    public int[] mcuSignature()
    {
        // Error if the signature has not been read
        if(_mcuSignature == null) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return null;
        }

        // Return the signature
        return _mcuSignature;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private SWDFlashLoader.Specifier _flSpecNP = null;
    private SWDFlashLoader.Specifier _flSpecPM = null;

    private SWDFlashLoader.Specifier _getFlashLoaderSpecifier()
    {
        if(!_inProgMode) {
            // This part is needed to access the partial (minimum) amount of information of the MCU
            if(_flSpecNP == null) _flSpecNP = SWDFlashLoader.getSpecifierFor(_config, _swdExecInst);
            return _flSpecNP;
        }

        if(_flSpecPM == null) _flSpecPM = SWDFlashLoader.getSpecifierFor(_config, _swdExecInst);
        return _flSpecPM;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _systemInitializedOnce = false;

    private boolean _initSystemOnce(final SWDFlashLoader.Specifier flSpec)
    {
        // Exit if it is already initialized
        if(_systemInitializedOnce) return true;

        // Intialize the system once
        try {
            if(flSpec != null && flSpec.instruction_InitializeSystemOnce != null) _swdExecInst._exec(flSpec.instruction_InitializeSystemOnce);
        }

        catch(final USB2GPIO.TansmitError e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName, _config.memoryFlash.driverName);
            return false;
        }

        // Set flag
        _systemInitializedOnce = true;

        // Done
        return true;
    }

    private boolean _uninitSystemExit(final SWDFlashLoader.Specifier flSpec)
    {
        // Exit if was not initialized
        if(!_systemInitializedOnce) return true;

        // Intialize the system once and clear the cached specifier
        try {
            if(flSpec != null && flSpec.instruction_UninitializeSystemExit != null) {
                _swdExecInst._exec(flSpec.instruction_UninitializeSystemExit);
                _flSpecNP = null;
                _flSpecPM = null;
            }
        }

        catch(final USB2GPIO.TansmitError e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName, _config.memoryFlash.driverName);
            return false;
        }

        // Clear flag
        _systemInitializedOnce = false;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public long swdRd32(final long address) throws Exception
    {
        try                      { return _swdRdCoreMem(address); }
        catch(final Exception e) { _swd_abort(); throw e;         }
    }

    public void swdWr32(final long address, final long value) throws Exception
    {
        try                      { _swdWrCoreMem(address, value); }
        catch(final Exception e) { _swd_abort(); throw e;         }
    }

    public void swdRdBuff(final long address, final int[] buff, final int count, final int transferSize) throws Exception
    {
        try                      { _swdRdCoreMem(address, buff, count, transferSize); }
        catch(final Exception e) { _swd_abort(); throw e;                             }
    }

    public void swdRdBuff(final long address, final int[] buff, final int count) throws Exception
    { swdRdBuff(address, buff, count, 0); }

    public void swdRdBuff(final long address, final int[] buff) throws Exception
    { swdRdBuff(address, buff, buff.length, 0); }

    public void swdWrBuff(final long address, final int[] buff, final int count, final int transferSize) throws Exception
    {
        try                      { _swdWrCoreMem(address, buff, count, transferSize); }
        catch(final Exception e) { _swd_abort(); throw e;                             }
    }

    public void swdWrBuff(final long address, final int[] buff, final int count) throws Exception
    { swdWrBuff(address, buff, count, 0); }

    public void swdWrBuff(final long address, final int[] buff) throws Exception
    { swdWrBuff(address, buff, buff.length, 0); }

    public SWDExecInst swdExecInst()
    { return _swdExecInst; }

    public long swdExecInst(final long[][] instructions) throws USB2GPIO.TansmitError
    { return _swdExecInst._exec(instructions, null); }

    public long swdExecInst(final long[][] instructions, final int[] dataBuff) throws USB2GPIO.TansmitError
    { return _swdExecInst._exec(instructions, dataBuff); }

    public long xviGet(final SWDExecInstOpcode.XVI xvi                  ) { return _swdExecInst._xviGet( xvi.value() ); }
    public void xviSet(final SWDExecInstOpcode.XVI xvi, final long value) { _swdExecInst._xviSet( xvi.value(), value ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean chipErase()
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Exit if the device is already erased
        if(_chipErased) return true;

        // Get the flash loader specification
        final SWDFlashLoader.Specifier flSpec = _getFlashLoaderSpecifier();

        // Check if the requested operation is supported
        final boolean partialErase = (_config.memoryFlash.partEraseAddressBeg >= 0) && (_config.memoryFlash.partEraseSize > 0);

        if(partialErase) {
            if(flSpec.instruction_EraseFlashPages == null) return USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_PFMemErs, ProgClassName, _config.memoryFlash.driverName);
        }
        else {
            if(flSpec.instruction_EraseFlash == null) return USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_EFMemErs, ProgClassName, _config.memoryFlash.driverName);
        }

        // Initialize the system
        if( !_initSystemOnce(flSpec) ) return false;

        // ##### !!! TODO : How about lock bits, etc.? !!! #####

        // Erase the flash
        if( flSpec != null && (flSpec.instruction_EraseFlash != null || flSpec.instruction_EraseFlashPages != null) ) {
            try {
                // Unlock the flash as needed
                if( _swdIsFlashLocked(flSpec) ) _swdUnlockFlash(flSpec);
                // Erase the flash
                if(partialErase) _swdExecInst._exec(flSpec.instruction_EraseFlashPages);
                else             _swdExecInst._exec(flSpec.instruction_EraseFlash     );
             }
            catch(final USB2GPIO.TansmitError e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_ErsFlash, ProgClassName, _config.memoryFlash.driverName);
                return false;
            }
        }
        else {
            // ##### !!! TODO : How to implement the alternative methods? !!! #####
            return false;
        }

        // Set flag
        _chipErased = true;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int _flashMemoryTotalSize()
    { return _config.memoryFlash.totalSize; }

    @Override
    public byte _flashMemoryEmptyValue()
    { return _getFlashLoaderSpecifier().flashMemoryEmptyValue; }

    @Override
    public int _flashMemoryAlignWriteSize(final int numBytes)
    { return USB2GPIO.alignWriteSize(numBytes, _config.memoryFlash.pageSize); }

    @Override
    public int _eepromMemoryTotalSize()
    { return _config.memoryEEPROM.totalSize; }

    @Override
    public byte _eepromMemoryEmptyValue()
    { return _getFlashLoaderSpecifier().flashMemoryEmptyValue; }

    @Override
    public int[] _readDataBuff()
    { return _config.memoryFlash.readDataBuff; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private int _verifyReadFlash_directRead(final SWDFlashLoader.Specifier flSpec, final byte[] refData, final int sa, final int nb, final IntConsumer progressCallback)
    {
        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Determine the chunk size
        // ##### !!! TODO : It seems that if the chunk size is too large, the data read using SWD will be corrupted !!! #####
        // ##### ??? TODO : Optimize using '_swdRdCoreMem(startAddress, buff, transferSize)' ??? #####

        final int ChunkSize = Math.min(_config.memoryFlash.pageSize, flSpec.rdMaxSWDTransferSize);

        // Determine the number of chunks
        final boolean notAligned = (nb % ChunkSize) != 0;
        final int     numChunks  = (nb / ChunkSize) + (notAligned ? 1 : 0);

        // Read the bytes (and compare them if requested)
        final boolean verify = (refData != null);
              int     rdbIdx = 0;
              int     verIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk (flash can be read even without a page-aligned address)
            final int numReads = Math.min(ChunkSize, nb - rdbIdx);

            // Read the chunk bytes
            final int[] cbytes = new int[numReads];

            try {
                SysUtil.sleepMS(1); // ##### !!! TODO : It seems that without this delay, the SWD response will most likely be corrupted !!! #####
                _swdRdCoreMem(_config.memoryFlash.address + sa + c * ChunkSize, cbytes);
            }
            catch(final USB2GPIO.TansmitError e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName);
                return -1;
            }

            // Process the chunk bytes
            for(int b = 0; b < numReads; b += 2) {

                // Store the bytes to the result buffer
                _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b    ];
                _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b + 1];

                // Compare the bytes as needed
                if(verify && verIdx < refData.length) {
                    if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                    ++verIdx;
                    if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                    ++verIdx;
                }

                // Call the progress callback function for the current value
                pcb.callProgressCallbackCurrent(progressCallback, nb);

            } // for b

        } // for c

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return nb;
    }

    private int _verifyReadFlash_instBuff(final SWDFlashLoader.Specifier flSpec, final byte[] refData, final int sa, final int nb, final IntConsumer progressCallback)
    {
        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Determine the chunk size
        // ##### !!! TODO : It seems that if the chunk size is too large, the data read using SWD will be corrupted !!! #####
        // ##### ??? TODO : Optimize using '_swdRdCoreMem(startAddress, buff, transferSize)' and 'ib.rdCMem(..., ..., transferSize)' ??? #####
        final int ChunkSize = Math.min(_config.memoryFlash.pageSize, flSpec.rdMaxSWDTransferSize);

        // Determine the number of chunks
        final boolean notAligned = (nb % ChunkSize) != 0;
        final int     numChunks  = (nb / ChunkSize) + (notAligned ? 1 : 0);

        // Initialize the flash-reader thread
        final Thread frThread = new Thread() {
            @Override
            public void run() {
                try {
                    _swdExecInst._exec(flSpec.instruction_ReadFlash, flSpec.instruction_dataBuffFlash);
                }
                catch(final Exception e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                    // Notify error
                    USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName + ' ' + Texts._WorkerThread);
                    _swdExecInst._xviSet(flSpec.instruction_xviSignalJobState, SWDFlashLoader.XVI_SIGNAL_JOB_ERROR);
                }
            }
        };

        // Read the bytes (and compare them if requested)
        final boolean verify = (refData != null);
              int     rdbIdx = 0;
              int     verIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk (most flash can be read even without a page-aligned address)
            final int numReads = Math.min(ChunkSize, nb - rdbIdx);

            // Read the chunk bytes
            final int[] cbytes = new int[numReads];

            try {

                // Instruct the flash-reader thread to wait
                _swdExecInst._xviSet(flSpec.instruction_xviSignalWorkerCommand, SWDFlashLoader.XVI_SIGNAL_WORKER_WAIT);

                // In the first iteration, start the flash-reader thread
                if(c == 0) {
                    // Mark the flash-reader thread as not ready
                    _swdExecInst._xviSet(flSpec.instruction_xviSignalJobState, SWDFlashLoader.XVI_SIGNAL_JOB_WAIT);
                    // Start the thread
                    frThread.start();
                }

                // Wait until the flash-reader thread is ready
                while( _swdExecInst._xviGet(flSpec.instruction_xviSignalJobState) == SWDFlashLoader.XVI_SIGNAL_JOB_WAIT ) Thread.yield();

                // Set the flash address
                _swdExecInst._xviSet(flSpec.instruction_xviFlashEEPROMAddress , _config.memoryFlash.address + sa + c * ChunkSize);

                // Set the flash read size as needed
                if( !SWDExecInstOpcode.XVI.isNA(flSpec.instruction_xviFlashEEPROMReadSize) ) {
                    _swdExecInst._xviSet(flSpec.instruction_xviFlashEEPROMReadSize, numReads);
                }

                // Instruct the flash-reader thread to read the chunk and wait for the job to finish
               _swdExecInst._xviSet(flSpec.instruction_xviSignalWorkerCommand, SWDFlashLoader.XVI_SIGNAL_WORKER_EXECUTE);

                while(true) {
                    final long res = _swdExecInst._xviGet(flSpec.instruction_xviSignalJobState);
                         if(res == SWDFlashLoader.XVI_SIGNAL_JOB_COMPLETE   ) break;
                    else if(res == SWDFlashLoader.XVI_SIGNAL_JOB_ERROR      ) return -1;
                    else if(res == SWDFlashLoader.XVI_SIGNAL_JOB_IN_PROGRESS) {
                        Thread.yield();
                        continue;
                    }
                } // while

                // Mark the flash-reader thread as no longer ready
                _swdExecInst._xviSet(flSpec.instruction_xviSignalJobState, SWDFlashLoader.XVI_SIGNAL_JOB_WAIT);

                // Copy the data from the SRAM buffer or from the instruction data buffer
                if(flSpec.addrProgBuffer >= 0) _swdRdCoreMem(flSpec.addrProgBuffer, cbytes, numReads);
                else                           XCom.arrayCopy(cbytes, flSpec.instruction_dataBuffFlash, 0, numReads);

                // Process the chunk bytes
                for(int b = 0; b < numReads; b += 2) {

                    // Store the bytes to the result buffer
                    _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b    ];
                    _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b + 1];

                    // Compare the bytes as needed
                    if(verify && verIdx < refData.length) {
                        if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                        ++verIdx;
                        if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                        ++verIdx;
                    }

                    // Call the progress callback function for the current value
                    pcb.callProgressCallbackCurrent(progressCallback, nb);

                } // for b

            } // try
            catch(final USB2GPIO.TansmitError e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName + ' ' + Texts._MainThread);
                return -1;
            }

        } // for c

        // Instruct the flash-reader thread to exit
       _swdExecInst._xviSet(flSpec.instruction_xviSignalWorkerCommand, SWDFlashLoader.XVI_SIGNAL_WORKER_EXIT);

        try {
            frThread.join();
        }
        catch(final InterruptedException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            return -1;
        }

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return nb;
    }

    private int _verifyReadFlash_flProgram(final SWDFlashLoader.Specifier flSpec, final byte[] refData, final int sa, final int nb, final IntConsumer progressCallback)
    {
        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Determine the chunk size
        // ##### !!! TODO : It seems that if the chunk size is too large, the data read using SWD will be corrupted !!! #####
        // ##### ??? TODO : Optimize using '_swdRdCoreMem(startAddress, buff, transferSize)' ??? #####
        final int ChunkSize = Math.min(_config.memoryFlash.pageSize, flSpec.rdMaxSWDTransferSize);

        // Determine the number of chunks
        final boolean notAligned = (nb % ChunkSize) != 0;
        final int     numChunks  = (nb / ChunkSize) + (notAligned ? 1 : 0);

        // Read the bytes (and compare them if requested)
        final boolean verify = (refData != null);
              int     rdbIdx = 0;
              int     verIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk (flash can be read even without a page-aligned address)
            final int numReads = Math.min(ChunkSize, nb - rdbIdx);

            // Read the chunk bytes
            final int[] cbytes = new int[numReads];

            try {

                // Prepare the flash loader program
                _swd_haltCore(true);

                if(c == 0) {
                    // Write the program to the SRAM
                    _swdWrCoreMem(flSpec.addrProgStart, flSpec.flProgram);
                    // Set the extra parameters if they are specified
                    if(flSpec.rdProgExtraParams != null) {
                        int idx = 1;
                        for(final long pxp : flSpec.rdProgExtraParams) {
                            _swdWrCoreMem(flSpec.addrProgSignal + 4 * idx, pxp);
                            ++idx;
                        }
                    }
                }

                // Set the VTOR and XPSR
                _swdWrCoreMem(_config.cortexMReg.VTOR, flSpec.addrProgStart                            );
                _swdWrCoreReg(CoreReg.XPSR           , 0x01000000L                                     ); // Ensure the core runs in Thumb mode

                // Set the dynamic execution data
                _swdWrCoreReg(CoreReg.PC             , flSpec.flProgram[1] & 0xFFFFFFFEL               );
                _swdWrCoreReg(CoreReg.SP             , flSpec.flProgram[0]                             );
                _swdWrCoreReg(CoreReg.R0             , flSpec.addrProgBuffer                           );
                _swdWrCoreReg(CoreReg.R1             , _config.memoryFlash.address + sa + c * ChunkSize);
                _swdWrCoreReg(CoreReg.R2             , numReads                                        );
                _swdWrCoreReg(CoreReg.R3             , flSpec.addrProgSignal                           );
                _swdWrCoreMem(flSpec.addrProgSignal  ,  0                                              );

                // Read the chunk by executing the program, wait until it is completed, and check for error
                _swd_unhaltCore(false, true);

                long status = 0;

                while(status == 0) {
                    status = (long) (int) _swdRdCoreMem(flSpec.addrProgSignal);
                    if(status < 0) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_RdFlashM, ProgClassName, _config.memoryFlash.driverName, status);
                }

                // Copy the data from the SRAM buffer
                _swdRdCoreMem(flSpec.addrProgBuffer, cbytes);

            } // try
            catch(final USB2GPIO.TansmitError e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName);
                return -1;
            }

            // Process the chunk bytes
            for(int b = 0; b < numReads; b += 2) {

                // Store the bytes to the result buffer
                _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b    ];
                _config.memoryFlash.readDataBuff[rdbIdx++] = cbytes[b + 1];

                // Compare the bytes as needed
                if(verify && verIdx < refData.length) {
                    if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                    ++verIdx;
                    if( _config.memoryFlash.readDataBuff[verIdx] != (refData[verIdx] & 0xFF) ) return verIdx;
                    ++verIdx;
                }

                // Call the progress callback function for the current value
                pcb.callProgressCallbackCurrent(progressCallback, nb);

            } // for b

        } // for c

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return nb;
    }

    private int _verifyReadFlash(final byte[] data, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    {
        // ##### !!! How about MCU with a dual bank flash? !!! #####

        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Determine the start address and number of bytes
        long sa = (startAddress < 0) ? _config.memoryFlash.address   : startAddress;
        int  nb = (numBytes     < 0) ? _config.memoryFlash.totalSize : numBytes;

        if(sa >= _config.memoryFlash.address) sa -= _config.memoryFlash.address;

        // Check the start address and number of bytes
        if( !USB2GPIO.checkStartAddressAndNumberOfBytes_even( (int) sa, nb, _config.memoryFlash.totalSize, ProgClassName ) ) return -1;

        // Get the flash loader specification
        final SWDFlashLoader.Specifier flSpec = _getFlashLoaderSpecifier();

        // If 'flSpec.instruction_xviFlashEEPROMReadSize' is N/A then the read must be done in the multiple of page size
        if( SWDExecInstOpcode.XVI.isNA(flSpec.instruction_xviFlashEEPROMReadSize) ) {
            // Align the number of bytes and pad the buffer as needed
            final USB2GPIO.ANBResult anbr = USB2GPIO.alignNumberOfBytesAndPadBuffer( data, (int) sa, nb, _config.memoryFlash.pageSize, _config.memoryFlash.totalSize, flSpec.flashMemoryEmptyValue, ProgClassName );
            if(anbr == null) return -1;
            // Check the start address and number of bytes as needed
            nb = anbr.nb;
            if( !USB2GPIO.checkStartAddressAndNumberOfBytes_pageSize( (int) sa, nb, _config.memoryFlash.pageSize, ProgClassName ) ) return -1;
        }

        // Prepare the result buffer
        if(_config.memoryFlash.readDataBuff == null || _config.memoryFlash.readDataBuff.length != numBytes) {
            _config.memoryFlash.readDataBuff = new int[nb];
        }

        // Initialize the system
        if( !_initSystemOnce(flSpec) ) return -1;

        // Read the flash
        int res = -1;

        if(flSpec != null && flSpec.supportDirectFlashRead) {
            // Read the flash using the direct-flash-read if it is supported
            res = _verifyReadFlash_directRead( flSpec, data, (int) sa, nb, progressCallback );
        }
        else if(flSpec != null && flSpec.instruction_ReadFlash != null) {
            // Read the flash using the instruction buffer if it is specified
            res = _verifyReadFlash_instBuff( flSpec, data, (int) sa, nb, progressCallback );
        }
        else if(flSpec != null && flSpec.flProgram != null) {
            // Read the flash using the flash loader program if it is specified
            res = _verifyReadFlash_flProgram( flSpec, data, (int) sa, nb, progressCallback );
        }

        // Done
        return (res == nb) ? numBytes : res;
    }

    @Override
    public boolean readFlash(final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash(null, startAddress, numBytes, progressCallback) == numBytes; }

    @Override
    public int verifyFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return _verifyReadFlash(refData, startAddress, numBytes, progressCallback); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _writeFlash_instBuff(final SWDFlashLoader.Specifier flSpec, final byte[] data, final int sa, final int nb, final IntConsumer progressCallback)
    {
        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Clear flag
        _chipErased = false;

        // Determine the chunk size
        // ##### !!! TODO : It seems that if the chunk size is too large, the data written using SWD will be corrupted !!! #####
        // ##### ??? TODO : Optimize using '_swdWrCoreMem(startAddress, buff, transferSize)' and 'ib.wrCMem(..., ..., transferSize)' ??? #####
        final int ChunkSize = Math.min(_config.memoryFlash.pageSize, flSpec.wrMaxSWDTransferSize);

        // Get the number of chunks to be written and the current page address
        final int numChunks = nb / ChunkSize;
              int cpgAddr   = sa;

        // Initialize the flash-writer thread
        final Thread fwThread = new Thread() {
            @Override
            public void run() {
                try {
                    _swdExecInst._exec(flSpec.instruction_WriteFlash, flSpec.instruction_dataBuffFlash);
                }
                catch(final Exception e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                    // Notify error
                    USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName + ' ' + Texts._WorkerThread);
                    _swdExecInst._xviSet(flSpec.instruction_xviSignalJobState, SWDFlashLoader.XVI_SIGNAL_JOB_ERROR);
                }
            }
        };

        // Write the chunks
        int datIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            try {

                // ##### !!! TODO : Skip writing the page if it is blank (its contents are all 'flSpec.flashMemoryEmptyValue') !!! #####

                // Instruct the flash-writer thread to wait
                _swdExecInst._xviSet(flSpec.instruction_xviSignalWorkerCommand, SWDFlashLoader.XVI_SIGNAL_WORKER_WAIT);

                // Unlock the flash as needed
                if( _swdIsFlashLocked(flSpec) ) _swdUnlockFlash(flSpec);

                // In the first iteration, start the flash-writer thread
                if(c == 0) {
                    // Mark the flash-writer thread as not ready
                    _swdExecInst._xviSet(flSpec.instruction_xviSignalJobState, SWDFlashLoader.XVI_SIGNAL_JOB_WAIT);
                    // Start the thread
                    fwThread.start();
                }

                // Wait until the flash-writer thread is ready
                while( _swdExecInst._xviGet(flSpec.instruction_xviSignalJobState) == SWDFlashLoader.XVI_SIGNAL_JOB_WAIT ) Thread.yield();

                // Copy the data to the SRAM buffer or to the instruction data buffer
                if(flSpec.addrProgBuffer >= 0) _swdWrCoreMem( flSpec.addrProgBuffer, USB2GPIO.ba2ia(data, datIdx, ChunkSize) );
                else                           USB2GPIO.ba2ia(flSpec.instruction_dataBuffFlash, data, datIdx, ChunkSize);

                // Set the flash address
                _swdExecInst._xviSet(flSpec.instruction_xviFlashEEPROMAddress, _config.memoryFlash.address + cpgAddr);

                /*
                // ##### ??? TODO : Is this really no longer needed ??? #####
                SysUtil.sleepUS(1);
                //*/

                // Instruct the flash-writer thread to write the chunk and wait for the job to finish
               _swdExecInst._xviSet(flSpec.instruction_xviSignalWorkerCommand, SWDFlashLoader.XVI_SIGNAL_WORKER_EXECUTE);

                while(true) {
                    final long res = _swdExecInst._xviGet(flSpec.instruction_xviSignalJobState);
                         if(res == SWDFlashLoader.XVI_SIGNAL_JOB_COMPLETE   ) break;
                    else if(res == SWDFlashLoader.XVI_SIGNAL_JOB_ERROR      ) return false;
                    else if(res == SWDFlashLoader.XVI_SIGNAL_JOB_IN_PROGRESS) {
                        Thread.yield();
                        continue;
                    }
                } // while

                // Mark the flash-writer thread as no longer ready
                _swdExecInst._xviSet(flSpec.instruction_xviSignalJobState, SWDFlashLoader.XVI_SIGNAL_JOB_WAIT);

            } // try
            catch(final USB2GPIO.TansmitError e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName + ' ' + Texts._MainThread);
                return false;
            }

            // Call the progress callback function for the current value as many times as necessary
            pcb.callProgressCallbackCurrentMulti(progressCallback, nb, ChunkSize / 2);

            // Increment the counters
            cpgAddr += ChunkSize;
            datIdx  += ChunkSize;

        } // for c

        // Instruct the flash-writer thread to exit
       _swdExecInst._xviSet(flSpec.instruction_xviSignalWorkerCommand, SWDFlashLoader.XVI_SIGNAL_WORKER_EXIT);

        try {
            fwThread.join();
        }
        catch(final InterruptedException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            return false;
        }

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return true;
    }

    private boolean _writeFlash_flProgram(final SWDFlashLoader.Specifier flSpec, final byte[] data, final int sa, final int nb, final IntConsumer progressCallback)
    {
        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, nb);

        // Clear flag
        _chipErased = false;

        // Determine the chunk size
        // ##### !!! TODO : It seems that if the chunk size is too large, the data written using SWD will be corrupted !!! #####
        // ##### ??? TODO : Optimize using '_swdWrCoreMem(startAddress, buff, transferSize)' ??? #####
        final int pageSize  = _config.memoryFlash.pageSize / (_config.memoryFlash.wrHalfPage ? 2 : 1);
        final int ChunkSize = Math.min(pageSize, flSpec.wrMaxSWDTransferSize);

        // Get the number of chunks per page
        final int numChunks = USB2GPIO.checkPageSize_chunkSize(pageSize, ChunkSize, ProgClassName);

        if(numChunks < 1) return false;

        // Get the number of pages to be written and the current page address
        final int numPages = nb / pageSize;
              int cpgAddr  = sa;

        // Write the pages
        int datIdx = 0;

        for(int p = 0; p < numPages; ++p) {

            try {

                // ##### !!! TODO : Skip writing the page if it is blank (its contents are all 'flSpec.flashMemoryEmptyValue') !!! #####

                // Prepare the flash loader program
                _swd_haltCore(true);

                if(p == 0) {
                    // Write the program to the SRAM
                    _swdWrCoreMem(flSpec.addrProgStart, flSpec.flProgram);
                    // Set the extra parameters if they are specified
                    if(flSpec.wrProgExtraParams != null) {
                        int idx = 1;
                        for(final long pxp : flSpec.wrProgExtraParams) {
                            _swdWrCoreMem(flSpec.addrProgSignal + 4 * idx, pxp);
                            ++idx;
                        }
                    }
                }

                // Set the VTOR and XPSR
                _swdWrCoreMem(_config.cortexMReg.VTOR, flSpec.addrProgStart                 );
                _swdWrCoreReg(CoreReg.XPSR           , 0x01000000L                          ); // Ensure the core runs in Thumb mode

                // Set the dynamic execution data
                _swdWrCoreReg(CoreReg.PC             , flSpec.flProgram[1] & 0xFFFFFFFEL    );
                _swdWrCoreReg(CoreReg.SP             , flSpec.flProgram[0]                  );
                _swdWrCoreReg(CoreReg.R0             , _config.memoryFlash.address + cpgAddr);
                _swdWrCoreReg(CoreReg.R1             , flSpec.addrProgBuffer                );
                _swdWrCoreReg(CoreReg.R2             , pageSize                             );
                _swdWrCoreReg(CoreReg.R3             , flSpec.addrProgSignal                );
                _swdWrCoreMem(flSpec.addrProgSignal  , 0                                    );

                // Copy the data to the SRAM buffer
                if(numChunks > 1) {
                    for(int c = 0; c < numChunks; ++c) {
                        final int offset = c * ChunkSize;
                        _swdWrCoreMem( flSpec.addrProgBuffer + offset, USB2GPIO.ba2ia(data, datIdx + offset, ChunkSize ) );
                    }
                }
                else {
                        _swdWrCoreMem( flSpec.addrProgBuffer         , USB2GPIO.ba2ia(data, datIdx         , pageSize  ) );
                }

                // Write the page by executing the program, wait until it is completed, and check for error
                _swd_unhaltCore(false, true);

                long status = 0;

                while(status == 0) {
                    status = (long) (int) _swdRdCoreMem(flSpec.addrProgSignal);
                    if(status < 0) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_WrFlashM, ProgClassName, _config.memoryFlash.driverName, status);
                }

            } // try
            catch(final USB2GPIO.TansmitError e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName);
                return false;
            }

            // Call the progress callback function for the current value as many times as necessary
            pcb.callProgressCallbackCurrentMulti(progressCallback, nb, pageSize / 2);

            // Increment the counters
            cpgAddr += pageSize;
            datIdx  += pageSize;

        } // for p

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, nb);

        // Done
        return true;
    }

    @Override
    public boolean writeFlash(final byte[] data, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    {
        // ##### !!! How about MCU with a dual bank flash? !!! #####

        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Get the flash loader specification
        final SWDFlashLoader.Specifier flSpec = _getFlashLoaderSpecifier();

        // Determine the start address and number of bytes
              long sa = (startAddress < 0) ? _config.memoryFlash.address   : startAddress;
        final int  nb = (numBytes     < 0) ? _config.memoryFlash.totalSize : numBytes;

        if(sa >= _config.memoryFlash.address) sa -= _config.memoryFlash.address;

        // Align the number of bytes and pad the buffer as needed
        final USB2GPIO.ANBResult anbr = USB2GPIO.alignNumberOfBytesAndPadBuffer( data, (int) sa, nb, _config.memoryFlash.pageSize, _config.memoryFlash.totalSize, flSpec.flashMemoryEmptyValue, ProgClassName );
        if(anbr == null) return false;

        // Check the start address and number of bytes
        if( !USB2GPIO.checkStartAddressAndNumberOfBytes_pageSize( (int) sa, anbr.nb, _config.memoryFlash.pageSize, ProgClassName ) ) return false;

        // Initialize the system
        if( !_initSystemOnce(flSpec) ) return false;

        // Write the flash
        if(flSpec != null && flSpec.instruction_WriteFlash != null) {
            // Read the flash using the instruction buffer if it is specified
            return _writeFlash_instBuff( flSpec, anbr.buff, (int) sa, anbr.nb, progressCallback );
        }
        else if(flSpec != null && flSpec.flProgram != null) {
            // Write the flash using the flash loader program if it is specified
            return _writeFlash_flProgram( flSpec, anbr.buff, (int) sa, anbr.nb, progressCallback );
        }

        // Not done
        return false;
    }

    // ##### !!! TODO : How about the OTP flash? !!! #####

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # Many ARM MCUs do not have an embedded EEPROM.
     *        # Only a few ARM MCUs (such as STM32L[0|1]* and R*A4M1*) have embedded EEPROM.
     */
    private int[]     _eepromBuffer = null;
    private boolean[] _eepromFDirty = null;

    private boolean _readEEPROM_directRead(final SWDFlashLoader.Specifier flSpec)
    {
        // Determine the chunk size and the number of chunks
        // ##### !!! TODO : It seems that if the chunk size is too large, the data read using SWD will be corrupted !!! #####
        // ##### ??? TODO : Optimize using '_swdRdCoreMem(startAddress, buff, transferSize)' ??? #####
        final int ChunkSize = Math.min(_config.memoryEEPROM.pageSize, flSpec.rdMaxSWDTransferSize);
        final int numChunks = (_config.memoryEEPROM.totalSize / ChunkSize);

        // Read the bytes
        int rdbIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk
            final int numReads = Math.min(ChunkSize, _config.memoryEEPROM.totalSize - rdbIdx);

            // Read the chunk bytes
            final int[] cbytes = new int[numReads];

            try {
                SysUtil.sleepMS(1); // ##### !!! TODO : It seems that without this delay, the SWD response will most likely be corrupted !!! #####
                _swdRdCoreMem(_config.memoryEEPROM.address + c * ChunkSize, cbytes);
            }
            catch(final USB2GPIO.TansmitError e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName);
                return false;
            }

            // Store the bytes to the result buffer
            for(int b = 0; b < numReads; ++b) _eepromBuffer[rdbIdx++] = cbytes[b];

        } // for c

        // Done
        return true;
    }

    private boolean _readEEPROM_instBuff(final SWDFlashLoader.Specifier flSpec)
    {
        // Determine the chunk size
        // ##### !!! TODO : It seems that if the chunk size is too large, the data read using SWD will be corrupted !!! #####
        // ##### ??? TODO : Optimize using '_swdRdCoreMem(startAddress, buff, transferSize)' and 'ib.rdCMem(..., ..., transferSize)' ??? #####
        final int ChunkSize = Math.min(_config.memoryEEPROM.pageSize, flSpec.rdMaxSWDTransferSize);

        // Get the number of chunks to be read and the current page address
        final int numChunks = _config.memoryEEPROM.totalSize / ChunkSize;
              int cpgAddr   = 0;

        // Initialize the EEPROM-reader thread
        final Thread erThread = new Thread() {
            @Override
            public void run() {
                try {
                    _swdExecInst._exec(flSpec.instruction_ReadEEPROM, flSpec.instruction_dataBuffEEPROM);
                }
                catch(final Exception e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                    // Notify error
                    USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName + ' ' + Texts._WorkerThread);
                    _swdExecInst._xviSet(flSpec.instruction_xviSignalJobState, SWDFlashLoader.XVI_SIGNAL_JOB_ERROR);
                }
            }
        };

        // Write the chunks
        int rdbIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read the chunk bytes
            final int[] cbytes = new int[ChunkSize];

            try {

                // Instruct the EEPROM-reader thread to wait
                _swdExecInst._xviSet(flSpec.instruction_xviSignalWorkerCommand, SWDFlashLoader.XVI_SIGNAL_WORKER_WAIT);

                // In the first iteration, start the EEPROM-reader thread
                if(c == 0) {
                    // Mark the EEPROM-reader thread as not ready
                    _swdExecInst._xviSet(flSpec.instruction_xviSignalJobState, SWDFlashLoader.XVI_SIGNAL_JOB_WAIT);
                    // Start the thread
                    erThread.start();
                }

                // Wait until the EEPROM-reader thread is ready
                while( _swdExecInst._xviGet(flSpec.instruction_xviSignalJobState) == SWDFlashLoader.XVI_SIGNAL_JOB_WAIT ) Thread.yield();

                // Set the EEPROM address
                _swdExecInst._xviSet(flSpec.instruction_xviFlashEEPROMAddress, _config.memoryEEPROM.address + cpgAddr);

                // Set the EEPROM read size as needed
                if( !SWDExecInstOpcode.XVI.isNA(flSpec.instruction_xviFlashEEPROMReadSize) ) {
                    _swdExecInst._xviSet(flSpec.instruction_xviFlashEEPROMReadSize, ChunkSize);
                }

                // Instruct the EEPROM-reader thread to read the chunk and wait for the job to finish
               _swdExecInst._xviSet(flSpec.instruction_xviSignalWorkerCommand, SWDFlashLoader.XVI_SIGNAL_WORKER_EXECUTE);

                while(true) {
                    final long res = _swdExecInst._xviGet(flSpec.instruction_xviSignalJobState);
                         if(res == SWDFlashLoader.XVI_SIGNAL_JOB_COMPLETE   ) break;
                    else if(res == SWDFlashLoader.XVI_SIGNAL_JOB_ERROR      ) return false;
                    else if(res == SWDFlashLoader.XVI_SIGNAL_JOB_IN_PROGRESS) {
                        Thread.yield();
                        continue;
                    }
                } // while

                // Mark the EEPROM-reader thread as no longer ready
                _swdExecInst._xviSet(flSpec.instruction_xviSignalJobState, SWDFlashLoader.XVI_SIGNAL_JOB_WAIT);

                // Copy the data from the SRAM buffer or from the instruction data buffer
                if(flSpec.addrProgBuffer >= 0) _swdRdCoreMem(flSpec.addrProgBuffer, cbytes, ChunkSize);
                else                           XCom.arrayCopy(cbytes, flSpec.instruction_dataBuffEEPROM, 0, ChunkSize);

                // Store the bytes to the result buffer
                for(int b = 0; b < ChunkSize; ++b) _eepromBuffer[rdbIdx++] = cbytes[b];

            } // try
            catch(final USB2GPIO.TansmitError e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName + ' ' + Texts._MainThread);
                return false;
            }

            // Increment the counters
            cpgAddr += ChunkSize;

        } // for c

        // Instruct the EEPROM-reader thread to exit
       _swdExecInst._xviSet(flSpec.instruction_xviSignalWorkerCommand, SWDFlashLoader.XVI_SIGNAL_WORKER_EXIT);

        try {
            erThread.join();
        }
        catch(final InterruptedException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            return false;
        }

        // Done
        return true;
    }

    private boolean _readEEPROM_elProgram(final SWDFlashLoader.Specifier flSpec)
    {
        // Determine the chunk size and the number of chunks
        // ##### !!! TODO : It seems that if the chunk size is too large, the data read using SWD will be corrupted !!! #####
        // ##### ??? TODO : Optimize using '_swdRdCoreMem(startAddress, buff, transferSize)' ??? #####
        final int ChunkSize = Math.min(_config.memoryEEPROM.pageSize, flSpec.rdMaxSWDTransferSize);
        final int numChunks = (_config.memoryEEPROM.totalSize / ChunkSize);

        // Read the bytes
        int rdbIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            // Read in chunk
            final int numReads = Math.min(ChunkSize, _config.memoryEEPROM.totalSize - rdbIdx);

            // Read the chunk bytes
            final int[] cbytes = new int[numReads];

            try {

                // Prepare the EEPROM loader program
                _swd_haltCore(true);

                if(c == 0) {
                    // Write the program to the SRAM
                    _swdWrCoreMem(flSpec.addrProgStart, flSpec.elProgram);
                    // Set the extra parameters if they are specified
                    if(flSpec.rdProgExtraParams != null) {
                        int idx = 1;
                        for(final long pxp : flSpec.rdProgExtraParams) {
                            _swdWrCoreMem(flSpec.addrProgSignal + 4 * idx, pxp);
                            ++idx;
                        }
                    }
                }

                // Set the VTOR and XPSR
                _swdWrCoreMem(_config.cortexMReg.VTOR, flSpec.addrProgStart                        );
                _swdWrCoreReg(CoreReg.XPSR           , 0x01000000L                                 ); // Ensure the core runs in Thumb mode

                // Set the dynamic execution data
                _swdWrCoreReg(CoreReg.PC             , flSpec.elProgram[1] & 0xFFFFFFFEL           );
                _swdWrCoreReg(CoreReg.SP             , flSpec.elProgram[0]                         );
                _swdWrCoreReg(CoreReg.R0             , flSpec.addrProgBuffer                       );
                _swdWrCoreReg(CoreReg.R1             , _config.memoryEEPROM.address + c * ChunkSize);
                _swdWrCoreReg(CoreReg.R2             , numReads                                    );
                _swdWrCoreReg(CoreReg.R3             , flSpec.addrProgSignal                       );
                _swdWrCoreMem(flSpec.addrProgSignal  , 0                                           );

                // Read the chunk by executing the program, wait until it is completed, and check for error
                _swd_unhaltCore(false, true);

                long status = 0;

                while(status == 0) {
                    status = (long) (int) _swdRdCoreMem(flSpec.addrProgSignal);
                    if(status < 0) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_RdEEPROM, ProgClassName, _config.memoryFlash.driverName, status);
                }

                // Copy the data from the SRAM buffer
                _swdRdCoreMem(flSpec.addrProgBuffer, cbytes);

            } // try
            catch(final USB2GPIO.TansmitError e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName);
                return false;
            }

            // Store the bytes to the result buffer
            for(int b = 0; b < numReads; ++b) _eepromBuffer[rdbIdx++] = cbytes[b];

        } // for c

        // Done
        return true;
    }

    private boolean _readAllEEPROMBytes()
    {
        // Prepare the result buffer
        if(_eepromBuffer == null) _eepromBuffer = new int[_config.memoryEEPROM.totalSize];

        // Get the flash loader specification
        final SWDFlashLoader.Specifier flSpec = _getFlashLoaderSpecifier();

        // Initialize the system
        if( !_initSystemOnce(flSpec) ) return false;

        // Read the EEPROM
        if(flSpec != null && flSpec.supportDirectEEPROMRead) {
            // Read the EEPROM using the direct-EEPROM-read if it is supported
            return _readEEPROM_directRead(flSpec);
        }
        else if(flSpec != null && flSpec.instruction_ReadEEPROM != null) {
            // Read the EEPROM using the instruction buffer if it is specified
            return _readEEPROM_instBuff(flSpec);
        }
        else if(flSpec != null && flSpec.elProgram != null) {
            // Read the EEPROM using the EEPROM loader program if it is specified
            return _readEEPROM_elProgram(flSpec);
        }

        // Not done
        return false;
    }

    @Override
    public int readEEPROM(final int address)
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Error if EEPROM is not available
        if(_config.memoryEEPROM.totalSize <= 0) {
            USB2GPIO.notifyError(Texts.ProgXXX_ENotAvailable, ProgClassName);
            return -1;
        }

        // Check the address
        if(address < 0 || address >= _config.memoryEEPROM.totalSize) {
            USB2GPIO.notifyError(Texts.ProgXXX_EAddrOoR, ProgClassName);
            return -1;
        }

        // Read the entire EEPROM as needed
        if(_eepromBuffer == null) {
            // Read the bytes
            if( !_readAllEEPROMBytes() ) return -1;
            // Mark everything as not dirty
            _eepromFDirty = new boolean[_config.memoryEEPROM.totalSize];
            Arrays.fill(_eepromFDirty, false);
        }

        // Return the byte
        return _eepromBuffer[address];
    }

    @Override
    public boolean writeEEPROM(final int address, final byte data)
    {
        // Error if not in programming mode
        if(!_inProgMode) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Error if EEPROM is not available
        if(_config.memoryEEPROM.totalSize <= 0) return USB2GPIO.notifyError(Texts.ProgXXX_ENotAvailable, ProgClassName);

        // Check the address
        if(address < 0 || address >= _config.memoryEEPROM.totalSize) return USB2GPIO.notifyError(Texts.ProgXXX_EAddrOoR, ProgClassName);

        // The EEPROM must be written in page mode, hence, read the entire EEPROM first as needed
        if(_eepromBuffer == null) {
            if( readEEPROM(0) < 0 ) return false;
        }

        // Store the new byte to the buffer and mark the position as dirty
        final int newData = data & 0xFF;

        if(_eepromBuffer[address] != newData) {
            _eepromBuffer[address] = newData;
            _eepromFDirty[address] = true;
        }

        // Done
        return true;
    }

    private boolean _writeEEPROM_instBuff(final SWDFlashLoader.Specifier flSpec)
    {
        // Determine the chunk size, the number of chunks per page, and the total number of chunks
        // ##### !!! TODO : It seems that if the chunk size is too large, the data written using SWD will be corrupted !!! #####
        // ##### ??? TODO : Optimize using '_swdWrCoreMem(startAddress, buff, transferSize)' and 'ib.wrCMem(..., ..., transferSize)' ??? #####
        final int ChunkSize     = Math.min(_config.memoryEEPROM.pageSize, flSpec.wrMaxSWDTransferSize);
        final int ChunksPerPage = _config.memoryEEPROM.pageSize / ChunkSize;
        final int numChunks     = (_config.memoryEEPROM.totalSize / ChunkSize);

        // Initialize the current page address
        int cpgAddr = 0;

        // Initialize the EEPROM-writer thread
        final Thread ewThread = new Thread() {
            @Override
            public void run() {
                try {
                    _swdExecInst._exec(flSpec.instruction_WriteEEPROM, flSpec.instruction_dataBuffEEPROM);
                }
                catch(final Exception e) {
                    // Print the stack trace if requested
                    if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                    // Notify error
                    USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName + ' ' + Texts._WorkerThread);
                    _swdExecInst._xviSet(flSpec.instruction_xviSignalJobState, SWDFlashLoader.XVI_SIGNAL_JOB_ERROR);
                }
            }
        };

        // Write the chunks
        int datIdx = 0;

        for(int c = 0; c < numChunks; ++c) {

            try {

                // Instruct the EEPROM-writer thread to wait
                _swdExecInst._xviSet(flSpec.instruction_xviSignalWorkerCommand, SWDFlashLoader.XVI_SIGNAL_WORKER_WAIT);

                // Unlock the EEPROM as needed
                if( _swdIsEEPROMLocked(flSpec) ) _swdUnlockEEPROM(flSpec);

                // In the first iteration, start the EEPROM-writer thread
                if(c == 0) {
                    // Mark the EEPROM-writer thread as not ready
                    _swdExecInst._xviSet(flSpec.instruction_xviSignalJobState, SWDFlashLoader.XVI_SIGNAL_JOB_WAIT);
                    // Start the thread
                    ewThread.start();
                }

                // Wait until the EEPROM-writer thread is ready
                while( _swdExecInst._xviGet(flSpec.instruction_xviSignalJobState) == SWDFlashLoader.XVI_SIGNAL_JOB_WAIT ) Thread.yield();

                // Check if the chunk is dirty
                boolean chunkDirty = false;

                for(int b = 0; b < ChunkSize; ++b) {
                    if(_eepromFDirty[c * ChunkSize + b]) {
                        chunkDirty = true;
                        break;
                    }
                }

                // Only write if the chunk is dirty
                if(chunkDirty) {

                    // Copy the data to the SRAM buffer or to the instruction data buffer
                    if(flSpec.addrProgBuffer >= 0) _swdWrCoreMem( flSpec.addrProgBuffer, XCom.arrayCopy(_eepromBuffer, datIdx, ChunkSize) );
                    else                           XCom.arrayCopy(flSpec.instruction_dataBuffEEPROM, _eepromBuffer, datIdx, ChunkSize);

                    // Set the EEPROM address
                    _swdExecInst._xviSet(flSpec.instruction_xviFlashEEPROMAddress, _config.memoryEEPROM.address + cpgAddr);

                    // Instruct the EEPROM-writer thread to write the chunk and wait for the job to finish
                    _swdExecInst._xviSet(flSpec.instruction_xviSignalWorkerCommand, SWDFlashLoader.XVI_SIGNAL_WORKER_EXECUTE);

                    while(true) {
                        final long res = _swdExecInst._xviGet(flSpec.instruction_xviSignalJobState);
                             if(res == SWDFlashLoader.XVI_SIGNAL_JOB_COMPLETE   ) break;
                        else if(res == SWDFlashLoader.XVI_SIGNAL_JOB_ERROR      ) return false;
                        else if(res == SWDFlashLoader.XVI_SIGNAL_JOB_IN_PROGRESS) {
                            Thread.yield();
                            continue;
                        }
                    } // while

                    // Mark the EEPROM-writer thread as no longer ready
                    _swdExecInst._xviSet(flSpec.instruction_xviSignalJobState, SWDFlashLoader.XVI_SIGNAL_JOB_WAIT);

                } // if chunkDirty

            } // try
            catch(final USB2GPIO.TansmitError e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName + ' ' + Texts._MainThread);
                return false;
            }

            // Increment the counters
            cpgAddr += ChunkSize;
            datIdx  += ChunkSize;

        } // for c

        // Instruct the EEPROM-writer thread to exit
       _swdExecInst._xviSet(flSpec.instruction_xviSignalWorkerCommand, SWDFlashLoader.XVI_SIGNAL_WORKER_EXIT);

        try {
            ewThread.join();
        }
        catch(final InterruptedException e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            return false;
        }

        // Done
        return true;
    }

    private boolean _writeEEPROM_elProgram(final SWDFlashLoader.Specifier flSpec)
    {
        // Determine the chunk size
        // ##### !!! TODO : It seems that if the chunk size is too large, the data written using SWD will be corrupted !!! #####
        // ##### ??? TODO : Optimize using '_swdWrCoreMem(startAddress, buff, transferSize)' ??? #####
        final int ChunkSize = Math.min(_config.memoryEEPROM.pageSize, flSpec.wrMaxSWDTransferSize);

        // Get the number of chunks per page
        final int numChunks = USB2GPIO.checkPageSize_chunkSize(_config.memoryEEPROM.pageSize, ChunkSize, ProgClassName);

        if(numChunks < 1) return false;

        // Get the number of pages to be written and the current page address
        final int numPages = _config.memoryEEPROM.numPages;
              int cpgAddr  = 0;

        // Write the pages
        int datIdx = 0;

        for(int p = 0; p < numPages; ++p) {

            try {

                // Prepare the EEPROM loader program
                _swd_haltCore(true);

                if(p == 0) {
                    // Write the program to the SRAM
                    _swdWrCoreMem(flSpec.addrProgStart, flSpec.elProgram);
                    // Set the extra parameters if they are specified
                    if(flSpec.wrProgExtraParams != null) {
                        int idx = 1;
                        for(final long pxp : flSpec.wrProgExtraParams) {
                            _swdWrCoreMem(flSpec.addrProgSignal + 4 * idx, pxp);
                            ++idx;
                        }
                    }
                }

                // Check if the page is dirty
                boolean pageDirty = false;

                for(int b = 0; b < _config.memoryEEPROM.pageSize; ++b) {
                    if(_eepromFDirty[p * _config.memoryEEPROM.pageSize + b]) {
                        pageDirty = true;
                        break;
                    }
                }

                // Only write if the page is dirty
                if(pageDirty) {

                    // Set the VTOR and XPSR
                    _swdWrCoreMem(_config.cortexMReg.VTOR, flSpec.addrProgStart                  );
                    _swdWrCoreReg(CoreReg.XPSR           , 0x01000000L                           ); // Ensure the core runs in Thumb mode

                    // Set the dynamic execution data
                    _swdWrCoreReg(CoreReg.PC             , flSpec.elProgram[1] & 0xFFFFFFFEL     );
                    _swdWrCoreReg(CoreReg.SP             , flSpec.elProgram[0]                   );
                    _swdWrCoreReg(CoreReg.R0             , _config.memoryEEPROM.address + cpgAddr);
                    _swdWrCoreReg(CoreReg.R1             , flSpec.addrProgBuffer                 );
                    _swdWrCoreReg(CoreReg.R2             , _config.memoryEEPROM.pageSize         );
                    _swdWrCoreReg(CoreReg.R3             , flSpec.addrProgSignal                 );
                    _swdWrCoreMem(flSpec.addrProgSignal  , 0                                     );

                    // Copy the data to the SRAM buffer
                    if(numChunks > 1) {
                        for(int c = 0; c < numChunks; ++c) {
                            final int offset = c * ChunkSize;
                            _swdWrCoreMem( flSpec.addrProgBuffer + offset, XCom.arrayCopy(_eepromBuffer, datIdx + offset, ChunkSize                     ) );
                        }
                    }
                    else {
                            _swdWrCoreMem( flSpec.addrProgBuffer         , XCom.arrayCopy(_eepromBuffer, datIdx         , _config.memoryEEPROM.pageSize ) );
                    }

                    // Write the page by executing the program, wait until it is completed, and check for error
                    _swd_unhaltCore(false, true);

                    long status = 0;

                    while(status == 0) {
                        status = (long) (int) _swdRdCoreMem(flSpec.addrProgSignal);
                        if(status < 0) USB2GPIO.TansmitError.throwTansmitError(Texts.ProgXXX_FailSWD_WrFlashM, ProgClassName, _config.memoryFlash.driverName, status);
                    }

                } // if pageDirty

            } // try
            catch(final USB2GPIO.TansmitError e) {
                // Print the stack trace if requested
                if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
                // Notify error
                USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName);
                return false;
            }

            // Increment the counters
            cpgAddr += _config.memoryEEPROM.pageSize;
            datIdx  += _config.memoryEEPROM.pageSize;

        } // for p

        // Done
        return true;
    }

    public boolean commitEEPROM()
    {
        // Simply exit if there is no EEPROM buffer
        if(_eepromBuffer == null) return true;

        // Get the flash loader specification
        final SWDFlashLoader.Specifier flSpec = _getFlashLoaderSpecifier();

        // Initialize the system
        if( !_initSystemOnce(flSpec) ) return false;

        // Write the EEPROM
        if(flSpec != null && flSpec.instruction_WriteEEPROM != null) {
            // Read the EEPROM using the instruction buffer if it is specified
            if( !_writeEEPROM_instBuff(flSpec) ) return false;
        }
        else if(flSpec != null && flSpec.elProgram != null) {
            // Write the EEPROM using the EEPROM loader program if it is specified
            if( !_writeEEPROM_elProgram(flSpec) ) return false;
        }

        // Mark everything as not dirty
        Arrays.fill(_eepromFDirty, false);

        /*
        _eepromBuffer = null;
        //*/

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # Fuses are emulated by ProgSWD because ARM MCUs have many variations on how configuration bits
     *          and/or security bits are implemented.
     *        # Fuses are configuration bits and/or security bits that are NOT related to the overall/full read
     *          and/or write protection of the MCUs' memories via SWD.
     *        # Some ARM MCUs may have only partial support for configuration bits and/or security bits; some
     *          of them may not even support configuration bits and/or security bits at all.
     *        # Some ARM MCUs may require a full chip erase to erase the configuration bits and/or security bits.
     *        # In most cases, the programming session needs to be ended and the MCU needs to be reset so that the
     *          new configuration bits and/or security bits (and therefore, the fuses) can be loaded; therefore,
     *          ProgSWD will only actually write the new configuration bits and/or security bits to the MCU at the
     *          end of the programming session. On some MCUs, a full power-cycle (power-on reset) is required for
     *          the new bits to be loaded.
     */

    public int numberOfFuses()
    {
        // Ensure the FLB data are read
        final SWDFlashLoader.Specifier flSpec = _readFLB_instBuff();

        if(flSpec == null) return 0;

        // Return the number of fuses
        // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses
        return flSpec.instruction_dataBuffFLB.length - 1;
    }

    public long[] readFuses()
    {
        // Ensure the FLB data are read
        final SWDFlashLoader.Specifier flSpec = _readFLB_instBuff();

        if(flSpec == null) return null;

        // Copy and return the data
        // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses
        final long[] values = new long[flSpec.instruction_dataBuffFLB.length - 1];

        for(int i = 0; i < values.length; ++i) {
            values[i] = flSpec.instruction_dataBuffFLB[i + 1] & 0xFFFFFFFFL;
        }

        return values;
    }

    public boolean writeFuses(final long[] values)
    {
        // Ensure the FLB data are read
        final SWDFlashLoader.Specifier flSpec = _readFLB_instBuff();

        if(flSpec == null) return false;

        // Write only the data that the user wants to change
        // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses
        boolean wroteByte = false;

        for(int i = 0; i < values.length; ++i) {
            if( values[i] >= 0 && ( values[i] != (flSpec.instruction_dataBuffFLB[1 + i] & 0xFFFFFFFFL) ) ) {
                flSpec.instruction_dataBuffFLB[1 + i] = (int) values[i];
                wroteByte                             = true;
            }
        }

        if(wroteByte) xviSet(flSpec.instruction_xviFLB_FDirty, 1); // Set flag

        // Done
        return true;
    }

    public boolean writeFuses(final int[] values_)
    {
        final long[] values = new long[values_.length];

        for(int i = 0; i < values_.length; ++i) values[i] = values_[i] & 0xFFFFFFFFL;

        return writeFuses(values);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * NOTE : # Lock bits are emulated by ProgSWD because ARM MCUs have many variations on how configuration
     *          bits and/or security bits are implemented.
     *        # Lock bits are configuration bits and/or security bits that are ONLY related to the overall/full
     *          read and/or write protection of the MCUs' memories via SWD.
     *        # Some ARM MCUs may have only partial support for configuration bits and/or security bits; some
     *          of them may not even support configuration bits and/or security bits at all.
     *        # Some ARM MCUs may require a full chip erase to erase the configuration bits and/or security bits.
     *        # In most cases, the programming session needs to be ended and the MCU needs to be reset so that
     *          the new configuration bits and/or security bits (and therefore, the lock bits) can be loaded;
     *          therefore, ProgSWD will only actually write the new configuration bits and/or security bits to
     *          the MCU at the end of the programming session. On some MCUs, a full power-cycle (power-on reset)
     *          is required for the new bits to be loaded.
     */

    @Override
    public long readLockBits()
    {
        // Ensure the FLB data are read
        final SWDFlashLoader.Specifier flSpec = _readFLB_instBuff();

        if(flSpec == null) return -1;

        // Return the byte
        // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses
        return flSpec.instruction_dataBuffFLB[0] & 0xFF;
    }

    @Override
    public boolean writeLockBits(final long value_)
    {
        // Ensure the FLB data are read
        final SWDFlashLoader.Specifier flSpec = _readFLB_instBuff();

        if(flSpec == null) return false;

        // Write the byte as needed
        // NOTE : The first element always contains the lock bits while the remaining elements always contain the fuses
        final int value = (int) (value_ & 0xFF);

        if( value != flSpec.instruction_dataBuffFLB[0] ) {
            flSpec.instruction_dataBuffFLB[0] = value;
            xviSet(flSpec.instruction_xviFLB_LBDirty, 1); // Set flag
        }

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _flbDataRead = false;

    private SWDFlashLoader.Specifier _readFLB_instBuff()
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return null;
        }

        // Get the flash loader specification
        final SWDFlashLoader.Specifier flSpec = _getFlashLoaderSpecifier();

        // There is no need to continue if the FLB data was already read
        if(_flbDataRead) return flSpec;


        // Error if the instructions are not found
        if(flSpec.instruction_WriteFLB == null || flSpec.instruction_ReadFLB == null) return null;

        // Initialize the system
        if( !_initSystemOnce(flSpec) ) return null;

        // Execute the instruction
        try {
            _swdExecInst._exec(flSpec.instruction_ReadFLB, flSpec.instruction_dataBuffFLB);
        }
        catch(final USB2GPIO.TansmitError e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName, _config.memoryFlash.driverName);
            return null;
        }

        // Set flag
        _flbDataRead = true;

        // Done
        return flSpec;
    }

    private boolean _commitFLB()
    {
        // There is no need to continue if the FLB data was never read
        if(!_flbDataRead) return true;

        // Get the flash loader specification
        final SWDFlashLoader.Specifier flSpec = _getFlashLoaderSpecifier();

        // Error if the instructions are not found
        if(flSpec.instruction_WriteFLB == null || flSpec.instruction_ReadFLB == null) return false;

        /*
        for(int i = 0; i < flSpec.instruction_dataBuffFLB.length; ++i) {
            SysUtil.stdDbg().printf("%d %02X\n", i, flSpec.instruction_dataBuffFLB[i] );
        }
        //*/

        // Initialize the system
        if( !_initSystemOnce(flSpec) ) return false;

        // Execute the instruction
        try {
            _swdExecInst._exec(flSpec.instruction_WriteFLB, flSpec.instruction_dataBuffFLB);
        }
        catch(final USB2GPIO.TansmitError e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName, _config.memoryFlash.driverName);
            return false;
        }

        // Clear flag
        _flbDataRead = false;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public long execCustomInstruction(final String name)
    {
        /* ##### !!! TODO : COMPLETE AND IMPROVE THE TEST !!! #####
         * String[] paramName;
         * XVI   [] paramXVI;
         */

        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Get the flash loader specification
        final SWDFlashLoader.Specifier flSpec = _getFlashLoaderSpecifier();

        // Get the custom instruction
        final SWDFlashLoader.Specifier.CustomInstruction customInstruction = flSpec.getCustomInstruction(name);

        if(customInstruction == null) return -1;

        // Initialize the system
        if( !_initSystemOnce(flSpec) ) return -1;

        // Execute the instruction
        try {
            return _swdExecInst._exec(customInstruction.instruction, customInstruction.dataBuffLB);
        }
        catch(final USB2GPIO.TansmitError e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Notify error
            USB2GPIO.notifyError(Texts.ProgXXX_FailSWD_Command, ProgClassName, _config.memoryFlash.driverName);
            return -1;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected boolean _autoNotifyErrorMessage = false;

    protected void _suppressAllErrorMessageNotifications()
    {
        // Suppress all error message notifications
        final USB_GPIO usb_gpio = (_usb2gpio instanceof USB_GPIO) ? ( (USB_GPIO) _usb2gpio ) : null;

        if(usb_gpio != null) {
            _autoNotifyErrorMessage = usb_gpio.getAutoNotifyErrorMessage();
            usb_gpio.setAutoNotifyErrorMessage(false);
        }

        USB2GPIO.redirectNotifyErrorToString(true);
    }

    protected void _restoreAllErrorMessageNotifications()
    {
        // Restore all error message notifications.
        final USB_GPIO usb_gpio = (_usb2gpio instanceof USB_GPIO) ? ( (USB_GPIO) _usb2gpio ) : null;

        if(usb_gpio != null) usb_gpio.setAutoNotifyErrorMessage(_autoNotifyErrorMessage);

        USB2GPIO.redirectNotifyErrorToString(false);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Add this to 'SWDExecInstOpcode' and 'SWDExecInstBuilder' ??? #####

    public long armCPUID()
    {
        /*
         * Bit#   31...24       23...20   19..16   15...4   3...0
         *        IMPLEMENTER   VARIANT   ****     PARTNO   REVISION
         *        0x??          0x?       0xA      0xPPP    0x?
         *        │             │         │        │        │
         *        │             │         │        │        └→ implementation defined
         *        │             │         │        │
         *        │             │         │        └→ implementation defined
         *        │             │         │
         *        │             │         └→ 0xC ARMv6-M or ARMv8-M Baseline
         *        │             │            0xF ARMv7-M or ARMv8-M Mainline
         *        │             │
         *        │             └→ implementation defined
         *        │
         *        └→ 0x41 -> ARM Limited (ASCII character 'A')
         *           0x49 -> Infineon    (ASCII character 'I')
         *           0x63 -> ARM China   (ASCII character 'c')
         *           0x72 -> Realtek     (ASCII character 'r')
         *
         * Known PARTNO
         *                       0xPPP
         *     ARM Cortex-M0   : 0xC20 (ARM Limited)
         *     ARM Cortex-M0+  : 0xC60 (ARM Limited)
         *     ARM Cortex-M1   : 0xC21 (ARM Limited)
         *     ARM Cortex-M3   : 0xC23 (ARM Limited)
         *     ARM Cortex-M4   : 0xC24 (ARM Limited)
         *     ARM Cortex-M7   : 0xC27 (ARM Limited)
         *     ARM Cortex-M23  : 0xD20 (ARM Limited)
         *     ARM Cortex-M33  : 0xD21 (ARM Limited)
         *     ARM Cortex-M35P : 0xD31 (ARM Limited)
         *     ARM Cortex-M52  : 0xD24 (ARM China  )
         *     ARM Cortex-M55  : 0xD22 (ARM Limited)
         *     ARM Cortex-M85  : 0xD23 (ARM Limited)
         *
         *     STAR-MC1        : 0x132 (ARM China  )
         *     Infineon SLx2   : 0xDB0 (Infineon   )
         *     Realtek M200    : 0xD20 (Realtek    )
         *     Realtek M300    : 0xD22 (Realtek    )
         *
         * The IMPLEMENTER and PARTNO constants are extracted from:
         *     https://github.com/openocd-org/openocd/blob/master/src/target/arm.h
         *     https://github.com/openocd-org/openocd/blob/master/src/target/cortex_m.h
         */

        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Get the CPUID as needed
        final int idx = (_idxSelMultidropID >= 0) ? _idxSelMultidropID : 0;

        if(_armCPUID[idx] < 0) {
            try                                  { _armCPUID[idx] = _swdRdCoreMem(_config.cortexMReg.CPUID); }
            catch(final USB2GPIO.TansmitError e) { _armCPUID[idx] = -1;                                      }
        }

        // Return the CPUID
        return _armCPUID[idx];
    }

    public boolean armCPUIs(final ARMCortexMThumb.CPU cpu)
    {
        final int partNo = (int) ( ( armCPUID() >> 4 ) & 0x00000FFFL );

        switch(cpu) {
            case M0     : return (partNo == 0xC20);
            case M0plus : return (partNo == 0xC60);
            case M1     : return (partNo == 0xC21);
            case M3     : return (partNo == 0xC23);
            case M4     : return (partNo == 0xC24);
            case M7     : return (partNo == 0xC27);
            case M23    : return (partNo == 0xD20);
            case M33    : return (partNo == 0xD21);
            case M35P   : return (partNo == 0xD31);
            case M52    : return (partNo == 0xD24);
            case M55    : return (partNo == 0xD22);
            case M85    : return (partNo == 0xD23);
        }

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Add this to 'SWDExecInstOpcode' and 'SWDExecInstBuilder' ??? #####

    public int stm32DeviceID()
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Get the DBGMCU_IDCODE as needed
        final int idx = (_idxSelMultidropID >= 0) ? _idxSelMultidropID : 0;

        if(_int_mcuID1[idx] < 0) {

            // Suppress all error message notifications
            _suppressAllErrorMessageNotifications();

            // Find and read the DBGMCU_IDCODE
            final long[]  rdAddrs = new long[] { 0xE0042000L, 0x40015800L, 0xE0044000L, 0x5C001000L };
                  int     devID   = -1;

            for(final long rda : rdAddrs) {
                try {
                    final long res = _swdRdCoreMem(rda);
                    if(res < 0) continue;
                    final long val = res & 0x00000FFFL;
                    if(val != 0 && val != 0x00000FFFL) {
                        devID = (int) val;
                        break;
                    }
                }
                catch(final USB2GPIO.TansmitError e) {}
            }

            if(devID == 0x411 /* STMF2[0|1][5|7]x */) {
                // On revision A devices, the STM32F40x and STM32F41x return the same DBGMCU_IDCODE as the STM32F20x and STM32F21x devices
                // https://www.st.com/resource/en/errata_sheet/es0182-stm32f405407xx-and-stm32f415417xx-device-errata-stmicroelectronics.pdf
                if( armCPUIs(ARMCortexMThumb.CPU.M4) ) devID = 0x413; /* STMF4[0|1][5|7]x */
            }

            if( devID < 0 && armCPUIs(ARMCortexMThumb.CPU.M0plus) ) {
                // On STM32WL5x, DBGMCU_IDCODE cannot be read using CPU1 (DBGMCU is not a standard CoreSight component, hence, it does
                // not appear in the CPU1 ROM table); use the UID64 to check if it is an STM32WL5x
                // https://www.st.com/resource/en/reference_manual/rm0453-stm32wl5x-advanced-armbased-32bit-mcus-with-subghz-radio-solution-stmicroelectronics.pdf
                try { if( _swdRdCoreMem(0x1FFF7584L /* UID64.[STID:DEVID] */) == 0x0080E115L /* STM32WL5x */ ) devID = 0x497; /* STM32WL5x */ }
                catch(final USB2GPIO.TansmitError e) {}
            }

            // Restore all error message notifications
            _restoreAllErrorMessageNotifications();

            // Store the device ID
            _int_mcuID1[idx] = devID;

        } // if

        // Return the device ID
        return _int_mcuID1[idx];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Add this to 'SWDExecInstOpcode' and 'SWDExecInstBuilder' ??? #####

    public long nrf5PartCode()
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Get the part code as needed
        final int idx = (_idxSelMultidropID >= 0) ? _idxSelMultidropID : 0;

        if(_lng_mcuID1[idx] < 0) {

            // Suppress all error message notifications
            _suppressAllErrorMessageNotifications();

            // Find and read the part code
            long partCode  = -1;

            try                                  { partCode = _swdRdCoreMem(0x10000100L); }
            catch(final USB2GPIO.TansmitError e) {                                        }

            // Restore all error message notifications
            _restoreAllErrorMessageNotifications();

            // Store the part code
            _lng_mcuID1[idx] = partCode;

        } // if

        // Return the part code
        return _lng_mcuID1[idx];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Add this to 'SWDExecInstOpcode' and 'SWDExecInstBuilder' ??? #####

    public long samdDeviceID()
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Get the chip ID as needed
        final int idx = (_idxSelMultidropID >= 0) ? _idxSelMultidropID : 0;

        if(_lng_mcuID1[idx] < 0) {

            // Suppress all error message notifications
            _suppressAllErrorMessageNotifications();

            // Find and read the device ID
            long devID  = -1;

                // ##### ??? TODO : Mask out the 'DIE' and 'REVISION' using 0xFFFF00FFL ??? #####

                try                                  { devID = _swdRdCoreMem(0x41002018L); }
                catch(final USB2GPIO.TansmitError e) {                                     }

            // Restore all error message notifications
            _restoreAllErrorMessageNotifications();

            // Store the device ID
            _lng_mcuID1[idx] = devID;

        } // if

        // Return the device ID
        return _lng_mcuID1[idx];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Add this to 'SWDExecInstOpcode' and 'SWDExecInstBuilder' ??? #####

    public long sam3ChipID()
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Get the chip ID as needed
        final int idx = (_idxSelMultidropID >= 0) ? _idxSelMultidropID : 0;

        if(_lng_mcuID1[idx] < 0) {

            // Suppress all error message notifications
            _suppressAllErrorMessageNotifications();

            // Find and read the chip ID and extended chip ID
            long chipID  = -1;
            long chipXID = -1;

                try                                  { chipID = _swdRdCoreMem(0x400E0740L); chipXID = _swdRdCoreMem(0x400E0744L); }
                catch(final USB2GPIO.TansmitError e) {                                                                            }

            if(chipID <= 0) {
                try                                  { chipID = _swdRdCoreMem(0x400E0940L); chipXID = _swdRdCoreMem(0x400E0944L); }
                catch(final USB2GPIO.TansmitError e) {                                                                            }
            }

            // Restore all error message notifications
            _restoreAllErrorMessageNotifications();

            // Store the chip ID and extended chip ID
            _lng_mcuID1[idx] = chipID;
            _lng_mcuID2[idx] = chipXID;

        } // if

        // ##### ??? TODO : How to return the extended chip ID ??? #####

        // Return the chip ID
        return _lng_mcuID1[idx];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // ##### ??? TODO : Add this to 'SWDExecInstOpcode' and 'SWDExecInstBuilder' ??? #####

    public long renFMIFRT()
    {
        // Error if not in programming mode
        if(!_inProgMode) {
            USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);
            return -1;
        }

        // Get the FMIFRT as needed
        final int idx = (_idxSelMultidropID >= 0) ? _idxSelMultidropID : 0;

        if(_lng_mcuID1[idx] < 0) {

            // Suppress all error message notifications
            _suppressAllErrorMessageNotifications();

            // Find and read the FMIFRT bytes
            long data0 = -1;
            long data1 = -1;
            long data2 = -1;

            // https://www.renesas.com/us/en/document/tcu/ra-family-addition-register-definition-flash
            // https://en.na4.teamsupport.com/knowledgeBase/21397541

                try                                  { data0 = _swdRdCoreMem(0x407FB19CL    );                                         }
                catch(final USB2GPIO.TansmitError e) {                                                                                 }

                try                                  { data1 = _swdRdCoreMem(0x01001C10L + 0); data2 = _swdRdCoreMem(0x01001C10L + 4); }
                catch(final USB2GPIO.TansmitError e) {                                                                                 }

            if(data1 <= 0) {
                try                                  { data1 = _swdRdCoreMem(0x407FB1C0L + 0); data2 = _swdRdCoreMem(0x407FB1C0L + 4); }
                catch(final USB2GPIO.TansmitError e) {                                                                                 }
            }

            if(data1 <= 0) {
                try                                  { data1 = _swdRdCoreMem(0x010080F0L + 0); data2 = _swdRdCoreMem(0x010080F0L + 4); }
                catch(final USB2GPIO.TansmitError e) {                                                                                 }
            }

            // Restore all error message notifications
            _restoreAllErrorMessageNotifications();

            // Store the FMIFRT bytes
            _lng_mcuID1[idx] = data0;
            _lng_mcuID2[idx] = data1;
            _lng_mcuID3[idx] = data2;

        } // if

        // ##### ??? TODO : How to return the other FMIFRT bytes ??? #####

        // Return the FMIFRT
        return _lng_mcuID1[idx];
    }

} // class ProgSWD

