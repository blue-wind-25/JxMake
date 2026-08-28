/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.ugc;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.function.IntConsumer;

import jxm.Texts;
import jxm.xb.XCom;


/*
 * Reflection wrapper for the USB/FFM-dependent boot loader programmer classes (ProgBootSTM32DFU,
 * ProgBootLUFAHID, ProgBootAVRDFU, ProgBootUSBasp).
 *
 * Those classes -- along with their common ancestor ProgBootUSB and its abstract subclasses
 * ProgBootUSB_DFU/ProgBootUSB_HID -- depend on the JavaDoesUSB library and the Foreign Function
 * & Memory API, and are therefore only compiled into the build when using Java 25 or later (see
 * the `ExcludeFFM` logic in the `Makefile`). When building with an older JDK those classes do
 * not exist as compilation units at all, so `ProgExec.java` cannot reference them directly without
 * breaking the Java 8 build.
 *
 * The nested classes below resolve the real classes at runtime via reflection instead, so `ProgExec.java`
 * can reference `ProgExec_RW.STM32DFU/LUFAHID/AVRDFU/USBasp` unconditionally and compile cleanly
 * under both Java 8 and Java 25+. When the JAR was built with Java < 25 (so the real classes are
 * absent from the classpath), `isAvailable()` returns false and every other method throws.
 *
 * Each wrapper implements `IProgCommon` itself (that interface has no FFM dependency and is always
 * compiled) and simply forwards those calls to the reflectively-constructed instance, so callers
 * can pass a wrapper anywhere an `IProgCommon` is expected. The `IProgCommon` delegation and the
 * reflective construction are factored into the common `_Base` class below; each wrapper subclass
 * only supplies its target class name and its class-specific `begin(...)` overloads.
 */
final class ProgExec_RW
{
    // Keep in sync with the `JAVA_MAJOR ... -lt 25` check that drives `ExcludeFFM` in the `Makefile`
    // that is what actually determines whether the wrapped classes are compiled into the build in the
    // first place
    private static final int MinJavaVersion = 25;

    private ProgExec_RW()
    {}

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean _isAvailable(final String className)
    {
        try { Class.forName(className); return true; }
        catch(final ClassNotFoundException e) { return false; }
    }

    private static Class<?> _requireClass(final String className) throws Exception
    {
        try {
            return Class.forName(className);
        }
        catch(final ClassNotFoundException e) {
            throw XCom.newJXMRuntimeError(Texts.EMsg_ProgExecRWClassUnavail, "_requireClass", className, MinJavaVersion);
        }
    }

    private static Class<?> _configClass(final String className) throws Exception
    { return _requireClass(className + "$Config"); }

    private static Object _newConfig(final String className) throws Exception
    { return _unwrap( () -> _configClass(className).getDeclaredConstructor().newInstance() ); }

    private static IProgCommon _construct(final String className, final Object config) throws Exception
    {
        final Class<?>       clazz  = _requireClass(className);
        final Constructor<?> ctor   = clazz.getConstructor(_configClass(className));
        return (IProgCommon) _unwrap( () -> ctor.newInstance(config) );
    }

    private static boolean _invokeBegin(final String className, final IProgCommon impl, final Class<?>[] paramTypes, final Object... args) throws Exception
    {
        final Method method = _requireClass(className).getMethod("begin", paramTypes);

        return (Boolean) _unwrap( () -> method.invoke(impl, args) );
    }

    // Small helper interface so reflective calls can be written once and have their checked
    // `InvocationTargetException`/`InstantiationException`/`IllegalAccessException` unwrapped
    // consistently, re-throwing the real (target) exception where possible
    private interface _ReflectCall { Object call() throws Exception; }

