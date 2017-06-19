package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.web.logic.config.WebConfig;

/**
 * Class to represent a reference field of an object for display in the webapp
 * @author Mark Woodbridge
 */
public class DisplayReference extends DisplayField
{
    ProxyReference proxy;
    ReferenceDescriptor desc;

    /**
     * Create a new DisplayReference object.
     * @param proxy proxy for the referenced object
     * @param ref metadata for the referenced object
     * @param webConfig the WebConfig object for this webapp
     * @param classKeys Map of class name to set of keys
     * @param objectType the type of the object.
     * @throws Exception if an error occurs
     */
    public DisplayReference(ProxyReference proxy,
                            ReferenceDescriptor ref,
                            WebConfig webConfig,
                            Map<String,
                            List<FieldDescriptor>> classKeys,
                            String objectType)
        throws Exception {
        super(getProxyList(proxy), ref, webConfig, null, classKeys, null, objectType);
        this.proxy = proxy;
        desc = ref;
    }

    /**
     * Get ReferenceDescriptor for this reference.
     * @return ReferenceDescriptor
     */
    public ReferenceDescriptor getDescriptor() {
        return desc;
    }

    /**
     * Get the id of the object
     * @return the id
     */
    public int getId() {
        return proxy.getId().intValue();
    }

    /**
     * Get the referenced object
     * @return the object
     */
    public Object getObject() {
        return proxy.getObject();
    }

    /**
     * Helper method for the constructor.
     */
    private static List<ProxyReference> getProxyList(ProxyReference proxy) {
        List<ProxyReference> proxyList = new ArrayList<ProxyReference>();
        if (proxy != null) {
            proxyList.add(proxy);
        }
        return proxyList;
    }
}
