package org.intermine.webservice.client.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.HttpConnection;

public abstract class ResultSet {
	

    private HttpConnection connection = null;
    private String stringResults = null;
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
    
    private void init() {
    	reader = getNewReader();
    }
    
    private BufferedReader getNewReader() {
         if (connection != null) {
             return new BufferedReader(new InputStreamReader(connection
                         .getResponseBodyAsStream()));
         } else {
             return new BufferedReader(new StringReader(stringResults));
         }
    }
    
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
