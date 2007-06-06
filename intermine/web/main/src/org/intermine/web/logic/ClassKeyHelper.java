package org.intermine.web.logic;

/* 
 * Copyright (C) 2002-2007 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

/**
 * Methods to read and manage keys for classes. Keys define how certain classes
 * are identified and are used in defining bag creation.
 * 
 * @author rns
 * 
 */
public class ClassKeyHelper 
{
    private static final Logger LOG = Logger.getLogger(ClassKeyHelper.class);

    /**
     * Read class keys from a properties into a map from classname to set of
     * available keys.
     * 
     * @param model
     *            the data model
     * @param props
     *            a properties object describing class keys
     * @return map from class name to set of available keys
     */
    public static Map readKeys(Model model, Properties props) {
        Map classKeys = new HashMap();
        for (Iterator i = props.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String clsName = (String) entry.getKey();
            String pkg = model.getPackageName();
            ClassDescriptor cld = model.getClassDescriptorByName(pkg + "."
                    + clsName);
            if (cld != null) {
                String keys = (String) entry.getValue();
                String[] tokens = keys.split(",");
                for (int o = 0; o < tokens.length; o++) {
                    String keyString = tokens[o].trim();
                    String[] keyBits = keyString.split(" ");
                    Set keyFields = new HashSet();
                    for (int j = 0; j < keyBits.length; j++) {
                        String keyBit = keyBits[j];
                        FieldDescriptor fld = cld
                                .getFieldDescriptorByName(keyBit);
                        if (fld != null) {
                            keyFields.add(fld);
                        } else {
                            LOG.warn("problem loading class key: " + keyString
                                    + " for class " + clsName);
                        }
                    }
                    if (keyFields.size() == keyBits.length) {
                        ClassKeyHelper.addKey(classKeys, clsName, keyFields);
                        // now add this key to all subclasses
                        Iterator subIter = model.getAllSubs(cld).iterator();
                        while (subIter.hasNext()) {
                            ClassKeyHelper.addKey(classKeys, TypeUtil
                                    .unqualifiedName(((ClassDescriptor) subIter
                                            .next()).getName()), keyFields);
                        }
                    }
                }
            } else {
                LOG.warn("key defined for class '" + clsName
                        + "' but class not found in model");
            }
        }
        return classKeys;
    }

    /**
     * Add a key to set of keys for a given class.
     * 
     * @param classKeys
     *            existing map of classname to set of keys
     * @param clsName
     *            class name for key
     * @param key
     *            set of FieldDescriptors that describe the key
     */
    protected static void addKey(Map classKeys, String clsName, Set key) {
        Set keySet = (Set) classKeys.get(clsName);
        if (keySet == null) {
            keySet = new HashSet();
            classKeys.put(clsName, keySet);
        }
        keySet.add(key);
    }

    /**
     * For a given class/field return true if it is an 'identifying' field. An
     * identifying field is an attribute (not a reference or collection) of the
     * class that is part of any key defined for that class.
     * 
     * @param classKeys
     *            map of classname to set of keys
     * @param clsName
     *            the class name to look up
     * @param fieldName
     *            the field name to look up
     * @return true if the field is an 'identifying' field for the class.
     */
    public static boolean isKeyField(Map<String, Set> classKeys, String clsName,
                                     String fieldName) {
        if (clsName.indexOf('.') != -1) {
            clsName = TypeUtil.unqualifiedName(clsName);
        }
        Set keys = classKeys.get(clsName);
        if (keys != null) {
            Iterator i = keys.iterator();
            while (i.hasNext()) {
                Set key = (Set) i.next();
                Iterator j = key.iterator();
                while (j.hasNext()) {
                    FieldDescriptor fld = (FieldDescriptor) j.next();
                    if (fld.getName().equals(fieldName) && fld.isAttribute()) {
                        return true;
                    }

                }
            }
        }
        return false;
    }

    /**
     * For a given classreturn true if it has any identifying fields.i An
     * identifying field is an attribute (not a reference or collection) of the
     * class that is part of any key defined for that class.
     * 
     * @param classKeys
     *            map of classname to set of keys
     * @param clsName
     *            the class name to look up
     * @return true if the class has any key fields
     */
    public static boolean hasKeyFields(Map<String, Set> classKeys, String clsName) {
        if (clsName.indexOf('.') != -1) {
            clsName = TypeUtil.unqualifiedName(clsName);
        }
        Set keys = (Set) classKeys.get(clsName);
        if (keys != null && (keys.size() > 0)) {
            return true;
        }
        return false;
    }

    /**
     * Return the key fields of a given class.
     * @param classKeys map of classname to set of keys
     * @param clsName the class name to look up
     * @return the fields that are class keys for the class
     */
    public static Collection getKeyFields(Map<String, Set> classKeys, String clsName) {
        if (clsName.indexOf('.') != -1) {
            clsName = TypeUtil.unqualifiedName(clsName);
        }
        return (Collection) classKeys.get(clsName);
    }

    
    /**
     * Return names of the key fields for a given class.
     * @param classKeys map of classname to set of keys
     * @param clsName the class name to look up
     * @return the names of fields that are class keys for the class
     */
    public static Collection<String> getKeyFieldNames(Map<String, Set> classKeys, String clsName) {
        if (clsName.indexOf('.') != -1) {
            clsName = TypeUtil.unqualifiedName(clsName);
        }
        Set fieldNames = new HashSet();
        Set keys = classKeys.get(clsName);
        if (keys != null) {
            Iterator i = keys.iterator();
            while (i.hasNext()) {
                Set key = (Set) i.next();
                Iterator j = key.iterator();
                while (j.hasNext()) {
                    fieldNames.add(((FieldDescriptor) j.next()).getName());
                }
            }
        }
        return fieldNames;
    }
    
    
    /**
     * For a given object/field return true if it is an 'identifying' field. An
     * identifying field is an attribute (not a reference or collection) of the
     * class that is part of any key defined for that class.
     * 
     * @param classKeys
     *            map of classname to set of keys
     * @param o
     *            the object to check
     * @param fieldName
     *            the field name to look up
     * @return true if the field is an 'identifying' field for one of the
     *         classes that the object is
     */
    public static boolean isKeyField(Map classKeys, InterMineObject o,
            String fieldName) {
        return getKeyFieldClass(classKeys, o, fieldName) != null;
    }

    /**
     * For a given object/field name combination, if the field is a key field of
     * one of the Class that the object is, return that Class.
     * 
     * @param classKeys
     *            map of classname to set of keys
     * @param o
     *            object to check
     * @param fieldName
     *            the field name to look up
     * @return the Class that fieldName is a key field in, otherwise null
     */
    public static Class getKeyFieldClass(Map classKeys, InterMineObject o,
            String fieldName) {
        Set classes = DynamicUtil.decomposeClass(o.getClass());

        Iterator iter = classes.iterator();
        while (iter.hasNext()) {
            Class c = (Class) iter.next();
            if (isKeyField(classKeys, c.getName(), fieldName)) {
                return c;
            }
        }

        return null;
    }
}
