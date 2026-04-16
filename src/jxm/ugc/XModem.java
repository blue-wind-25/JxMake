/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import jxm.*;
import jxm.xb.*;


/*
 * Minimal implementation of the XMODEM protocol.
 *
 * ----------------------------------------------------------------------------------------------------
 *
 * This class is written mostly based on the algorithms and information found from:
 *
 *     XMODEM File Transfer Protocol
 *     https://www.geeksforgeeks.org/computer-networks/xmodem-file-transfer-protocol
 *
 *     XModem Protocol with CRC
 *     http://ee6115.mit.edu/amulet/xmodem.htm
 *
 *     Calculating 16-Bit CRCs (CRC-16)
 *     https://mdfs.net/Info/Comp/Comms/CRC16.htm
 */
public class XModem {

    private static final String DevClassName = "XModem";

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int   MAX_RETRIES = 5;

    private static final int   BLK_SIZE    = 128;

    private static final int   SOH         = 0x01;
    private static final int   EOT         = 0x04;
    private static final int   ACK         = 0x06;
    private static final int   NAK         = 0x15;
    private static final int   CAN         = 0x18;
    private static final int   START       = 0x43; // 'C'

    private static final int[] CRC16_TABLE = new int[256];

    static {
        // Generate the lookup table
        for(int i = 0; i < 256; ++i) {
            int crc16 = i << 8;
            for(int j = 0; j < 8; ++j) {
                if( (crc16 & 0x8000) != 0 ) crc16   = (crc16 << 1) ^ 0x1021;
                else                        crc16 <<= 1;
            }
            CRC16_TABLE[i] = crc16 & 0xFFFF;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static int crc16Calc(final byte[] buff, final int offset, final int length)
    {
        int crc16 = 0x0000;

        for(int i = 0; i < length; ++i) {
            final int index = ( (crc16 >>> 8) ^ ( buff[offset + i] & 0xFF ) ) & 0xFF;
            crc16 = ( (crc16 << 8) ^ CRC16_TABLE[index] ) & 0xFFFF;
        }

        return crc16;
    }

    private static boolean crc16Check(final byte[] buff)
    {
        final int crc16 = ( ( buff[BLK_SIZE + 3] & 0xFF ) << 8 ) | ( buff[BLK_SIZE + 4] & 0xFF );

        return ( crc16Calc(buff, 3, BLK_SIZE) == crc16 );
    }

    private static void crc16Put(final byte[] buff)
    {
        final int crc16 = crc16Calc(buff, 3, BLK_SIZE);

        buff[BLK_SIZE + 3] = (byte) ( (crc16 >> 8) & 0xFF );
        buff[BLK_SIZE + 4] = (byte) (  crc16       & 0xFF );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    final SerialDevice _ttyPort;

    public XModem(final SerialDevice ttyPort)
    { _ttyPort = ttyPort; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean _readBytes​(final byte[] buff, final int size)
    {
        // Prepare the buffer
        int len = size;
        int ofs = 0;

        // Read the byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(1000);

        while(len != 0) {
            // Read some byte(s)
            final int cnt = _ttyPort.readBytes​(buff, len, ofs);
            if(cnt < 0) return false; // Check for error
            len -= cnt;
            ofs += cnt;
            // Check for timeout
            if(cnt > 0) { tms.reset();                                                                            }
            else        { if( tms.timeout() ) return USB2GPIO.notifyError(Texts.PDevXXX_RxTimeout, DevClassName); }
        }

        // Done
        return true;
    }

    private boolean _writeBytes(final byte[] buff, final int size)
    {
        // Prepare the buffer
        int len = size;
        int ofs = 0;

        // Write the byte(s)
        final XCom.TimeoutMS tms = new XCom.TimeoutMS(1000);

        while(len != 0) {
            // Write some byte(s)
            final int cnt = _ttyPort.writeBytes(buff, len, ofs);
            if(cnt < 0) return false; // Check for error
            len -= cnt;
            ofs += cnt;
            // Check for timeout
            if(cnt > 0) { tms.reset();                                                                            }
            else        { if( tms.timeout() ) return USB2GPIO.notifyError(Texts.PDevXXX_TxTimeout, DevClassName); }
        }

        // Ensure all the bytes are written
        while( _ttyPort.bytesAwaitingWrite() > 0 );

        // Done
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private final byte[] _one = new byte[1];

    private int _recvOne()
    {
        if( !_readBytes​(_one, 1) ) return -1;

        return _one[0] & 0xFF;
    }

    private boolean _sendOne(final int value)
    {
        _one[0] = (byte) value;

        return _writeBytes(_one, 1);
    }

    private boolean _sendStart()
    { return _sendOne(START); }

    private boolean _sendEOT()
    { return _sendOne(EOT); }

    private boolean _sendACK()
    { return _sendOne(ACK); }

    private boolean _sendNAK()
    { return _sendOne(NAK); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * +------+--------------+----------------------------+------------------+----------------+
     * | SOH  | Block Number | Complement of Block Number | Data (128 bytes) | Checksum/CRC   |
     * | 0x01 | 0x01         | 0xFE                       | <payload...>     | <1 or 2 bytes> |
     * +------+--------------+----------------------------+----------------- +----------------+
     *
     * Transmitter Side
     *     1. Wait for receiver to send 'C' (0x43) for CRC mode or NAK (0x15) for checksum mode
     *     2. For each block:
     *        a. Build frame: SOH + BlockNum + ~BlockNum + 128 bytes + CRC (or checksum)
     *        b. Send frame
     *        c. Wait for ACK (0x06) or NAK (0x15)
     *           - If ACK: proceed to next block
     *           - If NAK: retransmit current block
     *     3. After last block:
     *        a. Send EOT (0x04)
     *        b. Wait for ACK
     *        c. Done
     *
     * Receiver Side
     *     1. Send 'C' (0x43) to request CRC mode (or NAK for checksum)
     *     2. Loop:
     *        a. Receive frame (expect 133 bytes)
     *        b. Validate:
     *           - SOH = 0x01
     *           - BlockNum and ~BlockNum match
     *           - CRC or checksum is correct
     *        c. If valid: send ACK (0x06), store data
     *        d. If invalid: send NAK (0x15), discard data
     *     3. When EOT (0x04) received:
     *        a. Send ACK
     *        b. Done
     */

    public boolean receive(final int[] buff, final int size_)
    {
        // Send the data
              int    size   = size_;
              int    offset = 0;
              int    blkNum = 1;
        final byte[] blk    = new byte[BLK_SIZE + 5];

        while(size > 0) {

            // Read a frame
            boolean done = false;

            for(int r = 0; r < MAX_RETRIES; ++r) {

                // Send START
                if(blkNum == 1) {
                    if( !_sendStart() ) return false;
                }

                // Read and check the frame
                if( _readBytes​(blk, blk.length) ) {
                    if( (blk[0] & 0xFF) == SOH && (blk[1] & 0xFF) == blkNum && crc16Check(blk) ) {
                        // It was a good frame, break
                        done = true;
                        break;
                    }
                }

                // It was a bad frame, NAK
                if( !_sendNAK() ) return false;

            } // for

            if(!done) return false; // Check for too many retries

            // It was a good frame, ACK
            if( !_sendACK() ) return false;

            // Copy the data and update the counters
            for( int i = 0; i < Math.min(size, BLK_SIZE); ++i ) buff[offset + i] = blk[3 + i] & 0xFF;

            offset += BLK_SIZE;
            size   -= BLK_SIZE;

            ++blkNum;

        } // while

        // Wait for EOT
        for(int r = 0; r < MAX_RETRIES; ++r) {

            if( _recvOne() == EOT ) return _sendACK();

            if( !_sendNAK() ) return false;

        } // for

        // Error
        return false;
    }

    public boolean transmit(final int[] buff, final int size_)
    {
        // Send START
        boolean done = false;

        for(int r = 0; r < MAX_RETRIES; ++r) {

            if( _recvOne() == START ) {
                done = true;
                break;
            }

        } // for

        if(!done) return false; // Check for too many retries

        // Send the data
              int    size   = size_;
              int    offset = 0;
              int    blkNum = 1;
        final byte[] blk    = new byte[BLK_SIZE + 5];

        while(size > 0) {

            // Put the opening bytes
            blk[0] = (byte) ( SOH              );
            blk[1] = (byte) (   blkNum         );
            blk[2] = (byte) ( ~(blkNum) & 0xFF );

            // Copy the data and put the CRC
            for( int i = 0; i < Math.min(size, BLK_SIZE); ++i ) blk[3 + i] = (byte) buff[offset + i];
            crc16Put(blk);

            // Send a frame
            done = false;

            for(int r = 0; r < MAX_RETRIES; ++r) {

                // Send the bytes
                if( !_writeBytes(blk, blk.length) ) return false;

                // Break on ACK
                if( _recvOne() == ACK ) {
                    done = true;
                    break;
                }

            } // for

            if(!done) return false; // Check for too many retries

            // Update the counters
            offset += BLK_SIZE;
            size   -= BLK_SIZE;

            ++blkNum;

        } // while

        // Send EOT
        for(int r = 0; r < MAX_RETRIES; ++r) {

            if( !_sendEOT() ) return false;

            if( _recvOne() == ACK ) return true;

        } // for

        // Error
        return false;
    }

} // class XModem
