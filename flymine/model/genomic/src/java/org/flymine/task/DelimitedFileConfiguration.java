package org.flymine.task;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;

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
    ClassDescriptor configClassDescriptor = null;
    FieldDescriptor keyFieldDescriptor = null;
    ArrayList columnFieldDescriptors = null;
    int keyColumnNumber = -1;
    
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

        Map columnFieldDescriptorMap = new TreeMap();
        
        Enumeration enumeration = properties.propertyNames();

        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();

            if (key.startsWith("column.")) {
                String columnNumberString = key.substring(7);

                try {
                    keyColumnNumber = Integer.valueOf(columnNumberString).intValue();

                    String fieldName = properties.getProperty(key);

                    FieldDescriptor columnFD =
                        configClassDescriptor.getFieldDescriptorByName(fieldName);

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
        
        String keyColumnString = properties.getProperty("keyColumn");

        if (keyColumnString == null) {
            throw new IllegalArgumentException("keyColumn not set in property file for "
                                               + "DelimitedFileConfiguration");
        } else {
            try {
                keyColumnNumber = Integer.valueOf(keyColumnString).intValue();
                if (columnFieldDescriptors.size() <= keyColumnNumber) {
                    throw new IllegalArgumentException("keyColumn (" + keyColumnNumber
                                                       + ") out of range "
                                                       + "in property file for "
                                                       + "DelimitedFileConfiguration");
                }
                
                keyFieldDescriptor = (FieldDescriptor) columnFieldDescriptors.get(keyColumnNumber);
                
                if (keyFieldDescriptor == null) {
                    throw new IllegalArgumentException("no column configuration found for "
                                                       + "keyColumn: " + keyColumnString);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("column number (" + keyColumnString
                                                   + ") not parsable "
                                                   + "in property file for "
                                                   + "DelimitedFileConfiguration");
            }
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
     * Return the column in the input file that should be used as the key for looking up objects. 
     * @return the key column - the first column is 0
     */
    public int getKeyColumnNumber () {
        return keyColumnNumber;
    }
    
    /**
     * Return the FieldDescriptor of the field in the configClassDescriptor that should be used as
     * the primary key when looking up objects.
     * @return the FieldDescriptor for the key field.
     */
    public FieldDescriptor getKeyFieldDescriptor() {
        return keyFieldDescriptor;
    }

    /**
     * Return a List of the configured FieldDescriptors.  The List is indexed by column number
     * (starting with column 0).  If a column has no configured FieldDescriptor the List will have
     * null at that index.
     * @return the configured FieldDescriptors
     */
    public List getColumnFieldDescriptors() {
        return columnFieldDescriptors;
    }
}
