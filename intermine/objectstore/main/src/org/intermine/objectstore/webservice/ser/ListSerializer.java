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

import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.encoding.ser.VectorSerializer;
import org.xml.sax.Attributes;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.Vector;
import java.util.Collection;

/**
 * Serializer for classes that implement List
 *
 * @author Mark Woodbridge
 */
public class ListSerializer extends VectorSerializer
{
    /**
     * @see VectorSerializer#serialize
     */
    public void serialize(QName name, Attributes attributes, Object value,
                          SerializationContext context) throws IOException {
        super.serialize(name, attributes, new Vector((Collection) value), context);
    }
}
