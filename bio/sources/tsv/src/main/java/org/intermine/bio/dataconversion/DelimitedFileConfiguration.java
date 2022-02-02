package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.TypeUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeMap;

/**
 * This class specifies how the values in a tab or comma separated file should be used to fill in
 * objects in an ObjectStore.
 *
 * @author Daniela BUtano
 * @author Kim Rutherford
 */

public class DelimitedFileConfiguration
{
    Map<String, Map> classNameColumnFieldDescriptorMap = new HashMap();
    private ClassDescriptor configClassDescriptor = null;
    private Map<String, List> classNameColumnFieldDescriptors = new HashMap();

    /**
     * Create a new DelimitedFileConfiguration from an InputStream.
     * @param model The model to use when looking for ClassDescriptors and FieldDescriptors
     * @param inputStream The InputStream to read the configuration from
     * @throws IOException throws if the read fails
     */
    public DelimitedFileConfiguration (Model model, InputStream inputStream)
        throws IOException {

        Properties properties = new Properties();
        properties.load(inputStream);

        Map columnFieldDescriptorMap;


        Enumeration enumeration = properties.propertyNames();

        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            String columnNumberString = key.substring(7);

            try {
                int keyColumnNumber = Integer.valueOf(columnNumberString).intValue();

                String value = properties.getProperty(key);
                String className = value.substring(0, value.indexOf("."));
                configClassDescriptor = model.getClassDescriptorByName(className);
                if (configClassDescriptor == null) {
                    throw new IllegalArgumentException("cannot find ClassDescriptor for: "
                            + className);
                }

                if (classNameColumnFieldDescriptorMap.get(className) == null ) {
                    columnFieldDescriptorMap = new TreeMap();
                    classNameColumnFieldDescriptorMap.put(className, columnFieldDescriptorMap);
                } else {
                    columnFieldDescriptorMap = classNameColumnFieldDescriptorMap.get(className);
                }

                String fieldName = value.substring(value.indexOf(".") + 1);
                FieldDescriptor columnFD =
                    configClassDescriptor.getFieldDescriptorByName(fieldName);
                if (columnFD == null) {
                    throw new IllegalArgumentException("cannot find FieldDescriptor for "
                                                       + fieldName + " in " + className);
                }

                if (!columnFD.isAttribute()) {
                    String message = "field: " + fieldName + " in "
                            + className + " is not an attribute field so cannot be used as a "
                            + "className in DelimitedFileConfiguration";
                    throw new IllegalArgumentException(message);
                }

                columnFieldDescriptorMap.put(new Integer(keyColumnNumber), columnFD);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("column number (" + key + ") not parsable "
                                                   + "in property file for "
                                                   + "DelimitedFileConfiguration");
            }
        }

        Iterator classNameIter = getClassNames().iterator();
        while (classNameIter.hasNext()) {
            String className = (String) classNameIter.next();
            int mapMax = findMapMaxKey(classNameColumnFieldDescriptorMap.get(className));

            List<FieldDescriptor> columnFieldDescriptors = new ArrayList(mapMax + 1);

            columnFieldDescriptorMap = classNameColumnFieldDescriptorMap.get(className);
            for (int columnNumber = 0; columnNumber < mapMax + 1; columnNumber++) {
                FieldDescriptor columnFD =
                        (FieldDescriptor) columnFieldDescriptorMap.get(new Integer(columnNumber));

                columnFieldDescriptors.add(columnFD);
            }
            classNameColumnFieldDescriptors.put(className, columnFieldDescriptors);
        }
    }

    private int findMapMaxKey(Map map) {
        if (map.size() == 0) {
            throw new IllegalArgumentException("Map empty in findMapMaxKey()");
        }

        int maxSoFar = Integer.MIN_VALUE;

        Iterator mapKeyIter = map.keySet().iterator();

        while (mapKeyIter.hasNext()) {
            Integer key = (Integer) mapKeyIter.next();

            if (key.intValue() > maxSoFar) {
                maxSoFar = key.intValue();
            }
        }

        return maxSoFar;
    }

    /**
     * Return the class names
     * @return the class names
     */
    public Set<String> getClassNames() {
        return classNameColumnFieldDescriptorMap.keySet();
    }

    /**
     * Return the ClassDescriptor of the class to modify.
     * @return the ClassDescriptor
     */
    public ClassDescriptor getConfigClassDescriptor() {
        return configClassDescriptor;
    }

    /**
     * Return a List of the configured AttributeDescriptors.  The List is indexed by column number
     * (starting with column 0).  If a column has no configured AttributeDescriptor the List will
     * have null at that index.
     * @return the configured AttributeDescriptors
     */
    public List getColumnFieldDescriptors(String className) {
        return classNameColumnFieldDescriptors.get(className);
    }

    /**
     * Return a List of Class objects corresponding to the fields returned by
     * getColumnFieldDescriptors().
     * @return the Class objects
     */
    public List getColumnFieldClasses(String className) {
        List columnFieldClasses = new ArrayList();
        List columnFieldDescriptors = classNameColumnFieldDescriptors.get(className);
        for (int i = 0; i < columnFieldDescriptors.size(); i++) {
            AttributeDescriptor ad = (AttributeDescriptor) columnFieldDescriptors.get(i);
            if (ad == null) {
                columnFieldClasses.add(null);
            } else {
                String type = ad.getType();
                columnFieldClasses.add(TypeUtil.instantiate(type));
            }
        }

        return columnFieldClasses;
    }
}
