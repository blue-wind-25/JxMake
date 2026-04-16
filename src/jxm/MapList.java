/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;


import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.function.Supplier;

import jxm.xb.*;


public class MapList {

    //                            Handle           Key     Value(s)
    private static final HashMap< String, HashMap< String, ArrayList<String> > > _mapList      = new HashMap<> ();
    private static       long                                                    _mapHandleCnt = 0;

    private static final long                                                    MapIDMMask    = SysUtil.random32();

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private synchronized static String _newMapHandle()
    { return SysUtil.createHandleID("map", ++_mapHandleCnt, MapIDMMask); }

    public synchronized static boolean mapIsValidHandle(final String handle)
    { return SysUtil.isValidHandleID("map", handle) && _mapList.containsKey(handle); }

    public synchronized static String mapNew()
    {
        final String                               handle = _newMapHandle();
        final HashMap< String, ArrayList<String> > newMap = new HashMap<>();

        _mapList.put(handle, newMap);

        return handle;
    }

    public synchronized static boolean mapDelete(final String handle)
    { return _mapList.remove(handle) != null; }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static boolean mapClear(final String handle)
    {
        final HashMap< String, ArrayList<String> > map = _mapList.get(handle);
        if(map == null) return false;

        map.clear();

        return true;
    }

    public synchronized static boolean mapPut(final String handle, final String key, final String value, final boolean add, final boolean unique)
    {
        final HashMap< String, ArrayList<String> > map = _mapList.get(handle);
        if(map == null) return false;

        ArrayList<String> mvalues = map.get(key);
        if(mvalues == null) {
            mvalues = new ArrayList<String>();
            map.put(key, mvalues);
        }

        if(!add) mvalues.clear();

        if( !unique || !mvalues.contains(value) ) mvalues.add(value);

        return true;
    }

    public synchronized static boolean mapPut(final String handle, final String key, final ArrayList<String> values, final boolean add, final boolean unique)
    {
        final HashMap< String, ArrayList<String> > map = _mapList.get(handle);
        if(map == null) return false;

        ArrayList<String> mvalues = map.get(key);
        if(mvalues == null) {
            mvalues = new ArrayList<String>();
            map.put(key, mvalues);
        }

        if(!add) mvalues.clear();

        for(final String value : values) {
            if( !unique || !mvalues.contains(value) ) mvalues.add(value);
        }

        return true;
    }

    public synchronized static boolean mapRemove(final String handle, final String key)
    {
        final HashMap< String, ArrayList<String> > map = _mapList.get(handle);
        if(map == null) return false;

        return map.remove(key) != null;
    }

    public synchronized static ArrayList<String> mapGet(final String handle, final String key)
    {
        final HashMap< String, ArrayList<String> > map = _mapList.get(handle);
        if(map == null) return null;

        final ArrayList<String> mvalues = map.get(key);
        if(mvalues == null) return null;

        return mvalues;
    }

    public synchronized static Set<String> mapKeys(final String handle)
    {
        final HashMap< String, ArrayList<String> > map = _mapList.get(handle);
        if(map == null) return null;

        return map.keySet();
    }

    public synchronized static int mapNumKeys(final String handle)
    {
        final HashMap< String, ArrayList<String> > map = _mapList.get(handle);
        if(map == null) return 0;

        return map.keySet().size();
    }

    public synchronized static int mapNumVals(final String handle, final String key)
    {
        final HashMap< String, ArrayList<String> > map = _mapList.get(handle);
        if(map == null) return 0;

        final ArrayList<String> mvalues = map.get(key);
        if(mvalues == null) return 0;

        return mvalues.size();
    }

    private /*synchronized*/ static < M extends Map<String, String> > M _mapGet_impl(final String handle, final Supplier<M> mapSupplier) throws JXMException
    {
        final HashMap< String, ArrayList<String> > map = _mapList.get(handle);
        if(map == null) return null;

        final M res = mapSupplier.get();

        for( final String k : map.keySet() ) {
            res.put( k, XCom.flatten( map.get(k), "" ) );
        }

        return res;
    }

    public synchronized static TreeMap<String, String> mapGetOrdered(final String handle) throws JXMException
    { return _mapGet_impl(handle, TreeMap::new); }

    public synchronized static HashMap<String, String> mapGetUnordered(final String handle) throws JXMException
    { return _mapGet_impl(handle, HashMap::new); }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static String mapNewFrom(final ArrayList<String> handles, final boolean deleteOriginals, final boolean unique)
    {
        final String                               handle = _newMapHandle();
        final HashMap< String, ArrayList<String> > newMap = new HashMap<>();

        _mapList.put(handle, newMap);

        for(final String orgHandle : handles) {

            final HashMap< String, ArrayList<String> > orgMap = _mapList.get(orgHandle);
            if(orgMap == null) continue;

            for( final String key : orgMap.keySet() ) {
                ArrayList<String> mvalues = mvalues = newMap.get(key);
                if(mvalues == null) {
                    mvalues = new ArrayList<>();
                    newMap.put( key, mvalues );
                }
                for( final String value : orgMap.get(key) )  {
                    if( !unique || !mvalues.contains(value) ) mvalues.add(value);
                }
            } // for

        } // for

        if(deleteOriginals) {
            for(final String orgHandle : handles) mapDelete(orgHandle);
        }

        return handle;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static String toEData(final String handle) throws IOException
    {
        final HashMap< String, ArrayList<String> > map = _mapList.get(handle);
        if(map == null) return null;

        return SerializableDeepClone.serializeToBase64String(map);
    }

    public synchronized static String fromEData(final String edata) throws IOException, ClassNotFoundException
    {
        @SuppressWarnings("unchecked")
        final HashMap< String, ArrayList<String> > map = ( HashMap< String, ArrayList<String> > ) SerializableDeepClone.deserializeFromBase64String(edata);

        final String handle = _newMapHandle();

        _mapList.put(handle, map);

        return handle;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized static void dump(final String handle)
    {
        final Set<String> keys = mapKeys(handle);
        if(keys == null) return;

        for(final String key : keys) {
            SysUtil.stdOut().printf("[%s]\n", key);
            for( final String val : mapGet(handle, key) ) SysUtil.stdOut().printf("    %s\n", val);
        }

        SysUtil.stdOut().println();
    }

} // class MapList
