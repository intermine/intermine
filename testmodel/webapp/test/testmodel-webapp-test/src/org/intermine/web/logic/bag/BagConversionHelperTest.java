package org.intermine.web.logic.bag;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.results.ResultElement;
import org.intermine.api.results.WebResults;
import org.intermine.api.results.flatouterjoins.MultiRow;
import org.intermine.api.results.flatouterjoins.MultiRowValue;
import org.intermine.api.template.ApiTemplate;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.template.TemplateQuery;
import org.intermine.template.xml.TemplateQueryBinding;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;

import servletunit.struts.MockStrutsTestCase;

public class BagConversionHelperTest extends MockStrutsTestCase {

    ServletContext context;
    ObjectStoreWriter uosw = null;
    List<ApiTemplate> conversionTemplates;
    Profile profile;
    HttpSession session;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final String template = "<template name=\"convertEmployeesToAddresses\" title=\"Convert employees to addresses\" longDescription=\"\" comment=\"\" >\n" +
                "      <query name=\"convertEmployeesToAddresses\" model=\"testmodel\" view=\"Employee.id Employee.address.id\">\n" +
                "        <node path=\"Employee\" type=\"Employee\"/>\n" +
                "        <node path=\"Employee.id\" type=\"Integer\">\n" +
                "          <constraint op=\"=\" value=\"0\" description=\"id\" identifier=\"\" editable=\"true\" code=\"A\"/>\n" +
                "        </node>\n" +
                "        <node path=\"Employee.address.id\" type=\"Integer\"/>\n" +
                "      </query>\n" +
                "    </template>";
        uosw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        context = getActionServlet().getServletContext();
        final TemplateQueryBinding tqb = new TemplateQueryBinding();
        final Map<String, TemplateQuery> tqs = tqb.unmarshalTemplates(new StringReader(template), 1);
        final ApiTemplate tq = new ApiTemplate(tqs.get("convertEmployeesToAddresses"));
        conversionTemplates = new ArrayList<ApiTemplate>(Collections.singleton(tq));
        final ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        final ProfileManager profileManager = new ProfileManager(os, uosw);
        profile = new Profile(profileManager, "test", new Integer(101), "testpass",
                new HashMap(), new HashMap(), new HashMap(), true, false);
        session = getSession();
        session.setAttribute(Constants.PROFILE, profile);
    }

    @Override
    public void tearDown() throws Exception {
        uosw.close();
    }

    // this calls getConvertedObjects with a list of Employees and gets back converted Addresses
    public void testGetConvertedObjects() throws Exception {
        final InterMineAPI im = SessionMethods.getInterMineAPI(context);
        final ObjectStore os = im.getObjectStore();

        final Results r = getEmployeesAndAddresses();

        assertEquals("Results: " + r, 2, r.size());
        final InterMineBag imb = new InterMineBag("Fred", "Employee", "Test bag", new Date(), BagState.CURRENT, os, null, uosw, null);
        imb.addIdToBag(((Employee) ((List) r.get(0)).get(0)).getId(), "Employee");
        imb.addIdToBag(((Employee) ((List) r.get(1)).get(0)).getId(), "Employee");
        profile.saveBag("Fred", imb);
        final List expected = new ArrayList();
        expected.add(((List) r.get(0)).get(1));
        expected.add(((List) r.get(1)).get(1));

        final WebResults results = BagConversionHelper.getConvertedObjects(getSession(), conversionTemplates, Employee.class, Address.class, imb);
        final List got = new ArrayList();
        for (final MultiRow<ResultsRow<MultiRowValue<ResultElement>>> mr : results) {
            got.add(mr.get(0).get(0).getValue().getObject());
        }
        assertEquals(expected, got);
    }

    private Results getEmployeesAndAddresses() throws Exception {
        final InterMineAPI im = SessionMethods.getInterMineAPI(context);
        final ObjectStore os = im.getObjectStore();
        final List names = Arrays.asList(new String[] {"EmployeeA3", "EmployeeB2"});
        final Query q = new Query();
        final QueryClass qc1 = new QueryClass(Employee.class);
        final QueryClass qc2 = new QueryClass(Address.class);
        q.addFrom(qc1);
        q.addToSelect(qc1);
        q.addFrom(qc2);
        q.addToSelect(qc2);
        final ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(cs);
        cs.addConstraint(new BagConstraint(new QueryField(qc1, "name"), ConstraintOp.IN, names));
        cs.addConstraint(new ContainsConstraint(new QueryObjectReference(qc1, "address"),
                    ConstraintOp.CONTAINS, qc2));
        return os.execute(q);
    }

}
