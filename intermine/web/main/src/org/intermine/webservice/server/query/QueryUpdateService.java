package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.SavedQuery;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.exceptions.ServiceException;

/**
 * Same in all respects as the upload service, but this class overwrites queries with the provided
 * names.
 *
 * @author Alex Kalderimis
 *
 */
public class QueryUpdateService extends QueryUploadService
{

    /** @param im The InterMine state object **/
    public QueryUpdateService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Map<String, String> saveQueries(Map<String, PathQuery> toSave) {
        Map<String, String> retval = new HashMap<String, String>();
        for (Entry<String, PathQuery> pair: toSave.entrySet()) {
            String name = pair.getKey();
            SavedQuery previousState = profile.getSavedQueries().get(pair.getKey());
            try {
                profile.saveQuery(name, new SavedQuery(name, new Date(), pair.getValue()));
                retval.put(name, name);
            } catch (Exception e) {
                try {
                    if (previousState != null) {
                        profile.saveQuery(pair.getKey(), previousState);
                    }
                } catch (Exception e2) {
                    // Ignore;
                }
                throw new ServiceException("Could not save query " + pair.getKey(), e);
            }
        }
        return retval;
    }
}
