package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.json.JSONArray;

/**
 * @author Alexis Kalderimis
 *
 */
public class JSONDataTableRowIterator implements Iterator<JSONArray>
{

    private final ExportResultsIterator subIter;

    /**
     * Constructor
     * @param it An ExportResultsIterator that will be used internally to process the data.
     */
    public JSONDataTableRowIterator(ExportResultsIterator it) {
        this.subIter = it;
    }

    @Override
    public boolean hasNext() {
        return subIter.hasNext();
    }

    @Override
    public JSONArray next() {
        List<ResultElement> row = subIter.next();
        List<Object> jsonRow = new ArrayList<Object>();
        for (int i = 0; i < row.size(); i++) {
            ResultElement re = row.get(i);
            if (re == null || re.getId() == null) {
                jsonRow.add(null);
            } else {
                Object field = re.getField();
                if (field instanceof CharSequence) {
                    field = field.toString();
                }
                jsonRow.add(re.getField());
            }
        }
        JSONArray next = new JSONArray(jsonRow);
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported for this implementation");
    }

}
