package org.intermine.api;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import org.intermine.model.InterMineObject;


/**
 * Class to manage external links.
 *
 * @author Julie Sullivan
 */
public class LinkRedirectManager
{
    protected Properties webProperties;
    protected static final String ATTR_MARKER_RE = "<<attributeValue>>";

    /**
     * Constructor
     * @param webProperties the web properties
     */
    public LinkRedirectManager(Properties webProperties) {
        this.webProperties = webProperties;
    }

    /**
     *
     * @param imo InterMineObject to generate link
     * @param im intermine API object to retrieve model
     * @return URL to link to
     */
    public String generateLink(InterMineAPI im, InterMineObject imo) {
        return null;
    }

}
