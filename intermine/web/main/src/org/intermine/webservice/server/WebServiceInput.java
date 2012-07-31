package org.intermine.webservice.server;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Base class for input classes used with web services.
 * @author Jakub Kulaviak
 **/
public class WebServiceInput
{

    private Integer start;

    private Integer maxCount;

    private String userName;

    private String password;

    /**
     * @return user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName user name
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password password
     */
    public void setPassword(String password) {
        this.password = password;
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
}
