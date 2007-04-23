package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;


/**
 * TestCase to provide methods for comparing Items and collections of Items.
 *
 * @author Richard Smith
 */
public abstract class ItemsTestCase extends TestCase
{
    public static final String ENDL = System.getProperty("line.separator");
    public ItemsTestCase(String arg) {
        super(arg);
    }


    public static void assertEquals(Collection a, Collection b) throws Exception
    {
        compatibleCollections(a, b);
        TestCase.assertEquals(a, b);
    }
    
    public static void assertEquals(Set a, Set b) throws Exception
    {
        // check that these are both items sets
        if (!checkItemSet(a) || ! checkItemSet(b)) {
            TestCase.assertEquals(a, b);
            return;
        }

        String message = compareItemSets(a, b, false);
        if (message .length() > 0) {
            fail(message);
        }
    }

    public static String compareItemSets(Set a, Set b) {
        return compareItemSets(a, b, true);
    }
    
    private static boolean checkItemSet(Set set) {
        Iterator iter = set.iterator();
        while (iter.hasNext()) {
            if (!(iter.next() instanceof Item)) {
                return false;
            }
        }
        return true;
    }
    
    private static String compareItemSets(Set a, Set b, boolean checkItems) {
        // check these are both item sets
        if (checkItems && (!checkItemSet(a) || !checkItemSet(b))) {
            throw new IllegalArgumentException("Comparing sets that contains objects that "
                                               + "aren't Items: a = " + a + ", b = " + b);
        }
        
        // now have compatible collections of items, compare them
        StringBuffer message = new StringBuffer();
        Set inAnotB = diffItemSets(a, b);
        Set inBnotA = diffItemSets(b, a);
        if (inAnotB.isEmpty() && inBnotA.isEmpty()) {
            // should be success, let TestCase handle it
            TestCase.assertEquals(a, b);
        } else {
            // fail with a helpful message
            message.append("Item collections do not match." + ENDL);
            if (!inAnotB.isEmpty()) {
                message.append("In expected, not actual: " + ENDL);
                TreeSet ts = new TreeSet();
                ts.addAll(inAnotB);
                message.append(ts + ENDL);
                message.append("Summary of expected: " + ENDL);
                message.append(countItemClasses(inAnotB));
            } else if (a.isEmpty()) {
                message.append("Expected set was empty. " + ENDL);
            }
            if (!inBnotA.isEmpty()) {
                message.append("In actual, not expected: " + ENDL);
                TreeSet ts = new TreeSet();
                ts.addAll(inBnotA);
                message.append(ts + ENDL);
                message.append("Summary of actual: " + ENDL);
                message.append(countItemClasses(inBnotA));
            } else if (b.isEmpty()) {
                message.append("Actual set was empty. " + ENDL);
            }
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
    public static Set diffItemSets(Set a, Set b) {
        Set diff = new HashSet(a);
        Iterator i = a.iterator();
        while (i.hasNext()) {
            Item itemA = (Item) i.next();
            Iterator j = b.iterator();
            while (j.hasNext()) {
                Item itemB = (Item) j.next();
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
    public static String countItemClasses(Collection<Item> items) {
        Map<String, List> counts = new TreeMap<String, List>();
        for(Item item : items) {
            List clsItems = counts.get(item.getClassName());
            if (clsItems == null) {
                clsItems = new ArrayList();
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
    
    
    // fail with helpful message if e.g. we are asserting a Set .equals a List
    private static void compatibleCollections(Collection a, Collection b) {
        if (a.getClass().isAssignableFrom(b.getClass())
            || b.getClass().isAssignableFrom(a.getClass())) {
            TestCase.assertEquals(a, b);
        } else {
            TestCase.fail("Collections are of incompatible types: "
                          + a.getClass() + " (" + a.toString() + ") and "
                          + b.getClass() + " (" + b.toString() + ").");
        }
    }
    
    public Set readItemSet(String fileName) throws Exception {
        return new HashSet(FullParser.parse(getClass().getClassLoader()
                                .getResourceAsStream(fileName)));
    }
    
    public void writeItemsFile(Collection items, String fileName) throws IOException {
        FileWriter fw = new FileWriter(new File(fileName));
        fw.write(FullRenderer.render(items));
        fw.close();
    }
}
