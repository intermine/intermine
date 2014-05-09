package org.intermine.webservice.server.jbrowse;

import java.util.Map;
import java.util.Map.Entry;

public interface MapListener<K, V> {

    public void add(Entry<K, V> entry, boolean hasMore);

    public void add(Map<K, V> map, boolean hasMore);
}
