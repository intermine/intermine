package org.intermine.web.logic.config;

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
 * Will store a link configured through HeaderConfig (WebConfig) and used on report page header
 * @author radek
 *
 */
public class HeaderConfigLink
{

    private String url;
    private String text;
    private String image;

    /**
     * Set
     * @param url header link url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     *
     * @return link url
     */
    public String getUrl() {
        return this.url;
    }

    /**
     * Set
     * @param text header link text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     *
     * @return link text
     */
    public String getText() {
        return this.text;
    }

    /**
     * Set
     * @param image header link image name
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     *
     * @return link image name
     */
    public String getImage() {
        return this.image;
    }

}
