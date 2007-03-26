package org.intermine.web.results;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.util.TypeUtil;
import org.intermine.web.config.WebConfig;

/**
 * Class to represent a reference field of an object for display in the webapp
 * @author Mark Woodbridge
 */
public class DisplayReference extends DisplayField
{
    ProxyReference proxy;
    Map keyAttributes;
    ReferenceDescriptor desc;
    
    /**
     * Create a new DisplayReference object.
     * @param proxy proxy for the referenced object
     * @param ref metadata for the referenced object
     * @param webConfig the WebConfig object for this webapp
     * @param webProperties the web properties from the session
     * @param classKeys Map of class name to set of keys
     * @throws Exception if an error occurs
     */
    public DisplayReference(ProxyReference proxy, ReferenceDescriptor ref,
                            WebConfig webConfig, Map webProperties, Map classKeys) 
        throws Exception {
        super(getProxyList(proxy), ref, webConfig, webProperties, classKeys);
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
     * Get the identifier fields and values for the object
     * @return the identifiers
     * @throws Exception if an error occurs
     */
    public Map getKeyAttributes() throws Exception {
        if (keyAttributes == null) {
            keyAttributes = new HashMap();
            Set pks = PrimaryKeyUtil.getPrimaryKeyFields(fd.getClassDescriptor().getModel(),
                                                         proxy.getObject().getClass());
            for (Iterator i = pks.iterator(); i.hasNext();) {
                FieldDescriptor refFieldDescriptor = (FieldDescriptor) i.next();
                if (refFieldDescriptor.isAttribute()) {
                    Object fieldValue = TypeUtil.getFieldValue(proxy.getObject(),
                                                               refFieldDescriptor.getName());
                    keyAttributes.put(refFieldDescriptor.getName(), fieldValue);
                }
            }
        }
        return keyAttributes;
    }

    /**
     * Helper method for the constructor.
     */
    private static List getProxyList(ProxyReference proxy) {
        List proxyList = new ArrayList();
        if (proxy != null) {
            proxyList.add(proxy);
        }
        return proxyList;
    }
}