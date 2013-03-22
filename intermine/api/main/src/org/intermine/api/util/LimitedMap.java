package org.intermine.api.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A caching map that drops its eldest entries if the map grows beyond a certain size.
 * @author Alex Kalderimis
 *
 * @param <K> The type of the keys.
 * @param <V> The type of the values.
 */
public class LimitedMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 4102004209918022983L;
    private final int maxEntries;

    public LimitedMap(int maxNoOfEntries) {
        this.maxEntries = maxNoOfEntries;
    }

    @Override
    protected boolean removeEldestEntry(@SuppressWarnings("rawtypes") Map.Entry eldest) {
        return size() > maxEntries;
    }
}
