package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.util.*;

import com.mockobjects.sql.MockSingleRowResultSet;
import com.mockobjects.sql.MockMultiRowResultSet;

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.ontology.DagTerm;
import org.intermine.sql.DatabaseFactory;
import org.intermine.sql.Database;
import org.intermine.util.TypeUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;

public class DagConverterTest extends TestCase {
    MockItemWriter itemWriter;

    public void setUp() throws Exception {
        itemWriter = new MockItemWriter(new HashMap());
    }

    public void test1() throws Exception {
        DagConverter converter = new DagConverter(itemWriter, "", "namespace#");
        DagTerm a = new DagTerm("SO:42", "large gene");
        DagTerm b = new DagTerm("SO:43", "small gene");
        converter.process(a);
        converter.process(b);
        Set expected = new HashSet();
        Item item = new Item();
        item.setIdentifier("0_0");
        item.setClassName("namespace#LargeGene");
        item.setImplementations("");
        Attribute attribute = new Attribute();
        attribute.setName("label");
        attribute.setValue("large gene");
        item.addAttribute(attribute);
        attribute = new Attribute();
        attribute.setName("ID");
        attribute.setValue("SO:42");
        item.addAttribute(attribute);
        expected.add(item);
        item = new Item();
        item.setIdentifier("0_1");
        item.setClassName("namespace#SmallGene");
        item.setImplementations("");
        attribute = new Attribute();
        attribute.setName("label");
        attribute.setValue("small gene");
        item.addAttribute(attribute);
        attribute = new Attribute();
        attribute.setName("ID");
        attribute.setValue("SO:43");
        item.addAttribute(attribute);
        expected.add(item);
        assertEquals(expected, itemWriter.getItems());
    }
}
