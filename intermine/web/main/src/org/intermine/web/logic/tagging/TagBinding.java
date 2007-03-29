package org.intermine.web.logic.tagging;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.model.userprofile.Tag;
import org.intermine.util.SAXParser;
import org.intermine.web.logic.profile.ProfileManager;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Convert Tags from the Profile to and from XML
 *
 * @author Kim Rutherford
 */
public class TagBinding
{
    /**
     * Convert a Tag to XML and write XML to given writer.
     *
     * @param tag the Tag
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(Tag tag, XMLStreamWriter writer) {
        try {
            writer.writeStartElement("tag");
            writer.writeAttribute("name", tag.getTagName());
            writer.writeAttribute("objectIdentifier", tag.getObjectIdentifier());
            writer.writeAttribute("type", "" + tag.getType());
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse Tags from XML and write them to the userprofile object store
     * @param pm a ProfileManager used to get UserProfile objects for a given username
     * @param userName the user whose tags are being unmarshalled
     * @param reader the saved Tags
     * @return number of new tags created
     */
    public int unmarshal(ProfileManager pm, String userName, Reader reader) {
        TagHandler handler = new TagHandler(pm, userName);
        try {
            SAXParser.parse(new InputSource(reader), handler);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return handler.getCount();
    }
}
