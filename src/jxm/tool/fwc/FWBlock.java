/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool.fwc;


import jxm.*;


public class FWBlock {

    private       long  _startAddress = -1;
    private final Bytes _bytes        = new Bytes();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean empty()
    { return ( _bytes.length() == 0 ); }

    public int length()
    { return _bytes.length(); }

    public byte[] data()
    { return _bytes.data(); }

    public void clear()
    { _bytes.clear(); }

    public long startAddress()
    { return _bytes.empty() ? -1 : _startAddress; }

    public long finalAddress()
    { return _bytes.empty() ? -1 : ( _startAddress + _bytes.length() - 1 ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public FWBlock()
    {}

    public FWBlock(final FWBlock fwBlock, final long addressOffset)
    {
        _startAddress = fwBlock._startAddress + addressOffset;
        _bytes.appendAfter(fwBlock._bytes);
    }

    public FWBlock(final byte[] data, final long startAddress)
    {
        _startAddress = startAddress;
        _bytes.appendAfter(data);
    }

    public FWBlock deepClone()
    {
        final FWBlock fwb = new FWBlock();

        fwb._startAddress = this._startAddress;
        fwb._bytes.appendAfter(this._bytes);

        return fwb;
    }

    public boolean equals(final FWBlock fwBlock)
    { return ( (_startAddress == fwBlock._startAddress) && _bytes.equals(fwBlock._bytes) ); }

    public int compareTo(final FWBlock fwBlock)
    {
        if(_startAddress < fwBlock._startAddress) return -1;
        if(_startAddress > fwBlock._startAddress) return  1;

        if( length() < fwBlock.length() ) return -1;
        if( length() > fwBlock.length() ) return  1;

        return 0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean overlapWith(final FWBlock fwBlock, final long addressOffset)
    {
        // Check if this block or the given block is empty
        if( empty() || fwBlock.empty() ) return false;

        // Check for intersection
        if( ( fwBlock.startAddress() + addressOffset ) >= startAddress() && ( fwBlock.startAddress() + addressOffset ) <= finalAddress() ) return true;
        if( ( fwBlock.finalAddress() + addressOffset ) >= startAddress() && ( fwBlock.finalAddress() + addressOffset ) <= finalAddress() ) return true;

        // No intersection
        return false;
    }

    public boolean appendBefore(final FWBlock fwBlock, final long addressOffset)
    {
        // Simply return 'true' if the block to be appended is empty
        if( fwBlock.empty() ) return true;

        // Check if this block is empty
        if( empty() ) {
            if(addressOffset != 0) return false;
            _bytes.appendAfter(fwBlock._bytes);
            return true;
        }

        // Cannot append if the final and start addresses are not consecutive
        if( ( fwBlock.finalAddress() + addressOffset ) != startAddress() - 1 ) return false;

        // Append before
        _bytes.appendBefore(fwBlock._bytes);

        // Done
        return true;
    }

    public boolean appendAfter(final FWBlock fwBlock, final long addressOffset)
    {
        // Simply return 'true' if the block to be appended is empty
        if( fwBlock.empty() ) return true;

        // Check if this block is empty
        if( empty() ) {
            if(addressOffset != 0) return false;
            _bytes.appendAfter(fwBlock._bytes);
            return true;
        }

        // Cannot append if the start and final addresses are not consecutive
        if( ( fwBlock.startAddress() + addressOffset ) != finalAddress() + 1 ) return false;

        // Append after
        _bytes.appendAfter(fwBlock._bytes);

        // Done
        return true;
    }

} // class FWBlock
