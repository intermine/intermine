package org.flymine.xml.full;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.flymine.metadata.Model;
import org.flymine.util.StringUtil;
import org.flymine.util.DynamicUtil;

/**
* Class providing Item utility methods
* @author Mark Woodbridge
*/
public class ItemHelper
{

    /**
    * Create an outline business object from an Item, does not fill in fields.
    * @param item a the Item to realise
    * @param model the parent model
    * @return the materialised business object
    */
    public static Object instantiateObject(Item item, Model model) {
        String classNames = "";
        if (item.getClassName() != null) {
            classNames += item.getClassName();
        }
        if (item.getImplementations() != null) {
            classNames += " " + item.getImplementations();
        }
        
        try {
            Set classes = new HashSet();
            for (Iterator i = StringUtil.tokenize(classNames).iterator(); i.hasNext();) {
                classes.add(generateClass((String) i.next(), model));
            }
            return DynamicUtil.createObject(classes);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot find one of '" + classNames + "' in model", e);
        }
    }
    
    /**
     * Create a class given a namspace qualified string, if class is not in the
     * the given model looks for core classes in org.flymine.model package.
     * @param a namespace qualified class name
     * @param model the parent model
     * @throws ClassNotFoundException if invalid class string
     */
    private static Class generateClass(String namespacedClass, Model model)
        throws ClassNotFoundException {
        String localName = namespacedClass.substring(namespacedClass.indexOf("#") + 1);
        Class cls;
        try {
            cls = Class.forName(model.getPackageName() + "." + localName);
        } catch (ClassNotFoundException e) {
            cls = Class.forName("org.flymine.model." + localName);
        }
        return cls;
    }
}
