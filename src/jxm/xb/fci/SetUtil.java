/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm.xb.fci;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import java.util.regex.Pattern;

import jxm.*;
import jxm.tool.*;
import jxm.xb.*;


public class SetUtil {

    public static void _execute_explode(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        final XCom.VariableValue parts = evalVals.get(0);
        final String             sep   = FuncCall._readFlattenOptParam(evalVals, 1, " ");

        XCom.explode(retVal, parts, sep);
    }

    public static void _execute_sfconstants(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final String sep) throws JXMException
    {
        final XCom.VariableValue parts = evalVals.get(0);

        XCom.explode(retVal, parts, sep);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_implode(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        final XCom.VariableValue parts = evalVals.get(0);
        final String             sep   = FuncCall._readFlattenOptParam(evalVals, 1, " ");

        retVal.add( new XCom.VariableStore( true, XCom.flatten(parts, sep) ) );
    }

    public static void _execute_ftconstants(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final String sep) throws JXMException
    {
        final XCom.VariableValue parts = evalVals.get(0);

        retVal.add( new XCom.VariableStore( true, XCom.flatten(parts, sep) ) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_part_count(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    { retVal.add( new XCom.VariableStore( true, String.valueOf( evalVals.get(0).size() ) ) ); }

    public static void _execute_partn(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        final XCom.VariableValue parts =                                                     evalVals.get(0);
              int                index = (   XCom.toLong( execBlock, execData, XCom.flatten( evalVals.get(1), "" )             ).intValue()
                                           + XCom.toLong( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 2, "0") ).intValue()
                                         );

        if(index < 0) index = parts.size() + index + 1;

        if( index < 1 || index > parts.size() )
            retVal.add(XCom.VarStr_EmptyString);
        else
            retVal.add( new XCom.VariableStore( true, parts.get(index - 1).value ) );
    }

    public static void _execute_partnm(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        final XCom.VariableValue parts =                                                 evalVals.get(0);
        final int                beg_  = XCom.toLong( execBlock, execData, XCom.flatten( evalVals.get(1), "" ) ).intValue();
        final int                end_  = XCom.toLong( execBlock, execData, XCom.flatten( evalVals.get(2), "" ) ).intValue();
        final int                beg   = Math.max( 0           ,                                 beg_ - 1 );
        final int                end   = Math.min( parts.size(), end_ <= 0 ? Integer.MAX_VALUE : end_     );

        for(int i = beg; i < end; ++i) retVal.add( new XCom.VariableStore( true, parts.get(i).value ) );
    }

    private static int _execute_part_impl_fidx(final ArrayList<String> set, final String chk, final int beg)
    {
        for( int i = beg; i < set.size(); ++i ) {
            if( set.get(i).equals(chk) ) return i;
        }

        return -1;
    }

    private static int _execute_part_impl_lidx(final ArrayList<String> set, final String chk, final int beg)
    {
        for( int i = beg; i >= 0; --i ) {
            if( set.get(i).equals(chk) ) return i;
        }

        return -1;
    }

    public static void _execute_part_xidx(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean lastOccurrence) throws JXMException
    {
        // Get the subject, reference, and index values
        final ArrayList<String> set = new ArrayList<>();
        final ArrayList<String> chk = new ArrayList<>();
        final ArrayList<Long  > frm = new ArrayList<>();

        for( final XCom.VariableStore item : evalVals.get(0) ) set.add(item.value);
        for( final XCom.VariableStore item : evalVals.get(1) ) chk.add(item.value);

        if( evalVals.size() == 3 ) {
            for( final XCom.VariableStore item : evalVals.get(2) ) frm.add( XCom.toLong(execBlock, execData, item.value) );
        }
        else {
            frm.add( Long.valueOf( lastOccurrence ? set.size() : 1 ) );
        }

        final int cnt = Math.max( chk.size(), frm.size() );

        // Get the index(es)
        for(int i = 0; i < cnt; ++i) {
            final String r = ( i < chk.size() ) ? chk.get(i) : chk.get( chk.size() - 1 );
            final Long   f = ( i < frm.size() ) ? frm.get(i) : frm.get( frm.size() - 1 );
            retVal.add( new XCom.VariableStore(
                true,
                String.valueOf(
                    1 + ( lastOccurrence ? _execute_part_impl_lidx​( set, r, Math.min( set.size(), f.intValue() <= 0 ? Integer.MAX_VALUE : f.intValue()     ) - 1 )
                                         : _execute_part_impl_fidx( set, r, Math.max( 0         ,                                         f.intValue() - 1 )     )
                        )
                )
            ) );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_part_remove(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the index values
        final ArrayList<Integer> idx = new ArrayList<>();

        for( final XCom.VariableStore item : evalVals.get(1) ) idx.add( XCom.toLong(execBlock, execData, item.value).intValue() );

        // Store the updated values
        for(int i = 0; i < evalVals.get(0).size(); ++i) {
            final int chkIdx = idx.indexOf(i + 1);
            if(chkIdx < 0) retVal.add( new XCom.VariableStore( true, evalVals.get(0).get(i).value ) );
        }
    }

    public static void _execute_part_insert(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get index value
        final int     idx_ = XCom.toLong( execBlock, execData, XCom.flatten( evalVals.get(1), "" ) ).intValue();
        final int     idx  = Math.abs(idx_);
        final boolean rem  = (idx_ < 0);

        // Store the updated values
        for(int i = 0; i < evalVals.get(0).size(); ++i) {
            if(i == idx - 1) {
                for( final XCom.VariableStore item : evalVals.get(2) ) {
                    retVal.add( new XCom.VariableStore(true, item.value) );
                }
                if(!rem) retVal.add( new XCom.VariableStore( true, evalVals.get(0).get(i).value ) );
            }
            else {
                         retVal.add( new XCom.VariableStore( true, evalVals.get(0).get(i).value ) );
            }
        }
    }

    public static void _execute_part_replace(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the index values
        final ArrayList<Integer> idx = new ArrayList<>();

        for( final XCom.VariableStore item : evalVals.get(1) ) idx.add( XCom.toLong(execBlock, execData, item.value).intValue() );

        if( idx.size() != evalVals.get(2).size() ) throw XCom.newJXMRuntimeError(Texts.EMsg_prep_NumIdxRepNotSame, idx.size(), evalVals.get(2).size() );

        // Replace and store the values
        for(int i = 0; i < evalVals.get(0).size(); ++i) {
            final int repIdx = idx.indexOf(i + 1);
            if(repIdx < 0) retVal.add( new XCom.VariableStore( true, evalVals.get(0).get(i     ).value ) );
            else           retVal.add( new XCom.VariableStore( true, evalVals.get(2).get(repIdx).value ) );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_contains(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the subject values
        final HashSet<String> set = new HashSet<>();

        for( final XCom.VariableStore item : evalVals.get(0) ) set.add(item.value);

        // Perform search
        for( final XCom.VariableStore chk : evalVals.get(1) ) {
            retVal.add( new XCom.VariableStore( true, set.contains(chk.value) ? XCom.Str_T : XCom.Str_F ) );
        }
    }

    public static void _execute_lookup(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Check the number of keys and values
        if( evalVals.get(1).size() != evalVals.get(2).size() ) {
            throw XCom.newJXMRuntimeError( Texts.EMsg_lookup_KeyValueSizeDiff, evalVals.get(1).size(), evalVals.get(2).size() );
        }

        // Get the search key
        final String sk = XCom.flatten(evalVals.get(0), "");

        // Find the index to the search key
        int idx = -1;
        for(int i = 0; i < evalVals.get(1).size(); ++i) {
            if( evalVals.get(1).get(i).value.equals(sk) ) {
                idx = i;
                break;
            }
        }

        if(idx < 0) return;

        // Return the search value
        retVal.add( new XCom.VariableStore( true, evalVals.get(2).get(idx).value ) );
    }

    public static void _execute_unique(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Use a tree set to determine if the values are unique
        final HashSet<String> set = new HashSet<>();

        // Store only the unique values
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            if( set.contains(item.value) ) continue;
                set.add     (item.value);
            retVal.add( new XCom.VariableStore(true, item.value) );
        }
    }

    public static void _execute_erase_ifxxx(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean ifNot) throws JXMException
    {
        // Get the reference values
        final HashSet<String> chk = new HashSet<>();

        for( final XCom.VariableStore item : evalVals.get(1) ) chk.add(item.value);

        // Get the flag
        final boolean isRegex = XCom.toBoolean( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 2, "false") );

        // Prepare the pattern as needed
        Pattern pattern = null;

        if(isRegex) {
            final StringBuilder sb = new StringBuilder();
            for(final String p : chk) {
                if( sb.length() != 0 ) sb.append('|');
                                       sb.append('(');
                                       sb.append(p  );
                                       sb.append(')');
            }
            pattern = ReCache._reGetPattern( sb.toString() );
        }

        // Perform erase if or if not
        for( final XCom.VariableStore set : evalVals.get(0) ) {
            final String  value = set.value;
            final boolean match = isRegex ? pattern.matcher(value).matches() : chk.contains(value);
            if(ifNot) {
                if( match) retVal.add( new XCom.VariableStore(true, value) );
            }
            else {
                if(!match) retVal.add( new XCom.VariableStore(true, value) );
            }
        }
    }

    public static void _execute_erase_empty(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for( final XCom.VariableStore set : evalVals.get(0) ) {
            final String value = set.value.trim();
            if( !value.isEmpty() ) retVal.add( new XCom.VariableStore(true, value) );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_repeat(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the arguments
        final ArrayList<String> rterm = new ArrayList<>();
        final ArrayList<Long  > count = new ArrayList<>();

        for( final XCom.VariableStore item : evalVals.get(0) ) rterm.add(                                  item.value  );
        for( final XCom.VariableStore item : evalVals.get(1) ) count.add( XCom.toLong(execBlock, execData, item.value) );

        // If the number of counters is less than the number terms, duplicate the last counter
        final int rtermSize = rterm.size();
        final int countSize = count.size();

        if(countSize < rtermSize) {
            final Long lcount = count.get(countSize - 1);
            for(int i = 0; i < rtermSize - countSize; ++i) count.add(lcount);
        }

        // Repeat and concatenate the items
        final StringBuilder sb  = new StringBuilder();
              int           idx = 0;

        for(final String item : rterm) {
            // Repeat and concatenate
            sb.setLength(0);
            for(int r = 0; r < count.get(idx); ++r) sb.append(item);
            ++idx;
            // Store the result
            retVal.add( new XCom.VariableStore( true, sb.toString() ) );
        }
    }

    public static void _execute_stack(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        for(final XCom.VariableValue varVal : evalVals) {
            for(final XCom.VariableStore item : varVal) {
                final String value = item.value;//.trim();
                if( !value.isEmpty() ) retVal.add( new XCom.VariableStore(true, value) );
            }
        }
    }

    public static void _execute_series(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get largest number of set members
        int maxCnt = 0;

        for(final XCom.VariableValue varVal : evalVals) {
            if( varVal.size() > maxCnt ) maxCnt = varVal.size();
        }

        // Series the parts
        final StringBuilder sb = new StringBuilder();

        for(int i = 0; i < maxCnt; ++i) {
            // Series and concatenate
            sb.setLength(0);
            for(int j = 0; j < evalVals.size(); ++j) {
                final int psize = evalVals.get(j).size();
                if(psize > 0) {
                    String part = (i < psize) ? evalVals.get(j).get(i        ).value
                                              : evalVals.get(j).get(psize - 1).value;
                    if(part == null) part = "";
                    else             part = part;//.trim();
                    if( !part.isEmpty() ) sb.append(part);
                }
            }
            // Store the result
            retVal.add( new XCom.VariableStore( true, sb.toString() ) );
        }
    }

    public static void _execute_interleave(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get largest number of set members
        int maxCnt = 0;

        for(final XCom.VariableValue varVal : evalVals) {
            if( varVal.size() > maxCnt ) maxCnt = varVal.size();
        }

        // Interleave and store the parts
        for(int i = 0; i < maxCnt; ++i) {
            for(int j = 0; j < evalVals.size(); ++j) {
                final int psize = evalVals.get(j).size();
                if(psize > 0) {
                    String part = (i < psize) ? evalVals.get(j).get(i        ).value
                                              : evalVals.get(j).get(psize - 1).value;
                    if(part == null) part = "";
                    else             part = part;//.trim();
                    if( !part.isEmpty() ) retVal.add( new XCom.VariableStore(true, part) );
                }
            }
        }
    }

    public static void _execute_ftt_stack(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        _execute_stack(retVal, evalVals);

        final XCom.VariableStore res = new XCom.VariableStore( true, XCom.flatten(retVal, "") );

        retVal.clear();
        retVal.add(res);
    }

    public static void _execute_sort(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final boolean descending) throws JXMException
    {
        // Get the optional pre-sorting regular expression filter
        final String  psfStr    = FuncCall._readFlattenOptParam(evalVals, 1, "").trim();
        final String  psfRep    = FuncCall._readFlattenOptParam(evalVals, 2, "").trim();
        final Pattern psfRegExp = psfStr.isEmpty() ? null : ReCache._reGetPattern(psfStr);

        // Get the values
        final ArrayList<String> values = new ArrayList<>();

        for( final XCom.VariableStore item : evalVals.get(0) ) values.add(item.value);

        // Sort the values without pre-sorting filter
        if(psfRegExp == null) {
            if(descending) Collections.sort( values, Collections.reverseOrder() );
            else           Collections.sort( values                             );
        }

        // Sort the values using pre-sorting filter
        else {
            Collections.sort(
                values,
                new Comparator<String>() {
                    @Override
                    public int compare(final String o1, final String o2)
                    {
                        String f1 = psfRegExp.matcher(o1).replaceAll(psfRep);
                        String f2 = psfRegExp.matcher(o2).replaceAll(psfRep);
                        if( f1.isEmpty() ) f1 = o1;
                        if( f2.isEmpty() ) f2 = o1;
                        if(descending) return f2.compareTo(f1);
                        else           return f1.compareTo(f2);
                    }
                }
            );
        }

        // Store the sorted values
        for(final String value : values) {
            retVal.add( new XCom.VariableStore(true, value) );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_map_new(final XCom.VariableValue retVal) throws JXMException
    {
        // Create a new map and store its handle
        retVal.add( new XCom.VariableStore( true, MapList.mapNew() ) );
    }

    public static void _execute_map_new_from(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get the map handles
        final ArrayList<String> handles = new ArrayList<>();

        for( final XCom.VariableStore item : evalVals.get(0) ) handles.add(item.value);

        // Get the flags
        final boolean deleteOriginals = XCom.toBoolean( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 1, "false") );
        final boolean unique          = XCom.toBoolean( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 2, "true" ) );

        // Create a new map and store its handle
        retVal.add( new XCom.VariableStore( true, MapList.mapNewFrom(handles, deleteOriginals, unique) ) );
    }

    public static void _execute_map_delete(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the map handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Delete the map
        MapList.mapDelete(handle);
    }

    public static void _execute_map_clear(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the map handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Clear the map
        MapList.mapClear(handle);
    }

    public static void _execute_map_putadd(final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean add) throws JXMException
    {
        // Get the map handle and key
        final String handle = XCom.flatten( evalVals.get(0), "" );
        final String key    = XCom.flatten( evalVals.get(1), "" );

        // Get the flag
        final boolean unique = XCom.toBoolean( execBlock, execData, FuncCall._readFlattenOptParam(evalVals, 3, "true") );

        // Get the values
        final ArrayList<String> values = new ArrayList<>();

        for( final XCom.VariableStore item : evalVals.get(2) ) values.add(item.value);

        // Put or add the values
        MapList.mapPut(handle, key, values, add, unique);
    }

    public static void _execute_map_remove(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the map handle and key
        final String handle = XCom.flatten( evalVals.get(0), "" );
        final String key    = XCom.flatten( evalVals.get(1), "" );

        // Remove the key
        MapList.mapRemove(handle, key);
    }

    public static void _execute_map_get(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the map handle and key
        final String handle = XCom.flatten( evalVals.get(0), "" );
        final String key    = XCom.flatten( evalVals.get(1), "" );

        // Get the value(s)
        final ArrayList<String> values = MapList.mapGet(handle, key);

        // Store the value(s)
        if(values != null) {
            for(final String value : values) {
                retVal.add( new XCom.VariableStore(true, value) );
            }
        }
    }

    public static void _execute_map_keys(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the map handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Get the key(s)
        final Set<String> keys = MapList.mapKeys(handle);

        // Store the key(s)
        if(keys != null) {
            for(final String key : keys) {
                retVal.add( new XCom.VariableStore(true, key) );
            }
        }
    }

    public static void _execute_map_num_keys(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the map handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Get the number of key(s)
        final String numKeys = String.valueOf( MapList.mapNumKeys(handle) );

        // Store the number of key(s)
        retVal.add( new XCom.VariableStore(true, numKeys) );
    }

    public static void _execute_map_num_vals(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the map handle and key
        final String handle = XCom.flatten( evalVals.get(0), "" );
        final String key    = XCom.flatten( evalVals.get(1), "" );

        // Get the number of values(s)
        final String numVals = String.valueOf( MapList.mapNumVals(handle, key) );

        // Store the number of values(s)
        retVal.add( new XCom.VariableStore(true, numVals) );
    }

    public static void _execute_map_to_edata(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the map handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Store the encoded representation data
        try {
            retVal.add( new XCom.VariableStore( true, MapList.toEData(handle) ) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_map_fr_edata(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the encoded representation data
        final String edata = XCom.flatten( evalVals.get(0), "" );

        // Store the map handle
        try {
            retVal.add( new XCom.VariableStore( true, MapList.fromEData(edata) ) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_map_vl_handle(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the map handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Check if the given handle is valid
        retVal.add( new XCom.VariableStore( true, MapList.mapIsValidHandle(handle) ? XCom.Str_T : XCom.Str_F ) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_nmap_frm_json(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the JSON string
        final String jsonString = XCom.flatten( evalVals.get(0), "" );

        // Store the map handle
        try {
            retVal.add( new XCom.VariableStore( true, JSONUtil.jsonStringToNestedMap(jsonString) ) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_nmap_delete(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the map handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Delete the map
        JSONUtil.nmapDelete(handle);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_stk_new(final XCom.VariableValue retVal) throws JXMException
    {
        // Create a new stack and store its handle
        retVal.add( new XCom.VariableStore( true, StackList.stackNew() ) );
    }

    public static void _execute_stk_delete(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the stack handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Delete the stack
        StackList.stackDelete(handle);
    }

    public static void _execute_stk_clear(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the stack handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Clear the stack
        StackList.stackClear(handle);
    }

    public static void _execute_stk_push(final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the stack handle and key
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Get the values
        final ArrayList<String> values = new ArrayList<>();

        for( final XCom.VariableStore item : evalVals.get(1) ) values.add(item.value);

        // Push the values
        StackList.stackPush(handle, values);
    }

    public static void _execute_stk_peek_pop(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final boolean pop) throws JXMException
    {
        // Get the stack handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Peek the value(s)
        final ArrayList<String> values = pop ? StackList.stackPop(handle) : StackList.stackPeek(handle);

        // Store the value(s)
        if(values != null) {
            for(final String value : values) {
                retVal.add( new XCom.VariableStore(true, value) );
            }
        }
    }
    public static void _execute_stk_num_elems(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the stack handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Get the number of element(s)
        final String numElems = String.valueOf( StackList.stackNumElems(handle) );

        // Store the number of element(s)
        retVal.add( new XCom.VariableStore(true, numElems) );
    }

    public static void _execute_stk_to_edata(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the stack handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Store the encoded representation data
        try {
            retVal.add( new XCom.VariableStore( true, StackList.toEData(handle) ) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_stk_fr_edata(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the encoded representation data
        final String edata = XCom.flatten( evalVals.get(0), "" );

        // Store the stack handle
        try {
            retVal.add( new XCom.VariableStore( true, StackList.fromEData(edata) ) );
        }
        catch(final Exception e) {
            // Print the stack trace if requested
            if( XCom.enableAllExceptionStackTrace() ) e.printStackTrace();
            // Throw as a different exception
            throw XCom.newJXMRuntimeError( e.toString() );
        }
    }

    public static void _execute_stk_vl_handle(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals) throws JXMException
    {
        // Get the stack handle
        final String handle = XCom.flatten( evalVals.get(0), "" );

        // Check if the given handle is valid
        retVal.add( new XCom.VariableStore( true, StackList.stackIsValidHandle(handle) ? XCom.Str_T : XCom.Str_F ) );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void _execute_cmp(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get largest number of set members
        final int lhsCnt = evalVals.get(0).size();
        final int cmpCnt = evalVals.get(1).size();
        final int rhsCnt = evalVals.get(2).size();
        final int maxCnt = Math.max( Math.max(lhsCnt, rhsCnt), cmpCnt );

        // Get the last values
        final String lhsLast = (lhsCnt != 0) ? evalVals.get(0).get(lhsCnt - 1).value : "";
        final String cmpLast = (cmpCnt != 0) ? evalVals.get(1).get(cmpCnt - 1).value : "";
        final String rhsLast = (rhsCnt != 0) ? evalVals.get(2).get(rhsCnt - 1).value : "";

        // Perform comparison
        for(int i = 0; i < maxCnt; ++i) {

                  boolean res  = false;

            final String  lhs  = (i < lhsCnt) ? evalVals.get(0).get(i).value : lhsLast;
            final String  cmp  = (i < cmpCnt) ? evalVals.get(1).get(i).value : cmpLast;
            final String  rhs  = (i < rhsCnt) ? evalVals.get(2).get(i).value : rhsLast;

            if( cmp.isEmpty() ) throw XCom.newJXMRuntimeError(Texts.EMsg_xxx_EmptyValueStr, "cmp", "<cmp>");

            final boolean scmp = cmp.equals("&==") || cmp.equals("&!=");
            final long    nlhs = scmp ? 0 : XCom.toLong(execBlock, execData, lhs);
            final long    nrhs = scmp ? 0 : XCom.toLong(execBlock, execData, rhs);

            switch(cmp) {

                case "&==" : res =  lhs.equals(rhs); break;
                case "&!=" : res = !lhs.equals(rhs); break;

                case "=="  : res = (nlhs == nrhs)  ; break;
                case "!="  : res = (nlhs != nrhs)  ; break;
                case "<"   : res = (nlhs <  nrhs)  ; break;
                case "<="  : res = (nlhs <= nrhs)  ; break;
                case ">"   : res = (nlhs >  nrhs)  ; break;
                case ">="  : res = (nlhs >= nrhs)  ; break;

                default    : throw XCom.newJXMRuntimeError(Texts.EMsg_cmp_InvalidCmpOper, cmp);

            } // switch

            retVal.add( new XCom.VariableStore(true, res ? XCom.Str_T : XCom.Str_F) );

        } // for
    }

    public static void _execute_not(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        for( final XCom.VariableStore item : evalVals.get(0) ) {
            final String value = item.value;//.trim();
            if( !value.isEmpty() ) retVal.add( new XCom.VariableStore( true, XCom.toBoolean(execBlock, execData, value) ? XCom.Str_F : XCom.Str_T ) );
        }
    }

    public static void _execute_and_or(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData, final boolean or) throws JXMException
    {
        boolean any = false;
        boolean res = !or;

        for(final XCom.VariableValue varVal : evalVals) {
            for(final XCom.VariableStore item : varVal) {
                final String value = item.value;//.trim();
                if( !value.isEmpty() ) {
                    if(or) {
                        any  = true;
                        res |= XCom.toBoolean(execBlock, execData, value);
                        if(res) break;
                    }
                    else {
                        any  = true;
                        res &= XCom.toBoolean(execBlock, execData, value);
                        if(!res) break;
                    }
                }
            }
        }

        if(!any) res = false;

        retVal.add( new XCom.VariableStore(true, res ? XCom.Str_T : XCom.Str_F) );
    }

    public static void _execute_csel(final XCom.VariableValue retVal, final ArrayList<XCom.VariableValue> evalVals, final ExecBlock execBlock, final XCom.ExecData execData) throws JXMException
    {
        // Get largest number of set members
        final int cselCnt = evalVals.get(0).size();
        final int valTCnt = evalVals.get(1).size();
        final int valFCnt = evalVals.get(2).size();
        final int maxCnt  = Math.max( cselCnt, Math.max(valTCnt, valFCnt) );

        // Get the last values
        final String cselLast = (cselCnt != 0) ? evalVals.get(0).get(cselCnt - 1).value : "";
        final String valTLast = (valTCnt != 0) ? evalVals.get(1).get(valTCnt - 1).value : "";
        final String valFLast = (valFCnt != 0) ? evalVals.get(2).get(valFCnt - 1).value : "";

        // Perform comparison
        for(int i = 0; i < maxCnt; ++i) {

            final String csel = (i < cselCnt) ? evalVals.get(0).get(i).value : cselLast;
            final String valT = (i < valTCnt) ? evalVals.get(1).get(i).value : valTLast;
            final String valF = (i < valFCnt) ? evalVals.get(2).get(i).value : valFLast;

            retVal.add( new XCom.VariableStore( true, XCom.toBoolean(execBlock, execData, csel) ? valT : valF ) );

        } // for
    }

} // class SetUtil
