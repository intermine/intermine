package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A service for upgrading lists after a new build.
 * @author Daniela Butano
 *
 */
public class ListUpgradingService extends ListUploadService//AuthenticatedListService
{

    /**
     * Constructor.
     * @param im The InterMine API settings.
     */
    public ListUpgradingService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        Profile profile = getPermission().getProfile();
        String listName = getRequiredParameter(ListInput.NAME_PARAMETER);
        List<Integer> internalIDs = new ArrayList<Integer>();
        InterMineBag listToUpgrade = profile.getSavedBags().get(listName);
        if (listToUpgrade == null) {
            throw new ResourceNotFoundException(listName + " doesn't exists");
        }
        if (!listToUpgrade.isToUpgrade()) {
            throw new BadRequestException(listName + " can not be upgraded because its state is "
                                        + listToUpgrade.getState());
        }
        BufferedReader r = getReader(request);
        final StrMatcher matcher = getMatcher();
        String line = null;
        try {
            while ((line = r.readLine()) != null) {
                final StrTokenizer st =
                        new StrTokenizer(line, matcher);
                while (st.hasNext()) {
                    final String token = st.nextToken();
                    internalIDs.add(Integer.parseInt(token));
                }
            }
        } finally {
            if (r != null) {
                r.close();
            }
        }

        listToUpgrade.upgradeOsb(internalIDs, true);

        addOutputInfo(LIST_NAME_KEY, listToUpgrade.getName());
        addOutputInfo(LIST_SIZE_KEY, "" + listToUpgrade.size());
        addOutputInfo(LIST_ID_KEY, "" + listToUpgrade.getSavedBagId());
    }
}
