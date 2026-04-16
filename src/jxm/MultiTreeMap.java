/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import jxm.xb.*;


public class MultiTreeMap<K, V> {

    private final TreeMap< K, List<V> > _treeMap = new TreeMap<>();
            int                         _size    = 0;

    public int size()
    { return _size; }

    public boolean containsKey(final K key)
    { return _treeMap.containsKey(key); }

    public void put(final K key, final V value)
    {
        _treeMap.computeIfAbsent( key, k -> new ArrayList<>() ).add(value);
        ++_size;
    }

    public List<V> get(final K key)
    { return containsKey(key) ? _treeMap.get(key) : null; }

    public boolean remove(final K key, final V value)
    {
        if( !containsKey(key) ) return false;

        if( !_treeMap.get(key).contains(value) ) return false;

        _treeMap.get(key).remove(value);
        --_size;

        return true;
    }

    public void removeAll(final K key)
    {
        if( containsKey(key) ) {
            _size -= _treeMap.get(key).size();
            _treeMap.remove(key);
        }
    }

    public void clear()
    {
        _treeMap.clear();
        _size = 0;
    }

} // class MultiTreeMap
