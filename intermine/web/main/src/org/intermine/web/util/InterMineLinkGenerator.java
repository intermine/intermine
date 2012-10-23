package org.intermine.web.util;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import org.intermine.api.mines.FriendlyMineManager;
import org.json.JSONObject;

/**
 * Helper class for intermine links generated on report and list pages
 *
 * @author Julie Sullivan
 */
public abstract class InterMineLinkGenerator
{

    /**
     * Query other intermines for this object
     *
     * @param olm class resonsible for generating links
     * @param filterValue value of query constraint, eg. organism(s) or department name
     * @param identifier identifier(s) for the object on report page or in list
     * @param mineName name of mine (NULL if all mines are being queried)
     * @return map of mines to objects to link to
     */
    public abstract Collection<JSONObject> getLinks(FriendlyMineManager olm, String mineName,
            String filterValue, String identifier);
}
