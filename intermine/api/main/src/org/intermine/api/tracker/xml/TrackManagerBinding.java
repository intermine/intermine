package org.intermine.api.tracker.xml;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.intermine.objectstore.ObjectStore;

/**
 * Code for reading and writing all tracks objects as XML
 *
 * @author Daniela Butano
 */
public final class TrackManagerBinding
{
    private TrackManagerBinding() {
        // don't
    }

    /**
     * Convert all tracks to XML and write XML to given writer.
     * @param uos the UserObjectStore
     * @param writer the XMLStreamWriter to write to
     */
    public static void marshal(ObjectStore uos, XMLStreamWriter writer) {
        try {
            writer.writeStartElement("tracks");
            TemplateTrackBinding.marshal(uos, writer);
            LoginTrackBinding.marshal(uos, writer);
            ListTrackBinding.marshal(uos, writer);
            QueryTrackBinding.marshal(uos, writer);
            SearchTrackBinding.marshal(uos, writer);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException("exception while marshalling tracks", e);
        }
    }
}
