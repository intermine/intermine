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

import java.util.Iterator;

import org.intermine.api.results.ExportResultsIterator;

/**
 * @author Alexis Kalderimis
 * A class for producing rows of results as JSON arrays being written to the output.
 */
public class JSONRowResultProcessor extends JSONResultProcessor
{

    private String baseUrl;

    /**
     * Constructor.
     * @param baseUrl The base URL to be used for constructing links with.
     */
    public JSONRowResultProcessor(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    protected Iterator<? extends Object> getResultsIterator(ExportResultsIterator it) {
        JSONRowIterator jsonIter = new JSONRowIterator(it, baseUrl);
        return jsonIter;
    }
}
