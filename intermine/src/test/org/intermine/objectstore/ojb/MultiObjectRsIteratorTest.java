package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import com.mockobjects.sql.MockSingleRowResultSet;
import com.mockobjects.sql.MockResultSetMetaData;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.PBKey;

public class MultiObjectRsIteratorTest extends TestCase
{
    public MultiObjectRsIteratorTest(String arg) {
        super(arg);
    }

    private MockSingleRowResultSet mrs;
    private MockResultSetMetaData mrsmd;

    public void setUp() throws Exception {
        mrs = new MockSingleRowResultSet();
        mrsmd = new MockResultSetMetaData();

        mrsmd.setupAddColumnNames(new String[] {"a1field1", "a1field2", "a2field1", "a3field1"});
        mrsmd.setupAddColumnTypes(new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});
        mrsmd.setupGetColumnCount(4);
        mrs.setupMetaData(mrsmd);

        mrs.addExpectedIndexedValues(new Object[] { "test1", "test2", "test3", "test4" });
    }


    public void testGetUnalisedColumns1() throws Exception {

        MultiObjectRsIterator rsiterator = new MultiObjectRsIterator();

        ResultSet rs = rsiterator.getUnaliasedColumns(mrs, "a1");
        assertEquals("test1", rs.getString("field1"));
        assertEquals("test2", rs.getString("field2"));
    }

    public void testGetUnalisedColumns2() throws Exception {

        MultiObjectRsIterator rsiterator = new MultiObjectRsIterator();

        ResultSet rs = rsiterator.getUnaliasedColumns(mrs, "a2");
        assertEquals("test3", rs.getString("field1"));
    }

    public void testGetUnalisedColumns3() throws Exception {

        MultiObjectRsIterator rsiterator = new MultiObjectRsIterator();

        ResultSet rs = rsiterator.getUnaliasedColumns(mrs, "a3");
        assertEquals("test4", rs.getString("field1"));
    }
}
