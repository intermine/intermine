package org.intermine.bio.web;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * This is a bean to store cross-reference data
 * This class is half finished, more fields may be added
 *
 * @author Fengyuan Hu
 *
 */
public class XRef
{
    private String sourceName;
    private String url;
    private String imageName;

    /**
     * @param sourceName the source of external data source
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * @param url the url to link out
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @param imageName the name of the logo
     */
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    /**
     *
     * @return sourceName
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     *
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     *
     * @return imageName
     */
    public String getImageName() {
        return imageName;
    }
}
