package org.intermine.webservice.client.live;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.lists.ItemList;
import org.intermine.webservice.client.results.Item;
import org.intermine.webservice.client.services.ListService;
import org.intermine.webservice.client.services.ListService.ListCreationInfo;
import org.intermine.webservice.client.util.TestUtil;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.junit.AfterClass;
import org.junit.Test;

public class LiveListTest {

    private static final Logger LOGGER = Logger.getLogger(LiveListTest.class);
    private static ListService testmine =
            new ServiceFactory(TestUtil.getRootUrl(), TestUtil.getToken()).getListService();
    private static final List<ItemList> tempLists = new ArrayList<ItemList>();
    private static int initialSize = 0;

    @AfterClass
    public static void tearDown() {
        if (!tempLists.isEmpty()) {
            for (ItemList i : tempLists) {
                try {
                    LOGGER.debug("Deleting " + i);
                    testmine.deleteList(i);
                } catch (Throwable t){
                    LOGGER.error("Error deleting " + i, t);
                }
            }
        }
        int nowSize = testmine.getAccessibleLists().size();
        if (initialSize != nowSize) {
            throw new RuntimeException("Error cleaning up - we started with " + initialSize
                    + " lists but now we have " + nowSize);
        }
    }

    /**
     *
     *
     */
    @Test
    public void accessibleLists() {
        List<ItemList> lists = testmine.getAccessibleLists();
        assertTrue(lists.size() > 0);
        initialSize = lists.size();
    }

    @Test
    public void listMap() {
        Map<String, ItemList> lists = testmine.getListMap();
        ItemList favs = lists.get("My-Favourite-Employees");
        assertEquals(favs.getSize(), 4);
    }

    @Test
    public void status() {
        ItemList favs = testmine.getList("My-Favourite-Employees");
        assertEquals("CURRENT", favs.getStatus());
    }

    @Test
    public void items() {
        ItemList favs = testmine.getList("My-Favourite-Employees");
        Item timo = favs.get(1);
        assertEquals("Timo Becker", timo.getString("name"));
        assertEquals(Integer.valueOf(56224), timo.getInt("seniority"));
        assertTrue(timo.isa("Manager"));
        assertTrue(timo.isa("Employee"));
        assertTrue(! timo.isa("Department"));
    }

    @Test
    public void iteration() {
        ItemList favs = testmine.getList("My-Favourite-Employees");
        Set<String> expectedNames = new HashSet<String>(Arrays.asList("Bernd Stromberg", "David Brent", "Neil Godwin", "Timo Becker"));
        Set<String> gotNames = new HashSet<String>();
        for (Item i: favs) {
            gotNames.add(i.getString("name"));
        }

        assertEquals(expectedNames, gotNames);
    }

