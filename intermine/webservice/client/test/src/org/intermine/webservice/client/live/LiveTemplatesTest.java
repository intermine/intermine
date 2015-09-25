package org.intermine.webservice.client.live;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.pathquery.Constraints;
import org.intermine.template.TemplateQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.results.Page;
import org.intermine.webservice.client.services.TemplateService;
import org.intermine.webservice.client.template.TemplateParameter;
import org.intermine.webservice.client.util.TestUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class LiveTemplatesTest {

    private static TemplateService authorised =
            new ServiceFactory(TestUtil.getRootUrl(), TestUtil.getToken()).getTemplateService();
    private static TemplateService unauthorised =
            new ServiceFactory(TestUtil.getRootUrl()).getTemplateService();
    private static final Page subset = new Page(1, 2);

    @Test
    public void templateNames() {
        Set<String> authNames = authorised.getTemplateNames();
        Set<String> unAuthNames = unauthorised.getTemplateNames();

        assertTrue(authNames.size() > unAuthNames.size());

        assertTrue(unAuthNames + " should contain ManagerLookup", unAuthNames.contains("ManagerLookup"));
        assertFalse(unAuthNames + " should not contain private-template-1", unAuthNames.contains("private-template-1"));

        assertTrue(authNames + " should contain ManagerLookup", authNames.contains("ManagerLookup"));
        assertTrue(authNames + " should contain private-template-1", authNames.contains("private-template-1"));
    }

    @Test
    public void templates() {
        Map<String, TemplateQuery> templates = unauthorised.getTemplates();
        assertTrue(templates.containsKey("ManagerLookup"));
        assertNotNull(templates.get("ManagerLookup"));
        assertEquals(2, templates.get("ManagerLookup").getView().size());
    }

    @Test
    public void templatesForType() {
        Map<String, TemplateQuery> allTemplates = unauthorised.getTemplates();
        Set<TemplateQuery> managerTemplates = unauthorised.getTemplatesForType("Manager");
        assertTrue(managerTemplates.contains(allTemplates.get("ManagerLookup")));
        assertTrue(managerTemplates.size() > 1);
        Set<TemplateQuery> secretaryTemplates = unauthorised.getTemplatesForType("Secretary");
        assertTrue(secretaryTemplates.isEmpty());
        assertTrue(authorised.getTemplatesForType("Manager").size() > managerTemplates.size());
    }

    @Test
    public void count() {
        TemplateQuery managerLookup = unauthorised.getTemplate("ManagerLookup");
        assertEquals(2, unauthorised.getCount(managerLookup));
        TemplateQuery ceoRivals = unauthorised.getTemplate("CEO_Rivals");
        assertEquals(5, unauthorised.getCount(ceoRivals));
        managerLookup.replaceConstraint(managerLookup.getConstraintForCode("A"),
                Constraints.lookup("Manager", "David Brent", null));
        assertEquals(1, unauthorised.getCount(managerLookup));
    }

    @Test
    public void countWithParams() {
        List<TemplateParameter> params = new ArrayList<TemplateParameter>();
        params.add(new TemplateParameter("Manager", "LOOKUP", "David Brent", null));
        assertEquals(1, unauthorised.getCount("ManagerLookup", params));
    }

    @Test
    public void allResults() {
        List<TemplateParameter> params = new ArrayList<TemplateParameter>();
        params.add(new TemplateParameter("Manager", "LOOKUP", "David Brent", null));
        List<List<String>> results = unauthorised.getAllResults("ManagerLookup", params);
        assertEquals(1, results.size());
        assertEquals("David Brent", results.get(0).get(0));
    }

    @Test
    public void allResults2() {
        TemplateQuery managerLookup = unauthorised.getTemplate("ManagerLookup");
        managerLookup.replaceConstraint(managerLookup.getConstraintForCode("A"),
                Constraints.lookup("Manager", "David Brent", null));
        List<List<String>> results = unauthorised.getAllResults(managerLookup);
        assertEquals(1, results.size());
        assertEquals("David Brent", results.get(0).get(0));
    }

    @Test
    public void resultFrom1() {
        List<TemplateParameter> params = new ArrayList<TemplateParameter>();
        params.add(new TemplateParameter("CEO.name", "!=", "Charles Miner", null));
        assertEquals(5, unauthorised.getCount("CEO_Rivals", params));
        List<List<String>> results = unauthorised.getResults("CEO_Rivals", params, subset);
        assertEquals(2, results.size());
        assertEquals("497964", results.get(1).get(1));
    }

    @Test
    public void resultFrom2() {
        TemplateQuery ceoRivals = unauthorised.getTemplate("CEO_Rivals");
        ceoRivals.replaceConstraint(ceoRivals.getConstraintForCode("A"),
                Constraints.neq("CEO.name", "Charles Miner"));
        assertEquals(5, unauthorised.getCount(ceoRivals));
        List<List<String>> results = unauthorised.getResults(ceoRivals, subset);
        assertEquals(2, results.size());
        assertEquals("497964", results.get(1).get(1));
    }

    @Test
    public void allJSONresults1() throws JSONException {
        List<TemplateParameter> params = new ArrayList<TemplateParameter>();
        params.add(new TemplateParameter("Manager", "LOOKUP", "David Brent", null));
        List<JSONObject> results = unauthorised.getAllJSONResults("ManagerLookup", params);
        assertEquals(1, results.size());
        assertEquals("David Brent", results.get(0).getString("name"));
    }

    @Test
    public void allJSONresults2() throws JSONException {
        List<TemplateParameter> params = new ArrayList<TemplateParameter>();
        params.add(new TemplateParameter("CEO.name", "!=", "Charles Miner", null));
        List<JSONObject> results = unauthorised.getAllJSONResults("CEO_Rivals", params);
        assertEquals(5, results.size());
        assertEquals("Gogirep", results.get(3).getJSONObject("company").getString("name"));
    }

    @Test
    public void allJSONResults3() throws JSONException {
        TemplateQuery ceoRivals = unauthorised.getTemplate("CEO_Rivals");
        ceoRivals.replaceConstraint(ceoRivals.getConstraintForCode("A"),
                Constraints.neq("CEO.name", "Charles Miner"));
        List<JSONObject> results = unauthorised.getAllJSONResults(ceoRivals);
        assertEquals(5, results.size());
        assertEquals("Gogirep", results.get(3).getJSONObject("company").getString("name"));
    }

    @Test
    public void someJSONResults1() throws JSONException {
        List<TemplateParameter> params = new ArrayList<TemplateParameter>();
        params.add(new TemplateParameter("CEO.name", "!=", "Charles Miner", null));
        List<JSONObject> results = unauthorised.getJSONResults("CEO_Rivals", params, subset);
        assertEquals(2, results.size());
        assertEquals(333836, results.get(1).getInt("salary"));
        assertEquals("Capitol Versicherung AG", results.get(1).getJSONObject("company").getString("name"));
    }

    @Test
    public void someJSONResults2() throws JSONException {
        TemplateQuery ceoRivals = unauthorised.getTemplate("CEO_Rivals");
        ceoRivals.replaceConstraint(ceoRivals.getConstraintForCode("A"),
                Constraints.neq("CEO.name", "Charles Miner"));
        List<JSONObject> results = unauthorised.getJSONResults(ceoRivals, subset);
        assertEquals(2, results.size());
        assertEquals(333836, results.get(1).getInt("salary"));
        assertEquals("Capitol Versicherung AG", results.get(1).getJSONObject("company").getString("name"));
    }

    @Test
    public void allRowIterator() {
        List<TemplateParameter> params = new ArrayList<TemplateParameter>();
        params.add(new TemplateParameter("CEO.name", "!=", "Charles Miner", null));
        Iterator<List<String>> it = unauthorised.getAllRowsIterator("CEO_Rivals", params);
        int count = 0;
        while (it.hasNext()) {
            count++;
            it.next();
        }
        assertEquals(5, count);
    }

    @Test
    public void allRowIterator2() {
        TemplateQuery ceoRivals = unauthorised.getTemplate("CEO_Rivals");
        ceoRivals.replaceConstraint(ceoRivals.getConstraintForCode("A"),
                Constraints.neq("CEO.name", "Charles Miner"));
        Iterator<List<String>> it = unauthorised.getAllRowsIterator(ceoRivals);
        int count = 0;
        while (it.hasNext()) {
            count++;
            it.next();
        }
        assertEquals(5, count);
    }

    @Test
    public void rowIterator() {
        List<TemplateParameter> params = new ArrayList<TemplateParameter>();
        params.add(new TemplateParameter("CEO.name", "!=", "Charles Miner", null));
        Iterator<List<String>> it = unauthorised.getRowIterator("CEO_Rivals", params, subset);
        int count = 0;
        while (it.hasNext()) {
            count++;
            it.next();
        }
        assertEquals(2, count);
    }

    @Test
    public void rowIterator2() {
        TemplateQuery ceoRivals = unauthorised.getTemplate("CEO_Rivals");
        ceoRivals.replaceConstraint(ceoRivals.getConstraintForCode("A"),
                Constraints.neq("CEO.name", "Charles Miner"));
        Iterator<List<String>> it = unauthorised.getRowIterator(ceoRivals, subset);
        int count = 0;
        while (it.hasNext()) {
            count++;
            it.next();
        }
        assertEquals(2, count);
    }
}
