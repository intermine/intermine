package org.intermine.web.util;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import org.intermine.api.mines.LinkManager;
import org.json.JSONObject;

/**
 * Helper class for intermine links generated on report pages
 *
 * @author Julie Sullivan
 */
public abstract class InterMineLinkGenerator
{

    /**
     * Query other intermines for this object
     *
     * @param olm class resonsible for generating links
     * @param filterValue value of query constraint, eg. organism or department name
     * @param identifier identifier for the object on this report page
     * @return map of mines to objects to link to
     */
    public abstract Collection<JSONObject> getLinks(LinkManager olm, String filterValue,
            String identifier);
}
