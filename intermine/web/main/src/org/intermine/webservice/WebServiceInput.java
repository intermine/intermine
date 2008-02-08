package org.intermine.webservice;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * WebServiceInput is class that encapsulates parameters for running web service
 * parsed from parameters in request.
 * @author Jakub Kulaviak
 **/
public class WebServiceInput
{

    /**
     * XML_FORMAT constant
     */
    public static final String XML_FORMAT = "xml";
    /**
     * TSV_FORMAT constant
     */
    public static final String TSV_FORMAT = "tab";

    private String xml;
    private Integer start;
    private Integer maxCount;
    private boolean computeTotalCount = false;
    private boolean onlyTotalCount = false;
    private String format;

    /**
     * Returns which format will be used for output.
     * @return format
     */
    public String getFormat() {
        return format;
    }

    /**
     * Sets output format.
     * @param format format
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Returns true if at the output only count of results should be displayed.
     * @return value
     */
    public boolean isOnlyTotalCount() {
        return onlyTotalCount;
    }

    /**
     * Set true if  at the output only count of results should be displayed.
     * @param onlyTotalCount count
     */
    public void setOnlyTotalCount(boolean onlyTotalCount) {
        this.onlyTotalCount = onlyTotalCount;
    }

    /**
     * Returns true if should be displayed total count of all available results.
     * @return value
     */
    public boolean isComputeTotalCount() {
        return computeTotalCount;
    }

    /**
     * Sets true if compute total count of all available results.
     * @param computeTotalCount true if compute total count
     */
    public void setComputeTotalCount(boolean computeTotalCount) {
        this.computeTotalCount = computeTotalCount;
    }

    /**
     * Gets xml query string.
     * @return xml
     */
    public String getXml() {
        return xml;
    }

    /**
     * Sets xml query string.
     * @param xml xml string
     */
    public void setXml(String xml) {
        this.xml = xml;
    }

    /**
     * Sets from which index should be results returned 1-based.
     * @param start start
     */
    public void setStart(Integer start) {
        this.start = start;
    }

    /**
     * Sets maximum of returned results.
     * @param maxCount maximal count
     */
    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    /**
     * Returns index of first returned result
     * @return index of first returned result
     */
    public Integer getStart() {
        return start;
    }

    /**
     * Returns maximum count of results do be returned.
     * @return maximum count
     */
    public Integer getMaxCount() {
        return maxCount;
    }

    /**
     * Returns true if xml format of output is set.
     * @return true if xml format is set
     */
    public boolean isXmlFormat() {
        return XML_FORMAT.equalsIgnoreCase(getFormat());
    }
}
