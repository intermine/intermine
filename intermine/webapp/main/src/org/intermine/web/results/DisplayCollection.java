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

import org.intermine.metadata.ClassDescriptor;
import org.intermine.web.config.WebConfig;

/**
 * Class to represent a collection field of an object for the webapp
 * @author Mark Woodbridge
 */
public class DisplayCollection extends DisplayField
{
    /**
     * Construct a new DisplayCollection object
     * @param collection the actual collection
     * @param cld the type of this collection
     * @param webConfig the WebConfig object for this webapp
     * @param webProperties the web properties from the session
     * @throws Exception if an error occurs
     */
    public DisplayCollection(Collection collection, ClassDescriptor cld,
                             WebConfig webConfig, Map webProperties) throws Exception {
        super(collection, cld, webConfig, webProperties);
    }
}
