package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.exolab.castor.mapping.*;
import org.exolab.castor.xml.*;

import org.flymine.FlyMineException;
import org.flymine.objectstore.ObjectStoreException;

import java.util.Iterator;
import java.util.List;
import java.io.IOException;
import java.net.URL;

import org.xml.sax.InputSource;

/**
 * Provides a method for unmarshalling XML given source  into java
 * business objects then calls store on each.  Uses Castor to handle XML binding.
 * store() is AbstractDataLoader.store().
 *
 * @author Richard Smith
 */

public class XmlDataLoader extends DataLoader
{
    /**
     * @see DataLoader#DataLoader
     */
    public XmlDataLoader(IntegrationWriter iw) {
        super(iw);
    }

    /**
     * Static method to unmarshall business objects from a given xml file and call
     * store on each.
     *
     * @param source access to xml file
     * @throws FlyMineException if anything goes wrong with xml or storing
     */
    public void processXml(InputSource source) throws FlyMineException {
        try {
            String modelName = iw.getObjectStore().getModel().getName();
            String mapFile = "castor_xml_" + modelName + ".xml";
            URL mapUrl = XmlDataLoader.class.getClassLoader().getResource(mapFile);
            Mapping mapping = new Mapping();
            mapping.loadMapping(mapUrl);

            Unmarshaller unmarshaller = new Unmarshaller(mapping);
            unmarshaller.setMapping(mapping);

            // FlyMine xml should be have business objects enclosed within a list
            List objects = (List) unmarshaller.unmarshal(source);

            Iterator iter = objects.iterator();
            while (iter.hasNext()) {
                store(iter.next());
            }

        } catch (MappingException e) {
            throw new FlyMineException("Problem loading castor mapping file", e);
        } catch (ValidationException e) {
            throw new FlyMineException("Problem validating source xml", e);
        } catch (MarshalException e) {
            throw new FlyMineException("Problem unmarshaller from data source", e);
        } catch (IOException e) {
            throw new FlyMineException("Problem reading from FileReader", e);
        } catch (ObjectStoreException e) {
            throw new FlyMineException("Problem with store method", e);
        }
    }
}
