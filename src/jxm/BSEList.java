/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.math.BigInteger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.Objects;
import java.util.HashMap;

import jxm.*;
import jxm.xb.*;


public class BSEList {

    //                           Handle  Value
    private static final HashMap<String, ByteStreamEditor> _bseList      = new HashMap<>();
    private static       long                              _bseHandleCnt = 0;

    private static final long                              BSEIDMMask    = SysUtil.random32();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private synchronized static String _newBSEHandle()
    { return SysUtil.createHandleID("bytestreameditor", ++_bseHandleCnt, BSEIDMMask); }

    public synchronized static boolean bseIsValidHandle(final String handle)
    { return SysUtil.isValidHandleID("bytestreameditor", handle) && _bseList.containsKey(handle); }

    public synchronized static String bseNew(byte defVal)
    {
        final String           handle = _newBSEHandle();
        final ByteStreamEditor newBSE = new ByteStreamEditor(defVal);

        _bseList.put(handle, newBSE);

        return handle;
    }

    public synchronized static String bseNew()
    { return bseNew( (byte) 0 ); }

    public synchronized static boolean bseDelete(final String handle)
    { return _bseList.remove(handle) != null; }

    public synchronized static ByteStreamEditor bseGet(final String handle)
    { return _bseList.get(handle); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static class ByteStreamEditor {

      //private static final int CapTotStart = 4;
        private static final int CapTotStart = 1024;
        private static final int CapIncMFac  = 2;
        private static final int CapIncSFac  = Math.max(CapTotStart / CapIncMFac / 8, 1);

        private final byte    _DefVal;

        private       byte    _buff[] = new byte[CapTotStart];
        private       int     _capTot = CapTotStart;
        private       int     _capInc = CapIncSFac;
        private       int     _size   = 0;
        private       int     _cursor = 0;
        private       boolean _useLE  = SysUtil.osIsLE(); // By default use the native endianness

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public ByteStreamEditor(final byte devVal)
        {
            _DefVal = devVal;

            Arrays.fill(_buff, _DefVal);
        }

        private synchronized void _incCap_impl(final int mFac)
        {
            final int    newCapInc = _capInc * mFac;
            final int    newCapTot = _capTot + newCapInc;
            final byte[] newbuff   = new byte[newCapTot];

            /*
            SysUtil.stdOut().printf("_incCap() : %05d -> %05d\n", _capTot, newCapTot);
            //*/

            System.arraycopy(_buff, 0, newbuff, 0, _capTot);
            Arrays.fill(newbuff, _capTot, newCapTot, _DefVal);

            _capTot = newCapTot;
            _capInc = newCapInc;
            _buff   = newbuff;
        }

        private synchronized void _incCap()
        { _incCap_impl(CapIncMFac); }

        private synchronized void _incCapToAtLeast(final int newCapacity)
        {
            if(_capTot >= newCapacity) return;
            _incCap_impl( (newCapacity - _capTot) / _capInc + 1 );
        }

        private synchronized void _seek(final int ref, final int ofs)
        {
            _cursor = ref + ofs;
            if(_cursor < 0) _cursor = 0;

            while(_cursor >= _capTot) _incCap();

            _size = Math.max(_size, _cursor - 1);
        }

        private synchronized void _put(final byte value)
        {
            if(_cursor >= _capTot) _incCap();

            _buff[_cursor] = value;

            /*
            SysUtil.stdDbg().printf( "#%d = %02X\n", _cursor, value & 0xFF );
            //*/

            ++_cursor;
            ++_size;
        }

        private synchronized void _putMulti(final byte[] bytes)
        {
            if(bytes.length <= 0) return;

            _incCapToAtLeast(bytes.length);

            /*
            SysUtil.stdDbg().printf( "#%d to #%d \n", _cursor, _cursor + bytes.length - 1 );
            //*/

            for(final byte b : bytes) {
                _buff[_cursor] = b;
                ++_cursor;
                ++_size;
            }
        }

        private synchronized byte _get()
        {
            if(_cursor >= _size) return _DefVal;

            return _buff[_cursor++];
        }

        public synchronized int size()
        { return _size; }

        public synchronized int cursor()
        { return _cursor; }

        public synchronized void seekBeg(final int offset)
        { _seek(0, offset); }

        public synchronized void seekEnd(final int offset)
        { _seek(_size, offset); }

        public synchronized void seekCur(final int offset)
        { _seek(_cursor, offset); }

        public synchronized void truncate()
        {
            _size = _cursor - 1;
            if(_size < 0) _size = 0;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void setBE()
        { _useLE = false; }

        public synchronized void setLE()
        { _useLE = true; }

        public synchronized boolean isBE()
        { return !_useLE; }

        public synchronized boolean isLE()
        { return _useLE; }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void saveFile(final String path_) throws IOException
        {
            final Path path  = Paths.get(path_);

            Files.write( path, Arrays.copyOfRange(_buff, 0, _size) );
        }

        public synchronized void loadFile(final String path) throws IOException
        { _putMulti( SysUtil.readTextFileAsByteArray(path) ); }

        public synchronized void saveFile_jxm(final String path) throws JXMRuntimeError
        {
            try                        { saveFile(path);                                }
            catch(final IOException e) { throw XCom.newJXMRuntimeError( e.toString() ); }
        }

        public synchronized void loadFile_jxm(final String path) throws JXMRuntimeError
        {
            try                        { loadFile(path);                                }
            catch(final IOException e) { throw XCom.newJXMRuntimeError( e.toString() ); }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized String saveBase64() throws JXMRuntimeError
        { return XCom.base64StringFromByteArray( Arrays.copyOfRange(_buff, 0, _size) ); }

        public synchronized void loadBase64(final String base64Str) throws JXMRuntimeError
        {
            final byte[] bytes = XCom.byteArrayFromBase64String(base64Str);

            _putMulti(bytes);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void wrByte(final byte value)
        { _put(value); }

        public synchronized byte rdByte()
        { return _get(); }

        public synchronized void wrByte(final char value) throws JXMRuntimeError
        { wrByte( String.valueOf(value) ); }

        public synchronized void wrByte(final String value) throws JXMRuntimeError
        {
            try {
                // Try to parse it as a byte in string form first
                try {
                    _put( Byte.parseByte(value) );
                    return;
                }
                catch(final NumberFormatException e) {}
                // Convert from string to byte sequence and write the bytes
                final byte[] bytes = value.getBytes("UTF-8");
                _putMulti(bytes);
            }
            catch(final Exception e) {
                throw XCom.newJXMRuntimeError( e.toString() );
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void wrUInt08(final int value)
        { _put( (byte) (value & 0xFF) ); }

        public synchronized void wrSInt08(final int value)
        { wrUInt08(value); }

        public synchronized int rdUInt08()
        { return _get() & 0xFF; }

        public synchronized int rdSInt08()
        { return _get(); }

        public synchronized void wrUInt08(final String value)
        { wrUInt08( Integer.parseInt(value) ); }

        public synchronized void wrSInt08(final String value)
        { wrSInt08( Integer.parseInt(value) ); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void wrUInt16(final int value)
        {
            if(_useLE) {
                _put( (byte) ( (value >> 0) & 0xFF ) );
                _put( (byte) ( (value >> 8) & 0xFF ) );
            }
            else {
                _put( (byte) ( (value >> 8) & 0xFF ) );
                _put( (byte) ( (value >> 0) & 0xFF ) );
            }
        }

        public synchronized void wrSInt16(final int value)
        { wrUInt16(value); }

        public synchronized int rdUInt16()
        {
            final byte b0 = _get();
            final byte b1 = _get();

            if(_useLE) {
                return ( (b1 & 0xFF) << 8 ) |
                       ( (b0 & 0xFF) << 0 );
            }
            else {
                return ( (b0 & 0xFF) << 8 ) |
                       ( (b1 & 0xFF) << 0 );
            }
        }

        public synchronized int rdSInt16()
        { return (short) rdUInt16(); }

        public synchronized void wrUInt16(final String value)
        { wrUInt16( Integer.parseInt(value) ); }

        public synchronized void wrSInt16(final String value)
        { wrSInt16( Integer.parseInt(value) ); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void wrUInt32(final long value)
        {
            if(_useLE) {
                _put( (byte) ( (value >>  0) & 0xFF ) );
                _put( (byte) ( (value >>  8) & 0xFF ) );
                _put( (byte) ( (value >> 16) & 0xFF ) );
                _put( (byte) ( (value >> 24) & 0xFF ) );
            }
            else {
                _put( (byte) ( (value >> 24) & 0xFF ) );
                _put( (byte) ( (value >> 16) & 0xFF ) );
                _put( (byte) ( (value >>  8) & 0xFF ) );
                _put( (byte) ( (value >>  0) & 0xFF ) );
            }
        }

        public synchronized void wrSInt32(final long value)
        { wrUInt32(value); }

        public synchronized long rdUInt32()
        {
            final byte b0 = _get();
            final byte b1 = _get();
            final byte b2 = _get();
            final byte b3 = _get();

            if(_useLE) {
                return ( (long) (b3 & 0xFF) << 24 ) |
                       ( (long) (b2 & 0xFF) << 16 ) |
                       ( (long) (b1 & 0xFF) <<  8 ) |
                       ( (long) (b0 & 0xFF) <<  0 );
            }
            else {
                return ( (long) (b0 & 0xFF) << 24 ) |
                       ( (long) (b1 & 0xFF) << 16 ) |
                       ( (long) (b2 & 0xFF) <<  8 ) |
                       ( (long) (b3 & 0xFF) <<  0 );
            }
        }

        public synchronized long rdSInt32()
        { return (int) rdUInt32(); }

        public synchronized void wrUInt32(final String value)
        { wrUInt32( Long.parseLong(value) ); }

        public synchronized void wrSInt32(final String value)
        { wrSInt32( Long.parseLong(value) ); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void wrSInt64(final long value)
        {
            if(_useLE) {
                _put( (byte) ( (value >>  0) & 0xFF ) );
                _put( (byte) ( (value >>  8) & 0xFF ) );
                _put( (byte) ( (value >> 16) & 0xFF ) );
                _put( (byte) ( (value >> 24) & 0xFF ) );
                _put( (byte) ( (value >> 32) & 0xFF ) );
                _put( (byte) ( (value >> 40) & 0xFF ) );
                _put( (byte) ( (value >> 48) & 0xFF ) );
                _put( (byte) ( (value >> 56) & 0xFF ) );
            }
            else {
                _put( (byte) ( (value >> 56) & 0xFF ) );
                _put( (byte) ( (value >> 48) & 0xFF ) );
                _put( (byte) ( (value >> 40) & 0xFF ) );
                _put( (byte) ( (value >> 32) & 0xFF ) );
                _put( (byte) ( (value >> 24) & 0xFF ) );
                _put( (byte) ( (value >> 16) & 0xFF ) );
                _put( (byte) ( (value >>  8) & 0xFF ) );
                _put( (byte) ( (value >>  0) & 0xFF ) );
            }
        }

        public void wrUInt64(final BigInteger value)
        { wrUInt64( value.toString() ); }

        public long rdSInt64()
        {
            final byte b0 = _get();
            final byte b1 = _get();
            final byte b2 = _get();
            final byte b3 = _get();
            final byte b4 = _get();
            final byte b5 = _get();
            final byte b6 = _get();
            final byte b7 = _get();

            if(_useLE) {
                return ( (long) (b7 & 0xFF) << 56 ) |
                       ( (long) (b6 & 0xFF) << 48 ) |
                       ( (long) (b5 & 0xFF) << 40 ) |
                       ( (long) (b4 & 0xFF) << 32 ) |
                       ( (long) (b3 & 0xFF) << 24 ) |
                       ( (long) (b2 & 0xFF) << 16 ) |
                       ( (long) (b1 & 0xFF) <<  8 ) |
                       ( (long) (b0 & 0xFF) <<  0 );
            }
            else {
                return ( (long) (b0 & 0xFF) << 56 ) |
                       ( (long) (b1 & 0xFF) << 48 ) |
                       ( (long) (b2 & 0xFF) << 40 ) |
                       ( (long) (b3 & 0xFF) << 32 ) |
                       ( (long) (b4 & 0xFF) << 24 ) |
                       ( (long) (b5 & 0xFF) << 16 ) |
                       ( (long) (b6 & 0xFF) <<  8 ) |
                       ( (long) (b7 & 0xFF) <<  0 );
            }
        }

        public synchronized BigInteger rdUInt64()
        { return new BigInteger( Long.toUnsignedString( rdSInt64() ), 10 ); }

        public synchronized void wrSInt64(final String value)
        { wrSInt64( Long.parseLong(value) ); }

        public synchronized void wrUInt64(final String value)
        { wrSInt64( Long.parseUnsignedLong(value) ); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void wrFlt32(final float value)
        { wrSInt32( Float.floatToIntBits(value) ); }

        public synchronized float rdFlt32()
        { return Float.intBitsToFloat( (int) rdSInt32() ); }

        public synchronized void wrFlt32(final String value)
        { wrFlt32( Float.parseFloat(value) ); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void wrDbl64(final double value)
        { wrSInt64( Double.doubleToLongBits(value) ); }

        public synchronized double rdDbl64()
        { return Double.longBitsToDouble( rdSInt64() ); }

        public synchronized void wrDbl64(final String value)
        { wrDbl64( Double.parseDouble(value) ); }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void wrUTF8(final String str, final boolean writeLength) throws UnsupportedEncodingException
        {
            final byte[] bytes = str.getBytes("UTF-8");

            if(writeLength) wrSInt32(bytes.length);

            _putMulti(bytes);

            if(!writeLength) wrUInt08(0);
        }

        public synchronized void wrUTF8(final String str) throws UnsupportedEncodingException
        { wrUTF8(str, true); }

        public synchronized String rdUTF8(final boolean readLength) throws UnsupportedEncodingException
        {
            int bytesLength = readLength ? ( (int) rdSInt32() ) : 0;

            if(bytesLength == 0) {
                for(int i = _cursor; i < _size; ++i) {
                    if(_buff[i] == 0) break;
                    ++bytesLength;
                }
            }

            final byte[] bytes = new byte[bytesLength];

            for(int i = 0; i < bytesLength; ++i) bytes[i] = _get();

            if(!readLength) rdUInt08();

            return new String(bytes, "UTF-8");
        }

        public synchronized String rdUTF8() throws UnsupportedEncodingException
        { return rdUTF8(true); }

        public synchronized void wrUTF8_jxm(final String str, final boolean writeLength) throws JXMRuntimeError
        {
            try                                         { wrUTF8(str, writeLength);                      }
            catch(final UnsupportedEncodingException e) { throw XCom.newJXMRuntimeError( e.toString() ); }
        }

        public synchronized String rdUTF8_jxm(final boolean readLength) throws JXMRuntimeError
        {
            try                                         { return rdUTF8(readLength);                     }
            catch(final UnsupportedEncodingException e) { throw XCom.newJXMRuntimeError( e.toString() ); }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void wrUTF16(final String str, final boolean writeLength) throws UnsupportedEncodingException
        {
            final byte[] bytes = str.getBytes(_useLE ? "UTF-16LE" : "UTF-16BE");

            if(writeLength) wrSInt32(bytes.length);

            _putMulti(bytes);

            if(!writeLength) wrUInt16(0);
        }

        public synchronized void wrUTF16(final String str) throws UnsupportedEncodingException
        { wrUTF16(str, true); }

        public synchronized String rdUTF16(final boolean readLength) throws UnsupportedEncodingException
        {
            int bytesLength = readLength ? ( (int) rdSInt32() ) : 0;

            if(bytesLength == 0) {
                for(int i = _cursor; i < _size; i += 2) {
                    if(_buff[i] == 0 && _buff[i + 1] == 0) break;
                    ++bytesLength;
                }
                bytesLength *= 2;
            }

            final byte[] bytes = new byte[bytesLength];

            for(int i = 0; i < bytesLength; ++i) bytes[i] = _get();

            if(!readLength) rdUInt16();

            return new String(bytes, _useLE ? "UTF-16LE" : "UTF-16BE");
        }

        public String rdUTF16() throws UnsupportedEncodingException
        { return rdUTF16(true); }

        public synchronized void wrUTF16_jxm(final String str, final boolean writeLength) throws JXMRuntimeError
        {
            try                                         { wrUTF16(str, writeLength);                     }
            catch(final UnsupportedEncodingException e) { throw XCom.newJXMRuntimeError( e.toString() ); }
        }

        public synchronized String rdUTF16_jxm(final boolean readLength) throws JXMRuntimeError
        {
            try                                         { return rdUTF16(readLength);                    }
            catch(final UnsupportedEncodingException e) { throw XCom.newJXMRuntimeError( e.toString() ); }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void wrUTF32(final String str, final boolean writeLength) throws UnsupportedEncodingException
        {
            final byte[] bytes = str.getBytes(_useLE ? "UTF-32LE" : "UTF-32BE");

            if(writeLength) wrSInt32(bytes.length);

            _putMulti(bytes);

            if(!writeLength) wrUInt32(0);
        }

        public synchronized void wrUTF32(final String str) throws UnsupportedEncodingException
        { wrUTF32(str, true); }

        public synchronized String rdUTF32(final boolean readLength) throws UnsupportedEncodingException
        {
            int bytesLength = readLength ? ( (int) rdSInt32() ) : 0;

            if(bytesLength == 0) {
                for(int i = _cursor; i < _size; i += 4) {
                    if(_buff[i] == 0 && _buff[i + 1] == 0 && _buff[i + 2] == 0 && _buff[i + 3] == 0) break;
                    ++bytesLength;
                }
                bytesLength *= 4;
            }

            final byte[] bytes = new byte[bytesLength];

            for(int i = 0; i < bytesLength; ++i) bytes[i] = _get();

            if(!readLength) rdUInt32();

            return new String(bytes, _useLE ? "UTF-32LE" : "UTF-32BE");
        }

        public synchronized String rdUTF32() throws UnsupportedEncodingException
        { return rdUTF32(true); }

        public synchronized void wrUTF32_jxm(final String str, final boolean writeLength) throws JXMRuntimeError
        {
            try                                         { wrUTF32(str, writeLength);                     }
            catch(final UnsupportedEncodingException e) { throw XCom.newJXMRuntimeError( e.toString() ); }
        }

        public synchronized String rdUTF32_jxm(final boolean readLength) throws JXMRuntimeError
        {
            try                                         { return rdUTF32(readLength);                    }
            catch(final UnsupportedEncodingException e) { throw XCom.newJXMRuntimeError( e.toString() ); }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void rangeClear(final int len)
        {
            // Determine the range to be cleared
            final int cBeg = _cursor;
            final int cEnd = Math.min(cBeg + len, _size);
            final int cLen = cEnd - cBeg;

            // Determine the range to be moved
            final int mBeg = cEnd;
            final int mEnd = _size;
                  int mCnt = cBeg;

            // Move the bytes
            for(int i = mBeg; i < mEnd; ++i) {
                _buff[mCnt++] = _buff[i];
            }

            // Clear the bytes (reduce the size)
            _size -= cLen;
            if(_cursor > _size) _cursor = _size;
        }

        public synchronized String rangeCopy(final int len)
        {
            // Determine the range to be copied
            final int cBeg = _cursor;
            final int cEnd = Math.min(cBeg + len, _size);

            // Create a new byte-stream-editor object
            final String newEH = bseNew(_DefVal);

            // Copy the bytes
            bseGet(newEH)._putMulti( Arrays.copyOfRange(_buff, cBeg, cEnd) );

            // Return the handle to the new byte-stream-editor object
            return newEH;
        }

        public synchronized String rangeCut(final int len)
        {
            // Create a new byte-stream-editor object to hold the copied bytes
            final String newEH = rangeCopy(len);

            // Clear the range
            rangeClear(len);

            // Return the handle to the new byte-stream-editor object
            return newEH;
        }

        public synchronized void rangePaste(final String handle)
        {
            // Get the and check the source byte-stream-editor object
            final ByteStreamEditor srcEH = bseGet(handle);
            if(srcEH == null) return;

            // Get and check the size of the byte-stream-editor object
            final int sLen = srcEH._size;
            if(sLen <= 0) return;

            // Increase the capacity
            _incCapToAtLeast(_size + sLen);

            // Move the bytes
            for(int i = _size - 1; i >= _cursor; --i) {
                _buff[i + sLen] = _buff[i];
            }

            // Copy the bytes
            for(int i = 0; i < sLen; ++i) {
                _buff[_cursor + i] = srcEH._buff[i];
            }

            // Increase the size
            _size += sLen;

            // Update the cursor position
            _cursor += sLen;
        }

        public synchronized void rangeOverwrite(final String handle)
        {
            // Get the and check the source byte-stream-editor object
            final ByteStreamEditor srcEH = bseGet(handle);
            if(srcEH == null) return;

            // Get and check the size of the byte-stream-editor object
            final int sLen = srcEH._size;
            if(sLen <= 0) return;

            // Increase the capacity
            _incCapToAtLeast(_cursor + sLen);

            // Copy the bytes
            for(int i = 0; i < sLen; ++i) {
                _buff[_cursor + i] = srcEH._buff[i];
            }

            // Increase the size
            _size = Math.max(_size, _cursor + sLen);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////

        public synchronized void dump()
        {
            SysUtil.stdOut().printf("[#%d] [%d/%d] [+%d]\n", _cursor, _size, _capTot, _capInc);

            for(int i = 0; i < _size; ++i) {
                SysUtil.stdOut().printf("%02X ", _buff[i]);
            }
            if(_size == 0) SysUtil.stdOut().println("<empty>");
            else           SysUtil.stdOut().println(         );

            SysUtil.stdOut().println();
        }

        public synchronized void dumpExt(final String msg)
        {
            if(msg != null) SysUtil.stdOut().println(msg);

            SysUtil.stdOut().printf("[@%08X] [#%d] [%d/%d] [+%d]\n", Objects.hashCode(this), _cursor, _size, _capTot, _capInc);

            if(_size == 0) {
                SysUtil.stdOut().println("<empty>");
            }
            else {
                // Print the indexes
                SysUtil.stdOut().print("# ");
                for(int i = 0; i < _size; ++i) {
                    SysUtil.stdOut().printf("%02X ", i);
                }
                SysUtil.stdOut().println();
                // Print the values
                SysUtil.stdOut().print("= ");
                for(int i = 0; i < _size; ++i) {
                    SysUtil.stdOut().printf("%02X ", _buff[i]);
                }
                SysUtil.stdOut().println();
                // Print the cursor
                SysUtil.stdOut().print("  ");
                for(int i = 0; i < _cursor; ++i) {
                    SysUtil.stdOut().print("   ");
                }
                SysUtil.stdOut().println("^^ ");
            }

            SysUtil.stdOut().println();
        }

        public synchronized void dumpExt()
        { dumpExt(null); }

    } // class ByteStreamEditor

} // class BSEList
