/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;

import java.util.Arrays;


public final class Bytes {

    private byte[] _data = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean empty()
    { return (_data == null) || (_data.length == 0); }

    public int length()
    { return empty() ? 0 : _data.length; }

    public byte[] data()
    { return _data; }

    public void clear()
    { _data = null; }

    public void appendAfter(final byte[] array, final int offset)
    {
        // Calculate the new total length
        int length = length() + array.length - offset;

        if(length == 0) return;

        // Combine the bytes
        final byte[] result = new byte[length];
              int    pos    = 0;

        if( !empty() ) {
            System.arraycopy(_data, 0, result, pos, _data.length);
            pos += _data.length;
        }

        System.arraycopy(array, offset, result, pos, array.length - offset);

        // Store the result
        _data = result;
    }

    public void appendAfter(final byte[]... arrays)
    {
        // Calculate the new total length
        int length = length();

        for(byte[] array : arrays) {
            if(array == null) continue;
            length += array.length;
        }

        if(length == 0) return;

        // Combine the bytes
        final byte[] result = new byte[length];
              int    pos    = 0;

        if( !empty() ) {
            System.arraycopy(_data, 0, result, pos, _data.length);
            pos += _data.length;
        }

        for(byte[] array : arrays) {
            if(array == null) continue;
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }

        // Store the result
        _data = result;
    }

    public void appendBefore(final Bytes bytes)
    {
        final byte[] data = _data;

        clear();
        appendAfter(bytes._data, data);
    }

    public void appendAfter(final Bytes bytes)
    { appendAfter(bytes._data); }

    public boolean equals(final Bytes bytes)
    { return Arrays.equals(_data, bytes._data); }

} // Bytes
