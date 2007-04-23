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
import org.apache.axis.encoding.DeserializerImpl;
import org.xml.sax.SAXException;

import java.io.StringReader;

import org.intermine.modelproduction.ModelParser;
import org.intermine.modelproduction.xml.InterMineModelParser;

/**
 * Deserialize a Model from XML to an Object
 *
 * @author Mark Woodbridge
 */
public class ModelDeserializer extends DeserializerImpl
{
    /**
     * {@inheritDoc}
     */
    public final void onEndElement(String namespace, String localName,
                                   DeserializationContext context) throws SAXException {
        try {
            String model = context.getCurElement().toString();
            ModelParser parser = new InterMineModelParser();
            setValue(parser.process(new StringReader(model)));
        } catch (Exception exp) {
            throw new SAXException(exp);
        }
    }
}
