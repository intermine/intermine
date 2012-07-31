package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
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
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.DatabaseSchema;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.IntToIntMap;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;

/**
 * Class providing utility methods to help with primary key and data source priority configuration
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public final class DataLoaderHelper
{
    private DataLoaderHelper() {
    }

    private static final Logger LOG = Logger.getLogger(DataLoaderHelper.class);

    protected static Map<Source, Properties> sourceKeys = new HashMap<Source, Properties>();
    protected static Map<Model, Map<String, List<String>>> modelDescriptors
        = new IdentityHashMap<Model, Map<String, List<String>>>();
    protected static Set<Source> verifiedSources = new HashSet<Source>();

    /**
     * Build a map from class and field names to a priority-ordered List of source name Strings.
     *
     * @param model the Model
     * @return the Map
     */
    protected static Map<String, List<String>> getDescriptors(Model model) {
        Map<String, List<String>> descriptorSources = null;
        synchronized (modelDescriptors) {
            descriptorSources = modelDescriptors.get(model);
            if (descriptorSources == null) {
                descriptorSources = new HashMap<String, List<String>>();
                Properties priorities = PropertiesUtil.loadProperties(model.getName()
                                                                      + "_priorities.properties");
                if (priorities == null) {
                    throw new RuntimeException("Could not load priorities config file "
                            + model.getName() + "_priorities.properties");
                }
                for (Map.Entry<Object, Object> entry : priorities.entrySet()) {
                    String descriptorName = (String) entry.getKey();
                    String sourceNames = (String) entry.getValue();
                    List<String> sources = new ArrayList<String>();
                    String[] tokens = sourceNames.split(",");
                    for (int o = 0; o < tokens.length; o++) {
                        String token = tokens[o].trim();
                        if (token.contains(" ")) {
                            throw new IllegalArgumentException("Data source " + token + " for key "
                                    + descriptorName + " must not contain any spaces");
                        }
                        sources.add(token);
                    }
                    descriptorSources.put(descriptorName, sources);
                }
                modelDescriptors.put(model, descriptorSources);
            }
        }
        return descriptorSources;
    }

    private static Map<GetPrimaryKeyCacheKey, Set<PrimaryKey>> getPrimaryKeyCache
        = new HashMap<GetPrimaryKeyCacheKey, Set<PrimaryKey>>();

    /**
     * Return a Set of PrimaryKeys relevant to a given Source for a ClassDescriptor. The Set
     * contains all the primary keys that exist on a particular class that are used by the
     * source, without performing any recursion. The Model.getClassDescriptorsForClass()
     * method is recommended if you wish for all the primary keys of the class' parents
     * as well.
     *
     * @param cld the ClassDescriptor
     * @param source the Source
     * @param os the ObjectStore that these PrimaryKeys are used in, for creating indexes
     * @return a Set of PrimaryKeys
     */
    public static Set<PrimaryKey> getPrimaryKeys(ClassDescriptor cld, Source source,
            ObjectStore os) {
        GetPrimaryKeyCacheKey key = new GetPrimaryKeyCacheKey(cld, source);
        synchronized (getPrimaryKeyCache) {
            Set<PrimaryKey> keySet = getPrimaryKeyCache.get(key);
            if (keySet == null) {
                keySet = new LinkedHashSet<PrimaryKey>();
                Properties keys = getKeyProperties(source);
                if (keys != null) {
                    if (!verifiedSources.contains(source)) {
                        String packageNameWithDot = cld.getName().substring(0, cld.getName()
                                .lastIndexOf('.') + 1);
                        LOG.info("Verifying primary key config for source " + source
                                + ", packageName = " + packageNameWithDot);
                        for (Map.Entry<Object, Object> entry : keys.entrySet()) {
                            String cldName = (String) entry.getKey();
                            String keyList = (String) entry.getValue();
                            if (!cldName.contains(".")) {
                                ClassDescriptor iCld = cld.getModel().getClassDescriptorByName(
                                        packageNameWithDot + cldName);
                                if  (iCld != null) {
                                    Map<String, PrimaryKey> map = PrimaryKeyUtil
                                        .getPrimaryKeys(iCld);

                                    String[] tokens = keyList.split(",");
                                    for (int i = 0; i < tokens.length; i++) {
                                        String token = tokens[i].trim();
                                        if (map.get(token) == null) {
                                            throw new IllegalArgumentException("Primary key "
                                                    + token + " for class " + cldName
                                                    + " required by datasource " + source.getName()
                                                    + " in " + source.getName()
                                                    + "_keys.properties is not defined in "
                                                    + cld.getModel().getName()
                                                    + "_keyDefs.properties");
                                        }
                                    }
                                } else {
                                    LOG.warn("Ignoring entry for " + cldName + " in file "
                                            + cld.getModel().getName()
                                            + "_keyDefs.properties - not in model!");
                                }
                            }
                        }
                        verifiedSources.add(source);
                    }
                    Map<String, PrimaryKey> map = PrimaryKeyUtil.getPrimaryKeys(cld);
                    String cldName = TypeUtil.unqualifiedName(cld.getName());
                    String keyList = (String) keys.get(cldName);
                    if (keyList != null) {
                        String[] tokens = keyList.split(",");
                        for (int i = 0; i < tokens.length; i++) {
                            String token = tokens[i].trim();
                            if (map.get(token) == null) {
                                throw new IllegalArgumentException("Primary key " + token
                                        + " for class " + cld.getName()
                                        + " required by data source " + source.getName() + " in "
                                        + source.getName() + "_keys.properties is not defined in "
                                        + cld.getModel().getName() + "_keyDefs.properties");
                            } else {
                                keySet.add(map.get(token));
                            }
                        }
                    }
                    for (Map.Entry<Object, Object> entry : keys.entrySet()) {
                        String propKey = (String) entry.getKey();
                        String fieldList = (String) entry.getValue();
                        int posOfDot = propKey.indexOf('.');
                        if (posOfDot > 0) {
                            String propCldName = propKey.substring(0, posOfDot);
                            if (cldName.equals(propCldName)) {
                                String keyName = propKey.substring(posOfDot + 1);
                                PrimaryKey pk = new PrimaryKey(keyName, fieldList, cld);
                                if (!keySet.contains(pk)) {
                                    keySet.add(pk);
                                    if (os instanceof ObjectStoreInterMineImpl) {
                                        ObjectStoreInterMineImpl osimi
                                            = (ObjectStoreInterMineImpl) os;
                                        DatabaseSchema schema = osimi.getSchema();
                                        ClassDescriptor tableMaster = schema.getTableMaster(cld);
                                        String tableName = DatabaseUtil.getTableName(tableMaster);
                                        List<String> fields = new ArrayList<String>();

                                        for (String field : pk.getFieldNames()) {
                                            String colName =
                                                DatabaseUtil.generateSqlCompatibleName(field);
                                            if (tableMaster.getReferenceDescriptorByName(field,
                                                    true) != null) {
                                                colName += "id";
                                            }
                                            fields.add(colName);
                                        }
                                        String sql = "CREATE INDEX " + tableName + "__" + keyName
                                            + " ON " + tableName + " (" + StringUtil.join(fields,
                                                        ", ") + ")";
                                        System.out .println("Creating index: " + sql);
                                        LOG.info("Creating index: " + sql);
                                        Connection conn = null;
                                        try {
                                            conn = osimi.getConnection();
                                            conn.createStatement().execute(sql);
                                        } catch (SQLException e) {
                                            LOG.warn("Index creation failed", e);
                                        } finally {
                                            if (conn != null) {
                                                osimi.releaseConnection(conn);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Unable to find keys for source "
                            + source.getName() + " in file " + source.getName()
                            + "_keys.properties");
                }
                getPrimaryKeyCache.put(key, keySet);
            }
            return keySet;
        }
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
            keys = sourceKeys.get(source);
            if (keys == null) {
                String sourceNameKeysFileName = source.getName() + "_keys.properties";
                keys = PropertiesUtil.loadProperties(sourceNameKeysFileName);

                String sourceTypeKeysFileName = source.getType() + "_keys.properties";
                if (keys == null) {
                    keys = PropertiesUtil.loadProperties(sourceTypeKeysFileName);
                }

                if (keys == null) {
                    throw new RuntimeException("can't find keys for source: " + source
                                               + " after trying to find: " + sourceNameKeysFileName
                                               + " and: " + sourceTypeKeysFileName);
                }

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
            ClassDescriptor cld, PrimaryKey pk, Source source,
            IntToIntMap idMap) throws MetaDataException {
        for (String fieldName : pk.getFieldNames()) {
            FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
            if (fd instanceof AttributeDescriptor) {
                Object value;
                try {
                    value = obj.getFieldValue(fieldName);
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
                    refObj = (InterMineObject) obj.getFieldProxy(fieldName);
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
                Set<ClassDescriptor> classDescriptors = model.getClassDescriptorsForClass(refObj
                        .getClass());

            CLDS:
                for (ClassDescriptor refCld : classDescriptors) {
                    Set<PrimaryKey> primaryKeys;

                    if (source == null) {
                        primaryKeys = new LinkedHashSet<PrimaryKey>(PrimaryKeyUtil
                                .getPrimaryKeys(refCld).values());
                    } else {
                        primaryKeys = DataLoaderHelper.getPrimaryKeys(refCld, source, null);
                    }

                    for (PrimaryKey refPK : primaryKeys) {
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

    private static ThreadLocal<Map<PrimaryKeyCacheKey, Set<String>>> primaryKeyCache
        = new ThreadLocal<Map<PrimaryKeyCacheKey, Set<String>>>() {
            @Override protected Map<PrimaryKeyCacheKey, Set<String>> initialValue() {
                return new HashMap<PrimaryKeyCacheKey, Set<String>>();
            }
        };

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
    public static boolean fieldIsPrimaryKey(Model model, Class<?> clazz, String fieldName,
            Source source) {
        Map<PrimaryKeyCacheKey, Set<String>> cache = primaryKeyCache.get();
        PrimaryKeyCacheKey key = new PrimaryKeyCacheKey(model, clazz, source);
        Set<String> fields = cache.get(key);
        if (fields == null) {
            fields = new HashSet<String>();
            for (ClassDescriptor cld : model.getClassDescriptorsForClass(clazz)) {
                for (PrimaryKey pk : getPrimaryKeys(cld, source, null)) {
                    fields.addAll(pk.getFieldNames());
                }
            }
            cache.put(key, fields);
        }
        return fields.contains(fieldName);
    }

    private static class PrimaryKeyCacheKey
    {
        private Model model;
        private Class<?> clazz;
        private Source source;

        public PrimaryKeyCacheKey(Model model, Class<?> clazz, Source source) {
            this.model = model;
            this.clazz = clazz;
            this.source = source;
        }

        @Override
        public int hashCode() {
            return clazz.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof PrimaryKeyCacheKey) {
                PrimaryKeyCacheKey pkck = (PrimaryKeyCacheKey) o;
                return (model == pkck.model) && clazz.equals(pkck.clazz)
                    && source.equals(pkck.source);
            }
            return false;
        }
    }

    private static class GetPrimaryKeyCacheKey
    {
        private ClassDescriptor cld;
        private Source source;

        public GetPrimaryKeyCacheKey(ClassDescriptor cld, Source source) {
            this.cld = cld;
            this.source = source;
        }

        @Override
        public int hashCode() {
            return cld.getName().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof GetPrimaryKeyCacheKey) {
                GetPrimaryKeyCacheKey key = (GetPrimaryKeyCacheKey) o;
                return (cld == key.cld) && source.equals(key.source);
            }
            return false;
        }
    }
}
