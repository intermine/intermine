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
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Manager of public lists used by web service.
 * @author Jakub Kulaviak
 **/
public class ListManager
{
    private static final long MAX_WAIT = 20000;
    private final BagManager bagManager;
    private final Profile profile;

    /**
     * ListManager constructor.
     * @param request request
     */
    public ListManager(HttpServletRequest request) {
        this.bagManager = SessionMethods.getInterMineAPI(request.getSession()).getBagManager();
        this.profile = SessionMethods.getProfile(request.getSession());
    }


    /**
     * Returns public current lists that contain object with specified id.
     * @param objectId object id
     * @return A list of list names
     */
    public List<String> getListsNames(Integer objectId) {
        List<String> ret = new ArrayList<String>();

        Collection<InterMineBag> bags
            = bagManager.getCurrentUserOrGlobalBagsContainingId(profile, objectId);

        for (InterMineBag bag : bags) {
            ret.add(bag.getName());
        }
        return ret;
    }

    /**
     * Returns the lists available to the current user.
     * @return A collection of lists the current user can access.
     */
    public Collection<InterMineBag> getLists() {
        Date waitUntil = new Date(System.currentTimeMillis() + MAX_WAIT);
        // Wait up to 20 secs for the bags to be updated.
        while (new Date().before(waitUntil)) {
            if (!bagManager.isAnyBagNotCurrent(profile)) {
                break;
            }
        }
        return bagManager.getUserAndGlobalBags(profile).values();
    }
    
    public boolean isAnyBagUnresolvable() {
        return bagManager.isAnyBagToUpgrade(profile);
    }

    /**
     * Returns the current lists available to the current user which contain the
     * specified object.
     * @return A collection of lists.
     */
    public Collection<InterMineBag> getListsContaining(Integer objectId) {
        return bagManager.getCurrentUserOrGlobalBagsContainingId(profile, objectId);
    }
}
