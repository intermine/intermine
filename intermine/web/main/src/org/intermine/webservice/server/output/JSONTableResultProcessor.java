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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.intermine.api.results.ResultElement;

/**
 * @author Alexis Kalderimis
 * A class for producing rows of results as JSON arrays being written to the output.
 */
public class JSONTableResultProcessor extends JSONResultProcessor
{


    /**
     * Constructor.
     */
    public JSONTableResultProcessor() {
        // empty constructor
    }

    @Override
    protected Iterator<? extends Object> getResultsIterator(Iterator<List<ResultElement>> it) {
        return Collections.EMPTY_LIST.iterator();
    }
}
