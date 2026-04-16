/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */

/*
 * ################################## !!! WARNING !!! ##################################
 * This source code file relies on the Foreign Function and Memory (FFM) API; therefore,
 * it is excluded from the build when using Java versions earlier than 23.
 * ################################## !!! WARNING !!! ##################################
 */


package jxm.ugc;


import java.io.Serializable;

import java.util.Arrays;
import java.util.List;

import net.codecrete.usb.*;

import jxm.*;
import jxm.annotation.*;
import jxm.xb.*;


/*
 * Base class for all 'ProgBoot*' subclasses that interface with bootloaders using USB protocols
 * other than CDC-ACM.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * NOTE : On Windows, you may need to use a custom INF file or Zadig to assign WinUSB as the device
 *        driver.
 */
public abstract class ProgBootUSB implements IProgCommon {

    /*
     * Transfer speed depends on the underlying protocol and the MCU's flash page size.
     */

    private   static final String DevClassName = "ProgBootUSB";
    protected static       String ProgClassName;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : For most MCUs, the empty value of flash memory is 0xFF; however, on some MCUs, it may be 0x00
    protected static final byte FlashMemory_EmptyValue = (byte) 0xFF;

    @SuppressWarnings("serial")
    public static class Config extends SerializableDeepClone<Config> {

        // JxMake use a special field name for serial version UID
        @DataFormat.Hex16 public static final long __0_JxMake_SerialVersionUID__ = SysUtil.extSerialVersionUID(0x00000001);

        // NOTE : The default values below are for (almost all?) AVR MCUs that can be programmed using ISP

        public static class MemoryFlash implements Serializable {
            public boolean paged        = true;
            public int     totalSize    = 0;
            public int     pageSize     = 0;
            public int     numPages     = 0;

            public int[]   readDataBuff = null;
        }

        public static class MemoryEEPROM implements Serializable {
            public int totalSize =  0;
            public int pageSize  = -1; // Optional (may also indicate slightly different MCU families)
            public int numPages  = -1; // ---
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public final MemoryFlash  memoryFlash  = new MemoryFlash ();
        public final MemoryEEPROM memoryEEPROM = new MemoryEEPROM();

    } // class Config

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static int _getUInt16(final byte[] buff, final int offset)
    { return (buff[offset] & 0xFF) | ( (buff[offset + 1] & 0xFF) << 8 ); }

    protected static int _getUInt24(final byte[] buff, final int offset)
    { return (buff[offset] & 0xFF) | ( (buff[offset + 1] & 0xFF) << 8 ) | ( (buff[offset + 2] & 0xFF) << 16 ); }

