/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.lang.reflect.Method;

import java.security.NoSuchAlgorithmException;

import jxm.*;


public class XCacheHelper {

    public  static final String  MagicWord = "JxMake_SpecFile_Cache";

    private static       boolean _verbose  = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void setVerboseMode(final boolean verbose)
    { _verbose = verbose; }

    public static boolean getVerboseMode()
    { return _verbose; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getCSpecFilePath(final String mainJXMSpecFile_absPath)
    {
        try {
            return SysUtil.getCacheFileName_compiledSpecFile(mainJXMSpecFile_absPath);
        }
        catch(final NoSuchAlgorithmException e) {}

        return null;
    }

    public static void saveCSpecFileHeader(final DataOutputStream dos) throws IOException
    {
        dos.writeUTF ( MagicWord             );
        dos.writeLong( SysUtil.jxmVerValue() );
        dos.writeUTF ( SysUtil.jxmVerDevel() );
    }

    public static boolean loadCSpecFileHeader(final DataInputStream dos) throws IOException
    {
        if( !dos.readUTF ().equals( MagicWord             ) ) return false;
        if(  dos.readLong() !=      SysUtil.jxmVerValue()   ) return false;
        if( !dos.readUTF ().equals( SysUtil.jxmVerDevel() ) ) return false;

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean saveIfNull(final DataOutputStream dos, final Object obj) throws IOException
    {
        final boolean isNull = (obj == null);

        dos.writeBoolean(!isNull);

        return isNull;
    }

    public static boolean loadIfNull(final DataInputStream dis) throws IOException
    { return !dis.readBoolean(); }


    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void saveExecBlocks(final DataOutputStream dos, final XCom.ExecBlocks execBlocs) throws IOException
    {
        // Save the number of execution blocks
        dos.writeInt( execBlocs.size() );

        // Save the execution blocks
        for(final ExecBlock eb : execBlocs) {
            // Save the type
            dos.writeUTF( eb.getClass().getName() );
            // Save the data
            eb.saveToStream(dos);
        }
    }

    public static final XCom.ExecBlocks loadExecBlocks(final DataInputStream dis) throws IOException
    {
        // Load the number of execution blocks
        final int ebCount = dis.readInt();

        // Load the execution blocks
        XCom.ExecBlocks execBlocks = new XCom.ExecBlocks();
        String          className  = null;

        try {
            for(int i = 0; i < ebCount; ++i) {
                // Get the exec block class name
                className = dis.readUTF();
                // Create a new instance from the data
                final Class<?> clazz  = Class.forName(className);
                final Method   method = clazz.getMethod("loadFromStream", DataInputStream.class);
                final Object   retVal = method.invoke( null, new Object[] { dis } );
                if(retVal == null) return null;
                // Store the instance to the list
                execBlocks.add( (ExecBlock) retVal );
            }
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newIOException( "loading class '" + ( className.isEmpty() ? "<ERROR>" :  className ) + "'\n" + e.toString() );
        }

        // Done
        return execBlocks;
    }

} // class XCacheHelper
