package org.flymine.objectstore.ojb;

import junit.framework.Test;

import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.sql.ResultSet;

import org.apache.ojb.broker.accesslayer.ResultSetAndStatement;

import org.flymine.model.testmodel.*;

import org.flymine.objectstore.SetupDataTestCase;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.sql.query.ExplainResult;
import org.flymine.testing.OneTimeTestCase;

public class JdbcAccessFlyMineImplTest extends SetupDataTestCase
{
    private JdbcAccessFlyMineImpl ja;
    private Query q1;
    private PersistenceBrokerFlyMine pb;

    public JdbcAccessFlyMineImplTest(String arg) {
        super(arg);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(JdbcAccessFlyMineImplTest.class);
    }

    public void setUp() throws Exception {
        super.setUp();

        // simple query
        q1 = new Query();
        QueryClass company = new QueryClass(Company.class);
        q1.addFrom(company);
        q1.addToSelect(company);
        QueryField f1 = new QueryField(company, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, new QueryValue("CompanyA"));
        q1.setConstraint(sc1);

        // Get db and writer in order to store data
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        pb = (PersistenceBrokerFlyMine) ((ObjectStoreOjbImpl) os).getPersistenceBroker();

        // clear the cache to ensure that objects are materialised later (in case broker reused)
        ((ObjectStoreWriterOjbImpl) writer).pb.clearCache();

        ja = (JdbcAccessFlyMineImpl) pb.serviceJdbcAccess();
    }

    public static void oneTimeSetUp() throws Exception {
        SetupDataTestCase.oneTimeSetUp();
        setUpResults();
    }

    public static void setUpResults() throws Exception {
        // SubQuery
        ResultsHolder rh1 = new ResultsHolder(2);
        rh1.colNames = new String[] {"a2_", "a3_"};
        rh1.colTypes = new int[] {Types.VARCHAR, Types.INTEGER};
        rh1.rows = 2;
        results.put("SubQuery", rh1);

        // WhereClassClass
        ResultsHolder rh2 = new ResultsHolder(10);
        rh2.colNames = new String[] {"a1_id", "a1_addressid", "a1_ceoid", "a1_name", "a1_vatnumber", "a2_id", "a2_addressid", "a2_ceoid", "a2_name", "a2_vatnumber"};
        rh2.colTypes = new int[] {Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.INTEGER};
        rh2.rows = 2;
        results.put("WhereClassClass", rh2);

        // WhereSimpleEquals
        ResultsHolder rh3 = new ResultsHolder(1);
        rh3.colNames = new String[] {"a2_"};
        rh3.colTypes = new int[] {Types.VARCHAR};
        rh3.rows = 1;
        results.put("WhereSimpleEquals", rh3);
    }

    public void executeTest(String type) throws Exception {
        ResultSetAndStatement retval =
            new ResultSetAndStatement(pb.serviceConnectionManager().getSupportedPlatform());
        retval = ja.executeQuery((Query) queries.get(type), 0, 10000);
        ResultSet rs = retval.m_rs;
        ResultSetMetaData md = retval.m_rs.getMetaData();

        ResultsHolder rh = (ResultsHolder) results.get(type);
        assertEquals(type + " - number of columns: ", rh.columns, md.getColumnCount());
        for(int i=0; i<rh.columns; i++) {
            assertEquals(type + " - name of column " + (i+1) + ":", rh.colNames[i], md.getColumnName(i+1));
            assertEquals(type + " - type of column " + (i+1) + ":", rh.colTypes[i], md.getColumnType(i+1));
        }
        if (rh.rows > 0) {
            rs.last();
            assertEquals(type + " - number of rows: ", rh.rows, rs.getRow());
        }
    }


    public void testReturnNotNull() throws Exception {
        assertNotNull(ja.executeQuery(q1, 0, 10));
    }

    public void testStatementNotNull() throws Exception {
        ResultSetAndStatement retval =
            new ResultSetAndStatement(pb.serviceConnectionManager().getSupportedPlatform());
        retval = ja.executeQuery(q1, 0, 10);
        assertNotNull(retval.m_stmt);
    }

    public void testResultSetNotNull() throws Exception {
        ResultSetAndStatement retval =
            new ResultSetAndStatement(pb.serviceConnectionManager().getSupportedPlatform());
        retval = ja.executeQuery(q1, 0, 10);
        assertNotNull(retval.m_rs);
    }

    public void testResultSetStatement() throws Exception {
        ResultSetAndStatement retval =
            new ResultSetAndStatement(pb.serviceConnectionManager().getSupportedPlatform());
        retval = ja.executeQuery(q1, 0, 10);
        assertEquals(retval.m_stmt, retval.m_rs.getStatement());
    }


    public void testExplainNotNull() throws Exception {
        ExplainResult er = ja.explainQuery(q1, 0, 10);
        if (er == null) {
            fail("a null ExplainResult was returned");
        }
    }


    static class ResultsHolder {
        protected int columns;  // number of columns
        protected String colNames[];
        protected int colTypes[];
        protected int rows = -1;   // number of rows returned by query

        public ResultsHolder() {
        }

        public ResultsHolder(int columns) {
            this.columns = columns;
        }

        public ResultsHolder(int columns, int rows) {
            this.columns = columns;
            this.rows = rows;
        }
    }

}
