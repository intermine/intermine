package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2007 FlyMine
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

import org.intermine.web.logic.Constants;

import java.io.Serializable;

import org.apache.log4j.Logger;

/**
 * An inline table created by running a template.
 * @author Kim Rutherford
 */
public class InlineTemplateTable implements Serializable
{
    private int inlineSize = -1;
    private ArrayList inlineResults;
    private static final Logger LOG = Logger.getLogger(InlineTemplateTable.class);
    private final List columnNames;
    private int resultsSize = -1;
    private int maxInlineTableSize = 10;
    
    /**
     * Construct a new InlineTemplateTable
     * @param pagedResults the Results of running the template query
     * @param webProperties the web properties from the session
     */
    public InlineTemplateTable(PagedResults pagedResults, Map webProperties) {
        this.columnNames = pagedResults.getColumnNames();
        resultsSize = pagedResults.getSize();
        
        String maxInlineTableSizeString =
            (String) webProperties.get(Constants.INLINE_TABLE_SIZE);

        try {
            maxInlineTableSize = Integer.parseInt(maxInlineTableSizeString);
        } catch (NumberFormatException e) {
            LOG.warn("Failed to parse " + Constants.INLINE_TABLE_SIZE + " property: "
                     + maxInlineTableSizeString);
        }
        
        inlineSize = maxInlineTableSize;

        if (resultsSize < inlineSize) {
            inlineSize = resultsSize;
        }   

        inlineResults = new ArrayList(inlineSize);

        for (int i = 0; i < inlineSize; i++) {
            inlineResults.add(pagedResults.getRows().get(i));
        }
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
     * This is used only in JSP files currently.
     * @return the first getSize() rows from the Results object that was passed to the constructor
     */
    public List getInlineResults() {
        return inlineResults;
    }
    
    /**
     * Return the number of rows in the Results object that was passed to the constructor.
     * @return the row count
     */
    public int getResultsSize() {
        return resultsSize;
    }
}
