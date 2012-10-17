package org.intermine.api.config;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

/**
 * Methods to read and manage keys for classes. Keys define how certain classes
 * are identified and are used in defining bag creation.
 *
 * @author rns
 *
 */
public final class ClassKeyHelper
{
    private ClassKeyHelper() {
        // don't instantiate
    }

    private static final Logger LOG = Logger.getLogger(ClassKeyHelper.class);

    private static final Map<Model, Map<String, List<FieldDescriptor>>> classKeys =
        new HashMap<Model, Map<String, List<FieldDescriptor>>>();

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
    public static Map<String, List<FieldDescriptor>> readKeys(Model model, Properties props) {
        if (!classKeys.containsKey(model)) {
            Map<String, List<FieldDescriptor>> theseKeys = new HashMap<String, List<FieldDescriptor>>();
            for (ClassDescriptor cld : model.getTopDownLevelTraversal()) {
                String clsName = cld.getUnqualifiedName();
                if (props.containsKey(cld.getUnqualifiedName())) {
                    String keys = (String) props.get(clsName);
                    String[] tokens = keys.split(",");
                    for (String token : tokens) {
                        String keyString = token.trim();
                        FieldDescriptor fld = cld.getFieldDescriptorByName(keyString);
                        if (fld != null) {
                            ClassKeyHelper.addKey(theseKeys, clsName, fld);
                            for (ClassDescriptor subCld : model.getAllSubs(cld)) {
                                ClassKeyHelper.addKey(theseKeys, subCld.getUnqualifiedName(), fld);
                            }
                        } else {
                            LOG.warn("problem loading class key: " + keyString
                                    + " for class " + clsName);
                        }
                    }
                } else {
                    LOG.warn("No key defined for " + clsName);
                }
                classKeys.put(model, theseKeys);
            }
        }
        return classKeys.get(model);
    }

    /**
     * Add a key to set of keys for a given class.
     *
     * @param classKeys
     *            existing map of classname to set of keys
     * @param clsName
     *            class name for key
     * @param key
     *            a FieldDescriptor that describes the key
     */
    protected static void addKey(Map<String, List<FieldDescriptor>> classKeys, String clsName,
            FieldDescriptor key) {
        List<FieldDescriptor> keyList = classKeys.get(clsName);
        if (keyList == null) {
            keyList = new ArrayList<FieldDescriptor>();
            classKeys.put(clsName, keyList);
        }
        if (!keyList.contains(key)) {
            keyList.add(key);
        }
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
    public static boolean isKeyField(Map<String, List<FieldDescriptor>> classKeys, String clsName,
            String fieldName) {
        String className = TypeUtil.unqualifiedName(clsName);

        List<FieldDescriptor> keys = classKeys.get(className);
        if (keys != null) {
            for (FieldDescriptor key : keys) {
                if (key.getName().equals(fieldName) && key.isAttribute()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * For a given classreturn true if it has any identifying fields. An
     * identifying field is an attribute (not a reference or collection) of the
     * class that is part of any key defined for that class.
     *
     * @param classKeys
     *            map of classname to set of keys
     * @param clsName
     *            the class name to look up
     * @return true if the class has any key fields
     */
    public static boolean hasKeyFields(Map<String, List<FieldDescriptor>> classKeys,
            String clsName) {
        String className = TypeUtil.unqualifiedName(clsName);

        List<FieldDescriptor> keys = classKeys.get(className);
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
    public static List<FieldDescriptor> getKeyFields(Map<String, List<FieldDescriptor>> classKeys,
            String clsName) {
        String className = TypeUtil.unqualifiedName(clsName);

        return classKeys.get(className);
    }


    /**
     * Return names of the key fields for a given class.
     * @param classKeys map of classname to set of keys
     * @param clsName the class name to look up
     * @return the names of fields that are class keys for the class
     */
    public static List<String> getKeyFieldNames(Map<String, List<FieldDescriptor>> classKeys,
            String clsName) {
        String className = TypeUtil.unqualifiedName(clsName);

        List<String> fieldNames = new ArrayList<String>();
        List<FieldDescriptor> keys = classKeys.get(className);
        if (keys != null) {
            for (FieldDescriptor key : keys) {
                fieldNames.add(key.getName());
            }
        }
        return fieldNames;
    }

    /**
     * Get a key field value for the given object.  This will return null if there are no key fields
     * for the object's class or if there are no non-null values for the key fields.  The key fields
     * are kept in a consistent order with inherited fields appearing before those defined in a
     * subclass.
     * @param obj an object from the model
     * @param classKeys the key field definition for this model
     * @return the first available key field value or null
     */
    public static Object getKeyFieldValue(FastPathObject obj,
            Map<String, List<FieldDescriptor>> classKeys) {
        String clsName = DynamicUtil.getSimpleClass(obj).getSimpleName();

        try {
            for (String keyField : getKeyFieldNames(classKeys, clsName)) {
                Object valueFromObject = obj.getFieldValue(keyField);
                if (valueFromObject != null) {
                    return valueFromObject;
                }
            }
        } catch (IllegalAccessException e) {
            // this shouldn't happen because objects conform to the model
            LOG.error("Error fetching a key field value from object: " + obj);
        }
        return null;
    }
}
