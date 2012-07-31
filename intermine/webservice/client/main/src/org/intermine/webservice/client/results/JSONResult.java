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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.ErrorMessageParser;
import org.intermine.webservice.client.util.HttpConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Alex Kalderimis
 *
 * Utility class for parsing JSON object results
 *
 */
public class JSONResult extends ResultSet
{

    /**
     * Constructor
     * @param c source of data
     */
    public JSONResult(HttpConnection c) {
        super(c);
    }

    /**
     * Constructor with a string
     * @param s The source of the content.
     */
    public JSONResult(String s) {
        super(s);
    }

    /**
     * @return The results as a list of JSON-Objects.
     * @throws JSONException If there is an error parsing the content.
     */
    public List<JSONObject> getObjects() throws JSONException {
        JSONArray results = getResults();
        List<JSONObject> objects = new ArrayList<JSONObject>();
        for (int index = 0; index < results.length(); index++) {
            objects.add(results.getJSONObject(index));
        }
        return objects;
    }

    /**
     * @return The results as a JSONArray.
     */
    public JSONArray getResults() {
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = getNextLine()) != null) {
            sb.append(checkLineForErrors(line));
        }
        try {
            JSONObject resultSet = new JSONObject(sb.toString());
            JSONArray results = resultSet.getJSONArray("results");
            return results;
        } catch (JSONException e) {
            throw new ServiceException("Bad JSON: " + sb.toString(), e);
        }
    }

    private String checkLineForErrors(String line) {
        if (line.startsWith("<error>")) {
            throw new ServiceException(ErrorMessageParser.parseError(line));
        }
        return line;
    }

    private class JSONIterator implements Iterator<JSONObject>
    {

        private JSONArray results;
        private int index = 0;

        public JSONIterator() {
            results = getResults();
        }

        public boolean hasNext() {
            return (index < results.length());
        }

        public JSONObject next() {
            JSONObject next;
            try {
                next = results.getJSONObject(index);
            } catch (JSONException e) {
                throw new RuntimeException("Problem parsing json object", e);
            }
            index++;
            return next;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * @return An iterator over each object in the result set.
     * @throws JSONException if there is an error parsing the data in the JSON.
     */
    public Iterator<JSONObject> getIterator() throws JSONException {
        return new JSONIterator();
    }

    @Override
    List<List<String>> getData() {
        throw new UnsupportedOperationException();
    }

}
