package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.PersistenceBroker;

import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.objectstore.query.*;
import org.flymine.model.testmodel.*;

public abstract class QueryTestCase extends TestCase
{
    protected Map queries;
    protected Map results;

    private DescriptorRepository dr;

    /**
     * Constructor
     */
    public QueryTestCase(String arg) {
        super(arg);
    }

    /**
     * Set up the test
     *
     * @throws Exception if an error occurs
     */
    public void setUp() throws Exception {
        super.setUp();

        Database db = DatabaseFactory.getDatabase("db.unittest");
        ObjectStoreOjbImpl os = ObjectStoreOjbImpl.getInstance(db);
        PersistenceBroker broker = os.getPersistenceBroker();
        dr = broker.getDescriptorRepository();
        queries = new HashMap();
        results = new HashMap();
        setUpQueries();
        setUpResults();
    }

    /**
     * Set up the set of queries we are testing
     *
     * @throws Exception if an error occurs
     */
    public void setUpQueries() throws Exception {
        queries.put("SubQuery", subQuery());
        queries.put("WhereSimpleEquals", whereSimpleEquals());
        queries.put("WhereSimpleNotEquals", whereSimpleNotEquals());
        queries.put("WhereSimpleLike", whereSimpleLike());
        queries.put("WhereEqualsString", whereEqualString());
        queries.put("WhereAndSet", whereAndSet());
        queries.put("WhereOrSet", whereOrSet());
        queries.put("WhereNotSet", whereNotSet());
        queries.put("WhereSubQueryField", whereSubQueryField());
        queries.put("WhereSubQueryClass", whereSubQueryClass());
        queries.put("WhereNotSubQueryClass", whereNotSubQueryClass());
        queries.put("WhereNegSubQueryClass", whereNegSubQueryClass());
        queries.put("WhereClassClass", whereClassClass());
        queries.put("WhereNotClassClass", whereNotClassClass());
        queries.put("WhereNegClassClass", whereNegClassClass());
        queries.put("WhereClassObject", whereClassObject());
    }

    /**
     * Set up any data needed
     *
     * @throws Exception if an error occurs
     */
    public void setUpData() throws Exception {

    }

    /**
     * Set up all the results expected for a given subset of queries
     *
     * @throws Exception if an error occurs
     */
    public abstract void setUpResults() throws Exception;

    /**
     * Execute a test for a query. This should run the query and
     * contain an assert call to assert that the returned results are
     * thos expected.
     *
     * @param type the type of query we are testing (ie. the key in the queries Map)
     * @throws Exception if type does not appear in the queries map
     */
    public abstract void executeTest(String type) throws Exception;

    /**
     * Test the queries produce the appropriate result
     *
     * @throws Exception if an error occurs
     */
    public void testQueries() throws Exception {
        Iterator i = results.keySet().iterator();
        while (i.hasNext()) {
            String type = (String) i.next();
            // Does this appear in the queries map;
            if (!(queries.containsKey(type))) {
                throw new Exception(type + " does not appear in the queries map");
            }
            executeTest(type);
        }

    }


    public Query subQuery() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(5));
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        q1.addToSelect(v1);
        Query q2 = new Query();
        q2.addFrom(q1);
        QueryField f1 = new QueryField(q1, c1, "name");
        QueryField f2 = new QueryField(q1, v1);
        q2.addToSelect(f1);
        q2.addToSelect(f2);
        return q2;
    }

    public Query whereSimpleEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryField f1 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        return q1;
    }

    public Query whereSimpleNotEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryField f1 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.NOT_EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        return q1;
    }

    public Query whereSimpleLike() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("flibble");
        QueryField f1 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        return q1;
    }

    public Query whereEqualString() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("flibble");
        QueryField f1 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        return q1;
    }

    public Query whereAndSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("flibble");
        QueryValue v2 = new QueryValue(new Integer(5));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, SimpleConstraint.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        return q1;
    }

    public Query whereOrSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("flibble");
        QueryValue v2 = new QueryValue(new Integer(5));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, SimpleConstraint.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.OR);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        return q1;
    }

    public Query whereNotSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("flibble");
        QueryValue v2 = new QueryValue(new Integer(5));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, SimpleConstraint.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        cs1.setNegated(true);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        return q1;
    }

    public Query whereSubQueryField() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryField f1 = new QueryField(c1, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        QueryClass c2 = new QueryClass(Department.class);
        QueryField f2 = new QueryField(c2, "name");
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, f2);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    public Query whereSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryClass c2 = new QueryClass(Department.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, c2);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    public Query whereNotSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryClass c2 = new QueryClass(Department.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.DOES_NOT_CONTAIN, c2);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    public Query whereNegSubQueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryClass c2 = new QueryClass(Department.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, c2);
        sqc1.setNegated(true);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        return q2;
    }

    public Query whereClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, qc2);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    public Query whereNotClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.NOT_EQUALS, qc2);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    public Query whereNegClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, qc2);
        cc1.setNegated(true);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        return q1;
    }

    public Query whereClassObject() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        Company obj = new Company();
        ClassDescriptor cld = dr.getDescriptorFor(Company.class);
        FieldDescriptor fld = cld.getFieldDescriptorByName("id");
        fld.getPersistentField().set(obj, new Integer(2345));
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, obj);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        q1.setConstraint(cc1);
        return q1;
    }


}
