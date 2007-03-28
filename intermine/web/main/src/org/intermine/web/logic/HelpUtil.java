package org.intermine.web.logic;

/* 
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;

/**
 * Helper methods for the help functions in the webapp.
 * @author Kim Rutherford
 */
public class HelpUtil
{
    /**
     * Look through the classDescriptions to find a matching help text for the given "Class.field"
     * key.  If a description doesn't exist for the given Class, look for super class descriptions
     * that match.  eg.  If key is "Manager.name", look in classDescriptions for "Manager.name"
     * first.  If that doesn't exist try looking for "Employee.name".
     * @param model the InterMine Model
     * @param classDescriptions a Map from "ClassName.fieldName" to descriptions
     * @param key the initial key to use when looking up descriptions.  eg. "Manager.name"
     * @return the description
     * @throws ClassNotFoundException
     */
    public static String getHelpText(Model model, Map classDescriptions, String key) {
        String className;
        String fieldName = null;
        int dotIndex = key.indexOf(".");
        if (dotIndex == -1) {
            className = key;            
        } else {
            className = key.substring(0, dotIndex);
            fieldName = key.substring(dotIndex + 1);
        }
        Class cls;
        try {
            cls = Class.forName(model.getPackageName() + "." + className);
        } catch (ClassNotFoundException e) {
            return null;
        }
        Set cds = model.getClassDescriptorsForClass(cls);
        Iterator cdIter = cds.iterator();
        while (cdIter.hasNext()) {
            ClassDescriptor cd = (ClassDescriptor) cdIter.next();
            String newKey;
            if (fieldName == null) {
                newKey = cd.getUnqualifiedName();
            } else {
                newKey = cd.getUnqualifiedName() + "." + fieldName;
            }
            if (classDescriptions.containsKey(newKey)) {
                return (String) classDescriptions.get(newKey);
            }
        }

        return null;
    }
}
