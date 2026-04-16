/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.tool;


import java.util.ArrayList;
import java.util.Set;

import jxm.*;
import jxm.tool.*;
import jxm.xb.*;


public class JSONUtil {

    private static final String _NMap_UnnamedArray = "__Array__";

    private static void _jsonExtractValue(final String mapHandle, final String key, final JSONDecoder.JSONValue jsonValue) throws Exception
    {
        // Process as a 'JSONArray'
        if( jsonValue.isArray() ) {
            final String mhArray = MapList.mapNew();
            _jsonExtractArray( mhArray, jsonValue.getArray() );
            MapList.mapPut(mapHandle, key, mhArray, true, false);
        }

        // Process as a 'JSONObject'
        else if( jsonValue.isObject() ) {
            final String mhObject = MapList.mapNew();
            _jsonExtractObject( mhObject, jsonValue.getObject() );
            MapList.mapPut(mapHandle, key, mhObject, true, false);
        }

        // Process as a primitive object
        else {
            MapList.mapPut( mapHandle, key, jsonValue.getString(true), true, false );
        }
    }

    private static void _jsonExtractArray(final String mapHandle, final JSONDecoder.JSONArray jsonArray) throws Exception
    {
        for(final JSONDecoder.JSONValue jsv : jsonArray) {

            _jsonExtractValue(mapHandle, _NMap_UnnamedArray, jsv);

        } // for
    }

    private static void _jsonExtractObject(final String mapHandle, final JSONDecoder.JSONObject jsonObject) throws Exception
    {
        final Set<String> keys = jsonObject.keySet();

        for(final String key : keys) {

            _jsonExtractValue (mapHandle, key, jsonObject.get(key) );

        } // for
    }

    public static String jsonStringToNestedMap(final String jsonStr) throws Exception
    {
        // Create a new root map
        final String mhRoot = MapList.mapNew();

        try {

            // Decode the JSON string
            final JSONDecoder.JSONValue jsonValue = JSONDecoder.decode(jsonStr);

            // Process the value(s)
            if( jsonValue.isArray() ) {
                _jsonExtractArray( mhRoot, jsonValue.getArray() );
            }
            else if( jsonValue.isObject() ) {
                _jsonExtractObject( mhRoot, jsonValue.getObject() );
            }
            else {
                // ##### ??? TODO : What to do here ??? #####
                throw XCom.newJXMFatalLogicError(Texts.EMsg_NotImplementedYet);
            }

        } // try
        catch(final Exception e) {
            // Delete the nested map
            nmapDelete(mhRoot);
            // Re-throw the exception
            throw e;
        }

        // Return the new root map handle
        return mhRoot;
    }

    public static String jsonFileToNestedMap(final String jsonFilePath) throws Exception
    { return jsonStringToNestedMap( SysUtil.readTextFileAsString(jsonFilePath) ); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static void nmapDelete(final String handle)
    {
        final Set<String> keys = MapList.mapKeys(handle);
        if(keys == null) return;

        for(final String key : keys) {
            for( final String val : MapList.mapGet(handle, key) ) {
                if( MapList.mapIsValidHandle(val) ) MapList.mapDelete(val);
            }
        }

        MapList.mapDelete(handle);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void _nmapDump(final String padding, final String handle)
    {
        final Set<String> keys = MapList.mapKeys(handle);
        if(keys == null) return;

        for(final String key : keys) {

            final boolean           isUA       = _NMap_UnnamedArray.equals(key);
            final ArrayList<String> valArray   = MapList.mapGet(handle, key);
            final int               valCount   = valArray.size();
                  int               valIndex   = 0;
                  boolean           keyPrinted = false;

            for(final String val : valArray) {

                final boolean isMH = MapList.mapIsValidHandle(val);

                if(!keyPrinted) {
                    if(!isUA && isMH) SysUtil.stdOut().printf( "%s[%s:%s]\n", padding, key, SysUtil.strHandleID(val) );
                    else              SysUtil.stdOut().printf( "%s[%s]\n"   , padding, key                           );
                    keyPrinted = true;
                }

                if     (isUA && isMH) SysUtil.stdOut().printf( "%s    #%d:%s\n", padding, valIndex++, SysUtil.strHandleID(val) );
                else if(valCount > 1) SysUtil.stdOut().printf( "%s    #%d\n"   , padding, valIndex++                           );

                if(isMH) _nmapDump(padding + "    ", val);
                else     SysUtil.stdOut().printf("%s    %s\n", padding, val);

            } // for val

        } // for key
    }

    public synchronized static void nmapDump(final String handle)
    {
        _nmapDump("", handle);
        SysUtil.stdOut().println();
    }

} // class JSONUtil
