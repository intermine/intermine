package org.intermine.webservice.client.results;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.AbstractList;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Class representing a row of data as a list. This class parses JSON row data in the
 * 8+ webservice API version format (ie. a simple array of values such as
 * <code>[1, "foo", null, true, 1.23]</code>.
 *
 * @author Alex Kalderimis
 *
 */
public class JsonRow extends AbstractList<Object>
{

    private final JSONArray data;

    /**
     * Construct a new result row object backed by the given JSONArray.
     * @param ja The source array.
     */
    public JsonRow(JSONArray ja) {
        this.data = ja;
    }

    /**
     * Construct a new result row object backed by the JSONArray which can
     * be parsed from the given String.
     * @param input The source string.
     */
    public JsonRow(String input) {
        try {
            this.data = new JSONArray(input);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object get(int index) {
        try {
            return data.get(index);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int size() {
        return data.length();
    }

}
