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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;

import org.intermine.api.InterMineAPI;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.model.InterMineObject;
import org.intermine.pathquery.Path;
import org.intermine.web.logic.PortalHelper;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Alexis Kalderimis
 *
 */
public class MinimalJsonIterator implements Iterator<JSONArray>
{
    private final ExportResultsIterator subIter;
    private final InterMineAPI im;

    /**
     * Constructor
     * @param it An ExportResultsIterator that will be used internally to process the data.
     * @param im A reference to the the API settings bundle.
     */
    public MinimalJsonIterator(ExportResultsIterator it, InterMineAPI im) {
        this.subIter = it;
        this.im = im;
    }

    public JSONArray next() {
        List<ResultElement> row = subIter.next();
        List<Object> jsonRow = new ArrayList<Object>();
        for (int i = 0; i < row.size(); i++) {
            ResultElement re = row.get(i);
            jsonRow.add(re.getField());
        }
        JSONArray next = new JSONArray(jsonRow);
        return next;
    }

    public boolean hasNext() {
        return subIter.hasNext();
    }

    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported for this implementation");
    }
}
