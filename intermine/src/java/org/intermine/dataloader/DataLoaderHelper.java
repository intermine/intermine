package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
import org.intermine.metadata.MetadataManager;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.model.datatracking.Source;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SubqueryConstraint;
import org.intermine.util.DynamicUtil;
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
 */
public class DataLoaderHelper
{
    protected static Map modelKeys = new HashMap();
    protected static Map sourceKeys = new HashMap();
    protected static Map modelDescriptors = new HashMap();

    private static final Logger LOG = Logger.getLogger(DataLoaderHelper.class);

    /**
     * Compare the priorities of two sources over a field
     * @param fd FieldDescriptor for the field
     * @param src1 the first Source
     * @param src2 the second Source
     * @return a positive integer if src1 is of higher priority than src2, a negative integer if
     * src2 is of higher priority than src1 or zero if the sources are equal.
     * @throws IllegalArgumentException if the class is not in the file, or both of the sources
     * are not listed for that class
     */
    public static int comparePriority(FieldDescriptor fd, Source src1, Source src2) {
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
        } else {
            throw new IllegalArgumentException("Could not determine priorities for sources "
                    + src1.getName() + " and " + src2.getName() + " for field "
                    + fd.getClassDescriptor().getName() + "." + fd.getName()
                    + " - is the config file set up correctly?");
        }
    }

    /**
     * Build a map from model to a list of the Class- and FieldDescriptors for which priority
     * information is available
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
     * Retrieve a map from key name to PrimaryKey object. The Map contains all the primary keys
     * that exist on a particular class, without performing any recursion.
     *
     * @param cld the ClassDescriptor to fetch primary keys for
     * @return the Map from key names to PrimaryKeys
     */
    public static Map getPrimaryKeys(ClassDescriptor cld) {
        Map keyMap = new LinkedHashMap();
        Properties keys = getKeyProperties(cld.getModel());
        String cldName = TypeUtil.unqualifiedName(cld.getName());
        Properties cldKeys = PropertiesUtil.getPropertiesStartingWith(cldName, keys);
        cldKeys = PropertiesUtil.stripStart(cldName, cldKeys);
        for (Iterator i = cldKeys.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String keyName = (String) entry.getKey();
            PrimaryKey key = new PrimaryKey((String) entry.getValue());
            keyMap.put(keyName, key);
        }
        return keyMap;
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
        Properties keys = getKeyProperties(source);
        Map map = getPrimaryKeys(cld);
        Set keySet = new HashSet();
        String cldName = TypeUtil.unqualifiedName(cld.getName());
        String keyList = (String) keys.get(cldName);
        if (keyList != null) {
            String[] tokens = keyList.split(",");
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i].trim();
                keySet.add(map.get(token));
            }
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
     * Return the Properties that specify the key fields for the classes in this Model
     *
     * @param model the Model
     * @return the relevant Properties
     */
    public static Properties getKeyProperties(Model model) {
        Properties keys = null;
        synchronized (modelKeys) {
            keys = (Properties) modelKeys.get(model);
            if (keys == null) {
                keys = MetadataManager.loadKeyDefinitions(model.getName());
                modelKeys.put(model, keys);
            }
        }
        return keys;
    }

    /**
     * Generates a query that searches for all objects in the database equivalent to a given
     * example object according to the primary keys defined for the given source.
     *
     * @param model a Model
     * @param obj the Object to take as an example
     * @param source the Source database
     * @param idMap an IntToIntMap from source IDs to destination IDs
     * @return a Query
     * @throws MetaDataException if anything goes wrong
     */
    public static Query createPKQuery(Model model, InterMineObject obj,
            Source source, IntToIntMap idMap) throws MetaDataException {
        try {
            int subCount = 0;
            Query q = new Query();
            q.setDistinct(false);
            QueryClass qcIMO = new QueryClass(InterMineObject.class);
            q.addFrom(qcIMO);
            q.addToSelect(qcIMO);
            ConstraintSet where = new ConstraintSet(ConstraintOp.OR);
            Query subQ = null;

            Set classDescriptors = model.getClassDescriptorsForClass(obj.getClass());
            Iterator cldIter = classDescriptors.iterator();
            while (cldIter.hasNext()) {
                ClassDescriptor cld = (ClassDescriptor) cldIter.next();
                Set primaryKeys = DataLoaderHelper.getPrimaryKeys(cld, source);
                if (!primaryKeys.isEmpty()) {
                    Iterator pkSetIter = primaryKeys.iterator();
                    while (pkSetIter.hasNext()) {
                        PrimaryKey pk = (PrimaryKey) pkSetIter.next();
                        subQ = new Query();
                        subQ.setDistinct(false);
                        QueryClass qc = new QueryClass(cld.getType());
                        subQ.addFrom(qc);
                        subQ.addToSelect(qc);
                        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
                        Iterator pkIter = pk.getFieldNames().iterator();
                        while (pkIter.hasNext()) {
                            String fieldName = (String) pkIter.next();
                            FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
                            if (fd instanceof AttributeDescriptor) {
                                Object value = TypeUtil.getFieldValue(obj, fieldName);
                                if (value == null) {
                                    cs.addConstraint(new SimpleConstraint(new QueryField(qc,
                                                    fieldName), ConstraintOp.IS_NULL));
                                } else {
                                    cs.addConstraint(new SimpleConstraint(new QueryField(qc,
                                                    fieldName), ConstraintOp.EQUALS,
                                                new QueryValue(value)));
                                }
                            } else if (fd instanceof CollectionDescriptor) {
                                throw new MetaDataException("A collection cannot be part of"
                                        + " a primary key");
                            } else if (fd instanceof ReferenceDescriptor) {
                                InterMineObject refObj = (InterMineObject)
                                    TypeUtil.getFieldProxy(obj, fieldName);
                                if (refObj == null) {
                                    cs.addConstraint(new ContainsConstraint(
                                                new QueryObjectReference(qc, fieldName),
                                                ConstraintOp.IS_NULL));
                                } else {
                                    Integer destId = null;
                                    if (refObj.getId() != null) {
                                        destId = idMap.get(refObj.getId());
                                    }
                                    if (destId == null) {
                                        if (refObj instanceof ProxyReference) {
                                            refObj = ((ProxyReference) refObj).getObject();
                                        }
                                        QueryClass qc2 = new QueryClass(((ReferenceDescriptor) fd)
                                                .getReferencedClassDescriptor().getType());
                                        subQ.addFrom(qc2);
                                        cs.addConstraint(new ContainsConstraint(
                                                    new QueryObjectReference(qc, fieldName),
                                                    ConstraintOp.CONTAINS, qc2));
                                        cs.addConstraint(new SubqueryConstraint(qc2,
                                                    ConstraintOp.IN,
                                                    createPKQuery(model, refObj, source, idMap)));
                                    } else {
                                        InterMineObject destObj = (InterMineObject)
                                            DynamicUtil.createObject(Collections.singleton(
                                                        InterMineObject.class));
                                        destObj.setId(destId);
                                        cs.addConstraint(new ContainsConstraint(
                                                    new QueryObjectReference(qc, fieldName),
                                                    ConstraintOp.CONTAINS, destObj));
                                    }
                                }
                            }
                        }
                        subQ.setConstraint(cs);
                        where.addConstraint(new SubqueryConstraint(qcIMO, ConstraintOp.IN, subQ));
                        subCount++;
                    }
                }
            }
            q.setConstraint(where);
            if (subCount == 1) {
                return subQ;
            } else {
                return q;
            }
        } catch (Exception e) {
            LOG.error("Broken with: " + DynamicUtil.decomposeClass(obj.getClass())
                      + "[" + obj.getId() + "]");
            LOG.error(e);
            throw new MetaDataException(e);
        }
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
