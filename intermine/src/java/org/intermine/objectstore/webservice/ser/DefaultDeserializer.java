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
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.apache.axis.encoding.Deserializer;
import org.apache.axis.encoding.DeserializerImpl;
import org.apache.axis.encoding.DeserializerTarget;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.message.SOAPHandler;

import org.flymine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * This the deserializer (xml->object) for all objects used in objectstore calls
 * (except lists...which will eventually be handled by axis)
 *
 * @author Mark Woodbridge
 */
public class DefaultDeserializer extends DeserializerImpl
{
    protected static final Logger LOG = Logger.getLogger(DefaultDeserializer.class);

    QName xmlType;
    Class javaType;

    /**
     * Constructor
     * @param javaType the type of the object to instantiate
     * @param xmlType the qname (tag) of the xml version of the object
     */
    public DefaultDeserializer(Class javaType, QName xmlType) {
        this.xmlType = xmlType;
        this.javaType = javaType;
    }

    /**
     * @see DeserializerImpl#onStartElement
     */
    public void onStartElement(String namespace, String localName, String prefix,
                               Attributes attributes, DeserializationContext context)
        throws SAXException {
        
        if (context.isNil(attributes)) { 
            return;
        }
        
        try {
            setValue(javaType.newInstance());
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }

    /**
     * @see DeserializerImpl#onStartChild
     */
    public SOAPHandler onStartChild(String namespace, String localName, String prefix, 
                                    Attributes attributes, DeserializationContext context) 
        throws SAXException {
        
        if (context.isNil(attributes)) {
            return null;
        }

        QName itemType = context.getTypeFromAttributes(namespace, localName, attributes);
        Deserializer dSer = null;
        if (itemType != null) {
           dSer = context.getDeserializerForType(itemType);
        }
        if (dSer == null) {
            dSer = new DeserializerImpl();
        }

        dSer.registerValueTarget(new DeserializerTarget(this, localName));
        
        addChildDeserializer(dSer);
        
        return (SOAPHandler) dSer;
    }

    /**
     * @see DeserializerImpl#setChildValue
     */
   public void setChildValue(Object value, Object hint) throws SAXException {
       String fieldName = (String) hint;
       try {
           TypeUtil.setFieldValue(this.value, fieldName, value);
       } catch (IllegalAccessException e) {
           throw new SAXException(e);
       }
    }
}
