/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.dataloader;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.exolab.castor.mapping.*;
import org.exolab.castor.xml.*;

import org.flymine.FlyMineException;
import org.flymine.util.TypeUtil;
import org.flymine.util.ListBean;

/**
 * Castor specific method for marshalling business objects into XML.
 * Business objects must have ids set but these are used only for internal
 * references in xml and are not exposed.
 *
 * @author Richard Smith
 */

public class XmlWriterCastorImpl implements XmlWriter
{
    Mapping map;

    /**
     * Construct with the name of the data model.
     *
     * @param model name of the data model being used
     * @throws FlyMineException if anything goes wrong
     */
    public XmlWriterCastorImpl(String model) throws FlyMineException {
        try {
            String mapFile = "castor_xml_" + model.toLowerCase() + ".xml";
            URL mapUrl = XmlWriterCastorImpl.class.getClassLoader().getResource(mapFile);
            map = new Mapping();
            map.loadMapping(mapUrl);
        } catch (IOException e) {
            throw new FlyMineException ("Problem loading Castor mapping file. ", e);
        } catch (MappingException e) {
            throw new FlyMineException ("Problem loading Castor mapping file. ", e);
        }
    }

    /**
     * Marshal a single object to the given XML file.
     *
     * @param obj a business object to marshal
     * @param file file to write XML to
     * @throws FlyMineException if anything goes wrong
     */
    public void writeXml(Object obj, File file) throws FlyMineException {
        try {
            Writer writer = new FileWriter(file);
            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(map);

            List list = new ArrayList();
            list.add(obj);
            List flat = TypeUtil.flatten(list);

            ListBean bean = new ListBean();
            bean.setItems(list);
            marshaller.marshal(bean);
        } catch (CastorException e) {
            throw new FlyMineException("Problem with Castor. ", e);
        } catch (IOException e) {
            throw new FlyMineException("IO problem. ", e);
        } catch (MappingException e) {
            throw new FlyMineException("Problem setting Castor mapping. ", e);
        } catch (Exception e) {
            throw new FlyMineException(e);
        }
    }

    /**
     * Marshal a collection of business objects to the given XML file.
     *
     * @param col a collection of business objects
     * @param file file to write XML to
     * @throws FlyMineException if anything goes wrong
     */
    public void writeXml(Collection col, File file) throws FlyMineException {
        try {
            Writer writer = new FileWriter(file);
            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(map);

            List flatList = TypeUtil.flatten(col);

            ListBean bean = new ListBean();
            bean.setItems(flatList);
            marshaller.marshal(bean);
        } catch (CastorException e) {
            throw new FlyMineException("Problem with Castor. ", e);
        } catch (IOException e) {
            throw new FlyMineException("IO problem. ", e);
        } catch (MappingException e) {
            throw new FlyMineException("Problem setting Castor mapping. ", e);
        } catch (Exception e) {
            throw new FlyMineException(e);
        }
    }
}
