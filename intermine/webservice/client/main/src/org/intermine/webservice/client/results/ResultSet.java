package org.intermine.webservice.client.results;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;

import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.HttpConnection;

/**
 * Common behaviour and interface for result sets.
 * @author Alex Kalderimis
 *
 */
public abstract class ResultSet
{

    private HttpConnection connection = null;
    private String stringResults = null;
    private InputStream streamResults = null;
    private BufferedReader reader = null;

    /**
     * Constructor.
     * @param connection source of data
     */
    public ResultSet(HttpConnection connection) {
        this.connection = connection;
        init();
    }

    /**
     * Constructor with a String.
     *
     * @param stringResults a String containing the http response
     */
    public ResultSet(String stringResults) {
        this.stringResults = stringResults;
        init();
    }

    /**
     * Constructor with a reader.
     *
     * Use this constructor when you want to make the request yourself.
     * @param reader A presupplied reader, presumably obtained by opening a URL or a file.
     */
    public ResultSet(BufferedReader reader) {
        this.reader = reader;
    }

    /**
     * Constructor for use in testing.
     * @param is The source of data.
     */
    ResultSet(InputStream is) {
        this.streamResults = is;
        init();
    }

    private void init() {
        reader = getNewReader();
    }

    /**
     * Get a reader over the data, for reading line-by-line.
     * @return A reader of the data.
     */
    protected BufferedReader getReader() {
        return reader;
    }

    /**
     * Get the raw data from this result set.
     * @return The data as a two-dimensional list (table) of strings.
     */
    abstract List<List<String>> getData();

    private BufferedReader getNewReader() {
        if (connection != null) {
            return new BufferedReader(new InputStreamReader(connection
                        .getResponseBodyAsStream()));
        } else if (streamResults != null) {
            return new BufferedReader(new InputStreamReader(streamResults));
        } else {
            return new BufferedReader(new StringReader(stringResults));
        }
    }

    /**
     * Get the next line of results.
     * @return A string containing the next line of data.
     */
    public String getNextLine() {
        String nextLine = null;
        try {
            nextLine = reader.readLine();
        } catch (IOException e) {
            closeConnection();
            throw new ServiceException("Reading from response stream failed", e);
        }
        if (nextLine == null) {
            closeConnection();
        }
        return nextLine;
    }

    private void closeConnection() {
        if (connection != null) {
            connection.close();
        }
    }
}
