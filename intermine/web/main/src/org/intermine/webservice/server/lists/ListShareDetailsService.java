package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.UnauthorizedException;

/** @author Alex Kalderimis **/
public class ListShareDetailsService extends JSONService
{

    private final SharedBagManager sbm;
    private final ProfileManager pm;

    /** @param im The InterMine state object **/
    public ListShareDetailsService(InterMineAPI im) {
        super(im);
        pm = im.getProfileManager();
        sbm = SharedBagManager.getInstance(pm);
    }

    @Override
    protected void postInit() {
        super.postInit();
        if (!this.isAuthenticated()) {
            throw new UnauthorizedException("Users must authenticate.");
        }
    }

    @Override
    public String getResultsKey() {
        return "lists";
    }

    @Override
    protected void execute() throws Exception {
        Profile user = getPermission().getProfile();
        Map<String, InterMineBag> usersBags = user.getSavedBags();
        Map<String, Set<String>> usersWhoCanAccessEachBag
            = new HashMap<String, Set<String>>();

        for (InterMineBag bag: usersBags.values()) {
            usersWhoCanAccessEachBag.put(bag.getName(), sbm.getUsersWithAccessToBag(bag));
        }

        Map<String, String> ownersOfBagsSharedWithMe = new HashMap<String, String>();

        for (InterMineBag bag: user.getSharedBags().values()) {
            ownersOfBagsSharedWithMe.put(bag.getName(), pm.getProfileUserName(bag.getProfileId()));
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("sharedByUser", usersWhoCanAccessEachBag);
        data.put("sharedWithUser", ownersOfBagsSharedWithMe);

        addResultItem(data, false);
    }

}
