package org.flymine.objectstore.ojb;

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
import java.util.ArrayList;
import java.util.Collection;

import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.util.collections.ManageableHashSet;
import org.apache.ojb.broker.util.collections.ManageableArrayList;
import org.apache.ojb.broker.ta.PersistenceBrokerFactoryIF;
import org.apache.ojb.broker.singlevm.PersistenceBrokerImpl;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;

import org.flymine.objectstore.query.*;
import org.flymine.objectstore.proxy.LazyCollection;
import org.flymine.objectstore.proxy.LazyInitializer;
import org.flymine.objectstore.proxy.LazyReference;
import org.flymine.sql.Database;
import org.flymine.sql.query.ExplainResult;

/**
 * Extension of PersistenceBrokerImpl to implement PersistenceBrokerFlyMine
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class PersistenceBrokerFlyMineImpl extends PersistenceBrokerImpl
    implements PersistenceBrokerFlyMine
{
    //protected static final org.apache.log4j.Logger LOG =
    //org.apache.log4j.Logger.getLogger(PersistenceBrokerFlyMineImpl.class);

    private Database database;

    /**
     * No argument constructor for testing purposes
     *
     */
    public PersistenceBrokerFlyMineImpl() {
    }

    /**
     * @see PersistenceBrokerImpl#PersistenceBrokerImpl
     */
    public PersistenceBrokerFlyMineImpl(PBKey key, PersistenceBrokerFactoryIF pbf)
    {
        super(key, pbf);
    }

    /**
     * @see PersistenceBrokerFlyMine#execute
     */
    public List execute(Query query, int start, int limit, boolean optimise) {
        List results = new ArrayList();
        Iterator iter = new MultiObjectRsIterator(query, this, start, limit, optimise);
        while (iter.hasNext()) { // if iterator is of the right length...
            results.add(iter.next());
        }
        return results;
    }

    /**
     *  @see PersistenceBrokerFlyMine#explain
     */
    public ExplainResult explain(Query query, int start, int limit, boolean optimise) {
        return ((JdbcAccessFlyMineImpl) serviceJdbcAccess()).explainQuery(query, start, limit,
                optimise);
    }

    /**
     * @see PersistenceBrokerFlyMine#count
     */
    public int count(Query query) {
        return ((JdbcAccessFlyMineImpl) serviceJdbcAccess()).countQuery(query);
    }

    /**
     * Generate a flymine Results object to act as proxy to a collection.  The Results object
     * pages the contents of the collection.
     * Build an org.flymine.objectstore.query that will return all elements of the collection
     *
     * The Collection is retrieved only if <b>cascade.retrieve is true</b>
     * or if <b>forced</b> is set to true.     *
     *
     * @param obj - the object to be updated
     * @param cld - the ClassDescriptor describing obj
     * @param cod - the CollectionDescriptor describing the collection attribute to be loaded
     * @param forced - if set to true loading is forced, even if cds differs.
     *
     */
    protected void retrieveCollection(Object obj, ClassDescriptor cld, CollectionDescriptor cod,
                                      boolean forced) {

        if (forced || cod.getCascadeRetrieve()) {

            PersistentField collectionField = cod.getPersistentField();
            Query flymineQuery = null;
            org.apache.ojb.broker.query.Query ojbQuery = null;

            if (cod.isLazy()) {
                // if lazy then replace collection with a LazyCollection

                flymineQuery = getCollectionQuery(obj, cld, cod);
                Collection lazyCol = new LazyCollection(flymineQuery);
                collectionField.set(obj, lazyCol);
            } else {
                // run an ojb query to materialize the entire contents of the collection
                Class collectionClass = cod.getCollectionClass();

                ojbQuery = getFKQuery1toN(obj, cld, cod);
                if (collectionClass == null) {
                    // TODO could be an array?
                    // allow OJB to use what it chooses (currently ManageableVector)
                    Collection result = getCollectionByQuery(ojbQuery, cod.isLazy());
                    collectionField.set(obj, result);
                } else if (List.class.isAssignableFrom(collectionClass)) {
                    ManageableCollection result
                        = getCollectionByQuery(ManageableArrayList.class,
                                               cod.getItemClass(), ojbQuery);
                    collectionField.set(obj, result);
                } else if (Set.class.isAssignableFrom(collectionClass)) {
                    ManageableCollection result
                        = getCollectionByQuery(ManageableHashSet.class,
                                               cod.getItemClass(), ojbQuery);
                    collectionField.set(obj, result);
                } else {
                    throw new PersistenceBrokerException("Unrecognised collection type: "
                                                         + collectionClass);
                }
            }
        }
        // else do nothing, collection fields left as null
    }


    /**
     * Build a flymine query to return the contents of a given collection using ojb metadata.
     *
     * @param thisObj the materialized object which has this collection
     * @param thisCld ClassDescriptor for the materialized object
     * @param cod describes the collection
     * @return a flymine query to populate a collection
     */
    protected Query getCollectionQuery(Object thisObj, ClassDescriptor thisCld,
                                       CollectionDescriptor cod) {
        // TODO: handle ordering on collections?
        try {
            Query query = new Query();
            QueryClass qcThis = new QueryClass(thisCld.getClassOfObject());
            QueryClass qcCol = new QueryClass(cod.getItemClass());
            ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);
            query.addToSelect(qcCol);
            query.addFrom(qcCol).addFrom(qcThis);

            // constrain that qcThis describes the materialized object
            ClassConstraint cc1 = new ClassConstraint(qcThis, ConstraintOp.EQUALS, thisObj);
            constraints.addConstraint(cc1);

            // constrain that this.collection <of items> contains item
            QueryCollectionReference qr
                = new QueryCollectionReference(qcThis, cod.getAttributeName());
            ContainsConstraint cc2 =
                new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcCol);
            constraints.addConstraint(cc2);

            query.setConstraint(constraints);
            return query;

        } catch (NoSuchFieldException e) {
            throw (new PersistenceBrokerException("failed to build collection query: "
                                                  + e.getMessage()));
        }
    }

    /**
     * Override getReferencedObject to use dynamically generated proxies
     *
     * @param obj the object containing the reference
     * @param rds the descriptor of the reference
     * @param cld the descriptor of the referenced object
     * @return the referenced object
     */
    protected Object getReferencedObject(Object obj, ObjectReferenceDescriptor rds,
                                         ClassDescriptor cld) {
        Class referencedClass = rds.getItemClass();
        Object[] pkVals = rds.getForeignKeyValues(obj, cld);
        boolean allPkNull = true;

        for (int i = 0; i < pkVals.length; i++) {
            if (pkVals[i] != null) {
                allPkNull = false;
                break;
            }
        }

        if (allPkNull) {
            return null;
        }

        if (rds.isLazy()) {
            try {
                Query query = new Query();
                QueryClass qcThis = new QueryClass(obj.getClass());
                QueryClass qcRef = new QueryClass(referencedClass);
                ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);
                query.addToSelect(qcRef);
                query.addFrom(qcRef).addFrom(qcThis);

                // constrain that qcThis describes the materialized object
                ClassConstraint cc1 = new ClassConstraint(qcThis, ConstraintOp.EQUALS, obj);
                constraints.addConstraint(cc1);

                // constrain that this.reference <to item> is item
                QueryReference qr = new QueryObjectReference(qcThis, rds.getAttributeName());
                ContainsConstraint cc2 =
                    new ContainsConstraint(qr, ConstraintOp.CONTAINS, qcRef);
                constraints.addConstraint(cc2);

                query.setConstraint(constraints);

                return (LazyReference)
                    LazyInitializer.getDynamicProxy(referencedClass, query, (Integer) pkVals[0]);
            } catch (NoSuchFieldException e) {
                throw new PersistenceBrokerException(e);
            }
        } else {
            Class referencedProxy = rds.getItemProxyClass();
            if (referencedProxy != null) {
                try {
                    return referencedProxy.getDeclaredConstructor(new Class[] {})
                        .newInstance(new Object[] {});
                } catch (Exception e) {
                    throw new PersistenceBrokerException(e);
                }
            } else {
                return getObjectByIdentity(new Identity(referencedClass, pkVals));
            }
        }
    }

    /**
     * @see PersistenceBrokerFlyMine#setDatabase
     */
    public void setDatabase(Database db) {
        database = db;
    }

    /**
     * @see PersistenceBrokerFlyMine#getDatabase
     */
    public Database getDatabase() {
        return database;
    }
}
