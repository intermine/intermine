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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.ontology.DagTerm;
import org.intermine.ontology.DagTermSynonym;
import org.intermine.ontology.OboTerm;
import org.intermine.ontology.OboTermSynonym;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

public class OboConverterTest extends TestCase {
    String NAMESPACE = "http://www.flymine.org/model/genomic#";
    MockItemWriter itemWriter;
    private ItemFactory itemFactory = new ItemFactory();

    public void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap());
    }

    public void test1() throws Exception {
        DagConverter converter = new OboConverter(itemWriter, "", "SO", "http://www.flymine.org",
                                                  NAMESPACE + "OntologyTerm");
        DagTerm a = new OboTerm("SO:42", "parent");
        DagTerm b = new OboTerm("SO:43", "child");
        DagTerm c = new OboTerm("SO:44", "partof");
        c.addSynonym(new OboTermSynonym("syn2", "exact_synonym"));
        b.addSynonym(new OboTermSynonym("syn1", "narrow_synonym"));
        b.addSynonym(new OboTermSynonym("syn2", "exact_synonym"));
        a.addChild(b);
        a.addComponent(c);
        converter.process(Arrays.asList(new Object[] {a, b, c}));

        Set expected = new HashSet();

        Item item = itemFactory.makeItem();
        item.setIdentifier("0_0");
        item.setClassName(NAMESPACE + "Ontology");
        item.setImplementations("");
        Attribute attribute = new Attribute();
        attribute.setName("title");
        attribute.setValue("SO");
        item.addAttribute(attribute);
        item.setAttribute("url", "http://www.flymine.org");
        expected.add(item);

        item = itemFactory.makeItem();
        item.setIdentifier("0_1");
        item.setClassName(NAMESPACE + "OntologyTerm");
        item.setImplementations("");
        attribute = new Attribute();
        attribute.setName("name");
        attribute.setValue("parent");
        item.addAttribute(attribute);
        attribute = new Attribute();
        attribute.setName("identifier");
        attribute.setValue("SO:42");
        item.addAttribute(attribute);
        Reference ref = new Reference();
        ref.setName("ontology");
        ref.setRefId("0_0");
        item.addReference(ref);
        ReferenceList refs = new ReferenceList();
        refs.setName("childRelations");
        refs.addRefId("0_3");
        refs.addRefId("0_5");
        item.addCollection(refs);
        refs = new ReferenceList();
        refs.setName("parentRelations");
        item.addCollection(refs);
        item.addAttribute(new Attribute("description", ""));
        item.addAttribute(new Attribute("namespace", ""));
        item.setAttribute("obsolete", "false");
        expected.add(item);

        item = itemFactory.makeItem();
        item.setIdentifier("0_2");
        item.setClassName(NAMESPACE + "OntologyTerm");
        item.setImplementations("");
        attribute = new Attribute();
        attribute.setName("name");
        attribute.setValue("child");
        item.addAttribute(attribute);
        attribute = new Attribute();
        attribute.setName("identifier");
        attribute.setValue("SO:43");
        item.addAttribute(attribute);
        ref = new Reference();
        ref.setName("ontology");
        ref.setRefId("0_0");
        item.addReference(ref);
        refs = new ReferenceList();
        refs.setName("childRelations");
        item.addCollection(refs);
        refs = new ReferenceList();
        refs.setName("parentRelations");
        refs.addRefId("0_3");
        item.addCollection(refs);
        item.addAttribute(new Attribute("description", ""));
        item.addAttribute(new Attribute("namespace", ""));
        refs = new ReferenceList();
        refs.setName("synonyms");
        refs.addRefId("1_0");
        refs.addRefId("1_1");
        item.addCollection(refs);
        item.setAttribute("obsolete", "false");
        expected.add(item);

        item = itemFactory.makeItem();
        item.setIdentifier("0_3");
        item.setClassName(NAMESPACE + "OntologyRelation");
        item.setImplementations("");
        attribute = new Attribute();
        attribute.setName("type");
        attribute.setValue("is_a");
        item.addAttribute(attribute);
        ref = new Reference();
        ref.setName("childTerm");
        ref.setRefId("0_2");
        item.addReference(ref);
        ref = new Reference();
        ref.setName("parentTerm");
        ref.setRefId("0_1");
        item.addReference(ref);
        expected.add(item);

        item = itemFactory.makeItem();
        item.setIdentifier("0_4");
        item.setClassName(NAMESPACE + "OntologyTerm");
        item.setImplementations("");
        attribute = new Attribute();
        attribute.setName("name");
        attribute.setValue("partof");
        item.addAttribute(attribute);
        attribute = new Attribute();
        attribute.setName("identifier");
        attribute.setValue("SO:44");
        item.addAttribute(attribute);
        ref = new Reference();
        ref.setName("ontology");
        ref.setRefId("0_0");
        item.addReference(ref);
        refs = new ReferenceList();
        refs.setName("childRelations");
        item.addCollection(refs);
        refs = new ReferenceList();
        refs.setName("parentRelations");
        refs.addRefId("0_5");
        item.addCollection(refs);
        item.addAttribute(new Attribute("description", ""));
        item.addAttribute(new Attribute("namespace", ""));
        refs = new ReferenceList();
        refs.setName("synonyms");
        refs.addRefId("1_1");
        item.addCollection(refs);
        item.setAttribute("obsolete", "false");
        expected.add(item);

        item = itemFactory.makeItem();
        item.setIdentifier("0_5");
        item.setClassName(NAMESPACE + "OntologyRelation");
        item.setImplementations("");
        attribute = new Attribute();
        attribute.setName("type");
        attribute.setValue("part_of");
        item.addAttribute(attribute);
        ref = new Reference();
        ref.setName("childTerm");
        ref.setRefId("0_4");
        item.addReference(ref);
        ref = new Reference();
        ref.setName("parentTerm");
        ref.setRefId("0_1");
        item.addReference(ref);
        expected.add(item);

        item = itemFactory.makeItem();
        item.setIdentifier("1_0");
        item.setClassName(NAMESPACE + "OntologyTermSynonym");
        item.setImplementations("");
        item.addAttribute(new Attribute("name", "syn1"));
        item.addAttribute(new Attribute("type", "narrow_synonym"));
        expected.add(item);

        item = itemFactory.makeItem();
        item.setIdentifier("1_1");
        item.setClassName(NAMESPACE + "OntologyTermSynonym");
        item.setImplementations("");
        item.addAttribute(new Attribute("name", "syn2"));
        item.addAttribute(new Attribute("type", "exact_synonym"));
        expected.add(item);

        System.out.println(printCompareItemSets(expected, itemWriter.getItems()));
        assertEquals(expected, itemWriter.getItems());
    }

    /**
     * If given expected and actual item sets differ return a string detailing items in expected
     * and not in actual and in actual but not expected.
     * @param expected the expected set of org.intermine.xml.full.Items
     * @param actual the actual set of org.intermine.xml.full.Items
     * @return the differences between the to
     */
    public String printCompareItemSets(Set expected, Set actual) {
        String expectedNotActual = "in expected, not actual: " + compareItemSets(expected, actual);
        String actualNotExpected = "in actual, not expected: " + compareItemSets(actual, expected);

        //if ((expectedNotActual.length() > 27) || (actualNotExpected.length() > 27)) {
            return expectedNotActual + System.getProperty("line.separator") + actualNotExpected;
        //}
        //return "";
    }

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
