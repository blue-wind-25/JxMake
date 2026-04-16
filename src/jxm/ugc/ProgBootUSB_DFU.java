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


import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import java.util.stream.Collectors;

import net.codecrete.usb.*;

import jxm.*;
import jxm.xb.*;


/*
 * Base class for all 'ProgBoot*' subclasses that interface with bootloaders using the USB DFU protocol.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written partially based on the algorithms and information found from:
 *
 *     DFU 1.1 specification
 *     https://www.usb.org/sites/default/files/DFU_1.1.pdf
 *
 *     Device Firmware Upload (DFU) for STM32
 *     https://github.com/manuelbl/JavaDoesUSB/tree/main/examples/stm_dfu
 *     Copyright (C) 2017 Manuel Bleichenbacher
 *     MIT License
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * NOTE : On Windows, you may need to use a custom INF file or Zadig to assign WinUSB as the device
 *        driver.
 */
public abstract class ProgBootUSB_DFU extends ProgBootUSB {

    /*
     * Transfer speed depends on the underlying protocol and the MCU's flash page size.
     */

    private static final String DevClassName = "ProgBootUSB_DFU";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("serial")
    protected static class DFUError extends Exception {

        public DFUError(final String errMsg, final Object... args)
        { super( String.format("%s: " + errMsg, ProgClassName, args) ); }

    } // class DFUError

    protected static enum DFURequest {

        DETACH      , // 0x00 - Requests the device to leave DFU mode and enter the application - not meaningful in the case of the bootloader
        DOWNLOAD    , // 0x01 - Requests data transfer from Host to the device in order to load them into device internal flash memory - includes also erase commands
        UPLOAD      , // 0x02 - Requests data transfer from device to Host in order to load content of device internal flash memory into a Host file
        GET_STATUS  , // 0x03 - Requests device to send status report to the Host (including status resulting from the last request execution and the state the device enters immediately after this request)
        CLEAR_STATUS, // 0x04 - Requests device to clear error status and move to next step
        GET_STATE   , // 0x05 - Requests the device to send only the state it enters immediately after this request
        ABORT         // 0x06 - Requests device to exit the current state/operation and enter idle state immediately

    } // enum DFURequest

    protected static enum DFUDeviceStatus {

        OK              , // 0x00 - No error condition is present
        ERR_TARGET      , // 0x01 - File is not targeted for use by this device
        ERR_FILE        , // 0x02 - File is for this device but fails some vendor-specific verification test
        ERR_WRITE       , // 0x03 - Device is unable to write memory
        ERR_ERASE       , // 0x04 - Memory erase function failed
        ERR_CHECK_ERASED, // 0x05 - Memory erase check failed
        ERR_PROG        , // 0x06 - Program memory function failed
        ERR_VERIFY      , // 0x07 - Programmed memory failed verification
        ERR_ADDRESS     , // 0x08 - Cannot program memory due to received address that is out of range
        ERR_NOTDONE     , // 0x09 - Received DFU_DNLOAD with wLength = 0, but device does not think it has all of the data yet
        ERR_FIRMWARE    , // 0x0A - Device's firmware is corrupt - it cannot return to run-time (non-DFU) operations
        ERR_VENDOR      , // 0x0B - iString indicates a vendor-specific error
        ERR_USBR        , // 0x0C - Device detected unexpected USB reset signaling
        ERR_POR         , // 0x0D - Device detected unexpected power on reset
        ERR_UNKNOWN     , // 0x0E - Something went wrong, but the device does not know what it was
        ERR_STALLEDPKT    // 0x0F - Device stalled an unexpected request

        ;

        public static DFUDeviceStatus fromValue(byte value) { return values()[value]; }

    } // enum DFUDeviceStatus

    protected static enum DFUDeviceState {

        APP_IDLE               , // 0x00 - Device is running its normal application
        APP_DETACH             , // 0x01 - Device is running its normal application, has received the DFU_DETACH request, and is waiting for a USB reset
        DFU_IDLE               , // 0x02 - Device is operating in the DFU mode and is waiting for requests
        DFU_DNLOAD_SYNC        , // 0x03 - Device has received a block and is waiting for the host to solicit the status via DFU_GETSTATUS
        DFU_DNBUSY             , // 0x04 - Device is programming a control-write block into its nonvolatile memories
        DFU_DNLOAD_IDLE        , // 0x05 - Device is processing a download operation - expecting DFU_DNLOAD requests
        DFU_MANIFEST_SYNC      , // 0x06 - Device has received the final block of firmware from the host and is waiting for receipt of DFU_GETSTATUS to begin the Manifestation phase - or device has completed the Manifestation phase and is waiting for receipt of DFU_GETSTATUS
        DFU_MANIFEST           , // 0x07 - Device is in the Manifestation phase - not all devices will be able to respond to DFU_GETSTATUS when in this state
        DFU_MANIFEST_WAIT_RESET, // 0x08 - Device has programmed its memories and is waiting for a USB reset or a power on reset
        DFU_UPLOAD_IDLE        , // 0x09 - The device is processing an upload operation - expecting DFU_UPLOAD requests
        DFU_ERROR                // 0x0A - An error has occurred - awaiting the DFU_CLRSTATUS request

