package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;

/**
 * Manager of public lists used by web service.
 * @author Jakub Kulaviak
 **/
public class ListManager implements Producer<Map<String, InterMineBag>>
{
    private static final long MAX_WAIT = 0;
    private final BagManager bagManager;
    private final Profile profile;

    /**
     * ListManager constructor.
     * @param im The InterMine state object
     * @param profile The current user's profile.
     */
    public ListManager(InterMineAPI im, Profile profile) {
        this.bagManager = im.getBagManager();
        this.profile = profile;
    }


    /**
     * Returns public current lists that contain object with specified id.
     * @param objectId object id
     * @return A list of list names
     */
    public List<String> getListsNames(Integer objectId) {
        List<String> ret = new ArrayList<String>();

        Collection<InterMineBag> bags
            = bagManager.getCurrentBagsContainingId(profile, objectId);

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
            if (!bagManager.isAnyBagNotCurrentOrUpgrading(profile)) {
                break;
            }
        }
        return bagManager.getBags(profile).values();
    }

    /**
     * Returns the lists available to the current user in a Map.
     * @return An unmodifiable map from list name to list.
     */
    public Map<String, InterMineBag> getListMap() {
        return Collections.unmodifiableMap(bagManager.getBags(profile));
    }

    /**
     * Return true if there is at least one bag  in the 'to_upgrade' state.
     * @return true if there are any bags to upgrade
     */
    public boolean isAnyBagUnresolvable() {
        return bagManager.isAnyBagToUpgrade(profile);
    }

    /**
     * Returns the current lists available to the current user which contain the
     * specified object.
     * @param objectId the id of an InterMineObject to look up
     * @return A collection of lists.
     */
    public Collection<InterMineBag> getListsContaining(Integer objectId) {
        return bagManager.getCurrentBagsContainingId(profile, objectId);
    }


    @Override
    public Map<String, InterMineBag> produce() {
        return getListMap();
    }
}
