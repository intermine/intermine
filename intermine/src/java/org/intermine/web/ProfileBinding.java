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

//import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.metadata.Model;
//import org.intermine.objectstore.ObjectStore;

/**
 * Code for reading and writing Profile objects as XML.
 *
 * @author Kim Rutherford
 */

public class ProfileBinding
{
    /**
     * Convert a UserProfile to XML and write XML to given writer.
     * @param profile the UserProfile
     * @param model the Model - use when marshalling queries
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(Profile profile, Model model, XMLStreamWriter writer) {
        try {
            writer.writeStartElement("userprofile");
            writer.writeAttribute("username", profile.getUsername());
            writer.writeAttribute("password", profile.getPassword());

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

                PathQueryBinding.marshal(query, queryName, model.getName(), writer);
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
            throw new RuntimeException(e);
        }
    }

    
//     public static Profile unmarshal(InputStream is, ObjectStore os) {
//         return null;
//     }
}
