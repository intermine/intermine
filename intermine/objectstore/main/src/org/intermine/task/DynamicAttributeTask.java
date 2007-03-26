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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.io.File;
import java.beans.PropertyDescriptor;

import org.apache.log4j.Logger;

/**
 * An ant task that provides a method for setting attributes dynamically within a class.
 *
 * Typically a sub class of this task will have a handle to a class that does some work for it, but
 * in order to make the class reusable, it's properties are factored out into either a config or a
 * build file. The task subclass can use the configureDynamicAttributes method, along with some
 * properties that have been set at runtime to initialize the helper/worker class.
 *  
 * @author Peter Mclaren
 * @author Richard Smith
 * */

public class DynamicAttributeTask extends Task
{
    protected static final Logger LOG = Logger.getLogger(DynamicAttributeTask.class);

    /**
     * Look at set methods on a target object and lookup values in project
     * properties.  If no value found property will not be set but no error
     * will be thrown.
     * @param bean an object to search for setter methods
     */
    protected void configureDynamicAttributes(Object bean) {
        Project antProject = getProject();
        Hashtable projectProps = antProject.getProperties();
        PropertyDescriptor[] props =  PropertyUtils.getPropertyDescriptors(bean);
        for (int i = 0; i < props.length; i++) {
            PropertyDescriptor desc = props[i];
            Method setter = desc.getWriteMethod();
            if (setter != null) {
                Class propType = desc.getPropertyType();
                String propName = setter.getName().substring(3).toLowerCase();
                Object propValue = projectProps.get(propName);
                
                if (propValue == null) {
                    // there is not all-lowercase property in projectProps, so try the camelCase
                    // version
                    String setterName = setter.getName();
                    String camelCasePropName =
                        setterName.substring(3, 4).toLowerCase() + setterName.substring(4);
                    propName = camelCasePropName;
                    propValue = projectProps.get(camelCasePropName);
                }
                
                if (propValue == null) {
                    // still not found, try replacing each capital (after first) in camelCase
                    // to be a dot - i.e. setSrcDataDir -> src.data.dir
                    String setterName = setter.getName();
                    String camelCasePropName =
                        setterName.substring(3, 4).toLowerCase() + setterName.substring(4);
                    String dotName = "";
                    for (int j = 0; j < camelCasePropName.length(); j++) {
                        if (Character.isUpperCase(camelCasePropName.charAt(j))) {
                            dotName += "." + camelCasePropName.substring(j, j + 1).toLowerCase();
                        } else {
                            dotName += camelCasePropName.substring(j, j + 1);
                        }
                    }
                    propValue = projectProps.get(dotName);
                }

                if (propValue != null) {
                    try {
                        if (propType.equals(File.class)) {

                            String filePropValue = (String) propValue;
                            //First check to see if we were given a complete file path, if so then
                            // we can use it directly instead of trying to search for it.
                            File maybeFile = new File(filePropValue);
                            if (maybeFile.exists()) {
                                propValue = maybeFile;
                                LOG.info("Configuring task to use file:" + filePropValue);
                            } else {
                                propValue = getProject().resolveFile(filePropValue);
                            }
                        }
                        PropertyUtils.setProperty(bean, propName, propValue);
                    } catch (Exception e) {
                        throw new BuildException("failed to set value for " + propName + " to "
                                                 + propValue + " in " + bean, e);
                    }
                }
            }
        }
    }
}
