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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.lang.reflect.Method;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.Writer;

import org.biomage.tools.xmlutils.MAGEReader;

import org.intermine.model.fulldata.Attribute;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Reference;
import org.intermine.model.fulldata.ReferenceList;
import org.intermine.util.TypeUtil;
import org.intermine.dataconversion.ItemWriter;

/**
 * Convert MAGE-ML to InterMine Full Data Xml via MAGE-OM objects.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public class MageConverter extends FileConverter
{
    protected static final String MAGE_NS = "http://www.biomage.org#";

    protected HashMap seenMap;
    protected int id = 0;

    /**
     * @see FileConverter#FileConverter
     */
    public MageConverter(BufferedReader reader, ItemWriter writer) {
        super(reader, writer);
    }
        
    /**
     * Read a MAGE-ML file and convert to Item objects.
     * @throws Exception if io or reflection problems occur
     */
    public void process() throws Exception {
        seenMap = new LinkedHashMap();
        id = 0;
        File f = File.createTempFile("mageconvert", ".xml");
        try {
            Writer fileWriter = new FileWriter(f);
            int c;
            while ((c = reader.read()) > 0) {
                fileWriter.write(c);
            }
            fileWriter.close();
            MAGEReader mageReader = new MAGEReader(f.getPath());
            seenMap = new LinkedHashMap();
            createItem(mageReader.getMAGEobj());
        } finally {
            f.delete();
        }
        writer.storeAll(seenMap.values());
    }

    /**
     * Create an item and associated fieldsm references anc collections
     * given a MAGE object.
     * @param obj a MAGE object to create items for
     * @return the created item
     * @throws Exception if reflection problems occur
     */
    protected Item createItem(Object obj) throws Exception {
        if (seenMap.containsKey(obj)) {
            return (Item) seenMap.get(obj);
        }

        Class cls = obj.getClass();
        String className = TypeUtil.unqualifiedName(cls.getName());

        Item item = new Item();
        item.setClassName(MAGE_NS + className);
        item.setImplementations("");

        if (!cls.getName().equals("org.biomage.Common.MAGEJava")) {
            item.setIdentifier(alias(className) + "_" + (id++));
            seenMap.put(obj, item);
        }
        
        for (Iterator i = TypeUtil.getFieldInfos(cls).values().iterator(); i.hasNext();) {
            TypeUtil.FieldInfo info = (TypeUtil.FieldInfo) i.next();
            Method m = info.getGetter();
            if (m.getParameterTypes().length == 0) {
                Object value = m.invoke(obj, null);
                if (value != null) {
                    if (Collection.class.isAssignableFrom(m.getReturnType())) {
                        // collection
                        ReferenceList refs = new ReferenceList();
                        refs.setName(info.getName());
                        StringBuffer sb = new StringBuffer();
                        for (Iterator j = ((Collection) value).iterator(); j.hasNext();) {
                            sb.append(createItem(j.next()).getIdentifier() + " ");
                        }
                        if (sb.length() > 0) {
                            refs.setRefIds(sb.toString().trim());
                            item.addCollections(refs);
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
                            ref.setRefId(createItem(value).getIdentifier());
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
}
