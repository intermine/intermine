package org.intermine.api.mines;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.util.Properties;

/**
 * The type of objects that can request data from mines.
 *
 * @author Alex Kalderimis
 *
 */
public interface MineRequester
{

    /**
     * Request information as encoded in a string.
     * @param urlString The data to request.
     * @param contentType The content type.
     * @return A reader over the data.
     */
    BufferedReader requestURL(String urlString, ContentType contentType);

    /**
     * Accept configuration provided in the form of properties.
     * @param requesterConfig The configuration.
     */
    void configure(Properties requesterConfig);

}
