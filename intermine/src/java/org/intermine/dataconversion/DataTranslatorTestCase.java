package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.intermine.xml.full.Item;

import com.hp.hpl.jena.ontology.OntModel;

/**
 * TestCase for all DataTranslators
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public abstract class DataTranslatorTestCase extends TargetItemsTestCase
{
    /**
     * Get the Collection of test source Items
     * @return the Collection of Items
     * @throws Exception if an error occurs
     */
    protected abstract Collection getSrcItems() throws Exception;

    /**
     * Subclasses must provide access to the ontology model.
     * @return the ontology model
     */
    protected abstract OntModel getOwlModel();



    /**
     * Given two sets of Items (a and b) return a set of Items that are present in a
     * but not b.
     * @param a a set of Items
     * @param b a set of Items
     * @return the set of Items in a but not in b
     */
    public Set compareItemSets(Set a, Set b) {
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


}
