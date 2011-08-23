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
import java.util.List;

import org.intermine.api.InterMineAPI;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;

/**
 * A result processor for result rows.
 * @author Alex Kalderimis
 *
 */
public class JSONRowResultProcessor extends JSONResultProcessor
{
    private final InterMineAPI im;
    /**
     * Constructor.
     * @param im The API settings bundle
     */
    public JSONRowResultProcessor(InterMineAPI im) {
        this.im = im;
    }

    @Override
    protected Iterator<? extends Object> getResultsIterator(Iterator<List<ResultElement>> it) {
        JSONRowIterator jsonIter = new JSONRowIterator((ExportResultsIterator) it, im);
        return jsonIter;
    }

}
