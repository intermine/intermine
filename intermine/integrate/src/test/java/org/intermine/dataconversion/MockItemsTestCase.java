package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import junit.framework.TestCase;

import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;


/**
 * TestCase to provide methods for comparing Items and collections of Items.
 *
 * @author Julie Sullivan
 */
public abstract class MockItemsTestCase extends TestCase
{
    /**
     *
     */
    public static final String ENDL = System.getProperty("line.separator");

    /**
     * @param arg
     */
    public MockItemsTestCase(String arg) {
        super(arg);
    }

    /**
     * @param a
     * @param b
     * @throws Exception
     */
    public static void assertEquals(Set<Item> a, Set<Item> b) throws Exception
    {
        Set<MockItem> mockA = createMockItems(a);
        Set<MockItem> mockB = createMockItems(b);
        String message = compareItemSets(mockA, mockB);
        if (message.length() > 0) {
            fail(message);
        }
    }

    // use identifiers to map relationships, then throw identifier away
    private static Set<MockItem> createMockItems(Set<Item> a) {
        Set<MockItem> items = new LinkedHashSet<MockItem>();
        Map<String, MockItem> identifiers = new HashMap<String, MockItem>();
        Iterator<Item> iter = a.iterator();

        // map identifier - mockItem
        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            MockItem mockItem = new MockItem(item);
            String identifier = item.getIdentifier();
            items.add(mockItem);
            identifiers.put(identifier, mockItem);
        }

        // replace identifier with referenced item
        Iterator<Item> iterator = a.iterator();
        while (iterator.hasNext()) {
            Item item = (Item) iterator.next();
            MockItem mockItem = identifiers.get(item.getIdentifier());
            Iterator<?> it = item.getReferences().iterator();
            while (it.hasNext()) {
                Reference reference = (Reference) it.next();
                MockItem referencedItem = identifiers.get(reference.getRefId());
                mockItem.addMockReference(reference.getName(), referencedItem);
            }

            it = item.getCollections().iterator();
            while (it.hasNext()) {
                ReferenceList collection = (ReferenceList) it.next();
                List<MockItem> collectedItems = new ArrayList<MockItem>();
                List<String> refIds = collection.getRefIds();
                for (String refId : refIds) {
                    collectedItems.add(identifiers.get(refId));
                }

                mockItem.addMockCollection(collection.getName(), collectedItems);
            }
        }
        return items;
    }

    private static String compareItemSets(Set<MockItem> a, Set<MockItem> b) {

        // now have compatible collections of items, compare them
        StringBuffer message = new StringBuffer();

        Set<MockItem> inAnotB = diffItemSets(a, b);
        Set<MockItem> inBnotA = diffItemSets(b, a);

        if (inAnotB.isEmpty() && inBnotA.isEmpty()) {
             // should be success, let TestCase handle it
            // TestCase.assertEquals(a, b);

        } else {

            StringBuffer aToString = new StringBuffer(), bToString = new StringBuffer();

            // fail with a helpful message
            message.append("Item collections do not match." + ENDL);
            if (!inAnotB.isEmpty()) {
                message.append("In expected, not actual: " + ENDL);
                for (MockItem mockItem : inAnotB) {
                    String xml = mockItem.toXML();
                    message.append(xml + ENDL);
                    aToString.append(xml);
                }
                message.append("Summary of expected: " + ENDL);
                message.append(countItemClasses(inAnotB));
            } else if (a.isEmpty()) {
                message.append("Expected set was empty. " + ENDL);
            }
            if (!inBnotA.isEmpty()) {
                message.append("In actual, not expected: " + ENDL);
                for (MockItem mockItem : inBnotA) {
                    String xml = mockItem.toXML();
                    message.append(xml);
                    bToString.append(xml);
                }
                message.append("Summary of actual: " + ENDL);
                message.append(countItemClasses(inBnotA) + ENDL);
            } else if (b.isEmpty()) {
                message.append("Actual set was empty. " + ENDL);
            }

// TODO compare each item and list what is different
//            message.append("DIFFERENCE:" + ENDL);
//            message.append(StringUtils.difference(aToString.toString(), bToString.toString()));
        }
        return message.toString();
    }


    /**
     * Given two sets of Items (a and b) return a set of Items that are present in a
     * but not b.
     * @param a a set of Items
     * @param b a set of Items
     * @return the set of Items in a but not in b
     */
    public static Set<MockItem> diffItemSets(Set<MockItem> a, Set<MockItem> b) {
        Set<MockItem> diff = new HashSet<MockItem>(a);
        Iterator<MockItem> i = a.iterator();
        while (i.hasNext()) {
            MockItem itemA = (MockItem) i.next();
            Iterator<MockItem> j = b.iterator();
            while (j.hasNext()) {
                MockItem itemB = (MockItem) j.next();
                if (itemA.equals(itemB)) {
                    diff.remove(itemA);
                }
            }
        }
        return diff;
    }


    /**
     * For a collection of items return a string containing counts of each
     * item classname - useful for degugging tests.
     * @param items the Item collection to count
     * @return a formatted string with counts for each classname in the collection
     */
    public static String countItemClasses(Collection<MockItem> items) {
        Map<String, List> counts = new TreeMap<String, List>();
        for(MockItem item : items) {
            List<String> clsItems = counts.get(item.getClassName());
            if (clsItems == null) {
                clsItems = new ArrayList<String>();
                counts.put(item.getClassName(), clsItems);
            }
            clsItems.add(item.getIdentifier());
        }

        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, List> entry : counts.entrySet()) {
            sb.append(entry.getKey() + " - " + entry.getValue().size() + " "
                      + entry.getValue() + ENDL);
        }
        return sb.toString();
    }

//  // fail with helpful message if e.g. we are asserting a Set .equals a List
//  private static void compatibleCollections(Collection a, Collection b) {
//      if (a.getClass().isAssignableFrom(b.getClass())
//          || b.getClass().isAssignableFrom(a.getClass())) {
//          TestCase.assertEquals(a, b);
//      } else {
//          TestCase.fail("Collections are of incompatible types: "
//                        + a.getClass() + " (" + a.toString() + ") and "
//                        + b.getClass() + " (" + b.toString() + ").");
//      }
//  }
//

    /**
     * @param fileName
     * @return set of items from XML file
     * @throws Exception
     */
    public Set<Item> readItemSet(String fileName) throws Exception {
        return new LinkedHashSet<Item>(FullParser.parse(getClass().getClassLoader()
                                                  .getResourceAsStream(fileName)));
    }

    /**
     * @param items
     * @param fileName
     * @throws IOException
     */
    public void writeItemsFile(Collection<Item> items, String fileName) throws IOException {
        FileWriter fw = new FileWriter(new File(fileName));
        fw.write(FullRenderer.render(items));
        fw.close();
    }
}