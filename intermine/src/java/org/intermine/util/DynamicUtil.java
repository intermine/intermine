package org.flymine.util;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import net.sf.cglib.*;

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;

/**
 * Utilities to create DynamicBeans
 *
 * @author Andrew Varley
 */
public class DynamicUtil
{

    /**
     * Cannot construct
     */
    private DynamicUtil() {
    }

    /**
     * Create a DynamicBean from a set of interface names
     *
     * @param model the Model we are using
     * @param interfaces the interfaces to implement
     * @return the DynamicBean
     * @throws ClassNotFoundException if any class cannot be found
     */
    public static Object createObject(Model model, Set interfaces) throws ClassNotFoundException {
        Set expandedInterfaces = new HashSet();
        Iterator intIter = interfaces.iterator();
        while (intIter.hasNext()) {
            String intName = (String) intIter.next();
            ClassDescriptor cld = model.getClassDescriptorByName(intName);
            if (cld == null) {
                throw new ClassNotFoundException("Cannot find " + intName
                                                 + " in " + model.getName());
            }
            if (!cld.isInterface()) {
                throw new IllegalArgumentException(intName + " is not an interface");
            }
        }

        // Now create the object
        return DynamicBean.create(null, convertToClassArray(interfaces));

    }

    /**
     * Convert a set of interface names to an array of Class objects
     *
     * @param names the set of interface names
     * @return array of Class objects
     * @throws ClassNotFoundException if class cannot be found
     */
    protected static Class [] convertToClassArray(Set names) throws ClassNotFoundException {
        Iterator iter = names.iterator();
        List list = new ArrayList();

        while (iter.hasNext()) {
            list.add(Class.forName((String) iter.next()));
        }

        return (Class []) list.toArray(new Class [] {});
    }


}
