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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Attribute;
import org.intermine.xml.full.ItemHelper;

public class MockItemReader implements ItemReader
{
    Map storedItems;

    public MockItemReader(Map map) {
        storedItems = map;
    }

    public Iterator itemIterator() throws ObjectStoreException {
        return storedItems.values().iterator();
    }

    public Item getItemById(String objectId) throws ObjectStoreException {
        return (Item) storedItems.get(objectId);
    }

    public Iterator getItemsByAttributeValue(String className, String fieldName, String value) throws ObjectStoreException {
        Set items = new LinkedHashSet();
        Iterator i = itemIterator();
        while (i.hasNext()) {
            Item item = (Item) i.next();
            if (item.getClassName().equals(className) || getImps(item).contains(className)) {
                if (getAttribute(item, fieldName) != null && getAttribute(item, fieldName).getValue().equals(value)) {
                    items.add(item);
                }
            }
        }
        return items.iterator();
    }

    private Set getImps(Item item) {
        Set imps = new HashSet();
        imps.addAll(Arrays.asList(item.getImplementations().split(" ")));
        return imps;
    }

    private Attribute getAttribute(Item item, String f) {
        Iterator i = item.getAttributes().iterator();
        while (i.hasNext()) {
            Attribute a = (Attribute) i.next();
            if (a.getName().equals(f)) {
                return a;
            }
        }
        return null;
    }

    public List getItemsByDescription(Set constraints) throws ObjectStoreException {
        List items = new ArrayList();

        Iterator i = itemIterator();
        while (i.hasNext()) {
            Item item = (Item) i.next();



            boolean matches = true;
            Iterator descIter = constraints.iterator();
            while (descIter.hasNext()) {
                FieldNameAndValue f = (FieldNameAndValue) descIter.next();
                if (!f.matches(item)) {
                    matches = false;
                }
//                 System.out.println(f);
//                 if (!f.isReference()) {
//                     if (!item.hasAttribute(f.getFieldName())
//                         || !item.getAttribute(f.getFieldName()).getValue().equals(f.getValue())) {

//                         matches = false;
//                     }
//                 } else {
//                     if (!item.hasReference(f.getFieldName())
//                         || !item.getReference(f.getFieldName()).getRefId().equals(f.getValue())) {
//                         matches = false;
//                     }
//                 }
            }
            if (matches) {
                items.add(item);
            }
        }
        return items;
    }
}
