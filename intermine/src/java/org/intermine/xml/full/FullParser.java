package org.flymine.xml.full;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.apache.commons.digester.Digester;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.xml.sax.SAXException;

import org.flymine.util.TypeUtil;
import org.flymine.util.DynamicUtil;
import org.flymine.util.StringUtil;
import org.flymine.metadata.Model;

/**
 * Unmarshal XML Full format data into java business objects.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public class FullParser
{
    private static Log log = LogFactory.getLog(FullParser.class);

    /**
     * Parse a FlyMine Full XML file
     *
     * @param is the InputStream to parse
     * @return a list of Items
     * @throws SAXException if there is an error in the XML file
     * @throws IOException if there is an error reading the XML file
     * @throws ClassNotFoundException if a class cannot be found
     */
    public static List parse(InputStream is)
        throws IOException, SAXException, ClassNotFoundException {

        if (is == null) {
            throw new NullPointerException("Parameter 'is' cannot be null");
        }

        Digester digester = new Digester();
        digester.setValidating(false);
        digester.setLogger(log);

        digester.addObjectCreate("items", ArrayList.class);

        digester.addObjectCreate("items/item", Item.class);
        digester.addSetProperties("items/item", new String[]{"id", "class", "implements"},
                                  new String[] {"identifier", "className", "implementations"});
        digester.addSetNext("items/item", "add");

        digester.addObjectCreate("items/item/attribute", Attribute.class);
        digester.addSetProperties("items/item/attribute");
        digester.addSetNext("items/item/attribute", "addAttribute");

        digester.addObjectCreate("items/item/reference", Reference.class);
        digester.addSetProperties("items/item/reference", "ref_id", "refId");
        digester.addSetNext("items/item/reference", "addReference");

        digester.addObjectCreate("items/item/collection", ReferenceList.class);
        digester.addSetProperties("items/item/collection");
        digester.addCallMethod("items/item/collection/reference", "addRefId", 1);
        digester.addCallParam("items/item/collection/reference", 0, "ref_id");
        digester.addSetNext("items/item/collection", "addCollection");

        return (List) digester.parse(is);
    }

    /**
     * Create business objects from a collection of Items.
     * @param items a collection of items to realise
     * @param model the parent model
     * @return a collection of realised business objects
     * @throws ClassNotFoundException if invalid item className found
     */
    public static List realiseObjects(Collection items, Model model)
        throws ClassNotFoundException {
        Map objMap = new LinkedHashMap(); // map from id to outline object

        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            Item item = (Item) iter.next();
            objMap.put(item.getIdentifier(), realiseObject(item, model));
        }

        iter = items.iterator();
        while (iter.hasNext()) {
            populateObject((Item) iter.next(), objMap);
        }

        List result = new ArrayList();
        Iterator i = objMap.keySet().iterator();
        while (i.hasNext()) {
            result.add(objMap.get(i.next()));
        }
        return result;
    }

    /**
     * Create an outline business object from an Item, does not fill in fields.
     * @param item a the Item to realise
     * @param model the parent model
     * @return the materialised business object
     * @throws ClassNotFoundException if item className is not valid
     */
    protected static Object realiseObject(Item item, Model model) throws ClassNotFoundException {
        Set classes = new HashSet();
        if (item.getClassName() != null && !item.getClassName().equals("")) {
            classes.add(generateClass(item.getClassName(), model));
        }
        if (item.getImplementations() != null) {
            for (Iterator i =  StringUtil.tokenize(item.getImplementations()).iterator();
                 i.hasNext();) {
                classes.add(generateClass((String) i.next(), model));
            }
        }
        return DynamicUtil.createObject(classes);
    }

    /**
     * Fill in fields of an outline business object.
     * @param item a the Item to read field data from
     * @param objMap a map of item identifiers to outline business objects
     */
    protected static void populateObject(Item item, Map objMap) {
        Object obj = objMap.get(item.getIdentifier());

        try {
            // Set the data for every given attribute except id
            Iterator attrIter = item.getAttributes().iterator();
            while (attrIter.hasNext()) {
                Attribute attr = (Attribute) attrIter.next();
                Class attrClass = TypeUtil.getFieldInfo(obj.getClass(), attr.getName()).getType();
                if (!attr.getName().equalsIgnoreCase("id")) {
                    TypeUtil.setFieldValue(obj, attr.getName(),
                                           TypeUtil.stringToObject(attrClass, attr.getValue()));
                }
            }

            // Set the data for every given reference
            Iterator refIter = item.getReferences().iterator();
            while (refIter.hasNext()) {
                Reference ref = (Reference) refIter.next();
                Object refObj = objMap.get(ref.getRefId());
                TypeUtil.setFieldValue(obj, ref.getName(), refObj);
            }

            // Set objects for every collection
            Iterator colIter = item.getCollections().iterator();
            while (colIter.hasNext()) {
                ReferenceList refList = (ReferenceList) colIter.next();
                Collection col = (Collection) TypeUtil.getFieldValue(obj, refList.getName());
                for (Iterator i = refList.getRefIds().iterator(); i.hasNext();) {
                    col.add(objMap.get(i.next()));
                }
            }
        } catch (IllegalAccessException e) {
        }
    }

    /**
     * Create a class given a namspace qualified string, if class is not in the
     * the given model looks for core classes in org.flymine.model package.
     * @param a namespace qualified class name
     * @param model the parent model
     * @throws ClassNotFoundException if invalid class string
     */
    private static Class generateClass(String namespacedClass, Model model)
        throws ClassNotFoundException {
        String localName = namespacedClass.substring(namespacedClass.indexOf("#") + 1);
        Class cls;
        try {
            cls = Class.forName(model.getPackageName() + "." + localName);
        } catch (ClassNotFoundException e) {
            cls = Class.forName("org.flymine.model." + localName);
        }
        return cls;
    }
}
