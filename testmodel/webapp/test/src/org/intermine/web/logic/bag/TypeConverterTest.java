package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.web.logic.template.TemplateQueryBinding;

import servletunit.struts.MockStrutsTestCase;

/**
 * @author Matthew Wakeling
 */
public class TypeConverterTest extends MockStrutsTestCase
{
    ServletContext context;
    List<TemplateQuery> conversionTemplates;
    
    public TypeConverterTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        super.setUp();
        String template = "<template name=\"convertEmployeesToAddresses\" title=\"Convert employees to addresses\" longDescription=\"\" comment=\"\" important=\"false\">\n" + 
                "      <query name=\"convertEmployeesToAddresses\" model=\"testmodel\" view=\"Employee.id Employee.address.id\">\n" + 
                "        <node path=\"Employee\" type=\"Employee\"/>\n" + 
                "        <node path=\"Employee.id\" type=\"Integer\">\n" + 
                "          <constraint op=\"=\" value=\"0\" description=\"id\" identifier=\"\" editable=\"true\" code=\"A\"/>\n" + 
                "        </node>\n" + 
                "        <node path=\"Employee.address.id\" type=\"Integer\"/>\n" + 
                "      </query>\n" + 
                "    </template>";
        context = getActionServlet().getServletContext();
        TemplateQueryBinding tqb = new TemplateQueryBinding();
        Map tqs = tqb.unmarshal(new StringReader(template), null, SessionMethods.getClassKeys(context));
        TemplateQuery tq = (TemplateQuery) tqs.get("convertEmployeesToAddresses");
        conversionTemplates = new ArrayList(Collections.singleton(tq));
    }
    
    public void tearDown() throws Exception {
         getActionServlet().destroy();
    }

    public void testGetConvertedObjectMap() throws Exception {
        ObjectStore os = (ObjectStore) context.getAttribute(Constants.OBJECTSTORE);
        ObjectStoreWriter uosw = ((ProfileManager) context.getAttribute(Constants.PROFILE_MANAGER)).getUserProfileObjectStore();

        Results r = getEmployeesAndAddresses();
        assertEquals("Results: " + r, 2, r.size());
        ObjectStoreWriter osw = new ObjectStoreWriterInterMineImpl(os);
        InterMineBag imb = new InterMineBag("Fred", "Employee", "Test bag", new Date(), os, null, uosw);
        ObjectStoreBag osb = imb.getOsb();
        osw.addToBag(osb, ((Employee) ((List) r.get(0)).get(0)).getId());
        osw.addToBag(osb, ((Employee) ((List) r.get(1)).get(0)).getId());
        Map expected = new HashMap();
        expected.put(((List) r.get(0)).get(0), Collections.singletonList(((List) r.get(0)).get(1)));
        expected.put(((List) r.get(1)).get(0), Collections.singletonList(((List) r.get(1)).get(1)));

        List<TemplateQuery> convTemplates = BagQueryRunner.getConversionTemplates(context);
        Map got = TypeConverter.getConvertedObjectMap(context, convTemplates, Employee.class, Address.class, imb);

        assertEquals(expected, got);
    }
    
    public void testGetConvertedObjects() throws Exception {
        ObjectStore os = (ObjectStore) context.getAttribute(Constants.OBJECTSTORE);
        ObjectStoreWriter uosw = ((ProfileManager) context.getAttribute(Constants.PROFILE_MANAGER)).getUserProfileObjectStore();
        
        Results r = getEmployeesAndAddresses();
        assertEquals("Results: " + r, 2, r.size());
        ObjectStoreWriter osw = new ObjectStoreWriterInterMineImpl(os);
        InterMineBag imb = new InterMineBag("Fred", "Employee", "Test bag", new Date(), os, null, uosw);
        ObjectStoreBag osb = imb.getOsb();
        osw.addToBag(osb, ((Employee) ((List) r.get(0)).get(0)).getId());
        osw.addToBag(osb, ((Employee) ((List) r.get(1)).get(0)).getId());
        List expected = new ArrayList();
        expected.add(((List) r.get(0)).get(1));
        expected.add(((List) r.get(1)).get(1));

        List<TemplateQuery> convTemplates = BagQueryRunner.getConversionTemplates(context);
        WebResults got = TypeConverter.getConvertedObjects(getSession(), context, convTemplates, Employee.class, Address.class, imb);

        assertEquals(expected, got);
    }
    
    
    private Results getEmployeesAndAddresses() throws Exception {
        ObjectStore os = (ObjectStore) context.getAttribute(Constants.OBJECTSTORE);
        List names = Arrays.asList(new String[] {"EmployeeA3", "EmployeeB2"});
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        QueryClass qc2 = new QueryClass(Address.class);
        q.addFrom(qc1);
        q.addToSelect(qc1);
        q.addFrom(qc2);
        q.addToSelect(qc2);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(cs);
        cs.addConstraint(new BagConstraint(new QueryField(qc1, "name"), ConstraintOp.IN, names));
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc1, "address"),
                    ConstraintOp.CONTAINS, qc2));
        return os.execute(q);
    }
    
    public void testGetConversionMapQuery() throws Exception {
        String bag = "bag";
        PathQuery pq = TypeConverter.getConversionMapQuery(conversionTemplates, Employee.class, Address.class, bag);
        assertEquals(1, pq.getAllConstraints().size());
        Constraint expected = new Constraint(ConstraintOp.IN, bag, false, "", "A", null, null);
        assertEquals(expected, pq.getAllConstraints().get(0));
    }
    
    public void testGetConversionQuery() throws Exception {
        String bag = "bag";
        PathQuery pq = TypeConverter.getConversionQuery(conversionTemplates, Employee.class, Address.class, bag);
        assertEquals(1, pq.getAllConstraints().size());
        Constraint expected = new Constraint(ConstraintOp.IN, bag, false, "", "A", null, null);
        assertEquals(expected, pq.getAllConstraints().get(0));
        List expectedView = new ArrayList(Collections.singleton("Employee.address.id"));
        assertEquals(expectedView, pq.getViewStrings());
    }
}
