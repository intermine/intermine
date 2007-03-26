package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
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
import org.intermine.util.TypeUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * This class specifies how the values in a tab or comma separated file should be used to fill in
 * objects in an ObjectStore.
 *
 * @author Kim Rutherford
 */

public class DelimitedFileConfiguration
{
    private ClassDescriptor configClassDescriptor = null;
    private List columnFieldDescriptors = null;
    private List columnFieldClasses;

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

        String className = properties.getProperty("className");

        if (className == null) {
            throw new IllegalArgumentException("className not set in property file for "
                                               + "DelimitedFileConfiguration");
        } else {
            configClassDescriptor = model.getClassDescriptorByName(className);
        }

        if (configClassDescriptor == null) {
            throw new IllegalArgumentException("cannot find ClassDescriptor for: " + className);
        }

        Map columnFieldDescriptorMap = new TreeMap();

        Enumeration enumeration = properties.propertyNames();

        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();

            if (key.startsWith("column.")) {
                String columnNumberString = key.substring(7);

                try {
                    int keyColumnNumber = Integer.valueOf(columnNumberString).intValue();

                    String fieldName = properties.getProperty(key);

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
        }

        int mapMax = findMapMaxKey(columnFieldDescriptorMap);

        columnFieldDescriptors = new ArrayList(mapMax + 1);

        for (int columnNumber = 0; columnNumber < mapMax + 1; columnNumber++) {
            FieldDescriptor columnFD =
                (FieldDescriptor) columnFieldDescriptorMap.get(new Integer(columnNumber));

            columnFieldDescriptors.add(columnFD);
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
    public List getColumnFieldDescriptors() {
        return columnFieldDescriptors;
    }

    /**
     * Return a List of Class objects corresponding to the fields returned by 
     * getColumnFieldDescriptors().
     * @return the Class objects
     */
    public List getColumnFieldClasses() {
        if (columnFieldClasses == null) {
            columnFieldClasses = new ArrayList();
            for (int i = 0; i < columnFieldDescriptors.size(); i++) {
                AttributeDescriptor ad = (AttributeDescriptor) columnFieldDescriptors.get(i);
                if (ad == null) {
                    columnFieldClasses.add(null);
                } else {
                    String className = ad.getType();
                    columnFieldClasses.add(TypeUtil.instantiate(className));
                }
            }
        }
        
        return columnFieldClasses;
    }
}
