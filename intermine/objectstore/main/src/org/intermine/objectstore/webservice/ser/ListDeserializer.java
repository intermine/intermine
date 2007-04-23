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

import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.encoding.ser.VectorDeserializer;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Deserialize classes that implement List
 *
 * @author Mark Woodbridge
 */
public class ListDeserializer extends VectorDeserializer
{
    /**
     * {@inheritDoc}
     */
    public final void onEndElement(String namespace, String localName,
                                   DeserializationContext context) throws SAXException {
        value = new ArrayList((Vector) value);
    }
}
