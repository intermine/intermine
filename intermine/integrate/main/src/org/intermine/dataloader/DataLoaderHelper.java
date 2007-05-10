package org.intermine.dataloader;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.metadata.PrimaryKey;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.IntToIntMap;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Class providing utility methods to help with primary key and data source priority configuration
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class DataLoaderHelper
{
    private static final Logger LOG = Logger.getLogger(DataLoaderHelper.class);

    protected static Map sourceKeys = new HashMap();
    protected static Map modelDescriptors = new HashMap();
    protected static Set verifiedSources = new HashSet();

    /**
     * Compare the priorities of two sources over a field.
     *
     * @param fd FieldDescriptor for the field
     * @param src1 the first Source
     * @param src2 the second Source
     * @return a positive integer if src1 is of higher priority than src2, a negative integer if
     * src2 is of higher priority than src1 or zero if the sources are equal.
     * @throws IllegalArgumentException if the class is not in the file, or both of the sources
     * are not listed for that class
     */
    public static int comparePriority(FieldDescriptor fd, Source src1, Source src2) {
        return comparePriority(fd, src1, src2, new Integer(1), new Integer(2), null, null, null,
                false, false);
    }

    /**
     * Compare the priorities of two sources over a field.
     *
     * @param fd FieldDescriptor for the field
     * @param src1 the first Source
     * @param src2 the second Source
     * @param value1 the first field value
     * @param value2 the second field value
     * @param iw an IntegrationWriterAbstractImpl
     * @param source the iw's main source
     * @param skelSource the iw's skeleton source
     * @param storeValue1 a boolean meaning value1 is from the source database rather than the
     * production database
     * @param storeValue2 same as storeValue2
     * @return a positive integer if src1 is of higher priority than src2, a negative integer if
     * src2 is of higher priority than src1 or zero if the sources are equal.
     * @throws IllegalArgumentException if the class is not in the file, or both of the sources
     * are not listed for that class
     */
    public static int comparePriority(FieldDescriptor fd, Source src1, Source src2, Object value1,
            Object value2, IntegrationWriterAbstractImpl iw, Source source, Source skelSource,
            boolean storeValue1, boolean storeValue2) {
        if (src1.equals(src2)) {
            return 0;
        }
        if (src1.getName().equals(src2.getName())) {
            if (src1.getSkeleton() && (!src2.getSkeleton())) {
                return -1;
            } else if (src2.getSkeleton() && (!src1.getSkeleton())) {
                return 1;
            } else {
                return 0;
            }
        }
        ClassDescriptor cld = fd.getClassDescriptor();
        String cldName = TypeUtil.unqualifiedName(cld.getName());
        Map descriptorSources = getDescriptors(cld.getModel());
        List srcs = (List) descriptorSources.get(cldName + "." + fd.getName());
        if (srcs == null) {
            srcs = (List) descriptorSources.get(cldName);
        }
        if (srcs != null && srcs.contains(src1.getName()) && srcs.contains(src2.getName())) {
            return srcs.indexOf(src2.getName()) - srcs.indexOf(src1.getName());
        } else if ((value1 == null) && (value2 == null)) {
            return src1.getName().compareTo(src2.getName());
        } else if (value1 == null) {
            return -1;
        } else if (value2 == null) {
            return 1;
        } else {
            try {
                if (storeValue1 && (value1 instanceof InterMineObject)) {
                    if (value1 instanceof ProxyReference) {
                        value1 = ((ProxyReference) value1).getObject();
                    }
                    value1 = iw.store((InterMineObject) value1, source, skelSource,
                            IntegrationWriterAbstractImpl.SKELETON);
                }
                if (storeValue2 && (value2 instanceof InterMineObject)) {
                    if (value2 instanceof ProxyReference) {
                        value2 = ((ProxyReference) value2).getObject();
                    }
                    value2 = iw.store((InterMineObject) value2, source, skelSource,
                            IntegrationWriterAbstractImpl.SKELETON);
                }
            } catch (ObjectStoreException e) {
                throw new RuntimeException(e);
            }
            if (((value1 instanceof InterMineObject) && (value2 instanceof InterMineObject)
                        && (((InterMineObject) value1).getId().equals(((InterMineObject) value2)
                                .getId()))) || value1.equals(value2)) {
                return src1.getName().compareTo(src2.getName());
            } else {
                if (value1 instanceof ProxyReference) {
                    value1 = ((ProxyReference) value1).getObject();
                }
                if (value2 instanceof ProxyReference) {
                    value2 = ((ProxyReference) value2).getObject();
                }
                throw new IllegalArgumentException("Conflicting values for field "
                        + fd.getClassDescriptor().getName() + "." + fd.getName()
                        + " between " + src1.getName() + " (value "
                        + (value1.toString().length() <= 1000 ? value1
                           : value1.toString().subSequence(0, 999)) + ") and "
                        + src2.getName() + " (value "
                        + (value2.toString().length() <= 1000 ? value2
                           : value2.toString().subSequence(0, 999)) + ") and "
                        + "). This field needs configuring in "
                        + cld.getModel().getName() + "_priorities.properties");
            }
        }
    }

    /**
     * Build a map from class and field names to a priority-ordered List of source name Strings.
     *
     * @param model the Model
     * @return the Map
     */
    protected static Map getDescriptors(Model model) {
        Map descriptorSources = null;
        synchronized (modelDescriptors) {
            descriptorSources = (Map) modelDescriptors.get(model);
            if (descriptorSources == null) {
                descriptorSources = new HashMap();
                Properties priorities = PropertiesUtil.loadProperties(model.getName()
                                                                      + "_priorities.properties");
                if (priorities == null) {
                    throw new RuntimeException("Could not load priorities config file "
                            + model.getName() + "_priorities.properties");
                }
                for (Iterator i = priorities.entrySet().iterator(); i.hasNext();) {
                    Map.Entry entry = (Map.Entry) i.next();
                    String descriptorName = (String) entry.getKey();
                    String sourceNames = (String) entry.getValue();
                    List sources = new ArrayList();
                    String[] tokens = sourceNames.split(",");
                    for (int o = 0; o < tokens.length; o++) {
                        String token = tokens[o].trim();
                        sources.add(token);
                    }
                    descriptorSources.put(descriptorName, sources);
                }
                modelDescriptors.put(model, descriptorSources);
            }
        }
        return descriptorSources;
    }


    /**
     * Return a Set of PrimaryKeys relevant to a given Source for a ClassDescriptor. The Set
     * contains all the primary keys that exist on a particular class that are used by the
     * source, without performing any recursion. The Model.getClassDescriptorsForClass()
     * method is recommended if you wish for all the primary keys of the class' parents
     * as well.
     *
     * @param cld the ClassDescriptor
     * @param source the Source
     * @return a Set of PrimaryKeys
     */
    public static Set getPrimaryKeys(ClassDescriptor cld, Source source) {
        Set keySet = new HashSet();
        Properties keys = getKeyProperties(source);
        if (keys != null) {
            if (!verifiedSources.contains(source)) {
                String packageNameWithDot = cld.getName().substring(0, cld.getName()
                        .lastIndexOf('.') + 1);
                LOG.info("Verifying primary key config for source " + source + ", packageName = "
                        + packageNameWithDot);
                Iterator iter = keys.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    String cldName = (String) entry.getKey();
                    String keyList = (String) entry.getValue();
                    ClassDescriptor iCld = cld.getModel().getClassDescriptorByName(
                            packageNameWithDot + cldName);
                    if  (iCld != null) {
                        Map map = PrimaryKeyUtil.getPrimaryKeys(iCld);

                        String[] tokens = keyList.split(",");
                        for (int i = 0; i < tokens.length; i++) {
                            String token = tokens[i].trim();
                            if (map.get(token) == null) {
                                throw new IllegalArgumentException("Primary key " + token
                                        + " for class " + cldName + " required by datasource "
                                        + source.getName() + " in " + source.getName()
                                        + "_keys.properties is not defined in "
                                        + cld.getModel().getName() + "_keyDefs.properties");
                            }
                        }
                    } else {
                        LOG.warn("Ignoring entry for " + cldName + " in file "
                                + cld.getModel().getName() + "_keyDefs.properties - not in model!");
                    }
                }
                verifiedSources.add(source);
            }
            Map map = PrimaryKeyUtil.getPrimaryKeys(cld);
            String cldName = TypeUtil.unqualifiedName(cld.getName());
            String keyList = (String) keys.get(cldName);
            if (keyList != null) {
                String[] tokens = keyList.split(",");
                for (int i = 0; i < tokens.length; i++) {
                    String token = tokens[i].trim();
                    if (map.get(token) == null) {
                        throw new IllegalArgumentException("Primary key " + token
                                + " for class " + cld.getName() + " required by data source "
                                + source.getName() + " in " + source.getName() + "_keys.properties"
                                + " is not defined in " + cld.getModel().getName()
                                + "_keyDefs.properties");
                    } else {
                        keySet.add(map.get(token));
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Unable to find keys for source "
                    + source.getName() + " in file " + source.getName() + "_keys.properties");
        }
        return keySet;
    }

    /**
     * Return the Properties that enumerate the keys for this Source
     *
     * @param source the Source
     * @return the relevant Properties
     */
    protected static Properties getKeyProperties(Source source) {
        Properties keys = null;
        synchronized (sourceKeys) {
            keys = (Properties) sourceKeys.get(source);
            if (keys == null) {
                keys = PropertiesUtil.loadProperties(source.getName() + "_keys.properties");
                sourceKeys.put(source, keys);
            }
        }
        return keys;
    }

    /**
     * Look a the values of the given primary key in the object and return true if and only if some
     * part of the primary key is null.  If the primary key contains a reference it is sufficient
     * for any of the primary keys of the referenced object to be non-null (ie
     * objectPrimaryKeyIsNull() returning true).
     * @param model the Model in which to find ClassDescriptors
     * @param obj the Object to check
     * @param cld one of the classes that obj is.  Only primary keys for this classes will be
     * checked
     * @param pk the primary key to check
     * @param source the Source database
     * @param idMap an IntToIntMap from source IDs to destination IDs
     * @return true if the the given primary key is non-null for the given object
     * @throws MetaDataException if anything goes wrong
     */
    public static boolean objectPrimaryKeyNotNull(Model model, InterMineObject obj,
            ClassDescriptor cld, PrimaryKey pk, Source source, IntToIntMap idMap)
    throws MetaDataException {
        Iterator pkFieldIter = pk.getFieldNames().iterator();
      PK:
        while (pkFieldIter.hasNext()) {
            String fieldName = (String) pkFieldIter.next();
            FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
            if (fd instanceof AttributeDescriptor) {
                Object value;
                try {
                    value = TypeUtil.getFieldValue(obj, fieldName);
                } catch (IllegalAccessException e) {
                    throw new MetaDataException("Failed to get field " + fieldName
                            + " for key " + pk + " from " + obj, e);
                }
                if (value == null) {
                    return false;
                }
            } else if (fd instanceof CollectionDescriptor) {
                throw new MetaDataException("Primary key " + pk.getName() + " for class "
                        + cld.getName() + " cannot contain collection " + fd.getName()
                        + ": collections cannot be part of a primary key. Please edit"
                        + model.getName() + "_keyDefs.properties");
            } else if (fd instanceof ReferenceDescriptor) {
                InterMineObject refObj;
                try {
                    refObj = (InterMineObject) TypeUtil.getFieldProxy(obj, fieldName);
                } catch (IllegalAccessException e) {
                    throw new MetaDataException("Failed to get field " + fieldName
                            + " for key " + pk + " from " + obj, e);

                }
                if (refObj == null) {
                    return false;
                }

                if ((refObj.getId() != null) && (idMap.get(refObj.getId()) != null)) {
                    // We have previously loaded the object in this reference.
                    continue;
                }

                if (refObj instanceof ProxyReference) {
                    refObj = ((ProxyReference) refObj).getObject();
                }

                boolean foundNonNullKey = false;
                boolean foundKey = false;
                Set classDescriptors = model.getClassDescriptorsForClass(refObj.getClass());
                Iterator cldIter = classDescriptors.iterator();

              CLDS:
                while (cldIter.hasNext()) {
                    ClassDescriptor refCld = (ClassDescriptor) cldIter.next();

                    Set primaryKeys;

                    if (source == null) {
                        primaryKeys = new HashSet(PrimaryKeyUtil.getPrimaryKeys(refCld).values());
                    } else {
                        primaryKeys = DataLoaderHelper.getPrimaryKeys(refCld, source);
                    }

                    Iterator pkSetIter = primaryKeys.iterator();


                    while (pkSetIter.hasNext()) {
                        PrimaryKey refPK = (PrimaryKey) pkSetIter.next();
                        foundKey = true;

                        if (objectPrimaryKeyNotNull(model, refObj, refCld, refPK, source, idMap)) {
                           foundNonNullKey = true;
                           break CLDS;
                        }
                    }
                }

                if (foundKey && (!foundNonNullKey)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns true if the given field is a member of any primary key on the given class, for the
     * given source.
     *
     * @param model the Model in which to find ClassDescriptors
     * @param clazz the Class in which to look
     * @param fieldName the name of the field to check
     * @param source the Source that the keys belong to
     * @return true if the field is a primary key
     */
    public static boolean fieldIsPrimaryKey(Model model, Class clazz, String fieldName,
            Source source) {
        Set classDescriptors = model.getClassDescriptorsForClass(clazz);
        Iterator cldIter = classDescriptors.iterator();
        while (cldIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) cldIter.next();
            Set primaryKeys = DataLoaderHelper.getPrimaryKeys(cld, source);
            Iterator pkIter = primaryKeys.iterator();
            while (pkIter.hasNext()) {
                PrimaryKey pk = (PrimaryKey) pkIter.next();
                if (pk.getFieldNames().contains(fieldName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
