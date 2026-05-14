/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.Arrays;

import jxm.xb.*;
import jxm.tool.*;


@SuppressWarnings( { "unchecked", "serial" } )
public class SerializableDeepClone<T> implements Serializable {

    public T deepClone() throws Exception
    { return applyDeepClone( (T) this ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static <T> T applyDeepClone(final T inst) throws ClassNotFoundException, IOException
    {
        // Serialize the object
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try(
            final ObjectOutputStream oos = new ObjectOutputStream(bos)
        ) {
            oos.writeObject(inst);
            oos.flush();
        }

        // Deserialize and create a new object
        try(
            final ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( bos.toByteArray() ) )
        ) {
            // Return the new object
            return (T) ois.readObject();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static <T> boolean equals(final T inst1, final T inst2)
    {
        try {

            // Serialize the 1st object
            final ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
            try(
                final ObjectOutputStream oos1 = new ObjectOutputStream(bos1)
            ) {
                oos1.writeObject(inst1);
                oos1.flush();
            }

            // Serialize the 2nd object
            final ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
            try(
                final ObjectOutputStream oos2 = new ObjectOutputStream(bos2)
            ) {
                oos2.writeObject(inst2);
                oos2.flush();
            }

            // Compare the byte stream and return the comparison result
            return Arrays.equals( bos1.toByteArray(), bos2.toByteArray() );

        } // try
        catch(final IOException  e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Return as not equals
            return false;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String serializeToBase64String(final Object obj) throws IOException
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(
            final ObjectOutputStream oos = new ObjectOutputStream(baos)
        ) {
            oos.writeObject(obj);
            oos.flush();
        }

        return XCom.base64StringFromByteArray( baos.toByteArray() );
    }

    public static Object deserializeFromBase64String(final String str) throws IOException, ClassNotFoundException
    {
        try(
            final ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( XCom.byteArrayFromBase64String(str) ) )
        ) {
            return ois.readObject();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static < T extends SerializableDeepClone<T> > String toJSON(final SerializableDeepClone<T> object)
    {
        try {
            return JSONEncoderLite.serialize(object);
        }
        catch(final Exception e) {
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            return null;
        }
    }

    public static < T /*extends SerializableDeepClone<T>*/ > T fromJSON(final String jsonString, final Class<T> rootType)
    {
        // Error if 'rootType' is not a direct (or indirect) subclass of SerializableDeepClone<?>
        Class<?> chk = rootType;

        while(true) {
            final Class<?> sup = chk.getSuperclass();
            if(sup == null) return null;
            if( sup.getName().equals( SerializableDeepClone.class.getName() ) ) break;
            chk = sup;
        }

        // Deserialize
        try {
            return rootType.cast( JSONDecoder.deserialize(jsonString, rootType) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Return error
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static < T extends SerializableDeepClone<T> > String toINI(final SerializableDeepClone<T> object)
    {
        try {
            return INIEncoderLite.serialize(object);
        }
        catch(final Exception e) {
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            return null;
        }
    }

    public static < T /*extends SerializableDeepClone<T>*/ > T fromINI(final String iniString, final Class<T> rootType)
    {
        // Error if 'rootType' is not a direct (or indirect) subclass of SerializableDeepClone<?>
        Class<?> chk = rootType;

        while(true) {
            final Class<?> sup = chk.getSuperclass();
            if(sup == null) return null;
            if( sup.getName().equals( SerializableDeepClone.class.getName() ) ) break;
            chk = sup;
        }

        // Deserialize
        try {
            return rootType.cast( INIDecoder.deserialize(iniString, rootType) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Return error
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static < T /*extends SerializableDeepClone<T>*/ > T fromPSpecStr(final String pspecString, final Class<T> rootType)
    {
        final String str = pspecString.trim();

        // Parse as JSON
        if( ( str.startsWith("{") || str.startsWith("[") ) && !str.startsWith("[CLASS:") ) return fromJSON(pspecString, rootType);

        // Parse as INI
        return fromINI(pspecString, rootType);
    }

} // class SerializableDeepClone
