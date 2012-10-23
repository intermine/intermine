package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.web.logic.config.WebConfig;

/**
 * Class to represent a collection field of an object for the webapp
 * @author Mark Woodbridge
 */
public class DisplayCollection extends DisplayField
{
    CollectionDescriptor desc;

    /**
     * Construct a new DisplayCollection object
     * @param collection the actual collection
     * @param desc the descriptors for this collection
     * @param webConfig the WebConfig object for this webapp
     * @param webProperties telling us how many Collection rows to show
     * @param classKeys Map of class name to set of keys
     * @param listOfTypes as determined using PathQueryResultHelper on a Collection
     * @throws Exception if an error occurs
     */
    public DisplayCollection(Collection<?> collection, CollectionDescriptor desc,
                             WebConfig webConfig, Properties webProperties, Map<String,
                             List<FieldDescriptor>> classKeys, List<Class<?>> listOfTypes)
        throws Exception {
        super(collection, desc, webConfig, webProperties, classKeys, listOfTypes);
        this.desc = desc;
    }

    public DisplayCollection(Collection<?> collection, CollectionDescriptor desc,
                             WebConfig webConfig, Properties webProperties, Map<String,
                             List<FieldDescriptor>> classKeys, List<Class<?>> listOfTypes, String objectType)
        throws Exception {
        super(collection, desc, webConfig, webProperties, classKeys, listOfTypes, objectType);
        this.desc = desc;
    }

    /**
     * Get ReferenceDescriptor for this reference.
     * @return ReferenceDescriptor
     */
    public ReferenceDescriptor getDescriptor() {
        return desc;
    }

    /**
     *
     * @return Collection for JSP instead of dealing with InlineResultsTable
     */
    @SuppressWarnings("unchecked")
    public Collection getCollection() {
        return this.collection;
    }

}
