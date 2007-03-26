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
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.ontology.DagTerm;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

public class DagConverterTest extends TestCase {
    String NAMESPACE = "http://www.flymine.org/model/genomic#";
    MockItemWriter itemWriter;
    private ItemFactory itemFactory = new ItemFactory();

    public void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap());
    }

    public void test1() throws Exception {
        DagConverter converter = new DagConverter(itemWriter, "", "SO", "http://www.flymine.org",
                                                  NAMESPACE + "OntologyTerm");
        DagTerm a = new DagTerm("SO:42", "parent");
        DagTerm b = new DagTerm("SO:43", "child");
        DagTerm c = new DagTerm("SO:44", "partof");
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

        assertEquals(expected, itemWriter.getItems());
    }
}