        ;

        public static DFUDeviceState fromValue(byte value) { return values()[value]; }

    } // enum DFUDeviceState

    protected static record DFUResult(DFUDeviceStatus status, DFUDeviceState... state) {}

    protected static record DFUStatus(DFUDeviceStatus status, DFUDeviceState state, int pollTimeout)
    {
        public static DFUStatus fromBytes(final byte[] buff)
        {
            final DFUDeviceStatus status      = DFUDeviceStatus.fromValue(buff[0]);
            final DFUDeviceState  state       = DFUDeviceState .fromValue(buff[4]);
            final int             pollTimeout = _getUInt24(buff, 1);

            return new DFUStatus(status, state, pollTimeout);
        }

    } // record DFUStatus

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static String _setToString(final Set<DFUDeviceState> expectedSet)
    { return '[' + expectedSet.stream().map(Enum::name).collect(Collectors.joining("|") ) + ']'; }

    protected static void _throwDFUError(final String errMsg, final Object... args) throws DFUError
    { throw new DFUError(errMsg, args); }

    protected static void _throwDFUError_InvalidState(final DFUDeviceState actual, final Set<DFUDeviceState> expectedSet) throws DFUError
    { _throwDFUError( Texts.CmdXErr_BLInvalidState, actual, _setToString(expectedSet) ); }

    protected static void _throwDFUError_InvalidState(final DFUDeviceState actual, final DFUDeviceState expected) throws DFUError
    { _throwDFUError( Texts.CmdXErr_BLInvalidState, actual, expected ); }

    protected static void _throwDFUError_InvalidStatus(final DFUDeviceStatus actual, final DFUDeviceStatus expected) throws DFUError
    { _throwDFUError( Texts.CmdXErr_BLInvalidStatus, actual, expected ); }

    protected static void _throwDFUError_InvalidStateStatus(DFUDeviceState actual1, final DFUDeviceStatus actual2, final Set<DFUDeviceState> expectedSet1, final DFUDeviceStatus expected2) throws DFUError
    { _throwDFUError( Texts.CmdXErr_BLInvalidStaSts, actual1, actual2, _setToString(expectedSet1), expected2 ); }

