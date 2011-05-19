package org.intermine.web.logic.config;

/*
 * Copyright (C) 2002-2011 FlyMine
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

    private String linkUrl;
    private String linkText;
    private String linkImageName;

    /**
     * Set
     * @param url header link url
     */
    public void setLinkUrl(String url) {
        this.linkUrl = url;
    }

    /**
     *
     * @return link url
     */
    public String getLinkUrl() {
        return this.linkUrl;
    }

    /**
     * Set
     * @param text header link text
     */
    public void setLinkText(String text) {
        this.linkText = text;
    }

    /**
     *
     * @return link text
     */
    public String getLinkText() {
        return this.linkText;
    }

    /**
     * Set
     * @param imageName header link image name
     */
    public void setLinkImageName(String imageName) {
        this.linkImageName = imageName;
    }

    /**
     *
     * @return link image name
     */
    public String getLinkImageName() {
        return this.linkImageName;
    }

}
