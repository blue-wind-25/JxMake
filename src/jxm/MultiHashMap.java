/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details.
 */


package jxm;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import jxm.xb.*;


public class MultiHashMap<K, V> {

    private final HashMap< K, List<V> > _hashMap = new HashMap<>();
            int                         _size    = 0;

    public int size()
    { return _size; }

    public boolean containsKey(final K key)
    { return _hashMap.containsKey(key); }

    public void put(final K key, final V value)
    {
        _hashMap.computeIfAbsent( key, k -> new ArrayList<>() ).add(value);
        ++_size;
    }

    public List<V> get(final K key)
    { return containsKey(key) ? _hashMap.get(key) : null; }

    public boolean remove(final K key, final V value)
    {
        if( !containsKey(key) ) return false;

        if( !_hashMap.get(key).contains(value) ) return false;

        _hashMap.get(key).remove(value);
        --_size;

        return true;
    }

    public void removeAll(final K key)
    {
        if( containsKey(key) ) {
            _size -= _hashMap.get(key).size();
            _hashMap.remove(key);
        }
    }

    public void clear()
    {
        _hashMap.clear();
        _size = 0;
    }

} // class MultiHashMap
