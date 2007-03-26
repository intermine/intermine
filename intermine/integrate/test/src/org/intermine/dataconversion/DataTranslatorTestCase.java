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

import java.util.*;
import java.io.InputStream;

import org.intermine.xml.full.Item;
import org.intermine.metadata.Model;
import org.intermine.metadata.MetaDataException;

/**
 * TestCase for all DataTranslators
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 * @author Peter McLaren - adding some logger related code.
 */
public abstract class DataTranslatorTestCase extends TargetItemsTestCase
{
    protected Model srcModel;
    protected Properties mapping;

    /**
     * Create a new DataTranslatorTestCase object.
     * @param arg the argument to pass the to super constructor
     */
    public DataTranslatorTestCase(String arg, String oswAlias) {
        super(arg, oswAlias);
    }

    /**
     * @see TargetItemsTestCase#setUp()
     */
    public void setUp() throws Exception {
        super.setUp();
        mapping = new Properties();
        InputStream is =
            getClass().getClassLoader().getResourceAsStream(getSrcModelName() + "_mappings");
        if (is != null) {
            mapping.load(is);
        }
        srcModel = Model.getInstanceByName(getSrcModelName());

    }

    /**
     * Get the target Model for this test.
     * @param ns the namespace for the target model
     * @return the target Model
     * @throws MetaDataException if the Model cannot be found
     */
    public Model getTargetModel(String ns) throws MetaDataException {
        if (ns.equals("http://www.flymine.org/model/genomic#")) {
            return Model.getInstanceByName("genomic");
        }

        throw new RuntimeException("can't find Model for: " + ns);
    }

    /**
     * Get the Collection of test source Items
     * @return the Collection of Items
     * @throws Exception if an error occurs
     */
    protected abstract Collection getSrcItems() throws Exception;

    /**
     * Given two sets of Items (a and b) return a set of Items that are present in a
     * but not b.
     * @param a a set of Items
     * @param b a set of Items
     * @return the set of Items in a but not in b
     */
    public static Set compareItemSets(Set a, Set b) {
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
     * If given expected and actual item sets differ return a string detailing items in expected
     * and not in actual and in actual but not expected.
     * @param expected the expected set of org.intermine.xml.full.Items
     * @param actual the actual set of org.intermine.xml.full.Items
     * @return the differences between the to
     */
    public static String printCompareItemSets(Set expected, Set actual) {
        String expectedNotActual = "in expected, not actual: "
                + makeLogStringFromItemSet(compareItemSets(expected, actual));
        String actualNotExpected = "in actual, not expected: "
                + makeLogStringFromItemSet(compareItemSets(actual, expected));

        if ((expectedNotActual.length() > 27) || (actualNotExpected.length() > 27)) {
            return expectedNotActual + System.getProperty("line.separator") + actualNotExpected;
        }
        return "";
    }


    /**
     * Get the source Model for this test.
     * @return the source Model
     */
    protected abstract String getSrcModelName();


    public static String makeLogStringFromItemSet(Set itemSet){

        TreeSet orderedItemSet = new TreeSet();

        for (Iterator isit = itemSet.iterator(); isit.hasNext(); ) {

            orderedItemSet.add(new ItemLogOrderWrapper((Item)isit.next()));
        }

        StringBuffer logStringBuffer = new StringBuffer();

        for (Iterator oisit = orderedItemSet.iterator(); oisit.hasNext();) {

            logStringBuffer.append(((ItemLogOrderWrapper)oisit.next()).toString());
        }

        return logStringBuffer.toString();
    }


    static class ItemLogOrderWrapper implements Comparable {

        private Item item2Wrap;

        ItemLogOrderWrapper(Item item2Wrap) {
            this.item2Wrap = item2Wrap;
        }

        public int compareTo(Object o) {

            if (o instanceof ItemLogOrderWrapper) {

                ItemLogOrderWrapper ilow = (ItemLogOrderWrapper) o;

                int compVal = getItemClassName().compareToIgnoreCase(ilow.getItemClassName());

                if (compVal == 0) {
                    compVal = getItemIdentifier().compareToIgnoreCase(ilow.getItemIdentifier());
                }
                return compVal;

            } else {
                throw new RuntimeException("ItemLogOrderWrapper can't compare with a "
                        + o.getClass().getName());
            }
        }

        String getItemClassName() {
            return item2Wrap.getClassName();
        }

        String getItemIdentifier() {
            return item2Wrap.getIdentifier();
        }

        public String toString() {
            return item2Wrap.toString();
        }
    }

}

