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

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;

import org.intermine.xml.full.ItemHelper;

import org.intermine.model.fulldata.Attribute;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Reference;
import org.intermine.model.fulldata.ReferenceList;

public class ItemPrefetchConstraintDynamicTest extends TestCase
{

    public void testGetConstraint() throws Exception {
        // near field is identifier
        ItemPrefetchConstraintDynamic ipcd1 = new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "anything");
        Item i1 = ItemHelper.createFulldataItem("1_0", "class1", "");
        FieldNameAndValue expected1 = new FieldNameAndValue("anything", "1_0", true);
        assertEquals(expected1, ipcd1.getConstraint(i1));
        assertEquals(new HashMap(), ipcd1.idToFnavs);

        // near field is classname
        ItemPrefetchConstraintDynamic ipcd2 = new ItemPrefetchConstraintDynamic(ObjectStoreItemPathFollowingImpl.CLASSNAME, "anything");
        FieldNameAndValue expected2 = new FieldNameAndValue("anything", "class1", false);
        assertEquals(expected2, ipcd2.getConstraint(i1));
        assertEquals(new HashMap(), ipcd2.idToFnavs);

        // far field is identifier, reference
        ItemPrefetchConstraintDynamic ipcd3 = new ItemPrefetchConstraintDynamic("ref1", ObjectStoreItemPathFollowingImpl.IDENTIFIER);
        Reference r1 = ItemHelper.createFulldataReference("ref1", "2_0");
        i1.addReferences(r1);
        FieldNameAndValue expected3 = new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "2_0", false);
        assertEquals(expected3, ipcd3.getConstraint(i1));
        Map idToFnavs3 = new HashMap();
        idToFnavs3.put("2_0", new HashSet(Collections.singleton(expected3)));
        assertEquals(idToFnavs3, ipcd3.idToFnavs);

        // far field is identifier, collection
        ItemPrefetchConstraintDynamic ipcd4 = new ItemPrefetchConstraintDynamic("col1", ObjectStoreItemPathFollowingImpl.IDENTIFIER);
        ReferenceList c1 = ItemHelper.createFulldataReferenceList("col1", "2_0 2_1");
        i1.addCollections(c1);
        // why is isReference set to true?
        FieldNameAndValue expected4 = new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "2_0 2_1", true);
        assertEquals(expected4, ipcd4.getConstraint(i1));
        Map idToFnavs4 = new HashMap();
        idToFnavs4.put("2_0", new HashSet(Collections.singleton(expected4)));
        idToFnavs4.put("2_1", new HashSet(Collections.singleton(expected4)));
        assertEquals(idToFnavs4, ipcd4.idToFnavs);

        // attribute
        ItemPrefetchConstraintDynamic ipcd5 = new ItemPrefetchConstraintDynamic("att1", "farFieldName");
        Attribute a1 = ItemHelper.createFulldataAttribute("att1", "value");
        i1.addAttributes(a1);
        FieldNameAndValue expected5 = new FieldNameAndValue("farFieldName", "value", false);
        assertEquals(expected5, ipcd5.getConstraint(i1));
        assertEquals(new HashMap(), ipcd5.idToFnavs);
    }
}
