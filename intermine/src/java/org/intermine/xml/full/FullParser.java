package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2005 FlyMine
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

import org.apache.commons.digester.Digester;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.xml.sax.SAXException;

import org.intermine.ontology.OntologyUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.metadata.Model;

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
     * Parse a InterMine Full XML file
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

        for (Iterator i = items.iterator(); i.hasNext();) {
            Item item = (Item) i.next();
            objMap.put(item.getIdentifier(),
                DynamicUtil.instantiateObject(
                    OntologyUtil.generateClassNames(item.getClassName(),
                                                    model),
                    OntologyUtil.generateClassNames(item.getImplementations(),
                                                    model)));
        }

        List result = new ArrayList();
        for (Iterator i = items.iterator(); i.hasNext();) {
            result.add(populateObject((Item) i.next(), objMap));
        }

        return result;
    }
    
    /**
     * Fill in fields of an outline business object which is in the map under item.identifier
     * Note that this modifies the relevant object in the map
     * It also returns the object for convenience
     * @param item a the Item to read field data from
     * @param objMap a map of item identifiers to outline business objects
     * @return a populated object
     */
    protected static Object populateObject(Item item, Map objMap) {
        Object obj = objMap.get(item.getIdentifier());

        try {
            // Set the data for every given attribute except id
            Iterator attrIter = item.getAttributes().iterator();
            while (attrIter.hasNext()) {
                Attribute attr = (Attribute) attrIter.next();
                TypeUtil.FieldInfo info = TypeUtil.getFieldInfo(obj.getClass(), attr.getName());
                if (info == null) {
                    throw new IllegalArgumentException("Field " + attr.getName()
                            + " not found in " + DynamicUtil.decomposeClass(obj.getClass()));
                }
                Class attrClass = info.getType();
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
        
        return obj;
    }
}
