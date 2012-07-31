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

import org.intermine.webservice.client.util.HttpConnection;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * A class for managing results received as a jsonrows result-set.
 *
 * This class contains logic for parsing data in this format into meaningful
 * data-structures, and handling errors.
 * @author Alex Kalderimis
 *
 */
public class RowResultSet extends ResultSet
{

    private List<String> views;
    private final StringBuffer containerBuffer = new StringBuffer();
    private final boolean useNewAPI;

    /**
     * Construct a new result-set with an HttpConnection and a list of output columns.
     * @param connection The connection to receive results from.
     * @param views The columns selected for output.
     * @param version The web service version of the service these results come from.
     */
    public RowResultSet(HttpConnection connection, List<String> views, int version) {
        super(connection);
        init(views);
        useNewAPI = version >= 8;
    }

    /**
     * Constructor with a reader.
     *
     * Use this constructor when you want to make the request yourself.
     *
     * @param reader A presupplied reader, presumably obtained by opening a URL or a file.
     * @param views The columns selected for output.
     * @param version The web service version of the service these results come from.
     */
    public RowResultSet(BufferedReader reader, List<String> views, int version) {
        super(reader);
        init(views);
        useNewAPI = version >= 8;
    }

    /**
     * Construct a new result-set from a string and a list of output columns.
     *
     * This constructor is primarily used in testing.
     *
     * @param stringResults The results as a single string.
     * @param views The columns selected for output.
     */
    public RowResultSet(String stringResults, List<String> views) {
        super(stringResults);
        init(views);
        useNewAPI = false;
    }

    // At Package level for testing
    /**
     * Construct a new result-set with an HttpConnection and a list of output columns.
     * @param is The input-stream to receive results from.
     * @param views The columns selected for output.
     */
    RowResultSet(InputStream is, List<String> views) {
        super(is);
        init(views);
        useNewAPI = false;
    }

    private void init(List<String> views) {
        this.views = views;
    }

    @Override
    public List<List<String>> getData() {
        List<List<String>> ret = new ArrayList<List<String>>();
        String rowData = null;
        while ((rowData = getNextRow()) != null) {
            List<Object> r = new ResultRowList(rowData);
            List<String> row = new ArrayList<String>();
            for (Object o: r) {
                row.add(o.toString());
            }
            ret.add(row);
        }
        return ret;
    }

    /**
     * Get the data for this result set in a parsed form.
     *
     * In this format, each row is parsed into a suitable object type. The types
     * of this data should match that of the data-types of the attributes selected for
     * output. You will need to cast these objects to a suitable type before use.
     *
     * @return A two-dimensional list (tables) of objects.
     */
    public List<List<Object>> getRowsAsLists() {
        List<List<Object>> ret = new ArrayList<List<Object>>();
        String rowData = null;
        while ((rowData = getNextRow()) != null) {
            if (useNewAPI) {
                ret.add(new JsonRow(rowData));
            } else {
                ret.add(new ResultRowList(rowData));
            }
        }
        return ret;
    }

    /**
     * Get the data for this result set in a parsed form.
     *
     * In this format, each row is parsed into a suitable object type. The types
     * of this data should match that of the data-types of the attributes selected for
     * output. You will need to cast these objects to a suitable type before use.
     *
     * @return A list of rows, where each row is a map from column name to value.
     */
    public List<Map<String, Object>> getRowsAsMaps() {
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        String rowData = null;
        while ((rowData = getNextRow()) != null) {
            if (useNewAPI) {
                ret.add(new JsonRowMap(rowData, views));
            } else {
                ret.add(new ResultRowMap(rowData, views));
            }
        }
        return ret;
    }

    private void checkContainerStatus() {
        String container = containerBuffer.toString();
        try {
            JSONObject parsed = new JSONObject(container);
            if (!parsed.getBoolean("wasSuccessful")) {
                throw new ServiceException(parsed.getString("error"));
            }
        } catch (JSONException e) {
            throw new ServiceException(
                    "Error parsing container - transmission may have been interrupted");
        }
    }

    private String getNextRow() {
        String nextLine = getNextLine();
        if (nextLine == null) {
            checkContainerStatus();
            return null;
        }
        if (!nextLine.startsWith("[")) {
            containerBuffer.append(nextLine);
            return getNextRow();
        }
        if (nextLine.endsWith(",")) {
            return nextLine.substring(0, nextLine.length() - 1);
        }
        return nextLine;
    }

    private class RowMapIterator implements Iterator<Map<String, Object>>
    {
        private Map<String, Object> next;

        public RowMapIterator() {
            next = nextRowMap();
        }

        private Map<String, Object> nextRowMap() {
            String line = getNextRow();

            if (line != null) {
                return useNewAPI ? new JsonRowMap(line, views) : new ResultRowMap(line, views);
            } else {
                return null;
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Map<String, Object> next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            Map<String, Object> tmp = next;
            next = nextRowMap();
            return tmp;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class RowListIterator implements Iterator<List<Object>>
    {
        private List<Object> next;

        public RowListIterator() {
            next = nextRowList();
        }

        private List<Object> nextRowList() {
            String line = getNextRow();

            if (line != null) {
                return useNewAPI ? new JsonRow(line) : new ResultRowList(line);
            } else {
                return null;
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public List<Object> next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            List<Object> tmp = next;
            next = nextRowList();
            return tmp;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Get a memory efficient iterator over the result rows as lists
     * @return an iterator over the rows in this result set as lists of values
     */
    public Iterator<List<Object>> getListIterator() {
        return new RowListIterator();
    }

    /**
     * Get a memory efficient iterator over the result rows as lists
     * @return an iterator over the rows in this result set as maps of column names to values
     */
    public Iterator<Map<String, Object>> getMapIterator() {
        return new RowMapIterator();
    }

}
