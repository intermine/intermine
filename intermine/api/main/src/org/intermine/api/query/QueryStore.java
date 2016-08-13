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


/**
 *
 * @author Alex
 *
 */
public interface QueryStore
{

    /**
     * @param xml query xml
     * @return query ID
     * @throws BadQueryException if query is bad
     */
    String putQuery(String xml) throws BadQueryException;

    /**
     *
     * @param key key
     * @return query
     * @throws KeyFormatException key is wrong format
     * @throws NotPresentException key doesn't exist
     */
    String getQuery(String key) throws KeyFormatException, NotPresentException;
}
