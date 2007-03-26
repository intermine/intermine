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

import org.apache.axis.Constants;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.encoding.Serializer;
import org.apache.axis.wsdl.fromJava.Types;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;

import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * Serializer for a Model
 *
 * @author Mark Woodbridge
 */
public class ModelSerializer implements Serializer
{
    /**
     * @see Serializer#serialize
     */
    public void serialize(QName name, Attributes attributes,
                          Object value, SerializationContext context)
        throws IOException {
        context.startElement(name, attributes);
        context.writeString(value.toString());
        context.endElement();
    }

    /**
     * @see Serializer#getMechanismType
     */
    public String getMechanismType() { return Constants.AXIS_SAX; }

    /**
     * @see Serializer#writeSchema
     */
    public Element writeSchema(Class javaType, Types types) throws Exception {
        return null;
    }
}
