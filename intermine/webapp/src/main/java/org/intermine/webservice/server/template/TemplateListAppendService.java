package org.intermine.webservice.server.template;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagQueryResult;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.query.MainHelper;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/**
 * A class to append items from a set of template results to a
 * list.
 * @author alex
 *
 */
public class TemplateListAppendService extends TemplateToListService
{

    /**
     * Constructor.
     * @param im The reference to the InterMine settings bundle.
     */
    public TemplateListAppendService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void generateListFromQuery(PathQuery pq,
        String name, String description, Collection<String> tags,
        Profile profile) throws ObjectStoreException, PathException {
        Query q = MainHelper.makeQuery(
                pq,
                bagManager.getBags(profile),
                new HashMap<String, QuerySelectable>(),
                im.getBagQueryRunner(),
                new HashMap<String, BagQueryResult>());

        InterMineBag list = profile.getSavedBags().get(name);
        if (list == null) {
            throw new BadRequestException(
                    "You do not have access to a list called '" + name + "'");
        }
        try {
            list.addToBagFromQuery(q);
            try {
                im.getBagManager().addTagsToBag(tags, list, profile);
            } catch (TagManager.TagNameException e) {
                throw new BadRequestException(e.getMessage());
            } catch (TagManager.TagNamePermissionException e) {
                throw new ServiceForbiddenException(e.getMessage());
            }
        } finally {
            output.addResultItem(Arrays.asList("" + list.size()));
        }
    }
}
