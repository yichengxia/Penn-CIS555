package edu.upenn.cis.cis555.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is to implement LRU cache with LinkedHashMap.
 */
public class LRUHashMap<K, V> extends LinkedHashMap<K, V> {
    
    private final int capacity;

    public LRUHashMap(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() >= capacity;
    }
}
