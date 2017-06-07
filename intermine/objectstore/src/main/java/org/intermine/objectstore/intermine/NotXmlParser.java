package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.sf.cglib.proxy.Factory;

import org.apache.log4j.Logger;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.TypeUtil;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.proxy.ProxyCollection;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.util.DynamicBean;
import org.intermine.util.DynamicUtil;

/**
 * Parses a String suitable for storing in the OBJECT field of database tables into an Object.
 *
 * @author Matthew Wakeling
 */
public final class NotXmlParser
{
    private NotXmlParser() {
    }

    private static final Logger LOG = Logger.getLogger(NotXmlParser.class);
    /**
     * The string that delimits the sections of the NotXml.
     */
    public static final String DELIM = "$_^";
    /**
     * The character that denotes an encoded delimiter in the string.
     */
    public static final String ENCODED_DELIM = "d";
    /**
     * A Pattern that will find delimiters.
     */
    public static final Pattern SPLITTER = Pattern.compile(DELIM, Pattern.LITERAL);
    private static final Pattern SPACE_SPLITTER = Pattern.compile(" ", Pattern.LITERAL);
    private static int opCount = 0;
    private static long splitTime = 0;
    private static long classTime = 0;
    private static long createTime = 0;
    private static long parseTime = 0;
    private static Map<String, Class<? extends FastPathObject>> classCache
        = Collections.synchronizedMap(new HashMap<String, Class<? extends FastPathObject>>());

