package org.intermine.api.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.Map;

import org.intermine.api.util.LimitedMap;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;

/**
 * An in-memory implementation of a query store.
 *
 * This implementation is aware of when it was instantiated, and complains differently
 * to the user about missing keys depending on whether the key is from a time before the
 * lifetime of this object, in which case the server may have been restarted, or after, in
 * which case it is all their fault.
 *
 * @author Alex Kalderimis
 *
 */
public class MemoryQueryStore implements QueryStore
{

    private final long startTime = System.currentTimeMillis();
    private final Map<Long, String> idToXML;
    private final Map<String, Long> xmlToId;
    private final int maxSize;

    /**
     * @param maxSize maximum size
     */
    public MemoryQueryStore(int maxSize) {
        this.maxSize = maxSize;
        idToXML = new LimitedMap<Long, String>(maxSize);
        xmlToId = new LimitedMap<String, Long>(maxSize);
    }


    /**
     * @param xml xml
     * @return id
     * @throws BadQueryException if query is bad
     */
    @Override
    public synchronized String putQuery(String xml) throws BadQueryException {
        Long id = xmlToId.get(xml);
        if (id != null) {
            return id.toString();
        }
        try {
            PathQueryBinding.unmarshalPathQuery(new StringReader(xml),
                    PathQuery.USERPROFILE_VERSION);
        } catch (Exception e) {
            String message = "XML is not well formatted.";
            throw new BadQueryException(message, e);
        }

        id = System.currentTimeMillis();

        idToXML.put(id, xml);
        xmlToId.put(xml, id);

        return id.toString();
    }

    /**
     * @param key key
     * @return query
     * @throws KeyFormatException if something goes wrong
     * @throws NotPresentException if key is not in query store
     */
    @Override
    public String getQuery(String key)
        throws KeyFormatException, NotPresentException {
        Long id;
        try {
            id = Long.valueOf(key, 10);
        } catch (NumberFormatException e) {
            throw new KeyFormatException("The key for this query store must be a 64bit number", e);
        }
        if (id < startTime) {
            String message = "Key not in query store. "
                    + "This key may have come from an expired session";
            throw new NotPresentException(message);
        }
        if (!idToXML.containsKey(id)) {
            String message = "Key not in query store.";
            if (idToXML.size() == maxSize && id < System.currentTimeMillis()) {
                // Operating at capacity, probably dropped it.
                message += " The query store only has capacity for " + maxSize + " queries. Yours"
                        + " may have been dropped.";
            }
            throw new NotPresentException(message);
        }
        return idToXML.get(id);
    }

}
