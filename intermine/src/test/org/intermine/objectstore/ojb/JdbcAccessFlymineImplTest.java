package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.sql.ResultSet;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.accesslayer.JdbcAccess;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.accesslayer.ResultSetAndStatement;

import org.flymine.model.testmodel.Company;
import org.flymine.model.testmodel.Employee;
import org.flymine.model.testmodel.Address;

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.QueryObjectReference;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.ojb.JdbcAccessFlymineImpl;
import org.flymine.objectstore.ojb.ObjectStoreOjbImpl;
import org.flymine.sql.DatabaseFactory;
import org.flymine.objectstore.ObjectStore;
import org.flymine.sql.Database;


public class JdbcAccessFlymineImplTest extends QueryTestCase
{

    private PersistenceBroker broker;
    private JdbcAccessFlymineImpl ja;
    private ObjectStore os;
    private Query q1, q2;

    public JdbcAccessFlymineImplTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        Database db = DatabaseFactory.getDatabase("db.unittest");
        ObjectStoreOjbImpl os = ObjectStoreOjbImpl.getInstance(db);
        broker = os.getPersistenceBroker();

        broker = ObjectStoreOjbImpl.getInstance(db).getPersistenceBroker();
        ja = (JdbcAccessFlymineImpl) broker.serviceJdbcAccess();

        // simple query
        q1 = new Query();
        QueryClass company = new QueryClass(Company.class);
        q1.addFrom(company);
        q1.addToSelect(company);
        QueryField f1 = new QueryField(company, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, new QueryValue("CompanyA"));
        q1.setConstraint(sc1);

    }


    public void setUpResults() throws Exception {
        // SubQuery
        ResultsHolder rh1 = new ResultsHolder(2);
        rh1.colNames = new String[] {"a2_", "a3_"};
        rh1.colTypes = new int[] {Types.VARCHAR, Types.INTEGER};
        //rh1.rows = 10;  query doesn't yet return any data...
        results.put("SubQuery", rh1);

        // WhereClassClass
        ResultsHolder rh2 = new ResultsHolder(8);
        rh2.colNames = new String[] {"a1_id", "a1_addressid", "a1_name", "a1_vatnumber", "a2_id", "a2_addressid", "a2_name", "a2_vatnumber"};
        rh2.colTypes = new int[] {Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.INTEGER};
        results.put("WhereClassClass", rh2);
    }

    public void executeTest(String type) throws Exception {
        ResultSetAndStatement retval =
            new ResultSetAndStatement(broker.serviceConnectionManager().getSupportedPlatform());
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
            new ResultSetAndStatement(broker.serviceConnectionManager().getSupportedPlatform());
        retval = ja.executeQuery(q1, 0, 10);
        assertNotNull(retval.m_stmt);
    }

    public void testResultSetNotNull() throws Exception {
        ResultSetAndStatement retval =
            new ResultSetAndStatement(broker.serviceConnectionManager().getSupportedPlatform());
        retval = ja.executeQuery(q1, 0, 10);
        assertNotNull(retval.m_rs);
    }

    public void testResultSetStatement() throws Exception {
        ResultSetAndStatement retval =
            new ResultSetAndStatement(broker.serviceConnectionManager().getSupportedPlatform());
        retval = ja.executeQuery(q1, 0, 10);
        assertEquals(retval.m_stmt, retval.m_rs.getStatement());
    }


    /**** test values when data is put in db
    public void testSimpleQueryValues() throws Exception {
        ResultSetAndStatement retval =
            new ResultSetAndStatement(broker.serviceConnectionManager().getSupportedPlatform());
        retval = ja.executeQuery(q1, 0, 10);
        retval.m_rs.next();

        assertEquals(retval.m_rs.getString("a1_name"), "CompanyA");
        assertEquals(retval.m_rs.getInt("a1_vatNumber"), 1234);

        if (retval.m_rs.next()) {
            fail("Expected only one row in ResultSet");
        }
    }
    ****/


    class ResultsHolder {
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