    /**
     * Parse the given NotXml String into an Object.
     *
     * @param xml the NotXml String
     * @param os the ObjectStore from which to create lazy objects
     * @return an InterMineObject
     * @throws ClassNotFoundException if a class cannot be found
     */
    public static InterMineObject parse(String xml,
            ObjectStoreInterMineImpl os) throws ClassNotFoundException {
        if ((xml == null) || "null".equals(xml)) {
            Exception e = new Exception();
            e.fillInStackTrace();
            LOG.warn("Parsing " + xml, e);
        }
        long time1 = System.currentTimeMillis();
        String[] a = SPLITTER.split(xml);
        long time2 = System.currentTimeMillis();
        splitTime += time2 - time1;

        InterMineObject retval;

        Class<? extends FastPathObject> clazz = classCache.get(a[1]);
        if (clazz == null) {
            Set<Class<?>> classes = new HashSet<Class<?>>();
            if (!"".equals(a[1])) {
                String[] b = SPACE_SPLITTER.split(a[1]);
                for (int i = 0; i < b.length; i++) {
                    classes.add(Class.forName(b[i]));
                }
            }
            time1 = System.currentTimeMillis();
            classTime += time1 - time2;

            retval = (InterMineObject) DynamicUtil.createObject(classes);
            clazz = retval.getClass();
            classCache.put(a[1], clazz);
        } else {
            time1 = System.currentTimeMillis();
            classTime += time1 - time2;
            retval = (InterMineObject) DynamicUtil.createObject(clazz);
        }
        time2 = System.currentTimeMillis();
        createTime += time2 - time1;

        if (retval instanceof Factory) {
            DynamicBean bean = (DynamicBean) ((Factory) retval).getCallback(0);
            Map<String, Object> valueMap = bean.getMap();
            Map<String, FieldDescriptor> fields = os.getModel()
                .getFieldDescriptorsForClass(retval.getClass());
            Map<String, TypeUtil.FieldInfo> fieldInfos = TypeUtil.getFieldInfos(clazz);
            boolean fetchFromInterMineObject = os.getSchema().isFetchFromInterMineObject();
            for (int i = 2; i < a.length; i += 2) {
                if (a[i].startsWith("a")) {
                    String fieldName = a[i].substring(1).intern();
                    Class<?> fieldClass = fieldInfos.get(fieldName).getType();
                    String firstString = (i + 1 == a.length ? "" : a[i + 1]);
                    StringBuffer string = null;
                    while ((i + 2 < a.length) && (a[i + 2].startsWith(ENCODED_DELIM))) {
                        i++;
                        if (string == null) {
                            string = new StringBuffer(firstString);
                        }
                        string.append(DELIM).append(a[i + 1].substring(1));
                    }
                    if (ClobAccess.class.equals(fieldClass)) {
                        valueMap.put(fieldName, ClobAccess.decodeDbDescription(os, string == null
                                ? firstString : string.toString()));
                    } else {
                        valueMap.put(fieldName,
                                TypeUtil.stringToObject(fieldClass, (string == null ? firstString
                                        : string.toString())));
                    }
                } else if (a[i].startsWith("r")) {
                    String fieldName = a[i].substring(1).intern();
                    Integer id = Integer.valueOf(a[i + 1]);
                    if (fetchFromInterMineObject) {
                        valueMap.put(fieldName, new ProxyReference(os, id,
                                    InterMineObject.class));
                    } else {
                        ReferenceDescriptor ref = (ReferenceDescriptor) fields.get(fieldName);
                        if (ref == null) {
                            throw new RuntimeException("failed to get field " + fieldName
                                    + " for object from XML: " + xml);
                        }
                        @SuppressWarnings("unchecked")
                        Class<? extends InterMineObject> tmpType =
                            (Class<? extends InterMineObject>) ref.getReferencedClassDescriptor()
                                                                   .getType();
                        valueMap.put(fieldName, new ProxyReference(os, id, tmpType));
                    }
                }
            }

            for (Map.Entry<String, Class<?>> collEntry : os.getModel().getCollectionsForClass(clazz)
                    .entrySet()) {
                Collection<Object> lazyColl = new ProxyCollection<Object>(os, retval,
                        collEntry.getKey(), collEntry.getValue());
                valueMap.put(collEntry.getKey(), lazyColl);
            }
            time1 = System.currentTimeMillis();
            parseTime += time1 - time2;
            opCount++;
            if (opCount >= 100000) {
                LOG.info("(Fast Factory) Split: " + splitTime + " ms, Class: " + classTime
                        + " ms, Create: " + createTime + " ms, Parse: " + parseTime + " ms");
                opCount = 0;
            }
            return retval;
        } else {
            try {
                retval.setoBJECT(a, os);
    //                LOG.error("Used fast xml parser for class " + classes);
                time1 = System.currentTimeMillis();
                parseTime += time1 - time2;
                opCount++;
                if (opCount >= 100000) {
                    LOG.info("(Fast Class) Split: " + splitTime + " ms, Class: " + classTime
                            + " ms, Create: " + createTime + " ms, Parse: " + parseTime + " ms");
                    opCount = 0;
                }
                return retval;
            } catch (IllegalStateException e) {
                // It's alright - fall back to old slow method.

                //LOG.error("Falling back to slow parsing for " + retval.getClass(), e);

                Map<String, FieldDescriptor> fields = os.getModel()
                    .getFieldDescriptorsForClass(retval.getClass());
                Map<String, TypeUtil.FieldInfo> fieldInfos = TypeUtil.getFieldInfos(clazz);
                for (int i = 2; i < a.length; i += 2) {
                    if (a[i].startsWith("a")) {
                        String fieldName = a[i].substring(1);
                        Class<?> fieldClass = fieldInfos.get(fieldName).getType();
                        String firstString = (i + 1 == a.length ? "" : a[i + 1]);
                        StringBuffer string = null;
                        if (firstString.length() * 10 < xml.length() * 9) {
                            string = new StringBuffer(firstString);
                        }
                        while ((i + 2 < a.length) && (a[i + 2].startsWith(ENCODED_DELIM))) {
                            i++;
                            if (string == null) {
                                string = new StringBuffer(firstString);
                            }
                            string.append(DELIM).append(a[i + 1].substring(1));
                        }
                        if (ClobAccess.class.equals(fieldClass)) {
                            retval.setFieldValue(fieldName, ClobAccess.decodeDbDescription(os,
                                    string == null ? firstString : string.toString()));
                        } else {
                            retval.setFieldValue(fieldName, TypeUtil.stringToObject(fieldClass,
                                    (string == null ? firstString : string.toString())));
                        }
                    } else if (a[i].startsWith("r")) {
                        String fieldName = a[i].substring(1);
                        Integer id = Integer.valueOf(a[i + 1]);
                        ReferenceDescriptor ref = (ReferenceDescriptor) fields.get(fieldName);
                        if (ref == null) {
                            throw new RuntimeException("failed to get field " + fieldName
                                    + " for object from XML: " + xml);
                        }
                        @SuppressWarnings("unchecked")
                        Class<? extends InterMineObject> tmpType =
                            (Class<? extends InterMineObject>) ref.getReferencedClassDescriptor()
                                                                   .getType();
                        retval.setFieldValue(fieldName, new ProxyReference(os, id, tmpType));
                    }
                }

                for (Map.Entry<String, FieldDescriptor> collEntry : fields.entrySet()) {
                    FieldDescriptor maybeColl = collEntry.getValue();
                    if (maybeColl instanceof CollectionDescriptor) {
                        CollectionDescriptor coll = (CollectionDescriptor) maybeColl;
                        Collection<Object> lazyColl = new ProxyCollection<Object>(os, retval,
                                coll.getName(), coll.getReferencedClassDescriptor().getType());
                        retval.setFieldValue(coll.getName(), lazyColl);
                    }
                }
                time1 = System.currentTimeMillis();
                parseTime += time1 - time2;
                opCount++;
                if (opCount >= 100000) {
                    LOG.info("(Fallback) Split: " + splitTime + " ms, Class: " + classTime
                            + " ms, Create: " + createTime + " ms, Parse: " + parseTime + " ms");
                    opCount = 0;
                }
                return retval;
            }
        }
    }
}
