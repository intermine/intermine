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

import org.intermine.model.fulldata.Item;


public class FieldNameAndValueTest extends TestCase
{
    private FieldNameAndValue fnav;

    public void testMatchesReference() throws Exception {
        fnav = new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "", true);
        assertFalse(fnav.matches(createItem("0_101")));
        fnav = new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "0_101", true);
        assertTrue(fnav.matches(createItem("0_101")));
        fnav = new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "0_101", true);
        assertFalse(fnav.matches(createItem("0_1011")));
        fnav = new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "0_1011", true);
        assertFalse(fnav.matches(createItem("0_101")));
        fnav = new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "0_101 0_102", true);
        assertTrue(fnav.matches(createItem("0_101")));
        fnav = new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "0_101 0_102", true);
        assertTrue(fnav.matches(createItem("0_102")));
        fnav = new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "0_101 0_102 0_103", true);
        assertTrue(fnav.matches(createItem("0_102")));
        fnav = new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "0_101 0_102", true);
        assertFalse(fnav.matches(createItem("0_103")));
        fnav = new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "0_101", true);
        assertFalse(fnav.matches(createItem("0_102")));
    }

    private Item createItem(String identifier) {
        Item item = new Item();
        item.setClassName("classname");
        item.setIdentifier(identifier);
        return item;
    }
}
