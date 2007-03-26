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
import org.apache.axis.encoding.DeserializerFactory;
import javax.xml.rpc.encoding.Deserializer;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * Produce ModelDeserializers
 *
 * @author Mark Woodbridge
 */
public class ModelDeserializerFactory implements DeserializerFactory
{
    private ArrayList mechanisms;

    /**
     * @see DeserializerFactory#getDeserializerAs
     */
    public Deserializer getDeserializerAs(String mechanismType) {
        return new ModelDeserializer();
    }

    /**
     * @see DeserializerFactory#getSupportedMechanismTypes
     */
    public Iterator getSupportedMechanismTypes() {
        if (mechanisms == null) {
            mechanisms = new ArrayList();
            mechanisms.add(Constants.AXIS_SAX);
        }
        return mechanisms.iterator();
    }
}
