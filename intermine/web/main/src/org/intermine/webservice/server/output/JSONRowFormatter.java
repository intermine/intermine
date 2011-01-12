package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * @author Alexis Kalderimis
 * 
 */
public class JSONRowFormatter extends JSONFormatter {
    
    /**
     * The key for setting the header attributes for the title.
     */
    public static final String KEY_TITLE = "title";
    
    /**
     * The key for setting the header attributes for the count.
     */
    public static final String KEY_COUNT = "count";
    
    /**
     * The key for setting the header attributes for the TSV export URL.
     */
    public static final String KEY_EXPORT_TSV_URL = "tsv_url";
    
    /**
     * The key for setting the header attributes for the CSV export URL.
     */
    public static final String KEY_EXPORT_CSV_URL = "csv_url";
    
    /**
     * The key for setting the header attributes for the previous page
     */
    public static final String KEY_PREVIOUS_PAGE = "previous";
    
    /**
     * The key for setting the header attributes for the next page
     */
    public static final String KEY_NEXT_PAGE = "next";
    
    /**
     * Constructor.
     */
    public JSONRowFormatter() {
        // Empty constructor
    }
}
