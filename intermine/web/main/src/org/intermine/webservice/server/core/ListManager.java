package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.struts.ObjectDetailsController;

/**
 * Manager of public lists used by web service. 
 * @author Jakub Kulaviak
 **/
public class ListManager
{
    
   private HttpServletRequest request;

    /**
     * ListManager constructor.
     * @param request request
     */
    public ListManager(HttpServletRequest request) {
        this.request = request;
    }
    
    /**
     * Returns public lists that contain object with specified id.
     * @param objectId object id
     * @return list
     */
    public List<String> getListsNames(Integer objectId) {
        List<String> ret = new ArrayList<String>();
        List<InterMineBag> bags = ObjectDetailsController.
            getGlobalBags(request.getSession(), objectId);
        for (InterMineBag bag : bags) {
            ret.add(bag.getName());
        }
        return ret;
    }
}
