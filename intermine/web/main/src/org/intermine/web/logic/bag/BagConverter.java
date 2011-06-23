package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.results.WebResults;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathException;
import org.intermine.web.logic.config.WebConfig;

/**
 * @author "Xavier Watkins"
 *
 */
public abstract class BagConverter
{

    protected InterMineAPI im = null;
    protected WebConfig webConfig = null;

    /**
     *
     * @param im intermine API
     * @param webConfig the webconfig
     */
    public BagConverter(InterMineAPI im, WebConfig webConfig) {
        this.im = im;
        this.webConfig = webConfig;
    }

    /**
     * Returns a List<ResultRows> of converted objects
     * @param profile user's profile
     * @param parameters the parameters
     * @param fromList the list to convert
     * @param type the type to convert to
     * @return a List of ResultRow elements
     * @throws ClassNotFoundException  class not found
     * @throws ObjectStoreException objectstore
     * @throws PathException bad path
     */
    public abstract WebResults getConvertedObjects(Profile profile, List<Integer> fromList,
            String type, String parameters)
        throws ClassNotFoundException, ObjectStoreException, PathException;

    /**
     * Get the ActionMessage to display in the webapp
     * @param externalids the initial ids as a comma separated list
     * @param parameters the parameters
     * @param convertedSize the converted size
     * @param type the type
     * @return an ActionMessage
     * @throws ObjectStoreException exception
     * @throws UnsupportedEncodingException exception
     */
    public abstract ActionMessage getActionMessage(String externalids, int convertedSize,
            String type, String parameters)
        throws ObjectStoreException, UnsupportedEncodingException;

    /**
    *
    * @param profile user's profile
    * @param bagType class of list
    * @param bagList list of intermine object IDs
    * @param constraintValue value of constraint
    * @return list of intermine IDs
    */
    public abstract List<Integer> getConvertedObjectIds(Profile profile, String bagType,
            List<Integer> bagList, String constraintValue);
}
