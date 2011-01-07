package org.intermine.webservice.client.core;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.ErrorMessageParser;
import org.intermine.webservice.client.util.HttpConnection;

/**
 * Utility class for parsing tab separated result.
 * @author Jakub Kulaviak
 * @author Alexis Kalderimis
 **/
public class TabTableResult extends ResultSet {
	
	public TabTableResult(HttpConnection c) {
		super(c);
	}
	
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
        if (line.startsWith("<error>")) {
            throw new ServiceException(ErrorMessageParser.parseError(line));
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

        public boolean hasNext() {
            return next != null;
        }

        public List<String> next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            List<String> tmp = next;
            next = parseNext();
            return tmp;
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Iterator<List<String>> getIterator() {
        return new TableIterator();
    }
}
