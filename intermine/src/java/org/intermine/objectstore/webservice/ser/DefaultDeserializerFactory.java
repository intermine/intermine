package org.flymine.objectstore.webservice.ser;
 
/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.xml.namespace.QName;

import org.apache.axis.encoding.ser.BaseDeserializerFactory;

/**
 * Produce DefaultDeserializers
 *
 * @author Mark Woodbridge
 */
public class DefaultDeserializerFactory extends BaseDeserializerFactory
{
    /**
     * Constructor
     * @param javaType the type of the object to instantiate
     * @param xmlType the qname (tag) of the xml version of the object
     */
    public DefaultDeserializerFactory(Class javaType, QName xmlType) {
        super(DefaultDeserializer.class, xmlType, javaType);
    }
}
