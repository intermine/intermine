package org.intermine.bio.webservice;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * Export a List as GFF3.
 * @author Alex Kalderimis
 *
 */
public class GFF3ListService extends GFFQueryService
{

    private static final String LIST_PARAM = "list";

    /**
     *
     * @param im intermine API
     */
    public GFF3ListService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected PathQuery getQuery() {
        InterMineBag list = getList();
        PathQuery pq = new PathQuery(im.getModel());
        pq.addView(list.getType() + ".id");
        pq.addConstraint(Constraints.in(list.getType(), list.getName()));
        return pq;
    }

    private InterMineBag getList() {
        String listName = getRequiredParameter(LIST_PARAM);
        Profile p = getPermission().getProfile();
        InterMineBag list = im.getBagManager().getBag(p, listName);
        if (list == null) {
            throw new BadRequestException("Cannot access a list called" + listName);
        }
        return list;
    }

}
