package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.intermine.dataloader.BaseEquivalentObjectFetcher;
import org.intermine.dataloader.EquivalentObjectFetcher;
import org.intermine.dataloader.Source;
import org.intermine.metadata.MetaDataException;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.IntToIntMap;
import org.intermine.web.logic.bag.IdUpgrader;

import org.apache.log4j.Logger;

/**
 * Bag object id upgrader that uses the primary keys to find the objects in the new ObjectStore.
 * @author Kim Rutherford
 */
public class PkQueryIdUpgrader implements IdUpgrader
{
    private static final Logger LOG = Logger
            .getLogger(PkQueryIdUpgrader.class);
    private Source source = null;
    EquivalentObjectFetcher eof;

    /**
     * No argument constructor - will use all available keyDefs to upgrade bags.
     */
    public PkQueryIdUpgrader(ObjectStore os) {
        this.source = null;
        this.eof = new BaseEquivalentObjectFetcher(os.getModel(), new IntToIntMap(), os);
    }

    /**
     * Construct with the name of a source - will use defined keys to upgrade bags.
     * @param sourceName name of source
     * @param os the ObjectStore to query
     */
    public PkQueryIdUpgrader(String sourceName, ObjectStore os) {
        this.source = new Source();
        this.source.setName(sourceName);
        this.eof = new BaseEquivalentObjectFetcher(os.getModel(), new IntToIntMap(), os);
    }

    /**
     * For the given object from an old ObjectStore, find the corresponding InterMineObjects in a
     * new ObjectStore.  Primary keys are used to find the objects.
     * @param oldObject the template object
     * @param os ObjectStore used to resolve objects
     * @return the set of new InterMineObjects
     */
    public Set getNewIds(InterMineObject oldObject, ObjectStore os) {
        Query query;
        try {
            query = eof.createPKQuery(oldObject, source, false);
        } catch (MetaDataException e) {
            throw new RuntimeException("Unable to create query for new object", e);
        } catch (IllegalArgumentException e) {
            LOG.error("createPKQuery() failed for old object: " + oldObject.getId()
                      + " with error message: " + e.getMessage());
            return Collections.EMPTY_SET;
        }

        if (query == null) {
            LOG.error("No usable primary key query found for object: " + oldObject);
            return Collections.EMPTY_SET;
        }
        
        SingletonResults results = os.executeSingleton(query);

        // faster just to execute the query immediately:
        results.setNoOptimise();
        results.setNoExplain();

        int size = results.size();

        if (size == 0) {
            LOG.error("createPKQuery() found no results for old object: " + oldObject.getId()
                      + " executed query: " + query);
            return new HashSet();
        } else {
            if (size > 1) {
                LOG.error("createPKQuery() query didn't return 1 result for: "
                          + oldObject.getId() + " (size was " + size + ", values: "
                          + results + ")");
            }

            Set returnSet = new HashSet();

            Iterator iter = results.iterator();

            while (iter.hasNext()) {
                InterMineObject newObject = (InterMineObject) iter.next();

                returnSet.add(newObject.getId());
            }
            return returnSet;
        }
    }
}
