package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Manager of public lists used by web service.
 * @author Jakub Kulaviak
 **/
public class ListManager
{
    private BagManager bagManager;

    /**
     * ListManager constructor.
     * @param request request
     */
    public ListManager(HttpServletRequest request) {
        this.bagManager = SessionMethods.getInterMineAPI(request.getSession()).getBagManager();
    }

    /**
     * Returns public lists that contain object with specified id.
     * @param objectId object id
     * @return list
     */
    public List<String> getListsNames(Integer objectId) {
        List<String> ret = new ArrayList<String>();

        Collection<InterMineBag> bags = bagManager.getGlobalBagsContainingId(objectId);
        for (InterMineBag bag : bags) {
            ret.add(bag.getName());
        }
        return ret;
    }
}
