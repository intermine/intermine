package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.objectstore.ObjectStoreException;
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

    /**
    * Method to return list of values and the counts of converted objects for that object
    * used for display on list analysis page.
    *
    * @param bag intermine bag
    * @param profile user profile
    * @return map of values to counts
    */
    public abstract Map<String, String> getCounts(Profile profile, InterMineBag bag);
}
