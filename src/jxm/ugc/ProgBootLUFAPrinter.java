/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.io.File;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;

import java.util.function.IntConsumer;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;

import javax.print.attribute.Attribute;
import javax.print.attribute.standard.QueuedJobCount;

import jxm.*;
import jxm.tool.fwc.*;
import jxm.xb.*;


/*
 * Standard implementation of the LUFA Printer protocol for programming MCUs with compatible bootloaders.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * NOTE :
 *     # Ensure the correct driver is installed:
 *           # On Windows, it should be 'Generic / Text Only'.
 *           # On Linux  , it should be 'Generic Text-Only Printer'  or 'Raw Queue'.
 *           # On MacOS  , it should be 'Generic PostScript Printer' or 'Generic PCL Printer'.
 *     # Disable spooling for best reliability:
 *           # On Windows:
 *                 # Go to Printer Properties → Advanced tab.
 *                 # Uncheck "Spool print documents" → Select "Print directly to the printer".
 *           # On Linux & macOS:
 *                 # Connect the device and find its URI with:
 *                       lpinfo -v
 *                   and look for something like:
 *                       direct usb://Generic/Generic_/_Text_Only
 *                 # Register the printer with:
 *                       lpadmin -p LUFA_BootloaderLPT -E -v usb://Generic/Generic_/_Text_Only -m raw
 *                 # Confirm with:
 *                       lpstat -v
 *                   and check for output like:
 *                       device for LUFA_BootloaderLPT: usb://Generic/Generic_/_Text_Only
 *                 # Confirm with:
 *                       lpoptions -p LUFA_BootloaderLPT -l
 *                   to ensure no filters are applied.
 *             Or use the desktop environment's GUI printer manager, if available.
 *
 * WARNING :
 *     # Sending data to the printer is asynchronous.
 *     # Writing data to flash may appear complete even while the OS is still streaming bytes to the
 *       target.
 *     # If data transmission fails, it may appear as a pending print job. Please check your OS GUI
 *       to ensure these types of jobs are deleted to avoid unintended consequences.
 */
public class ProgBootLUFAPrinter implements IProgCommon {

    private static final String ProgClassName = "ProgBootLUFAPrinter";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : LUFA Printer bootloader does not support most of these queries!

    private static final byte FlashMemory_EmptyValue = (byte) 0xFF; // NOTE : For AVR MCUs, the empty value of flash memory is 0xFF

    private static final int  FlashWrite_StepCount   = 12;
    private static final int  FlashWrite_StepMulti   = 16;
    private static final int  FlashWrite_StepBytes   = FlashWrite_StepCount * FlashWrite_StepMulti * 2;

    public ProgBootLUFAPrinter() throws Exception
    {}

    @Override
    public int _flashMemoryTotalSize()
    { return -1; }

    @Override
    public byte _flashMemoryEmptyValue()
    { return FlashMemory_EmptyValue; }

    // NOTE : This function returns a dummy value that indicates the number of steps in the flash write process.
    @Override
    public int _flashMemoryAlignWriteSize(final int numBytes)
    { return FlashWrite_StepBytes; }

    @Override
    public int _eepromMemoryTotalSize()
    { return -1; }

    @Override
    public byte _eepromMemoryEmptyValue()
    { return FlashMemory_EmptyValue; }

