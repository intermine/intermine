package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.Iterator;
import java.util.Properties;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;

/**
 * Generate a Properties object that can be passed to the ObjectStoreSummary constructor.
 *
 * @author Kim Rutherford
 */

public class ObjectStoreSummaryGenerator
{
    /**
     * Get the number of instances of a particular class in the ObjectStore.
     * @param os the ObjectStore to query
     * @param className the class name to look up
     * @throws ObjectStoreException if an error occurs during the database query
     * @throws ClassNotFoundException if the className doesn't refer to a known class
     * @return the count of the instances of the class
     */
    private static int getClassCount(ObjectStore os, String className)
        throws ObjectStoreException, ClassNotFoundException {
        Query q = new Query();
        QueryClass qc = new QueryClass(Class.forName(className));
        q.addToSelect(new QueryField(qc, "id"));
        q.addFrom(qc);

        int count = os.count(q, os.getSequence());
        return count;
    }

    /**
     * Count all the objects of each class and put the results in the given Properties objects.
     * The properties will look like: org.flymine.model.Gene.count = 12345
     */
    private static void getAllClassCounts (ObjectStore os, Properties properties)
        throws ObjectStoreException {
        Model model = os.getModel();
        Set classDescriptors = model.getClassDescriptors();
        Iterator classDescriptorIterator = classDescriptors.iterator();

        while (classDescriptorIterator.hasNext()) {
            ClassDescriptor cd = (ClassDescriptor) classDescriptorIterator.next();
            String className = cd.getName();
            try {
                int count = getClassCount(os, className);
                properties.put(className + ObjectStoreSummary.CLASS_COUNTS_SUFFIX , count + "");
            } catch (ClassNotFoundException e) {
                throw new Error("internal error: " + e);
            }
        }
    }

    /**
     * For the given configuration Properties object return Properties object that summarises the
     * given ObjectStore.  The configurationProperties have this form:
     * org.intermine.model.SomeObject.fields = someField someOtherField
     * @param os the ObjectStore to summarise.
     * @param configurationProperties A Properties object describing the objects and fields to
     * summarise
     * @throws ObjectStoreException if there is a problem with the ObjectStore
     * @return a Properties object summarising the given ObjectStore
     */
    public static Properties getAsProperties(ObjectStore os, Properties configurationProperties)
        throws ObjectStoreException {
        Properties properties = new Properties();

        getAllClassCounts(os, properties);

        return properties;
    }
}
