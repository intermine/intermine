package org.flymine.objectstore.ojb;

import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.ta.PersistenceBrokerFactoryIF;
import org.apache.ojb.broker.singlevm.PersistenceBrokerImpl;
import org.apache.ojb.broker.metadata.ObjectReferenceDescriptor;
import org.apache.ojb.broker.metadata.ClassDescriptor;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.flymine.objectstore.proxy.LazyInitializer;
import org.flymine.objectstore.proxy.LazyReference;
import org.flymine.sql.query.ExplainResult;
import org.flymine.sql.Database;

import org.flymine.objectstore.query.*;

/**
 * Extension of PersistenceBrokerImpl to allow execution of ObjectStore queries
 *
 * @author Mark Woodbridge
 */
public class PersistenceBrokerFlyMineImpl extends PersistenceBrokerImpl
{
    protected static final org.apache.log4j.Logger LOG = 
        org.apache.log4j.Logger.getLogger(LazyInitializer.class);

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
     * Executes a query with start and limit result indices
     *
     * @param query the ObjectStore query
     * @param start start index
     * @param limit maximum number of rows to return
     * @return a list of ResultsRows
     */
    public List execute(Query query, int start, int limit) {
        List results = new ArrayList();
        Iterator iter = new MultiObjectRsIterator(query, this, start, limit);
        while (iter.hasNext()) { // if iterator is of the right length...
            results.add(iter.next());
        }
        return results;
    }

    /**
     * Runs EXPLAIN on the given query with start and limit result indices
     *
     * @param query the ObjectStore query
     * @param start start index
     * @param limit maximum number of rows to return
     * @return parsed results of the EXPLAIN
     */
    public ExplainResult explain(Query query, int start, int limit) {
        return ((JdbcAccessFlymineImpl) serviceJdbcAccess()).explainQuery(query, start, limit);
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
            Object o = null;
            try {
                o = referencedClass.getDeclaredConstructor(new Class[] {})
                    .newInstance(new Object[] {});
            } catch (Exception e) {
            }
            Query query = new Query();
            QueryClass qc1 = new QueryClass(referencedClass);
            QueryClass qc2 = new QueryClass(obj.getClass());
            query.addToSelect(qc1);
            query.addFrom(qc1);
            query.addFrom(qc2);
            ClassConstraint cc1 = new ClassConstraint(qc2, ClassConstraint.EQUALS, obj);
            String[] s = obj.getClass().getName().split("[.]");
            QueryReference qr = null;
            try {
                qr = new QueryObjectReference(qc2, rds.getAttributeName());
            } catch (Exception e) {
                throw new PersistenceBrokerException(e);
            }
            ContainsConstraint cc2 = 
                new ContainsConstraint(qr, ContainsConstraint.CONTAINS, qc1);
            ConstraintSet cs = new ConstraintSet(ConstraintSet.AND);            
            cs.addConstraint(cc1);
            cs.addConstraint(cc2);
            query.setConstraint(cs);
            //TODO this pkVals stuff is a temporary measure until .equals is sensible
            return (LazyReference) 
                LazyInitializer.getDynamicProxy(referencedClass, query, (Integer) pkVals[0]);
        }
        
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

    /**
     * Sets the database object that this PersistenceBroker object carries around.
     *
     * @param db the Database object
     */
    public void setDatabase(Database db) {
        database = db;
    }

    /**
     * Gets the database object from this PersistenceBroker object.
     *
     * @return the Database object
     */
    public Database getDatabase() {
        return database;
    }
}
