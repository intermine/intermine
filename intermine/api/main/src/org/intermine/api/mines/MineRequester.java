package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;

/**
 * The type of objects that can request data from mines.
 * @author alex
 *
 */
public interface MineRequester
{
    /**
     * Run a query, and return a reader for the result.
     * @param mine The description of the mine.
     * @param xmlQuery The query description.
     * @return A reader over the result.
     */
    BufferedReader runQuery(Mine mine, String xmlQuery);

    /**
     * Request information as encoded in a string.
     * @param urlString The data to request.
     * @return A reader over the data.
     */
    BufferedReader requestURL(String urlString);

}