    @Test
    public void creationFromFile() throws URISyntaxException {

        ListCreationInfo info = testmine.new ListCreationInfo("Employee");
        URL idsURL = getClass().getResource("ids.txt");
        File ids = new File(idsURL.toURI());
        info.setContent(ids);

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);
        assertEquals(3, newList.size());
        assertEquals(new HashSet<String>(Arrays.asList("David", "Edgar")), newList.getUnmatchedIdentifiers());
    }

    @Test
    public void creationFromArray() throws FileNotFoundException, ParseException {

        ListCreationInfo info = testmine.new ListCreationInfo("Employee");
        info.setContent(Arrays.asList("Anne", "Brenda", "Carol", "David", "Edgar"));
        info.addTag("java-list");

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);
        assertEquals(3, newList.size());
        assertEquals(new HashSet<String>(Arrays.asList("David", "Edgar")), newList.getUnmatchedIdentifiers());
    }

    @Test
    public void trickyCharacters() throws FileNotFoundException, ParseException {
        ListCreationInfo info = testmine.new ListCreationInfo("Employee");
        info.setContent(Arrays.asList("Herr P\u00f6tsch"));
        info.addTag("java-list");

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);
        assertEquals(1, newList.size());
    }

    @Test
    public void createFromQuery() {
        PathQuery pq = new PathQuery(testmine.getFactory().getModel());
        pq.addView("Employee.id");
        pq.addConstraint(Constraints.eq("Employee.department.name", "Sales"));

        ListCreationInfo info  = testmine.new ListCreationInfo(pq);
        info.addTag("java-list");

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);
        assertEquals(18, newList.size());
    }

    @Test
    public void listDeletion() throws ParseException, FileNotFoundException {
        ListCreationInfo info = testmine.new ListCreationInfo("Employee");
        info.setContent(Arrays.asList("Anne", "Brenda", "Carol", "David", "Edgar"));
        info.addTag("java-list");

        ItemList delendum = testmine.createList(info);

        assertTrue(delendum != null);
        testmine.deleteList(delendum);

        assertNull(testmine.getList(delendum.getName()));
    }

    @Test
    public void listsWithObject() {
        List<ItemList> allLists = testmine.getAccessibleLists();

        List<ItemList> publicIdLists = testmine.getListsWithObject("David Brent", "Manager");
        assertTrue(allLists.size() > publicIdLists.size());
        assertTrue(publicIdLists.size() > 0);

        int davidsId = 0;
        for (Item i : publicIdLists.get(0)) {
            if ("David Brent".equals(i.getString("name"))) {
                davidsId = i.getInt("id");
                break;
            }
        }

        List<ItemList> dbIdLists = testmine.getListsWithObject(davidsId);
        assertTrue(dbIdLists.size() == publicIdLists.size());
    }

    @Test
    public void appendIds() throws FileNotFoundException, ParseException {
        ListCreationInfo info = testmine.new ListCreationInfo("Employee");
        info.setContent(Arrays.asList("Anne", "Brenda", "Carol", "David", "Edgar"));
        info.addTag("java-list");

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);

        int previousSize = newList.size();

        newList.append("David Brent");

        assertTrue(newList.size() == previousSize + 1);

        previousSize = newList.size();
        Set<String> badIds = newList.getUnmatchedIdentifiers();

        newList.append("Does not exist");
        assertTrue(newList.size() == previousSize);
        badIds.add("Does not exist");
        assertEquals(badIds, newList.getUnmatchedIdentifiers());
    }

    @Test
    public void appendItemList() throws FileNotFoundException, ParseException {
        ListCreationInfo info = testmine.new ListCreationInfo("Employee");
        info.setContent(Arrays.asList("Anne", "Brenda", "Carol", "David", "Edgar"));
        info.addTag("java-list");

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);

        ItemList favs = testmine.getList("My-Favourite-Employees");

        newList.addAll(favs);

        assertTrue(newList.size() == 7);
    }

    @Test
    public void appendItem() throws FileNotFoundException, ParseException {
        ListCreationInfo info = testmine.new ListCreationInfo("Employee");
        info.setContent(Arrays.asList("Anne", "Brenda", "Carol", "David", "Edgar"));
        info.addTag("java-list");

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);

        ItemList favs = testmine.getList("My-Favourite-Employees");
        Item david = null;
        for (Item i: favs) {
            if ("David Brent".equals(i.getString("name"))) {
                david = i;
                break;
            }
        }

        newList.append(david);

        assertTrue(newList.size() == 4);
    }

    @Test
    public void appendResultSet() throws FileNotFoundException, ParseException {
        ListCreationInfo info = testmine.new ListCreationInfo("Employee");
        info.setContent(Arrays.asList("Anne", "Brenda", "Carol", "David", "Edgar"));
        info.addTag("java-list");

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);

        PathQuery pq = new PathQuery(testmine.getFactory().getModel());
        pq.addViews("Employee.id");
        pq.addConstraint(Constraints.eq("Employee.department.name", "Sales"));

        newList.append(pq);

        assertEquals(21, newList.size());
    }

    @Test
    public void merging() throws ParseException {
        ItemList favs = testmine.getList("My-Favourite-Employees");
        ItemList umlauters = testmine.getList("Umlaut holders");

        ItemList newList = testmine.merge(favs, umlauters);
        tempLists.add(newList);

        assertEquals(6, newList.size());
        assertEquals(newList.size(), favs.size() + umlauters.size());
        assertEquals("Employee", newList.getType());
    }

    @Test
    public void intersecting() throws ParseException {
        ItemList a = testmine.getList("My-Favourite-Employees");
        ItemList b = testmine.getList("some favs-some unknowns");
        ItemList c = testmine.getList("some favs-some unknowns-some umlauts");

        ItemList newList = testmine.intersect(a, b, c);
        tempLists.add(newList);

        assertEquals(1, newList.size());
        assertEquals("Manager", newList.getType());
        assertEquals("David Brent", newList.get(0).getString("name"));
    }

    @Test
    public void diffing() throws ParseException {
        ItemList a = testmine.getList("My-Favourite-Employees");
        ItemList b = testmine.getList("some favs-some unknowns");
        ItemList c = testmine.getList("some favs-some unknowns-some umlauts");

        ItemList newList = testmine.diff(a, b, c);
        tempLists.add(newList);

        assertEquals(7, newList.size());
        assertEquals("Employee", newList.getType());
    }

    @Test
    public void subtracting() {
        ItemList a = testmine.getList("My-Favourite-Employees");
        ItemList b = testmine.getList("some favs-some unknowns-some umlauts");

        ItemList c = b.subtract(a);
        tempLists.add(c);

        assertEquals(3, c.size());
        assertEquals("Employee", c.getType());
    }

    @Test
    public void removingAll() {
        ListCreationInfo info = testmine.new ListCreationInfo("Employee");
        info.setContent("Anne", "Brenda", "Carol", "David Brent");
        info.addTag("java-list");

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);

        ItemList favs = testmine.getList("My-Favourite-Employees");

        assertEquals(4, newList.size());
        assertTrue(newList.removeAll(favs));
        assertEquals(3, newList.size());
        assertTrue(!newList.removeAll(favs));
    }

    @Test
    public void removing() {
        ListCreationInfo info = testmine.new ListCreationInfo("Employee");
        info.setContent("Anne", "Brenda", "Carol", "Timo Becker");
        info.addTag("java-list");

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);

        ItemList favs = testmine.getList("My-Favourite-Employees");
        Item timo = favs.get(1);
        Item someoneElse = favs.get(0);

        assertEquals(4, newList.size());
        assertTrue(newList.remove(timo));
        assertEquals(3, newList.size());
        boolean changed = newList.remove(someoneElse);
        assertTrue(!changed);
    }

    @Test
    public void removingFromCollection() {
        ListCreationInfo info = testmine.new ListCreationInfo("Employee");
        info.setContent("Anne", "Brenda", "Carol", "Timo Becker", "Frank M\u00f6llers");
        info.addTag("java-list");

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);

        ItemList favs = testmine.getList("My-Favourite-Employees");
        ItemList umlauts = testmine.getList("Umlaut holders");
        List<Item> toRemove = new ArrayList<Item>();
        toRemove.addAll(favs);
        toRemove.addAll(umlauts);

        assertEquals(5, newList.size());
        assertTrue(newList.removeAll(toRemove));
        assertEquals(3, newList.size());
        boolean changed = newList.removeAll(toRemove);
        assertTrue(!changed);
    }

    @Test
    public void renaming() {
        ListCreationInfo info = testmine.new ListCreationInfo("Employee", "to-be-renamed");
        info.setContent(Arrays.asList("Anne", "Brenda", "Carol", "David", "Edgar"));
        info.addTag("java-list");

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);

        assertEquals("to-be-renamed", newList.getName());

        newList.rename("has-been-renamed");

        assertEquals("has-been-renamed", newList.getName());
        assertNull(testmine.getList("to-be-renamed"));
    }

    @Test
    public void tagging() {
        ListCreationInfo info = testmine.new ListCreationInfo("Employee", "to-be-tagged");
        info.setContent(Arrays.asList("Anne", "Brenda", "Carol", "David", "Edgar"));
        info.addTag("java-list");

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);

        Set<String> tags1 = newList.getTags();
        assertTrue(tags1.contains("java-list"));

        newList.updateTags(); // Has no effect unless things are changed.
        assertEquals(tags1, newList.getTags());

        testmine.addTags(newList, "testing1", "testing2");
        tags1.addAll(Arrays.asList("testing1", "testing2"));

        assertFalse(tags1.equals(newList.getTags()));

        newList.updateTags(); // Synchronizes with live tag state.
        assertEquals(tags1, newList.getTags());

        newList.addTags("testing3");
        tags1.add("testing3");
        assertEquals(tags1, newList.getTags());

        newList.removeTags("testing1", "testing3");
        tags1.removeAll(Arrays.asList("testing1", "testing3"));

        assertEquals(tags1, newList.getTags());
    }

    @SuppressWarnings("serial")
    @Test
    public void finding() {
        ItemList favs = testmine.getList("My-Favourite-Employees");
        List<Item> davids = favs.find(new HashMap<String, Object>() {{ put("name", "David Brent"); }});
        assertEquals(1, davids.size());
        Item david = davids.get(0);
        assertNotNull(david);
        assertEquals("David Brent", david.getString("name"));
        assertFalse(david.getBoolean("fullTime"));
        assertEquals(new Integer(81361), david.getInt("seniority"));

        List<Item> hasAnO = favs.find(new HashMap<String, Object>() {{ put("name", "*o*"); }});
        assertEquals(3, hasAnO.size());
    }

    @SuppressWarnings("serial")
    @Test
    public void containing() {
        ListCreationInfo info = testmine.new ListCreationInfo("Employee");
        info.setContent(Arrays.asList("Anne", "Brenda", "Carol", "David", "Edgar"));
        info.addTag("java-list");

        ItemList newList = testmine.createList(info);
        tempLists.add(newList);

        ItemList favs = testmine.getList("My-Favourite-Employees");
        Item david = favs.find(new HashMap<String, Object>() {{ put("name", "David Brent"); }}).get(0);
        Item timo = favs.find(new HashMap<String, Object>() {{ put("name", "Timo Becker"); }}).get(0);

        assertFalse(newList.contains(david));
        assertFalse(newList.containsAll(Arrays.asList(david, timo)));

        newList.append(david);
        assertTrue(newList.contains(david));
        assertFalse(newList.containsAll(Arrays.asList(david, timo)));

        newList.append(timo);
        assertTrue(newList.contains(david));
        assertTrue(newList.contains(timo));
        assertTrue(newList.containsAll(Arrays.asList(david, timo)));
    }

    @Test
    public void references() {
        ItemList favs = testmine.getList("My-Favourite-Employees");
        Item bernd = favs.get(0);
        Item department = bernd.getReference("department");

        assertNotNull(department);
        assertEquals("Schadensregulierung M-Z", department.getString("name"));

        Item company = department.getReference("company");
        assertNotNull(company);
        assertEquals("Capitol Versicherung AG", company.getString("name"));
    }

    @Test
    public void illegalTagNames() {
        ItemList favs = testmine.getList("My-Favourite-Employees");
        try {
            testmine.addTags(favs, "!$%^&*(");
            fail("Should not have been allowed to add that tag");
        } catch (ServiceException e) {
            String message = e.getMessage() != null ? e.getMessage() : "";
            if (e.getCause() != null) {
                message += e.getCause().getMessage();
            }
            assertTrue( "Message (" + message + ") should be informative", message.indexOf("Invalid name") >= 0);
        }
    }

    @Test
    public void forbiddenTagNames() {
        ItemList favs = testmine.getList("My-Favourite-Employees");
        try {
            testmine.addTags(favs, "im:foo");
            fail("Should not have been allowed to add that tag");
        } catch (ServiceException e) {
            String message = e.getMessage() != null ? e.getMessage() : "";
            if (e.getCause() != null) {
                message += e.getCause().getMessage();
            }
            assertTrue( "Message (" + message + ") should be informative", message.indexOf("starting with im") >= 0);
        }
    }

    @Test
    public void collections() {
        ItemList favs = testmine.getList("My-Favourite-Employees");
        Item stromberg = favs.get(0);
        Item department = stromberg.getReference("department");

        Set<Item> employees = department.getCollection("employees");

        assertNotNull(employees);
        assertEquals(6, employees.size());
        assertTrue(employees.contains(stromberg));
    }
}
