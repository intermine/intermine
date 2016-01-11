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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/** @author Alex Kalderimis **/
public class ListAppendService extends ListUploadService
{

    /** @param im The InterMine state object **/
    public ListAppendService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void makeList(
            ListInput listInput,
            String type,
            Profile profile,
            Set<String> rubbishbin) throws Exception {

        Set<String> ids = new LinkedHashSet<String>();
        Set<String> unmatchedIds = new HashSet<String>();

        ListCreationInput input = (ListCreationInput) listInput;

        InterMineBag bag = profile.getSavedBags().get(input.getListName());
        if (bag == null) {
            throw new ServiceForbiddenException(
                input.getListName() + " is not a list you have access to");
        }

        processIdentifiers(bag.getType(), input, ids, unmatchedIds, bag);

        setListSize(bag.size());

        for (Iterator<String> i = unmatchedIds.iterator(); i.hasNext();) {
            List<String> row = new ArrayList<String>(Arrays.asList(i.next()));
            if (i.hasNext()) {
                row.add("");
            }
            output.addResultItem(row);
        }

    }

}