    protected static int _getUInt32(final byte[] buff, final int offset)
    { return (buff[offset] & 0xFF) | ( (buff[offset + 1] & 0xFF) << 8 ) | ( (buff[offset + 2] & 0xFF) << 16 ) | ( (buff[offset + 3] & 0xFF) << 24 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static final int USB_DEVICE_CLASS_VENDOR_SPECIFIC = 0xFF;

    protected static List<UsbDevice> getAllDevices(final int vid, final int pid, final String manufacturerString, final String productString, final int bFDescriptorType, final int bInterfaceClass, final int bInterfaceSubClass, final int bInterfaceProtocol)
    {
        return Usb.findDevices( (final UsbDevice device) -> {
            return (
                ( device.getVendorId() == vid )
                &&
                ( device.getProductId() == pid )
                &&
                ( manufacturerString == null || manufacturerString.equals( device.getManufacturer() ) )
                &&
                ( productString == null || productString.equals( device.getProduct() ) )
                &&
                ( bFDescriptorType <= 0 || _getDescriptorOffset( device.getConfigurationDescriptor(), bFDescriptorType ) > 0 )
                &&
                ( bInterfaceClass <= 0 || _getInterfaceNumber(device, bInterfaceClass, bInterfaceSubClass, bInterfaceProtocol) >= 0 )
            );
        } );
    }

    protected static List<UsbDevice> getAllDevices(final int vid, final int pid, final String manufacturerString, final String productString, final int bInterfaceClass, final int bInterfaceSubClass, final int bInterfaceProtocol)
    { return getAllDevices(vid, pid, manufacturerString, productString, -1, bInterfaceClass, bInterfaceSubClass, bInterfaceProtocol); }

    protected static List<UsbDevice> getAllDevices(final int vid, final int pid, final String manufacturerString, final String productString)
    { return getAllDevices(vid, pid, manufacturerString, productString, -1, -1, -1, -1); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static int _getDescriptorOffset(final byte[] descriptor, final int bFDescriptorType)
    {
        int offset = 0;

        while(offset < descriptor.length) {
            if(descriptor[offset + 1] == bFDescriptorType) return offset;
            offset += descriptor[offset] & 0xFF;
        }

        return -1;
    }

    protected static int _getInterfaceNumber(final UsbDevice device, final int bInterfaceClass, final int bInterfaceSubClass, final int bInterfaceProtocol)
    {
        for( final UsbInterface ifc : device.getInterfaces() ) {
            final UsbAlternateInterface alt = ifc.getCurrentAlternate();
            if( alt.getClassCode() == bInterfaceClass && alt.getSubclassCode() == bInterfaceSubClass && alt.getProtocolCode() == bInterfaceProtocol ) return ifc.getNumber();
        }

        if( device.getInterfaces().size() == 1 ) {
            if( device.getInterfaces().get(0).getAlternates().size() == 1 ) {
                if( device.getClassCode() == bInterfaceClass && device.getSubclassCode() == bInterfaceSubClass && device.getProtocolCode() == bInterfaceProtocol ) return 0;
            }
        }

        return -1;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void _openClaimDevice(final boolean detachStandardDrivers)
    {
        // ##### !!! TODO : Create alternative for Windows !!! #####
        if(detachStandardDrivers) _device.detachStandardDrivers();

        _device.open();
        _device.claimInterface(_interfaceNumber);
    }

    protected void _closeDevice()
    {
        if(_device != null) {
          //if(_interfaceNumber >= 0) _device.releaseInterface(_interfaceNumber);
            _device.close();
        }

        _device          = null;
        _interfaceNumber = -1;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int DEF_WAIT_USB_DISCONNECT_TIMOUT_MS = 5000;

    protected boolean _waitUSBDeviceDisconnect(final int timeoutMS)
    {
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(timeoutMS);

        while(true) {

            SysUtil.sleepMS( Math.max(10, timeoutMS / 100) );

            if( !_device.isConnected() ) return true;

            if( tms.timeout() ) {
                USB2GPIO.notifyError(Texts.ProgXXX_USBDisconnectTout, ProgClassName);
                return false;
            }

        } // while

    }

    protected boolean _waitUSBDeviceDisconnect()
    { return _waitUSBDeviceDisconnect(DEF_WAIT_USB_DISCONNECT_TIMOUT_MS); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void _selectAlternateSetting(final int alternateNumber)
    { _device.selectAlternateSetting( _interfaceNumber, alternateNumber ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static UsbControlTransfer _createVendorUsbControlTransfer(final UsbRecipient recipient, final int request, final int value, final int index)
    { return new UsbControlTransfer(UsbRequestType.VENDOR, recipient, request, value, index); }

    protected static UsbControlTransfer _createVendorDeviceUsbControlTransfer(final int request, final int value, final int index)
    { return _createVendorUsbControlTransfer(UsbRecipient.DEVICE, request, value, index); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected Config    _config;

    protected UsbDevice _device          = null;
    protected int       _interfaceNumber = -1;

    protected ProgBootUSB(final String progClassName, final Config config) throws Exception
    {
        // Store the programmer class name
        ProgClassName = progClassName;

        // Store the configuration
        _config = config.deepClone();

        // Check the configuration values
        if(_config.memoryFlash.totalSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFTotSize, ProgClassName);

        if(_config.memoryFlash.paged) {
            if(_config.memoryFlash.pageSize <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFPageSize, ProgClassName);
            if(_config.memoryFlash.numPages <= 0) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFNumPages, ProgClassName);

            if(_config.memoryFlash.pageSize * _config.memoryFlash.numPages != _config.memoryFlash.totalSize) throw XCom.newJXMFatalLogicError(Texts.ProgXXX_InvMFPageSpec, ProgClassName);
        }
    }

    public Config config()
    { return _config; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int _flashMemoryTotalSize()
    { return _config.memoryFlash.totalSize; }

    @Override
    public int _eepromMemoryTotalSize()
    { return _config.memoryEEPROM.totalSize; }

    @Override
    public int[] _readDataBuff()
    { return _config.memoryFlash.readDataBuff; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected int[] _mcuSignature = null;

    // NOTE : By default, assume the bootloader is able to read the device signature/ID
    @Override
    public boolean supportSignature()
    { return true; }

    @Override
    public boolean verifySignature(final int[] signatureBytes)
    {
        // Error if the signature has not been read
        if(_mcuSignature == null) return USB2GPIO.notifyError(Texts.ProgXXX_NotInProgMode, ProgClassName);

        // Compare the signature
        return Arrays.equals(_mcuSignature, signatureBytes);
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

    // WARNING : Since bootloader programmers do not have full support for reading and writing lock bits,
    //           this feature is not implemented here!

    @Override
    public long readLockBits()
    { return -1; }

    @Override
    public boolean writeLockBits(final long value)
    { return false; }

} // class ProgBootUSB
