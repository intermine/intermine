package org.flymine.dataloader;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.exolab.castor.mapping.*;
import org.exolab.castor.xml.*;

import org.flymine.FlyMineException;
import org.flymine.util.ModelUtil;
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
            URL mapUrl = XmlDataLoader.class.getClassLoader().getResource(mapFile);
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
            List flat = (List) flatten(list);

            ListBean bean = new ListBean();
            bean.setItems(list);
            marshaller.marshal(bean);
        } catch (CastorException e) {
            throw new FlyMineException("Problem with Castor. ", e);
        } catch (IOException e) {
            throw new FlyMineException("IO problem. ", e);
        } catch (MappingException e) {
            throw new FlyMineException("Problem setting Castor mapping. ", e);
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

            List flatList = (List) flatten(col);

            ListBean bean = new ListBean();
            bean.setItems(flatList);
            marshaller.marshal(bean);
        } catch (CastorException e) {
            throw new FlyMineException("Problem with Castor. ", e);
        } catch (IOException e) {
            throw new FlyMineException("IO problem. ", e);
        } catch (MappingException e) {
            throw new FlyMineException("Problem setting Castor mapping. ", e);
        }
    }

    // make all nested objects top-level in returned collection
    private Collection flatten(Collection c) throws FlyMineException {
        try {
            List toStore = new ArrayList();
            Iterator i = c.iterator();
            while (i.hasNext()) {
                flatten(i.next(), toStore);
            }
            return toStore;
        } catch (Exception e) {
            throw new FlyMineException("Problem occurred flattening collection. ", e);
        }
    }

    private void flatten(Object o, Collection c) throws Exception {
        if (o == null || c.contains(o)) {
            return;
        }
        c.add(o);
        Method[] getters = TypeUtil.getGetters(o.getClass());
        for (int i = 0; i < getters.length; i++) {
            Method getter = getters[i];
            Class returnType = getter.getReturnType();
            if (ModelUtil.isCollection(returnType)) {
                Iterator iter = ((Collection) getter.invoke(o, new Object[] {})).iterator();
                while (iter.hasNext()) {
                    flatten(iter.next(), c);
                }
            } else if (ModelUtil.isReference(returnType)) {
                flatten(getter.invoke(o, new Object[] {}), c);
            }
        }
    }

}
