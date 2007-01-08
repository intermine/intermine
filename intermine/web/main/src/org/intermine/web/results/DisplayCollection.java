package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Map;

import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.web.config.WebConfig;

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
     * @param webProperties the web properties from the session
     * @throws Exception if an error occurs
     */
    public DisplayCollection(Collection collection, CollectionDescriptor desc,
                             WebConfig webConfig, Map webProperties, Map classKeys) 
        throws Exception {
        super(collection, desc, webConfig, webProperties, classKeys);
        this.desc = desc;
    }

    /**
     * Get ReferenceDescriptor for this reference.
     * @return ReferenceDescriptor
     */
    public ReferenceDescriptor getDescriptor() {
        return desc;
    }
}
