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

import org.flymine.model.fulldata.Item;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.SingletonResults;

/**
 * Provides an interface between a DataTranslator and the source Item ObjectStore which it wishes to
 * read.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreItemReader implements ItemReader
{
    private ObjectStore os;

    /**
     * Constructs a new ObjectStoreItemReader.
     *
     * @param os the ObjectStore
     */
    public ObjectStoreItemReader(ObjectStore os) {
        this.os = os;
    }

    /**
     * @see ItemReader#itemIterator
     */
    public Iterator itemIterator() throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(Item.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        return (new SingletonResults(q, os, os.getSequence())).iterator();
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
}

