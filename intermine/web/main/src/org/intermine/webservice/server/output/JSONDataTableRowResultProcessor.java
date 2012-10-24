package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Iterator;
import java.util.List;

import org.intermine.api.results.ResultElement;
import org.intermine.api.results.ExportResultsIterator;

/**
 * A result processor for result rows.
 * @author Alex Kalderimis
 *
 */
public class JSONDataTableRowResultProcessor extends JSONResultProcessor
{
    /**
     * Constructor.
     * @param im The API settings bundle
     */
    public JSONDataTableRowResultProcessor() {
    }

    @Override
    protected Iterator<? extends Object> getResultsIterator(Iterator<List<ResultElement>> it) {
        Iterator<? extends Object> jsonIter 
            = new JSONDataTableRowIterator((ExportResultsIterator) it);
        return jsonIter;
    }

}
