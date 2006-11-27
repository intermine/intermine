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

import java.io.Reader;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.model.userprofile.Tag;
import org.intermine.util.SAXParser;
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
        return handler.count;
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
        private Set tags;
        private int count;

        /**
         * Constructor
         * @param userName the name of the user whose profile is being read
         * @param tags will be populated with any tags to add to the target profile
         */
        public TagHandler(String userName, Set tags) {
            this.userName = userName;
            this.tags = tags;
            reset();
        }

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
                Tag tag = new Tag();
                tag.setTagName(tagName);
                tag.setObjectIdentifier(tagObjectIdentifier);
                tag.setType(tagType);

                // either put tags in set to be added to unmarshalled profile or, when
                // called from ImportTagAction save the tags straight away.
                if (tags != null) {
                    tags.add(tag);
                } else {
                    if (profileManager.getTags(tagName, tagObjectIdentifier,
                                               tagType, userName).isEmpty()) {
                        profileManager.addTag(tagName, tagObjectIdentifier, tagType, userName);
                        count++;
                    }
                }
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
