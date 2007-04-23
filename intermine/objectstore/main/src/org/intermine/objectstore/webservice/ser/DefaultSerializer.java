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

import java.io.IOException;
import java.util.Iterator;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;

import org.apache.axis.encoding.Serializer;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.wsdl.fromJava.Types;

import org.intermine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * This the serializer (object to xml translator) for all objects used in objectstore calls
 * (except lists...which will eventually be handled by axis)
 *
 * @author Mark Woodbridge
 */
public class DefaultSerializer implements Serializer
{
    private static final Logger LOG = Logger.getLogger(DefaultSerializer.class);

    /**
     * {@inheritDoc}
     */
    public void serialize(QName name, Attributes attributes, Object value,
                          SerializationContext context) throws IOException {

        context.startElement(name, attributes);

        //iqlquery, resultsinfo, businessobject (list and model have their own serializers)
        for (Iterator fieldInfos = TypeUtil.getFieldInfos(value.getClass()).values().iterator();
             fieldInfos.hasNext();) {
            TypeUtil.FieldInfo fieldInfo = (TypeUtil.FieldInfo) fieldInfos.next();
            try {
                Class fieldType = fieldInfo.getGetter().getReturnType();
                Object fieldValue = fieldInfo.getGetter().invoke(value, new Object[0]);
                QName qname = new QName(name.getNamespaceURI(), fieldInfo.getName());
                QName xmlType = context.getQNameForClass(fieldType);
                context.serialize(qname, null, fieldValue, xmlType, true, null);
            } catch (Exception e) {
                LOG.warn("Unable to serialize field \"" + fieldInfo.getName() + "\" in \""
                         + value.getClass() + "\" due to error: " + e);
            }
        }
        context.endElement();
    }

    /**
     * {@inheritDoc}
     */
    public Element writeSchema(Class javaType, Types types) throws Exception {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getMechanismType() {
        return null;
    }
}
