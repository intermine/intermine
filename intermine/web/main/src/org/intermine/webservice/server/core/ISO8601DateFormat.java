package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * A date formatter for the ISO8601 date format.
 * @author Alex Kalderimis
 */
public class ISO8601DateFormat extends SimpleDateFormat
{

    private static final long serialVersionUID = -5686664384857682278L;
    private static final DateFormat INSTANCE = new ISO8601DateFormat();

    /**
     * Constructor.
     */
    public ISO8601DateFormat() {
        super("yyyy-MM-dd'T'HH:mm:ssZ");
    }

    /**
     * @return an instance of a DateFormat.
     */
    public static DateFormat getFormatter() {
        return INSTANCE;
    }
}
