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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.flymine.model.fulldata.Item;
import org.flymine.model.fulldata.Attribute;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.QueryReference;
import org.flymine.objectstore.query.QueryCollectionReference;
import org.flymine.objectstore.query.SingletonResults;
import org.apache.log4j.Logger;

/**
 * Provides an interface between a DataTranslator and the source Item ObjectStore which it wishes to
 * read.
 *
 * @author Matthew Wakeling
 * @author Richard Smith
 */
public class ObjectStoreItemReader implements ItemReader
{
    private ObjectStoreItemPathFollowingImpl os;
    protected static final Logger LOG = Logger.getLogger(ObjectStoreItemReader.class);

    /**
     * Constructs a new ObjectStoreItemReader.
     *
     * @param os the ObjectStore
     */
    public ObjectStoreItemReader(ObjectStore os) {
        this.os = new ObjectStoreItemPathFollowingImpl(os);
    }

    /**
     * @see ItemReader#itemIterator
     */
    public Iterator itemIterator() throws ObjectStoreException {
        Query q = new Query();
        // database has a hard time selecting distinct on object xml
        q.setDistinct(false);
        QueryClass qc = new QueryClass(Item.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults sr = new SingletonResults(q, os, os.getSequence());
        sr.setBatchSize(1000);
        return sr.iterator();
    }

    /**
     * @see ItemReader#getItemById
     */
    public Item getItemById(String objectId) throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(Item.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        QueryField f = new QueryField(qc, "identifier");
        QueryValue v = new QueryValue(objectId);
        Constraint c = new SimpleConstraint(f, ConstraintOp.EQUALS, v);
        q.setConstraint(c);
        List results = new SingletonResults(q, os, os.getSequence());
        if (results.size() > 1) {
            throw new IllegalStateException("Multiple Items in the datatracker with that objectId");
        } else if (results.size() == 1) {
            return (Item) results.get(0);
        }
        return null;
    }

    /**
     * @see ItemReader#getItemsByAttributeValue
     */
    public Iterator getItemsByAttributeValue(String className, String attName, String value) {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Item.class);
        q.addFrom(qc1);
        q.addToSelect(qc1);
        QueryClass qc2 = new QueryClass(Attribute.class);
        q.addFrom(qc2);
        ConstraintSet c = new ConstraintSet(ConstraintOp.AND);
        QueryField f1 = new QueryField(qc2, "name");
        QueryValue v1 = new QueryValue(attName);
        Constraint c1 = new SimpleConstraint(f1, ConstraintOp.EQUALS, v1);
        c.addConstraint(c1);
        QueryField f2 = new QueryField(qc2, "value");
        QueryValue v2 = new QueryValue(value);
        Constraint c2 = new SimpleConstraint(f2, ConstraintOp.EQUALS, v2);
        c.addConstraint(c2);
        QueryReference ref = new QueryCollectionReference(qc1, "attributes");
        Constraint c3 = new ContainsConstraint(ref, ConstraintOp.CONTAINS, qc2);
        c.addConstraint(c3);
        q.setConstraint(c);
        LOG.error(q.toString());
        SingletonResults sr = new SingletonResults(q, os, os.getSequence());
        return sr.iterator();
    }

    /**
     * @see ItemReader#getItemsByDescription
     */
    public List getItemsByDescription(Set constraints) throws ObjectStoreException {
        return os.getItemsByDescription(constraints);
    }
}
