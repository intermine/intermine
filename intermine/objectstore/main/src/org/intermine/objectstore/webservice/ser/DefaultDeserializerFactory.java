package org.intermine.objectstore.webservice.ser;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.xml.namespace.QName;

import org.apache.axis.encoding.ser.BaseDeserializerFactory;
import org.apache.axis.encoding.Deserializer;

/**
 * Produce DefaultDeserializers
 *
 * @author Mark Woodbridge
 */
public class DefaultDeserializerFactory extends BaseDeserializerFactory
{
    /**
     * Constructor
     * @param javaType the type of the object that the serializer will deserialize
     * @param xmlType the corresponding QName in the XML
     */
    public DefaultDeserializerFactory(Class javaType, QName xmlType) {
        super(DefaultDeserializer.class, xmlType, javaType);      
    }

    /**
     * @see BaseDeserializerFactory#getGeneralPurpose
     */
    protected Deserializer getGeneralPurpose(String mechanismType) {
        if (javaType == null || xmlType == null) {
           return super.getGeneralPurpose(mechanismType);
        }

        return new DefaultDeserializer(javaType, xmlType);
    }
}
