package org.intermine.xml;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;

/**
 * Static methods to assist parsing and rendering of XML.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */
public final class XmlHelper
{
    private XmlHelper() {
    }

    /**
     * Get the class name if object represents a material class in model.
     *
     * @param obj the object
     * @param model the parent model
     * @return class name
     */
    public static String getClassName(Object obj, Model model) {
        String clsName = getClassName(obj.getClass(), model);
        if ("".equals(clsName)) {
            clsName = getClassName(obj.getClass().getSuperclass(), model);
        }
        return clsName;
    }


    private static String getClassName(Class<?> cls, Model model) {
        if (cls != null) {
            ClassDescriptor cld = model.getClassDescriptorByName(cls.getName());
            if (cld != null && !cld.isInterface()) {
                return cld.getName();
            }
        }
        return "";
    }

}