    @Override
    public int[] _readDataBuff()
    { return null; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static ArrayList<String> listPrinters(final DocFlavor docFlavor)
    {
        final ArrayList<String> res = new ArrayList<>();

        for( final PrintService service : PrintServiceLookup.lookupPrintServices(docFlavor, null) ) {
            res.add( service.getName() );
        }

        return res;
    }

    public static ArrayList<String> listPrinters()
    { return listPrinters(DocFlavor.BYTE_ARRAY.AUTOSENSE); }

    private static int _getQueuedJobCount(final PrintService service)
    {
        for( final Attribute attribute : service.getAttributes().toArray() ) {
            if(attribute instanceof QueuedJobCount) return  ( (QueuedJobCount) attribute ).getValue();
        }

        return 0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private String _printerName = null;

    public boolean begin(final String printerName)
    {
        // Error if already in programming mode
        if(_printerName != null) return USB2GPIO.notifyError(Texts.ProgXXX_InProgMode, ProgClassName);

        // Save the printer name
        _printerName = printerName.trim();

        // Done
        return true;
    }

    @Override
    public boolean end()
    {
        // Error if not in programming mode
        if(_printerName == null) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Clear the printer name
        _printerName = null;

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : LUFA Printer bootloader does not support signature reading!

    @Override
    public boolean supportSignature()
    { return false; }

    @Override
    public boolean readSignature()
    { return false; }

    @Override
    public boolean verifySignature(final int[] signatureBytes)
    { return false; }

    @Override
    public int[] mcuSignature()
    { return null; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : LUFA Printer bootloader does not support chip erase!

    @Override
    public boolean chipErase()
    { return false; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : LUFA Printer bootloader does not support flash reading!

    @Override
    public boolean readFlash(final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return false; }

    @Override
    public int verifyFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    { return -1; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean _writeFlash_impl(final byte[] data, final int startAddress, final int numBytes, final IntConsumer progressCallback) throws Exception
    {
        // Error if not in programming mode
        if(_printerName == null) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Check the start address and number of bytes
        if(startAddress != 0) {
            // The start address must be zero
            return USB2GPIO.notifyError(Texts.ProgXXX_SAddrNotZero, ProgClassName);
        }

        if( (numBytes & 0x01) != 0 ) {
            // The number of bytes must be even (a multiple of 2)
            return USB2GPIO.notifyError(Texts.ProgXXX_NBytesNotEven, ProgClassName);
        }

        // Call the progress callback function for the initial value
        final ProgressCB pcb = new ProgressCB();

        pcb.callProgressCallbackInitial(progressCallback, FlashWrite_StepBytes);

        // Create a temporary file
        final File tmpIHex = File.createTempFile( ProgClassName, ".hex", new File( SysUtil.getRootTmpDir() ) );
        /* 01 */ pcb.callProgressCallbackCurrentMulti(progressCallback, FlashWrite_StepBytes, FlashWrite_StepMulti);

        // Save the binary data to a temporary Intel Hex file
        final FWComposer fwc = new FWComposer();

        fwc.loadRawBinaryData(data, startAddress);
        /* 02 */ pcb.callProgressCallbackCurrentMulti(progressCallback, FlashWrite_StepBytes, FlashWrite_StepMulti);

        fwc.saveIntelHexFile( tmpIHex.getAbsolutePath() );
        /* 03 */ pcb.callProgressCallbackCurrentMulti(progressCallback, FlashWrite_StepBytes, FlashWrite_StepMulti);

        // Load the temporary Intel Hex file as raw text
        final byte[] hexData = Files.readAllBytes( Paths.get( tmpIHex.getAbsolutePath() ) );
        /* 04 */ pcb.callProgressCallbackCurrentMulti(progressCallback, FlashWrite_StepBytes, FlashWrite_StepMulti);

        // Delete the temporary file
        tmpIHex.delete();
        /* 05 */ pcb.callProgressCallbackCurrentMulti(progressCallback, FlashWrite_StepBytes, FlashWrite_StepMulti);

        // Define raw data flavor
        final DocFlavor docFlavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        /* 06 */ pcb.callProgressCallbackCurrentMulti(progressCallback, FlashWrite_StepBytes, FlashWrite_StepMulti);

        // Locate the LUFA printer
        PrintService ps = null;

        for( final PrintService service : PrintServiceLookup.lookupPrintServices(docFlavor, null) ) {
            if( service.getName().equalsIgnoreCase(_printerName) ) {
                ps = service;
                break;
            }
        }
        /* 07 */ pcb.callProgressCallbackCurrentMulti(progressCallback, FlashWrite_StepBytes, FlashWrite_StepMulti);

        if( _getQueuedJobCount(ps) > 0 ) return USB2GPIO.notifyError(Texts.ProgXXX_PrinterBLBusy, ProgClassName);
        /* 08 */ pcb.callProgressCallbackCurrentMulti(progressCallback, FlashWrite_StepBytes, FlashWrite_StepMulti);

        // Create the document
        final Doc doc = new SimpleDoc(hexData, docFlavor, null);
        /* 09 */ pcb.callProgressCallbackCurrentMulti(progressCallback, FlashWrite_StepBytes, FlashWrite_StepMulti);

        // Create the job
        final DocPrintJob job = ps.createPrintJob();
        /* 10 */ pcb.callProgressCallbackCurrentMulti(progressCallback, FlashWrite_StepBytes, FlashWrite_StepMulti);

        // Send the document
        job.print(doc, null);
        /* 11 */ pcb.callProgressCallbackCurrentMulti(progressCallback, FlashWrite_StepBytes, FlashWrite_StepMulti);

        // Wait for the job to complete
        final XCom.TimeoutMS tms = new XCom.TimeoutMS( Math.max( 3000, 2000 + 100 * (data.length / 1024) ) );

        while(true) {

            SysUtil.sleepMS(100);

            if( _getQueuedJobCount(ps) <= 0 ) break;

            if( tms.timeout() ) return USB2GPIO.notifyError(Texts.ProgXXX_PrinterBLTimeout, ProgClassName);

        } // while

        /* 12 */ pcb.callProgressCallbackCurrentMulti(progressCallback, FlashWrite_StepBytes, FlashWrite_StepMulti);

        // Call the progress callback function for the final value
        pcb.callProgressCallbackFinal(progressCallback, FlashWrite_StepBytes);

        // Done
        return true;
    }

    @Override
    public boolean writeFlash(final byte[] data, final int startAddress, final int numBytes, final IntConsumer progressCallback)
    {
        try {
            return _writeFlash_impl(data, startAddress, numBytes, progressCallback);
        }
        catch(final Exception e) {
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
        }

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : LUFA Printer bootloader does not support EEPROM reading and writing!

    @Override
    public int readEEPROM(final int address)
    { return -1; }

    @Override
    public boolean writeEEPROM(final int address, final byte data)
    { return false; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // WARNING : LUFA Printer bootloader does not support lock bits reading and writing!

    @Override
    public long readLockBits()
    { return -1; }

    @Override
    public boolean writeLockBits(final long value)
    { return false; }

} // class ProgBootLUFAPrinter
