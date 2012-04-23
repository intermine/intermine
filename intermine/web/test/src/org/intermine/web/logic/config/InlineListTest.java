package org.intermine.web.logic.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.model.testmodel.Company;
import org.intermine.util.DynamicUtil;
import org.intermine.web.logic.results.InlineList;

/**
 * Tests an InlineList appearing on Report page
 * @author radek
 *
 */
public class InlineListTest extends TestCase
{

    Company company1;
    Company company2;
    Company company3;

    protected void setUp() throws Exception {
        super.setUp();

        // InterMine Objects
        company1 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company1.setId(new Integer(1));
        company1.setName("Weyland Yutani");

        company2 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company2.setId(new Integer(2));
        company2.setName("Initech");

        company3 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        company3.setId(new Integer(3));
        company3.setName("Umbrella Corp.");
    }

    /**
     * Duplo test name matches the complexity of the task contained...
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testDuplo() throws Exception {
        HashSet<Object> listOfObjects = new HashSet<Object>(
                new ArrayList<Object>(
                        Arrays.asList(company1, company2, company3)));
        InlineList inlineList = new InlineList(listOfObjects, "name", true, null, null);

        // traverse for compare
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : inlineList.getItems()) {
            Map<String, Object> resultElement = new LinkedHashMap<String, Object>();

            // Class type assertion
            assertTrue(item instanceof InlineListObject);

            // save
            resultElement.put("value", ((InlineListObject) item).getValue());
            resultElement.put("id", ((InlineListObject) item).getId());
            result.add(resultElement);
        }

        // [{value=Weyland Yutani, id=1}, {value=Initech, id=2}, {value=Umbrella Corp., id=3}]
        List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
        Map<String, Object> m1 = new LinkedHashMap<String, Object>();
        m1.put("value", "Weyland Yutani");
        m1.put("id", 1);
        l.add(m1);
        Map<String, Object> m2 = new LinkedHashMap<String, Object>();
        m2.put("value", "Initech");
        m2.put("id", 2);
        l.add(m2);
        Map<String, Object> m3 = new LinkedHashMap<String, Object>();
        m3.put("value", "Umbrella Corp.");
        m3.put("id", 3);
        l.add(m3);

        // count the number of elements as HashSet (next) will remove duplicates
        assertEquals(l.size(), result.size());
        // convert to HashSet for comparison
        assertEquals(new HashSet(l), new HashSet(result));
    }

}
