package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.Results;

import org.intermine.web.Constants;

import org.apache.log4j.Logger;

/**
 * An inline table created by running a template.
 * @author Kim Rutherford
 */
public class InlineTemplateTable
{
    private Results results;
    private Map webProperties;
    private int size = -1;
    private ArrayList inlineResults;
    private static final Logger LOG = Logger.getLogger(InlineTemplateTable.class);
    private final List columnNames;

    /**
     * Construct a new InlineTemplateTable
     * @param results the Results of running the template query
     * @param columnNames the names of each column in the Results object
     * @param webProperties the web properties from the session
     */
    public InlineTemplateTable(Results results, List columnNames, Map webProperties) {
        this.results = results;
        this.columnNames = columnNames;
        this.webProperties = webProperties;
    }

    /**
     * Return headings for the columns
     * @return the column names
     */
    public List getColumnNames() {
        return columnNames;
    }

    /**
     * Return the part of the Results that should be display inline in the object details pages.
     * @return the first getSize() rows from the Results object that was passed to the constructor
     */
    public List getInlineResults() {
        if (inlineResults == null) {
            inlineResults = new ArrayList(getSize());

            for (int i = 0; i < getSize(); i++) {
                inlineResults.add(results.get(i));
            }
        }
        
        return inlineResults;
    }
    
    /**
     * Return the number of table rows or the INLINE_TABLE_SIZE whichever is smaller.
     */
    private int getSize() {
        if (size == -1) {
            // default
            int maxInlineTableSize = 30;

            String maxInlineTableSizeString =
                (String) webProperties.get(Constants.INLINE_TABLE_SIZE);

            try {
                maxInlineTableSize = Integer.parseInt(maxInlineTableSizeString);
            } catch (NumberFormatException e) {
                LOG.warn("Failed to parse " + Constants.INLINE_TABLE_SIZE + " property: "
                         + maxInlineTableSizeString);
            }

            size = maxInlineTableSize;

            try {
                // do this rather than calling results.size() because Results.size() is slow
                results.get(size);
            } catch (IndexOutOfBoundsException e) {
                size = results.size();
            }
        }
         
        return size;
    }
}
