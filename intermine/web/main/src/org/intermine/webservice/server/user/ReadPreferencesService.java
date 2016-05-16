package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;

import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.core.ReadWriteJSONService;

/**
 * A service for reading the current state of a user's preferences.
 * @author Alex Kalderimis
 *
 */
public class ReadPreferencesService extends ReadWriteJSONService
{

    /** @param im The InterMine state object **/
    public ReadPreferencesService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getResultsKey() {
        return "preferences";
    }

    @Override
    protected void execute() {
        addResultItem(new HashMap<String, Object>(
                getPermission().getProfile().getPreferences()), false);
    }

}