    protected static void _throwDFUError_InvalidStatusState(DFUDeviceStatus actual1, final DFUDeviceState actual2, final DFUDeviceStatus expected1, final Set<DFUDeviceState> expectedSet2) throws DFUError
    { _throwDFUError( Texts.CmdXErr_BLInvalidStsSta, actual1, actual2, expected1, _setToString(expectedSet2) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // NOTE : These are the default values for standard DFU devices
    private static final int DEF_DFU_MODE_FN_DESC_bFDescriptorType   = 0x21; // DFU mode functional descriptor - bDescriptorType
    private static final int DEF_DFU_MODE_IF_DESC_bInterfaceClass    = 0xFE; // DFU mode interface  descriptor - bInterfaceClass
    private static final int DEF_DFU_MODE_IF_DESC_bInterfaceSubClass = 0x01; // DFU mode interface  descriptor - bInterfaceSubClass
    private static final int DEF_DFU_MODE_IF_DESC_bInterfaceProtocol = 0x02; // DFU mode interface  descriptor - bInterfaceProtocol

    protected static List<UsbDevice> _dfuGetAllDevices(final int vid, final int pid, final int bFDescriptorType, final int bInterfaceClass, final int bInterfaceSubClass, final int bInterfaceProtocol)
    {
        return Usb.findDevices( (final UsbDevice device) -> {
            return (
                ( device.getVendorId() == vid )
                &&
                ( device.getProductId() == pid )
                &&
                ( _getDescriptorOffset( device.getConfigurationDescriptor(), bFDescriptorType ) > 0 )
                &&
                ( _getInterfaceNumber(device, bInterfaceClass, bInterfaceSubClass, bInterfaceProtocol) >= 0 )
            );
        } );
    }

    protected static List<UsbDevice> _dfuGetAllDevices(final int vid, final int pid)
    {
        return _dfuGetAllDevices(
            vid,
            pid,
            DEF_DFU_MODE_FN_DESC_bFDescriptorType,
            DEF_DFU_MODE_IF_DESC_bInterfaceClass,
            DEF_DFU_MODE_IF_DESC_bInterfaceSubClass,
            DEF_DFU_MODE_IF_DESC_bInterfaceProtocol
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected int _dfuGetInterfaceNumber()
    { return _getInterfaceNumber(_device, DEF_DFU_MODE_IF_DESC_bInterfaceClass, DEF_DFU_MODE_IF_DESC_bInterfaceSubClass, DEF_DFU_MODE_IF_DESC_bInterfaceProtocol); }

    protected int _dfuGetTransferSize(final int bFDescriptorType)
    {
        final byte[] descriptor = _device.getConfigurationDescriptor();
        final int    offset     = _getDescriptorOffset(descriptor, bFDescriptorType);

        return _getUInt16(descriptor, offset + 5); // DFU mode functional descriptor - wTransferSize
    }

    protected int _dfuGetTransferSize()
    { return _dfuGetTransferSize(DEF_DFU_MODE_FN_DESC_bFDescriptorType); }

    protected String _dfuGetStringDescriptor(final int index)
    {
        final byte[] stringDesc = _device.controlTransferIn(
                                      new UsbControlTransfer( UsbRequestType.STANDARD, UsbRecipient.DEVICE, 6, (3 << 8) | index, 0 ),
                                      255
                                  );
        final int    descLen    = stringDesc[0] & 0xFF;

        return new String(stringDesc, 2, descLen - 2, StandardCharsets.UTF_16LE);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static UsbControlTransfer _dfuUsbControlTransfer(final DFURequest request, final int value, final int index)
    { return new UsbControlTransfer(UsbRequestType.CLASS, UsbRecipient.INTERFACE, request.ordinal(), value, index); }

    protected UsbControlTransfer _dfuUsbControlTransfer(final DFURequest request, final int value)
    { return _dfuUsbControlTransfer(request, value, _interfaceNumber); } // NOTE : wIndex is often used to specify the interface number

    protected byte[] _dfuUpload(final int value, final int len)
    { return _device.controlTransferIn( _dfuUsbControlTransfer(DFURequest.UPLOAD, value), len ); }

    protected DFUStatus _dfuGetStatus()
    {
        final byte[] res = _device.controlTransferIn( _dfuUsbControlTransfer(DFURequest.GET_STATUS, 0), 6 );

        if(res.length != 6) return null;

        return DFUStatus.fromBytes(res);
    }

    protected void _dfuClearStatus()
    { _device.controlTransferOut( _dfuUsbControlTransfer(DFURequest.CLEAR_STATUS, 0), null ); }

    protected void _dfuAbort()
    { _device.controlTransferOut( _dfuUsbControlTransfer(DFURequest.ABORT, 0), null ); }

    protected void _dfuDetach()
    { _device.controlTransferOut( _dfuUsbControlTransfer(DFURequest.DETACH, 0), null ); }

    protected boolean _dfuClearError()
    {
        final DFUStatus status = _dfuGetStatus();

        if( status.status() != DFUDeviceStatus.OK ) {
            _dfuClearStatus();
            SysUtil.sleepMS( status.pollTimeout() );
            return ( _dfuGetStatus().status() == DFUDeviceStatus.OK );
        }

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static void _dfuExpectState(final DFUStatus status, final DFUDeviceState... expectedStates) throws DFUError
    {
        final Set<DFUDeviceState> expectedSet = EnumSet.noneOf(DFUDeviceState.class);
                                  expectedSet.addAll( Arrays.asList(expectedStates) );

        if( !expectedSet.contains( status.state() ) ) {
            if(expectedStates.length > 1) _throwDFUError_InvalidState( status.state(), expectedSet       );
            else                          _throwDFUError_InvalidState( status.state(), expectedStates[0] );
        }
    }

    protected void _dfuExpectState(final DFUDeviceState... expectedStates) throws Exception
    { _dfuExpectState( _dfuGetStatus(), expectedStates ); }

    protected static void _dfuExpectStatus(final DFUStatus status, final DFUDeviceStatus expectedStatus) throws DFUError
    { if( status.status() != expectedStatus) _throwDFUError_InvalidStatus( status.status(), expectedStatus ); }

    protected void _dfuExpectStatus(final DFUDeviceStatus expectedStatus) throws Exception
    { _dfuExpectStatus( _dfuGetStatus(), expectedStatus ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected static void _dfuExpectStateStatus(final DFUStatus status, final DFUResult expectedResult) throws DFUError
    {
        if( ( !Arrays.asList( expectedResult.state() ).contains( status.state() ) ) ||
            ( status.status() != expectedResult.status() )                        ) {
            _throwDFUError_InvalidStateStatus(
                status.state ()                  ,
                status.status()                  ,
                Set.of( expectedResult.state () ),
                        expectedResult.status()
             );
        }
    }

    protected void _dfuExpectStateStatus(final DFUResult expectedResult) throws DFUError
    { _dfuExpectStateStatus( _dfuGetStatus(), expectedResult ); }

    protected static void _dfuExpectStatusState(final DFUStatus status, final DFUResult expectedResult) throws DFUError
    {
        if( ( !Arrays.asList( expectedResult.state() ).contains( status.state() ) ) ||
            ( status.status() != expectedResult.status() )                        ) {
            _throwDFUError_InvalidStatusState(
                status.status ()                 ,
                status.state  ()                 ,
                        expectedResult.status()  ,
                Set.of( expectedResult.state () )
             );
        }

    }

    protected void _dfuExpectStatusState(final DFUResult expectedResult) throws DFUError
    { _dfuExpectStatusState( _dfuGetStatus(), expectedResult ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected ProgBootUSB_DFU(final String progClassName, final Config config) throws Exception
    {
        // Process the superclass
        super(progClassName, config);
    }

} // class ProgBootUSB_DFU
