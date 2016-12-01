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

import org.intermine.api.results.ResultElement;
import org.json.JSONArray;

/**
 * @author Alexis Kalderimis
 *
 */
public class MinimalJsonIterator implements Iterator<JSONArray>
{
    private final Iterator<List<ResultElement>> subIter;

    /**
     * Constructor
     * @param it An ExportResultsIterator that will be used internally to process the data.
     */
    public MinimalJsonIterator(Iterator<List<ResultElement>> it) {
        this.subIter = it;
    }

    @Override
    public JSONArray next() {
        List<ResultElement> row = subIter.next();
        List<Object> jsonRow = new ArrayList<Object>();
        for (int i = 0; i < row.size(); i++) {
            ResultElement re = row.get(i);
            if (re == null) {
                // In the case of flattened outerjoins.
                jsonRow.add(null);
            } else {
                Object field = re.getField();
                // Stringify all char-sequences, (ie. force Clob evaluation)
                if (field instanceof CharSequence) {
                    field = field.toString();
                }
                jsonRow.add(field);
            }
        }
        JSONArray next = new JSONArray(jsonRow);
        return next;
    }

    @Override
    public boolean hasNext() {
        return subIter.hasNext();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported for this implementation");
    }
}
