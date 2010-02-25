package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionMessage;
import org.directwebremoting.WebContextFactory;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.results.WebResults;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathException;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;



/**
 * @author "Xavier Watkins"
 *
 */
public abstract class BagConverter {
    
    private static InterMineAPI im = null;
    private static Model model = null;
    private static WebConfig webConfig = null;
    private static ObjectStore os = null;
    
    
    /**
     * Returns a List<ResultRows> of converted objects
     * @param parameters the parameters
     * @param fromList the list to convert
     * @param type the type to convert to
     * @return a List of ResultRow elements
     * @throws ClassNotFoundException  class not found
     * @throws ObjectStoreException objectstore
     * @throws PathException bad path
     */
    public abstract WebResults getConvertedObjects(Profile profile, List<Integer> fromList, String type, 
            String ... parameters)
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
    public abstract ActionMessage getActionMessage(String externalids, int convertedSize, String type, 
            String ... parameters) 
    throws ObjectStoreException, UnsupportedEncodingException;
    
    public abstract String getFieldsFromConvertedObjects(Profile profile, String bagType,
            String bagName, String constraintValue);
}
