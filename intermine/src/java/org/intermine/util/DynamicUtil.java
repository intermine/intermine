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
    protected static final Logger LOG = Logger.getLogger(DynamicUtil.class);

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
        // Need to go up the ClassDescriptor inheritence tree so that the
        // created object can be cast to everything

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
            addInterface(cld, expandedInterfaces);
        }

        // Now create the object
        return DynamicBean.create(null, convertToClassArray(expandedInterfaces));

    }

    /**
     * Add an interface (and its parents) to a Set
     *
     * @param cld the ClassDescriptor of the interface to add
     * @param set the set of interfaces to add to
     */
    protected static void addInterface(ClassDescriptor cld, Set set) {
        // Add this interface
        set.add(cld.getName());

        // Recurse up the inheritence hierarchy
        Collection ints = cld.getInterfaceDescriptors();
        Iterator iter = ints.iterator();
        while (iter.hasNext()) {
            addInterface((ClassDescriptor) iter.next(), set);
        }

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
