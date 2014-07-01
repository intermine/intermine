package org.intermine.bio.dataconversion;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import com.mockrunner.jdbc.StatementResultSetHandler;
import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockResultSet;

/**
 * OntologyIdResolverFactory Unit Tests
 *
 * @author Fengyuan Hu
 *
 */
public class OntologyIdResolverFactoryTest extends TestCase {

    OntologyIdResolverFactory factory;
    List<String> resultHeader;
    List<List<Object>> dataList = new ArrayList<List<Object>>();
    ResultSet res;


    public OntologyIdResolverFactoryTest() {
    }

    public OntologyIdResolverFactoryTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        factory = new OntologyIdResolverFactory("gene");
        IdResolverFactory.resolver = new IdResolver("gene");

        // prepare mocked results
        resultHeader = new ArrayList<String>(Arrays.asList("identifier",
                                                            "name"));

        List<Object> line1 = new ArrayList<Object>();

        line1.add("gene_GO");
        line1.add("gene_GO_old_1");

        dataList.add(line1);

        List<Object> line2 = new ArrayList<Object>();

        line2.add("gene_GO");
        line2.add("gene_GO_old_2");

        dataList.add(line2);

        res = mockResultSet(resultHeader, dataList);
    }

    /**
    *
    * @param headers a list of strings
    * @param data a list of list of objects
    * @return ResultSet mocked ResultSet
    * @throws Exception
    */
   public ResultSet mockResultSet(List<String> headers, List<List<Object>> data) throws Exception {

       // empty set (not perfectly represent the case though)
       if (headers == null || data == null) {
           MockConnection connection = new MockConnection();
           StatementResultSetHandler statementHandler = connection
                   .getStatementResultSetHandler();
           MockResultSet result = statementHandler.createResultSet();
           statementHandler.prepareGlobalResultSet(result);
           connection.close();
           return result;
       }

       // validation
       if (headers.size() != data.get(0).size()) {
           throw new Exception("column sizes are not equal");
       }

       // create a mock result set
       MockResultSet mockResultSet = new MockResultSet("aMockedResultSet");

       // add header
       for (String string : headers) {
           mockResultSet.addColumn(string);
       }

       // add data
       for (List<Object> list : data) {
           mockResultSet.addRow(list);
       }

       return mockResultSet;
   }

    public void testAddIdsFromResultSet() throws Exception {
        int count = factory.addIdsFromResultSet(res);
        assertEquals(2, count);
        assertEquals(1, IdResolverFactory.resolver.getTaxons().size());
        assertEquals("0", IdResolverFactory.resolver.getTaxons().iterator().next());
        assertEquals(1, IdResolverFactory.resolver.getClassNames().size());
        assertEquals("gene", IdResolverFactory.resolver.getClassNames().iterator().next());
        assertEquals("gene_GO", IdResolverFactory.resolver.resolveId("0", "gene", "gene_GO_old_2").iterator().next());
    }
}
