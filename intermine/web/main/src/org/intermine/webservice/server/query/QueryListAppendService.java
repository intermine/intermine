package org.intermine.webservice.server.query;

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

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/**
 * Append items to a list from the results of a query.
 * @author Alex Kalderimis
 *
 */
public class QueryListAppendService extends QueryToListService
{

    /**
     * Constructor.
     * @param api A reference to the InterMine settings bundle.
     */
    public QueryListAppendService(InterMineAPI api) {
        super(api);
    }

    @Override
    protected void generateListFromQuery(PathQuery pq,
        String name, String description, Collection<String> tags,
        Profile profile) throws ObjectStoreException, PathException {

        InterMineBag list = getList(profile, name);
        checkQueryMatchesListType(pq, list);

        Query q = getQuery(pq, profile);

        try {
            list.addToBagFromQuery(q);
        } finally {
            output.addResultItem(Arrays.asList("" + list.size()));
        }
    }

    /**
     * Get a list from a profile, throwing a service forbidden exception if the list is not
     * accessible.
     * @param profile The profile the list is expected to be in.
     * @param name The name of the list.
     * @return The list.
     */
    protected InterMineBag getList(Profile profile, String name) {
        InterMineBag list = profile.getSavedBags().get(name);
        if (list == null) {
            throw new ServiceForbiddenException(name + " is not a list you have access to");
        }
        return list;
    }

    private void checkQueryMatchesListType(PathQuery pq, InterMineBag list) {
        String view = pq.getView().get(0);
        Path path;
        try {
            path = pq.makePath(view);
        } catch (PathException e) {
            throw new ServiceException(e);
        }
        ClassDescriptor queryClass = path.getLastClassDescriptor();
        String type = list.getQualifiedType();
        if (!queryClass.getAllSuperclassNames().contains(type)) {
            throw new BadRequestException("This query is not compatible with '" + list.getName()
                    + "' as the type of its result set (" + queryClass.getUnqualifiedName()
                    + ") is not a " + list.getType());
        }
    }
}

