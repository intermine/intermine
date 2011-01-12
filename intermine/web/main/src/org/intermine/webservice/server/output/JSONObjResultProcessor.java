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
 *
 */
public class JSONObjResultProcessor extends JSONResultProcessor
{

    /**
     * Constructor.
     */
    public JSONObjResultProcessor() {
        // Empty constructor
    }

    @Override
    protected Iterator<? extends Object> getResultsIterator(ExportResultsIterator it) {
        JSONResultsIterator jsonIter = new JSONResultsIterator(it);
        return jsonIter;
    }

}
