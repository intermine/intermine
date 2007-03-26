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
import org.apache.axis.encoding.SerializerFactory;
import javax.xml.rpc.encoding.Serializer;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * Produce DefaultSerializers
 *
 * @author Mark Woodbridge
 */
public class DefaultSerializerFactory implements SerializerFactory
{
    private ArrayList mechanisms;

    /**
     * @see SerializerFactory#getSerializerAs
     */
    public Serializer getSerializerAs(String mechanismType) {
        return new DefaultSerializer();
    }

    /**
     * @see SerializerFactory#getSupportedMechanismTypes
     */
    public Iterator getSupportedMechanismTypes() {
        if (mechanisms == null) {
            mechanisms = new ArrayList();
            mechanisms.add(Constants.AXIS_SAX);
        }
        return mechanisms.iterator();
    }
}
