package org.intermine.task;

/*
* Copyright (C) 2002-2005 FlyMine
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
 * An ant task that
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
        Project project = getProject();
        Hashtable projectProps = project.getProperties();

        PropertyDescriptor[] props =  PropertyUtils.getPropertyDescriptors(bean);
        for (int i = 0; i < props.length; i++) {
            PropertyDescriptor desc = props[i];
            Method setter = desc.getWriteMethod();
            if (setter != null) {
                Class propType = desc.getPropertyType();
                String propName = setter.getName().substring(3).toLowerCase();
                Object propValue = projectProps.get(propName);
                System.out.println("propType: " + propType + ", propName: " + propName);
                if (propValue != null) {
                    try {
                        if (propType.equals(File.class)) {
                            propValue = getProject().resolveFile((String) propValue);
                        }
                        PropertyUtils.setProperty(bean, propName, propValue);
                    } catch (Exception e) {
                        throw new BuildException(e);
                    }
                }
            }
        }
    }
}
