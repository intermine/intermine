package org.intermine.webservice.server.jbrowse;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Alex
 *
 * @param <K>
 * @param <V>
 */
public interface MapListener<K, V>
{

    /**
     *
     * @param entry entry
     * @param hasMore has more
     */
    void add(Entry<K, V> entry, boolean hasMore);

    /**
     *
     * @param map map
     * @param hasMore has more
     */
    void add(Map<K, V> map, boolean hasMore);
}
