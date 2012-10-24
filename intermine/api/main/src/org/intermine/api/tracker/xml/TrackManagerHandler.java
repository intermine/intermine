package org.intermine.api.tracker.xml;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.ObjectStoreWriter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Event handler for the track xml file
 * @author dbutano
 *
 */
public class TrackManagerHandler extends DefaultHandler
{
    private ObjectStoreWriter osw;
    private DefaultHandler trackHandler = null;

    /**
     * Create a new TrackHandler
     * @param uosw an ObjectStoreWriter to the user profile database
     * problem and continue if possible (used by read-userprofile-xml).
     */
    public TrackManagerHandler(ObjectStoreWriter uosw) {
        super();
        this.osw = uosw;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs)
        throws SAXException {
        if (TemplateTrackBinding.TEMPLATETRACKS.equals(qName)) {
            trackHandler = new TemplateTrackHandler(osw);
        } else if (LoginTrackBinding.LOGINTRACKS.equals(qName)) {
            trackHandler = new LoginTrackHandler(osw);
        } else if (ListTrackBinding.LISTTRACKS.equals(qName)) {
            trackHandler = new ListTrackHandler(osw);
        } else if (QueryTrackBinding.QUERYTRACKS.equals(qName)) {
            trackHandler = new QueryTrackHandler(osw);
        } else if (SearchTrackBinding.SEARCHTRACKS.equals(qName)) {
            trackHandler = new SearchTrackHandler(osw);
        }
        if (trackHandler != null) {
            trackHandler.startElement(uri, localName, qName, attrs);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if (TemplateTrackBinding.TEMPLATETRACKS.equals(qName)
            || LoginTrackBinding.LOGINTRACKS.equals(qName)
            || ListTrackBinding.LISTTRACKS.equals(qName)
            || QueryTrackBinding.QUERYTRACKS.equals(qName)
            || SearchTrackBinding.SEARCHTRACKS.equals(qName)) {
            trackHandler.endElement(uri, localName, qName);
            trackHandler = null;
        }
    }
}