    private static Object _unwrap(final _ReflectCall call) throws Exception
    {
        try { return call.call(); }
        catch(final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if(cause instanceof Exception) throw (Exception) cause;
            if(cause instanceof Error)     throw (Error)     cause;
            throw e;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    // Common base for the wrapper subclasses below: holds the reflectively-constructed instance and
    // forwards every `IProgCommon` method to it. Subclasses only need to pass their target class name to
    // the constructor and add their own `begin(...)` overloads (via the protected `_begin()` helper),
    // since the `begin` method's signature differs per wrapped class
    abstract static class _Base implements IProgCommon
    {
        private final String       _className;
        private final IProgCommon  _impl;

        _Base(final String className, final Object config) throws Exception
        {
            _className = className;
            _impl      = _construct(className, config);
        }

        final boolean _begin(final Class<?>[] paramTypes, final Object... args) throws Exception
        { return _invokeBegin(_className, _impl, paramTypes, args); }

        public int     _flashMemoryTotalSize()                        { return _impl._flashMemoryTotalSize(); }
        public byte    _flashMemoryEmptyValue()                       { return _impl._flashMemoryEmptyValue(); }
        public int     _flashMemoryAlignWriteSize(final int numBytes) { return _impl._flashMemoryAlignWriteSize(numBytes); }
        public int     _eepromMemoryTotalSize()                       { return _impl._eepromMemoryTotalSize(); }
        public byte    _eepromMemoryEmptyValue()                      { return _impl._eepromMemoryEmptyValue(); }
        public int[]   _readDataBuff()                                { return _impl._readDataBuff(); }

        public boolean end()                                       { return _impl.end(); }
        public boolean supportSignature()                          { return _impl.supportSignature(); }
        public boolean readSignature()                             { return _impl.readSignature(); }
        public boolean verifySignature(final int[] signatureBytes) { return _impl.verifySignature(signatureBytes); }
        public int[]   mcuSignature()                              { return _impl.mcuSignature(); }

        public boolean chipErase()                                     { return _impl.chipErase(); }
        public int     readEEPROM(final int address)                   { return _impl.readEEPROM(address); }
        public boolean writeEEPROM(final int address, final byte data) { return _impl.writeEEPROM(address, data); }
        public long    readLockBits()                                  { return _impl.readLockBits(); }
        public boolean writeLockBits(final long value)                 { return _impl.writeLockBits(value); }

        public boolean readFlash(final int startAddress, final int numBytes, final IntConsumer progressCallback)
        { return _impl.readFlash(startAddress, numBytes, progressCallback); }

        public boolean writeFlash(final byte[] data, final int startAddress, final int numBytes, final IntConsumer progressCallback)
        { return _impl.writeFlash(data, startAddress, numBytes, progressCallback); }

        public int verifyFlash(final byte[] refData, final int startAddress, final int numBytes, final IntConsumer progressCallback)
        { return _impl.verifyFlash(refData, startAddress, numBytes, progressCallback); }

    } // class _Base

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    static final class STM32DFU extends _Base
    {
        private static final String ClassName = "jxm.ugc.ProgBootSTM32DFU";

        static boolean   isAvailable()                  { return _isAvailable(ClassName); }
        static Class<?>  configClass() throws Exception { return _configClass(ClassName); }
        static Object    newConfig  () throws Exception { return _newConfig  (ClassName); }

        STM32DFU(final Object config) throws Exception
        { super(ClassName, config); }

        boolean begin(final int vid, final int pid, final String serialNumber) throws Exception
        { return _begin(new Class<?>[]{ int.class, int.class, String.class }, vid, pid, serialNumber); }

        boolean begin(final String serialNumber) throws Exception
        { return _begin(new Class<?>[]{ String.class }, serialNumber); }

    } // class STM32DFU

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    static final class LUFAHID extends _Base
    {
        private static final String ClassName = "jxm.ugc.ProgBootLUFAHID";

        static boolean   isAvailable()                  { return _isAvailable(ClassName); }
        static Class<?>  configClass() throws Exception { return _configClass(ClassName); }
        static Object    newConfig  () throws Exception { return _newConfig  (ClassName); }

        LUFAHID(final Object config) throws Exception
        { super(ClassName, config); }

        boolean begin(final int vid, final int pid) throws Exception
        { return _begin(new Class<?>[]{ int.class, int.class }, vid, pid); }

        boolean begin() throws Exception
        { return _begin(new Class<?>[]{}); }

    } // class LUFAHID

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    static final class AVRDFU extends _Base
    {
        private static final String ClassName = "jxm.ugc.ProgBootAVRDFU";

        static boolean   isAvailable()                  { return _isAvailable(ClassName); }
        static Class<?>  configClass() throws Exception { return _configClass(ClassName); }
        static Object    newConfig  () throws Exception { return _newConfig  (ClassName); }

        AVRDFU(final Object config) throws Exception
        { super(ClassName, config); }

        boolean begin(final int vid, final int pid, final String serialNumber) throws Exception
        { return _begin(new Class<?>[]{ int.class, int.class, String.class }, vid, pid, serialNumber); }

        boolean begin(final String serialNumber) throws Exception
        { return _begin(new Class<?>[]{ String.class }, serialNumber); }

    } // class AVRDFU

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    static final class USBasp extends _Base
    {
        private static final String ClassName = "jxm.ugc.ProgBootUSBasp";

        static boolean   isAvailable()                  { return _isAvailable(ClassName); }
        static Class<?>  configClass() throws Exception { return _configClass(ClassName); }
        static Object    newConfig  () throws Exception { return _newConfig  (ClassName); }

        USBasp(final Object config) throws Exception
        { super(ClassName, config); }

        boolean begin(final int vid, final int pid, final String manufacturerString, final String productString, final String serialNumber) throws Exception
        {
            return _begin(
                new Class<?>[]{ int.class, int.class, String.class, String.class, String.class },
                vid, pid, manufacturerString, productString, serialNumber
            );
        }

        boolean begin() throws Exception
        { return _begin(new Class<?>[]{}); }

    } // class USBasp

} // class ProgExec_RW
