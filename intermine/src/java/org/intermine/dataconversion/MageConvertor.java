package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.lang.reflect.Method;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.InputStream;

import org.biomage.tools.xmlutils.MAGEReader;

import org.flymine.model.fulldata.Attribute;
import org.flymine.model.fulldata.Identifier;
import org.flymine.model.fulldata.Item;
import org.flymine.model.fulldata.Reference;
import org.flymine.model.fulldata.ReferenceList;
import org.flymine.util.TypeUtil;


/**
 * Convert MAGE-ML to FlyMine Full Data Xml via MAGE-OM objects.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */

public class MageConvertor
{
    protected LinkedHashMap seenMap;
    private int identifier = 0;

    /**
     * Read a MAGE-ML file and convert to Item objects.
     * @param is InputStream of MAGE-ML data
     * @param ns namespace of objects in target xml file
     * @return a collection of Item objects
     * @throws Exception if io or reflection problems occur
     */
    public Collection convertMageML(InputStream is, String ns) throws Exception {
        File f = File.createTempFile("mageconvert", ".xml");
        try {
            FileWriter writer = new FileWriter(f);
            Reader reader = new BufferedReader(new InputStreamReader(is));
            int c;
            while ((c = reader.read()) > 0) {
                writer.write(c);
            }
            writer.close();
            MAGEReader mageReader = new MAGEReader(f.getPath());
            seenMap = new LinkedHashMap();
            identifier = 0;
            createItem(mageReader.getMAGEobj(), ns);
        } finally {
            f.delete();
        }
        return seenMap.values();
    }

    /**
     * Create an item and associated fieldsm references anc collections
     * given a MAGE object.
     * @param obj a MAGE object to create items for
     * @param ns namespace of objects in target xml file
     * @return the created item
     * @throws Exception if reflection problems occur
     */
    protected Item createItem(Object obj, String ns) throws Exception {
        if (seenMap.containsKey(obj)) {
            return (Item) seenMap.get(obj);
        }

        Class cls = obj.getClass();

        Item item = new Item();
        item.setClassName(ns + TypeUtil.unqualifiedName(cls.getName()));

        if (!cls.getName().equals("org.biomage.Common.MAGEJava")) {
            item.setIdentifier(newIdentifier(nextIdentifier().toString()));
            seenMap.put(obj, item);
        }

        Iterator iter = TypeUtil.getFieldInfos(cls).values().iterator();
        while (iter.hasNext()) {
            TypeUtil.FieldInfo info = (TypeUtil.FieldInfo) iter.next();
            Method m = info.getGetter();
            if (m.getParameterTypes().length == 0) {
                Object value = m.invoke(obj, null);
                if (value != null) {
                    if (Collection.class.isAssignableFrom(m.getReturnType())) {
                        // collection
                        ReferenceList ref = new ReferenceList();
                        ref.setName(info.getName());
                        Iterator refIter = ((Collection) value).iterator();
                        if (refIter.hasNext()) {
                            while (refIter.hasNext()) {
                                ref.addIdentifiers(createItem(refIter.next(), ns)
                                                   .getIdentifier());
                            }
                            item.addCollections(ref);
                        }
                    } else if (m.getReturnType().getName().startsWith("org.biomage")) {
                        if (m.getReturnType().getName().startsWith(cls.getName() + "$")) {
                            //attribute
                            Attribute attr = new Attribute();
                            attr.setName(info.getName());
                            Method getName = value.getClass().getMethod("getName", null);
                            attr.setValue((String) getName.invoke(value, null));
                            item.addAttributes(attr);
                        } else {
                            //reference
                            Reference ref = new Reference();
                            ref.setName(info.getName());
                            ref.setIdentifier(createItem(value, ns).getIdentifier());
                            item.addReferences(ref);
                        }
                    } else {
                        // attribute
                        Attribute attr = new Attribute();
                        attr.setName(info.getName());
                        attr.setValue(value.toString());
                        item.addAttributes(attr);
                        // TODO handle dates?
                    }
                }
            }
        }
        return item;
    }

    /**
     * Get the next identifier number in sequence.
     * @return the next number in sequence
     */
    protected Integer nextIdentifier() {
        return new Integer(++identifier);
    }

    private Identifier newIdentifier(String value) {
        Identifier id = new Identifier();
        id.setValue(value);
        return id;
    }
}
