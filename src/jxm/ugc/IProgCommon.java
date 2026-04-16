/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.util.function.IntConsumer;


public interface IProgCommon {

    public abstract int _flashMemoryTotalSize();
    public abstract byte _flashMemoryEmptyValue();
    public abstract int _flashMemoryAlignWriteSize(final int numBytes);

    public abstract int _eepromMemoryTotalSize();
    public abstract byte _eepromMemoryEmptyValue(); // NOTE : The empty value of EEPROM memory is usually the same as the empty value of flash memory

    public abstract int[] _readDataBuff();

    public abstract boolean end();

    public abstract boolean supportSignature();
    public abstract boolean readSignature();
    public abstract boolean verifySignature(final int[] signatureBytes);
    public abstract int[] mcuSignature();

    public abstract boolean chipErase();

    public abstract boolean readFlash(final int startAddress, final int numBytes, final IntConsumer progressCallback);
    public abstract boolean writeFlash(final byte[] data, final int startAddress, final int numBytes, final IntConsumer progressCallback);
    public abstract int verifyFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback);

    public abstract int readEEPROM(final int address);
    public abstract boolean writeEEPROM(final int address, final byte data);

    public abstract long readLockBits();
    public abstract boolean writeLockBits(final long value);

} // class IProgCommon
