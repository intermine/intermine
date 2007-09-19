package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.intermine.dataconversion.OntologyUtil;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.util.SAXParser;
import org.intermine.util.TypeUtil;

import java.io.InputStream;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;

/**
 * Unmarshal XML Full format data into java business objects.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public class FullParser
{    
    private static final Logger LOG = Logger.getLogger(FullParser.class);
    
    /**
     * Parse a InterMine Full XML file
     *
     * @param is the InputStream to parse
     * @return a list of Items
     * @throws Exception if there is an error while parsing
     */
    public static List parse(InputStream is)
        throws Exception {

        if (is == null) {
            throw new NullPointerException("Parameter 'is' cannot be null");
        }

        FullHandler handler = new FullHandler();
        SAXParser.parse(new InputSource(is), handler);

        return handler.getItems();
    }

    /**
     * Create business objects from a collection of Items.  If there are any problems, throw an
     * exception
     * @param items a collection of items to realise
     * @param model the parent model
     * @param useIdentifier if true, set the id of each new object using the identifier of the Item
     * problem and continue if possible
     * @return a collection of realised business objects
     * @throws ClassNotFoundException if one of the items has a class that isn't in the model 
     */
    public static List<InterMineObject> realiseObjects(Collection<Item> items, Model model,
                                                       boolean useIdentifier)
        throws ClassNotFoundException {
        return realiseObjects(items, model, useIdentifier, true);
    }

    /**
     * Create business objects from a collection of Items.
     * @param items a collection of items to realise
     * @param model the parent model
     * @param useIdentifier if true, set the id of each new object using the identifier of the Item
     * @param abortOnError if true, throw an exception if there is a problem.  If false, log the
     * problem and continue if possible
     * @return a collection of realised business objects
     * @throws ClassNotFoundException if one of the items has a class that isn't in the model 
     */
    public static List<InterMineObject> realiseObjects(Collection<Item> items, Model model,
                                                       boolean useIdentifier, boolean abortOnError)
        throws ClassNotFoundException {
        Map objMap = new LinkedHashMap(); // map from id to outline object

        for (Iterator<Item> i = items.iterator(); i.hasNext();) {
            Item item = i.next();
            objMap.put(item.getIdentifier(),
                DynamicUtil.instantiateObject(
                    OntologyUtil.generateClassNames(item.getClassName(),
                                                    model),
                    OntologyUtil.generateClassNames(item.getImplementations(),
                                                    model)));
        }

        List result = new ArrayList();
        for (Iterator i = items.iterator(); i.hasNext();) {
            result.add(populateObject((Item) i.next(), objMap, useIdentifier, abortOnError));
        }

        return result;
    }

    /**
     * Fill in fields of an outline business object which is in the map under item.identifier
     * Note that this modifies the relevant object in the map
     * It also returns the object for convenience
     * @param item a the Item to read field data from
     * @param objMap a map of item identifiers to outline business objects
     * @param useIdentifier if true, set the id of the new object using the identifier of the Item
     * @param abortOnError if true, throw an exception if there is a problem.  If false, log the
     * problem and continue if possible
     * @return a populated object
     */
    protected static Object populateObject(Item item, Map objMap, boolean useIdentifier,
                                           boolean abortOnError) {
        Object obj = objMap.get(item.getIdentifier());

        try {
            // Set the data for every given attribute except id
            Iterator attrIter = item.getAttributes().iterator();
            while (attrIter.hasNext()) {
                Attribute attr = (Attribute) attrIter.next();
                TypeUtil.FieldInfo info = TypeUtil.getFieldInfo(obj.getClass(), attr.getName());
                if (info == null) {
                    String message = "Field " + attr.getName()
                        + " not found in " + DynamicUtil.decomposeClass(obj.getClass());
                    if (abortOnError) {
                        throw new IllegalArgumentException(message);
                    } else {
                        LOG.warn(message);
                        continue;
                    }
                }
                Class attrClass = info.getType();
                if (!attr.getName().equalsIgnoreCase("id")) {
                    TypeUtil.setFieldValue(obj, attr.getName(),
                                           TypeUtil.stringToObject(attrClass, attr.getValue()));
                }
            }

            if (useIdentifier) {
                TypeUtil.setFieldValue(obj, "id", TypeUtil.stringToObject(Integer.class,
                                                                          item.getIdentifier()));
            }

            // Set the data for every given reference
            Iterator refIter = item.getReferences().iterator();
            while (refIter.hasNext()) {
                Reference ref = (Reference) refIter.next();
                Object refObj = objMap.get(ref.getRefId());
                if (refObj == null) {
                    LOG.warn("no field " + ref.getName() + " in object: " + obj);
                } else {
                    TypeUtil.setFieldValue(obj, ref.getName(), refObj);
                }
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
            // ignore
        }

        return obj;
    }
}
