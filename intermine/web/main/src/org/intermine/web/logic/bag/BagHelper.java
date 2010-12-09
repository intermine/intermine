package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryRunner;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.MainHelper;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;

/**
 * Helper methods for bags.
 *
 * @author Kim Rutherford
 */
public final class BagHelper
{
    private BagHelper() {
    }

    /** When generating new bag names, this is used as a prefix. */
    public static final String BAG_NAME_PREFIX = "bag";


    /**
     * Create a bag for the given profile and bag name from a PathQuery.  The PathQuery must
     * select only the id field from the type the bag is to be created from.  The name will be
     * made unique with "_n" if it already exists in the profile.
     * @param os the production ObjectStore
     * @param bagQueryRunner the bag query runner
     * @param profile bag will be created in this profile
     * @param bagName name of new bag
     * @param bagDescription a description for the new bag
     * @param bagType the class of object in the bag
     * @param pathQuery the query to create the bag from
     * @return the new bag, already saved
     * @throws ObjectStoreException if persistence problem
     */
    public static InterMineBag createBagFromPathQuery(PathQuery pathQuery, String bagName,
            String bagDescription, String bagType, Profile profile, InterMineAPI im) throws ObjectStoreException {
        if (pathQuery.getView().size() != 1) {
            throw new RuntimeException("Can only create bags from a PathQuery that selects just "
                    + "id");
        }
        try {
            Path idPath = pathQuery.makePath(pathQuery.getView().get(0));
            if (!"id".equals(idPath.getLastElement())) {
                throw new RuntimeException("Can only create bags from a PathQuery that selects"
                        + " just id");
            }
        } catch (PathException e) {
            throw new RuntimeException("Bag creation query has invalid path in view: "
                    + pathQuery.getView(), e);
        }

        ObjectStoreInterMineImpl os = (ObjectStoreInterMineImpl)im.getObjectStore();
        ObjectStoreWriterInterMineImpl osw = os.getNewWriter();

        BagManager bagManager = im.getBagManager();

        Query q = MainHelper.makeQuery(pathQuery, bagManager.getUserAndGlobalBags(profile), null, im.getBagQueryRunner(), null);

        InterMineBag bag = new InterMineBag(bagName, bagType, bagDescription, new Date(), os,
                profile.getUserId(), profile.getProfileManager().getProfileObjectStoreWriter());
        osw.addToBagFromQuery(bag.getOsb(), q);
        profile.saveBag(bag.getName(), bag);
        return bag;
    }

    /**
     * @see
     * @param bag the bag
     * @param os  the object store
     * @param dbName the database to link to
     * @param attrName the attribute name (identifier, omimId, etc)
     * @return the string of comma separated identifiers
     *    */

    public static String getIdList(InterMineBag bag, ObjectStore os, String dbName, String attrName)
    {
        Results results;

        Query q = new Query();
        QueryClass queryClass;
        try {
            queryClass = new QueryClass(Class.forName(bag.getQualifiedType()));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("no type in the bag??! -> ", e);
        }
        q.addFrom(queryClass);

        QueryField qf = new QueryField(queryClass, attrName);
        q.addToSelect(qf);

        QueryField id = new QueryField(queryClass, "id");

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);

        //added because sometimes identifier is null, and StringUtil.join complains
        SimpleConstraint sc = new SimpleConstraint(qf, ConstraintOp.IS_NOT_NULL);

        BagConstraint bagC = new BagConstraint(id, ConstraintOp.IN, bag.getOsb());

        cs.addConstraint(sc);
        cs.addConstraint(bagC);
        q.setConstraint(cs);

        results = os.executeSingleton(q, 10000, true, true, true);

        String delim = null;
        if (dbName.equalsIgnoreCase("flybase")) {
            delim = "|";
        } else if (StringUtils.isEmpty(delim)) {
            delim = ",";
        }
        return StringUtil.join(results, delim);
    }
}
