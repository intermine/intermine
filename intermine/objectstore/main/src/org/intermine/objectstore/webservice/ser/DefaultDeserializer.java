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

import java.lang.reflect.Method;

import org.apache.axis.encoding.DeserializerTarget;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.encoding.Deserializer;
import org.apache.axis.encoding.DeserializerImpl;
import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.message.SOAPHandler;
import org.apache.axis.soap.SOAPConstants;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import javax.xml.namespace.QName;

import org.intermine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Deserializer for (bean-like) objects sent via SOAP (i.e.everything except Lists and Models)
 *
 * @author Mark Woodbridge
 */
public class DefaultDeserializer extends DeserializerImpl
{
    private static final Logger LOG = Logger.getLogger(DefaultDeserializer.class);

    protected QName xmlType;
    protected Class javaType;

    /**
     * Constructor
     * @param javaType the type of the object that this instance will deserialize
     * @param xmlType the corresponding QName in the XML
     */
    public DefaultDeserializer(Class javaType, QName xmlType) {
        this.xmlType = xmlType;
        this.javaType = javaType;

        try {
            value = javaType.newInstance();
        } catch (Exception e) {
            LOG.error("error creating instance: " + e);
        }
    }

    /**
     * @see DeserializerImpl#onStartChild
     */
    public SOAPHandler onStartChild(String namespace, String localName, String prefix,
                                    Attributes attributes, DeserializationContext context)
        throws SAXException {
        SOAPConstants soapConstants = context.getMessageContext().getSOAPConstants();

        if (context.isNil(attributes)) {
            return null;
        }
        
        Method field = TypeUtil.getGetter(javaType, localName);

        if (field == null) {
            throw new SAXException("field not found");
        }

        QName childXMLType = context.getTypeFromXSITypeAttr(namespace, localName, attributes);

        String href = attributes.getValue(soapConstants.getAttrHref());
        
        Deserializer dSer = getDeserializer(childXMLType, field.getReturnType(), href, context);

        if (dSer == null) {
            dSer = new DeserializerImpl();
            return (SOAPHandler) dSer;
        }

        dSer.registerValueTarget(new DeserializerTarget(this, localName));
        addChildDeserializer(dSer);

        return (SOAPHandler) dSer;
    }

    /**
     * @see DeserializerImpl#getDeserializer
     */
    protected Deserializer getDeserializer(QName xmlType, 
                                           Class javaType, 
                                           String href,
                                           DeserializationContext context) {
        Deserializer dSer = null;

        if (xmlType != null && href == null) {
            dSer = context.getDeserializerForType(xmlType);
        } else {
            TypeMapping tm = context.getTypeMapping();
            QName defaultXMLType = tm.getTypeQName(javaType);
            if (href == null) {
                dSer = context.getDeserializer(javaType, defaultXMLType);
            } else {
                dSer = new DeserializerImpl();
                dSer.setDefaultType(defaultXMLType);
            }
        }
        return dSer;
    }

    /**
     * @see DeserializerTarget#setChildValue
     */
    public void setChildValue(Object value, Object hint) throws SAXException {
        try {
            String fieldName = (String) hint;
            TypeUtil.setFieldValue(this.value, fieldName, value);
        } catch (Exception e) {
            throw new SAXException(e);
        }
    }
}
