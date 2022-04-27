package org.intermine.webservice.server.idresolution;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.bag.BagQueryUpgrade;
import org.intermine.api.idresolution.IDResolver;
import org.intermine.api.idresolution.Job;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.lists.ListInput;

/** @author Daniela Butano **/
public class ListResolutionService extends JSONService
{
    /**
     * Default constructor.
     * @param im The InterMine state object.
     */
    public ListResolutionService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        Profile profile = getPermission().getProfile();
        String listName = getRequiredParameter(ListInput.NAME_PARAMETER);
        InterMineBag savedBag = profile.getSavedBags().get(listName);
        if (savedBag == null) {
            throw new ResourceNotFoundException(listName + " doesn't exists");
        }

        final BagQueryRunner runner = im.getBagQueryRunner();
        BagQueryUpgrade bagQueryUpgrade = new BagQueryUpgrade(runner, savedBag);
        Job job = IDResolver.getInstance().submit(bagQueryUpgrade);

        addResultValue(job.getUid(), false);
    }

    @Override
    protected String getResultsKey() {
        return "uid";
    }

}
