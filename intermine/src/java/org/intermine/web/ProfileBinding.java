package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.SAXParser;
import org.intermine.web.bag.InterMineBag;
import org.intermine.web.bag.InterMineBagBinding;
import org.intermine.web.bag.InterMineIdBag;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

import java.io.Reader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.InputSource;

/**
 * Code for reading and writing Profile objects as XML.
 *
 * @author Kim Rutherford
 */

public class ProfileBinding
{
    /**
     * Convert a Profile to XML and write XML to given writer.
     * @param profile the UserProfile
     * @param model the Model - use when marshalling queries
     * @param os the ObjectStore to use when looking up the ids of objects in bags
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(Profile profile, Model model, ObjectStore os, 
                               XMLStreamWriter writer) {
        try {
            writer.writeStartElement("userprofile");
            writer.writeAttribute("username", profile.getUsername());
            writer.writeAttribute("password", profile.getPassword());

            ItemFactory itemFactory = new ItemFactory(os.getModel());

            Set idSet = new HashSet();

            // serialise all object in all bags and all objects mentioned in primary keys of those
            // items (recursively)
            for (Iterator i = profile.getSavedBags().entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                InterMineBag bag = (InterMineBag) entry.getValue();

                if (bag instanceof InterMineIdBag) {
                    idSet.addAll(bag);
                }
            }
            
            if (!idSet.isEmpty()) {
                List objects = os.getObjectsByIds(idSet);

                writer.writeStartElement("items");
                Iterator objectsIter = objects.iterator();

                while (objectsIter.hasNext()) {
                    ResultsRow rr = (ResultsRow) objectsIter.next();
                    InterMineObject o = (InterMineObject) rr.get(0);
                    Item item = itemFactory.makeItem(o);
                    FullRenderer.render(writer, item);
                }

                writer.writeEndElement();
            }

            writer.writeStartElement("bags");
            for (Iterator i = profile.getSavedBags().entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                String bagName = (String) entry.getKey();
                InterMineBag bag = (InterMineBag) entry.getValue();

                InterMineBagBinding.marshal(bag, bagName, writer);
            }
            writer.writeEndElement();

            writer.writeStartElement("queries");
            for (Iterator i = profile.getSavedQueries().entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                String queryName = (String) entry.getKey();
                PathQuery query = (PathQuery) entry.getValue();

                PathQueryBinding.marshal(query, queryName, os.getModel().getName(), writer);
            }
            writer.writeEndElement();

            writer.writeStartElement("template-queries");
            for (Iterator i = profile.getSavedTemplates().entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                String templateName = (String) entry.getKey();
                TemplateQuery template = (TemplateQuery) entry.getValue();

                TemplateQueryBinding.marshal(template, writer);
            }
            writer.writeEndElement();

            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException("exception while marshalling profile", e);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("exception while marshalling profile", e);
        }
    }

    /**
     * Read a Profile from an XML stream Reader
     * @param reader contains the Profile XML
     * @param profileManager the ProfileManager to pass to the Profile constructor
     * @param os ObjectStore used to resolve object ids
     * @return the new Profile
     */
    public static Profile unmarshal(Reader reader, ProfileManager profileManager, ObjectStore os) {
        try {
            ProfileHandler profileHandler = new ProfileHandler(profileManager);
            SAXParser.parse(new InputSource(reader), profileHandler);
            return profileHandler.getProfile();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
