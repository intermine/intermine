package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Base class for link generators.
 * @author Jakub Kulaviak
 *
 */
public abstract class LinkGeneratorBase
{
    /**
     * Empty constructor.
     */
    protected LinkGeneratorBase() {
    }

    /**
     *  Encodes object string value to be able to be part of url.
     * @param o encoded object
     * @return encoded string
     */
    protected static String encode(Object o) {
        if (o == null) {
            return "";
        } else {
            try {
                return URLEncoder.encode(o.toString(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding string failed", e);
            }
        }
    }

}
