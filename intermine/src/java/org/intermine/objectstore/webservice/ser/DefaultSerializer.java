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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;

import org.apache.axis.encoding.Serializer;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.wsdl.fromJava.Types;

import org.flymine.objectstore.proxy.LazyReference;
import org.flymine.objectstore.query.SingletonResults;
import org.flymine.objectstore.query.fql.FqlQuery;

import org.apache.log4j.Logger;

/**
 * This the serializer (object to xml translator) for all objects used in objectstore calls
 * (except lists...which will eventually be handled by axis)
 *
 * @author Mark Woodbridge
 */
public class DefaultSerializer implements Serializer
{
    protected static final Logger LOG = Logger.getLogger(DefaultSerializer.class);

    /**
     * @see Serializer#serialize
     */
    public void serialize(QName name, Attributes attributes, Object value,
                          SerializationContext context) throws IOException {

        context.startElement(name, attributes);

        //fqlquery, explainresult, proxybean, businessobject
        Iterator entryIter = SerializationUtil.fieldValues(value).entrySet().iterator();
        while (entryIter.hasNext()) {
            Map.Entry entry = (Map.Entry) entryIter.next();
            Field field = (Field) entry.getKey();
            Object fieldValue = entry.getValue();

            //lazyreference
            if (fieldValue instanceof LazyReference) {
                LazyReference ref = (LazyReference) fieldValue;
                if (!ref.isMaterialised()) {
                    fieldValue = new ProxyBean(ref.getType().getName(),
                                                                 ref.getFqlQuery(), 
                                                                 ref.getId());
                }
            }
            
            //singletonresults
            if (fieldValue instanceof SingletonResults) {
                SingletonResults res = (SingletonResults) fieldValue;
                fieldValue = new ProxyBean(fieldValue.getClass().getName(),
                                                             new FqlQuery(res.getQuery()),
                                                             new Integer(-1));
            }

            if (fieldValue instanceof List) {
                fieldValue = new ArrayList((List) fieldValue);
            }

            QName qname = new QName(name.getNamespaceURI(), field.getName());
            QName xmlType = context.getQNameForClass(field.getType());
            context.serialize(qname, null, fieldValue, xmlType, true, null);
        }
        
        context.endElement();
    }

    /**
     * @see Serializer#writeSchema
     */
    public Element writeSchema(Class javaType, Types types) throws Exception {
        return null;
    }

    /**
     * @see Serializer#getMechanismType
     */
    public String getMechanismType() {
        return null;
    }
}
