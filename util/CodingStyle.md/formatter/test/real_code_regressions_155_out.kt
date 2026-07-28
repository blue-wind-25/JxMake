/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */

public actual interface MutableMap<K, V> : Map<K, V> {

    public actual fun clear(): Unit
    actual override val keys: MutableSet<K>
    public actual interface MutableEntry<K, V> : Map.Entry<K, V> {

        public actual fun setValue(newValue: V): V

    } // interface MutableEntry

} // interface MutableMap
