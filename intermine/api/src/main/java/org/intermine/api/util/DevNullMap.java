package org.intermine.api.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Set;

/**
 * An implementation of Map that is totally forgetful. Asking this map to remember anything is like
 * just piping it to <code>dev/null</code>.
 * @author Alex Kalderimis
 *
 * @param <K> The key type
 * @param <V> The value type
 */
public class DevNullMap<K, V> extends AbstractMap<K, V>
{

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new HashSet<Entry<K, V>>();
    }

    @Override
    public V put(K key, V value) {
        return null;
    }
}
