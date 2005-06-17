package org.intermine.web.dataset;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * 
 * @author Thomas Riley
 */
public class DataSetSource
{
    /** Name of source. */
    private String sourceName;
    /** URL */
    private String url;

    /**
     * @return Returns the sourceName.
     */
    public String getName() {
        return sourceName;
    }

    /**
     * @param sourceName The sourceName to set.
     */
    public void setName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * @return Returns the url.
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url The url to set.
     */
    public void setUrl(String url) {
        this.url = url;
    }
}
