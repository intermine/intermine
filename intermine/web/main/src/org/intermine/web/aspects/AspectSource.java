package org.intermine.web.aspects;

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
 * Describes a source of data contributing to some data set. The sources names and
 * hyperlinks are displayed on data set homepages.
 * 
 * @author Thomas Riley
 * @see org.intermine.web.dataset.Aspect
 */
public class AspectSource
{
    /** Name of source. */
    private String sourceName;
    /** URL */
    private String url;

    /**
     * Get the source name.
     * @return the source name
     */
    public String getName() {
        return sourceName;
    }

    /**
     * Set the source name.
     * @param sourceName source name
     */
    public void setName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * Get the URL back to source website.
     * @return URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the URL to source website.
     * @param url URL
     */
    public void setUrl(String url) {
        this.url = url;
    }
}
