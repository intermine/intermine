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

import org.intermine.model.userprofile.Tag;
import org.intermine.util.SAXParser;

import java.io.Reader;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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
     * Parse Tags from XML
     * @param pm a ProfileManager used to get UserProfile objects for a given username
     * @param userName the user whose tags are being unmarshalled
     * @param reader the saved Tags
     * @return a Set of Tags
     */
    public Set unmarshal(ProfileManager pm, String userName, Reader reader) {
        Set tags = new HashSet();
        try {
            SAXParser.parse(new InputSource(reader), new TagHandler(pm, userName));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return tags;
    }

    /**
     * Extension of PathQueryHandler to handle parsing TemplateQueries
     */
    static class TagHandler extends DefaultHandler
    {
        private String tagName;
        private String tagObjectIdentifier;
        private String tagType;
        private ProfileManager profileManager;
        private String userName;

        /**
         * Constructor
         * @param profileManager add each Tag using this ProfileManager
         * @param userName the name of the user whose profile is being read
         */
        public TagHandler(ProfileManager profileManager, String userName) {
            this.profileManager = profileManager;
            this.userName = userName;
            reset();
        }

        /**
         * @see DefaultHandler#startElement
         */
        public void startElement(String uri, String localName, String qName, Attributes attrs)
            throws SAXException {
            if (qName.equals("tag")) {
                tagName = attrs.getValue("name");
                tagObjectIdentifier = attrs.getValue("objectIdentifier");
                tagType = attrs.getValue("type");
            }
            super.startElement(uri, localName, qName, attrs);
        }

        /**
         * @see DefaultHandler#endElement
         */
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            super.endElement(uri, localName, qName);
            if (qName.equals("tag")) {
                profileManager.addTag(tagName, tagObjectIdentifier, tagType, userName);
                reset();
            }
        }

        private void reset() {
            tagName = "";
            tagObjectIdentifier = "";
            tagType = "";
        }
    }
}
