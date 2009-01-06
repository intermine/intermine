package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionMessage;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.results.WebResults;

/**
 * @author "Xavier Watkins"
 *
 */
public interface BagConverter
{

    /**
     * Returns a List<ResultRows> of converted objects
     * @param session the session
     * @param parameters the parameters
     * @param fromList the list to convert
     * @param type the type to convert to
     * @return a List of ResultRow elements
     * @throws ClassNotFoundException  class not found
     * @throws ObjectStoreException objectstore
     */
    public WebResults getConvertedObjects(HttpSession session, String parameters,
                                    List<Integer> fromList, String type)
                                    throws ClassNotFoundException, ObjectStoreException;

    /**
     * Get the ActionMessage to display in the webapp
     * @param model the model
     * @param externalids the initial ids as a comma separated list
     * @param organism the organism to convert to
     * @param convertedSize the converted size
     * @param type the type
     * @return an ActionMessage
     * @throws ObjectStoreException exception
     * @throws UnsupportedEncodingException exception
     */
    public ActionMessage getActionMessage(Model model, String externalids, int convertedSize,
                                          String type, String organism)
                                          throws ObjectStoreException, UnsupportedEncodingException;
}
