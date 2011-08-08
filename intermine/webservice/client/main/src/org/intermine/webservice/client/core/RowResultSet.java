package org.intermine.webservice.client.core;

import org.intermine.webservice.client.util.HttpConnection;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.json.JSONObject;
import org.json.JSONException;

public class RowResultSet extends ResultSet {

    private List<String> views;
    private final StringBuffer containerBuffer = new StringBuffer();

	public RowResultSet(HttpConnection connection, List<String> views) {
		super(connection);
        init(views);
	}

	public RowResultSet(String stringResults, List<String> views) {
		super(stringResults);
        init(views);
	}

    // Package level for testing
    RowResultSet(InputStream is, List<String> views) {
        super(is);
        init(views);
    }

    private void init(List<String> views) {
        this.views = views;
    }

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

    public List<List<Object>> getRowsAsLists() {
        List<List<Object>> ret = new ArrayList<List<Object>>();
        String rowData = null;
        while ((rowData = getNextRow()) != null) {
            ret.add(new ResultRowList(rowData));
        }
        return ret;
    }

    public List<Map<String, Object>> getRowsAsMaps() {
        List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
        String rowData = null;
        while ((rowData = getNextRow()) != null) {
            ret.add(new ResultRowMap(rowData, views));
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
            throw new ServiceException("Error parsing container - transmission may have been interrupted");
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
                return new ResultRowMap(line, views);
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
                return new ResultRowList(line);
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
     */
    public Iterator<List<Object>> getListIterator() {
        return new RowListIterator();
    }

    /**
     * Get a memory efficient iterator over the result rows as lists
     */
    public Iterator<Map<String, Object>> getMapIterator() {
        return new RowMapIterator();
    }

}
