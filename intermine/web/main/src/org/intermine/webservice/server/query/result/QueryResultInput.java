package org.intermine.webservice.server.query.result;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.server.WebServiceInput;


/**
 * QueryServiceInput is parameter object representing parameters for
 * QueryResultService web service.
 *
 * @author Jakub Kulaviak
 **/
public class QueryResultInput extends WebServiceInput
{

    private String xml;

    private boolean computeTotalCount = false;

    private String layout;

    /**
     * @return layout string specifying result table layout
     */
    public String getLayout() {
        return layout;
    }

    /**
     * @param layout string specifying result table layout
     */
    public void setLayout(String layout) {
        this.layout = layout;
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
}
