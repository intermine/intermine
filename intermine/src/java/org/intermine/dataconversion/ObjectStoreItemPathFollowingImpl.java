package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.flymine.metadata.CollectionDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.Model;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.fulldata.Attribute;
import org.flymine.model.fulldata.Item;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStorePassthruImpl;
import org.flymine.objectstore.query.BagConstraint;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryCollectionReference;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryNode;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.util.CacheHoldingArrayList;
import org.flymine.util.CacheMap;
import org.flymine.util.TypeUtil;

/**
 * Provides an implementation of an objectstore that fetches additional items which will be required
 * by a DataTranslator, and provides a method to obtain those items from a cache.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreItemPathFollowingImpl extends ObjectStorePassthruImpl
{
    Map descriptiveCache = Collections.synchronizedMap(new CacheMap(
                "ObjectStoreItemPathFollowingImpl DescriptiveCache"));

    /**
     * Creates an instance, from another ObjectStore instance.
     *
     * @param os an ObjectStore object to use
     */
    public ObjectStoreItemPathFollowingImpl(ObjectStore os) {
        super(os);
    }

    /**
     * @see ObjectStore#execute(Query)
     */
    public Results execute(Query q) throws ObjectStoreException {
        return new Results(q, this, getSequence());
    }

    /**
     * @see ObjectStore#execute(Query, int, int, boolean, boolean, int)
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        try {
            List retvalList = os.execute(q, start, limit, optimise, explain, sequence);
            CacheHoldingArrayList retval;
            if (retvalList instanceof CacheHoldingArrayList) {
                retval = (CacheHoldingArrayList) retvalList;
            } else {
                retval = new CacheHoldingArrayList(retvalList);
            }
            if (retval.size() > 1) {
                if ((q.getSelect().size() == 1) && (q.getSelect().get(0) instanceof QueryClass)) {
                    if (Item.class.equals(((QueryClass) q.getSelect().get(0)).getClass())) {
                        fetchRelated(retval);
                    }
                }
            }
            return retval;
        } catch (IllegalAccessException e) {
            throw new ObjectStoreException(e);
        }
    }

    /**
     * This method takes a description of Items to fetch, and returns a List of such Items.
     * The description is a Set of FieldNameAndValue objects.
     *
     * @param description a Set of FieldNameAndValue objects
     * @return a List of Item objects
     * @throws ObjectStoreException if something goes wrong
     */
    public List getItemsByDescription(Set description) throws ObjectStoreException {
        List retval = (List) descriptiveCache.get(description);
        if (retval == null) {
            Query q = new Query();
            QueryClass item = new QueryClass(Item.class);
            q.addFrom(item);
            q.addToSelect(item);
            q.setDistinct(false);
            ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
            Iterator descrIter = description.iterator();
            while (descrIter.hasNext()) {
                FieldNameAndValue f = (FieldNameAndValue) descrIter.next();
                if (f.getFieldName().equals("identifier")) {
                    QueryField qf = new QueryField(item, "identifier");
                    SimpleConstraint c = new SimpleConstraint(qf, ConstraintOp.EQUALS,
                            new QueryValue(f.getValue()));
                    cs.addConstraint(c);
                } else {
                    QueryClass attribute = new QueryClass(Attribute.class);
                    q.addFrom(attribute);
                    QueryCollectionReference r = new QueryCollectionReference(item, "attributes");
                    ContainsConstraint cc = new ContainsConstraint(r, ConstraintOp.CONTAINS,
                            attribute);
                    cs.addConstraint(cc);
                    QueryField qf = new QueryField(attribute, "name");
                    SimpleConstraint c = new SimpleConstraint(qf, ConstraintOp.EQUALS,
                            new QueryValue(f.getFieldName()));
                    cs.addConstraint(c);
                    qf = new QueryField(attribute, "value");
                    c = new SimpleConstraint(qf, ConstraintOp.EQUALS, new QueryValue(f.getValue()));
                    cs.addConstraint(c);
                }
            }
            retval = os.execute(q);
        }
        return retval;
    }

    /**
     * This method fetches the related Items to the given List of items, and places them in the
     * cache and in the holder part of the given list.
     *
     * @param batch the List of items
     * @throws IllegalAccessException sometimes
     * @throws ObjectStoreException not often
     */
    private void fetchRelated(CacheHoldingArrayList batch) throws IllegalAccessException,
    ObjectStoreException {
    }
}
