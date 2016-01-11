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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringEscapeUtils;

import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.exceptions.TransferInterruptedException;
import org.intermine.webservice.client.exceptions.ResultException;
import org.intermine.webservice.client.util.HttpConnection;

/**
 * A utility class for parsing tab separated values in the results. The purpose of this class
 * it to transform raw results in the form of an InterMine XML resultset
 * into a data structure containing the rows and their values. This can be done as an
 * iterator or the data can be read directly into a list.
 *
 * usage:
 * <pre>
 * XMLTableResult table = new XMLTableResult(connection);
 * if (getMeAList) {
 *     List&lt;List&lt;String&gt;&gt; data = table.getData();
 *     ...
 * } else {
 *     Iterator&lt;List&lt;String&gt;&gt; iterator = table.getIterator();
 *     ...
 * }
 * </pre>
 *
 * @author Alexis Kalderimis
 **/
public class XMLTableResult extends ResultSet
{

    private XMLStreamReader xmlReader = null;

    /**
     * Constructor.
     * Return a new XMLTableResult object that reads its data from an opened HTTP connection.
     *
     * @param c The connection to read the data from
     */
    public XMLTableResult(HttpConnection c) {
        super(c);
        init();
    }

    /**
     * Constructor.
     * Return a new XMLTableResult object that reads its data from a string.
     *
     * @param s A string to read the data from
     */
    public XMLTableResult(String s) {
        super(s);
        init();
    }

    /**
    * Constructor with a reader.
    *
    * Use this constructor when you want to make the request yourself.
    * @param reader A presupplied reader, presumably obtained by opening a URL or a file.
    */
    public XMLTableResult(BufferedReader reader) {
        super(reader);
        init();
    }

    private void init() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            xmlReader = factory.createXMLStreamReader(getReader());
        } catch (XMLStreamException e) {
            throw new RuntimeException("Error parsing XML result response", e);
        }
    }

    @Override
    public List<List<String>> getData() {
        List<List<String>> ret = new ArrayList<List<String>>();
        List<String> row = null;
        while ((row = getNextRow()) != null) {
            ret.add(row);
        }
        return ret;
    }

    private List<String> getNextRow() {
        List<String> row = new ArrayList<String>();
        boolean hasGotWholeRow = false;
        boolean hasGotWholeResultSet = false;
        String currentElemName = null;
        String errorMessage = null;
        String errorCause = null;
        String currentValue = null;
        try {
            while (xmlReader.hasNext()) {
                int eventType = xmlReader.getEventType();
                switch (eventType) {
                    case XMLStreamConstants.START_ELEMENT:
                        currentElemName = xmlReader.getLocalName();
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        String elemName = xmlReader.getLocalName();
                        if ("Result".equals(elemName)) {
                            hasGotWholeRow = true;
                        } else if ("ResultSet".equals(elemName)) {
                            hasGotWholeResultSet = true;
                        } else if ("i".equals(elemName)) {
                            row.add(currentValue);
                            currentValue = null;
                        }
                        break;

                    case XMLStreamConstants.CHARACTERS:
                        if        ("i".equals(currentElemName)) {
                            String valueChunk = StringEscapeUtils.unescapeXml(xmlReader.getText());
                            if (currentValue == null) {
                                currentValue = valueChunk;
                            } else {
                                currentValue += valueChunk;
                            }
                        } else if ("message".equals(currentElemName)) {
                            errorMessage = xmlReader.getText();
                        } else if ("cause".equals(currentElemName)) {
                            errorCause = xmlReader.getText();
                        } else {
                            String data = xmlReader.getText();
                            if (!(data == null || "".equals(data.trim()))) {
                                throw new ServiceException(
                                        "Character data found in illegal place: "
                                        + currentElemName + ", '" + data + "'"
                                );
                            }
                        }
                        break;
                    default:
                        break;
                }
                xmlReader.next();
                if (hasGotWholeRow) {
                    break;
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException("Error parsing XML result response", e);
        }
        // Check to make sure we can return data
        if (errorMessage != null || errorCause != null) {
            throw new ResultException(errorMessage, errorCause);
        }
        if (!hasGotWholeRow && !hasGotWholeResultSet) {
            throw new TransferInterruptedException();
        }
        if (row.isEmpty()) {
            return null;
        } else {
            return row;
        }
    }

    private class TableIterator implements Iterator<List<String>>
    {

        private List<String> next;

        public TableIterator() {
            next = getNextRow();
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
            next = getNextRow();
            return tmp;
        }

        @Override
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
