package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import javax.servlet.http.HttpSession;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ResultsRow;

/**
 * @author "Xavier Watkins"
 *
 */
public interface BagConverter
{
    /**
     * Get the list of converted intermine objects
     * @param session the session
     * @param parameter the parameters
     * @param fromList the list of objects to convert
     * @param type the type of the bag
     * @return
     * @throws ClassNotFoundException
     * @throws ObjectStoreException
     */
    public List<ResultsRow> getConvertedObjects(HttpSession session, String parameters, 
                                    List<Integer> fromList, String type) 
                                    throws ClassNotFoundException, ObjectStoreException;    
}
