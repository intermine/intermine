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
import java.util.NoSuchElementException;

import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.HttpConnection;

/**
 * A utility class for parsing tab separated values in the results. The purpose of this class
 * it to transform raw results in the form of a standard TSV file (tab delimited columns, one row
 * per line) into a data structure.
 *
 * usage:
 * <pre>
 * TabTableResult table = new TabTableResult(connection);
 * if (getMeAList) {
 *     List&lt;List&lt;String&gt;&gt; data = table.getData();
 *     ...
 * } else {
 *     Iterator&lt;List&lt;String&gt;&gt; iterator = table.getIterator();
 *     ...
 * }
 * </pre>
 *
 * @author Jakub Kulaviak
 * @author Alexis Kalderimis
 **/
public class TabTableResult extends ResultSet
{

    /**
     * Constructor.
     * Return a new TabTableResult object that reads its data from an opened HTTP connection.
     *
     * @param c The connection to read the data from
     */
    public TabTableResult(HttpConnection c) {
        super(c);
    }

    /**
     * Constructor.
     * Return a new TabTableResult object that reads its data from a string.
     *
     * @param s A string to read the data from
     */
    public TabTableResult(String s) {
        super(s);
    }

    /**
     * {@inheritDoc}
     */
    public List<List<String>> getData() {
        List<List<String>> ret = new ArrayList<List<String>>();
        String line;
        while ((line = getNextLine()) != null) {
            ret.add(parseLine(line));
        }
        return ret;
    }


    private List<String> parseLine(String line) {
        if (line.startsWith("[ERROR]")) {
            throw new ServiceException(line);
        }
        List<String> ret = new ArrayList<String>();
        String[] parts = line.split("\t");
        for (String part : parts) {
            ret.add(part);
        }
        return ret;
    }

    private class TableIterator implements Iterator<List<String>>
    {

        private List<String> next;

        public TableIterator() {
            next = parseNext();
        }

        private List<String> parseNext() {
            String line = getNextLine();

            if (line != null) {
                return parseLine(line);
            } else {
                return null;
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public List<String> next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            List<String> tmp = next;
            next = parseNext();
            return tmp;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Get a memory efficient iterator over the result rows as lists
     * @return An iterator over rows as lists fo strings.
     */
    public Iterator<List<String>> getIterator() {
        return new TableIterator();
    }
}
