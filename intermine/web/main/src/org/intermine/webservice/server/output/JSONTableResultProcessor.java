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

import java.util.Collections;
import java.util.Iterator;

import org.intermine.api.results.ExportResultsIterator;

/**
 * @author Alexis Kalderimis
 * A class for producing rows of results as JSON arrays being written to the output.
 */
public class JSONTableResultProcessor extends JSONResultProcessor
{


    /**
     * Constructor.
     * @param baseUrl The base URL to be used for constructing links with.
     */
    public JSONTableResultProcessor() {
        // empty constructor
    }

    @Override
    protected Iterator<? extends Object> getResultsIterator(ExportResultsIterator it) {
        return Collections.EMPTY_LIST.iterator();
    }
}
