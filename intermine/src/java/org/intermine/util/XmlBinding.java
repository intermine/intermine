package org.flymine.util;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import org.xml.sax.InputSource;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

import org.flymine.FlyMineException;

import org.apache.log4j.Logger;

/**
 * Represents an XML mapping - performs XML (un)marshalling of objects using Castor
 * @author Mark Woodbridge
 */
public class XmlBinding
{
    protected static final Logger LOG = Logger.getLogger(XmlBinding.class);

    protected Mapping mapping;

    /**
     * Constructor
     * @param filename a Castor mapping file
     * @throws FlyMineException if an error occurs in initialising the mapping
     */
    public XmlBinding(String filename) throws FlyMineException {
        mapping = new Mapping();
        try {
            mapping.loadMapping(getClass().getClassLoader().getResource(filename));
        } catch (Exception e) {
            throw new FlyMineException("Unable to initialise mapping: " + e);
        }
    }

    /**
     * Marshal an object to an XML file
     * @param obj the object to marshal
     * @param writer the Writer to use
     * @throws FlyMineException if an error occurs during marshalling
     */
    public void marshal(Object obj, Writer writer) throws FlyMineException {
        try {
            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(mapping);
            marshaller.marshal(obj);
        } catch (Exception e) {
            throw new FlyMineException("Error during marshalling: " + e);
        }
    }

    /**
     * Unmarshal an XML file to an object
     * @param source the InputSource to read from
     * @return the object
     * @throws FlyMineException if an error occurs during unmarshalling
     */
    public Object unmarshal(InputSource source) throws FlyMineException {
        try {
            Unmarshaller unmarshaller = new Unmarshaller(mapping);
            Object retval = unmarshaller.unmarshal(source);
            if (retval instanceof Collection) {
                Iterator iter = ((Collection) retval).iterator();
                while (iter.hasNext()) {
                    TypeUtil.setFieldValue(iter.next(), "id", null);
                }
            } else {
                TypeUtil.setFieldValue(retval, "id", null);
            }
            return retval;
        } catch (Exception e) {
            throw new FlyMineException("Error during unmarshalling: " + e);
        }
    }
}
